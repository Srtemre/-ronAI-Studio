package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AppLanguage
import com.example.util.Strings

enum class NavTab {
    HOME,
    PROJECTS,
    SETTINGS
}

data class BottomNavItem(
    val tab: NavTab,
    val titleKey: String,
    val activeIcon: ImageVector,
    val inactiveIcon: ImageVector
)

@Composable
fun IosBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    language: AppLanguage,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        BottomNavItem(
            tab = NavTab.HOME,
            titleKey = "nav_home",
            activeIcon = Icons.Filled.Home,
            inactiveIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            tab = NavTab.PROJECTS,
            titleKey = "nav_projects",
            activeIcon = Icons.Filled.Folder,
            inactiveIcon = Icons.Outlined.Folder
        ),
        BottomNavItem(
            tab = NavTab.SETTINGS,
            titleKey = "nav_settings",
            activeIcon = Icons.Filled.Settings,
            inactiveIcon = Icons.Outlined.Settings
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .testTag("ios_bottom_bar")
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outline
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val isSelected = currentTab == item.tab
                val interactionSource = remember { MutableInteractionSource() }
                val title = Strings.get(item.titleKey, language)

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) { onTabSelected(item.tab) }
                        .padding(vertical = 6.dp)
                        .testTag("nav_tab_${item.tab.name.lowercase()}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = if (isSelected) item.activeIcon else item.inactiveIcon,
                        contentDescription = title,
                        tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )

                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}
