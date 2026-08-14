package com.example.ui.screens.create

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.builder.ApkBuildEngine
import com.example.builder.BuildProgressState
import com.example.builder.BuildResult
import com.example.builder.BuildStep
import com.example.builder.KeystoreConfig
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
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream

enum class FastSourceType(val displayName: String) {
    HTML("HTML"),
    URL("Website URL"),
    ZIP("ZIP Archive")
}

enum class FastHtmlMode(val displayName: String) {
    CODE("Write Code"),
    FILE("Upload File")
}

data class FastCreateUiState(
    val name: String = "",
    val orientation: Orientation = Orientation.AUTO,
    val fastSourceType: FastSourceType = FastSourceType.HTML,
    val htmlMode: FastHtmlMode = FastHtmlMode.CODE,
    val htmlCode: String = "<!DOCTYPE html>\n<html>\n<head>\n  <meta charset=\"UTF-8\">\n  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n  <title>My Web App</title>\n  <style>\n    body { font-family: -apple-system, sans-serif; padding: 24px; text-align: center; background: #f8fafc; color: #1e293b; }\n    .card { background: white; border-radius: 16px; padding: 24px; box-shadow: 0 4px 12px rgba(0,0,0,0.05); margin-top: 20px; }\n    button { background: #007aff; color: white; border: none; padding: 12px 24px; border-radius: 10px; font-weight: 600; font-size: 16px; cursor: pointer; }\n  </style>\n</head>\n<body>\n  <div class=\"card\">\n    <h1>Welcome to My App</h1>\n    <p>This is a native Android application built with HTML App Builder.</p>\n    <button onclick=\"alert('Hello from your app!')\">Click Me</button>\n  </div>\n</body>\n</html>",
    val selectedHtmlFileName: String? = null,
    val selectedHtmlBytes: ByteArray? = null,
    val targetUrl: String = "https://",
    val selectedZipFileName: String? = null,
    val selectedZipBytes: ByteArray? = null,
    val iconColorHex: String = "#007AFF",
    val tempIconFile: File? = null,
    val isBuilding: Boolean = false,
    val buildProgress: BuildProgressState? = null,
    val buildResult: BuildResult? = null,
    val savedProjectId: Long? = null,
    val urlError: String? = null,
    val generalError: String? = null
)

