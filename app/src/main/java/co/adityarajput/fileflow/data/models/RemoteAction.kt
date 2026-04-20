package co.adityarajput.fileflow.data.models

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import co.adityarajput.fileflow.BuildConfig
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.Verb
import co.adityarajput.fileflow.data.models.Action.EMIT_CHANGES
import co.adityarajput.fileflow.services.SFTP
import co.adityarajput.fileflow.services.ls
import co.adityarajput.fileflow.utils.*
import co.adityarajput.fileflow.views.dullStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.schmizz.sshj.sftp.RemoteResourceInfo
import java.io.BufferedOutputStream
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.Paths
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.deleteRecursively
import kotlin.io.path.pathString
import java.io.File as IOFile
import java.nio.file.FileAlreadyExistsException as NioFileAlreadyExistsException

@Suppress("ClassName", "KotlinConstantConditions")
sealed class RemoteAction : Action() {
    abstract val srcServer: Server?

    override val isRemote get() = true

    @Serializable
    data class MOVE(
        override val srcServer: Server?,
        override val src: String,
        override val srcFileNamePattern: String,
        val destServer: Server?,
        val dest: String,
        val destFileNameTemplate: String,
        override val scanSubdirectories: Boolean = false,
        val keepOriginal: Boolean = true,
        val overwriteExisting: Boolean = false,
        val superlative: FileSuperlative = FileSuperlative.LATEST,
        val preserveStructure: Boolean = scanSubdirectories,
    ) : RemoteAction() {
        override val verb get() = if (keepOriginal) Verb.COPY else Verb.MOVE

        override val phrase = R.string.move_phrase

        @Composable
        override fun getDescription() = buildAnnotatedString {
            withStyle(dullStyle) { append("from ") }
            if (srcServer == null) {
                append(src.getDirectoryFromUri())
            } else {
                append(src)
            }
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            if (srcServer != null) {
                withStyle(dullStyle) { append("\non ") }
                append(srcServer.host)
            }
            withStyle(dullStyle) { append("\nto ") }
            if (destServer == null) {
                append(dest.getDirectoryFromUri())
            } else {
                append(dest)
            }
            if (preserveStructure)
                withStyle(dullStyle) { append(" & subfolders") }
            if (destServer != null) {
                withStyle(dullStyle) { append("\non ") }
                append(destServer.host)
            }
            withStyle(dullStyle) { append("\nas ") }
            append(destFileNameTemplate)
        }

        fun getDestFileName(srcFile: File) =
            srcFile.name!!.replace(
                Regex(srcFileNamePattern),
                destFileNameTemplate
                    .applyCustomReplacements()
                    .applyCustomFileReplacements(srcFile),
            )

        @OptIn(ExperimentalPathApi::class)
        override suspend fun execute(
            context: Context,
            registerExecution: suspend (String) -> Unit,
        ) {
            if (!BuildConfig.HAS_NETWORK_FEATURE)
                return

            val destDir = if (destServer == null) {
                File.fromPath(context, dest).let {
                    if (it == null) {
                        Logger.e("RemoteAction", "$dest is invalid")
                        return
                    } else it
                }
            } else null

            val srcRemoteFiles = mutableListOf<RemoteResourceInfo>()
            val tempDir =
                withContext(Dispatchers.IO) { Files.createTempDirectory("fileflow-sftp-temp-downloads") }

            val srcFiles = if (srcServer == null) {
                File.fromPath(context, src)
                    ?.listChildren(scanSubdirectories)
                    ?.filter {
                        it.isFile
                                && it.name != null
                                && Regex(srcFileNamePattern).matches(it.name!!)
                    }
                    ?.let {
                        if (superlative == FileSuperlative.NONE) it else
                            listOf(it.maxByOrNull(superlative.selector) ?: return)
                    }
                    ?.map { it to null }
                    ?: return
            } else {
                SFTP.runOn(srcServer) {
                    ls(src, scanSubdirectories) {
                        it.isRegularFile
                                && it.name != null
                                && Regex(srcFileNamePattern).matches(it.name)
                    }.let {
                        if (superlative == FileSuperlative.NONE) it else
                            listOf(it.maxBy(superlative.remoteSelector))
                    }.mapNotNull {
                        try {
                            val tempFilePath =
                                tempDir.resolve(Paths.get(src).relativize(Paths.get(it.path)))
                            Files.createDirectories(tempFilePath.parent)
                            get(it.path, tempFilePath.toString())
                            File.fromPath(context, tempFilePath.toString())!! to it
                        } catch (e: Exception) {
                            Logger.e("RemoteAction", "Failed to fetch ${it.name}", e)
                            null
                        }
                    }
                }!!
            }

            val destLocalPaths = mutableListOf<String?>()

            for ((srcFile, remoteFileInfo) in srcFiles) {
                val srcFileName = srcFile.name ?: continue
                val destFileName = getDestFileName(srcFile)
                val relativePath = srcFile.parent!!.pathRelativeTo(src)

                if (destServer == null) {
                    val destSubDir =
                        if (!preserveStructure || relativePath == null) destDir!!
                        else destDir!!.createDirectory(relativePath)
                    if (destSubDir == null) {
                        Logger.e(
                            "RemoteAction",
                            "Failed to create subdirectory in ${destDir.path}",
                        )
                        continue
                    }

                    if (
                        destSubDir
                            .listChildren(false)
                            .firstOrNull { it.isFile && it.name == destFileName }
                            ?.isIdenticalTo(srcFile, context)
                        == true
                    ) {
                        Logger.i(
                            "RemoteAction",
                            "Source and destination files are identical",
                        )
                        continue
                    }

                    try {
                        Logger.i(
                            "RemoteAction",
                            "Moving $srcFileName to ${destSubDir.path}/$destFileName",
                        )
                        destLocalPaths.add(
                            srcFile.moveTo(
                                destSubDir,
                                destFileName,
                                keepOriginal,
                                overwriteExisting,
                                context,
                            ),
                        )
                    } catch (e: NioFileAlreadyExistsException) {
                        Logger.e("RemoteAction", "$destFileName already exists", e)
                        continue
                    } catch (e: Exception) {
                        Logger.e("RemoteAction", "Failed to move $srcFileName", e)
                        continue
                    }
                } else {
                    val destSubDir = if (!preserveStructure || relativePath == null) dest
                    else "${dest.trimEnd('/')}$relativePath"

                    try {
                        Logger.i(
                            "RemoteAction",
                            "Moving $srcFileName to $destSubDir/$destFileName",
                        )
                        val uploadCompleted = SFTP.runOn(destServer) {
                            if (!overwriteExisting) {
                                ls(destSubDir, false) {
                                    if (it.isRegularFile && it.name == destFileName)
                                        throw NioFileAlreadyExistsException("$destSubDir/$destFileName already exists")

                                    false
                                }
                            }

                            try {
                                mkdirs(destSubDir)
                                put(
                                    srcFile.path,
                                    "${destSubDir.trimEnd('/')}/$destFileName",
                                )
                                if (!keepOriginal && remoteFileInfo != null)
                                    rm(remoteFileInfo.path)

                                true
                            } catch (e: Exception) {
                                Logger.e("RemoteAction", "Encountered error", e)
                                false
                            }
                        }
                        if (uploadCompleted != true) {
                            Logger.e(
                                "RemoteAction",
                                "Failed to upload $destFileName to $destServer",
                            )
                            return
                        }
                    } catch (e: Exception) {
                        Logger.e("RemoteAction", "Failed to move $srcFileName", e)
                        continue
                    }
                }

                registerExecution(srcFileName)
            }

            try {
                tempDir.deleteRecursively()
            } catch (e: Exception) {
                Logger.e("RemoteAction", "Failed to delete temp files", e)
            }

            if (destServer == null) {
                MediaScannerConnection.scanFile(
                    context,
                    destLocalPaths.filter { path ->
                        path != null && Constants.MEDIA_PREFIXES.any {
                            URLConnection.guessContentTypeFromName(path)
                                .startsWith(it)
                        }
                    }.distinct().toTypedArray(),
                    null,
                ) { path, _ -> Logger.d("RemoteAction", "Scanned media at $path") }
            } else if (!keepOriginal) {
                SFTP.runOn(srcServer!!) {
                    for (file in srcRemoteFiles) {
                        try {
                            rm(file.path)
                        } catch (e: Exception) {
                            Logger.e("RemoteAction", "Failed to delete ${file.name}", e)
                        }
                    }
                }
            }
        }
    }

