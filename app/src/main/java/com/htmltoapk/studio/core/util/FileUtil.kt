package com.htmltoapk.studio.core.util

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.InputStream
import java.io.OutputStream

object FileUtil {

    fun copy(uri: Uri, dest: File, context: Context) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Cannot open input stream for $uri")
    }

    fun copyStream(input: InputStream, dest: File) {
        dest.outputStream().use { output -> input.copyTo(output) }
    }

    fun readText(uri: Uri, context: Context): String =
        context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            ?: error("Cannot read $uri")

    fun writeText(file: File, text: String) {
        file.parentFile?.mkdirs()
        file.writeText(text, Charsets.UTF_8)
    }

    fun writeStream(file: File, writer: (OutputStream) -> Unit) {
        file.parentFile?.mkdirs()
        file.outputStream().use(writer)
    }

    fun ensureDir(dir: File): File = dir.apply { if (!exists()) mkdirs() }

    fun deleteRecursivelySafe(file: File?): Boolean {
        file ?: return true
        return runCatching { file.deleteRecursively() }.getOrDefault(false)
    }

    fun humanReadableSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val value = bytes / Math.pow(1024.0, digitGroups.toDouble())
        return "%.1f %s".format(value, units[digitGroups.coerceIn(0, units.size - 1)])
    }
}
