package com.htmltoapk.studio.data.repository

import com.htmltoapk.studio.data.datastore.SettingsDataStore
import com.htmltoapk.studio.data.model.AdvancedConfig
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.domain.repository.SettingsRepository
import com.htmltoapk.studio.ui.theme.AccentPreset
import com.htmltoapk.studio.ui.theme.AppLocale
import com.htmltoapk.studio.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val ds: SettingsDataStore
) : SettingsRepository {

    override val themeMode: Flow<ThemeMode> = ds.themeMode
    override val accent: Flow<AccentPreset> = ds.accent
    override val dynamicColor: Flow<Boolean> = ds.dynamicColor
    override val locale: Flow<AppLocale> = ds.locale
    override val defaultMinSdk: Flow<Int> = ds.defaultMinSdk
    override val defaultTargetSdk: Flow<Int> = ds.defaultTargetSdk
    override val defaultOrientation: Flow<Orientation> = ds.defaultOrientation
    override val defaultProguard: Flow<Boolean> = ds.defaultProguard

    override suspend fun setThemeMode(mode: ThemeMode) = ds.setThemeMode(mode)
    override suspend fun setAccent(preset: AccentPreset) = ds.setAccent(preset)
    override suspend fun setDynamicColor(enabled: Boolean) = ds.setDynamicColor(enabled)
    override suspend fun setLocale(locale: AppLocale) = ds.setLocale(locale)
    override suspend fun setDefaultMinSdk(v: Int) = ds.setDefaultMinSdk(v)
    override suspend fun setDefaultTargetSdk(v: Int) = ds.setDefaultTargetSdk(v)
    override suspend fun setDefaultOrientation(o: Orientation) = ds.setDefaultOrientation(o)
    override suspend fun setDefaultProguard(v: Boolean) = ds.setDefaultProguard(v)
    override suspend fun defaultAdvanced(): AdvancedConfig = ds.defaultAdvancedConfig()
}
