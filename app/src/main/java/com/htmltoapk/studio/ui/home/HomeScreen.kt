package com.htmltoapk.studio.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.htmltoapk.studio.R
import com.htmltoapk.studio.core.util.TimeUtil
import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.ui.components.EmptyState
import com.htmltoapk.studio.ui.components.LogoTile
import com.htmltoapk.studio.ui.components.RoundedCard
import com.htmltoapk.studio.ui.components.SectionHeader

@Composable
fun HomeScreen(
    onOpenProjects: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenEditor: (expert: Boolean, projectId: Long?) -> Unit,
    onOpenRecent: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    stringResource(R.string.home_welcome),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            SectionHeader(stringResource(R.string.home_quick_start), icon = Icons.Filled.Bolt)
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ModeCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.home_fast_mode),
                    description = stringResource(R.string.home_fast_mode_desc),
                    icon = Icons.Filled.AutoAwesome,
                    onClick = { onOpenEditor(false, null) }
                )
                ModeCard(
                    modifier = Modifier.weight(1f),
                    title = stringResource(R.string.home_expert_mode),
                    description = stringResource(R.string.home_expert_mode_desc),
                    icon = Icons.Filled.Tune,
                    onClick = { onOpenEditor(true, null) }
                )
            }
        }
        item {
            SectionHeader(stringResource(R.string.home_stats), icon = Icons.Filled.Folder)
            StatsRow(state.totalProjects, state.favorites, state.lastBuild)
        }
        item {
            SectionHeader(stringResource(R.string.home_recent_projects), icon = Icons.Filled.History)
        }
        if (state.recent.isEmpty()) {
            item {
                EmptyState(
                    title = stringResource(R.string.home_no_recent),
                    icon = Icons.Filled.History
                )
            }
        } else {
            items(state.recent, key = { it.id }) { project ->
                RecentProjectItem(project) { onOpenRecent(project.id) }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ModeCard(
    title: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.size(40.dp)
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StatsRow(total: Int, favorites: Int, lastBuild: Long) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(stringResource(R.string.home_stat_total), total.toString(), Modifier.weight(1f), Icons.Filled.Folder)
        StatCard(stringResource(R.string.home_stat_favorites), favorites.toString(), Modifier.weight(1f), Icons.Filled.Favorite)
        StatCard(
            stringResource(R.string.home_stat_last_build),
            if (lastBuild <= 0L) stringResource(R.string.home_stat_never) else TimeUtil.format(lastBuild),
            Modifier.weight(1f),
            Icons.Filled.History
        )
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier, icon: ImageVector) {
    RoundedCard(modifier = modifier, contentPadding = 14.dp) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(6.dp))
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RecentProjectItem(project: ProjectConfig, onClick: () -> Unit) {
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
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(project.appName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(project.packageName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    TimeUtil.format(project.modifiedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (project.favorite) {
                Icon(Icons.Filled.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
