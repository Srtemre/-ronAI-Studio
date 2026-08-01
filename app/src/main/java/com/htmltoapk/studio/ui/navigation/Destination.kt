package com.htmltoapk.studio.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.htmltoapk.studio.R

sealed class Destination(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    data object Home : Destination("home", R.string.nav_home, Icons.Filled.Home)
    data object Projects : Destination("projects", R.string.nav_projects, Icons.Filled.Folder)
    data object Settings : Destination("settings", R.string.nav_settings, Icons.Filled.Settings)

    data object EditorNewFast : Destination(
        route = "editor/new/fast?expert={expert}&projectId={projectId}",
        labelRes = R.string.editor_fast_mode,
        icon = Icons.Filled.Home
    ) {
        const val ARG_EXPERT = "expert"
        const val ARG_PROJECT_ID = "projectId"
        fun create(expert: Boolean, projectId: Long? = null): String {
            val b = StringBuilder("editor/new/fast?expert=").append(expert)
            if (projectId != null) b.append("&projectId=").append(projectId)
            return b.toString()
        }
    }

    companion object {
        val BottomItems = listOf(Home, Projects, Settings)
        val BottomRoutes: Set<String> = BottomItems.map { it.route }.toSet()
    }
}
