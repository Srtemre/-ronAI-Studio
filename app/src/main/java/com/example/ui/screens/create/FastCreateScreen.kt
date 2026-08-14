package com.example.ui.screens.create

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.builder.BuildStep
import com.example.domain.model.AppLanguage
import com.example.domain.model.Orientation
import com.example.ui.components.IosButton
import com.example.ui.components.IosButtonStyle
import com.example.ui.components.IosGroupSection
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.IosTextField
import com.example.ui.components.IosTopBar
import com.example.util.Strings
import java.io.File

@Composable
fun FastCreateScreen(
    viewModel: FastCreateViewModel,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onOpenWorkspace: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    val availableIconColors = listOf("#007AFF", "#34C759", "#FF9500", "#5856D6", "#FF3B30", "#1C1C1E")

    // Image Picker Launcher
    val iconPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.setTempIconFromUri(context, it) }
    }

    // HTML File Picker Launcher
    val htmlFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "index.html"
            val bytes = context.contentResolver.openInputStream(it)?.use { inStream -> inStream.readBytes() }
            if (bytes != null) {
                viewModel.setHtmlFile(fileName, bytes)
            }
        }
    }

    // ZIP File Picker Launcher
    val zipFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment?.substringAfterLast('/') ?: "project.zip"
            val bytes = context.contentResolver.openInputStream(it)?.use { inStream -> inStream.readBytes() }
            if (bytes != null) {
                viewModel.setZipFile(fileName, bytes)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("fast_create_screen")
    ) {
        IosTopBar(
            title = Strings.get("fast_create_title", language),
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

            // 1. App Name Section
            IosGroupSection(title = Strings.get("app_name_label", language)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    IosTextField(
                        value = state.name,
                        onValueChange = { viewModel.updateName(it) },
                        placeholder = Strings.get("app_name_placeholder", language),
                        testTag = "input_fast_app_name"
                    )
                }
            }

            // 2. App Icon Section
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
                                .testTag("fast_icon_preview"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (state.tempIconFile != null && state.tempIconFile!!.exists()) {
                                AsyncImage(
                                    model = state.tempIconFile,
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

                        // Actions: Upload / Remove
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IosButton(
                                text = if (state.tempIconFile != null) Strings.get("change_icon", language) else Strings.get("upload_icon", language),
                                onClick = { iconPickerLauncher.launch("image/*") },
                                icon = Icons.Default.Image,
                                style = IosButtonStyle.SECONDARY,
                                testTag = "btn_upload_fast_icon"
                            )

                            if (state.tempIconFile != null) {
                                IosButton(
                                    text = Strings.get("remove_icon", language),
                                    onClick = { viewModel.removeIcon() },
                                    icon = Icons.Default.Delete,
                                    style = IosButtonStyle.DESTRUCTIVE,
                                    testTag = "btn_remove_fast_icon"
                                )
                            }
                        }
                    }

                    // Fallback Color Palette
                    if (state.tempIconFile == null) {
                        Spacer(modifier = Modifier.height(14.dp))
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
            }

            // 3. Screen Orientation Section
            IosGroupSection(title = Strings.get("orientation", language)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    IosSegmentedControl(
                        items = Orientation.entries,
                        selectedItem = state.orientation,
                        onItemSelected = { viewModel.updateOrientation(it) },
                        itemLabel = {
                            when (it) {
                                Orientation.AUTO -> if (language == AppLanguage.TURKISH) "Otomatik" else "Auto"
                                Orientation.PORTRAIT -> if (language == AppLanguage.TURKISH) "Dikey" else "Portrait"
                                Orientation.LANDSCAPE -> if (language == AppLanguage.TURKISH) "Yatay" else "Landscape"
                            }
                        }
                    )
                }
            }

            // 4. Source Selector Section (HTML / URL / ZIP)
            IosGroupSection(title = Strings.get("source_type", language)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    IosSegmentedControl(
                        items = FastSourceType.entries,
                        selectedItem = state.fastSourceType,
                        onItemSelected = { viewModel.updateFastSourceType(it) },
                        itemLabel = { it.displayName }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Adaptive UI according to selected source
                    when (state.fastSourceType) {
                        FastSourceType.HTML -> {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                IosSegmentedControl(
                                    items = FastHtmlMode.entries,
                                    selectedItem = state.htmlMode,
                                    onItemSelected = { viewModel.updateHtmlMode(it) },
                                    itemLabel = {
                                        when (it) {
                                            FastHtmlMode.CODE -> Strings.get("html_mode_code", language)
                                            FastHtmlMode.FILE -> Strings.get("html_mode_file", language)
                                        }
                                    }
                                )

                                if (state.htmlMode == FastHtmlMode.CODE) {
                                    IosTextField(
                                        value = state.htmlCode,
                                        onValueChange = { viewModel.updateHtmlCode(it) },
                                        placeholder = Strings.get("html_code_placeholder", language),
                                        singleLine = false,
                                        minLines = 6,
                                        testTag = "fast_input_html_code"
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        IosButton(
                                            text = state.selectedHtmlFileName ?: Strings.get("select_html_file", language),
                                            onClick = { htmlFilePickerLauncher.launch("text/html") },
                                            icon = Icons.Default.FileUpload,
                                            style = IosButtonStyle.SECONDARY,
                                            testTag = "btn_select_html_file"
                                        )

                                        if (state.selectedHtmlFileName != null) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = Color(0xFF34C759),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${Strings.get("html_file_selected", language)}: ${state.selectedHtmlFileName}",
                                                    style = MaterialTheme.typography.bodySmall.copy(
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        fontSize = 12.sp
                                                    )
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        FastSourceType.URL -> {
                            Column {
                                IosTextField(
                                    value = state.targetUrl,
                                    onValueChange = { viewModel.updateTargetUrl(it) },
                                    placeholder = Strings.get("target_url_placeholder", language),
                                    testTag = "fast_input_target_url"
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
                        }

                        FastSourceType.ZIP -> {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                IosButton(
                                    text = state.selectedZipFileName ?: Strings.get("select_zip_file", language),
                                    onClick = { zipFilePickerLauncher.launch("application/zip") },
                                    icon = Icons.Default.FolderZip,
                                    style = IosButtonStyle.SECONDARY,
                                    testTag = "btn_select_zip_file"
                                )

                                if (state.selectedZipFileName != null) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF34C759),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${Strings.get("zip_file_selected", language)}: ${state.selectedZipFileName}",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                fontSize = 12.sp
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 5. Auto Configuration Notice
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = Strings.get("auto_config_title", language),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = Strings.get("auto_config_note", language),
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 6. Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Primary: BUILD APK
                IosButton(
                    text = Strings.get("fast_build_apk", language),
                    onClick = { viewModel.buildApk() },
                    icon = Icons.Default.PlayArrow,
                    style = IosButtonStyle.PRIMARY,
                    testTag = "btn_fast_build_apk"
                )

                // Secondary: Open in Workspace
                IosButton(
                    text = Strings.get("fast_open_workspace", language),
                    onClick = {
                        viewModel.saveAndOpenWorkspace { savedId ->
                            onOpenWorkspace(savedId)
                        }
                    },
                    icon = Icons.Default.Folder,
                    style = IosButtonStyle.SECONDARY,
                    testTag = "btn_fast_open_workspace"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }

    // Build Progress / Result Dialog
    if (state.isBuilding) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible while compiling */ },
            confirmButton = {},
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.5.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = Strings.get("build_in_progress", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = state.buildProgress?.currentStep?.displayName ?: "Compiling...",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = state.buildProgress?.detailMessage ?: Strings.get("building_apk_fast", language),
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        )
    } else if (state.buildResult != null) {
        val result = state.buildResult!!
        AlertDialog(
            onDismissRequest = { viewModel.dismissBuildResult() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.isSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = if (result.isSuccess) Color(0xFF34C759) else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (result.isSuccess) Strings.get("apk_ready", language) else Strings.get("build_failed", language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (result.isSuccess) {
                        Text(
                            text = Strings.get("apk_ready_desc", language),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        if (result.apkFile != null) {
                            Text(
                                text = "File: ${result.apkFile.name} (${result.apkFile.length() / 1024} KB)",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    } else {
                        Text(
                            text = result.humanReadableError ?: "An error occurred during build.",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.error
                            )
                        )
                    }
                }
            },
            confirmButton = {
                if (result.isSuccess && result.apkFile != null) {
                    IosButton(
                        text = Strings.get("share_apk", language),
                        onClick = {
                            val apkFile = result.apkFile
                            val apkUri = runCatching {
                                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
                            }.getOrNull() ?: Uri.fromFile(apkFile)

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/vnd.android.package-archive"
                                putExtra(Intent.EXTRA_STREAM, apkUri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share APK"))
                        },
                        icon = Icons.Default.Share,
                        style = IosButtonStyle.PRIMARY
                    )
                } else if (!result.isSuccess) {
                    IosButton(
                        text = Strings.get("retry_build", language),
                        onClick = { viewModel.buildApk() },
                        icon = Icons.Default.Refresh,
                        style = IosButtonStyle.PRIMARY
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        val savedId = state.savedProjectId
                        viewModel.dismissBuildResult()
                        if (result.isSuccess && savedId != null) {
                            onOpenWorkspace(savedId)
                        }
                    }
                ) {
                    Text(if (result.isSuccess) Strings.get("fast_open_workspace", language) else Strings.get("close", language))
                }
            }
        )
    }
}
