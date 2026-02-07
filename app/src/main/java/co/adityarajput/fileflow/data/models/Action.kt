package co.adityarajput.fileflow.data.models

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.FileSuperlative
import co.adityarajput.fileflow.utils.getGetDirectoryFromUri
import kotlinx.serialization.Serializable

@Serializable
sealed class Action(val title: String) {
    @Serializable
    data class MOVE(
        val src: String,
        val srcFileNamePattern: String,
        val dest: String,
        val destFileNameTemplate: String,
        val keepOriginal: Boolean = true,
        val overwriteExisting: Boolean = false,
        val superlative: FileSuperlative = FileSuperlative.LATEST,
    ) : Action(srcFileNamePattern)

    val verb
        get() = when (this) {
            is MOVE -> if (keepOriginal) R.string.copy else R.string.move
        }

    val description
        @Composable get() = when (this) {
            is MOVE -> buildAnnotatedString {
                val dullStyle = SpanStyle(MaterialTheme.colorScheme.onSurfaceVariant)

                withStyle(dullStyle) { append("from ") }
                append(src.getGetDirectoryFromUri())
                withStyle(dullStyle) { append("\nto ") }
                append(dest.getGetDirectoryFromUri())
                withStyle(dullStyle) { append("\nas ") }
                append(destFileNameTemplate)
            }
        }
}
