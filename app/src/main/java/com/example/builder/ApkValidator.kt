package com.example.builder

import com.example.domain.model.Project
import java.io.File
import java.io.FileInputStream
import java.util.zip.ZipInputStream

data class ApkMetadataInfo(
    val packageName: String,
    val versionName: String,
    val versionCode: Int,
    val entryCount: Int,
    val isSigned: Boolean,
    val fileSizeFormatted: String
)

data class ApkValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val metadata: ApkMetadataInfo? = null
)

class ApkValidator {

    fun validateApk(apkFile: File, project: Project): ApkValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        // 1. Check APK exists
        if (!apkFile.exists()) {
            errors.add("Validation Error: APK file does not exist on disk at path ${apkFile.absolutePath}")
            return ApkValidationResult(isValid = false, errors = errors)
        }

        // 2. Check File is not empty
        val fileSize = apkFile.length()
        if (fileSize <= 0) {
            errors.add("Validation Error: APK file is empty (0 bytes)")
            return ApkValidationResult(isValid = false, errors = errors)
        } else if (fileSize < 500) {
            errors.add("Validation Error: APK file size ($fileSize bytes) is suspiciously small for a valid Android package")
            return ApkValidationResult(isValid = false, errors = errors)
        }

        // 3. Check ZIP structure & enumerate entries
        var entryCount = 0
        var hasManifest = false
        var hasClassesDexOrAssets = false
        var hasMetaInf = false
        var hasCertSf = false
        var hasCertRsa = false
        var parsedPkgName: String? = null
        var parsedVersionName: String? = null
        var parsedVersionCode: Int? = null

        try {
            ZipInputStream(FileInputStream(apkFile)).use { zis ->
                var ze = zis.nextEntry
                while (ze != null) {
                    entryCount++
                    val name = ze.name

                    if (name == "AndroidManifest.xml") {
                        hasManifest = true
                    } else if (name == "classes.dex" || name.startsWith("assets/")) {
                        hasClassesDexOrAssets = true
                    } else if (name.startsWith("META-INF/")) {
                        hasMetaInf = true
                        if (name.endsWith(".SF")) hasCertSf = true
                        if (name.endsWith(".RSA") || name.endsWith(".DSA") || name.endsWith(".EC")) hasCertRsa = true
                    } else if (name == "assets/app_meta.json") {
                        val content = zis.readBytes().toString(Charsets.UTF_8)
                        try {
                            val json = org.json.JSONObject(content)
                            parsedPkgName = json.optString("packageName")
                            parsedVersionName = json.optString("versionName")
                            parsedVersionCode = json.optInt("versionCode")
                        } catch (e: Exception) {
                            // ignore json parse error
                        }
                    }

                    ze = zis.nextEntry
                }
            }
        } catch (e: Exception) {
            errors.add("Validation Error: Invalid ZIP archive structure - ${e.localizedMessage}")
            return ApkValidationResult(isValid = false, errors = errors)
        }

        if (!hasManifest) {
            errors.add("Validation Error: Missing AndroidManifest.xml inside APK root archive")
        }

        if (!hasClassesDexOrAssets) {
            errors.add("Validation Error: Missing compiled classes.dex or web assets inside APK archive")
        }

        // 4. Verify Package Name match
        val actualPkg = parsedPkgName ?: project.packageName
        if (actualPkg != project.packageName) {
            errors.add("Validation Error: Package Name mismatch! Expected '${project.packageName}', found '$actualPkg'")
        }

        // 5. Verify Version Name match
        val actualVerName = parsedVersionName ?: project.version
        if (actualVerName != project.version) {
            errors.add("Validation Error: Version Name mismatch! Expected '${project.version}', found '$actualVerName'")
        }

        // 6. Verify Version Code match
        val actualVerCode = parsedVersionCode ?: project.versionCode
        if (actualVerCode != project.versionCode) {
            errors.add("Validation Error: Version Code mismatch! Expected '${project.versionCode}', found '$actualVerCode'")
        }

        // 7. Verify APK Signature presence
        val isSigned = hasMetaInf && hasCertSf && hasCertRsa
        if (!isSigned) {
            errors.add("Validation Error: APK signature verification failed! Missing cryptographic META-INF signature files (CERT.SF / CERT.RSA)")
        }

        // 8. Application Metadata Verification
        if (project.name.isBlank()) {
            errors.add("Validation Error: Application metadata missing valid app name")
        }

        val isValid = errors.isEmpty()

        val fileSizeFormatted = String.format(java.util.Locale.US, "%.2f KB", fileSize / 1024.0)
        val metadata = ApkMetadataInfo(
            packageName = actualPkg,
            versionName = actualVerName,
            versionCode = actualVerCode,
            entryCount = entryCount,
            isSigned = isSigned,
            fileSizeFormatted = fileSizeFormatted
        )

        return ApkValidationResult(
            isValid = isValid,
            errors = errors,
            warnings = warnings,
            metadata = metadata
        )
    }
}
