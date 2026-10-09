package com.discocock.launcher.ui.theme

import androidx.compose.ui.graphics.Color

// Disco Launcher Prism-minimal palette (desktop defaults: #7C8CF8 / #A5B4FC).
val DiscoPrimary = Color(0xFF7C8CF8)
val DiscoAccent = Color(0xFFA5B4FC)
val DiscoBackground = Color(0xFF0F1115)
val DiscoSurface = Color(0xFF161A22)
val DiscoSurfaceVariant = Color(0xFF1E2430)
val DiscoOutline = Color(0xFF2A3140)
val DiscoOnSurface = Color(0xFFE2E6F0)
val DiscoOnSurfaceMuted = Color(0xFF9AA3B8)

// The 8 accent choices of the desktop launcher's Prism-minimal theme.
val AccentChoices: List<Pair<String, Color>> = listOf(
    "indigo" to Color(0xFFA5B4FC),
    "violet" to Color(0xFFA78BFA),
    "blue" to Color(0xFF60A5FA),
    "cyan" to Color(0xFF38BDF8),
    "emerald" to Color(0xFF34D399),
    "amber" to Color(0xFFFBBF24),
    "orange" to Color(0xFFFB923C),
    "rose" to Color(0xFFF87171),
)

fun accentColorOf(key: String): Color =
    AccentChoices.firstOrNull { it.first == key }?.second ?: DiscoAccent