    @Serializable
    data class DELETE_STALE(
        override val srcServer: Server,
        override val src: String,
        override val srcFileNamePattern: String,
        val retentionDays: Int = 30,
        override val scanSubdirectories: Boolean = false,
    ) : RemoteAction() {
        override val verb get() = Verb.DELETE_STALE

        override val phrase = R.string.delete_stale_phrase

        fun retentionTimeInMillis() = retentionDays * 86_400_000L

        @Composable
        override fun getDescription() = buildAnnotatedString {
            withStyle(dullStyle) { append("in ") }
            append(src)
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            withStyle(dullStyle) { append("\non ") }
            append(srcServer.host)
            withStyle(dullStyle) { append("\nif unmodified for ") }
            append((retentionTimeInMillis()).toShortHumanReadableTime())
        }

        override suspend fun execute(
            context: Context,
            registerExecution: suspend (String) -> Unit,
        ) {
            if (!BuildConfig.HAS_NETWORK_FEATURE)
                return

            SFTP.runOn(srcServer) {
                // INFO: While debugging, treat days as seconds
                val oldestMTime = (
                        System.currentTimeMillis() -
                                (if (BuildConfig.DEBUG) retentionDays * 1000L
                                else retentionTimeInMillis())
                        ) / 1000

                val srcFiles = ls(src, scanSubdirectories) {
                    it.isRegularFile
                            && it.name != null
                            && Regex(srcFileNamePattern).matches(it.name)
                            && it.attributes.mtime <= oldestMTime
                }

                for (file in srcFiles) {
                    val fileName = file.name ?: continue

                    Logger.i("RemoteAction", "Deleting $fileName")
                    try {
                        rm(file.path)
                    } catch (e: Exception) {
                        Logger.e("RemoteAction", "Failed to delete $fileName", e)
                        continue
                    }

                    registerExecution(fileName)
                }
            }
        }
    }

