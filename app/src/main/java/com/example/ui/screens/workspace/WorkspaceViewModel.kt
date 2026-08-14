package com.example.ui.screens.workspace

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.builder.ApkBuildEngine
import com.example.builder.BuildProgressState
import com.example.builder.BuildResult
import com.example.builder.KeystoreConfig
import com.example.data.repository.ProjectRepository
import com.example.domain.model.Project
import com.example.util.ProjectFileItem
import com.example.util.ProjectFileManager
import com.example.util.SecurityValidator
import com.example.util.ZipImportResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

enum class WorkspaceTab {
    FILES,
    EDITOR,
    PREVIEW,
    BUILD
}

data class WorkspaceUiState(
    val project: Project? = null,
    val selectedTab: WorkspaceTab = WorkspaceTab.FILES,
    val fileList: List<ProjectFileItem> = emptyList(),
    val activeFilePath: String = "index.html",
    val activeFileContent: String = "",
    val isFileContentDirty: Boolean = false,
    val showDeleteConfirm: Boolean = false,
    val showNewFileModal: Boolean = false,
    val newFileNameInput: String = "",
    val importMessage: String? = null,
    val isImporting: Boolean = false,
    val isBuildingApk: Boolean = false,
    val buildProgress: BuildProgressState? = null,
    val buildResult: BuildResult? = null,
    val keystoreConfig: KeystoreConfig = KeystoreConfig()
)

