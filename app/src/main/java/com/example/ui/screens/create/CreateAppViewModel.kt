package com.example.ui.screens.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProjectRepository
import com.example.domain.model.DisplayMode
import com.example.domain.model.Orientation
import com.example.domain.model.Project
import com.example.domain.model.SourceType
import com.example.util.ProjectFileManager
import com.example.util.SecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class CreateAppUiState(
    val name: String = "",
    val packageName: String = "com.app.myweb",
    val version: String = "1.0.0",
    val versionCode: Int = 1,
    val sourceType: SourceType = SourceType.HTML,
    val targetUrl: String = "https://",
    val htmlContent: String = "<!DOCTYPE html>\n<html>\n<head><title>My App</title></head>\n<body>\n  <h1>Welcome</h1>\n</body>\n</html>",
    val displayMode: DisplayMode = DisplayMode.STANDALONE,
    val orientation: Orientation = Orientation.AUTO,
    val enableJavaScript: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val enableOfflineCaching: Boolean = true,
    val iconColorHex: String = "#007AFF",
    val tempIconFile: File? = null,
    val existingIconFile: File? = null,
    val isIconRemoved: Boolean = false,
    val showAdvancedSettings: Boolean = false,
    val isEditing: Boolean = false,
    val editingProjectId: Long? = null,
    val isSavedSuccess: Boolean = false,
    val packageNameError: String? = null,
    val urlError: String? = null,
    val generalError: String? = null
)

