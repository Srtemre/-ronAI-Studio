package com.htmltoapk.studio.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.htmltoapk.studio.ui.editor.EditorScreen
import com.htmltoapk.studio.ui.home.HomeScreen
import com.htmltoapk.studio.ui.projects.ProjectsScreen
import com.htmltoapk.studio.ui.settings.SettingsScreen

@Composable
fun AppNavGraph() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in Destination.BottomRoutes

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    Destination.BottomItems.forEach { dest ->
                        val selected = backStackEntry?.destination?.hierarchy?.any { it.route == dest.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(dest.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = null) },
                            label = { Text(stringResource(dest.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Home.route) {
                HomeScreen(
                    onOpenProjects = { navController.navigate(Destination.Projects.route) },
                    onOpenSettings = { navController.navigate(Destination.Settings.route) },
                    onOpenEditor = { expert, projectId ->
                        navController.navigate(Destination.EditorNewFast.create(expert, projectId))
                    },
                    onOpenRecent = { id ->
                        navController.navigate(Destination.EditorNewFast.create(expert = false, projectId = id))
                    }
                )
            }
            composable(Destination.Projects.route) {
                ProjectsScreen(
                    onOpenProject = { id ->
                        navController.navigate(Destination.EditorNewFast.create(expert = false, projectId = id))
                    },
                    onNewProject = { expert ->
                        navController.navigate(Destination.EditorNewFast.create(expert = expert))
                    }
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = "editor/new/fast?expert={expert}&projectId={projectId}",
                arguments = listOf(
                    navArgument("expert") { type = NavType.BoolType; defaultValue = false },
                    navArgument("projectId") { type = NavType.LongType; defaultValue = -1L }
                )
            ) { backStackEntry ->
                val expert = backStackEntry.arguments?.getBoolean("expert") ?: false
                val projectId = backStackEntry.arguments?.getLong("projectId") ?: -1L
                EditorScreen(
                    expertMode = expert,
                    projectId = if (projectId <= 0L) null else projectId,
                    onClose = { navController.popBackStack() },
                    onSaved = { id ->
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