    @Serializable
    data class ZIP(
        override val srcServer: Server?,
        override val src: String,
        override val srcFileNamePattern: String,
        val destServer: Server?,
        val dest: String,
        val destFileNameTemplate: String,
        override val scanSubdirectories: Boolean = false,
        val overwriteExisting: Boolean = false,
        val preserveStructure: Boolean = scanSubdirectories,
    ) : RemoteAction() {
        override val verb get() = Verb.ZIP

        override val phrase = R.string.zip_phrase

        @Composable
        override fun getDescription() = buildAnnotatedString {
            withStyle(dullStyle) { append("from ") }
            if (srcServer == null) {
                append(src.getDirectoryFromUri())
            } else {
                append(src)
            }
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            if (srcServer != null) {
                withStyle(dullStyle) { append("\non ") }
                append(srcServer.host)
            }
            withStyle(dullStyle) { append("\nto ") }
            if (destServer == null) {
                append(dest.getDirectoryFromUri())
            } else {
                append(dest)
            }
            if (destServer != null) {
                withStyle(dullStyle) { append("\non ") }
                append(destServer.host)
            }
            withStyle(dullStyle) { append("\nas ") }
            append(destFileNameTemplate)
        }

        fun getDestFileName() = destFileNameTemplate.applyCustomReplacements()

        @OptIn(ExperimentalPathApi::class)
        override suspend fun execute(
            context: Context,
            registerExecution: suspend (String) -> Unit,
        ) {
            if (!BuildConfig.HAS_NETWORK_FEATURE)
                return

            val destFileName = getDestFileName()
            val destFile = if (destServer == null) {
                val destDir = File.fromPath(context, dest)
                if (destDir == null) {
                    Logger.e("RemoteAction", "$dest is invalid")
                    return
                }

                destDir.listChildren(false).firstOrNull { it.isFile && it.name == destFileName }
                    ?.run {
                        if (!overwriteExisting) {
                            Logger.e("RemoteAction", "$destFileName already exists")
                            return@execute
                        }

                        delete()
                    }

                destDir.createFile(destFileName, "application/zip") ?: run {
                    Logger.e("RemoteAction", "Failed to create $destFileName")
                    return@execute
                }
            } else {
                File.FSFile(
                    IOFile(
                        withContext(Dispatchers.IO) {
                            Files.createTempFile(destFileName, null).pathString
                        },
                    ),
                )
            }

            val tempDir =
                withContext(Dispatchers.IO) { Files.createTempDirectory("fileflow-sftp-temp-downloads") }

            val srcFiles = if (srcServer == null) {
                File.fromPath(context, src)
                    ?.listChildren(scanSubdirectories)
                    ?.filter {
                        it.isFile
                                && it.name != null
                                && Regex(srcFileNamePattern).matches(it.name!!)
                    }
                    ?: return
            } else {
                SFTP.runOn(srcServer) {
                    ls(src, scanSubdirectories) {
                        it.isRegularFile
                                && it.name != null
                                && Regex(srcFileNamePattern).matches(it.name)
                    }.mapNotNull {
                        try {
                            val tempFilePath =
                                tempDir.resolve(Paths.get(src).relativize(Paths.get(it.path)))
                            Files.createDirectories(tempFilePath.parent)
                            get(it.path, tempFilePath.toString())
                            File.fromPath(context, tempFilePath.toString())
                        } catch (e: Exception) {
                            Logger.e("RemoteAction", "Failed to fetch ${it.name}", e)
                            null
                        }
                    }
                }!!
            }

            ZipOutputStream(BufferedOutputStream(destFile.getOutputStream(context))).use { dest ->
                for (srcFile in srcFiles) {
                    val srcFileName = srcFile.name ?: continue
                    Logger.i("RemoteAction", "Adding $srcFileName to archive")

                    try {
                        dest.putNextEntry(
                            ZipEntry(
                                if (!preserveStructure) srcFileName
                                else srcFile.pathRelativeTo(src)!!,
                            ),
                        )
                        srcFile.getInputStream(context).use { src ->
                            if (src == null) {
                                Logger.e("RemoteAction", "Failed to open $srcFileName")
                                continue
                            }
                            src.copyTo(dest)
                        }
                        dest.closeEntry()
                    } catch (e: Exception) {
                        Logger.e("RemoteAction", "Failed to add $srcFileName to archive", e)
                        continue
                    }
                }
            }

            try {
                tempDir.deleteRecursively()
            } catch (e: Exception) {
                Logger.e("RemoteAction", "Failed to delete temp files", e)
            }

            if (destServer != null) {
                val uploadCompleted = SFTP.runOn(destServer) {
                    if (!overwriteExisting) {
                        ls(dest, false) {
                            if (it.isRegularFile && it.name == destFileName)
                                throw NioFileAlreadyExistsException("${dest.trimEnd('/')}/$destFileName already exists")

                            false
                        }
                    }

                    try {
                        put(
                            destFile.path,
                            "${dest.trimEnd('/')}/$destFileName",
                        )
                        true
                    } catch (_: Exception) {
                    }
                }
                if (uploadCompleted != true) {
                    Logger.e("RemoteAction", "Failed to upload $destFileName to $destServer")
                    return
                }
            }

            registerExecution(destFileName)
        }
    }

