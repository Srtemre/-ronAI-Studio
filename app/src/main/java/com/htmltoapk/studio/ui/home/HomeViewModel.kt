package com.htmltoapk.studio.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.domain.repository.ProjectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class HomeUiState(
    val totalProjects: Int = 0,
    val favorites: Int = 0,
    val lastBuild: Long = 0L,
    val recent: List<ProjectConfig> = emptyList()
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repo: ProjectRepository
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        repo.observeCount(), repo.observeFavoriteCount(), repo.observeLastBuild(), repo.observeRecent()
    ) { total, favs, lastBuild, recent ->
        HomeUiState(total, favs, lastBuild ?: 0L, recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())
}
