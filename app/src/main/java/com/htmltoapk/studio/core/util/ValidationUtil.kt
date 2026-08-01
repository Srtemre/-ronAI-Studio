package com.htmltoapk.studio.core.util

object ValidationUtil {
    private val PACKAGE_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val URL_REGEX = Regex("^https?://[\\w\\-]+(\\.[\\w\\-]+)+([/?#].*)?$")

    fun isValidPackage(pkg: String): Boolean =
        pkg.isNotBlank() && PACKAGE_REGEX.matches(pkg) && !pkg.contains("..")

    fun isValidUrl(url: String): Boolean = URL_REGEX.matches(url.trim())

    /**
     * Returns null when valid, or the resource name of the error string to show.
     */
    fun validateProject(
        appName: String,
        packageName: String,
        sourceUri: String
    ): String? = when {
        appName.isBlank() -> "error_app_name_required"
        !isValidPackage(packageName) -> "error_package_invalid"
        sourceUri.isBlank() -> "error_source_required"
        else -> null
    }
}
