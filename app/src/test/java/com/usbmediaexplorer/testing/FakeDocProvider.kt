package com.usbmediaexplorer.testing

import android.net.Uri
import android.os.ParcelFileDescriptor
import com.usbmediaexplorer.data.doc.DocNode
import com.usbmediaexplorer.data.doc.DocProvider
import com.usbmediaexplorer.data.doc.MediaCount
import java.io.InputStream
import java.io.OutputStream

/** Deterministic in-memory provider for USB/SAF ViewModel contract tests. */
class FakeDocProvider(nodes: List<DocNode>) : DocProvider {
    private val entries = nodes.associateBy { it.uri.toString() }
    private val childrenByParent = nodes.groupBy { it.displayPath.substringBeforeLast("/", "") }

    override fun supports(uri: Uri) = true
    override suspend fun node(uri: Uri): DocNode? = entries[uri.toString()]
    override suspend fun children(node: DocNode): List<DocNode> = childrenByParent[node.displayPath].orEmpty()
    override suspend fun parentOf(node: DocNode): DocNode? = entries.values.firstOrNull { it.displayPath == node.displayPath.substringBeforeLast("/", "") }
    override suspend fun childByName(node: DocNode, name: String): DocNode? = children(node).firstOrNull { it.name == name }
    override suspend fun exists(uri: Uri) = entries.containsKey(uri.toString())
    override suspend fun mediaChildren(node: DocNode, limit: Int): List<DocNode> = children(node).filter { it.kind.isMedia }.take(limit)
    override suspend fun mediaCount(node: DocNode): MediaCount = children(node).fold(MediaCount()) { count, child ->
        when (child.kind) {
            com.usbmediaexplorer.data.doc.MediaKind.VIDEO -> count.copy(videos = count.videos + 1)
            com.usbmediaexplorer.data.doc.MediaKind.IMAGE -> count.copy(images = count.images + 1)
            com.usbmediaexplorer.data.doc.MediaKind.AUDIO -> count.copy(audios = count.audios + 1)
            com.usbmediaexplorer.data.doc.MediaKind.DIRECTORY -> count.copy(folders = count.folders + 1)
            else -> count.copy(others = count.others + 1)
        }
    }
    override suspend fun directorySize(node: DocNode) = children(node).sumOf { it.size.coerceAtLeast(0) }
    override fun openInput(uri: Uri): InputStream? = null
    override fun openOutput(uri: Uri, append: Boolean): OutputStream? = null
    override fun openFd(uri: Uri, mode: String): ParcelFileDescriptor? = null
    override suspend fun createDirectory(parent: DocNode, name: String): DocNode? = null
    override suspend fun createFile(parent: DocNode, name: String, mimeType: String?): DocNode? = null
    override suspend fun rename(node: DocNode, newName: String): DocNode? = null
    override suspend fun delete(node: DocNode) = false
    override suspend fun deleteRecursive(node: DocNode) = false
    override suspend fun moveTo(node: DocNode, targetParent: DocNode): DocNode? = null
    override fun freeBytes(node: DocNode): Long? = 0L
    override fun totalBytes(node: DocNode): Long? = 0L
    override fun fileSystemLabel(node: DocNode): String? = "fake"
}
