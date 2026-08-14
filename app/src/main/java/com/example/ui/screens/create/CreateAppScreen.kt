package com.example.ui.screens.create

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.domain.model.AppLanguage
import com.example.domain.model.DisplayMode
import com.example.domain.model.Orientation
import com.example.domain.model.SourceType
import com.example.ui.components.IosButton
import com.example.ui.components.IosButtonStyle
import com.example.ui.components.IosGroupSection
import com.example.ui.components.IosListItem
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.IosSeparator
import com.example.ui.components.IosSwitch
import com.example.ui.components.IosTextField
import com.example.ui.components.IosTopBar
import com.example.util.Strings

@Composable
fun CreateAppScreen(
    viewModel: CreateAppViewModel,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onProjectSaved: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val availableIconColors = listOf("#007AFF", "#34C759", "#FF9500", "#5856D6", "#FF3B30", "#1C1C1E")

    // Image Picker Launcher for App Icon
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setTempIconFromUri(context, it) }
    }

    val activeIconFile = state.tempIconFile?.takeIf { it.exists() }
        ?: if (!state.isIconRemoved) state.existingIconFile?.takeIf { it.exists() } else null

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("create_app_screen")
    ) {
        IosTopBar(
            title = if (state.isEditing) Strings.get("action_edit", language) else Strings.get("title_create_app", language),
            onBackClick = onBackClick
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // General Error Banner if present
            if (state.generalError != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.generalError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            )
                        )
                    }
                }
            }

            // 1. Source Type Selector
            IosGroupSection(title = Strings.get("source_type", language)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    IosSegmentedControl(
                        items = SourceType.entries,
                        selectedItem = state.sourceType,
                        onItemSelected = { viewModel.updateSourceType(it) },
                        itemLabel = { it.displayName }
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = state.sourceType.description,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }

            // 2. Source Input Section
            IosGroupSection(
                title = when (state.sourceType) {
                    SourceType.URL, SourceType.PWA -> Strings.get("target_url_label", language)
                    SourceType.HTML -> Strings.get("entry_file_label", language)
                    SourceType.ZIP -> "ZIP Asset File"
                    SourceType.HTML_TEXT -> Strings.get("html_code_label", language)
                }
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    when (state.sourceType) {
                        SourceType.URL, SourceType.PWA -> {
                            IosTextField(
                                value = state.targetUrl,
                                onValueChange = { viewModel.updateTargetUrl(it) },
                                placeholder = Strings.get("target_url_placeholder", language),
                                testTag = "input_target_url"
                            )
                            if (state.urlError != null) {
                                Text(
                                    text = state.urlError ?: "",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp
                                    ),
                                    modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                                )
                            }
                        }
                        SourceType.HTML, SourceType.ZIP -> {
                            IosTextField(
                                value = state.targetUrl,
                                onValueChange = { viewModel.updateTargetUrl(it) },
                                placeholder = Strings.get("entry_file_placeholder", language),
                                testTag = "input_entry_file"
                            )
                        }
                        SourceType.HTML_TEXT -> {
                            IosTextField(
                                value = state.htmlContent,
                                onValueChange = { viewModel.updateHtmlContent(it) },
                                placeholder = Strings.get("html_code_placeholder", language),
                                singleLine = false,
                                minLines = 5,
                                testTag = "input_html_code"
                            )
                        }
                    }
                }
            }

            // 3. App Metadata Section
            IosGroupSection(title = Strings.get("app_metadata", language)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    IosTextField(
                        value = state.name,
                        onValueChange = { viewModel.updateName(it) },
                        label = Strings.get("app_name_label", language),
                        placeholder = Strings.get("app_name_placeholder", language),
                        testTag = "input_app_name"
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    IosTextField(
                        value = state.packageName,
                        onValueChange = { viewModel.updatePackageName(it) },
                        label = Strings.get("package_name_label", language),
                        placeholder = Strings.get("package_name_placeholder", language),
                        testTag = "input_package_name"
                    )
                    if (state.packageNameError != null) {
                        Text(
                            text = state.packageNameError ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.error,
                                fontSize = 11.sp
                            ),
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    IosTextField(
                        value = state.version,
                        onValueChange = { viewModel.updateVersion(it) },
                        label = Strings.get("version_label", language),
                        placeholder = "1.0.0",
                        testTag = "input_app_version"
                    )
                }
            }

            // 4. App Icon Section (Upload / Preview / Replace / Remove)
            IosGroupSection(title = Strings.get("app_icon_title", language)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Icon Preview
                        val parsedColor = runCatching { android.graphics.Color.parseColor(state.iconColorHex) }
                            .getOrDefault(android.graphics.Color.parseColor("#007AFF"))

                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(parsedColor))
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                    RoundedCornerShape(14.dp)
                                )
                                .testTag("expert_icon_preview"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (activeIconFile != null) {
                                AsyncImage(
                                    model = activeIconFile,
                                    contentDescription = Strings.get("icon_preview", language),
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val firstLetter = state.name.trim().take(1).uppercase().ifBlank { "A" }
                                Text(
                                    text = firstLetter,
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Upload / Remove Actions
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IosButton(
                                text = if (activeIconFile != null) Strings.get("change_icon", language) else Strings.get("upload_icon", language),
                                onClick = { iconPickerLauncher.launch("image/*") },
                                icon = Icons.Default.Image,
                                style = IosButtonStyle.SECONDARY,
                                testTag = "btn_upload_expert_icon"
                            )

                            if (activeIconFile != null) {
                                IosButton(
                                    text = Strings.get("remove_icon", language),
                                    onClick = { viewModel.removeIcon() },
                                    icon = Icons.Default.Delete,
                                    style = IosButtonStyle.DESTRUCTIVE,
                                    testTag = "btn_remove_expert_icon"
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Color Palette Selector
                    Text(
                        text = Strings.get("accent_color", language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        availableIconColors.forEach { hex ->
                            val colorInt = android.graphics.Color.parseColor(hex)
                            val isSelected = state.iconColorHex.equals(hex, ignoreCase = true)

                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(colorInt))
                                    .then(
                                        if (isSelected) {
                                            Modifier.border(2.5.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                        } else Modifier
                                    )
                                    .clickable { viewModel.updateIconColor(hex) },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. Display & Behavior Section
            IosGroupSection(title = Strings.get("display_behavior", language)) {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    // Display Mode
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = Strings.get("display_mode", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        IosSegmentedControl(
                            items = DisplayMode.entries,
                            selectedItem = state.displayMode,
                            onItemSelected = { viewModel.updateDisplayMode(it) },
                            itemLabel = { it.label.split(" ").first() }
                        )
                    }

                    IosSeparator()

                    // Orientation
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                        Text(
                            text = Strings.get("orientation", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        IosSegmentedControl(
                            items = Orientation.entries,
                            selectedItem = state.orientation,
                            onItemSelected = { viewModel.updateOrientation(it) },
                            itemLabel = { it.label.split(" ").first() }
                        )
                    }

                    IosSeparator()

                    // Switches
                    IosListItem(
                        title = Strings.get("enable_javascript", language),
                        trailingContent = {
                            IosSwitch(
                                checked = state.enableJavaScript,
                                onCheckedChange = { viewModel.updateEnableJavaScript(it) },
                                testTag = "switch_javascript"
                            )
                        }
                    )

                    IosSeparator()

                    IosListItem(
                        title = Strings.get("enable_local_storage", language),
                        trailingContent = {
                            IosSwitch(
                                checked = state.enableLocalStorage,
                                onCheckedChange = { viewModel.updateEnableLocalStorage(it) },
                                testTag = "switch_local_storage"
                            )
                        }
                    )

                    IosSeparator()

                    IosListItem(
                        title = Strings.get("enable_offline_cache", language),
                        trailingContent = {
                            IosSwitch(
                                checked = state.enableOfflineCaching,
                                onCheckedChange = { viewModel.updateEnableOfflineCaching(it) },
                                testTag = "switch_offline_cache"
                            )
                        }
                    )
                }
            }

            // 6. Collapsible Advanced Section
            IosGroupSection {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleAdvancedSettings() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = Strings.get("advanced_options", language),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            )
                        }

                        Icon(
                            imageVector = if (state.showAdvancedSettings) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    AnimatedVisibility(
                        visible = state.showAdvancedSettings,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            IosSeparator()
                            Spacer(modifier = Modifier.height(10.dp))
                            IosTextField(
                                value = state.versionCode.toString(),
                                onValueChange = { input ->
                                    input.toIntOrNull()?.let { viewModel.updateVersionCode(it) }
                                },
                                label = Strings.get("version_code_label", language),
                                placeholder = "1",
                                testTag = "input_version_code"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }

            // 7. Save Action Button
            IosButton(
                text = Strings.get("save_project", language),
                onClick = {
                    viewModel.saveProject { savedId ->
                        onProjectSaved(savedId)
                    }
                },
                style = IosButtonStyle.PRIMARY,
                testTag = "btn_save_project"
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
