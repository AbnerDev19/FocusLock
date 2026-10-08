package com.focuslock.services

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.focuslock.data.AppDatabase
import com.focuslock.data.AppSettingsEntity
import com.focuslock.data.ChallengeEntity
import com.focuslock.data.record
import com.focuslock.domain.Protection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AppBlockingService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    @Volatile private var blocked: Set<String> = emptySet()
    @Volatile private var challenge: ChallengeEntity? = null
    @Volatile private var settings: AppSettingsEntity = AppSettingsEntity()
    private var lastPkg = ""
    private var lastTime = 0L

    private val guarded = setOf(
        "com.android.settings", "com.google.android.packageinstaller",
        "com.android.packageinstaller", "com.samsung.android.packageinstaller"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        val db = AppDatabase.get(this)
        scope.launch { db.blockedAppDao().all().collect { list -> blocked = list.map { it.packageName }.toSet() } }
        scope.launch { db.challengeDao().active().collect { challenge = it } }
        scope.launch { db.settingsDao().observe().collect { settings = it ?: AppSettingsEntity() } }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return
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

    override fun onInterrupt() {}

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
