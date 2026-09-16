package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color

@Composable
fun MyApplicationTheme(
    appThemeState: AppThemeState = AppThemeState(),
    content: @Composable () -> Unit
) {
    val isDark = when (appThemeState.mode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    }

    val isNightVibes = appThemeState.style == AppUIStyle.NIGHT_VIBES
    val primary = if (isNightVibes) Color(0xFF0091FF) else appThemeState.preset.primaryColor
    val secondary = if (isNightVibes) Color(0xFF00E5FF) else appThemeState.preset.secondaryColor

    val darkScheme = darkColorScheme(
        primary = primary,
        onPrimary = if (!isNightVibes && appThemeState.preset == ThemePreset.OBSIDIAN) Color.Black else Color.White,
        secondary = secondary,
        onSecondary = Color.White,
        background = if (isNightVibes) Color(0xFF060913) else when (appThemeState.preset) {
            ThemePreset.OBSIDIAN -> Color(0xFF050505)
            ThemePreset.WALLPAPER -> Color(0xFF0B1120)
            else -> Color(0xFF121216)
        },
        surface = if (isNightVibes) Color(0xFF0E1626) else when (appThemeState.preset) {
            ThemePreset.OBSIDIAN -> Color(0xFF161616)
            ThemePreset.WALLPAPER -> Color(0xFF151E32)
            else -> Color(0xFF1E1C24)
        },
        surfaceVariant = if (isNightVibes) Color(0xFF152037) else when (appThemeState.preset) {
            ThemePreset.OBSIDIAN -> Color(0xFF222222)
            ThemePreset.WALLPAPER -> Color(0xFF1E293B)
            else -> Color(0xFF282532)
        },
        onBackground = Color(0xFFF1F5F9),
        onSurface = Color(0xFFE2E8F0),
        onSurfaceVariant = if (isNightVibes) Color(0xFF8DA0BC) else Color(0xFF94A3B8)
    )

    val lightScheme = lightColorScheme(
        primary = primary,
        onPrimary = Color.White,
        secondary = secondary,
        onSecondary = Color.White,
        background = Color(0xFFF8FAFC),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFF1F5F9),
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF1E293B),
        onSurfaceVariant = Color(0xFF64748B)
    )

    val colorScheme = if (isDark) darkScheme else lightScheme

    CompositionLocalProvider(LocalAppThemeState provides appThemeState) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
