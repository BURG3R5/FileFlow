package co.adityarajput.fileflow.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import co.adityarajput.fileflow.data.models.Rule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URLDecoder
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
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

    class SAFFile(val documentFile: DocumentFile) : File() {
        override suspend fun moveTo(
            destDir: File,
            destFileName: String,
            keepOriginal: Boolean,
            overwriteExisting: Boolean,
            context: Context,
        ) {
            if (destDir !is SAFFile)
                throw IllegalArgumentException("Destination directory must be a SAFFile")

            destDir.documentFile.findFile(destFileName)?.run {
                if (overwriteExisting) delete()
                else throw Exception("$destFileName already exists")
            }

            val destFile =
                destDir.documentFile.createFile(type ?: "application/octet-stream", destFileName)!!

            val resolver = context.contentResolver
            resolver.openInputStream(this.documentFile.uri).use { srcStream ->
                resolver.openOutputStream(destFile.uri).use { destStream ->
                    srcStream!!.copyTo(destStream!!)
                }
            }

            if (!keepOriginal) delete()
        }
    }

    class FSFile(val ioFile: IOFile) : File() {
        override suspend fun moveTo(
            destDir: File,
            destFileName: String,
            keepOriginal: Boolean,
            overwriteExisting: Boolean,
            context: Context,
        ) {
            if (destDir !is FSFile)
                throw IllegalArgumentException("Destination directory must be a FSFile")

            val options = mutableListOf<StandardCopyOption>()
            if (keepOriginal) options.add(StandardCopyOption.COPY_ATTRIBUTES)
            if (overwriteExisting) options.add(StandardCopyOption.REPLACE_EXISTING)

            try {
                withContext(Dispatchers.IO) {
                    if (keepOriginal) {
                        Files.copy(
                            this@FSFile.ioFile.toPath(),
                            destDir.ioFile.toPath().resolve(destFileName),
                            *options.toTypedArray(),
                        )
                    } else {
                        Files.move(
                            this@FSFile.ioFile.toPath(),
                            destDir.ioFile.toPath().resolve(destFileName),
                            *options.toTypedArray(),
                        )
                    }
                }
            } catch (e: Exception) {
                if (e is FileAlreadyExistsException) throw e

                Logger.w(
                    "Files",
                    "Failed to move file, falling back to create + copy [+ delete].",
                    e,
                )

                val destFile = IOFile(destDir.ioFile, destFileName)
                withContext(Dispatchers.IO) {
                    destFile.createNewFile()

                    this@FSFile.ioFile.inputStream().use { srcStream ->
                        destFile.outputStream().use { destStream ->
                            srcStream.copyTo(destStream)
                        }
                    }
                }

                if (!keepOriginal) delete()
            }
        }
    }

    val name
        get() = when (this) {
            is SAFFile -> documentFile.name
            is FSFile -> ioFile.name
        }

    val path: String
        get() = when (this) {
            is SAFFile -> documentFile.uri.toString()
            is FSFile -> ioFile.absolutePath
        }

    val parent
        get() = when (this) {
            is SAFFile -> documentFile.parentFile?.let { SAFFile(it) }
            is FSFile -> ioFile.parentFile?.let { FSFile(it) }
        }

    val isFile
        get() = when (this) {
            is SAFFile -> documentFile.isFile
            is FSFile -> ioFile.isFile
        }

    val isDirectory
        get() = when (this) {
            is SAFFile -> documentFile.isDirectory
            is FSFile -> ioFile.isDirectory
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

    fun pathRelativeTo(basePath: String) = path.getGetDirectoryFromUri()
        .substringAfter(basePath.getGetDirectoryFromUri(), "").ifBlank { null }

    fun listChildren(recurse: Boolean): List<File> {
        if (!isDirectory) return emptyList()

        if (!recurse) {
            return when (this) {
                is SAFFile -> documentFile.listFiles().map { SAFFile(it) }
                is FSFile -> ioFile.listFiles()?.map { FSFile(it) }.orEmpty()
            }
        }

        val files = mutableListOf<File>()
        listChildren(false).forEach {
            files.add(it)
            files.addAll(it.listChildren(true))
        }
        return files
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

    fun createDirectory(relativePath: String): File? {
        return when (this) {
            is SAFFile -> {
                val parts = relativePath.split('/').filter { it.isNotBlank() }
                var currentDir: DocumentFile = documentFile

                for (part in parts) {
                    val nextDir = currentDir.findFile(part)
                        ?: currentDir.createDirectory(part)

                    if (nextDir == null) {
                        Logger.e("Files", "Failed to create directory: $part")
                        return null
                    }

                    currentDir = nextDir
                }

                SAFFile(currentDir)
            }

            is FSFile -> IOFile(ioFile.path + '/' + relativePath).let {
                if (it.exists() || it.mkdirs()) FSFile(it) else null
            }
        }
    }

    abstract suspend fun moveTo(
        destDir: File,
        destFileName: String,
        keepOriginal: Boolean,
        overwriteExisting: Boolean,
        context: Context,
    )

    fun delete() = when (this) {
        is SAFFile -> documentFile.delete()
        is FSFile -> ioFile.delete()
    }
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

fun Context.findRulesToBeMigrated(rules: List<Rule>) =
    rules.filter { File.fromPath(this, it.action.src) == null }

fun Context.getAllStorages(): List<IOFile> {
    val storages = mutableListOf<IOFile>()

    try {
        getExternalFilesDirs(null).forEach {
            var storage = it
            var dir = it
            while (dir.parentFile != null) {
                dir = dir.parentFile!!
                if (dir.canRead())
                    storage = dir
            }
            storages.add(storage)
        }
    } catch (e: Exception) {
        Logger.e("Files", "Couldn't extract storages from external app directories", e)
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            (getSystemService(Context.STORAGE_SERVICE) as StorageManager).getStorageVolumes()
                .forEach {
                    if (it.directory != null && it.directory!!.canRead()) {
                        storages.add(it.directory!!)
                    }
                }
        } catch (e: Exception) {
            Logger.e("Files", "Couldn't extract storages from StorageManager", e)
        }
    }

    Logger.d("Files", "Found storages: ${storages.joinToString { it.absolutePath }}")

    return storages.distinct().apply {
        val primaryStorage = Environment.getExternalStorageDirectory()
        storages.remove(primaryStorage)
        storages.add(0, primaryStorage)
    }
}