    @Serializable
    data class EMIT_CHANGES(
        override val srcServer: Server,
        override val src: String,
        override val srcFileNamePattern: String,
        val intent: String,
        val packageName: String,
        val extras: String = "{}",
        override val scanSubdirectories: Boolean = false,
        val modifiedWithin: Long = Constants.ONE_HOUR_IN_MILLIS,
    ) : RemoteAction() {
        override val verb get() = Verb.EMIT_CHANGES

        override val phrase = R.string.emit_changes_phrase

        @Composable
        override fun getDescription() = buildAnnotatedString {
            withStyle(dullStyle) { append("in ") }
            append(src)
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            withStyle(dullStyle) { append("\non ") }
            append(srcServer.host)
            withStyle(dullStyle) { append("\nemit ") }
            append(intent)
            if (!intent.contains(packageName)) {
                withStyle(dullStyle) { append("\nto ") }
                append(packageName)
            }
            if (extras.isNotBlank() && extras != "{}") {
                withStyle(dullStyle) { append("\nwith ") }
                append(extras)
            }
            withStyle(dullStyle) { append("\nif modified within ") }
            append(modifiedWithin.toAccurateHumanReadableTime())
        }

        override suspend fun execute(
            context: Context,
            registerExecution: suspend (String) -> Unit,
        ) {
            if (!BuildConfig.HAS_NETWORK_FEATURE)
                return

            SFTP.runOn(srcServer) {
                val latestAllowedMTime = (System.currentTimeMillis() - modifiedWithin) / 1000

                val srcFiles = ls(src, scanSubdirectories) {
                    it.isRegularFile
                            && it.name != null
                            && Regex(srcFileNamePattern).matches(it.name)
                            && it.attributes.mtime >= latestAllowedMTime
                }

                if (srcFiles.isEmpty()) {
                    Logger.d("RemoteAction", "No files modified recently")
                    return@runOn
                }

                Logger.i("RemoteAction", "Emitting $intent to $packageName with $extras")
                try {
                    context.sendBroadcast(
                        Intent(intent).apply {
                            setPackage(this@EMIT_CHANGES.packageName)
                            Json.parseToJsonElement(this@EMIT_CHANGES.extras.ifBlank { "{}" }).jsonObject.forEach { (key, value) ->
                                try {
                                    putExtra(key, value.toSerializable())
                                } catch (e: Exception) {
                                    Logger.e(
                                        "RemoteAction",
                                        "Failed to put extra $key with value $value of type ${value::class.java}",
                                        e,
                                    )
                                    return@runOn
                                }
                            }
                        },
                    )
                } catch (e: Exception) {
                    Logger.e("RemoteAction", "Failed to emit $intent", e)
                    return@runOn
                }

                registerExecution(intent)
            }
        }
    }

    companion object {
        val entries by lazy {
            listOf(
                MOVE(null, "", "", null, "", ""),
                DELETE_STALE(Server.new("", 0, ""), "", ""),
                ZIP(null, "", "", null, "", ""),
                EMIT_CHANGES("", "", "", ""),
            )
        }
    }
}
