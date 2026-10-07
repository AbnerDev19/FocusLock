package com.focuslock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.focuslock.presentation.FocusLockApp
import com.focuslock.presentation.FocusLockTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FocusLockTheme { FocusLockApp() } }
    }
}
