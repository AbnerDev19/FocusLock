package com.focuslock.presentation

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF3B5BDB), onPrimary = Color.White, secondary = Color(0xFF12B886),
    background = Color(0xFFF8F9FA), surface = Color.White, surfaceVariant = Color(0xFFE9ECEF),
    onBackground = Color(0xFF212529), onSurface = Color(0xFF212529), onSurfaceVariant = Color(0xFF6C757D)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF748FFC), onPrimary = Color(0xFF0B1020), secondary = Color(0xFF38D9A9),
    background = Color(0xFF0E1116), surface = Color(0xFF161B22), surfaceVariant = Color(0xFF242B35),
    onBackground = Color(0xFFE6EDF3), onSurface = Color(0xFFE6EDF3), onSurfaceVariant = Color(0xFF8B949E)
)

@Composable
fun FocusLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) Dark else Light, content = content)
}
