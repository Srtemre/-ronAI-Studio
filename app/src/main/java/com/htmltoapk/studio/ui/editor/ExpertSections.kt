package com.htmltoapk.studio.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.htmltoapk.studio.R
import com.htmltoapk.studio.data.model.CacheMode
import com.htmltoapk.studio.data.model.UserAgentMode
import com.htmltoapk.studio.ui.components.ChoiceChips
import com.htmltoapk.studio.ui.components.LabeledTextField
import com.htmltoapk.studio.ui.components.RoundedCard
import com.htmltoapk.studio.ui.components.SectionHeader
import com.htmltoapk.studio.ui.components.ToggleRow

@Composable
fun ExpertSections(viewModel: EditorViewModel, state: EditorUiState, enabled: Boolean = true) {
    PermissionsSection(viewModel, state, enabled)
    WebViewSection(viewModel, state, enabled)
    SigningSection(viewModel, state, enabled)
    AdvancedSection(viewModel, state, enabled)
}

@Composable
private fun PermissionsSection(viewModel: EditorViewModel, state: EditorUiState, enabled: Boolean) {
    SectionHeader(stringResource(R.string.editor_section_permissions))
    RoundedCard {
        ToggleRow(
            title = stringResource(R.string.perm_internet),
            checked = state.permissions.internet,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(internet = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.perm_network_state),
            checked = state.permissions.networkState,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(networkState = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.perm_storage),
            checked = state.permissions.storage,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(storage = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.perm_camera),
            checked = state.permissions.camera,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(camera = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.perm_microphone),
            checked = state.permissions.microphone,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(microphone = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.perm_location),
            checked = state.permissions.location,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(location = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.perm_notifications),
            checked = state.permissions.notifications,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(notifications = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.perm_vibrate),
            checked = state.permissions.vibrate,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(permissions = it.permissions.copy(vibrate = isChecked)) }
            }
        )
    }
}

@Composable
private fun WebViewSection(viewModel: EditorViewModel, state: EditorUiState, enabled: Boolean) {
    SectionHeader(stringResource(R.string.editor_section_webview))
    RoundedCard {
        ToggleRow(
            title = stringResource(R.string.webview_javascript),
            checked = state.web.javaScript,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(javaScript = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_dom_storage),
            checked = state.web.domStorage,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(domStorage = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_mixed_content),
            checked = state.web.mixedContent,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(mixedContent = isChecked)) }
            }
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.webview_cache_mode), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(
                    CacheMode.DEFAULT to stringResource(R.string.cache_default),
                    CacheMode.NO_CACHE to stringResource(R.string.cache_no_cache),
                    CacheMode.CACHE_ELSE_NETWORK to stringResource(R.string.cache_cache_else_network),
                    CacheMode.NETWORK_ELSE_CACHE to stringResource(R.string.cache_network_else_cache),
                    CacheMode.CACHE_ONLY to stringResource(R.string.cache_cache_only)
                ),
                selected = state.web.cacheMode,
                onSelect = { c -> viewModel.update { it.copy(web = it.web.copy(cacheMode = c)) } }
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.webview_user_agent), style = MaterialTheme.typography.labelLarge)
            ChoiceChips(
                options = listOf(
                    UserAgentMode.DEFAULT to stringResource(R.string.user_agent_default),
                    UserAgentMode.DESKTOP to stringResource(R.string.user_agent_desktop),
                    UserAgentMode.CUSTOM to stringResource(R.string.user_agent_custom)
                ),
                selected = state.web.userAgentMode,
                onSelect = { u -> viewModel.update { it.copy(web = it.web.copy(userAgentMode = u)) } }
            )
            if (state.web.userAgentMode == UserAgentMode.CUSTOM) {
                LabeledTextField(
                    label = stringResource(R.string.user_agent_custom),
                    value = state.web.customUserAgent,
                    onValueChange = { v -> viewModel.update { it.copy(web = it.web.copy(customUserAgent = v)) } },
                    placeholder = "Mozilla/5.0 (Linux; Android 14) …",
                    enabled = enabled
                )
            }
        }
        ToggleRow(
            title = stringResource(R.string.webview_fullscreen),
            checked = state.web.fullscreen,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(fullscreen = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_immersive),
            checked = state.web.immersive,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(immersive = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_keep_screen_on),
            checked = state.web.keepScreenOn,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(keepScreenOn = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_edge_to_edge),
            checked = state.web.edgeToEdge,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(edgeToEdge = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_adaptive_icons),
            checked = state.web.adaptiveIcons,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(adaptiveIcons = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_load_remote),
            checked = state.web.loadRemote,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(loadRemote = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_zoom),
            checked = state.web.zoom,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(zoom = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_media_playback),
            checked = state.web.mediaPlayback,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(mediaPlayback = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.webview_file_access),
            checked = state.web.fileAccess,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(web = it.web.copy(fileAccess = isChecked)) }
            }
        )
    }
}

