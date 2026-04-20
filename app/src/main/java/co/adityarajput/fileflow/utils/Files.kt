package co.adityarajput.fileflow.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import co.adityarajput.fileflow.data.models.Action
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
            var e1: Exception?
            try {
                return DocumentFile.fromTreeUri(context, path.toUri())?.let {
                    if (it.exists()) SAFFile(it) else null
                }!!
            } catch (e: Exception) {
                e1 = e
            }

            try {
                return IOFile(path).let {
                    if (it.exists()) FSFile(it) else null
                }!!
            } catch (e2: Exception) {
                Logger.w("Files", "Error while creating SAFFile from path: $path", e1)
                Logger.w("Files", "Error while creating FSFile from path: $path", e2)
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
            var destFilePath: String? = null

            val destFile = when (destDir) {
                is SAFFile -> {
                    destDir.documentFile.findFile(destFileName)?.run {
                        if (overwriteExisting) delete()
                        else throw Exception("$destFileName already exists")
                    }

                    SAFFile(
                        destDir.documentFile.createFile(
                            documentFile.type ?: "application/octet-stream",
                            destFileName,
                        )!!,
                    )
                }

                is FSFile -> IOFile(destDir.ioFile, destFileName).let {
                    it.createNewFile()
                    destFilePath = it.absolutePath

                    FSFile(it)
                }
            }

            this.getInputStream(context).use { srcStream ->
                destFile.getOutputStream(context).use { destStream ->
                    srcStream!!.copyTo(destStream!!)
                }
            }

            if (!keepOriginal) delete()

            return destFilePath
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
            val options = mutableListOf<StandardCopyOption>()
            if (keepOriginal) options.add(StandardCopyOption.COPY_ATTRIBUTES)
            if (overwriteExisting) options.add(StandardCopyOption.REPLACE_EXISTING)
            var destFilePath: String? = null

            try {
                withContext(Dispatchers.IO) {
                    if (destDir !is FSFile)
                        throw Exception("Destination is not a FSFile")

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
                    val destFile = when (destDir) {
                        is FSFile -> IOFile(destDir.ioFile, destFileName).let {
                            it.createNewFile()
                            destFilePath = it.absolutePath

                            FSFile(it)
                        }

                        is SAFFile -> {
                            destDir.documentFile.findFile(destFileName)?.run {
                                if (overwriteExisting) delete()
                                else throw Exception("$destFileName already exists")
                            }

                            SAFFile(
                                destDir.documentFile.createFile(
                                    "application/octet-stream",
                                    destFileName,
                                )!!,
                            )
                        }
                    }

                    this@FSFile.getInputStream(context).use { srcStream ->
                        destFile.getOutputStream(context).use { destStream ->
                            srcStream.copyTo(destStream!!)
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

    fun isIdenticalTo(other: File, context: Context): Boolean {
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
        when (it.action) {
            is RemoteAction.MOVE if (it.action.destServer == null) ->
                File.fromPath(this, it.action.dest) == null

            is RemoteAction.ZIP if (it.action.destServer == null) ->
                File.fromPath(this, it.action.dest) == null

            is RemoteAction ->
                it.action.srcServer == null && File.fromPath(this, it.action.src) == null

            is Action.MOVE ->
                File.fromPath(this, it.action.dest) == null

            is Action.ZIP ->
                File.fromPath(this, it.action.dest) == null

            is Action ->
                File.fromPath(this, it.action.src) == null
        }
    }

fun Context.getAllStorages(): Map<IOFile, String> {
    val storages = mutableMapOf<IOFile, String>()

    try {
        getExternalFilesDirs(null).forEach {
            var storage = it
            var dir = it
            while (dir.parentFile != null) {
                dir = dir.parentFile!!
                if (dir.canRead())
                    storage = dir
            }
            storages[storage] = storage.name
        }
    } catch (e: Exception) {
        Logger.e("Files", "Couldn't extract storages from external app directories", e)
    }

    storages[Environment.getExternalStorageDirectory()] = "Primary Storage"

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        try {
            (getSystemService(Context.STORAGE_SERVICE) as StorageManager).getStorageVolumes()
                .forEach {
                    if (it.directory != null && it.directory!!.canRead()) {
                        storages[it.directory!!] = it.getDescription(this)
                    }
                }
        } catch (e: Exception) {
            Logger.e("Files", "Couldn't extract storages from StorageManager", e)
        }
    }

    Logger.d("Files", "Found storages: ${storages.keys.joinToString { it.absolutePath }}")

    return storages
}
