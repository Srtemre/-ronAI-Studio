package com.example.data.repository

import com.example.data.dao.ProjectDao
import com.example.data.entity.toDomain
import com.example.data.entity.toEntity
import com.example.domain.model.Project
import com.example.util.ProjectFileManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File
import java.io.InputStream

class ProjectRepository(
    private val projectDao: ProjectDao,
    private val fileManager: ProjectFileManager? = null
) {

    val allProjects: Flow<List<Project>> = projectDao.getAllProjects()
        .map { list -> list.map { it.toDomain() } }

    fun getRecentProjects(limit: Int = 3): Flow<List<Project>> = projectDao.getRecentProjects(limit)
        .map { list -> list.map { it.toDomain() } }

    suspend fun getProjectById(id: Long): Project? {
        return projectDao.getProjectById(id)?.toDomain()
    }

    suspend fun getAllProjectsList(): List<Project> {
        return projectDao.getAllProjectsDirect().map { it.toDomain() }
    }

    suspend fun saveProject(project: Project): Long {
        val now = System.currentTimeMillis()
        val toSave = project.copy(lastModified = now)
        val id = projectDao.insertProject(toSave.toEntity())

        // Automatically write inline htmlContent to index.html if sourceType is HTML_TEXT or HTML
        if (fileManager != null && toSave.htmlContent.isNotBlank()) {
            fileManager.saveTextFile(id, "index.html", toSave.htmlContent)
        }
        return id
    }

    suspend fun duplicateProject(sourceProjectId: Long): Long? {
        val original = getProjectById(sourceProjectId) ?: return null
        val now = System.currentTimeMillis()
        val duplicateProject = original.copy(
            id = 0,
            name = "${original.name} (Copy)",
            createdDate = now,
            lastModified = now
        )
        val newId = saveProject(duplicateProject)
        fileManager?.duplicateProjectFiles(sourceProjectId, newId)
        return newId
    }

    suspend fun deleteProject(id: Long) {
        projectDao.deleteProjectById(id)
        fileManager?.deleteProjectDirectory(id)
    }

    suspend fun deleteAllProjects() {
        projectDao.deleteAllProjects()
        fileManager?.deleteAllProjectsData()
    }

    suspend fun exportProjectsZip(): File? {
        val projects = getAllProjectsList()
        return fileManager?.exportAllProjectsToZip(projects)
    }

    suspend fun importProjectsFromZip(inputStream: InputStream): Pair<Int, String?> {
        val fm = fileManager ?: return Pair(0, "File manager not available")
        val backup = fm.readProjectsFromBackupZip(inputStream) ?: return Pair(0, "Invalid or corrupted backup archive")

        var importedCount = 0
        try {
            for (proj in backup.projects) {
                val oldId = proj.id
                val newProj = proj.copy(id = 0, lastModified = System.currentTimeMillis())
                val newId = projectDao.insertProject(newProj.toEntity())

                // Copy files from extracted temp dir
                val oldProjDir = File(backup.tempDir, "projects_data/proj_$oldId")
                if (oldProjDir.exists()) {
                    val targetDir = fm.getProjectDir(newId)
                    oldProjDir.copyRecursively(targetDir, overwrite = true)
                }
                importedCount++
            }
        } finally {
            backup.tempDir.deleteRecursively()
        }

        return Pair(importedCount, null)
    }
}