@Composable
private fun SigningSection(viewModel: EditorViewModel, state: EditorUiState, enabled: Boolean) {
    SectionHeader(stringResource(R.string.editor_section_signing))
    RoundedCard {
        val keystoreLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri -> viewModel.setKeystoreUri(uri) }

        LabeledTextField(
            label = stringResource(R.string.signing_keystore),
            value = if (state.signing.keystoreUri.isBlank()) "(none — debug signing will be used)" else state.signing.keystoreUri,
            onValueChange = {},
            leadingIcon = Icons.Filled.Key,
            trailing = {
                TextButton(
                    onClick = { keystoreLauncher.launch(arrayOf("*/*")) },
                    enabled = enabled
                ) {
                    Text(stringResource(R.string.signing_pick_keystore))
                }
            },
            enabled = false
        )
        LabeledTextField(
            label = stringResource(R.string.signing_keystore_password),
            value = state.signing.keystorePassword,
            onValueChange = { v -> viewModel.update { it.copy(signing = it.signing.copy(keystorePassword = v)) } },
            visualTransformation = PasswordVisualTransformation(),
            enabled = enabled
        )
        LabeledTextField(
            label = stringResource(R.string.signing_key_alias),
            value = state.signing.keyAlias,
            onValueChange = { v -> viewModel.update { it.copy(signing = it.signing.copy(keyAlias = v)) } },
            enabled = enabled
        )
        LabeledTextField(
            label = stringResource(R.string.signing_key_password),
            value = state.signing.keyPassword,
            onValueChange = { v -> viewModel.update { it.copy(signing = it.signing.copy(keyPassword = v)) } },
            visualTransformation = PasswordVisualTransformation(),
            enabled = enabled
        )
    }
}

@Composable
private fun AdvancedSection(viewModel: EditorViewModel, state: EditorUiState, enabled: Boolean) {
    SectionHeader(stringResource(R.string.editor_section_advanced))
    RoundedCard {
        LabeledTextField(
            label = stringResource(R.string.advanced_min_sdk),
            value = state.advanced.minSdk.toString(),
            onValueChange = { v ->
                val n = v.toIntOrNull() ?: return@LabeledTextField
                viewModel.update { it.copy(advanced = it.advanced.copy(minSdk = n.coerceIn(21, 34))) }
            },
            keyboardType = KeyboardType.Number,
            enabled = enabled
        )
        LabeledTextField(
            label = stringResource(R.string.advanced_target_sdk),
            value = state.advanced.targetSdk.toString(),
            onValueChange = { v ->
                val n = v.toIntOrNull() ?: return@LabeledTextField
                viewModel.update { it.copy(advanced = it.advanced.copy(targetSdk = n.coerceIn(21, 34))) }
            },
            keyboardType = KeyboardType.Number,
            enabled = enabled
        )
        LabeledTextField(
            label = stringResource(R.string.advanced_status_bar_color),
            value = state.advanced.statusBarColor,
            onValueChange = { v -> viewModel.update { it.copy(advanced = it.advanced.copy(statusBarColor = v)) } },
            enabled = enabled
        )
        LabeledTextField(
            label = stringResource(R.string.advanced_nav_bar_color),
            value = state.advanced.navBarColor,
            onValueChange = { v -> viewModel.update { it.copy(advanced = it.advanced.copy(navBarColor = v)) } },
            enabled = enabled
        )
        ToggleRow(
            title = stringResource(R.string.advanced_enable_pull_refresh),
            checked = state.advanced.pullToRefresh,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(advanced = it.advanced.copy(pullToRefresh = isChecked)) }
            }
        )
        LabeledTextField(
            label = stringResource(R.string.advanced_offline_fallback),
            value = state.advanced.offlineFallback,
            onValueChange = { v -> viewModel.update { it.copy(advanced = it.advanced.copy(offlineFallback = v)) } },
            placeholder = "offline.html",
            enabled = enabled
        )
        ToggleRow(
            title = stringResource(R.string.advanced_enable_deep_links),
            checked = state.advanced.deepLinks,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(advanced = it.advanced.copy(deepLinks = isChecked)) }
            }
        )
        ToggleRow(
            title = stringResource(R.string.advanced_proguard),
            checked = state.advanced.proguard,
            enabled = enabled,
            onCheckedChange = { isChecked: Boolean ->
                viewModel.update { it.copy(advanced = it.advanced.copy(proguard = isChecked)) }
            }
        )
        LabeledTextField(
            label = stringResource(R.string.advanced_custom_headers),
            value = state.advanced.customHeadersJson,
            onValueChange = { v -> viewModel.update { it.copy(advanced = it.advanced.copy(customHeadersJson = v)) } },
            placeholder = "{\"X-Custom\":\"value\"}",
            singleLine = false,
            enabled = enabled
        )
    }
}
