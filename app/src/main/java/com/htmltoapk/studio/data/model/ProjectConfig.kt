package com.htmltoapk.studio.data.model

import kotlinx.serialization.Serializable

@Serializable
data class ProjectConfig(
    val id: Long = 0L,
    val appName: String,
    val packageName: String,
    val version: String,
    val logoUri: String = "",
    val splashUri: String = "",
    val orientation: Orientation = Orientation.AUTO,
    val sourceType: SourceType,
    val sourceUri: String,
    val expertMode: Boolean = false,
    val web: WebViewConfig = WebViewConfig(),
    val permissions: PermissionsConfig = PermissionsConfig(),
    val signing: SigningConfig = SigningConfig(),
    val advanced: AdvancedConfig = AdvancedConfig(),
    val createdAt: Long = System.currentTimeMillis(),
    val modifiedAt: Long = System.currentTimeMillis(),
    val lastBuiltAt: Long = 0L,
    val favorite: Boolean = false,
    val generatedPath: String = "",
)
