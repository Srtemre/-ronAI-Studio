package com.htmltoapk.studio.domain.repository

import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.data.model.SourceType
import kotlinx.coroutines.flow.Flow

data class ProjectSort(val field: SortField, val ascending: Boolean)
enum class SortField { NAME, MODIFIED }
enum class ProjectFilter { ALL, FAVORITES, RECENT }

interface ProjectRepository {
    fun observe(filter: ProjectFilter, sort: ProjectSort, query: String): Flow<List<ProjectConfig>>
    fun observeRecent(): Flow<List<ProjectConfig>>
    fun observeCount(): Flow<Int>
    fun observeFavoriteCount(): Flow<Int>
    fun observeLastBuild(): Flow<Long?>
    suspend fun get(id: Long): ProjectConfig?
    suspend fun upsert(project: ProjectConfig): Long
    suspend fun rename(id: Long, name: String)
    suspend fun toggleFavorite(id: Long)
    suspend fun duplicate(id: Long): Long?
    suspend fun delete(id: Long)
    suspend fun markBuilt(id: Long, path: String)
    suspend fun export(id: Long): String
    suspend fun import(json: String): Int
}

interface SettingsRepository {
    val themeMode: Flow<com.htmltoapk.studio.ui.theme.ThemeMode>
    val accent: Flow<com.htmltoapk.studio.ui.theme.AccentPreset>
    val dynamicColor: Flow<Boolean>
    val locale: Flow<com.htmltoapk.studio.ui.theme.AppLocale>
    val defaultMinSdk: Flow<Int>
    val defaultTargetSdk: Flow<Int>
    val defaultOrientation: Flow<com.htmltoapk.studio.data.model.Orientation>
    val defaultProguard: Flow<Boolean>

    suspend fun setThemeMode(mode: com.htmltoapk.studio.ui.theme.ThemeMode)
    suspend fun setAccent(preset: com.htmltoapk.studio.ui.theme.AccentPreset)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setLocale(locale: com.htmltoapk.studio.ui.theme.AppLocale)
    suspend fun setDefaultMinSdk(v: Int)
    suspend fun setDefaultTargetSdk(v: Int)
    suspend fun setDefaultOrientation(o: com.htmltoapk.studio.data.model.Orientation)
    suspend fun setDefaultProguard(v: Boolean)
    suspend fun defaultAdvanced(): com.htmltoapk.studio.data.model.AdvancedConfig
}
