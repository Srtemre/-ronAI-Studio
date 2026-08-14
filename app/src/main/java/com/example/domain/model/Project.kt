package com.example.domain.model

enum class SourceType(val displayName: String, val description: String) {
    HTML("HTML File", "Local single or bundled HTML entry"),
    ZIP("ZIP Archive", "Compressed web package with assets"),
    URL("Website URL", "Remote web application or website link"),
    HTML_TEXT("HTML Code", "Custom inline HTML, CSS, and JS"),
    PWA("PWA", "Progressive Web App with Web Manifest")
}

enum class DisplayMode(val label: String) {
    STANDALONE("Standalone (Full App UI)"),
    FULLSCREEN("Fullscreen (No System Bars)"),
    MINIMAL_UI("Minimal UI (Browser Controls)")
}

enum class Orientation(val label: String) {
    AUTO("Auto-Rotate"),
    PORTRAIT("Portrait Only"),
    LANDSCAPE("Landscape Only")
}

data class Project(
    val id: Long = 0,
    val name: String,
    val packageName: String,
    val version: String = "1.0.0",
    val versionCode: Int = 1,
    val sourceType: SourceType = SourceType.HTML,
    val targetUrl: String = "",
    val htmlContent: String = "",
    val displayMode: DisplayMode = DisplayMode.STANDALONE,
    val orientation: Orientation = Orientation.AUTO,
    val enableJavaScript: Boolean = true,
    val enableLocalStorage: Boolean = true,
    val enableOfflineCaching: Boolean = true,
    val iconColorHex: String = "#007AFF",
    val createdDate: Long = System.currentTimeMillis(),
    val lastModified: Long = System.currentTimeMillis()
)
