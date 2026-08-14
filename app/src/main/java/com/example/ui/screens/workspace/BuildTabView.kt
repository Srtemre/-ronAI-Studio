package com.example.ui.screens.workspace

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Android
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.builder.BuildProgressState
import com.example.builder.BuildResult
import com.example.builder.BuildStep
import com.example.builder.KeystoreConfig
import com.example.domain.model.AppLanguage
import com.example.domain.model.Project
import com.example.ui.components.IosButton
import com.example.ui.components.IosButtonStyle
import com.example.ui.components.IosGroupSection
import com.example.ui.components.IosListItem
import com.example.ui.components.IosSeparator
import com.example.ui.components.IosTextField
import com.example.util.Strings
import java.io.File
import java.io.FileInputStream

@Composable
fun BuildTabView(
    project: Project,
    keystoreConfig: KeystoreConfig,
    isBuilding: Boolean,
    progressState: BuildProgressState?,
    buildResult: BuildResult?,
    language: AppLanguage = AppLanguage.ENGLISH,
    onKeystoreAliasChange: (String) -> Unit,
    onBuildClick: () -> Unit,
    onResetBuild: () -> Unit,
    onExportProject: ((File) -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var projectZipToExport by remember { mutableStateOf<File?>(null) }

    // SAF Launcher for Downloading final .apk
    val downloadApkLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")
    ) { destinationUri ->
        if (destinationUri != null && buildResult?.apkFile != null && buildResult.apkFile.exists()) {
            copyFileToUri(context, buildResult.apkFile, destinationUri)
            Toast.makeText(context, "APK saved to storage", Toast.LENGTH_SHORT).show()
        }
    }

    // SAF Launcher for Exporting Android Project .zip
    val exportProjectZipLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { destinationUri ->
        val file = projectZipToExport
        if (destinationUri != null && file != null && file.exists()) {
            copyFileToUri(context, file, destinationUri)
            Toast.makeText(context, "Android project exported successfully", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .testTag("build_tab_view")
    ) {
        when {
            // ===================================================================
            // 1. SUCCESS STATE: ONLY SHOWN WHEN REAL VALIDATED APK EXISTS
            // ===================================================================
            buildResult != null && buildResult.isSuccess && buildResult.apkFile != null && buildResult.apkFile.exists() -> {
                ApkReadySuccessView(
                    project = project,
                    buildResult = buildResult,
                    language = language,
                    onDownloadApk = {
                        val defaultName = buildResult.apkFile.name
                        downloadApkLauncher.launch(defaultName)
                    },
                    onBuildAgain = {
                        onResetBuild()
                        onBuildClick()
                    },
                    onExportAndroidProject = {
                        onExportProject { zipFile ->
                            projectZipToExport = zipFile
                            exportProjectZipLauncher.launch(zipFile.name)
                        }
                    },
                    onShareApk = {
                        shareApkFile(context, buildResult.apkFile)
                    }
                )
            }

            // ===================================================================
            // 2. FAILURE STATE: SHOWN WHEN BUILD OR VALIDATION FAILED
            // ===================================================================
            buildResult != null && !buildResult.isSuccess -> {
                BuildFailedView(
                    buildResult = buildResult,
                    language = language,
                    onRetryBuild = onBuildClick,
                    onReset = onResetBuild
                )
            }

            // ===================================================================
            // 3. BUILDING / ACTIVE PIPELINE STATE
            // ===================================================================
            isBuilding -> {
                ActiveBuildPipelineView(
                    projectName = project.name,
                    progressState = progressState,
                    language = language
                )
            }

            // ===================================================================
            // 4. IDLE STATE: READY TO BUILD
            // ===================================================================
            else -> {
                IdleBuildSetupView(
                    project = project,
                    keystoreConfig = keystoreConfig,
                    language = language,
                    onKeystoreAliasChange = onKeystoreAliasChange,
                    onBuildClick = onBuildClick
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

// =======================================================================
// SUCCESS VIEW (Only displayed when real validated APK exists)
// =======================================================================
@Composable
private fun ApkReadySuccessView(
    project: Project,
    buildResult: BuildResult,
    language: AppLanguage,
    onDownloadApk: () -> Unit,
    onBuildAgain: () -> Unit,
    onExportAndroidProject: () -> Unit,
    onShareApk: () -> Unit
) {
    val meta = buildResult.validationResult?.metadata
    val apkSize = meta?.fileSizeFormatted ?: "${(buildResult.apkFile?.length() ?: 0) / 1024} KB"

    IosGroupSection(title = Strings.get("build_completed", language)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Verified Status Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF34C759).copy(alpha = 0.12f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = Strings.get("apk_ready", language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF28A745)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // App Icon Presentation
            AppIconBadge(
                appName = project.name,
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App Name
            Text(
                text = project.name,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = Strings.get("apk_ready_desc", language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Information Details Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(14.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    IosDetailRow(label = Strings.get("package_name", language), value = project.packageName)
                    IosDetailRow(label = Strings.get("version_label", language), value = "${project.version} (Code ${project.versionCode})")
                    IosDetailRow(label = Strings.get("apk_size", language), value = apkSize)
                    IosDetailRow(label = Strings.get("signing_info", language), value = "RSA 2048-bit (SHA-256)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // PRIMARY ACTION: DOWNLOAD APK
            IosButton(
                text = Strings.get("download_apk", language),
                onClick = onDownloadApk,
                icon = Icons.Default.Download,
                style = IosButtonStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_download_apk"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // SECONDARY ACTION: BUILD AGAIN
            IosButton(
                text = Strings.get("build_again", language),
                onClick = onBuildAgain,
                icon = Icons.Default.Refresh,
                style = IosButtonStyle.SECONDARY,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_build_again"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // OPTIONAL ACTION: EXPORT ANDROID PROJECT
            IosButton(
                text = Strings.get("export_android_project", language),
                onClick = onExportAndroidProject,
                icon = Icons.Default.FolderZip,
                style = IosButtonStyle.OUTLINED,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_export_android_project"
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Share / Install
            IosButton(
                text = Strings.get("share_apk", language),
                onClick = onShareApk,
                icon = Icons.Default.Share,
                style = IosButtonStyle.OUTLINED,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_share_apk"
            )
        }
    }
}

// =======================================================================
// FAILURE VIEW
// =======================================================================
@Composable
private fun BuildFailedView(
    buildResult: BuildResult,
    language: AppLanguage,
    onRetryBuild: () -> Unit,
    onReset: () -> Unit
) {
    val context = LocalContext.current
    var isTechnicalDetailsExpanded by remember { mutableStateOf(false) }

    IosGroupSection(title = Strings.get("build_failed", language)) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Status Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFF3B30).copy(alpha = 0.12f))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = Color(0xFFFF3B30),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = Strings.get("build_failed", language),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF3B30)
                            )
                        )
                        Text(
                            text = "${Strings.get("phase", language)}: ${buildResult.finalStep.displayName}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Human-Readable Explanation
            Text(
                text = Strings.get("explanation", language),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = buildResult.humanReadableError ?: "The build encountered an error while processing the project.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Expandable Technical Details
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { isTechnicalDetailsExpanded = !isTechnicalDetailsExpanded }
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("technical_details", language),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = if (isTechnicalDetailsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isTechnicalDetailsExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E1E1E))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = Strings.get("build_logs_title", language),
                            style = MaterialTheme.typography.labelSmall.copy(color = Color(0xFFAAAAAA))
                        )
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy logs",
                            tint = Color(0xFF007AFF),
                            modifier = Modifier
                                .size(16.dp)
                                .clickable {
                                    val clip = ClipData.newPlainText("Build Logs", buildResult.buildLogs.joinToString("\n"))
                                    (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(clip)
                                    Toast.makeText(context, "Diagnostics copied to clipboard", Toast.LENGTH_SHORT).show()
                                }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (!buildResult.technicalDetails.isNullOrBlank()) {
                        Text(
                            text = buildResult.technicalDetails,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                color = Color(0xFFFF6B6B)
                            )
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    val allLogs = buildResult.buildLogs
                    if (allLogs.isNotEmpty()) {
                        allLogs.forEach { logLine ->
                            Text(
                                text = logLine,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 10.sp,
                                    color = if (logLine.contains("ERROR", true) || logLine.contains("FAILED", true)) Color(0xFFFF6B6B) else Color(0xFFE0E0E0)
                                )
                            )
                        }
                    } else {
                        Text(
                            text = "No diagnostic logs generated.",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Gray)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // RETRY BUTTON
            IosButton(
                text = Strings.get("retry_build", language),
                onClick = onRetryBuild,
                icon = Icons.Default.Refresh,
                style = IosButtonStyle.PRIMARY,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_retry_build"
            )

            Spacer(modifier = Modifier.height(8.dp))

            IosButton(
                text = Strings.get("reset", language),
                onClick = onReset,
                style = IosButtonStyle.OUTLINED,
                modifier = Modifier.fillMaxWidth(),
                testTag = "btn_reset_build"
            )
        }
    }
}

// =======================================================================
// ACTIVE BUILD PIPELINE VIEW (Real-time step tracker, no fake timers)
// =======================================================================
@Composable
private fun ActiveBuildPipelineView(
    projectName: String,
    progressState: BuildProgressState?,
    language: AppLanguage
) {
    val realSteps = listOf(
        BuildStep.PREPARING,
        BuildStep.VALIDATING,
        BuildStep.GENERATING_PROJECT,
        BuildStep.COMPILING,
        BuildStep.SIGNING,
        BuildStep.VALIDATING_APK
    )

    val currentStep = progressState?.currentStep ?: BuildStep.PREPARING
    val completedSteps = progressState?.completedSteps ?: emptySet()

    IosGroupSection(title = "${Strings.get("building_project", language)} $projectName") {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header active message
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.5.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = currentStep.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = progressState?.detailMessage ?: "Executing build pipeline...",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            IosSeparator()
            Spacer(modifier = Modifier.height(14.dp))

            // Step-by-Step real state indicators
            realSteps.forEachIndexed { index, step ->
                val isCompleted = completedSteps.contains(step)
                val isActive = currentStep == step && !isCompleted

                PipelineStepItem(
                    stepName = step.displayName,
                    isCompleted = isCompleted,
                    isActive = isActive
                )

                if (index < realSteps.lastIndex) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun PipelineStepItem(
    stepName: String,
    isCompleted: Boolean,
    isActive: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            isCompleted -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF34C759)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Completed",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            isActive -> {
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            else -> {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .border(1.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = stepName,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isActive) FontWeight.Bold else if (isCompleted) FontWeight.Medium else FontWeight.Normal,
                color = when {
                    isActive -> MaterialTheme.colorScheme.primary
                    isCompleted -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                }
            )
        )
    }
}

// =======================================================================
// IDLE BUILD SETUP VIEW
// =======================================================================
@Composable
private fun IdleBuildSetupView(
    project: Project,
    keystoreConfig: KeystoreConfig,
    language: AppLanguage,
    onKeystoreAliasChange: (String) -> Unit,
    onBuildClick: () -> Unit
) {
    // Project Summary Card
    IosGroupSection(title = Strings.get("release_target", language)) {
        Column(modifier = Modifier.padding(14.dp)) {
            IosListItem(
                title = project.name,
                subtitle = "${project.packageName} (v${project.version})",
                icon = Icons.Default.Android,
                iconBackground = MaterialTheme.colorScheme.primary
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Keystore Configuration Card
    IosGroupSection(title = Strings.get("keystore_config", language)) {
        Column(modifier = Modifier.padding(14.dp)) {
            IosListItem(
                title = Strings.get("managed_keystore", language),
                subtitle = Strings.get("managed_keystore_desc", language),
                icon = Icons.Default.Lock,
                iconBackground = Color(0xFF007AFF)
            )

            IosSeparator()

            Spacer(modifier = Modifier.height(12.dp))

            IosTextField(
                value = keystoreConfig.alias,
                onValueChange = onKeystoreAliasChange,
                label = Strings.get("keystore_alias", language),
                placeholder = "releasekey",
                testTag = "input_keystore_alias"
            )

            Spacer(modifier = Modifier.height(10.dp))

            IosTextField(
                value = keystoreConfig.getMaskedStorePassword(),
                onValueChange = {},
                label = Strings.get("keystore_password", language),
                placeholder = "••••••••••••",
                testTag = "input_keystore_pass"
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Pipeline Trigger Card
    IosGroupSection(title = Strings.get("build_and_sign_apk", language)) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = Strings.get("release_compilation_desc", language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            IosButton(
                text = Strings.get("build_and_sign_apk", language),
                onClick = onBuildClick,
                icon = Icons.Default.Android,
                style = IosButtonStyle.PRIMARY,
                testTag = "btn_build_apk"
            )
        }
    }
}

// =======================================================================
// HELPER COMPOSABLES & FUNCTIONS
// =======================================================================
@Composable
private fun AppIconBadge(
    appName: String,
    modifier: Modifier = Modifier
) {
    val initial = appName.trim().firstOrNull()?.uppercase() ?: "A"
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        Color(0xFF5856D6)
                    )
                )
            )
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initial,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        )
    }
}

@Composable
private fun IosDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun copyFileToUri(context: Context, sourceFile: File, destinationUri: Uri) {
    try {
        context.contentResolver.openOutputStream(destinationUri)?.use { outStream ->
            FileInputStream(sourceFile).use { inStream ->
                inStream.copyTo(outStream)
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun shareApkFile(context: Context, apkFile: File) {
    try {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/vnd.android.package-archive"
            putExtra(Intent.EXTRA_STREAM, contentUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share APK Binary"))
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
