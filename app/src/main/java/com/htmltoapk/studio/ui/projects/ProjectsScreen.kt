package com.htmltoapk.studio.ui.projects

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htmltoapk.studio.R
import com.htmltoapk.studio.core.util.TimeUtil
import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.domain.repository.ProjectFilter
import com.htmltoapk.studio.domain.repository.ProjectSort
import com.htmltoapk.studio.domain.repository.SortField
import com.htmltoapk.studio.ui.components.ConfirmDialog
import com.htmltoapk.studio.ui.components.EmptyState
import com.htmltoapk.studio.ui.components.LogoTile
import com.htmltoapk.studio.ui.components.TextInputDialog

@Composable
fun ProjectsScreen(
    onOpenProject: (Long) -> Unit,
    onNewProject: (Boolean) -> Unit,
    viewModel: ProjectsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var sortMenuExpanded by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProjectConfig?>(null) }
    var deleteTarget by remember { mutableStateOf<ProjectConfig?>(null) }
    var exportTarget by remember { mutableStateOf<ProjectConfig?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        viewModel.importProjects(uri) { count ->
            // simple toast-less feedback; could be exposed via state
        }
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        val target = exportTarget
        if (uri != null && target != null) {
            viewModel.exportProject(target.id, uri) { /* ok */ }
        }
        exportTarget = null
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.projects_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }) {
                    Icon(Icons.Filled.DriveFileMove, contentDescription = stringResource(R.string.action_import))
                }
                IconButton(onClick = { onNewProject(false) }) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.action_create))
                }
            }
        }

        item {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.projects_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filter == ProjectFilter.ALL,
                    onClick = { viewModel.setFilter(ProjectFilter.ALL) },
                    label = { Text(stringResource(R.string.projects_filter_all)) }
                )
                FilterChip(
                    selected = state.filter == ProjectFilter.FAVORITES,
                    onClick = { viewModel.setFilter(ProjectFilter.FAVORITES) },
                    label = { Text(stringResource(R.string.projects_filter_favorites)) }
                )
                FilterChip(
                    selected = state.filter == ProjectFilter.RECENT,
                    onClick = { viewModel.setFilter(ProjectFilter.RECENT) },
                    label = { Text(stringResource(R.string.projects_filter_recent)) }
                )
                Spacer(Modifier.weight(1f))
                Box {
                    IconButton(onClick = { sortMenuExpanded = true }) {
                        Icon(Icons.Filled.Sort, contentDescription = stringResource(R.string.projects_sort))
                    }
                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = { sortMenuExpanded = false }) {
                        SortMenuItem(stringResource(R.string.sort_name_asc)) { viewModel.setSort(ProjectSort(SortField.NAME, true)) }
                        SortMenuItem(stringResource(R.string.sort_name_desc)) { viewModel.setSort(ProjectSort(SortField.NAME, false)) }
                        SortMenuItem(stringResource(R.string.sort_date_desc)) { viewModel.setSort(ProjectSort(SortField.MODIFIED, false)) }
                        SortMenuItem(stringResource(R.string.sort_date_asc)) { viewModel.setSort(ProjectSort(SortField.MODIFIED, true)) }
                    }
                }
            }
        }

        if (state.projects.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.projects_empty),
                    icon = Icons.Filled.Add
                )
            }
        } else {
            items(state.projects, key = { it.id }) { project ->
                ProjectRow(
                    project = project,
                    onClick = { onOpenProject(project.id) },
                    onRename = { renameTarget = project },
                    onDuplicate = { viewModel.duplicate(project.id) },
                    onDelete = { deleteTarget = project },
                    onFavorite = { viewModel.toggleFavorite(project.id) },
                    onExport = {
                        exportTarget = project
                        exportLauncher.launch(project.appName + ".htmltoapk.json")
                    }
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    renameTarget?.let { p ->
        TextInputDialog(
            title = stringResource(R.string.rename_title),
            initial = p.appName,
            label = stringResource(R.string.field_app_name),
            onConfirm = {
                if (it.isNotBlank()) viewModel.rename(p.id, it)
                renameTarget = null
            },
            onDismiss = { renameTarget = null }
        )
    }
    deleteTarget?.let { p ->
        ConfirmDialog(
            title = stringResource(R.string.confirm_delete_title),
            message = stringResource(R.string.confirm_delete_message).format(p.appName),
            onConfirm = { viewModel.delete(p.id); deleteTarget = null },
            onDismiss = { deleteTarget = null }
        )
    }
}

@Composable
private fun SortMenuItem(label: String, onClick: () -> Unit) {
    DropdownMenuItem(text = { Text(label) }, onClick = onClick)
}

@Composable
private fun ProjectRow(
    project: ProjectConfig,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onExport: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            LogoTile(initials = project.appName.take(2))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(project.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(project.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "${project.version} • ${TimeUtil.format(project.modifiedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFavorite) {
                Icon(
                    if (project.favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = stringResource(R.string.action_favorite),
                    tint = if (project.favorite) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.content_description_more))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_rename)) },
                        onClick = { menuExpanded = false; onRename() },
                        leadingIcon = { Icon(Icons.Filled.Edit, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_duplicate)) },
                        onClick = { menuExpanded = false; onDuplicate() },
                        leadingIcon = { Icon(Icons.Filled.ContentCopy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_export)) },
                        onClick = { menuExpanded = false; onExport() },
                        leadingIcon = { Icon(Icons.Filled.FileUpload, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
                        onClick = { menuExpanded = false; onDelete() },
                        leadingIcon = { Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}
