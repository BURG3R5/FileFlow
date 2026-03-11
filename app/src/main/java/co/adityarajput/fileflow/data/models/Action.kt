package co.adityarajput.fileflow.data.models

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.Verb
import co.adityarajput.fileflow.utils.FileSuperlative
import co.adityarajput.fileflow.utils.getGetDirectoryFromUri
import co.adityarajput.fileflow.utils.toShortHumanReadableTime
import kotlinx.serialization.Serializable

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
            val dullStyle = SpanStyle(MaterialTheme.colorScheme.onSurfaceVariant)

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
    }

    companion object {
        val entries by lazy { listOf(MOVE("", "", "", ""), DELETE_STALE("", "")) }
    }
}
