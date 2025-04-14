package com.masum.musicplayer.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

enum class ThemeMode {
    LIGHT, DARK, SYSTEM
}

object ThemeManager {
    private var currentThemeMode by mutableStateOf(ThemeMode.SYSTEM)

    fun setThemeMode(mode: ThemeMode) {
        currentThemeMode = mode
    }

    fun getThemeMode(): ThemeMode = currentThemeMode

    @Composable
    fun MusicPlayerTheme(
        content: @Composable () -> Unit
    ) {
        val view = LocalView.current
        val isSystemInDarkTheme = isSystemInDarkTheme()
        
        val isDarkTheme = when (currentThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme
        }

        val colorScheme = if (isDarkTheme) {
            darkColorScheme(
                primary = Color(0xFF7C4DFF),
                secondary = Color(0xFF03DAC6),
                tertiary = Color(0xFF3700B3),
                background = Color(0xFF121212),
                surface = Color(0xFF1E1E1E),
                onBackground = Color(0xFFFFFFFF),
                onSurface = Color(0xFFFFFFFF),
                surfaceVariant = Color(0xFF2D2D2D),
                onSurfaceVariant = Color(0xFFB3B3B3)
            )
        } else {
            lightColorScheme(
                primary = Color(0xFF6200EE),
                secondary = Color(0xFF03DAC6),
                tertiary = Color(0xFF3700B3),
                background = Color(0xFFF5F5F5),
                surface = Color(0xFFFFFFFF),
                onBackground = Color(0xFF121212),
                onSurface = Color(0xFF121212),
                surfaceVariant = Color(0xFFE0E0E0),
                onSurfaceVariant = Color(0xFF757575)
            )
        }

        DisposableEffect(view) {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            
            onDispose {}
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = MaterialTheme.typography.copy(
                headlineLarge = MaterialTheme.typography.headlineLarge.copy(
                    letterSpacing = 0.15.sp
                ),
                titleLarge = MaterialTheme.typography.titleLarge.copy(
                    letterSpacing = 0.sp
                ),
                bodyLarge = MaterialTheme.typography.bodyLarge.copy(
                    letterSpacing = 0.5.sp
                )
            ),
            content = content
        )
    }
} 