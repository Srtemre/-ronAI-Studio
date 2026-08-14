package com.example.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AccentColor
import com.example.domain.model.AppLanguage
import com.example.domain.model.AppSettings
import com.example.domain.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.InputStream

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val projectRepository: ProjectRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings

    private val _showClearDataConfirm = MutableStateFlow(false)
    val showClearDataConfirm: StateFlow<Boolean> = _showClearDataConfirm.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    fun setThemeMode(themeMode: ThemeMode) {
        settingsRepository.updateThemeMode(themeMode)
    }

    fun setAccentColor(accentColor: AccentColor) {
        settingsRepository.updateAccentColor(accentColor)
    }

    fun setLanguage(language: AppLanguage) {
        settingsRepository.updateLanguage(language)
    }

    fun promptClearAllData() {
        _showClearDataConfirm.value = true
    }

    fun dismissClearDataConfirm() {
        _showClearDataConfirm.value = false
    }

    fun confirmClearAllData(onCleared: (() -> Unit)? = null) {
        viewModelScope.launch {
            projectRepository.deleteAllProjects()
            _showClearDataConfirm.value = false
            _userMessage.value = "data_cleared_success"
            onCleared?.invoke()
        }
    }

    fun exportProjects(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val file = projectRepository.exportProjectsZip()
            if (file != null && file.exists()) {
                onFileReady(file)
            } else {
                _userMessage.value = "No projects to export."
            }
        }
    }

    fun importProjects(inputStream: InputStream, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val (count, error) = projectRepository.importProjectsFromZip(inputStream)
            if (error != null) {
                _userMessage.value = error
                onComplete?.invoke(false)
            } else {
                _userMessage.value = "$count ${if (settings.value.language == AppLanguage.TURKISH) "proje içe aktarıldı." else "projects imported successfully."}"
                onComplete?.invoke(true)
            }
        }
    }

    fun clearMessage() {
        _userMessage.value = null
    }
}

