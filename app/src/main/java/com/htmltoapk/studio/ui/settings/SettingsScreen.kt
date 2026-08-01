package com.htmltoapk.studio.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness2
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htmltoapk.studio.BuildConfig
import com.htmltoapk.studio.R
import com.htmltoapk.studio.core.util.FileUtil
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.ui.components.ChoiceChips
import com.htmltoapk.studio.ui.components.RoundedCard
import com.htmltoapk.studio.ui.components.SectionHeader
import com.htmltoapk.studio.ui.components.ToggleRow
import com.htmltoapk.studio.ui.theme.AccentPreset
import com.htmltoapk.studio.ui.theme.AppLocale
import com.htmltoapk.studio.ui.theme.ThemeMode

@Composable
fun SettingsScreen(
    viewModel: SettingsScreenViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val cacheClearedMsg = stringResource(R.string.settings_cache_cleared)

    LaunchedEffect(state.cacheCleared) {
        if (state.cacheCleared) {
            snackbar.showSnackbar(cacheClearedMsg)
            viewModel.consumeCacheCleared()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        SettingsContent(
            state = state,
            onThemeMode = viewModel::setThemeMode,
            onDynamicColor = viewModel::setDynamicColor,
            onAccent = viewModel::setAccent,
            onLocale = viewModel::setLocale,
            onMinSdk = viewModel::setDefaultMinSdk,
            onTargetSdk = viewModel::setDefaultTargetSdk,
            onOrientation = viewModel::setDefaultOrientation,
            onProguard = viewModel::setDefaultProguard,
            onClearCache = viewModel::clearCache,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun SettingsContent(
    state: SettingsScreenState,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onAccent: (AccentPreset) -> Unit,
    onLocale: (AppLocale) -> Unit,
    onMinSdk: (Int) -> Unit,
    onTargetSdk: (Int) -> Unit,
    onOrientation: (Orientation) -> Unit,
    onProguard: (Boolean) -> Unit,
    onClearCache: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.nav_settings),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // Appearance
        SectionHeader(stringResource(R.string.settings_section_appearance), icon = Icons.Filled.Palette)
        RoundedCard {
            Text(stringResource(R.string.settings_theme), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(
                    ThemeMode.DARK to stringResource(R.string.theme_dark),
                    ThemeMode.LIGHT to stringResource(R.string.theme_light),
                    ThemeMode.SYSTEM to stringResource(R.string.theme_system)
                ),
                selected = state.themeMode,
                onSelect = onThemeMode
            )
            ToggleRow(
                title = stringResource(R.string.settings_dynamic_color),
                checked = state.dynamicColor,
                onCheckedChange = onDynamicColor
            )
            Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.labelLarge)
            AccentRow(state.accent, onAccent)
        }

        // Language
        SectionHeader(stringResource(R.string.settings_section_language), icon = Icons.Filled.Language)
        RoundedCard {
            ChoiceChips(
                options = listOf(
                    AppLocale.EN to stringResource(R.string.lang_english),
                    AppLocale.TR to stringResource(R.string.lang_turkish)
                ),
                selected = state.locale,
                onSelect = onLocale
            )
        }

        // Build defaults
        SectionHeader(stringResource(R.string.settings_section_build_defaults), icon = Icons.Filled.Tune)
        RoundedCard {
            Text(stringResource(R.string.settings_default_min_sdk), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(21, 23, 24, 26, 29).map { it to it.toString() },
                selected = state.defaultMinSdk,
                onSelect = onMinSdk
            )
            Text(stringResource(R.string.settings_default_target_sdk), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(33, 34).map { it to it.toString() },
                selected = state.defaultTargetSdk,
                onSelect = onTargetSdk
            )
            Text(stringResource(R.string.settings_default_orientation), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(
                    Orientation.AUTO to stringResource(R.string.orientation_auto),
                    Orientation.PORTRAIT to stringResource(R.string.orientation_portrait),
                    Orientation.LANDSCAPE to stringResource(R.string.orientation_landscape)
                ),
                selected = state.defaultOrientation,
                onSelect = onOrientation
            )
            ToggleRow(
                title = stringResource(R.string.settings_default_proguard),
                checked = state.defaultProguard,
                onCheckedChange = onProguard
            )
        }

        // Cache
        SectionHeader(stringResource(R.string.settings_section_cache), icon = Icons.Filled.Storage)
        RoundedCard {
            Text(stringResource(R.string.settings_cache_size), style = MaterialTheme.typography.labelLarge)
            Text(
                FileUtil.humanReadableSize(state.cacheBytes),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = onClearCache) {
                Text(stringResource(R.string.settings_clear_cache))
            }
        }

        // About
        SectionHeader(stringResource(R.string.settings_section_about), icon = Icons.Filled.Info)
        RoundedCard {
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                stringResource(R.string.settings_about_description),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "${stringResource(R.string.settings_about_version)}: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun AccentRow(selected: AccentPreset, onSelect: (AccentPreset) -> Unit) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AccentPreset.entries.forEach { preset ->
            val color = if (dark) preset.dark else preset.light
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
                    .clickable { onSelect(preset) },
                contentAlignment = Alignment.Center
            ) {
                if (preset == selected) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onPrimary)
                    )
                }
            }
        }
    }
}
