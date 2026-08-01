package com.htmltoapk.studio.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.htmltoapk.studio.data.model.AdvancedConfig
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.ui.theme.AccentPreset
import com.htmltoapk.studio.ui.theme.AppLocale
import com.htmltoapk.studio.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "settings")

/**
 * NOTE: deliberately has NO `@Inject constructor` — `DataStoreModule` provides
 * this with an `@ApplicationContext` Context.
 */
@Singleton
class SettingsDataStore private constructor(
    private val context: Context
) {
    object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT = stringPreferencesKey("accent")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val LOCALE = stringPreferencesKey("locale")

        val DEFAULT_MIN_SDK = intPreferencesKey("default_min_sdk")
        val DEFAULT_TARGET_SDK = intPreferencesKey("default_target_sdk")
        val DEFAULT_ORIENTATION = stringPreferencesKey("default_orientation")
        val DEFAULT_PROGUARD = booleanPreferencesKey("default_proguard")
    }

    private val baseData = context.dataStore.data.catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }

    val themeMode: Flow<ThemeMode> = baseData.map {
        runCatching { ThemeMode.valueOf(it[Keys.THEME_MODE] ?: ThemeMode.DARK.name) }
            .getOrDefault(ThemeMode.DARK)
    }
    val accent: Flow<AccentPreset> = baseData.map {
        runCatching { AccentPreset.valueOf(it[Keys.ACCENT] ?: AccentPreset.BLUE.name) }
            .getOrDefault(AccentPreset.BLUE)
    }
    val dynamicColor: Flow<Boolean> = baseData.map { it[Keys.DYNAMIC_COLOR] ?: false }
    val locale: Flow<AppLocale> = baseData.map {
        runCatching { AppLocale.valueOf(it[Keys.LOCALE] ?: AppLocale.EN.name) }
            .getOrDefault(AppLocale.EN)
    }
    val defaultMinSdk: Flow<Int> = baseData.map { it[Keys.DEFAULT_MIN_SDK] ?: 24 }
    val defaultTargetSdk: Flow<Int> = baseData.map { it[Keys.DEFAULT_TARGET_SDK] ?: 34 }
    val defaultOrientation: Flow<Orientation> = baseData.map {
        runCatching { Orientation.valueOf(it[Keys.DEFAULT_ORIENTATION] ?: Orientation.AUTO.name) }
            .getOrDefault(Orientation.AUTO)
    }
    val defaultProguard: Flow<Boolean> = baseData.map { it[Keys.DEFAULT_PROGUARD] ?: true }

    suspend fun setThemeMode(mode: ThemeMode) = context.dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    suspend fun setAccent(preset: AccentPreset) = context.dataStore.edit { it[Keys.ACCENT] = preset.name }
    suspend fun setDynamicColor(enabled: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    suspend fun setLocale(locale: AppLocale) = context.dataStore.edit { it[Keys.LOCALE] = locale.name }
    suspend fun setDefaultMinSdk(v: Int) = context.dataStore.edit { it[Keys.DEFAULT_MIN_SDK] = v }
    suspend fun setDefaultTargetSdk(v: Int) = context.dataStore.edit { it[Keys.DEFAULT_TARGET_SDK] = v }
    suspend fun setDefaultOrientation(o: Orientation) = context.dataStore.edit { it[Keys.DEFAULT_ORIENTATION] = o.name }
    suspend fun setDefaultProguard(v: Boolean) = context.dataStore.edit { it[Keys.DEFAULT_PROGUARD] = v }

    /** Build defaults snapshot used when seeding a new Expert Mode project (single read). */
    suspend fun defaultAdvancedConfig(): AdvancedConfig {
        val snap = baseData.first()
        return AdvancedConfig(
            minSdk = snap[Keys.DEFAULT_MIN_SDK] ?: 24,
            targetSdk = snap[Keys.DEFAULT_TARGET_SDK] ?: 34,
            proguard = snap[Keys.DEFAULT_PROGUARD] ?: true
        )
    }

    companion object {
        fun create(context: Context): SettingsDataStore = SettingsDataStore(context.applicationContext)
    }
}
