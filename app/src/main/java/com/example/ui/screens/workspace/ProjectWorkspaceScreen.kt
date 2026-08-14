package com.example.ui.screens.workspace

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Css
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Html
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Javascript
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppLanguage
import com.example.ui.components.IosButton
import com.example.ui.components.IosButtonStyle
import com.example.ui.components.IosGroupSection
import com.example.ui.components.IosListItem
import com.example.ui.components.IosSegmentedControl
import com.example.ui.components.IosSeparator
import com.example.ui.components.IosTextField
import com.example.ui.components.IosTopBar
import com.example.ui.components.SandboxedWebPreview
import com.example.util.ProjectFileItem
import com.example.util.ProjectFileManager
import com.example.util.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectWorkspaceScreen(
    viewModel: WorkspaceViewModel,
    fileManager: ProjectFileManager,
    language: AppLanguage,
    onBackClick: () -> Unit,
    onEditSettingsClick: (Long) -> Unit,
    onProjectDuplicated: (Long) -> Unit,
    onProjectDeleted: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // System File Import Launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importFileFromUri(uri)
        }
    }

    LaunchedEffect(state.importMessage) {
        state.importMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearImportMessage()
        }
    }

    val project = state.project

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("project_workspace_screen")
    ) {
        // Workspace Header
        IosTopBar(
            title = project?.name ?: Strings.get("nav_projects", language),
            onBackClick = onBackClick,
            trailingAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.selectTab(WorkspaceTab.PREVIEW) },
                        modifier = Modifier.testTag("btn_top_preview")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Preview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showOptionsMenu = true },
                            modifier = Modifier.testTag("btn_workspace_menu")
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showOptionsMenu,
                            onDismissRequest = { showOptionsMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(Strings.get("action_edit", language)) },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    project?.let { onEditSettingsClick(it.id) }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("duplicate_project", language)) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.duplicateProject { newId ->
                                        onProjectDuplicated(newId)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(Strings.get("action_delete", language), color = Color(0xFFFF3B30)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFFF3B30)) },
                                onClick = {
                                    showOptionsMenu = false
                                    viewModel.promptDeleteProject()
                                }
                            )
                        }
                    }
                }
            }
        )

        // Navigation Tab Selector
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            IosSegmentedControl(
                items = WorkspaceTab.entries,
                selectedItem = state.selectedTab,
                onItemSelected = { viewModel.selectTab(it) },
                itemLabel = {
                    when (it) {
                        WorkspaceTab.FILES -> "${Strings.get("tab_files", language)} (${state.fileList.size})"
                        WorkspaceTab.EDITOR -> Strings.get("tab_editor", language)
                        WorkspaceTab.PREVIEW -> Strings.get("tab_preview", language)
                        WorkspaceTab.BUILD -> Strings.get("tab_build", language)
                    }
                }
            )
        }

        // Tab Content Area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (state.selectedTab) {
                WorkspaceTab.FILES -> FilesTabView(
                    fileList = state.fileList,
                    language = language,
                    onFileClick = { relPath -> viewModel.openFileInEditor(relPath) },
                    onDeleteFile = { relPath -> viewModel.deleteFile(relPath) },
                    onImportClick = { filePickerLauncher.launch("*/*") },
                    onNewFileClick = { viewModel.openNewFileModal() }
                )

                WorkspaceTab.EDITOR -> EditorTabView(
                    activeFilePath = state.activeFilePath,
                    content = state.activeFileContent,
                    isDirty = state.isFileContentDirty,
                    availableFiles = state.fileList.filter { !it.isDirectory }.map { it.relativePath },
                    language = language,
                    onFileSelected = { viewModel.openFileInEditor(it) },
                    onContentChange = { viewModel.updateEditorContent(it) },
                    onSaveClick = { viewModel.saveActiveFile() }
                )

                WorkspaceTab.PREVIEW -> {
                    if (project != null) {
                        SandboxedWebPreview(
                            project = project,
                            fileManager = fileManager,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                WorkspaceTab.BUILD -> {
                    if (project != null) {
                        BuildTabView(
                            project = project,
                            keystoreConfig = state.keystoreConfig,
                            isBuilding = state.isBuildingApk,
                            progressState = state.buildProgress,
                            buildResult = state.buildResult,
                            language = language,
                            onKeystoreAliasChange = { viewModel.updateKeystoreAlias(it) },
                            onBuildClick = { viewModel.buildAndSignApk() },
                            onResetBuild = { viewModel.resetBuildState() },
                            onExportProject = { onFileReady ->
                                viewModel.exportAndroidProject(onFileReady)
                            }
                        )
                    }
                }
            }

            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }

    // New File Modal
    if (state.showNewFileModal) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissNewFileModal() },
            title = {
                Text(
                    text = Strings.get("create_new_file_title", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(Strings.get("create_new_file_desc", language))
                    Spacer(modifier = Modifier.height(10.dp))
                    IosTextField(
                        value = state.newFileNameInput,
                        onValueChange = { viewModel.updateNewFileNameInput(it) },
                        placeholder = "index.html, style.css, app.js",
                        testTag = "input_new_file_name"
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.createNewFile(state.newFileNameInput) },
                    modifier = Modifier.testTag("btn_confirm_new_file")
                ) {
                    Text(Strings.get("create", language), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissNewFileModal() }) {
                    Text(Strings.get("cancel", language))
                }
            }
        )
    }

    // Delete Confirmation Modal
    if (state.showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = {
                Text(
                    text = Strings.get("delete_confirm_title", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${Strings.get("delete_confirm_message", language)}\n\n\"${project?.name}\""
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteProject(onDeleted = onProjectDeleted) },
                    modifier = Modifier.testTag("btn_confirm_delete_project")
                ) {
                    Text(
                        text = Strings.get("action_delete", language),
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteConfirm() }) {
                    Text(text = Strings.get("cancel", language))
                }
            }
        )
    }
}

