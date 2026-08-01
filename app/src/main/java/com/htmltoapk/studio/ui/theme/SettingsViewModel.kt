package com.htmltoapk.studio.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.domain.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: AccentPreset = AccentPreset.BLUE,
    val dynamicColor: Boolean = false,
    val locale: AppLocale = AppLocale.EN,
    val defaultMinSdk: Int = 24,
    val defaultTargetSdk: Int = 34,
    val defaultOrientation: Orientation = Orientation.AUTO,
    val defaultProguard: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repo: SettingsRepository
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        repo.themeMode, repo.accent, repo.dynamicColor, repo.locale,
        repo.defaultMinSdk, repo.defaultTargetSdk, repo.defaultOrientation, repo.defaultProguard
    ) { values ->
        SettingsUiState(
            themeMode = values[0] as ThemeMode,
            accent = values[1] as AccentPreset,
            dynamicColor = values[2] as Boolean,
            locale = values[3] as AppLocale,
            defaultMinSdk = values[4] as Int,
            defaultTargetSdk = values[5] as Int,
            defaultOrientation = values[6] as Orientation,
            defaultProguard = values[7] as Boolean
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { repo.setThemeMode(mode) }
    fun setAccent(preset: AccentPreset) = viewModelScope.launch { repo.setAccent(preset) }
    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch { repo.setDynamicColor(enabled) }
    fun setLocale(locale: AppLocale) = viewModelScope.launch { repo.setLocale(locale) }
    fun setDefaultMinSdk(v: Int) = viewModelScope.launch { repo.setDefaultMinSdk(v) }
    fun setDefaultTargetSdk(v: Int) = viewModelScope.launch { repo.setDefaultTargetSdk(v) }
    fun setDefaultOrientation(o: Orientation) = viewModelScope.launch { repo.setDefaultOrientation(o) }
    fun setDefaultProguard(v: Boolean) = viewModelScope.launch { repo.setDefaultProguard(v) }
}
