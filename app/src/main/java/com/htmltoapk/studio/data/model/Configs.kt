package com.htmltoapk.studio.data.model

import kotlinx.serialization.Serializable

enum class SourceType { HTML_FILE, ZIP, FOLDER, URL, PASTE_HTML }
enum class Orientation { AUTO, PORTRAIT, LANDSCAPE }
enum class CacheMode(val value: Int) {
    DEFAULT(-1), NO_CACHE(0), CACHE_ELSE_NETWORK(1),
    NETWORK_ELSE_CACHE(2), CACHE_ONLY(3)
}
enum class UserAgentMode { DEFAULT, DESKTOP, CUSTOM }

@Serializable
data class WebViewConfig(
    val javaScript: Boolean = true,
    val domStorage: Boolean = true,
    val mixedContent: Boolean = false,
    val cacheMode: CacheMode = CacheMode.DEFAULT,
    val userAgentMode: UserAgentMode = UserAgentMode.DEFAULT,
    val customUserAgent: String = "",
    val fullscreen: Boolean = false,
    val immersive: Boolean = false,
    val keepScreenOn: Boolean = false,
    val edgeToEdge: Boolean = true,
    val adaptiveIcons: Boolean = true,
    val loadRemote: Boolean = true,
    val zoom: Boolean = false,
    val mediaPlayback: Boolean = true,
    val fileAccess: Boolean = false,
)

@Serializable
data class PermissionsConfig(
    val internet: Boolean = true,
    val networkState: Boolean = true,
    val storage: Boolean = false,
    val camera: Boolean = false,
    val microphone: Boolean = false,
    val location: Boolean = false,
    val notifications: Boolean = false,
    val vibrate: Boolean = false,
)

@Serializable
data class SigningConfig(
    val keystoreUri: String = "",
    val keystorePassword: String = "",
    val keyAlias: String = "",
    val keyPassword: String = "",
)

@Serializable
data class AdvancedConfig(
    val minSdk: Int = 24,
    val targetSdk: Int = 34,
    val statusBarColor: String = "#000000",
    val navBarColor: String = "#000000",
    val pullToRefresh: Boolean = false,
    val offlineFallback: String = "",
    val deepLinks: Boolean = false,
    val proguard: Boolean = true,
    val customHeadersJson: String = "",
)
