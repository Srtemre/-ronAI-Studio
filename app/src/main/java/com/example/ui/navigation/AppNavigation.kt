package com.example.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.database.AppDatabase
import com.example.data.repository.ProjectRepository
import com.example.data.repository.SettingsRepository
import com.example.domain.model.AppLanguage
import com.example.domain.model.SourceType
import com.example.ui.components.IosBottomBar
import com.example.ui.components.NavTab
import com.example.ui.screens.create.CreateAppScreen
import com.example.ui.screens.create.CreateAppViewModel
import com.example.ui.screens.create.CreateModeSelectionScreen
import com.example.ui.screens.create.FastCreateScreen
import com.example.ui.screens.create.FastCreateViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.projects.ProjectsScreen
import com.example.ui.screens.projects.ProjectsViewModel
import com.example.ui.screens.settings.SettingsScreen
import com.example.ui.screens.settings.SettingsViewModel

import com.example.util.ProjectFileManager
import com.example.ui.screens.workspace.ProjectWorkspaceScreen
import com.example.ui.screens.workspace.WorkspaceViewModel

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Projects : Screen("projects")
    object Settings : Screen("settings")
    object CreateModeSelection : Screen("create_mode_selection")
    object FastCreate : Screen("fast_create")
    object CreateApp : Screen("create_app?sourceType={sourceType}&projectId={projectId}") {
        fun createRoute(sourceType: SourceType? = null, projectId: Long? = null): String {
            val typeParam = sourceType?.name ?: ""
            val idParam = projectId?.toString() ?: ""
            return "create_app?sourceType=$typeParam&projectId=$idParam"
        }
    }
    object Workspace : Screen("workspace?projectId={projectId}") {
        fun createRoute(projectId: Long): String = "workspace?projectId=$projectId"
    }
}

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current

    // Initialize Database & Repositories
    val database = remember { AppDatabase.getDatabase(context) }
    val fileManager = remember { ProjectFileManager(context) }
    val projectRepository = remember { ProjectRepository(database.projectDao(), fileManager) }
    val settingsRepository = remember { SettingsRepository(context) }

    // Observe App Settings
    val appSettings by settingsRepository.settings.collectAsStateWithLifecycle()

    // ViewModels
    val homeViewModel = remember { HomeViewModel(projectRepository) }
    val projectsViewModel = remember { ProjectsViewModel(projectRepository) }
    val settingsViewModel = remember { SettingsViewModel(settingsRepository, projectRepository) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val currentTab = when {
        currentRoute?.startsWith(Screen.Projects.route) == true -> NavTab.PROJECTS
        currentRoute?.startsWith(Screen.Settings.route) == true -> NavTab.SETTINGS
        else -> NavTab.HOME
    }

    val showBottomBar = currentRoute in listOf(
        Screen.Home.route,
        Screen.Projects.route,
        Screen.Settings.route
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                IosBottomBar(
                    currentTab = currentTab,
                    onTabSelected = { tab ->
                        val targetRoute = when (tab) {
                            NavTab.HOME -> Screen.Home.route
                            NavTab.PROJECTS -> Screen.Projects.route
                            NavTab.SETTINGS -> Screen.Settings.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    language = appSettings.language
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            enterTransition = { fadeIn(animationSpec = tween(220)) },
            exitTransition = { fadeOut(animationSpec = tween(220)) }
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    viewModel = homeViewModel,
                    language = appSettings.language,
                    onCreateAppClick = { _ ->
                        navController.navigate(Screen.CreateModeSelection.route)
                    },
                    onViewProjectsClick = {
                        navController.navigate(Screen.Projects.route)
                    },
                    onProjectClick = { project ->
                        navController.navigate(Screen.Workspace.createRoute(project.id))
                    }
                )
            }

            composable(Screen.Projects.route) {
                ProjectsScreen(
                    viewModel = projectsViewModel,
                    language = appSettings.language,
                    onCreateNewClick = {
                        navController.navigate(Screen.CreateModeSelection.route)
                    },
                    onEditProjectClick = { projectId ->
                        navController.navigate(Screen.CreateApp.createRoute(projectId = projectId))
                    },
                    onOpenWorkspaceClick = { projectId ->
                        navController.navigate(Screen.Workspace.createRoute(projectId))
                    }
                )
            }

            composable(Screen.Settings.route) {
                SettingsScreen(
                    viewModel = settingsViewModel
                )
            }

            composable(Screen.CreateModeSelection.route) {
                CreateModeSelectionScreen(
                    language = appSettings.language,
                    onBackClick = { navController.popBackStack() },
                    onSelectFastMode = {
                        navController.navigate(Screen.FastCreate.route)
                    },
                    onSelectExpertMode = {
                        navController.navigate(Screen.CreateApp.createRoute())
                    }
                )
            }

            composable(Screen.FastCreate.route) {
                val fastCreateViewModel = remember {
                    FastCreateViewModel(projectRepository, fileManager)
                }

                FastCreateScreen(
                    viewModel = fastCreateViewModel,
                    language = appSettings.language,
                    onBackClick = { navController.popBackStack() },
                    onOpenWorkspace = { savedProjectId ->
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                        navController.navigate(Screen.Workspace.createRoute(savedProjectId))
                    }
                )
            }

            composable(
                route = Screen.Workspace.route,
                arguments = listOf(
                    navArgument("projectId") {
                        type = NavType.StringType
                        nullable = false
                        defaultValue = "0"
                    }
                )
            ) { backStackEntry ->
                val projectIdStr = backStackEntry.arguments?.getString("projectId")
                val projectId = projectIdStr?.toLongOrNull() ?: 0L

                val workspaceViewModel = remember(projectId) {
                    WorkspaceViewModel(
                        projectId = projectId,
                        projectRepository = projectRepository,
                        fileManager = fileManager
                    )
                }

                ProjectWorkspaceScreen(
                    viewModel = workspaceViewModel,
                    fileManager = fileManager,
                    language = appSettings.language,
                    onBackClick = { navController.popBackStack() },
                    onEditSettingsClick = { id ->
                        navController.navigate(Screen.CreateApp.createRoute(projectId = id))
                    },
                    onProjectDuplicated = { newId ->
                        navController.popBackStack()
                        navController.navigate(Screen.Workspace.createRoute(newId))
                    },
                    onProjectDeleted = {
                        navController.popBackStack()
                        navController.navigate(Screen.Projects.route)
                    }
                )
            }

            composable(
                route = Screen.CreateApp.route,
                arguments = listOf(
                    navArgument("sourceType") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    },
                    navArgument("projectId") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val sourceTypeStr = backStackEntry.arguments?.getString("sourceType")
                val projectIdStr = backStackEntry.arguments?.getString("projectId")

                val sourceType = sourceTypeStr?.takeIf { it.isNotEmpty() }?.let {
                    runCatching { SourceType.valueOf(it) }.getOrNull()
                }
                val projectId = projectIdStr?.takeIf { it.isNotEmpty() }?.toLongOrNull()

                val createAppViewModel = remember(sourceTypeStr, projectIdStr) {
                    CreateAppViewModel(projectRepository, fileManager).apply {
                        if (projectId != null && projectId > 0) {
                            loadProjectForEdit(projectId)
                        } else {
                            initWithSourceType(sourceType)
                        }
                    }
                }

                CreateAppScreen(
                    viewModel = createAppViewModel,
                    language = appSettings.language,
                    onBackClick = { navController.popBackStack() },
                    onProjectSaved = { savedProjectId ->
                        navController.popBackStack()
                        navController.navigate(Screen.Workspace.createRoute(savedProjectId))
                    }
                )
            }
        }
    }
}
