package com.htmltoapk.studio.importer

import android.content.Context
import android.net.Uri
import com.htmltoapk.studio.core.di.IoDispatcher
import com.htmltoapk.studio.core.util.FileUtil
import com.htmltoapk.studio.data.model.SourceType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.zip.ZipFile
import org.jsoup.Jsoup
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface SourceImporter {
    suspend fun import(
        type: SourceType,
        sourceUri: String,
        destDir: File
    ): ImportResult
}

data class ImportResult(
    val entryHtml: File,
    val assetsDir: File,
    val title: String? = null,
    val warnings: List<String> = emptyList()
)

@Singleton
class SourceImporterImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher
) : SourceImporter {

    override suspend fun import(
        type: SourceType,
        sourceUri: String,
        destDir: File
    ): ImportResult = withContext(io) {
        FileUtil.ensureDir(destDir)
        when (type) {
            SourceType.HTML_FILE -> importHtmlFile(Uri.parse(sourceUri), destDir)
            SourceType.ZIP -> importZip(Uri.parse(sourceUri), destDir)
            SourceType.FOLDER -> importFolder(Uri.parse(sourceUri), destDir)
            SourceType.URL -> importUrl(sourceUri, destDir)
            SourceType.PASTE_HTML -> importPaste(sourceUri, destDir)
        }
    }

    private fun importHtmlFile(uri: Uri, destDir: File): ImportResult {
        val text = FileUtil.readText(uri, context)
        if (text.isBlank()) error("HTML content is empty")
        val assets = File(destDir, "assets").apply { mkdirs() }
        val entry = File(assets, "index.html")
        FileUtil.writeText(entry, text)
        val title = runCatching { Jsoup.parse(text).title() }.getOrNull()
        return ImportResult(entry, assets, title)
    }

    private fun importZip(uri: Uri, destDir: File): ImportResult {
        val zipTmp = File(destDir, "_source.zip")
        FileUtil.copy(uri, zipTmp, context)
        if (!zipTmp.exists() || zipTmp.length() == 0L) error("Invalid ZIP archive")
        val assets = File(destDir, "assets").apply { mkdirs() }
        val assetsRoot = assets.canonicalPath
        var entryHtml: File? = null
        var title: String? = null
        ZipFile(zipTmp).use { zf ->
            val entries = zf.entries
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory) continue
                val out = File(assets, entry.name)
                // Zip-slip guard: refuse entries that escape the assets root.
                if (!out.canonicalPath.startsWith(assetsRoot + File.separator)) continue
                out.parentFile?.mkdirs()
                zf.getInputStream(entry).use { input ->
                    out.outputStream().use { output -> input.copyTo(output) }
                }
                if (entryHtml == null && entry.name.lowercase().endsWith(".html")) {
                    entryHtml = out
                    title = runCatching { Jsoup.parse(out.readText()).title() }.getOrNull()
                }
            }
        }
        zipTmp.delete()
        val html = entryHtml ?: error("No HTML file found in ZIP archive")
        return ImportResult(html, assets, title)
    }

    private fun importFolder(uri: Uri, destDir: File): ImportResult {
        val assets = File(destDir, "assets").apply { mkdirs() }
        val pickedDir = com.htmltoapk.studio.core.util.DocumentTreeHelper.listFiles(uri, context)
        if (pickedDir.isEmpty()) error("Folder is empty or cannot be accessed")
        var entryHtml: File? = null
        var title: String? = null
        val assetsRoot = assets.canonicalPath
        pickedDir.forEach { (name, input) ->
            input.use { stream ->
                val out = File(assets, name)
                if (!out.canonicalPath.startsWith(assetsRoot + File.separator)) return@use
                out.parentFile?.mkdirs()
                out.outputStream().use { output -> stream.copyTo(output) }
                if (entryHtml == null && name.lowercase().endsWith(".html")) {
                    entryHtml = out
                    title = runCatching { Jsoup.parse(out.readText()).title() }.getOrNull()
                }
            }
        }
        val html = entryHtml ?: error("No HTML file found in folder")
        return ImportResult(html, assets, title)
    }

    private fun importUrl(url: String, destDir: File): ImportResult {
        val assets = File(destDir, "assets").apply { mkdirs() }
        val entry = File(assets, "index.html")
        val doc = runCatching {
            Jsoup.connect(url)
                .userAgent("Mozilla/5.0 (Linux; Android) HTMLToApkStudio/1.0")
                .timeout(20_000)
                .maxBodySize(20 * 1024 * 1024)
                .get()
        }.getOrElse { error("Network error: ${it.message}") }

        // Make absolute URLs
        doc.select("a[href],link[href],script[src],img[src]").forEach { el ->
            val attr = if (el.hasAttr("href")) "href" else "src"
            val abs = el.absUrl(attr)
            if (abs.isNotEmpty()) el.attr(attr, abs)
        }
        FileUtil.writeText(entry, doc.outerHtml())
        return ImportResult(entry, assets, doc.title().ifBlank { null })
    }

    private fun importPaste(rawHtml: String, destDir: File): ImportResult {
        if (rawHtml.isBlank()) error("HTML content is empty")
        val assets = File(destDir, "assets").apply { mkdirs() }
        val entry = File(assets, "index.html")
        FileUtil.writeText(entry, rawHtml)
        val title = runCatching { Jsoup.parse(rawHtml).title() }.getOrNull()
        return ImportResult(entry, assets, title)
    }
}
