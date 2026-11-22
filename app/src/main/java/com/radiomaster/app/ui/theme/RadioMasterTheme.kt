package com.radiomaster.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.radiomaster.app.domain.repository.ThemeMode

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0F3D7A),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD7E4F7),
    onPrimaryContainer = Color(0xFF0A2547),
    secondary = Color(0xFFC98A1C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFDE8A8),
    onSecondaryContainer = Color(0xFF4A3203),
    surface = Color(0xFFF6F8FC),
    onSurface = Color(0xFF151A21),
    surfaceVariant = Color(0xFFE3E8F0),
    onSurfaceVariant = Color(0xFF454E5C),
    background = Color(0xFFF6F8FC),
    onBackground = Color(0xFF151A21),
    outline = Color(0xFF748090),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFAEC9F0),
    onPrimary = Color(0xFF0A2547),
    primaryContainer = Color(0xFF1B4C88),
    onPrimaryContainer = Color(0xFFD7E4F7),
    secondary = Color(0xFFF2C86B),
    onSecondary = Color(0xFF40290A),
    secondaryContainer = Color(0xFF6B4A10),
    onSecondaryContainer = Color(0xFFFDE8A8),
    surface = Color(0xFF0B0E14),
    onSurface = Color(0xFFE6EAF2),
    surfaceVariant = Color(0xFF1C2230),
    onSurfaceVariant = Color(0xFFB6BFCC),
    background = Color(0xFF0B0E14),
    onBackground = Color(0xFFE6EAF2),
    outline = Color(0xFF8A94A3),
)

/**
 * Material 3 with a fixed brand palette rather than dynamic color: a radio app's identity is the
 * globe, and a wallpaper-derived scheme made the map dots unreadable on some devices.
 *
 * [ThemeMode.SYSTEM] follows the system setting; the other two pin the appearance, which is the
 * point of the setting in a car dock where the system flips between day and night constantly.
 */
@Composable
fun RadioMasterTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkScheme else LightScheme,
        content = content,
    )
}
