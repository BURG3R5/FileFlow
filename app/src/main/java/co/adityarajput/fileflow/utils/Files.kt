package co.adityarajput.fileflow.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import java.net.URLDecoder
import java.io.File as IOFile

sealed class File {
    companion object {
        fun fromPath(context: Context, path: String): File? {
            try {
                if (context.isGranted(Permission.MANAGE_EXTERNAL_STORAGE)) {
                    val ioFile = IOFile(path)
                    if (ioFile.exists())
                        return FSFile(ioFile)
                } else {
                    val docFile = DocumentFile.fromTreeUri(context, path.toUri())
                    if (docFile != null && docFile.exists())
                        return SAFFile(docFile)
                }
            } catch (e: Exception) {
                Logger.e("Files", "Error while creating file from path: $path", e)
            }

            return null
        }
    }

    class SAFFile(val documentFile: DocumentFile) : File()

    class FSFile(val ioFile: IOFile) : File()

    val name
        get() = when (this) {
            is SAFFile -> documentFile.name
            is FSFile -> ioFile.name
        }

    val isFile
        get() = when (this) {
            is SAFFile -> documentFile.isFile
            is FSFile -> ioFile.isFile
        }

    val type
        get() = when (this) {
            is SAFFile -> documentFile.type
            is FSFile -> null
        }

    fun lastModified() = when (this) {
        is SAFFile -> documentFile.lastModified()
        is FSFile -> ioFile.lastModified()
    }

    fun length() = when (this) {
        is SAFFile -> documentFile.length()
        is FSFile -> ioFile.length()
    }

    fun listFiles() = when (this) {
        is SAFFile -> documentFile.listFiles().map { SAFFile(it) }
        is FSFile -> ioFile.listFiles()?.map { FSFile(it) } ?: emptyList()
    }

    fun isIdenticalTo(other: File, context: Context): Boolean {
        val resolver = context.contentResolver

        if (this is FSFile && other is FSFile) {
            return ioFile.readBytes().contentEquals(other.ioFile.readBytes())
        } else if (this is SAFFile && other is SAFFile) {
            resolver.openInputStream(documentFile.uri).use { src ->
                resolver.openInputStream(other.documentFile.uri).use { dest ->
                    if (src == null || dest == null) {
                        Logger.e("Files", "Failed to open file(s)")
                        return false
                    }
                    return src.readBytes().contentEquals(dest.readBytes())
                }
            }
        }

        return false
    }

    fun createFile(type: String?, name: String): File? {
        return when (this) {
            is SAFFile -> documentFile
                .createFile(type ?: "application/octet-stream", name)
                ?.let { SAFFile(it) }

            is FSFile -> IOFile(ioFile, name)
                .let { if (it.createNewFile()) FSFile(it) else null }
        }
    }

    fun delete() = when (this) {
        is SAFFile -> documentFile.delete()
        is FSFile -> ioFile.delete()
    }
}

fun Context.copyFile(src: File, dest: File): Boolean {
    val resolver = contentResolver

    if (src is File.SAFFile && dest is File.SAFFile) {
        resolver.openInputStream(src.documentFile.uri).use { srcStream ->
            resolver.openOutputStream(dest.documentFile.uri).use { destStream ->
                if (srcStream == null || destStream == null) {
                    Logger.e("Files", "Failed to open file(s)")
                    return false
                }
                Logger.i("Files", "Copying ${src.name} to ${dest.name}")
                srcStream.copyTo(destStream)
                return true
            }
        }
    } else if (src is File.FSFile && dest is File.FSFile) {
        src.ioFile.inputStream().use { srcStream ->
            dest.ioFile.outputStream().use { destStream ->
                Logger.i("Files", "Copying ${src.name} to ${dest.name}")
                srcStream.copyTo(destStream)
                return true
            }
        }
    }

    return false
}

fun String.getGetDirectoryFromUri() =
    if (isBlank()) {
        this
    } else if (this.contains(':')) {
        "/" + URLDecoder.decode(this, "UTF-8").split(":").last()
    } else {
        var file = IOFile(this)
        while (file.parentFile?.canRead() ?: false) file = file.parentFile!!

        substringAfter(file.name ?: "").ifBlank { "/" }
    }
