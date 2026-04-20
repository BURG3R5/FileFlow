package co.adityarajput.fileflow.data.models

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import co.adityarajput.fileflow.BuildConfig
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.Verb
import co.adityarajput.fileflow.utils.*
import co.adityarajput.fileflow.views.dullStyle
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import java.io.BufferedOutputStream
import java.net.URLConnection
import java.nio.file.FileAlreadyExistsException
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

@Suppress("ClassName")
@Serializable
sealed class Action {
    abstract val src: String
    abstract val srcFileNamePattern: String
    abstract val scanSubdirectories: Boolean

    abstract val verb: Verb
    abstract val phrase: Int

    @Composable
    abstract fun getDescription(): AnnotatedString

    abstract suspend fun execute(context: Context, registerExecution: suspend (String) -> Unit)

    val base: Action
        get() = when (this) {
            is MOVE -> entries[0]
            is DELETE_STALE -> entries[1]
            is ZIP -> entries[2]
            is EMIT_CHANGES -> entries[3]

            is RemoteAction.MOVE -> RemoteAction.entries[0]
            is RemoteAction.DELETE_STALE -> RemoteAction.entries[1]
            is RemoteAction.ZIP -> RemoteAction.entries[2]
            is RemoteAction.EMIT_CHANGES -> RemoteAction.entries[3]
        }

    val counterBase: Action
        get() = when (this) {
            is MOVE -> RemoteAction.entries[0]
            is DELETE_STALE -> RemoteAction.entries[1]
            is ZIP -> RemoteAction.entries[2]
            is EMIT_CHANGES -> RemoteAction.entries[3]

            is RemoteAction.MOVE -> entries[0]
            is RemoteAction.DELETE_STALE -> entries[1]
            is RemoteAction.ZIP -> entries[2]
            is RemoteAction.EMIT_CHANGES -> entries[3]
        }

    open val isRemote get() = true

    infix fun isSimilarTo(other: Action) = this::class == other::class

