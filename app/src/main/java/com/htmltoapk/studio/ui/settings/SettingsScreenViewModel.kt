package com.htmltoapk.studio.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.domain.repository.SettingsRepository
import com.htmltoapk.studio.ui.theme.AccentPreset
import com.htmltoapk.studio.ui.theme.AppLocale
import com.htmltoapk.studio.ui.theme.SettingsViewModel
import com.htmltoapk.studio.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsScreenState(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val accent: AccentPreset = AccentPreset.BLUE,
    val dynamicColor: Boolean = false,
    val locale: AppLocale = AppLocale.EN,
    val defaultMinSdk: Int = 24,
    val defaultTargetSdk: Int = 34,
    val defaultOrientation: Orientation = Orientation.AUTO,
    val defaultProguard: Boolean = true,
    val cacheBytes: Long = 0L,
    val cacheCleared: Boolean = false
)

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val shared: SettingsViewModel,
    private val repo: SettingsRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val cacheBytes = MutableStateFlow(0L)
    private val cacheCleared = MutableStateFlow(false)

    val state: StateFlow<SettingsScreenState> = combine(
        shared.state, cacheBytes, cacheCleared
    ) { s, bytes, cleared ->
        SettingsScreenState(
            themeMode = s.themeMode,
            accent = s.accent,
            dynamicColor = s.dynamicColor,
            locale = s.locale,
            defaultMinSdk = s.defaultMinSdk,
            defaultTargetSdk = s.defaultTargetSdk,
            defaultOrientation = s.defaultOrientation,
            defaultProguard = s.defaultProguard,
            cacheBytes = bytes,
            cacheCleared = cleared
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsScreenState())

    init { refreshCacheSize() }

    fun setThemeMode(mode: ThemeMode) = shared.setThemeMode(mode)
    fun setAccent(preset: AccentPreset) = shared.setAccent(preset)
    fun setDynamicColor(enabled: Boolean) = shared.setDynamicColor(enabled)
    fun setLocale(locale: AppLocale) = shared.setLocale(locale)
    fun setDefaultMinSdk(v: Int) = shared.setDefaultMinSdk(v)
    fun setDefaultTargetSdk(v: Int) = shared.setDefaultTargetSdk(v)
    fun setDefaultOrientation(o: Orientation) = shared.setDefaultOrientation(o)
    fun setDefaultProguard(v: Boolean) = shared.setDefaultProguard(v)

    fun refreshCacheSize() = viewModelScope.launch {
        val bytes = withContext(Dispatchers.IO) {
            val cache = context.cacheDir
            if (cache.exists()) cache.walkBottomUp().filter { it.isFile }.sumOf { it.length() } else 0L
        }
        cacheBytes.value = bytes
    }

    fun clearCache() = viewModelScope.launch {
        withContext(Dispatchers.IO) {
            // Only clear the OS cache dir — NOT filesDir/generated which holds
            // the user's generated project sources that we'd otherwise orphan.
            val cache = context.cacheDir
            if (cache.exists()) cache.listFiles()?.forEach { it.deleteRecursively() }
        }
        cacheCleared.value = true
        refreshCacheSize()
    }

    fun consumeCacheCleared() {
        cacheCleared.value = false
    }
}
