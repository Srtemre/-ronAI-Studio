package com.htmltoapk.studio.core.util

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract

/**
 * Walks a SAF document tree (returned by ACTION_OPEN_DOCUMENT_TREE) and yields
 * (relativePath, InputStream) pairs for every regular file inside the tree.
 */
object DocumentTreeHelper {

    fun listFiles(treeUri: Uri, context: Context): List<Pair<String, java.io.InputStream>> {
        val resolver = context.contentResolver
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri)
        )
        val results = mutableListOf<Pair<String, java.io.InputStream>>()
        walk(resolver, treeUri, children, "", results)
        return results
    }

    private fun walk(
        resolver: android.content.ContentResolver,
        treeUri: Uri,
        childrenUri: Uri,
        prefix: String,
        out: MutableList<Pair<String, java.io.InputStream>>
    ) {
        val cursor = resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME
            ),
            null, null, null
        ) ?: return

        cursor.use { c ->
            while (c.moveToNext()) {
                val docId = c.getString(0)
                val mime = c.getString(1)
                val name = c.getString(2) ?: continue
                val isDir = mime == DocumentsContract.Document.MIME_TYPE_DIR
                val relPath = if (prefix.isEmpty()) name else "$prefix/$name"
                if (isDir) {
                    val next = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
                    walk(resolver, treeUri, next, relPath, out)
                } else {
                    val fileUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, docId)
                    val stream = resolver.openInputStream(fileUri) ?: continue
                    out.add(relPath to stream)
                }
            }
        }
    }
}