class WorkspaceViewModel(
    private val projectId: Long,
    private val projectRepository: ProjectRepository,
    private val fileManager: ProjectFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(WorkspaceUiState())
    val uiState: StateFlow<WorkspaceUiState> = _uiState.asStateFlow()

    init {
        loadProjectData()
    }

    fun loadProjectData() {
        viewModelScope.launch {
            try {
                val proj = projectRepository.getProjectById(projectId)
                if (proj != null) {
                    // Ensure default index.html exists if empty
                    val files = fileManager.listProjectFiles(projectId)
                    if (files.isEmpty() && proj.htmlContent.isNotBlank()) {
                        fileManager.saveTextFile(projectId, "index.html", proj.htmlContent)
                    } else if (files.isEmpty()) {
                        val defaultHtml = "<!DOCTYPE html>\n<html>\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>${proj.name}</title>\n  <style>\n    body { font-family: -apple-system, sans-serif; padding: 20px; background: #f7f9fc; color: #0f172a; }\n  </style>\n</head>\n<body>\n  <h1>${proj.name}</h1>\n  <p>Welcome to your HTML app project.</p>\n</body>\n</html>"
                        fileManager.saveTextFile(projectId, "index.html", defaultHtml)
                    }

                    val updatedFiles = fileManager.listProjectFiles(projectId)
                    val targetFile = if (updatedFiles.any { it.relativePath == "index.html" }) {
                        "index.html"
                    } else {
                        updatedFiles.firstOrNull { !it.isDirectory }?.relativePath ?: "index.html"
                    }

                    val content = fileManager.readTextFile(projectId, targetFile)

                    _uiState.value = _uiState.value.copy(
                        project = proj,
                        fileList = updatedFiles,
                        activeFilePath = targetFile,
                        activeFileContent = content,
                        isFileContentDirty = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    importMessage = "Failed to load project: ${e.localizedMessage}"
                )
            }
        }
    }

    fun selectTab(tab: WorkspaceTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == WorkspaceTab.FILES) {
            refreshFiles()
        }
    }

    fun refreshFiles() {
        try {
            val files = fileManager.listProjectFiles(projectId)
            _uiState.value = _uiState.value.copy(fileList = files)
        } catch (_: Exception) {}
    }

    fun openFileInEditor(relativePath: String) {
        try {
            val sanitized = SecurityValidator.sanitizeRelativePath(relativePath)
            val content = fileManager.readTextFile(projectId, sanitized)
            _uiState.value = _uiState.value.copy(
                activeFilePath = sanitized,
                activeFileContent = content,
                isFileContentDirty = false,
                selectedTab = WorkspaceTab.EDITOR
            )
        } catch (_: Exception) {}
    }

    fun updateEditorContent(newContent: String) {
        _uiState.value = _uiState.value.copy(
            activeFileContent = newContent,
            isFileContentDirty = true
        )
    }

    fun saveActiveFile() {
        val current = _uiState.value
        try {
            fileManager.saveTextFile(projectId, current.activeFilePath, current.activeFileContent)

            // If index.html, update project htmlContent in DB
            if (current.activeFilePath == "index.html" && current.project != null) {
                viewModelScope.launch {
                    val updatedProj = current.project.copy(
                        htmlContent = current.activeFileContent,
                        lastModified = System.currentTimeMillis()
                    )
                    projectRepository.saveProject(updatedProj)
                    _uiState.value = _uiState.value.copy(project = updatedProj)
                }
            }

            _uiState.value = _uiState.value.copy(
                isFileContentDirty = false,
                importMessage = "Saved ${current.activeFilePath}"
            )
            refreshFiles()
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                importMessage = "Error saving file: ${e.localizedMessage}"
            )
        }
    }

    fun createNewFile(fileName: String) {
        if (fileName.isBlank()) return
        val sanitized = SecurityValidator.sanitizeRelativePath(fileName.trim())
        if (sanitized.isBlank()) return

        val defaultContent = when {
            sanitized.endsWith(".html") || sanitized.endsWith(".htm") -> "<!DOCTYPE html>\n<html>\n<head>\n  <title>New Page</title>\n</head>\n<body>\n\n</body>\n</html>"
            sanitized.endsWith(".css") -> "/* CSS Stylesheet */\nbody {\n  margin: 0;\n}"
            sanitized.endsWith(".js") -> "// JavaScript Code\nconsole.log('App loaded');"
            sanitized.endsWith(".json") -> "{\n  \"name\": \"app\"\n}"
            else -> ""
        }

        try {
            fileManager.saveTextFile(projectId, sanitized, defaultContent)
            openFileInEditor(sanitized)
            refreshFiles()
            _uiState.value = _uiState.value.copy(showNewFileModal = false, newFileNameInput = "")
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                importMessage = "Failed to create file: ${e.localizedMessage}"
            )
        }
    }

    fun deleteFile(relativePath: String) {
        try {
            fileManager.deleteFile(projectId, relativePath)
            refreshFiles()
            if (_uiState.value.activeFilePath == relativePath) {
                val remaining = fileManager.listProjectFiles(projectId).firstOrNull { !it.isDirectory }
                if (remaining != null) {
                    openFileInEditor(remaining.relativePath)
                }
            }
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(importMessage = "Failed to delete: ${e.localizedMessage}")
        }
    }

    fun importFileFromUri(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true)
            try {
                val contentResolver = fileManager.context.contentResolver
                val mimeType = contentResolver.getType(uri) ?: ""
                val pathStr = uri.path?.lowercase() ?: ""

                if (mimeType.contains("zip") || pathStr.endsWith(".zip")) {
                    val inputStream = contentResolver.openInputStream(uri)
                    if (inputStream != null) {
                        val result: ZipImportResult = fileManager.importZip(projectId, inputStream)
                        refreshFiles()
                        if (result.success) {
                            if (result.entryHtmlPath != null) {
                                openFileInEditor(result.entryHtmlPath)
                            }
                            _uiState.value = _uiState.value.copy(
                                isImporting = false,
                                importMessage = "Successfully imported ZIP with ${result.importedFilesCount} files!"
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isImporting = false,
                                importMessage = result.errorMessage ?: "Failed to import ZIP"
                            )
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(
                            isImporting = false,
                            importMessage = "Cannot open input stream for selected file."
                        )
                    }
                } else {
                    val importedName = fileManager.importSingleFileFromUri(projectId, uri)
                    refreshFiles()
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        importMessage = if (importedName != null) "Imported $importedName" else "Failed to import file"
                    )
                    if (importedName != null && (importedName.endsWith(".html") || importedName.endsWith(".css") || importedName.endsWith(".js"))) {
                        openFileInEditor(importedName)
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isImporting = false,
                    importMessage = "Import failed: ${e.localizedMessage}"
                )
            }
        }
    }

    fun duplicateProject(onDuplicated: (Long) -> Unit) {
        viewModelScope.launch {
            val newId = projectRepository.duplicateProject(projectId)
            if (newId != null) {
                onDuplicated(newId)
            }
        }
    }

    fun promptDeleteProject() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = true)
    }

    fun dismissDeleteConfirm() {
        _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
    }

    fun confirmDeleteProject(onDeleted: () -> Unit) {
        viewModelScope.launch {
            projectRepository.deleteProject(projectId)
            _uiState.value = _uiState.value.copy(showDeleteConfirm = false)
            onDeleted()
        }
    }

    fun openNewFileModal() {
        _uiState.value = _uiState.value.copy(showNewFileModal = true, newFileNameInput = "")
    }

    fun dismissNewFileModal() {
        _uiState.value = _uiState.value.copy(showNewFileModal = false)
    }

    fun updateNewFileNameInput(text: String) {
        _uiState.value = _uiState.value.copy(newFileNameInput = text)
    }

    fun clearImportMessage() {
        _uiState.value = _uiState.value.copy(importMessage = null)
    }

    fun updateKeystoreAlias(alias: String) {
        val cleanAlias = alias.trim().replace("[^a-zA-Z0-9_-]".toRegex(), "").ifBlank { "releasekey" }
        val updatedConfig = _uiState.value.keystoreConfig.copy(alias = cleanAlias)
        _uiState.value = _uiState.value.copy(keystoreConfig = updatedConfig)
    }

    fun resetBuildState() {
        _uiState.value = _uiState.value.copy(
            isBuildingApk = false,
            buildProgress = null,
            buildResult = null
        )
    }

    fun buildAndSignApk() {
        val proj = _uiState.value.project ?: return
        val engine = ApkBuildEngine(fileManager.context, fileManager)

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBuildingApk = true,
                buildProgress = BuildProgressState(
                    currentStep = com.example.builder.BuildStep.PREPARING,
                    detailMessage = "Initializing build pipeline..."
                ),
                buildResult = null
            )

            val result = try {
                engine.buildAndSignApk(
                    project = proj,
                    keystoreConfig = _uiState.value.keystoreConfig,
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(buildProgress = progress)
                    }
                )
            } catch (e: Exception) {
                BuildResult(
                    isSuccess = false,
                    finalStep = com.example.builder.BuildStep.FAILED,
                    humanReadableError = "Build engine encountered an unexpected error: ${e.localizedMessage}",
                    technicalDetails = "${e.javaClass.simpleName}: ${e.message}"
                )
            }

            _uiState.value = _uiState.value.copy(
                isBuildingApk = false,
                buildResult = result
            )
        }
    }

    fun exportAndroidProject(onFileReady: (File) -> Unit) {
        val proj = _uiState.value.project ?: return
        val engine = ApkBuildEngine(fileManager.context, fileManager)
        viewModelScope.launch {
            try {
                val zipFile = engine.exportAndroidProjectZip(proj)
                onFileReady(zipFile)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    importMessage = "Export failed: ${e.localizedMessage}"
                )
            }
        }
    }
}
