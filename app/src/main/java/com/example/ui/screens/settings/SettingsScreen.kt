package com.example.ui.screens.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AccentColor
import com.example.domain.model.AppLanguage
import com.example.domain.model.ThemeMode
import com.example.ui.components.IosGroupSection
import com.example.ui.components.IosListItem
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.IosSeparator
import com.example.ui.components.IosTopBar
import com.example.util.Strings
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appSettings by viewModel.settings.collectAsStateWithLifecycle()
    val showClearConfirm by viewModel.showClearDataConfirm.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()

    val currentLang = appSettings.language

    var pendingExportFile by remember { mutableStateOf<File?>(null) }

    // Launcher for saving exported zip file
    val exportFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri: Uri? ->
        if (uri != null && pendingExportFile != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    pendingExportFile!!.inputStream().use { input ->
                        input.copyTo(out)
                    }
                }
                Toast.makeText(context, Strings.get("projects_exported_success", currentLang), Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Export error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher for importing backup zip file
    val importFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                if (inputStream != null) {
                    viewModel.importProjects(inputStream) { success ->
                        if (success) {
                            Toast.makeText(context, Strings.get("projects_imported_success", currentLang), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    LaunchedEffect(userMessage) {
        userMessage?.let { msg ->
            val display = if (msg == "data_cleared_success") {
                Strings.get("data_cleared_success", currentLang)
            } else {
                msg
            }
            Toast.makeText(context, display, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen")
    ) {
        IosTopBar(
            title = Strings.get("nav_settings", currentLang),
            largeTitle = true
        )

        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Theme Mode Section
            IosGroupSection(title = Strings.get("appearance", currentLang)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = Strings.get("theme", currentLang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    IosSegmentedControl(
                        items = ThemeMode.entries,
                        selectedItem = appSettings.themeMode,
                        onItemSelected = { viewModel.setThemeMode(it) },
                        itemLabel = {
                            when (it) {
                                ThemeMode.SYSTEM -> Strings.get("theme_system", currentLang)
                                ThemeMode.LIGHT -> Strings.get("theme_light", currentLang)
                                ThemeMode.DARK -> Strings.get("theme_dark", currentLang)
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = Strings.get("accent_colors", currentLang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    // Accent Colors Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AccentColor.entries.forEach { accent ->
                            val isSelected = appSettings.accentColor == accent
                            val color = Color(accent.lightHex)

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { viewModel.setAccentColor(accent) }
                                    .testTag("accent_color_${accent.name.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Language Section
            IosGroupSection(title = Strings.get("language", currentLang)) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    IosSegmentedControl(
                        items = AppLanguage.entries,
                        selectedItem = appSettings.language,
                        onItemSelected = { viewModel.setLanguage(it) },
                        itemLabel = { it.displayName }
                    )
                }
            }

            // Data Management Section
            IosGroupSection(title = Strings.get("data_and_storage", currentLang)) {
                IosListItem(
                    title = Strings.get("export_projects", currentLang),
                    subtitle = Strings.get("export_projects_subtitle", currentLang),
                    icon = Icons.Default.Download,
                    iconBackground = MaterialTheme.colorScheme.primary,
                    onClick = {
                        viewModel.exportProjects { file ->
                            pendingExportFile = file
                            exportFileLauncher.launch("html_app_builder_backup.zip")
                        }
                    },
                    testTag = "item_export_projects"
                )

                IosSeparator()

                IosListItem(
                    title = Strings.get("import_projects", currentLang),
                    subtitle = Strings.get("import_projects_subtitle", currentLang),
                    icon = Icons.Default.Upload,
                    iconBackground = Color(0xFF34C759),
                    onClick = {
                        importFileLauncher.launch("application/zip")
                    },
                    testTag = "item_import_projects"
                )

                IosSeparator()

                IosListItem(
                    title = Strings.get("clear_project_data", currentLang),
                    subtitle = Strings.get("clear_project_data_subtitle", currentLang),
                    icon = Icons.Default.Delete,
                    iconBackground = Color(0xFFFF3B30),
                    onClick = { viewModel.promptClearAllData() },
                    testTag = "item_clear_all_data"
                )
            }

            // About Section
            IosGroupSection(title = Strings.get("about", currentLang)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 18.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = "App Logo",
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = Strings.get("app_title", currentLang),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = Strings.get("developed_by", currentLang),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Version 1.0.0",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = Strings.get("about_app_desc", currentLang),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Confirmation Dialog for Clear Project Data
    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissClearDataConfirm() },
            title = {
                Text(
                    text = Strings.get("clear_project_data", currentLang),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(text = Strings.get("clear_data_confirm", currentLang))
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmClearAllData() },
                    modifier = Modifier.testTag("btn_confirm_clear_data")
                ) {
                    Text(
                        text = Strings.get("action_delete", currentLang),
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissClearDataConfirm() },
                    modifier = Modifier.testTag("btn_cancel_clear_data")
                ) {
                    Text(text = Strings.get("cancel", currentLang))
                }
            }
        )
    }
}
