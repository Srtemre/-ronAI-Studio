package com.htmltoapk.studio.ui.editor

import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htmltoapk.studio.builder.ApkBuilder
import com.htmltoapk.studio.builder.BuildOutcome
import com.htmltoapk.studio.core.result.Result
import com.htmltoapk.studio.core.util.ValidationUtil
import com.htmltoapk.studio.data.model.AdvancedConfig
import com.htmltoapk.studio.data.model.Orientation
import com.htmltoapk.studio.data.model.PermissionsConfig
import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.data.model.SigningConfig
import com.htmltoapk.studio.data.model.SourceType
import com.htmltoapk.studio.data.model.WebViewConfig
import com.htmltoapk.studio.domain.repository.ProjectRepository
import com.htmltoapk.studio.domain.repository.SettingsRepository
import com.htmltoapk.studio.generator.ProjectGenerator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditorUiState(
    val id: Long = 0L,
    val appName: String = "",
    val packageName: String = "",
    val version: String = "1.0.0",
    val logoUri: String = "",
    val splashUri: String = "",
    val orientation: Orientation = Orientation.AUTO,
    val sourceType: SourceType = SourceType.HTML_FILE,
    val sourceUri: String = "",
    val pastedHtml: String = "",
    val websiteUrl: String = "",
    val expertMode: Boolean = false,
    val web: WebViewConfig = WebViewConfig(),
    val permissions: PermissionsConfig = PermissionsConfig(),
    val signing: SigningConfig = SigningConfig(),
    val advanced: AdvancedConfig = AdvancedConfig(),
    val createdAt: Long = System.currentTimeMillis(),
    val favorite: Boolean = false,
    val lastBuiltAt: Long = 0L,
    val generatedPath: String = "",
    val saveResult: Result<Long>? = null,
    val generationResult: Result<String>? = null,
    val buildResult: Result<String>? = null,
    @StringRes val validationErrorRes: Int? = null
) {
    fun toProject(): ProjectConfig = ProjectConfig(
        id = id,
        appName = appName,
        packageName = packageName,
        version = version,
        logoUri = logoUri,
        splashUri = splashUri,
        orientation = orientation,
        sourceType = sourceType,
        sourceUri = effectiveSourceUri(),
        expertMode = expertMode,
        web = web,
        permissions = permissions,
        signing = signing,
        advanced = advanced,
        createdAt = createdAt,
        favorite = favorite,
        lastBuiltAt = lastBuiltAt,
        generatedPath = generatedPath
    )

    fun effectiveSourceUri(): String = when (sourceType) {
        SourceType.PASTE_HTML -> pastedHtml
        SourceType.URL -> websiteUrl
        else -> sourceUri
    }
}

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val repo: ProjectRepository,
    private val settings: SettingsRepository,
    private val generator: ProjectGenerator,
    private val builder: ApkBuilder,
    savedState: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(EditorUiState())
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    init {
        val expert = savedState.get<Boolean>("expert") ?: false
        val projectId = savedState.get<Long>("projectId") ?: -1L
        _state.update { it.copy(expertMode = expert) }
        if (projectId > 0L) loadProject(projectId) else seedDefaults()
    }

    private fun seedDefaults() = viewModelScope.launch {
        val adv = settings.defaultAdvanced()
        _state.update { it.copy(advanced = adv) }
    }

    private fun loadProject(id: Long) = viewModelScope.launch {
        val project = repo.get(id) ?: return@launch
        _state.update {
            it.copy(
                id = project.id,
                appName = project.appName,
                packageName = project.packageName,
                version = project.version,
                logoUri = project.logoUri,
                splashUri = project.splashUri,
                orientation = project.orientation,
                sourceType = project.sourceType,
                sourceUri = if (project.sourceType in setOf(SourceType.PASTE_HTML, SourceType.URL)) "" else project.sourceUri,
                pastedHtml = if (project.sourceType == SourceType.PASTE_HTML) project.sourceUri else "",
                websiteUrl = if (project.sourceType == SourceType.URL) project.sourceUri else "",
                expertMode = project.expertMode,
                web = project.web,
                permissions = project.permissions,
                signing = project.signing,
                advanced = project.advanced,
                createdAt = project.createdAt,
                favorite = project.favorite,
                lastBuiltAt = project.lastBuiltAt,
                generatedPath = project.generatedPath
            )
        }
    }

    fun update(transform: (EditorUiState) -> EditorUiState) = _state.update(transform)

    fun setSourceUri(uri: Uri?) {
        if (uri != null) update { it.copy(sourceUri = uri.toString()) }
    }
    fun setLogoUri(uri: Uri?) { if (uri != null) update { it.copy(logoUri = uri.toString()) } }
    fun setSplashUri(uri: Uri?) { if (uri != null) update { it.copy(splashUri = uri.toString()) } }
    fun setKeystoreUri(uri: Uri?) {
        if (uri != null) update { it.copy(signing = it.signing.copy(keystoreUri = uri.toString())) }
    }

    fun save(onDone: (Long) -> Unit) {
        val current = _state.value
        val errorRes = ValidationUtil.validateProject(current.appName, current.packageName, current.effectiveSourceUri())
        if (errorRes != null) {
            _state.update { it.copy(validationErrorRes = resolveErrorRes(errorRes)) }
            return
        }
        _state.update { it.copy(validationErrorRes = null, saveResult = Result.Loading) }
        viewModelScope.launch {
            val id = repo.upsert(current.toProject())
            _state.update { it.copy(id = id, saveResult = Result.Success(id)) }
            onDone(id)
        }
    }

    fun generate() {
        val current = _state.value
        val errorRes = ValidationUtil.validateProject(current.appName, current.packageName, current.effectiveSourceUri())
        if (errorRes != null) {
            _state.update { it.copy(validationErrorRes = resolveErrorRes(errorRes)) }
            return
        }
        _state.update { it.copy(generationResult = Result.Loading) }
        viewModelScope.launch {
            // runCatching returns kotlin.Result<T>; convert it into our
            // domain Result<String> using explicit lambdas (NOT callable
            // references — `Result::Success` would require a type argument
            // for the generic outer `Result<T>` interface and fail to compile).
            val outcome: Result<String> = runCatching {
                val id = if (current.id == 0L) repo.upsert(current.toProject()) else current.id
                _state.update { it.copy(id = id) }
                val project = current.copy(id = id).toProject()
                val out = generator.generate(project)
                repo.markBuilt(id, out.absolutePath)
                out.absolutePath
            }.fold(
                onSuccess = { path: String -> Result.Success(path) },
                onFailure = { err: Throwable -> Result.Failure(err) }
            )
            _state.update { it.copy(generationResult = outcome) }
        }
    }

    fun buildApk() {
        val current = _state.value
        val errorRes = ValidationUtil.validateProject(current.appName, current.packageName, current.effectiveSourceUri())
        if (errorRes != null) {
            _state.update { it.copy(validationErrorRes = resolveErrorRes(errorRes)) }
            return
        }
        _state.update { it.copy(buildResult = Result.Loading) }
        viewModelScope.launch {
            // Same pattern as generate(): use explicit lambdas instead of
            // `Result::Success` / `Result::Failure` callable references so
            // the compiler doesn't demand a type argument for the outer
            // generic `Result<T>` interface.
            val outcome: Result<String> = runCatching {
                // Ensure project is persisted and sources are generated before building.
                val id = if (current.id == 0L) repo.upsert(current.toProject()) else current.id
                _state.update { it.copy(id = id) }
                val project = current.copy(id = id).toProject()
                val genDir = generator.generate(project)
                repo.markBuilt(id, genDir.absolutePath)
                val buildOutcome = builder.build(genDir, current.signing)
                when (buildOutcome) {
                    is BuildOutcome.Success -> buildOutcome.apk.absolutePath
                    is BuildOutcome.Unavailable -> error(buildOutcome.reason)
                    is BuildOutcome.Failure -> error(buildOutcome.error)
                }
            }.fold(
                onSuccess = { apkPath: String -> Result.Success(apkPath) },
                onFailure = { err: Throwable -> Result.Failure(err) }
            )
            _state.update { it.copy(buildResult = outcome) }
        }
    }

    fun consumeResults() {
        _state.update { it.copy(saveResult = null, generationResult = null, buildResult = null, validationErrorRes = null) }
    }

    private fun resolveErrorRes(name: String): Int = when (name) {
        "error_app_name_required" -> com.htmltoapk.studio.R.string.error_app_name_required
        "error_package_invalid" -> com.htmltoapk.studio.R.string.error_package_invalid
        "error_source_required" -> com.htmltoapk.studio.R.string.error_source_required
        else -> com.htmltoapk.studio.R.string.error_source_required
    }
}
