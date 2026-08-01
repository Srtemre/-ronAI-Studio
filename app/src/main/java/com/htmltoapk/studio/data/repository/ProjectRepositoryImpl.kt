package com.htmltoapk.studio.data.repository

import com.htmltoapk.studio.data.local.dao.ProjectDao
import com.htmltoapk.studio.data.local.entity.ProjectEntity
import com.htmltoapk.studio.data.model.ProjectConfig
import com.htmltoapk.studio.data.model.SourceType
import com.htmltoapk.studio.domain.repository.ProjectFilter
import com.htmltoapk.studio.domain.repository.ProjectRepository
import com.htmltoapk.studio.domain.repository.ProjectSort
import com.htmltoapk.studio.domain.repository.SortField
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProjectRepositoryImpl @Inject constructor(
    private val dao: ProjectDao,
    private val json: Json
) : ProjectRepository {

    override fun observe(
        filter: ProjectFilter,
        sort: ProjectSort,
        query: String
    ): Flow<List<ProjectConfig>> {
        val source: Flow<List<ProjectEntity>> = when {
            query.isNotBlank() -> dao.search(query)
            filter == ProjectFilter.FAVORITES -> dao.observeFavorites()
            filter == ProjectFilter.RECENT -> dao.observeRecent()
            else -> dao.observeAll()
        }
        return source.map { list ->
            val mapped = list.map { it.toDomain(json) }
            when (sort.field) {
                SortField.NAME -> if (sort.ascending) mapped.sortedBy { it.appName.lowercase() }
                else mapped.sortedByDescending { it.appName.lowercase() }
                SortField.MODIFIED -> if (sort.ascending) mapped.sortedBy { it.modifiedAt }
                else mapped.sortedByDescending { it.modifiedAt }
            }
        }
    }

    override fun observeRecent(): Flow<List<ProjectConfig>> =
        dao.observeRecent().map { list -> list.map { it.toDomain(json) } }

    override fun observeCount(): Flow<Int> = dao.observeCount()
    override fun observeFavoriteCount(): Flow<Int> = dao.observeFavoriteCount()
    override fun observeLastBuild(): Flow<Long?> = dao.observeLastBuild()

    override suspend fun get(id: Long): ProjectConfig? = withContext(Dispatchers.IO) {
        dao.getById(id)?.toDomain(json)
    }

    override suspend fun upsert(project: ProjectConfig): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val entity = project.copy(modifiedAt = now).toEntity(json)
        if (entity.id == 0L) dao.insert(entity)
        else { dao.update(entity); entity.id }
    }

    override suspend fun rename(id: Long, name: String) = withContext(Dispatchers.IO) {
        dao.rename(id, name, System.currentTimeMillis())
    }

    override suspend fun toggleFavorite(id: Long) = withContext(Dispatchers.IO) {
        val current = dao.getById(id) ?: return@withContext
        dao.setFavorite(id, !current.favorite, System.currentTimeMillis())
    }

    override suspend fun duplicate(id: Long): Long? = withContext(Dispatchers.IO) {
        val src = dao.getById(id) ?: return@withContext null
        val copy = src.copy(
            id = 0L,
            appName = src.appName + " (copy)",
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis(),
            lastBuiltAt = 0L,
            generatedPath = ""
        )
        dao.insert(copy)
    }

    override suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    override suspend fun markBuilt(id: Long, path: String) = withContext(Dispatchers.IO) {
        dao.markBuilt(id, System.currentTimeMillis(), path)
    }

    override suspend fun export(id: Long): String = withContext(Dispatchers.IO) {
        val entity = dao.getById(id) ?: error("Project not found")
        json.encodeToString(ProjectConfig.serializer(), entity.toDomain(json))
    }

    override suspend fun import(jsonStr: String): Int = withContext(Dispatchers.IO) {
        val cfg = json.decodeFromString(ProjectConfig.serializer(), jsonStr)
        val entity = cfg.copy(
            id = 0L,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        ).toEntity(this.json)
        dao.insert(entity)
        1
    }

    private fun ProjectEntity.toDomain(json: Json): ProjectConfig {
        // Entity columns are authoritative for fields that can be mutated via
        // DAO update queries (rename, setFavorite, markBuilt) — never trust the
        // configJson snapshot for those.
        return if (configJson.isBlank()) {
            ProjectConfig(
                id = id,
                appName = appName,
                packageName = packageName,
                version = version,
                logoUri = logoUri,
                splashUri = splashUri,
                orientation = com.htmltoapk.studio.data.model.Orientation.valueOf(orientation),
                sourceType = SourceType.valueOf(sourceType),
                sourceUri = sourceUri,
                expertMode = expertMode,
                createdAt = createdAt,
                modifiedAt = modifiedAt,
                lastBuiltAt = lastBuiltAt,
                favorite = favorite,
                generatedPath = generatedPath
            )
        } else {
            json.decodeFromString(ProjectConfig.serializer(), configJson).copy(
                id = id,
                appName = appName,
                packageName = packageName,
                version = version,
                logoUri = logoUri,
                splashUri = splashUri,
                orientation = com.htmltoapk.studio.data.model.Orientation.valueOf(orientation),
                sourceType = SourceType.valueOf(sourceType),
                sourceUri = sourceUri,
                expertMode = expertMode,
                createdAt = createdAt,
                modifiedAt = modifiedAt,
                lastBuiltAt = lastBuiltAt,
                favorite = favorite,
                generatedPath = generatedPath
            )
        }
    }

    private fun ProjectConfig.toEntity(json: Json): ProjectEntity {
        val configJson = json.encodeToString(ProjectConfig.serializer(), this)
        return ProjectEntity(
            id = id,
            appName = appName,
            packageName = packageName,
            version = version,
            logoUri = logoUri,
            splashUri = splashUri,
            orientation = orientation.name,
            sourceType = sourceType.name,
            sourceUri = sourceUri,
            expertMode = expertMode,
            configJson = configJson,
            createdAt = createdAt,
            modifiedAt = modifiedAt,
            lastBuiltAt = lastBuiltAt,
            favorite = favorite,
            generatedPath = generatedPath
        )
    }
}
