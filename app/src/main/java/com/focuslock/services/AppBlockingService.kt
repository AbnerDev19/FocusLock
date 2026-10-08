package com.focuslock.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import com.focuslock.data.AppDatabase
import com.focuslock.data.AppSettingsEntity
import com.focuslock.data.ChallengeEntity
import com.focuslock.data.record
import com.focuslock.domain.ContentLevel
import com.focuslock.domain.DomainClassifier
import com.focuslock.domain.Protection
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppBlockingService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handler = Handler(Looper.getMainLooper())
    private val shotExecutor = Executors.newSingleThreadExecutor()
    private val classifier by lazy { ImageClassifierFactory.create(this) }
    @Volatile private var blocked: Set<String> = emptySet()
    @Volatile private var challenge: ChallengeEntity? = null
    @Volatile private var settings: AppSettingsEntity = AppSettingsEntity()
    private var lastPkg = ""
    private var lastTime = 0L
    private var fgPkg = ""
    private var homePkg = ""
    private var cooldownUntil = 0L

    private val guarded = setOf(
        "com.android.settings", "com.google.android.packageinstaller",
        "com.android.packageinstaller", "com.samsung.android.packageinstaller"
    )

    private val ticker = object : Runnable {
        override fun run() {
            scan()
            handler.postDelayed(this, 2500)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        homePkg = packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), PackageManager.MATCH_DEFAULT_ONLY
        )?.activityInfo?.packageName.orEmpty()
        val db = AppDatabase.get(this)
        scope.launch { db.blockedAppDao().all().collect { list -> blocked = list.map { it.packageName }.toSet() } }
        scope.launch { db.challengeDao().active().collect { challenge = it } }
        scope.launch { db.settingsDao().observe().collect { settings = it ?: AppSettingsEntity() } }
        handler.postDelayed(ticker, 2500)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
        fgPkg = pkg
        if (!Protection.isActive(challenge)) return

        var block = settings.protectApps && pkg in blocked
        if (!block && pkg in guarded && Protection.hardcoreActive(settings)) {
            val root = rootInActiveWindow
            block = root?.findAccessibilityNodeInfosByText("FocusLock")?.isNotEmpty() == true
        }
        if (!block) return

        val now = SystemClock.elapsedRealtime()
        if (pkg == lastPkg && now - lastTime < 1500) return
        lastPkg = pkg
        lastTime = now

        scope.launch { AppDatabase.get(this@AppBlockingService).blockLogDao().record() }
        startActivity(
            Intent(this, BlockActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra("pkg", pkg)
        )
    }

    /** Filtro visual: captura a tela a cada 2,5 s, classifica no aparelho e descarta a imagem. */
    private fun scan() {
        if (Build.VERSION.SDK_INT < 30) return
        val st = settings
        if (!st.visualFilter || !Protection.isActive(challenge)) return
        if (!getSystemService(PowerManager::class.java).isInteractive) return
        if (fgPkg == packageName || fgPkg == homePkg) return
        if (SystemClock.elapsedRealtime() < cooldownUntil) return
        val min = ContentLevel.entries[st.sensitivity.coerceIn(1, 3)]
        takeScreenshot(Display.DEFAULT_DISPLAY, shotExecutor, object : AccessibilityService.TakeScreenshotCallback {
            override fun onSuccess(screenshot: AccessibilityService.ScreenshotResult) {
                val buffer = screenshot.hardwareBuffer
                val hw = Bitmap.wrapHardwareBuffer(buffer, screenshot.colorSpace)
                val bmp = hw?.copy(Bitmap.Config.ARGB_8888, false)
                hw?.recycle()
                buffer.close()
                if (bmp == null) return
                val level = try { classifier.classify(bmp) } catch (e: Exception) { ContentLevel.SAFE }
                bmp.recycle()
                if (DomainClassifier.shouldBlock(level, min)) handler.post { onExplicit() }
            }

            override fun onFailure(errorCode: Int) {}
        })
    }

    private fun onExplicit() {
        cooldownUntil = SystemClock.elapsedRealtime() + 8000
        scope.launch { AppDatabase.get(this@AppBlockingService).blockLogDao().record() }
        performGlobalAction(GLOBAL_ACTION_HOME)
        handler.postDelayed({
            startActivity(
                Intent(this, BlockActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    .putExtra("reason", "visual")
            )
        }, 300)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        shotExecutor.shutdown()
        scope.cancel()
        super.onDestroy()
    }
}
