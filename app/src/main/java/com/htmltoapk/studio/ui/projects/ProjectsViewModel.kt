package com.htmltoapk.studio.ui.projects

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.domain.repository.ProjectFilter
import com.htmltoapk.studio.domain.repository.ProjectRepository
import com.htmltoapk.studio.domain.repository.ProjectSort
import com.htmltoapk.studio.domain.repository.SortField
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ProjectsUiState(
    val filter: ProjectFilter = ProjectFilter.ALL,
    val sort: ProjectSort = ProjectSort(SortField.MODIFIED, ascending = false),
    val query: String = "",
    val projects: List<ProjectConfig> = emptyList()
)

@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val repo: ProjectRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val filter = MutableStateFlow(ProjectFilter.ALL)
    private val sort = MutableStateFlow(ProjectSort(SortField.MODIFIED, ascending = false))
    private val query = MutableStateFlow("")

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val state: StateFlow<ProjectsUiState> = combine(filter, sort, query) { f, s, q ->
        Triple(f, s, q)
    }.flatMapLatest { (f, s, q) ->
        repo.observe(f, s, q).map { list -> ProjectsUiState(f, s, q, list) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProjectsUiState())

    fun setFilter(f: ProjectFilter) { filter.value = f }
    fun setSort(s: ProjectSort) { sort.value = s }
    fun setQuery(q: String) { query.value = q }

    fun toggleFavorite(id: Long) = viewModelScope.launch { repo.toggleFavorite(id) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
    fun rename(id: Long, name: String) = viewModelScope.launch { repo.rename(id, name) }
    fun duplicate(id: Long) = viewModelScope.launch { repo.duplicate(id) }

    fun exportProject(id: Long, destUri: Uri, onDone: (Boolean) -> Unit) = viewModelScope.launch {
        val ok = withContext(Dispatchers.IO) {
            runCatching {
                val json = repo.export(id)
                val out = context.contentResolver.openOutputStream(destUri)
                    ?: error("Cannot open output stream for $destUri")
                out.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            }.isSuccess
        }
        onDone(ok)
    }

    fun importProjects(uri: Uri, onDone: (Int) -> Unit) = viewModelScope.launch {
        val count = withContext(Dispatchers.IO) {
            runCatching {
                val text = context.contentResolver.openInputStream(uri)?.use {
                    it.bufferedReader().readText()
                } ?: return@withContext 0
                repo.import(text)
            }.getOrDefault(0)
        }
        onDone(count)
    }
}
