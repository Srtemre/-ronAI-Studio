package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.domain.model.AccentColor
import com.example.domain.model.ThemeMode

fun getLightColorScheme(accentColor: AccentColor): ColorScheme {
    val primaryColor = Color(accentColor.lightHex)
    return lightColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.12f),
        onPrimaryContainer = primaryColor,
        secondary = IosIndigo,
        onSecondary = Color.White,
        background = IosBackgroundLight,
        onBackground = IosTextPrimaryLight,
        surface = IosSurfaceLight,
        onSurface = IosTextPrimaryLight,
        surfaceVariant = IosFieldBackgroundLight,
        onSurfaceVariant = IosTextSecondaryLight,
        outline = IosSeparatorLight,
        outlineVariant = Color(0xFFD1D1D6)
    )
}

fun getDarkColorScheme(accentColor: AccentColor): ColorScheme {
    val primaryColor = Color(accentColor.darkHex)
    return darkColorScheme(
        primary = primaryColor,
        onPrimary = Color.White,
        primaryContainer = primaryColor.copy(alpha = 0.22f),
        onPrimaryContainer = primaryColor,
        secondary = IosIndigo,
        onSecondary = Color.White,
        background = IosBackgroundDark,
        onBackground = IosTextPrimaryDark,
        surface = IosSurfaceDark,
        onSurface = IosTextPrimaryDark,
        surfaceVariant = IosFieldBackgroundDark,
        onSurfaceVariant = IosTextSecondaryDark,
        outline = IosSeparatorDark,
        outlineVariant = Color(0xFF3A3A3C)
    )
}

@Composable
fun HtmlAppBuilderTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accentColor: AccentColor = AccentColor.BLUE,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val colorScheme = if (darkTheme) getDarkColorScheme(accentColor) else getLightColorScheme(accentColor)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

