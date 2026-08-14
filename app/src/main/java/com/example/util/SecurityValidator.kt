package com.example.util

import android.content.Context
import java.io.File
import java.net.URI
import java.util.Locale

data class ValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

object SecurityValidator {

    private val JAVA_KEYWORDS = setOf(
        "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char",
        "class", "const", "continue", "default", "do", "double", "else", "enum",
        "extends", "final", "finally", "float", "for", "goto", "if", "implements",
        "import", "instanceof", "int", "interface", "long", "native", "new", "package",
        "private", "protected", "public", "return", "short", "static", "strictfp",
        "super", "switch", "synchronized", "this", "throw", "throws", "transient",
        "try", "void", "volatile", "while", "true", "false", "null", "fun", "val",
        "var", "when", "object", "typealias"
    )

    private val PACKAGE_NAME_REGEX = Regex("^[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*)+$")
    private val HEX_COLOR_REGEX = Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3}|[A-Fa-f0-9]{8})$")

    /**
     * Validates Android Package Name against naming rules & keywords.
     */
    fun validatePackageName(pkg: String): ValidationResult {
        val trimmed = pkg.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Package name cannot be empty.")
        }
        if (trimmed.length > 255) {
            return ValidationResult(false, "Package name is too long (maximum 255 characters).")
        }
        if (trimmed.length < 3) {
            return ValidationResult(false, "Package name is too short.")
        }
        if (!PACKAGE_NAME_REGEX.matches(trimmed)) {
            return ValidationResult(
                false,
                "Package name must have at least two segments separated by dots (e.g., com.example.app) and contain only letters, numbers, and underscores."
            )
        }

        val segments = trimmed.split(".")
        for (segment in segments) {
            if (segment.isEmpty()) {
                return ValidationResult(false, "Package name contains empty segment.")
            }
            if (!segment.first().isLetter()) {
                return ValidationResult(false, "Each segment in package name must begin with an ASCII letter ('$segment').")
            }
            if (JAVA_KEYWORDS.contains(segment.lowercase(Locale.US))) {
                return ValidationResult(false, "Package segment '$segment' is a reserved Java/Android keyword.")
            }
        }

        return ValidationResult(true)
    }

    /**
     * Validates application version string.
     */
    fun validateVersionName(version: String): ValidationResult {
        val trimmed = version.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "Version string cannot be empty.")
        }
        if (trimmed.length > 32) {
            return ValidationResult(false, "Version string is too long (maximum 32 characters).")
        }
        if (trimmed.any { it.isISOControl() || it == '"' || it == '<' || it == '>' || it == '&' }) {
            return ValidationResult(false, "Version string contains invalid or unsafe characters.")
        }
        return ValidationResult(true)
    }

    /**
     * Validates Android Version Code.
     */
    fun validateVersionCode(code: Int): ValidationResult {
        if (code < 1) {
            return ValidationResult(false, "Version code must be a positive integer (minimum 1).")
        }
        if (code > 2_100_000_000) {
            return ValidationResult(false, "Version code exceeds maximum allowable Android version code.")
        }
        return ValidationResult(true)
    }

    /**
     * Validates HTTP/HTTPS target URLs.
     */
    fun validateUrl(url: String): ValidationResult {
        val trimmed = url.trim()
        if (trimmed.isBlank()) {
            return ValidationResult(false, "URL cannot be empty.")
        }

        val lower = trimmed.lowercase(Locale.US)
        if (lower.startsWith("javascript:") || lower.startsWith("file:") || lower.startsWith("content:") || lower.startsWith("intent:")) {
            return ValidationResult(false, "Prohibited URL scheme for external web application.")
        }

        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return ValidationResult(false, "URL must start with http:// or https://")
        }

        return try {
            val uri = URI(trimmed)
            if (uri.host.isNullOrBlank()) {
                ValidationResult(false, "URL is missing a valid domain/host name.")
            } else {
                ValidationResult(true)
            }
        } catch (e: Exception) {
            ValidationResult(false, "Malformed URL format: ${e.localizedMessage}")
        }
    }

    /**
     * Validates and sanitizes a hex color string.
     */
    fun sanitizeHexColor(colorHex: String, defaultColor: String = "#007AFF"): String {
        val trimmed = colorHex.trim()
        return if (HEX_COLOR_REGEX.matches(trimmed)) {
            trimmed
        } else {
            defaultColor
        }
    }

    /**
     * Escapes XML attribute and text entities to prevent injection.
     */
    fun escapeXml(text: String): String {
        val sb = StringBuilder(text.length + 16)
        for (c in text) {
            when (c) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                '\'' -> sb.append("&apos;")
                else -> {
                    if (c.code in 0x20..0xD7FF || c == '\t' || c == '\n' || c == '\r') {
                        sb.append(c)
                    }
                }
            }
        }
        return sb.toString()
    }

    /**
     * Sanitizes a relative file path, preventing Zip Slip and directory traversal.
     */
    fun sanitizeRelativePath(rawPath: String): String {
        var clean = rawPath.replace('\\', '/')
            .replace("\u0000", "") // remove null bytes
            .trim()

        while (clean.startsWith("/")) {
            clean = clean.substring(1)
        }

        // Split segments and remove any . or ..
        val segments = clean.split("/").filter { it.isNotEmpty() && it != "." }
        val resolved = mutableListOf<String>()

        for (segment in segments) {
            if (segment == "..") {
                if (resolved.isNotEmpty()) {
                    resolved.removeAt(resolved.size - 1)
                }
            } else {
                resolved.add(segment)
            }
        }

        return resolved.joinToString("/")
    }

    /**
     * Checks whether a target file is strictly within the specified base directory.
     */
    fun isPathSafe(baseDir: File, targetFile: File): Boolean {
        return try {
            val canonicalBase = baseDir.canonicalPath
            val canonicalTarget = targetFile.canonicalPath
            canonicalTarget.startsWith(canonicalBase + File.separator) || canonicalTarget == canonicalBase
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Sanitizes build logs and error messages, redacting internal filesystem paths and credentials.
     */
    fun sanitizeLog(message: String, context: Context? = null): String {
        var sanitized = message

        // Redact app-specific internal root paths
        context?.let { ctx ->
            try {
                val filesPath = ctx.filesDir.canonicalPath
                val cachePath = ctx.cacheDir.canonicalPath
                sanitized = sanitized.replace(filesPath, "[internal_storage]")
                sanitized = sanitized.replace(cachePath, "[cache_storage]")
            } catch (_: Exception) {}
        }

        // Generic Linux / Android internal path pattern redaction
        sanitized = sanitized.replace(Regex("/data/user/\\d+/[a-zA-Z0-9._]+"), "[app_private_storage]")
        sanitized = sanitized.replace(Regex("/data/data/[a-zA-Z0-9._]+"), "[app_private_storage]")

        // Redact common secret patterns if present
        sanitized = sanitized.replace(Regex("password=[^\\s,]+", RegexOption.IGNORE_CASE), "password=••••••••")
        sanitized = sanitized.replace(Regex("storePassword=[^\\s,]+", RegexOption.IGNORE_CASE), "storePassword=••••••••")
        sanitized = sanitized.replace(Regex("keyPassword=[^\\s,]+", RegexOption.IGNORE_CASE), "keyPassword=••••••••")

        return sanitized
    }
}