@Composable
private fun FilesTabView(
    fileList: List<ProjectFileItem>,
    language: AppLanguage,
    onFileClick: (String) -> Unit,
    onDeleteFile: (String) -> Unit,
    onImportClick: () -> Unit,
    onNewFileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(modifier = Modifier.weight(1f)) {
                IosButton(
                    text = Strings.get("import_file_zip", language),
                    onClick = onImportClick,
                    icon = Icons.Default.UploadFile,
                    style = IosButtonStyle.PRIMARY,
                    testTag = "btn_import_file_zip"
                )
            }

            Box(modifier = Modifier.weight(1f)) {
                IosButton(
                    text = Strings.get("new_file", language),
                    onClick = onNewFileClick,
                    icon = Icons.Default.Add,
                    style = IosButtonStyle.SECONDARY,
                    testTag = "btn_new_file"
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (fileList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Strings.get("no_files_message", language),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            IosGroupSection(title = "${Strings.get("project_storage", language)} (${fileList.size})") {
                val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                fileList.forEachIndexed { index, file ->
                    val fileIcon = when {
                        file.isDirectory -> Icons.Default.Folder
                        file.extension.equals("html", ignoreCase = true) -> Icons.Default.Html
                        file.extension.equals("css", ignoreCase = true) -> Icons.Default.Css
                        file.extension.equals("js", ignoreCase = true) -> Icons.Default.Javascript
                        file.extension.equals("zip", ignoreCase = true) -> Icons.Default.FolderZip
                        file.extension.lowercase() in listOf("png", "jpg", "jpeg", "webp", "gif", "svg") -> Icons.Default.Image
                        else -> Icons.Default.Description
                    }

                    val iconBg = when {
                        file.isDirectory -> MaterialTheme.colorScheme.primary
                        file.extension.equals("html", ignoreCase = true) -> Color(0xFF2563EB)
                        file.extension.equals("css", ignoreCase = true) -> Color(0xFF0284C7)
                        file.extension.equals("js", ignoreCase = true) -> Color(0xFFD97706)
                        file.extension.equals("zip", ignoreCase = true) -> Color(0xFF4F46E5)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }

                    val formattedSize = if (file.isDirectory) "Directory" else "${file.sizeBytes / 1024} KB"

                    IosListItem(
                        title = file.relativePath,
                        subtitle = "$formattedSize • ${dateFormat.format(Date(file.lastModified))}",
                        icon = fileIcon,
                        iconBackground = iconBg,
                        onClick = {
                            if (!file.isDirectory) {
                                onFileClick(file.relativePath)
                            }
                        },
                        trailingContent = {
                            if (!file.isDirectory && file.relativePath != "index.html") {
                                IconButton(
                                    onClick = { onDeleteFile(file.relativePath) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete file",
                                        tint = Color(0xFFFF3B30),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        testTag = "file_item_$index"
                    )

                    if (index < fileList.size - 1) {
                        IosSeparator(paddingStart = 58)
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorTabView(
    activeFilePath: String,
    content: String,
    isDirty: Boolean,
    availableFiles: List<String>,
    language: AppLanguage,
    onFileSelected: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onSaveClick: () -> Unit
) {
    var showFileSelector by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Active file header & selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                        .clickable { showFileSelector = true }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = activeFilePath,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                    if (isDirty) {
                        Text(
                            text = " • ${Strings.get("unsaved", language)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = Color(0xFFFF9500),
                                fontWeight = FontWeight.SemiBold
                            )
                        )
                    }
                }

                DropdownMenu(
                    expanded = showFileSelector,
                    onDismissRequest = { showFileSelector = false }
                ) {
                    availableFiles.forEach { filePath ->
                        DropdownMenuItem(
                            text = { Text(filePath) },
                            onClick = {
                                showFileSelector = false
                                onFileSelected(filePath)
                            }
                        )
                    }
                }
            }

            IosButton(
                text = if (isDirty) "${Strings.get("save_file", language)} *" else Strings.get("saved", language),
                onClick = onSaveClick,
                icon = Icons.Default.Save,
                style = if (isDirty) IosButtonStyle.PRIMARY else IosButtonStyle.SECONDARY,
                testTag = "btn_save_file_editor"
            )
        }

        // Monospace Source Code Field
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp)
        ) {
            IosTextField(
                value = content,
                onValueChange = onContentChange,
                singleLine = false,
                placeholder = "<!-- Enter your HTML code here -->",
                modifier = Modifier.fillMaxSize(),
                testTag = "textarea_source_code"
            )
        }
    }
}
