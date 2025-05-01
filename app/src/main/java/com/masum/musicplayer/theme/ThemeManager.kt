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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    // Modern color palette
    private val PrimaryColor = Color(0xFF6C63FF)  // Vibrant purple
    private val SecondaryColor = Color(0xFF4CAF50) // Fresh green
    private val AccentColor = Color(0xFFFF4081)    // Energetic pink
    private val BackgroundColor = Color(0xFF121212) // Dark background
    private val SurfaceColor = Color(0xFF1E1E1E)   // Slightly lighter surface
    private val OnBackgroundColor = Color(0xFFFFFFFF) // White text
    private val OnSurfaceColor = Color(0xFFE0E0E0)   // Light gray text

    // Light theme colors
    private val LightColorScheme = lightColorScheme(
        primary = PrimaryColor,
        secondary = SecondaryColor,
        tertiary = AccentColor,
        background = Color(0xFFF5F5F5),
        surface = Color(0xFFFFFFFF),
        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFFFFFFFF),
        onTertiary = Color(0xFFFFFFFF),
        onBackground = Color(0xFF000000),
        onSurface = Color(0xFF000000)
    )

    // Dark theme colors
    private val DarkColorScheme = darkColorScheme(
        primary = PrimaryColor,
        secondary = SecondaryColor,
        tertiary = AccentColor,
        background = BackgroundColor,
        surface = SurfaceColor,
        onPrimary = Color(0xFFFFFFFF),
        onSecondary = Color(0xFFFFFFFF),
        onTertiary = Color(0xFFFFFFFF),
        onBackground = OnBackgroundColor,
        onSurface = OnSurfaceColor
    )

    // Custom typography
    val MusicPlayerTypography = androidx.compose.material3.Typography(
        displayLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            letterSpacing = 0.5.sp
        ),
        displayMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp,
            letterSpacing = 0.5.sp
        ),
        displaySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 24.sp,
            letterSpacing = 0.5.sp
        ),
        headlineLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = 0.5.sp
        ),
        headlineMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            letterSpacing = 0.5.sp
        ),
        headlineSmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp
        ),
        bodyLarge = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            letterSpacing = 0.5.sp
        ),
        bodyMedium = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp,
            letterSpacing = 0.5.sp
        ),
        bodySmall = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            letterSpacing = 0.5.sp
        )
    )

    @Composable
    fun MusicPlayerTheme(
        darkTheme: Boolean = true,
        content: @Composable () -> Unit
    ) {
        val view = LocalView.current
        val isSystemInDarkTheme = isSystemInDarkTheme()
        
        val isDarkTheme = when (currentThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> isSystemInDarkTheme
        }

        val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

        DisposableEffect(view) {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !isDarkTheme
            
            onDispose {}
        }

        MaterialTheme(
            colorScheme = colorScheme,
            typography = MusicPlayerTypography,
            content = content
        )
    }
} 