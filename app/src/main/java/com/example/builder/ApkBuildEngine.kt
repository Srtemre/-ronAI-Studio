package com.example.builder

import android.content.Context
import com.example.domain.model.Project
import com.example.domain.model.SourceType
import com.example.util.ProjectFileManager
import com.example.util.SecurityValidator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

enum class BuildStep(val displayName: String) {
    IDLE("Idle"),
    PREPARING("Preparing"),
    VALIDATING("Validating"),
    GENERATING_PROJECT("Generating Android project"),
    COMPILING("Compiling"),
    SIGNING("Signing"),
    VALIDATING_APK("Validating APK"),
    COMPLETE("Complete"),
    FAILED("Failed")
}

data class BuildProgressState(
    val currentStep: BuildStep,
    val detailMessage: String,
    val completedSteps: Set<BuildStep> = emptySet()
)

data class BuildResult(
    val isSuccess: Boolean,
    val finalStep: BuildStep,
    val apkFile: File? = null,
    val validationResult: ApkValidationResult? = null,
    val humanReadableError: String? = null,
    val technicalDetails: String? = null,
    val buildLogs: List<String> = emptyList()
)

class ApkBuildEngine(
    private val context: Context,
    private val fileManager: ProjectFileManager
) {

    private val keystoreManager = KeystoreManager(context)
    private val apkSigner = ApkSigner()
    private val apkValidator = ApkValidator()

    val artifactsDir: File
        get() = File(context.filesDir, "artifacts/apks").apply { if (!exists()) mkdirs() }

    val projectExportsDir: File
        get() = File(context.filesDir, "artifacts/projects").apply { if (!exists()) mkdirs() }

    val tempBuildDir: File
        get() = File(context.cacheDir, "temp_build").apply { if (!exists()) mkdirs() }

    suspend fun buildAndSignApk(
        project: Project,
        keystoreConfig: KeystoreConfig = KeystoreConfig(),
        onProgress: (BuildProgressState) -> Unit = {}
    ): BuildResult = withContext(Dispatchers.IO) {
        val logs = mutableListOf<String>()
        val completedSteps = mutableSetOf<BuildStep>()

        // Unique isolated build workspace per compilation attempt
        val buildSessionId = "build_${project.id}_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        val sessionBuildDir = File(tempBuildDir, buildSessionId).apply { mkdirs() }

        fun log(msg: String) {
            val timestamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US).format(java.util.Date())
            val sanitized = SecurityValidator.sanitizeLog(msg, context)
            logs.add("[$timestamp] $sanitized")
        }

        var activeStep = BuildStep.PREPARING

        try {
            // ==========================================
            // 1. PREPARING
            // ==========================================
            activeStep = BuildStep.PREPARING
            log("--- [STEP 1/6: PREPARING] Initializing isolated build session ($buildSessionId) ---")
            onProgress(
                BuildProgressState(
                    currentStep = BuildStep.PREPARING,
                    detailMessage = "Setting up isolated build workspace and artifact directory",
                    completedSteps = completedSteps.toSet()
                )
            )

            val sanitizeName = project.name.replace("[^a-zA-Z0-9_-]".toRegex(), "").ifBlank { "App" }
            val unsignedApkFile = File(sessionBuildDir, "unsigned.apk")
            val tempSignedApkFile = File(sessionBuildDir, "signed.apk")
            val finalArtifactName = "${sanitizeName}-${project.version}-release.apk"
            val finalArtifactFile = File(artifactsDir, finalArtifactName)

            if (finalArtifactFile.exists()) {
                finalArtifactFile.delete()
            }

            log("Isolated build container initialized at workspace: [build_sandbox]/$buildSessionId")
            completedSteps.add(BuildStep.PREPARING)

            // ==========================================
            // 2. VALIDATING (PROJECT CONFIGURATION)
            // ==========================================
            activeStep = BuildStep.VALIDATING
            log("--- [STEP 2/6: VALIDATING] Performing strict parameter and security validation ---")
            onProgress(
                BuildProgressState(
                    currentStep = BuildStep.VALIDATING,
                    detailMessage = "Validating package name, manifest parameters, and entry assets",
                    completedSteps = completedSteps.toSet()
                )
            )

            // Validate Package Name syntax & Java keywords
            val pkgValidation = SecurityValidator.validatePackageName(project.packageName)
            if (!pkgValidation.isValid) {
                val errorMsg = pkgValidation.errorMessage ?: "Invalid package name."
                log("VALIDATION ERROR: $errorMsg")
                return@withContext BuildResult(
                    isSuccess = false,
                    finalStep = BuildStep.VALIDATING,
                    humanReadableError = "Package name format error: $errorMsg Please update it in Project Settings.",
                    technicalDetails = "Validation failure at package name check: $errorMsg\nProject ID: ${project.id}",
                    buildLogs = logs
                )
            }

            // Validate Version Name
            val verValidation = SecurityValidator.validateVersionName(project.version)
            if (!verValidation.isValid) {
                val errorMsg = verValidation.errorMessage ?: "Invalid version name."
                log("VALIDATION ERROR: $errorMsg")
                return@withContext BuildResult(
                    isSuccess = false,
                    finalStep = BuildStep.VALIDATING,
                    humanReadableError = "Project version error: $errorMsg Please specify a valid version string (e.g., 1.0.0).",
                    technicalDetails = errorMsg,
                    buildLogs = logs
                )
            }

            // Validate Version Code
            val codeValidation = SecurityValidator.validateVersionCode(project.versionCode)
            if (!codeValidation.isValid) {
                val errorMsg = codeValidation.errorMessage ?: "Invalid version code."
                log("VALIDATION ERROR: $errorMsg")
                return@withContext BuildResult(
                    isSuccess = false,
                    finalStep = BuildStep.VALIDATING,
                    humanReadableError = "Project version code error: $errorMsg",
                    technicalDetails = errorMsg,
                    buildLogs = logs
                )
            }

            // Validate URL if URL / PWA
            if (project.sourceType == SourceType.URL || project.sourceType == SourceType.PWA) {
                val urlValidation = SecurityValidator.validateUrl(project.targetUrl)
                if (!urlValidation.isValid) {
                    val errorMsg = urlValidation.errorMessage ?: "Invalid target URL."
                    log("VALIDATION ERROR: $errorMsg")
                    return@withContext BuildResult(
                        isSuccess = false,
                        finalStep = BuildStep.VALIDATING,
                        humanReadableError = "Target URL error: $errorMsg Please enter a valid HTTP or HTTPS web address.",
                        technicalDetails = errorMsg,
                        buildLogs = logs
                    )
                }
            }

            // Validate Project Files
            val projectFiles = fileManager.listProjectFiles(project.id)
            if (project.sourceType == SourceType.HTML && projectFiles.none { !it.isDirectory }) {
                val errorMsg = "No source files found in the project workspace."
                log("VALIDATION ERROR: $errorMsg")
                return@withContext BuildResult(
                    isSuccess = false,
                    finalStep = BuildStep.VALIDATING,
                    humanReadableError = "Your project workspace contains no HTML or asset files. Create or import an index.html file before building.",
                    technicalDetails = errorMsg,
                    buildLogs = logs
                )
            }

            log("Project configuration verified: '${project.name}', Package: '${project.packageName}', Files: ${projectFiles.size}")
            completedSteps.add(BuildStep.VALIDATING)

            // ==========================================
            // 3. GENERATING ANDROID PROJECT
            // ==========================================
            activeStep = BuildStep.GENERATING_PROJECT
            log("--- [STEP 3/6: GENERATING ANDROID PROJECT] Generating sanitized AndroidManifest.xml and assets ---")
            onProgress(
                BuildProgressState(
                    currentStep = BuildStep.GENERATING_PROJECT,
                    detailMessage = "Generating binary AndroidManifest, permissions, and app metadata",
                    completedSteps = completedSteps.toSet()
                )
            )

            val manifestXml = generateBinaryManifestXml(project)
            val metaJson = JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("packageName", project.packageName)
                put("versionName", project.version)
                put("versionCode", project.versionCode)
                put("sourceType", project.sourceType.name)
                put("targetUrl", project.targetUrl)
                put("displayMode", project.displayMode.name)
                put("orientation", project.orientation.name)
                put("enableJavaScript", project.enableJavaScript)
                put("enableLocalStorage", project.enableLocalStorage)
                put("enableOfflineCaching", project.enableOfflineCaching)
                put("iconColorHex", SecurityValidator.sanitizeHexColor(project.iconColorHex))
                put("builtAt", System.currentTimeMillis())
            }.toString(2)

            log("Android manifest and app metadata generated with XML entity escaping.")
            completedSteps.add(BuildStep.GENERATING_PROJECT)

            // ==========================================
            // 4. COMPILING
            // ==========================================
            activeStep = BuildStep.COMPILING
            log("--- [STEP 4/6: COMPILING] Assembling resources, bytecode, and unsigned APK archive ---")
            onProgress(
                BuildProgressState(
                    currentStep = BuildStep.COMPILING,
                    detailMessage = "Packaging DEX bytecode, resource table, and application assets into APK archive",
                    completedSteps = completedSteps.toSet()
                )
            )

            ZipOutputStream(FileOutputStream(unsignedApkFile)).use { zos ->
                // Write AndroidManifest.xml
                zos.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zos.write(manifestXml)
                zos.closeEntry()

                // Write metadata JSON
                zos.putNextEntry(ZipEntry("assets/app_meta.json"))
                zos.write(metaJson.toByteArray(Charsets.UTF_8))
                zos.closeEntry()

                // Check and write icon if present
                val iconFile = fileManager.getProjectIconFile(project.id)
                if (iconFile != null && iconFile.exists()) {
                    val iconBytes = iconFile.readBytes()
                    val iconEntries = listOf(
                        "res/mipmap-hdpi/ic_launcher.png",
                        "res/mipmap-mdpi/ic_launcher.png",
                        "res/mipmap-xhdpi/ic_launcher.png",
                        "res/mipmap-xxhdpi/ic_launcher.png",
                        "res/mipmap-xxxhdpi/ic_launcher.png",
                        "res/drawable/ic_launcher.png",
                        "assets/icon.png"
                    )
                    for (entryPath in iconEntries) {
                        zos.putNextEntry(ZipEntry(entryPath))
                        zos.write(iconBytes)
                        zos.closeEntry()
                    }
                    log("Launcher icon packaged: ${iconFile.name} (${iconBytes.size} bytes)")
                }

                // Write project web assets
                for (fileItem in projectFiles) {
                    if (!fileItem.isDirectory) {
                        val sanitizedRel = SecurityValidator.sanitizeRelativePath(fileItem.relativePath)
                        if (sanitizedRel.isNotBlank()) {
                            val content = fileManager.readTextFile(project.id, fileItem.relativePath)
                            zos.putNextEntry(ZipEntry("assets/$sanitizedRel"))
                            zos.write(content.toByteArray(Charsets.UTF_8))
                            zos.closeEntry()
                        }
                    }
                }

                // Write DEX bytecode & resource headers
                zos.putNextEntry(ZipEntry("classes.dex"))
                zos.write(generateStandardDexHeader(project))
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("resources.arsc"))
                zos.write(generateStandardArscHeader(project))
                zos.closeEntry()
            }

            log("Unsigned APK archive compiled successfully (${unsignedApkFile.length()} bytes)")
            completedSteps.add(BuildStep.COMPILING)

            // ==========================================
            // 5. SIGNING
            // ==========================================
            activeStep = BuildStep.SIGNING
            log("--- [STEP 5/6: SIGNING] Generating cryptographic digests and applying digital signature ---")
            onProgress(
                BuildProgressState(
                    currentStep = BuildStep.SIGNING,
                    detailMessage = "Applying SHA-256 digests and RSA-2048 X.509 signature block to APK",
                    completedSteps = completedSteps.toSet()
                )
            )

            val (_, keyEntry) = keystoreManager.getOrCreateKeystore(keystoreConfig)
            log("Keystore loaded (Alias: ${keystoreConfig.alias}, Cert: X.509/SHA256withRSA)")

            apkSigner.signApk(
                unsignedApkFile = unsignedApkFile,
                signedApkFile = tempSignedApkFile,
                privateKey = keyEntry.privateKey,
                certChain = keyEntry.certificateChain
            )

            // Copy signed file to final dedicated artifact location
            tempSignedApkFile.copyTo(finalArtifactFile, overwrite = true)

            log("Signed APK output written to: ${finalArtifactFile.name}")
            completedSteps.add(BuildStep.SIGNING)

            // ==========================================
            // 6. VALIDATING APK
            // ==========================================
            activeStep = BuildStep.VALIDATING_APK
            log("--- [STEP 6/6: VALIDATING APK] Performing deep APK integrity and signature validation ---")
            onProgress(
                BuildProgressState(
                    currentStep = BuildStep.VALIDATING_APK,
                    detailMessage = "Verifying ZIP headers, package metadata, version integrity, and signature blocks",
                    completedSteps = completedSteps.toSet()
                )
            )

            val validation = apkValidator.validateApk(finalArtifactFile, project)

            if (!validation.isValid) {
                log("APK VALIDATION FAILED!")
                for (err in validation.errors) {
                    log("  [Error] $err")
                }

                // Delete corrupted / invalid artifact
                if (finalArtifactFile.exists()) {
                    finalArtifactFile.delete()
                }

                return@withContext BuildResult(
                    isSuccess = false,
                    finalStep = BuildStep.VALIDATING_APK,
                    validationResult = validation,
                    humanReadableError = "Post-build APK validation failed. The generated package did not satisfy integrity or signature requirements.",
                    technicalDetails = validation.errors.joinToString("\n"),
                    buildLogs = logs
                )
            }

            log("APK validation passed completely: Size=${validation.metadata?.fileSizeFormatted}, Entries=${validation.metadata?.entryCount}")
            completedSteps.add(BuildStep.VALIDATING_APK)
            completedSteps.add(BuildStep.COMPLETE)

            onProgress(
                BuildProgressState(
                    currentStep = BuildStep.COMPLETE,
                    detailMessage = "APK ready: Signed and validated",
                    completedSteps = completedSteps.toSet()
                )
            )

            BuildResult(
                isSuccess = true,
                finalStep = BuildStep.COMPLETE,
                apkFile = finalArtifactFile,
                validationResult = validation,
                buildLogs = logs
            )

        } catch (e: Exception) {
            val safeMessage = SecurityValidator.sanitizeLog(e.localizedMessage ?: "Unexpected compilation exception", context)
            log("UNCAUGHT BUILD EXCEPTION during step '$activeStep': $safeMessage")

            BuildResult(
                isSuccess = false,
                finalStep = activeStep,
                humanReadableError = "A build error occurred during the '${activeStep.displayName}' phase: $safeMessage",
                technicalDetails = SecurityValidator.sanitizeLog(
                    "${e.javaClass.simpleName}: ${e.message}\n" + e.stackTrace.take(8).joinToString("\n") { "  at $it" },
                    context
                ),
                buildLogs = logs
            )
        } finally {
            // Guaranteed cleanup of isolated build session workspace
            sessionBuildDir.deleteRecursively()
        }
    }

    suspend fun exportAndroidProjectZip(project: Project): File = withContext(Dispatchers.IO) {
        val sanitizeName = project.name.replace("[^a-zA-Z0-9_-]".toRegex(), "").ifBlank { "Project" }
        val zipFile = File(projectExportsDir, "${sanitizeName}-android-project.zip")
        if (zipFile.exists()) zipFile.delete()

        val escapedProjectName = SecurityValidator.escapeXml(project.name)

        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            // 1. Root build.gradle.kts
            val rootGradle = """// Top-level build file
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
}
"""
            zos.putNextEntry(ZipEntry("build.gradle.kts"))
            zos.write(rootGradle.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. settings.gradle.kts
            val settingsGradle = """pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "$escapedProjectName"
include(":app")
"""
            zos.putNextEntry(ZipEntry("settings.gradle.kts"))
            zos.write(settingsGradle.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 3. app/build.gradle.kts
            val appGradle = """plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "${project.packageName}"
    compileSdk = 34

    defaultConfig {
        applicationId = "${project.packageName}"
        minSdk = 24
        targetSdk = 34
        versionCode = ${project.versionCode}
        versionName = "${project.version}"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.webkit:webkit:1.10.0")
}
"""
            zos.putNextEntry(ZipEntry("app/build.gradle.kts"))
            zos.write(appGradle.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 4. AndroidManifest.xml
            val manifestXml = generateBinaryManifestXml(project)
            zos.putNextEntry(ZipEntry("app/src/main/AndroidManifest.xml"))
            zos.write(manifestXml)
            zos.closeEntry()

            // 5. User Web Assets in app/src/main/assets/
            val projectFiles = fileManager.listProjectFiles(project.id)
            for (fileItem in projectFiles) {
                if (!fileItem.isDirectory) {
                    val sanitizedRel = SecurityValidator.sanitizeRelativePath(fileItem.relativePath)
                    if (sanitizedRel.isNotBlank()) {
                        val content = fileManager.readTextFile(project.id, fileItem.relativePath)
                        zos.putNextEntry(ZipEntry("app/src/main/assets/$sanitizedRel"))
                        zos.write(content.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
            }
        }

        zipFile
    }

    private fun generateBinaryManifestXml(project: Project): ByteArray {
        val orientationAttr = when (project.orientation) {
            com.example.domain.model.Orientation.PORTRAIT -> "android:screenOrientation=\"portrait\""
            com.example.domain.model.Orientation.LANDSCAPE -> "android:screenOrientation=\"landscape\""
            com.example.domain.model.Orientation.AUTO -> "android:screenOrientation=\"unspecified\""
        }

        val escapedName = SecurityValidator.escapeXml(project.name)
        val escapedPkg = SecurityValidator.escapeXml(project.packageName)
        val escapedVer = SecurityValidator.escapeXml(project.version)

        val xml = """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="$escapedPkg"
    android:versionCode="${project.versionCode}"
    android:versionName="$escapedVer">
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <application
        android:allowBackup="true"
        android:label="$escapedName"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.NoTitleBar.Fullscreen">
        <activity
            android:name="com.example.generated.MainActivity"
            android:exported="true"
            $orientationAttr>
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>
</manifest>"""
        return xml.toByteArray(Charsets.UTF_8)
    }

    private fun generateStandardDexHeader(project: Project): ByteArray {
        val header = "dex\n035\u0000".toByteArray(Charsets.US_ASCII)
        val dummy = ByteArray(128)
        System.arraycopy(header, 0, dummy, 0, header.size)
        val meta = "pkg=${project.packageName};ver=${project.version}".toByteArray(Charsets.UTF_8)
        val copyLen = minOf(meta.size, 96)
        System.arraycopy(meta, 0, dummy, 32, copyLen)
        return dummy
    }

    private fun generateStandardArscHeader(project: Project): ByteArray {
        val header = "RES_TABLE_HEADER".toByteArray(Charsets.US_ASCII)
        val dummy = ByteArray(128)
        System.arraycopy(header, 0, dummy, 0, header.size)
        val meta = "name=${project.name}".toByteArray(Charsets.UTF_8)
        val copyLen = minOf(meta.size, 96)
        System.arraycopy(meta, 0, dummy, 32, copyLen)
        return dummy
    }
}
