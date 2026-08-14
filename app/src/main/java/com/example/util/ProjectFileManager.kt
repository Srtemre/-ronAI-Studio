package com.example.util

import android.content.Context
import android.net.Uri
import com.example.domain.model.DisplayMode
import com.example.domain.model.Orientation
import com.example.domain.model.Project
import com.example.domain.model.SourceType
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

data class ProjectFileItem(
    val name: String,
    val relativePath: String,
    val isDirectory: Boolean,
    val sizeBytes: Long,
    val lastModified: Long,
    val extension: String
)

data class ZipImportResult(
    val success: Boolean,
    val importedFilesCount: Int,
    val entryHtmlPath: String?,
    val errorMessage: String? = null
)

class ProjectFileManager(val context: Context) {

    companion object {
        private const val MAX_ZIP_ENTRIES = 5000
        private const val MAX_TOTAL_UNCOMPRESSED_BYTES = 200 * 1024 * 1024L // 200 MB
        private const val MAX_SINGLE_FILE_BYTES = 50 * 1024 * 1024L // 50 MB
        private const val BUFFER_SIZE = 8192
    }

    private val baseProjectsDir: File
        get() {
            val dir = File(context.filesDir, "projects")
            if (!dir.exists()) {
                dir.mkdirs()
            }
            return dir
        }