class FastCreateViewModel(
    private val projectRepository: ProjectRepository,
    private val fileManager: ProjectFileManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(FastCreateUiState())
    val uiState: StateFlow<FastCreateUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(name = name)
    }

    fun updateOrientation(orientation: Orientation) {
        _uiState.value = _uiState.value.copy(orientation = orientation)
    }

    fun updateFastSourceType(sourceType: FastSourceType) {
        _uiState.value = _uiState.value.copy(
            fastSourceType = sourceType,
            urlError = null,
            generalError = null
        )
    }

    fun updateHtmlMode(mode: FastHtmlMode) {
        _uiState.value = _uiState.value.copy(htmlMode = mode)
    }

    fun updateHtmlCode(code: String) {
        _uiState.value = _uiState.value.copy(htmlCode = code)
    }

    fun updateTargetUrl(url: String) {
        val error = if (url.isNotBlank() && url != "https://" && url != "http://") {
            val res = SecurityValidator.validateUrl(url)
            if (res.isValid) null else res.errorMessage
        } else null

        _uiState.value = _uiState.value.copy(targetUrl = url, urlError = error)
    }

    fun updateIconColor(colorHex: String) {
        val sanitized = SecurityValidator.sanitizeHexColor(colorHex)
        _uiState.value = _uiState.value.copy(iconColorHex = sanitized)
    }

    fun setTempIconFromUri(context: Context, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inStream ->
                    val tempFile = fileManager.saveTempIcon(inStream)
                    _uiState.value = _uiState.value.copy(tempIconFile = tempFile)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    generalError = "Could not load selected icon: ${e.localizedMessage}"
                )
            }
        }
    }

    fun removeIcon() {
        val oldFile = _uiState.value.tempIconFile
        if (oldFile != null && oldFile.exists()) {
            oldFile.delete()
        }
        _uiState.value = _uiState.value.copy(tempIconFile = null)
    }

    fun setHtmlFile(fileName: String, bytes: ByteArray) {
        _uiState.value = _uiState.value.copy(
            selectedHtmlFileName = fileName,
            selectedHtmlBytes = bytes,
            generalError = null
        )
    }

    fun setZipFile(fileName: String, bytes: ByteArray) {
        _uiState.value = _uiState.value.copy(
            selectedZipFileName = fileName,
            selectedZipBytes = bytes,
            generalError = null
        )
    }

    fun dismissBuildResult() {
        _uiState.value = _uiState.value.copy(
            isBuilding = false,
            buildProgress = null,
            buildResult = null
        )
    }

    private fun derivePackageName(name: String): String {
        val clean = name.lowercase().replace("[^a-z0-9]".toRegex(), "")
        return if (clean.isNotBlank()) "com.app.$clean" else "com.app.fastweb"
    }

    private suspend fun saveProjectInternal(): Long = withContext(Dispatchers.IO) {
        val current = _uiState.value
        val appName = current.name.trim().ifBlank { "My Web App" }
        val packageName = derivePackageName(appName)
        val now = System.currentTimeMillis()

        val domainSourceType = when (current.fastSourceType) {
            FastSourceType.HTML -> if (current.htmlMode == FastHtmlMode.CODE) SourceType.HTML_TEXT else SourceType.HTML
            FastSourceType.URL -> SourceType.URL
            FastSourceType.ZIP -> SourceType.ZIP
        }

        val project = Project(
            id = current.savedProjectId ?: 0L,
            name = appName,
            packageName = packageName,
            version = "1.0.0",
            versionCode = 1,
            sourceType = domainSourceType,
            targetUrl = if (current.fastSourceType == FastSourceType.URL) current.targetUrl.trim() else "index.html",
            htmlContent = if (current.fastSourceType == FastSourceType.HTML && current.htmlMode == FastHtmlMode.CODE) current.htmlCode else "",
            displayMode = DisplayMode.STANDALONE,
            orientation = current.orientation,
            enableJavaScript = true,
            enableLocalStorage = true,
            enableOfflineCaching = true,
            iconColorHex = current.iconColorHex,
            createdDate = now,
            lastModified = now
        )

        val projectId = projectRepository.saveProject(project)

        // Copy icon if provided
        if (current.tempIconFile != null && current.tempIconFile.exists()) {
            fileManager.copyTempIconToProject(current.tempIconFile, projectId)
        }

        // Handle source file content
        when (current.fastSourceType) {
            FastSourceType.HTML -> {
                if (current.htmlMode == FastHtmlMode.FILE && current.selectedHtmlBytes != null) {
                    fileManager.saveBinaryFileBytes(projectId, "index.html", current.selectedHtmlBytes)
                } else if (current.htmlMode == FastHtmlMode.CODE && current.htmlCode.isNotBlank()) {
                    fileManager.saveTextFile(projectId, "index.html", current.htmlCode)
                }
            }
            FastSourceType.ZIP -> {
                if (current.selectedZipBytes != null) {
                    fileManager.importZip(projectId, ByteArrayInputStream(current.selectedZipBytes))
                }
            }
            FastSourceType.URL -> {
                // Online URL project
            }
        }

        _uiState.value = _uiState.value.copy(savedProjectId = projectId)
        projectId
    }

    fun saveAndOpenWorkspace(onSuccess: (Long) -> Unit) {
        val current = _uiState.value
        if (current.fastSourceType == FastSourceType.URL) {
            val urlVal = SecurityValidator.validateUrl(current.targetUrl)
            if (!urlVal.isValid) {
                _uiState.value = _uiState.value.copy(urlError = urlVal.errorMessage)
                return
            }
        }

        viewModelScope.launch {
            try {
                val id = saveProjectInternal()
                onSuccess(id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    generalError = "Failed to save project: ${e.localizedMessage}"
                )
            }
        }
    }

    fun buildApk(onBuildFinished: (BuildResult) -> Unit = {}) {
        val current = _uiState.value

        // Validate URL if URL mode
        if (current.fastSourceType == FastSourceType.URL) {
            val urlVal = SecurityValidator.validateUrl(current.targetUrl)
            if (!urlVal.isValid) {
                _uiState.value = _uiState.value.copy(urlError = urlVal.errorMessage)
                return
            }
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isBuilding = true,
                buildProgress = BuildProgressState(
                    currentStep = BuildStep.PREPARING,
                    detailMessage = "Initializing fast build pipeline..."
                ),
                buildResult = null,
                generalError = null
            )

            try {
                val projectId = saveProjectInternal()
                val project = projectRepository.getProjectById(projectId) ?: throw IllegalStateException("Project could not be loaded")

                val engine = ApkBuildEngine(fileManager.context, fileManager)
                val result = engine.buildAndSignApk(
                    project = project,
                    keystoreConfig = KeystoreConfig(),
                    onProgress = { progress ->
                        _uiState.value = _uiState.value.copy(buildProgress = progress)
                    }
                )

                _uiState.value = _uiState.value.copy(
                    isBuilding = false,
                    buildResult = result
                )
                onBuildFinished(result)
            } catch (e: Exception) {
                val failResult = BuildResult(
                    isSuccess = false,
                    finalStep = BuildStep.FAILED,
                    humanReadableError = "Build failed: ${e.localizedMessage}",
                    technicalDetails = "${e.javaClass.simpleName}: ${e.message}"
                )
                _uiState.value = _uiState.value.copy(
                    isBuilding = false,
                    buildResult = failResult
                )
                onBuildFinished(failResult)
            }
        }
    }
}
