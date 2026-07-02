package com.kebuz.kebuzlect.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val NeonCyan = Color(0xFF55FFFF)
val NeonMagenta = Color(0xFFFF55FF)
val CyanInk = Color(0xFF0B8C8C)
val MagentaInk = Color(0xFFB800B8)
val StatusRed = Color(0xFFFF453A)
val StatusAmber = Color(0xFFFFB020)
val StatusGreen = Color(0xFF30D158)

data class KebuzColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val foreground: Color,
    val muted: Color,
    val divider: Color,
    val grid: Color,
    val dateBg: Color,
    val tile: Color,
    val cyan: Color,
    val magenta: Color,
    val red: Color,
    val amber: Color,
    val green: Color,
    val neonCyan: Color,
    val neonMagenta: Color,
)

val LightKebuzColors = KebuzColors(
    isDark = false,
    background = Color(0xFFF5F5F6),
    surface = Color(0xFFFFFFFF),
    foreground = Color(0xFF161618),
    muted = Color(0xFF76767E),
    divider = Color(0xFFE2E2E6),
    grid = Color(0xFF1E2024),
    dateBg = Color(0xFFECECEF),
    tile = NeonCyan.copy(alpha = 0.22f),
    cyan = CyanInk,
    magenta = MagentaInk,
    red = StatusRed,
    amber = StatusAmber,
    green = StatusGreen,
    neonCyan = NeonCyan,
    neonMagenta = NeonMagenta,
)

val DarkKebuzColors = KebuzColors(
    isDark = true,
    background = Color(0xFF0A0A0B),
    surface = Color(0xFF141416),
    foreground = Color(0xFFFAFAFA),
    muted = Color(0xFF86868E),
    divider = Color(0xFF222226),
    grid = Color(0xFF16181C),
    dateBg = Color(0xFF161618),
    tile = NeonCyan.copy(alpha = 0.14f),
    cyan = NeonCyan,
    magenta = NeonMagenta,
    red = StatusRed,
    amber = StatusAmber,
    green = StatusGreen,
    neonCyan = NeonCyan,
    neonMagenta = NeonMagenta,
)

val LocalKebuzColors = staticCompositionLocalOf { LightKebuzColors }