class CreateAppViewModel(
    private val projectRepository: ProjectRepository,
    private val fileManager: ProjectFileManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateAppUiState())
    val uiState: StateFlow<CreateAppUiState> = _uiState.asStateFlow()

    fun initWithSourceType(sourceType: SourceType?) {
        if (sourceType != null) {
            _uiState.value = _uiState.value.copy(
                sourceType = sourceType,
                targetUrl = if (sourceType == SourceType.URL || sourceType == SourceType.PWA) "https://" else "index.html"
            )
        }
    }

    fun loadProjectForEdit(projectId: Long) {
        viewModelScope.launch {
            val project = projectRepository.getProjectById(projectId)
            if (project != null) {
                val iconFile = fileManager?.getProjectIconFile(projectId)
                _uiState.value = CreateAppUiState(
                    name = project.name,
                    packageName = project.packageName,
                    version = project.version,
                    versionCode = project.versionCode,
                    sourceType = project.sourceType,
                    targetUrl = project.targetUrl,
                    htmlContent = project.htmlContent,
                    displayMode = project.displayMode,
                    orientation = project.orientation,
                    enableJavaScript = project.enableJavaScript,
                    enableLocalStorage = project.enableLocalStorage,
                    enableOfflineCaching = project.enableOfflineCaching,
                    iconColorHex = project.iconColorHex,
                    existingIconFile = iconFile,
                    isEditing = true,
                    editingProjectId = project.id
                )
            }
        }
    }

    fun updateName(name: String) {
        val sanitized = name.lowercase().replace("[^a-z0-9]".toRegex(), "")
        val autoPkg = if (sanitized.isNotEmpty()) "com.app.$sanitized" else "com.app.myweb"
        val currentPkg = _uiState.value.packageName
        val newPkg = if (currentPkg.startsWith("com.app.") || currentPkg.isBlank()) autoPkg else currentPkg

        val pkgResult = SecurityValidator.validatePackageName(newPkg)
        _uiState.value = _uiState.value.copy(
            name = name,
            packageName = newPkg,
            packageNameError = if (pkgResult.isValid) null else pkgResult.errorMessage
        )
    }

    fun updatePackageName(pkg: String) {
        val validation = SecurityValidator.validatePackageName(pkg)
        _uiState.value = _uiState.value.copy(
            packageName = pkg,
            packageNameError = if (validation.isValid) null else validation.errorMessage
        )
    }

    fun updateVersion(ver: String) {
        _uiState.value = _uiState.value.copy(version = ver)
    }

    fun updateVersionCode(code: Int) {
        _uiState.value = _uiState.value.copy(versionCode = if (code > 0) code else 1)
    }

    fun toggleAdvancedSettings() {
        _uiState.value = _uiState.value.copy(showAdvancedSettings = !_uiState.value.showAdvancedSettings)
    }

    fun setTempIconFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (fileManager != null) {
                    context.contentResolver.openInputStream(uri)?.use { inStream ->
                        val tempFile = fileManager.saveTempIcon(inStream)
                        _uiState.value = _uiState.value.copy(
                            tempIconFile = tempFile,
                            isIconRemoved = false
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    generalError = "Could not load icon: ${e.localizedMessage}"
                )
            }
        }
    }

    fun removeIcon() {
        val temp = _uiState.value.tempIconFile
        if (temp != null && temp.exists()) {
            temp.delete()
        }
        _uiState.value = _uiState.value.copy(
            tempIconFile = null,
            isIconRemoved = true
        )
    }

    fun updateSourceType(type: SourceType) {
        val defaultTarget = when (type) {
            SourceType.URL, SourceType.PWA -> "https://"
            SourceType.HTML -> "index.html"
            SourceType.ZIP -> "app_bundle.zip"
            SourceType.HTML_TEXT -> ""
        }
        _uiState.value = _uiState.value.copy(
            sourceType = type,
            targetUrl = defaultTarget,
            urlError = null
        )
    }

    fun updateTargetUrl(url: String) {
        val error = if (_uiState.value.sourceType == SourceType.URL || _uiState.value.sourceType == SourceType.PWA) {
            val res = SecurityValidator.validateUrl(url)
            if (res.isValid) null else res.errorMessage
        } else null

        _uiState.value = _uiState.value.copy(
            targetUrl = url,
            urlError = error
        )
    }

    fun updateHtmlContent(code: String) {
        _uiState.value = _uiState.value.copy(htmlContent = code)
    }

    fun updateDisplayMode(mode: DisplayMode) {
        _uiState.value = _uiState.value.copy(displayMode = mode)
    }

    fun updateOrientation(orient: Orientation) {
        _uiState.value = _uiState.value.copy(orientation = orient)
    }

    fun updateEnableJavaScript(enable: Boolean) {
        _uiState.value = _uiState.value.copy(enableJavaScript = enable)
    }

    fun updateEnableLocalStorage(enable: Boolean) {
        _uiState.value = _uiState.value.copy(enableLocalStorage = enable)
    }

    fun updateEnableOfflineCaching(enable: Boolean) {
        _uiState.value = _uiState.value.copy(enableOfflineCaching = enable)
    }

    fun updateIconColor(colorHex: String) {
        val sanitizedColor = SecurityValidator.sanitizeHexColor(colorHex)
        _uiState.value = _uiState.value.copy(iconColorHex = sanitizedColor)
    }

    fun saveProject(onSaved: (Long) -> Unit) {
        val current = _uiState.value

        // Validate package name
        val pkgVal = SecurityValidator.validatePackageName(current.packageName)
        if (!pkgVal.isValid) {
            _uiState.value = _uiState.value.copy(packageNameError = pkgVal.errorMessage)
            return
        }

        // Validate URL if online source
        if (current.sourceType == SourceType.URL || current.sourceType == SourceType.PWA) {
            val urlVal = SecurityValidator.validateUrl(current.targetUrl)
            if (!urlVal.isValid) {
                _uiState.value = _uiState.value.copy(urlError = urlVal.errorMessage)
                return
            }
        }

        val nameToSave = current.name.trim().ifBlank { "My Web App" }
        val verToSave = current.version.trim().ifBlank { "1.0.0" }
        val projectToSave = Project(
            id = current.editingProjectId ?: 0,
            name = nameToSave,
            packageName = current.packageName.trim(),
            version = verToSave,
            versionCode = current.versionCode,
            sourceType = current.sourceType,
            targetUrl = current.targetUrl.trim(),
            htmlContent = current.htmlContent,
            displayMode = current.displayMode,
            orientation = current.orientation,
            enableJavaScript = current.enableJavaScript,
            enableLocalStorage = current.enableLocalStorage,
            enableOfflineCaching = current.enableOfflineCaching,
            iconColorHex = SecurityValidator.sanitizeHexColor(current.iconColorHex),
            lastModified = System.currentTimeMillis()
        )

        viewModelScope.launch {
            try {
                val savedId = projectRepository.saveProject(projectToSave)

                // Handle icon file operations
                if (fileManager != null) {
                    if (current.tempIconFile != null && current.tempIconFile.exists()) {
                        fileManager.copyTempIconToProject(current.tempIconFile, savedId)
                    } else if (current.isIconRemoved) {
                        fileManager.deleteProjectIcon(savedId)
                    }
                }

                _uiState.value = _uiState.value.copy(isSavedSuccess = true, generalError = null)
                onSaved(savedId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(generalError = "Failed to save project: ${e.localizedMessage}")
            }
        }
    }
}
