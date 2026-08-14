package com.example.ui.screens.projects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppLanguage
import com.example.domain.model.Project
import com.example.ui.components.IosBottomSheet
import com.example.ui.components.IosButton
import com.example.ui.components.IosButtonStyle
import com.example.ui.components.IosGroupSection
import com.example.ui.components.IosListItem
import com.example.ui.components.IosSeparator
import com.example.ui.components.IosTextField
import com.example.ui.components.IosTopBar
import com.example.util.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    viewModel: ProjectsViewModel,
    language: AppLanguage,
    onCreateNewClick: () -> Unit,
    onEditProjectClick: (Long) -> Unit,
    onOpenWorkspaceClick: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val projects by viewModel.filteredProjects.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedProject by viewModel.selectedProject.collectAsStateWithLifecycle()
    val deleteConfirmProject by viewModel.showDeleteConfirm.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("projects_screen")
    ) {
        IosTopBar(
            title = Strings.get("nav_projects", language),
            largeTitle = true,
            trailingAction = {
                IconButton(
                    onClick = onCreateNewClick,
                    modifier = Modifier.testTag("btn_add_project_top")
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Project",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        )

        // Search Bar
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            IosTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = Strings.get("search_projects", language),
                testTag = "input_search_projects"
            )
        }

        // Projects List
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = Strings.get("no_projects_yet", language),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IosButton(
                        text = Strings.get("action_create_app", language),
                        onClick = onCreateNewClick,
                        style = IosButtonStyle.PRIMARY,
                        icon = Icons.Default.Add,
                        testTag = "btn_create_first_app"
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                item {
                    IosGroupSection {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                        projects.forEachIndexed { index, project ->
                            val colorInt = runCatching { android.graphics.Color.parseColor(project.iconColorHex) }
                                .getOrDefault(android.graphics.Color.parseColor("#007AFF"))

                            IosListItem(
                                title = project.name,
                                subtitle = "${project.packageName} • v${project.version}\n${dateFormat.format(Date(project.lastModified))}",
                                detailText = project.sourceType.displayName,
                                icon = Icons.Default.Code,
                                iconBackground = Color(colorInt),
                                showChevron = true,
                                onClick = { onOpenWorkspaceClick(project.id) },
                                testTag = "project_list_item_$index"
                            )

                            if (index < projects.size - 1) {
                                IosSeparator(paddingStart = 58)
                            }
                        }
                    }
                }
            }
        }
    }

    // Project Details & Actions Bottom Sheet
    selectedProject?.let { proj ->
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val colorInt = runCatching { android.graphics.Color.parseColor(proj.iconColorHex) }
            .getOrDefault(android.graphics.Color.parseColor("#007AFF"))

        IosBottomSheet(
            onDismissRequest = { viewModel.selectProjectForDetails(null) },
            testTag = "bottom_sheet_project_details"
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header Profile Card
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(colorInt)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = proj.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )
                        Text(
                            text = proj.packageName,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }

                // Info Group
                IosGroupSection {
                    IosListItem(
                        title = Strings.get("source_type", language),
                        detailText = proj.sourceType.displayName
                    )
                    IosSeparator()
                    IosListItem(
                        title = Strings.get("version_label", language),
                        detailText = "v${proj.version}"
                    )
                    IosSeparator()
                    IosListItem(
                        title = Strings.get("display_mode", language),
                        detailText = proj.displayMode.label.split(" ").first()
                    )
                    IosSeparator()
                    IosListItem(
                        title = Strings.get("last_modified", language),
                        detailText = dateFormat.format(Date(proj.lastModified))
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions: Open, Edit, Delete
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IosButton(
                        text = Strings.get("action_open", language),
                        onClick = {
                            viewModel.selectProjectForDetails(null)
                            onOpenWorkspaceClick(proj.id)
                        },
                        icon = Icons.Default.FolderOpen,
                        style = IosButtonStyle.PRIMARY,
                        testTag = "btn_open_project"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    IosButton(
                        text = Strings.get("action_edit", language),
                        onClick = {
                            viewModel.selectProjectForDetails(null)
                            onEditProjectClick(proj.id)
                        },
                        icon = Icons.Default.Edit,
                        style = IosButtonStyle.SECONDARY,
                        testTag = "btn_edit_project"
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    IosButton(
                        text = Strings.get("action_delete", language),
                        onClick = {
                            viewModel.promptDeleteProject(proj)
                        },
                        icon = Icons.Default.Delete,
                        style = IosButtonStyle.DESTRUCTIVE,
                        testTag = "btn_delete_project"
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    deleteConfirmProject?.let { proj ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteConfirm() },
            title = {
                Text(
                    text = Strings.get("delete_confirm_title", language),
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "${Strings.get("delete_confirm_message", language)}\n\n\"${proj.name}\""
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.confirmDeleteProject() },
                    modifier = Modifier.testTag("btn_confirm_delete")
                ) {
                    Text(
                        text = Strings.get("action_delete", language),
                        color = Color(0xFFFF3B30),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.dismissDeleteConfirm() },
                    modifier = Modifier.testTag("btn_cancel_delete")
                ) {
                    Text(text = Strings.get("cancel", language))
                }
            }
        )
    }
}
