package com.example.ui.screens.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ProjectRepository
import com.example.domain.model.Project
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProjectsViewModel(
    private val projectRepository: ProjectRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedProject = MutableStateFlow<Project?>(null)
    val selectedProject: StateFlow<Project?> = _selectedProject.asStateFlow()

    private val _showDeleteConfirm = MutableStateFlow<Project?>(null)
    val showDeleteConfirm: StateFlow<Project?> = _showDeleteConfirm.asStateFlow()

    val filteredProjects: StateFlow<List<Project>> = combine(
        projectRepository.allProjects,
        _searchQuery
    ) { projects, query ->
        if (query.isBlank()) {
            projects
        } else {
            projects.filter {
                it.name.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true) ||
                it.sourceType.displayName.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectProjectForDetails(project: Project?) {
        _selectedProject.value = project
    }

    fun duplicateProject(project: Project, onDuplicated: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val newId = projectRepository.duplicateProject(project.id)
            if (newId != null) {
                _selectedProject.value = null
                onDuplicated(newId)
            }
        }
    }

    fun promptDeleteProject(project: Project?) {
        _showDeleteConfirm.value = project
    }

    fun confirmDeleteProject() {
        val project = _showDeleteConfirm.value ?: return
        viewModelScope.launch {
            projectRepository.deleteProject(project.id)
            if (_selectedProject.value?.id == project.id) {
                _selectedProject.value = null
            }
            _showDeleteConfirm.value = null
        }
    }

    fun dismissDeleteConfirm() {
        _showDeleteConfirm.value = null
    }
}
