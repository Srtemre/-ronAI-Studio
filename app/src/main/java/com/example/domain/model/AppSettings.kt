package com.example.domain.model

enum class ThemeMode(val titleKey: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class AccentColor(
    val idName: String,
    val lightHex: Long,
    val darkHex: Long
) {
    BLUE("Blue", 0xFF007AFF, 0xFF0A84FF),
    PURPLE("Purple", 0xFF5856D6, 0xFF5E5CE6),
    GREEN("Green", 0xFF34C759, 0xFF30D158),
    ORANGE("Orange", 0xFFFF9500, 0xFFFF9F0A),
    PINK("Pink", 0xFFFF2D55, 0xFFFF375F),
    TEAL("Teal", 0xFF30B0C7, 0xFF40C8E0)
}

enum class AppLanguage(val code: String, val displayName: String) {
    ENGLISH("en", "English"),
    TURKISH("tr", "Türkçe")
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColor: AccentColor = AccentColor.BLUE,
    val language: AppLanguage = AppLanguage.ENGLISH
)