    @Serializable
    data class MOVE(
        override val src: String,
        override val srcFileNamePattern: String,
        val dest: String,
        val destFileNameTemplate: String,
        override val scanSubdirectories: Boolean = false,
        val keepOriginal: Boolean = true,
        val overwriteExisting: Boolean = false,
        val superlative: FileSuperlative = FileSuperlative.LATEST,
        val preserveStructure: Boolean = scanSubdirectories,
    ) : Action() {
        override val verb get() = if (keepOriginal) Verb.COPY else Verb.MOVE

        override val phrase = R.string.move_phrase

        @Composable
        override fun getDescription() = buildAnnotatedString {
            withStyle(dullStyle) { append("from ") }
            append(src.getDirectoryFromUri())
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            withStyle(dullStyle) { append("\nto ") }
            append(dest.getDirectoryFromUri())
            if (preserveStructure)
                withStyle(dullStyle) { append(" & subfolders") }
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

        override suspend fun execute(
            context: Context,
            registerExecution: suspend (String) -> Unit,
        ) {
            val destDir = File.fromPath(context, dest)
            if (destDir == null) {
                Logger.e("Action", "$dest is invalid")
                return
            }

            val srcFiles = File.fromPath(context, src)
                ?.listChildren(scanSubdirectories)
                ?.filter {
                    it.isFile
                            && it.name != null
                            && Regex(srcFileNamePattern).matches(it.name!!)
                }
                ?.let {
                    if (superlative == FileSuperlative.NONE) it else
                        listOf(it.maxByOrNull(superlative.selector) ?: return)
                } ?: return

            val destPaths = mutableListOf<String?>()

            for (srcFile in srcFiles) {
                val srcFileName = srcFile.name ?: continue
                val destFileName = getDestFileName(srcFile)

                val relativePath = srcFile.parent!!.pathRelativeTo(src)
                val destSubDir =
                    if (!preserveStructure || relativePath == null) destDir
                    else destDir.createDirectory(relativePath)
                if (destSubDir == null) {
                    Logger.e(
                        "Action",
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
                        "Action",
                        "Source and destination files are identical",
                    )
                    continue
                }

                try {
                    Logger.i(
                        "Action",
                        "Moving $srcFileName to ${destSubDir.path}/$destFileName",
                    )
                    destPaths.add(
                        srcFile.moveTo(
                            destSubDir,
                            destFileName,
                            keepOriginal,
                            overwriteExisting,
                            context,
                        ),
                    )
                } catch (e: FileAlreadyExistsException) {
                    Logger.e("Action", "$destFileName already exists", e)
                    continue
                } catch (e: Exception) {
                    Logger.e("Action", "Failed to move $srcFileName", e)
                    continue
                }

                registerExecution(srcFileName)
            }

            MediaScannerConnection.scanFile(
                context,
                destPaths.filter { path ->
                    path != null && Constants.MEDIA_PREFIXES.any {
                        URLConnection.guessContentTypeFromName(path)
                            .startsWith(it)
                    }
                }.distinct().toTypedArray(),
                null,
            ) { path, _ ->
                Logger.d("Action", "Scanned media at $path")
            }
        }
    }

    @Serializable
    data class DELETE_STALE(
        override val src: String,
        override val srcFileNamePattern: String,
        val retentionDays: Int = 30,
        override val scanSubdirectories: Boolean = false,
    ) : Action() {
        override val verb get() = Verb.DELETE_STALE

        override val phrase = R.string.delete_stale_phrase

        fun retentionTimeInMillis() = retentionDays * 86_400_000L

        @Composable
        override fun getDescription() = buildAnnotatedString {

            withStyle(dullStyle) { append("in ") }
            append(src.getDirectoryFromUri())
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            withStyle(dullStyle) { append("\nif unmodified for ") }
            append((retentionTimeInMillis()).toShortHumanReadableTime())
        }

        override suspend fun execute(
            context: Context,
            registerExecution: suspend (String) -> Unit,
        ) {
            val srcFiles = File.fromPath(context, src)
                ?.listChildren(scanSubdirectories)
                ?.filter {
                    it.isFile
                            && it.name != null
                            && Regex(srcFileNamePattern).matches(it.name!!)
                }
                ?.filter {
                    System.currentTimeMillis() - it.lastModified >=
                            // INFO: While debugging, treat days as seconds
                            if (BuildConfig.DEBUG) retentionDays * 1000L
                            else retentionTimeInMillis()
                }
                ?: return

            for (srcFile in srcFiles) {
                val srcFileName = srcFile.name ?: continue
                Logger.i("Action", "Deleting $srcFileName")

                val result = srcFile.delete()
                if (!result) {
                    Logger.e("Action", "Failed to delete $srcFileName")
                    continue
                }

                registerExecution(srcFileName)
            }
        }
    }

    @Serializable
    data class ZIP(
        override val src: String,
        override val srcFileNamePattern: String,
        val dest: String,
        val destFileNameTemplate: String,
        override val scanSubdirectories: Boolean = false,
        val overwriteExisting: Boolean = false,
        val preserveStructure: Boolean = scanSubdirectories,
    ) : Action() {
        override val verb get() = Verb.ZIP

        override val phrase = R.string.zip_phrase

        @Composable
        override fun getDescription() = buildAnnotatedString {
            withStyle(dullStyle) { append("from ") }
            append(src.getDirectoryFromUri())
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            withStyle(dullStyle) { append("\nto ") }
            append(dest.getDirectoryFromUri())
            withStyle(dullStyle) { append("\nas ") }
            append(destFileNameTemplate)
        }

        fun getDestFileName() = destFileNameTemplate.applyCustomReplacements()

        override suspend fun execute(
            context: Context,
            registerExecution: suspend (String) -> Unit,
        ) {
            val destDir = File.fromPath(context, dest)
            if (destDir == null) {
                Logger.e("Action", "$dest is invalid")
                return
            }
            val destFileName = getDestFileName()

            destDir.listChildren(false).firstOrNull { it.isFile && it.name == destFileName }?.run {
                if (!overwriteExisting) {
                    Logger.e("Action", "$destFileName already exists")
                    return@execute
                }

                delete()
            }
            val destFile = destDir.createFile(destFileName, "application/zip") ?: run {
                Logger.e("Action", "Failed to create $destFileName")
                return@execute
            }

            val srcFiles = File.fromPath(context, src)
                ?.listChildren(scanSubdirectories)
                ?.filter {
                    it.isFile
                            && it.name != null
                            && Regex(srcFileNamePattern).matches(it.name!!)
                }
                ?: return

            ZipOutputStream(BufferedOutputStream(destFile.getOutputStream(context))).use { dest ->
                for (srcFile in srcFiles) {
                    val srcFileName = srcFile.name ?: continue
                    Logger.i("Action", "Adding $srcFileName to archive")

                    try {
                        dest.putNextEntry(
                            ZipEntry(
                                if (!preserveStructure) srcFileName
                                else srcFile.pathRelativeTo(src)!!,
                            ),
                        )
                        srcFile.getInputStream(context).use { src ->
                            if (src == null) {
                                Logger.e("Action", "Failed to open $srcFileName")
                                continue
                            }
                            src.copyTo(dest)
                        }
                        dest.closeEntry()
                    } catch (e: Exception) {
                        Logger.e("Action", "Failed to add $srcFileName to archive", e)
                        continue
                    }
                }
            }

            registerExecution(destFileName)
        }
    }

    @Serializable
    data class EMIT_CHANGES(
        override val src: String,
        override val srcFileNamePattern: String,
        val intent: String,
        val packageName: String,
        val extras: String = "{}",
        override val scanSubdirectories: Boolean = false,
        val modifiedWithin: Long = Constants.ONE_HOUR_IN_MILLIS,
    ) : Action() {
        override val verb get() = Verb.EMIT_CHANGES

        override val phrase = R.string.emit_changes_phrase

        @Composable
        override fun getDescription() = buildAnnotatedString {
            withStyle(dullStyle) { append("in ") }
            append(src.getDirectoryFromUri())
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
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
            val srcFiles = File.fromPath(context, src)
                ?.listChildren(scanSubdirectories)
                ?.filter {
                    it.isFile
                            && it.name != null
                            && Regex(srcFileNamePattern).matches(it.name!!)
                }
                ?.filter { System.currentTimeMillis() - it.lastModified <= modifiedWithin }
                ?: return

            if (srcFiles.isEmpty()) {
                Logger.d("Action", "No files modified recently")
                return
            }

            Logger.i("Action", "Emitting $intent to $packageName with $extras")
            try {
                context.sendBroadcast(
                    Intent(intent).apply {
                        setPackage(this@EMIT_CHANGES.packageName)
                        Json.parseToJsonElement(this@EMIT_CHANGES.extras.ifBlank { "{}" }).jsonObject.forEach { (key, value) ->
                            try {
                                putExtra(key, value.toSerializable())
                            } catch (e: Exception) {
                                Logger.e(
                                    "Action",
                                    "Failed to put extra $key with value $value of type ${value::class.java}",
                                    e,
                                )
                                return
                            }
                        }
                    },
                )
            } catch (e: Exception) {
                Logger.e("Action", "Failed to emit $intent", e)
                return
            }

            registerExecution(intent)
        }
    }

    companion object {
        val entries by lazy {
            listOf(
                MOVE("", "", "", ""),
                DELETE_STALE("", ""),
                ZIP("", "", "", ""),
                EMIT_CHANGES("", "", "", ""),
            )
        }
    }
}
