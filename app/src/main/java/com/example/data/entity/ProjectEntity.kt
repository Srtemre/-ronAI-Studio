package com.example.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.DisplayMode
import com.example.domain.model.Orientation
import com.example.domain.model.Project
import com.example.domain.model.SourceType

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val packageName: String,
    val version: String,
    val versionCode: Int = 1,
    val sourceType: String,
    val targetUrl: String,
    val htmlContent: String,
    val displayMode: String,
    val orientation: String,
    val enableJavaScript: Boolean,
    val enableLocalStorage: Boolean,
    val enableOfflineCaching: Boolean,
    val iconColorHex: String,
    val createdDate: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)

fun ProjectEntity.toDomain(): Project {
    return Project(
        id = id,
        name = name,
        packageName = packageName,
        version = version,
        versionCode = versionCode,
        sourceType = runCatching { SourceType.valueOf(sourceType) }.getOrDefault(SourceType.HTML),
        targetUrl = targetUrl,
        htmlContent = htmlContent,
        displayMode = runCatching { DisplayMode.valueOf(displayMode) }.getOrDefault(DisplayMode.STANDALONE),
        orientation = runCatching { Orientation.valueOf(orientation) }.getOrDefault(Orientation.AUTO),
        enableJavaScript = enableJavaScript,
        enableLocalStorage = enableLocalStorage,
        enableOfflineCaching = enableOfflineCaching,
        iconColorHex = iconColorHex,
        createdDate = if (createdDate > 0) createdDate else lastModified,
        lastModified = lastModified
    )
}

fun Project.toEntity(): ProjectEntity {
    return ProjectEntity(
        id = id,
        name = name,
        packageName = packageName,
        version = version,
        versionCode = versionCode,
        sourceType = sourceType.name,
        targetUrl = targetUrl,
        htmlContent = htmlContent,
        displayMode = displayMode.name,
        orientation = orientation.name,
        enableJavaScript = enableJavaScript,
        enableLocalStorage = enableLocalStorage,
        enableOfflineCaching = enableOfflineCaching,
        iconColorHex = iconColorHex,
        createdDate = createdDate,
        lastModified = lastModified
    )
}
