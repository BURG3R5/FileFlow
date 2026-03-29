package co.adityarajput.fileflow.data.models

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.Verb
import co.adityarajput.fileflow.utils.*
import co.adityarajput.fileflow.views.dullStyle
import kotlinx.serialization.Serializable
import java.nio.file.FileAlreadyExistsException
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

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
        }

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
            append(src.getGetDirectoryFromUri())
            if (scanSubdirectories)
                withStyle(dullStyle) { append(" & subfolders") }
            withStyle(dullStyle) { append("\nto ") }
            append(dest.getGetDirectoryFromUri())
            if (preserveStructure)
                withStyle(dullStyle) { append(" & subfolders") }
            withStyle(dullStyle) { append("\nas ") }
            append(destFileNameTemplate)
        }

        @OptIn(ExperimentalUuidApi::class)
        fun getDestFileName(srcFile: File) =
            srcFile.name!!.replace(
                Regex(srcFileNamePattern),
                destFileNameTemplate.replace(
                    $$"${uuid}",
                    Uuid.random().toString(),
                ).replace(
                    $$"${folder}",
                    srcFile.parent?.name ?: "",
                ),
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
                    srcFile.moveTo(
                        destSubDir,
                        destFileName,
                        keepOriginal,
                        overwriteExisting,
                        context,
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
            val dullStyle = SpanStyle(MaterialTheme.colorScheme.onSurfaceVariant)

            withStyle(dullStyle) { append("in ") }
            append(src.getGetDirectoryFromUri())
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
                    System.currentTimeMillis() - it.lastModified() >=
                            // INFO: While debugging, treat days as seconds
                            if (context.isDebugBuild()) retentionDays * 1000L
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

    companion object {
        val entries by lazy { listOf(MOVE("", "", "", ""), DELETE_STALE("", "")) }
    }
}
