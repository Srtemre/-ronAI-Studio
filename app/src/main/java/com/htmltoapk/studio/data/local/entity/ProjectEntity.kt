package com.htmltoapk.studio.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val appName: String,
    val packageName: String,
    val version: String,
    val logoUri: String,
    val splashUri: String,
    val orientation: String,
    val sourceType: String,
    val sourceUri: String,
    val expertMode: Boolean,
    val configJson: String,
    val createdAt: Long,
    val modifiedAt: Long,
    val lastBuiltAt: Long,
    val favorite: Boolean,
    val generatedPath: String,
)
