package com.htmltoapk.studio.ui.theme

import androidx.compose.ui.graphics.Color

// Dark scheme (default)
val DarkPrimary = Color(0xFF8AB4F8)
val DarkOnPrimary = Color(0xFF002F66)
val DarkPrimaryContainer = Color(0xFF003F8E)
val DarkOnPrimaryContainer = Color(0xFFD3E3FF)
val DarkSecondary = Color(0xFFA6C8FF)
val DarkOnSecondary = Color(0xFF00315F)
val DarkSecondaryContainer = Color(0xFF004886)
val DarkOnSecondaryContainer = Color(0xFFD3E3FF)
val DarkTertiary = Color(0xFFFFB59C)
val DarkOnTertiary = Color(0xFF5D1700)
val DarkTertiaryContainer = Color(0xFF822500)
val DarkOnTertiaryContainer = Color(0xFFFFDBCF)
val DarkBackground = Color(0xFF0E1116)
val DarkOnBackground = Color(0xFFE3E2E6)
val DarkSurface = Color(0xFF101418)
val DarkOnSurface = Color(0xFFE3E2E6)
val DarkSurfaceVariant = Color(0xFF43474E)
val DarkOnSurfaceVariant = Color(0xFFC3C7CF)
val DarkOutline = Color(0xFF8D9199)
val DarkError = Color(0xFFFFB4AB)
val DarkOnError = Color(0xFF690005)

// Light scheme
val LightPrimary = Color(0xFF0B57D0)
val LightOnPrimary = Color(0xFFFFFFFF)
val LightPrimaryContainer = Color(0xFFD3E3FF)
val LightOnPrimaryContainer = Color(0xFF001B3F)
val LightSecondary = Color(0xFF555F71)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD9E3F8)
val LightOnSecondaryContainer = Color(0xFF121C2B)
val LightTertiary = Color(0xFF9C4233)
val LightOnTertiary = Color(0xFFFFFFFF)
val LightTertiaryContainer = Color(0xFFFFDBCF)
val LightOnTertiaryContainer = Color(0xFF3B0900)
val LightBackground = Color(0xFFFDFBFF)
val LightOnBackground = Color(0xFF1A1C1E)
val LightSurface = Color(0xFFFDFBFF)
val LightOnSurface = Color(0xFF1A1C1E)
val LightSurfaceVariant = Color(0xFFE0E2EC)
val LightOnSurfaceVariant = Color(0xFF43474E)
val LightOutline = Color(0xFF74777F)
val LightError = Color(0xFFBA1A1A)
val LightOnError = Color(0xFFFFFFFF)

/**
 * Accent palette presets selectable from Settings.
 */
enum class AccentPreset(val displayKey: String, val light: Color, val dark: Color) {
    BLUE("Blue", LightPrimary, DarkPrimary),
    PURPLE("Purple", Color(0xFF6750A4), Color(0xFFCFBCFF)),
    GREEN("Green", Color(0xFF006E1C), Color(0xFF89E296)),
    ORANGE("Orange", Color(0xFF8C5000), Color(0xFFFFB779)),
    RED("Red", Color(0xFFBA1A1A), Color(0xFFFFB4AB)),
    TEAL("Teal", Color(0xFF006A6A), Color(0xFF4EDADA));
}

object AccentColors {
    fun primary(preset: AccentPreset, dark: Boolean): Color =
        if (dark) preset.dark else preset.light
}
