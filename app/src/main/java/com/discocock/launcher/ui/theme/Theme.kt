package com.discocock.launcher.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Custom "Başlat" button color; Unspecified = keep the built-in style. */
val LocalLaunchButtonColor = staticCompositionLocalOf { Color.Unspecified }

private fun discoColorScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = Color(0xFF0F1115),
    secondary = accent,
    onSecondary = Color(0xFF0F1115),
    primaryContainer = DiscoSurfaceVariant,
    onPrimaryContainer = DiscoOnSurface,
    background = DiscoBackground,
    onBackground = DiscoOnSurface,
    surface = DiscoSurface,
    onSurface = DiscoOnSurface,
    surfaceVariant = DiscoSurfaceVariant,
    onSurfaceVariant = DiscoOnSurfaceMuted,
    outline = DiscoOutline,
    error = Color(0xFFF87171),
)

@Composable
fun DiscoTheme(accentColor: Color = DiscoAccent, content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = discoColorScheme(accentColor),
        content = content,
    )
}
