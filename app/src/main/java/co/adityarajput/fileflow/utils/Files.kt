package co.adityarajput.fileflow.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import co.adityarajput.fileflow.data.models.RemoteAction
import co.adityarajput.fileflow.data.models.Rule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.URLDecoder
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.io.File as IOFile

sealed class File {
    companion object {
        fun fromPath(context: Context, path: String): File? {
            try {
                return if (context.isGranted(Permission.MANAGE_EXTERNAL_STORAGE)) {
                    IOFile(path).let {
                        if (it.exists()) FSFile(it) else null
                    }
                } else {
                    DocumentFile.fromTreeUri(context, path.toUri())?.let {
                        if (it.exists()) SAFFile(it) else null
                    }
                }
            } catch (e: Exception) {
                Logger.e("Files", "Error while creating file from path: $path", e)
            }

            return null
        }
    }

    class SAFFile(val documentFile: DocumentFile) : File() {
        override val name get() = documentFile.name

        override val extension get() = documentFile.name?.substringAfterLast('.', "").orEmpty()

        override val path get() = documentFile.uri.toString()

        override val isFile get() = documentFile.isFile

        override val isDirectory get() = documentFile.isDirectory

        override val parent get() = documentFile.parentFile?.let { SAFFile(it) }

        override val lastModified get() = documentFile.lastModified()

        override val length get() = documentFile.length()

        override fun isIdenticalTo(other: File, context: Context): Boolean {
            if (other !is SAFFile) return false

            this.getInputStream(context).use { src ->
                other.getInputStream(context).use { dest ->
                    if (src == null || dest == null) {
                        Logger.e("Files", "Failed to open file(s)")
                        return false
                    }
                    return src.readBytes().contentEquals(dest.readBytes())
                }
            }
        }

        override fun listDirectChildren() =
            documentFile.listFiles().map { SAFFile(it) }

        override fun createFile(name: String, mimeType: String) =
            documentFile.createFile(mimeType, name)?.let { SAFFile(it) }

        override fun createDirectory(relativePath: String): File? {
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

            return SAFFile(currentDir)
        }

        override fun getInputStream(context: Context) =
            context.contentResolver.openInputStream(documentFile.uri)

        override fun getOutputStream(context: Context) =
            context.contentResolver.openOutputStream(documentFile.uri)

        override suspend fun moveTo(
            destDir: File,
            destFileName: String,
            keepOriginal: Boolean,
            overwriteExisting: Boolean,
            context: Context,
        ): String? {
            if (destDir !is SAFFile)
                throw IllegalArgumentException("Destination directory must be a SAFFile")

            destDir.documentFile.findFile(destFileName)?.run {
                if (overwriteExisting) delete()
                else throw Exception("$destFileName already exists")
            }

            val destFile =
                destDir.documentFile.createFile(
                    documentFile.type ?: "application/octet-stream",
                    destFileName,
                )!!

            val resolver = context.contentResolver
            resolver.openInputStream(this.documentFile.uri).use { srcStream ->
                resolver.openOutputStream(destFile.uri).use { destStream ->
                    srcStream!!.copyTo(destStream!!)
                }
            }

            if (!keepOriginal) delete()

            return null
        }

        override fun delete() = documentFile.delete()
    }

    class FSFile(val ioFile: IOFile) : File() {
        override val name: String? get() = ioFile.name

        override val extension get() = ioFile.extension

        override val path: String get() = ioFile.absolutePath

        override val isFile get() = ioFile.isFile

        override val isDirectory get() = ioFile.isDirectory

        override val parent: File? get() = ioFile.parentFile?.let { FSFile(it) }

        override val lastModified get() = ioFile.lastModified()

        override val length get() = ioFile.length()

        override fun getInputStream(context: Context) = ioFile.inputStream()

        override fun getOutputStream(context: Context) = ioFile.outputStream()

        override fun isIdenticalTo(other: File, context: Context) =
            other is FSFile && ioFile.readBytes().contentEquals(other.ioFile.readBytes())

        override fun listDirectChildren() = ioFile.listFiles()?.map { FSFile(it) }.orEmpty()

        override fun createFile(name: String, mimeType: String) =
            IOFile(ioFile, name).let {
                if (it.createNewFile()) FSFile(it) else null
            }

        override fun createDirectory(relativePath: String) =
            IOFile(ioFile.path + '/' + relativePath).let {
                if (it.exists() || it.mkdirs()) FSFile(it) else null
            }

        override suspend fun moveTo(
            destDir: File,
            destFileName: String,
            keepOriginal: Boolean,
            overwriteExisting: Boolean,
            context: Context,
        ): String? {
            if (destDir !is FSFile)
                throw IllegalArgumentException("Destination directory must be a FSFile")

            val options = mutableListOf<StandardCopyOption>()
            if (keepOriginal) options.add(StandardCopyOption.COPY_ATTRIBUTES)
            if (overwriteExisting) options.add(StandardCopyOption.REPLACE_EXISTING)
            var destFilePath: String? = null

            try {
                withContext(Dispatchers.IO) {
                    destFilePath = destDir.ioFile.toPath().resolve(destFileName).toString()
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

                withContext(Dispatchers.IO) {
                    val destFile = IOFile(destDir.ioFile, destFileName)

                    destFile.createNewFile()
                    destFilePath = destFile.absolutePath

                    this@FSFile.ioFile.inputStream().use { srcStream ->
                        destFile.outputStream().use { destStream ->
                            srcStream.copyTo(destStream)
                        }
                    }
                }

                if (!keepOriginal) delete()
            }

            return destFilePath
        }

        override fun delete() = ioFile.delete()
    }

    abstract val name: String?

    abstract val extension: String

    abstract val path: String

    abstract val isFile: Boolean

    abstract val isDirectory: Boolean

    abstract val parent: File?

    abstract val lastModified: Long

    abstract val length: Long

    fun pathRelativeTo(basePath: String): String? =
        if (isDirectory) {
            path.getDirectoryFromUri()
                .substringAfter(basePath.getDirectoryFromUri(), "")
                .ifBlank { null }
        } else {
            parent?.pathRelativeTo(basePath)
                ?.takeIf { it.isNotEmpty() && name != null }
                .let {
                    if (it == null) name
                    else "${it.removePrefix("/").removeSuffix("/")}/$name"
                }
        }

    abstract fun isIdenticalTo(other: File, context: Context): Boolean

    abstract fun listDirectChildren(): List<File>

    fun listChildren(recurse: Boolean): List<File> {
        if (!isDirectory) return emptyList()

        if (!recurse) return listDirectChildren()

        val files = mutableListOf<File>()
        listDirectChildren().forEach {
            files.add(it)
            files.addAll(it.listChildren(true))
        }
        return files
    }

    abstract fun createFile(name: String, mimeType: String): File?

    abstract fun createDirectory(relativePath: String): File?

    abstract fun getInputStream(context: Context): InputStream?

    abstract fun getOutputStream(context: Context): OutputStream?

    abstract suspend fun moveTo(
        destDir: File,
        destFileName: String,
        keepOriginal: Boolean,
        overwriteExisting: Boolean,
        context: Context,
    ): String?

    abstract fun delete(): Boolean
}

fun String.getDirectoryFromUri() =
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
    rules.filter {
        ((it.action as? RemoteAction)?.srcServer == null)
                && File.fromPath(this, it.action.src) == null
    }

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