    fun getProjectDir(projectId: Long): File {
        val safeId = if (projectId > 0) projectId else 0L
        val dir = File(baseProjectsDir, "proj_$safeId")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    private fun resolveSafeProjectFile(projectId: Long, rawRelativePath: String): File {
        val projectDir = getProjectDir(projectId)
        val sanitizedRel = SecurityValidator.sanitizeRelativePath(rawRelativePath)
        val targetFile = File(projectDir, sanitizedRel)

        if (!SecurityValidator.isPathSafe(projectDir, targetFile)) {
            throw SecurityException("Access denied: Invalid file path traversal detected.")
        }
        return targetFile
    }

    fun getEntryPointFile(projectId: Long, targetUrl: String = ""): File {
        val projectDir = getProjectDir(projectId)

        // 1. Check if targetUrl points to a file inside projectDir
        if (targetUrl.isNotBlank() && !targetUrl.startsWith("http://") && !targetUrl.startsWith("https://")) {
            val sanitized = SecurityValidator.sanitizeRelativePath(targetUrl)
            val file = File(projectDir, sanitized)
            if (SecurityValidator.isPathSafe(projectDir, file) && file.exists() && file.isFile) {
                return file
            }
        }

        // 2. Default index.html at root
        val rootIndex = File(projectDir, "index.html")
        if (rootIndex.exists()) return rootIndex

        // 3. Search for any index.html in subdirectories
        val foundIndex = findFileRecursively(projectDir, "index.html")
        if (foundIndex != null) return foundIndex

        // 4. Fallback to first html file found
        val anyHtml = listFilesRecursively(projectDir).firstOrNull {
            it.extension.equals("html", ignoreCase = true) || it.extension.equals("htm", ignoreCase = true)
        }
        if (anyHtml != null) {
            val resolved = File(projectDir, anyHtml.relativePath)
            if (SecurityValidator.isPathSafe(projectDir, resolved)) {
                return resolved
            }
        }

        return rootIndex
    }

    fun saveTextFile(projectId: Long, relativePath: String, content: String): File {
        val targetFile = resolveSafeProjectFile(projectId, relativePath)
        targetFile.parentFile?.mkdirs()
        targetFile.writeText(content, Charsets.UTF_8)
        return targetFile
    }

    fun readTextFile(projectId: Long, relativePath: String): String {
        return try {
            val targetFile = resolveSafeProjectFile(projectId, relativePath)
            if (targetFile.exists() && targetFile.isFile) {
                targetFile.readText(Charsets.UTF_8)
            } else {
                ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    fun saveBinaryFile(projectId: Long, relativePath: String, inputStream: InputStream): File {
        val targetFile = resolveSafeProjectFile(projectId, relativePath)
        targetFile.parentFile?.mkdirs()

        inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }

    fun saveBinaryFileBytes(projectId: Long, relativePath: String, bytes: ByteArray): File {
        val targetFile = resolveSafeProjectFile(projectId, relativePath)
        targetFile.parentFile?.mkdirs()
        targetFile.writeBytes(bytes)
        return targetFile
    }

    fun getProjectIconFile(projectId: Long): File? {
        val projectDir = getProjectDir(projectId)
        val candidateNames = listOf("icon.png", "icon.jpg", "icon.jpeg", "icon.webp", "app_icon.png")
        for (name in candidateNames) {
            val file = File(projectDir, name)
            if (file.exists() && file.isFile && file.length() > 0) {
                return file
            }
        }
        return null
    }

    fun saveProjectIcon(projectId: Long, inputStream: InputStream, fileName: String = "icon.png"): File {
        return saveBinaryFile(projectId, fileName, inputStream)
    }

    fun saveProjectIconBytes(projectId: Long, bytes: ByteArray, fileName: String = "icon.png"): File {
        return saveBinaryFileBytes(projectId, fileName, bytes)
    }

    fun deleteProjectIcon(projectId: Long): Boolean {
        var deleted = false
        val projectDir = getProjectDir(projectId)
        val candidateNames = listOf("icon.png", "icon.jpg", "icon.jpeg", "icon.webp", "app_icon.png")
        for (name in candidateNames) {
            val file = File(projectDir, name)
            if (file.exists() && file.isFile) {
                if (file.delete()) {
                    deleted = true
                }
            }
        }
        return deleted
    }

    fun hasProjectIcon(projectId: Long): Boolean {
        return getProjectIconFile(projectId) != null
    }

    fun saveTempIcon(inputStream: InputStream, extension: String = "png"): File {
        val tempDir = File(context.cacheDir, "temp_icons").apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDir, "temp_icon_${System.currentTimeMillis()}.${extension}")
        inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }
        return tempFile
    }

    fun saveTempIconBytes(bytes: ByteArray, extension: String = "png"): File {
        val tempDir = File(context.cacheDir, "temp_icons").apply { if (!exists()) mkdirs() }
        val tempFile = File(tempDir, "temp_icon_${System.currentTimeMillis()}.${extension}")
        tempFile.writeBytes(bytes)
        return tempFile
    }

    fun copyTempIconToProject(tempIconFile: File, projectId: Long): File? {
        if (!tempIconFile.exists()) return null
        val target = resolveSafeProjectFile(projectId, "icon.png")
        tempIconFile.copyTo(target, overwrite = true)
        return target
    }

    fun importZip(projectId: Long, inputStream: InputStream): ZipImportResult {
        val projectDir = getProjectDir(projectId)
        var fileCount = 0
        var totalBytesRead = 0L
        var detectedIndexRelativePath: String? = null

        val bufferedInput = BufferedInputStream(inputStream)
        bufferedInput.mark(16)

        // Validate ZIP magic header (0x50, 0x4B, 0x03, 0x04 or 0x50, 0x4B, 0x05, 0x06 for empty)
        val header = ByteArray(4)
        val bytesRead = bufferedInput.read(header)
        bufferedInput.reset()

        if (bytesRead < 4 || header[0] != 0x50.toByte() || header[1] != 0x4B.toByte()) {
            return ZipImportResult(
                success = false,
                importedFilesCount = 0,
                entryHtmlPath = null,
                errorMessage = "The selected file is not a valid ZIP archive format."
            )
        }

        try {
            ZipInputStream(bufferedInput).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry

                while (entry != null) {
                    fileCount++
                    if (fileCount > MAX_ZIP_ENTRIES) {
                        return ZipImportResult(
                            success = false,
                            importedFilesCount = fileCount,
                            entryHtmlPath = null,
                            errorMessage = "Security limit exceeded: ZIP archive contains more than $MAX_ZIP_ENTRIES files."
                        )
                    }

                    val rawName = entry.name
                    val sanitizedPath = SecurityValidator.sanitizeRelativePath(rawName)

                    if (sanitizedPath.isBlank() && !entry.isDirectory) {
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }

                    val targetFile = File(projectDir, sanitizedPath)

                    // Strict Zip Slip guard
                    if (!SecurityValidator.isPathSafe(projectDir, targetFile)) {
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()

                        var singleFileBytes = 0L
                        BufferedOutputStream(FileOutputStream(targetFile), BUFFER_SIZE).use { out ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var len: Int
                            while (zipIn.read(buffer).also { len = it } != -1) {
                                singleFileBytes += len
                                totalBytesRead += len

                                if (singleFileBytes > MAX_SINGLE_FILE_BYTES) {
                                    throw SecurityException("Individual file in ZIP exceeds size limit of ${MAX_SINGLE_FILE_BYTES / (1024 * 1024)} MB.")
                                }
                                if (totalBytesRead > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                                    throw SecurityException("Total uncompressed content exceeds size limit of ${MAX_TOTAL_UNCOMPRESSED_BYTES / (1024 * 1024)} MB.")
                                }

                                out.write(buffer, 0, len)
                            }
                        }

                        val fileName = targetFile.name.lowercase()
                        if (fileName == "index.html" || fileName == "index.htm") {
                            val rel = targetFile.relativeTo(projectDir).path.replace('\\', '/')
                            if (detectedIndexRelativePath == null || rel.length < detectedIndexRelativePath!!.length) {
                                detectedIndexRelativePath = rel
                            }
                        }
                    }

                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (fileCount == 0) {
                return ZipImportResult(
                    success = false,
                    importedFilesCount = 0,
                    entryHtmlPath = null,
                    errorMessage = "The ZIP archive is empty."
                )
            }

            return ZipImportResult(
                success = true,
                importedFilesCount = fileCount,
                entryHtmlPath = detectedIndexRelativePath
            )
        } catch (ze: ZipException) {
            return ZipImportResult(
                success = false,
                importedFilesCount = fileCount,
                entryHtmlPath = null,
                errorMessage = "Corrupted or password-protected ZIP archive: ${ze.localizedMessage}"
            )
        } catch (se: SecurityException) {
            return ZipImportResult(
                success = false,
                importedFilesCount = fileCount,
                entryHtmlPath = null,
                errorMessage = "Security validation failed: ${se.localizedMessage}"
            )
        } catch (e: Exception) {
            return ZipImportResult(
                success = false,
                importedFilesCount = fileCount,
                entryHtmlPath = null,
                errorMessage = "Failed to extract ZIP archive: ${e.localizedMessage ?: "I/O Error"}"
            )
        }
    }

    fun importSingleFileFromUri(projectId: Long, uri: Uri, customFileName: String? = null): String? {
        return try {
            val rawName = customFileName ?: getFileNameFromUri(uri) ?: "imported_file.html"
            val sanitizedName = SecurityValidator.sanitizeRelativePath(rawName).ifBlank { "imported_file.html" }
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            saveBinaryFile(projectId, sanitizedName, inputStream)
            sanitizedName
        } catch (e: Exception) {
            null
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val index = it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (index != -1) {
                            result = it.getString(index)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
        return result
    }

    fun listFilesRecursively(projectDir: File = baseProjectsDir): List<ProjectFileItem> {
        return listProjectFiles(projectDir, projectDir)
    }

    fun listProjectFiles(projectId: Long): List<ProjectFileItem> {
        val projectDir = getProjectDir(projectId)
        return listProjectFiles(projectDir, projectDir)
    }

    private fun listProjectFiles(projectDir: File, currentDir: File): List<ProjectFileItem> {
        val result = mutableListOf<ProjectFileItem>()
        val files = currentDir.listFiles() ?: return emptyList()

        for (file in files) {
            // Ensure no symlink escapes outside projectDir
            if (!SecurityValidator.isPathSafe(projectDir, file)) continue

            val relPath = file.relativeTo(projectDir).path.replace('\\', '/')
            val ext = file.extension
            result.add(
                ProjectFileItem(
                    name = file.name,
                    relativePath = relPath,
                    isDirectory = file.isDirectory,
                    sizeBytes = if (file.isFile) file.length() else 0L,
                    lastModified = file.lastModified(),
                    extension = ext
                )
            )
            if (file.isDirectory) {
                result.addAll(listProjectFiles(projectDir, file))
            }
        }

        return result.sortedWith(compareBy({ !it.isDirectory }, { it.relativePath }))
    }

    fun deleteFile(projectId: Long, relativePath: String): Boolean {
        return try {
            val targetFile = resolveSafeProjectFile(projectId, relativePath)
            if (targetFile.exists()) {
                targetFile.deleteRecursively()
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    fun duplicateProjectFiles(sourceProjectId: Long, targetProjectId: Long) {
        val sourceDir = getProjectDir(sourceProjectId)
        val targetDir = getProjectDir(targetProjectId)
        if (sourceDir.exists() && SecurityValidator.isPathSafe(baseProjectsDir, sourceDir) && SecurityValidator.isPathSafe(baseProjectsDir, targetDir)) {
            sourceDir.copyRecursively(targetDir, overwrite = true)
        }
    }

    fun deleteProjectDirectory(projectId: Long) {
        val projectDir = getProjectDir(projectId)
        if (projectDir.exists() && SecurityValidator.isPathSafe(baseProjectsDir, projectDir)) {
            projectDir.deleteRecursively()
        }
    }

    fun deleteAllProjectsData() {
        if (baseProjectsDir.exists()) {
            baseProjectsDir.deleteRecursively()
        }
    }

    fun exportAllProjectsToZip(projects: List<Project>): File {
        val exportDir = File(context.cacheDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()
        val zipFile = File(exportDir, "html_app_builder_backup_${System.currentTimeMillis()}.zip")
        if (zipFile.exists()) zipFile.delete()

        val jsonArray = JSONArray()
        for (project in projects) {
            val obj = JSONObject().apply {
                put("id", project.id)
                put("name", project.name)
                put("packageName", project.packageName)
                put("version", project.version)
                put("versionCode", project.versionCode)
                put("sourceType", project.sourceType.name)
                put("targetUrl", project.targetUrl)
                put("htmlContent", project.htmlContent)
                put("displayMode", project.displayMode.name)
                put("orientation", project.orientation.name)
                put("enableJavaScript", project.enableJavaScript)
                put("enableLocalStorage", project.enableLocalStorage)
                put("enableOfflineCaching", project.enableOfflineCaching)
                put("iconColorHex", project.iconColorHex)
                put("createdDate", project.createdDate)
                put("lastModified", project.lastModified)
            }
            jsonArray.put(obj)
        }

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zipOut ->
            // 1. Write manifest.json
            val manifestEntry = ZipEntry("projects_manifest.json")
            zipOut.putNextEntry(manifestEntry)
            zipOut.write(jsonArray.toString(2).toByteArray(Charsets.UTF_8))
            zipOut.closeEntry()

            // 2. Add files for each project
            for (project in projects) {
                val projDir = getProjectDir(project.id)
                if (projDir.exists() && projDir.isDirectory) {
                    val files = listFilesRecursively(projDir)
                    for (item in files) {
                        if (!item.isDirectory) {
                            val srcFile = File(projDir, item.relativePath)
                            if (srcFile.exists() && SecurityValidator.isPathSafe(projDir, srcFile)) {
                                val sanitizedRel = SecurityValidator.sanitizeRelativePath(item.relativePath)
                                val entryPath = "projects_data/proj_${project.id}/$sanitizedRel"
                                val entry = ZipEntry(entryPath)
                                zipOut.putNextEntry(entry)
                                FileInputStream(srcFile).use { inStream ->
                                    inStream.copyTo(zipOut)
                                }
                                zipOut.closeEntry()
                            }
                        }
                    }
                }
            }
        }

        return zipFile
    }

    data class ExtractedBackup(
        val projects: List<Project>,
        val tempDir: File
    )

    fun readProjectsFromBackupZip(inputStream: InputStream): ExtractedBackup? {
        val tempExtractDir = File(context.cacheDir, "import_temp_${System.currentTimeMillis()}")
        tempExtractDir.mkdirs()

        var manifestJsonString: String? = null

        try {
            ZipInputStream(BufferedInputStream(inputStream)).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                var entryCount = 0
                var totalBytes = 0L

                while (entry != null) {
                    entryCount++
                    if (entryCount > MAX_ZIP_ENTRIES) {
                        throw SecurityException("Backup contains excessive number of entries.")
                    }

                    val rawName = entry.name
                    val sanitizedPath = SecurityValidator.sanitizeRelativePath(rawName)
                    val targetFile = File(tempExtractDir, sanitizedPath)

                    if (!SecurityValidator.isPathSafe(tempExtractDir, targetFile)) {
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                        continue
                    }

                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        targetFile.parentFile?.mkdirs()
                        if (sanitizedPath == "projects_manifest.json") {
                            val baos = ByteArrayOutputStream()
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read: Int
                            while (zipIn.read(buffer).also { read = it } != -1) {
                                baos.write(buffer, 0, read)
                            }
                            manifestJsonString = baos.toString("UTF-8")
                        } else {
                            FileOutputStream(targetFile).use { out ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var read: Int
                                while (zipIn.read(buffer).also { read = it } != -1) {
                                    totalBytes += read
                                    if (totalBytes > MAX_TOTAL_UNCOMPRESSED_BYTES) {
                                        throw SecurityException("Backup total uncompressed size exceeds limit.")
                                    }
                                    out.write(buffer, 0, read)
                                }
                            }
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }

            if (manifestJsonString.isNullOrBlank()) {
                tempExtractDir.deleteRecursively()
                return null
            }

            val jsonArray = JSONArray(manifestJsonString)
            val projects = mutableListOf<Project>()
            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)
                    val rawPkg = obj.optString("packageName", "com.app.imported")
                    val validPkg = if (SecurityValidator.validatePackageName(rawPkg).isValid) rawPkg else "com.app.imported${i + 1}"
                    val rawColor = obj.optString("iconColorHex", "#007AFF")
                    val validColor = SecurityValidator.sanitizeHexColor(rawColor)

                    val project = Project(
                        id = obj.optLong("id", 0L),
                        name = obj.optString("name", "Imported Project").take(100),
                        packageName = validPkg,
                        version = obj.optString("version", "1.0.0").take(32),
                        versionCode = obj.optInt("versionCode", 1).coerceIn(1, 2_100_000_000),
                        sourceType = runCatching { SourceType.valueOf(obj.optString("sourceType", "URL")) }.getOrDefault(SourceType.URL),
                        targetUrl = obj.optString("targetUrl", ""),
                        htmlContent = obj.optString("htmlContent", ""),
                        displayMode = runCatching { DisplayMode.valueOf(obj.optString("displayMode", "STANDALONE")) }.getOrDefault(DisplayMode.STANDALONE),
                        orientation = runCatching { Orientation.valueOf(obj.optString("orientation", "PORTRAIT")) }.getOrDefault(Orientation.PORTRAIT),
                        enableJavaScript = obj.optBoolean("enableJavaScript", true),
                        enableLocalStorage = obj.optBoolean("enableLocalStorage", true),
                        enableOfflineCaching = obj.optBoolean("enableOfflineCaching", true),
                        iconColorHex = validColor,
                        createdDate = obj.optLong("createdDate", System.currentTimeMillis()),
                        lastModified = obj.optLong("lastModified", System.currentTimeMillis())
                    )
                    projects.add(project)
                } catch (_: Exception) {
                    // Skip single malformed entry gracefully
                }
            }

            return ExtractedBackup(projects = projects, tempDir = tempExtractDir)
        } catch (e: Exception) {
            tempExtractDir.deleteRecursively()
            return null
        }
    }

    private fun findFileRecursively(dir: File, targetName: String): File? {
        val files = dir.listFiles() ?: return null
        for (file in files) {
            if (file.isFile && file.name.equals(targetName, ignoreCase = true)) {
                return file
            }
            if (file.isDirectory) {
                val found = findFileRecursively(file, targetName)
                if (found != null) return found
            }
        }
        return null
    }
}
