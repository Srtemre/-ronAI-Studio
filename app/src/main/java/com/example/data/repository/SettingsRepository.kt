package com.example.data.repository

import android.content.Context
import com.example.domain.model.AccentColor
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("app_settings_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeName = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentName = prefs.getString("accent_color", AccentColor.BLUE.name) ?: AccentColor.BLUE.name
        val langName = prefs.getString("language", AppLanguage.ENGLISH.name) ?: AppLanguage.ENGLISH.name

        val themeMode = runCatching { ThemeMode.valueOf(themeName) }.getOrDefault(ThemeMode.SYSTEM)
        val accentColor = runCatching { AccentColor.valueOf(accentName) }.getOrDefault(AccentColor.BLUE)
        val language = runCatching { AppLanguage.valueOf(langName) }.getOrDefault(AppLanguage.ENGLISH)

        return AppSettings(themeMode = themeMode, accentColor = accentColor, language = language)
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        prefs.edit().putString("theme_mode", themeMode.name).apply()
        _settings.value = _settings.value.copy(themeMode = themeMode)
    }

    fun updateAccentColor(accentColor: AccentColor) {
        prefs.edit().putString("accent_color", accentColor.name).apply()
        _settings.value = _settings.value.copy(accentColor = accentColor)
    }

    fun updateLanguage(language: AppLanguage) {
        prefs.edit().putString("language", language.name).apply()
        _settings.value = _settings.value.copy(language = language)
    }
}
