package com.radiomaster.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.radiomaster.app.domain.model.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary = RadioPrimary,
    primaryContainer = RadioPrimaryContainer,
    secondary = RadioSecondary,
    secondaryContainer = RadioSecondaryContainer,
    surface = Color(0xFF0B0E14),
    background = Color(0xFF0B0E14)
)

private val LightColorScheme = lightColorScheme(
    primary = RadioPrimary,
    primaryContainer = RadioPrimaryContainer,
    secondary = RadioSecondary,
    secondaryContainer = RadioSecondaryContainer,
    background = Color(0xFFFFFFFF)
)

@Composable
fun RadioMasterTheme(
    themeMode: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.SYSTEM -> isSystemDark
    }
    val colorScheme = if (isDark) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = RadioTypography,
        content = content
    )
}
