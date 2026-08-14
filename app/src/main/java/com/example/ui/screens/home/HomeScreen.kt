package com.example.ui.screens.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MobileFriendly
import androidx.compose.material3.Icon
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.AppLanguage
import com.example.domain.model.Project
import com.example.domain.model.SourceType
import com.example.ui.components.IosButton
import com.example.ui.components.IosButtonStyle
import com.example.ui.components.IosGroupSection
import com.example.ui.components.IosListItem
import com.example.ui.components.IosSeparator
import com.example.util.Strings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    language: AppLanguage,
    onCreateAppClick: (initialSourceType: SourceType?) -> Unit,
    onViewProjectsClick: () -> Unit,
    onProjectClick: (Project) -> Unit,
    modifier: Modifier = Modifier
) {
    val recentProjects by viewModel.recentProjects.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalProjectsCount.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .testTag("home_screen")
    ) {
        // App Header & Subtitle
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 24.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Code,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = Strings.get("app_title", language),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = Strings.get("app_subtitle", language),
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }

        // Main Action (Single primary Create App)
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            IosButton(
                text = Strings.get("action_create_app", language),
                onClick = { onCreateAppClick(null) },
                icon = Icons.Default.Add,
                style = IosButtonStyle.PRIMARY,
                testTag = "btn_create_app"
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Recent Projects Section Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Strings.get("recent_projects", language),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.weight(1f)
            )

            if (recentProjects.isNotEmpty()) {
                TextButton(
                    onClick = onViewProjectsClick,
                    modifier = Modifier.testTag("btn_view_all_projects")
                ) {
                    Text(
                        text = Strings.get("view_all", language),
                        style = MaterialTheme.typography.labelLarge.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Recent Projects List or Empty State
        if (recentProjects.isEmpty()) {
            IosGroupSection {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MobileFriendly,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = Strings.get("no_projects_yet", language),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            IosGroupSection {
                val dateFormat = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                recentProjects.forEachIndexed { index, project ->
                    val colorInt = runCatching { android.graphics.Color.parseColor(project.iconColorHex) }
                        .getOrDefault(android.graphics.Color.parseColor("#007AFF"))
                    
                    IosListItem(
                        title = project.name,
                        subtitle = "${project.packageName} • v${project.version}",
                        detailText = project.sourceType.displayName,
                        icon = Icons.Default.Code,
                        iconBackground = Color(colorInt),
                        showChevron = true,
                        onClick = { onProjectClick(project) },
                        testTag = "recent_project_item_$index"
                    )

                    if (index < recentProjects.size - 1) {
                        IosSeparator(paddingStart = 58)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
