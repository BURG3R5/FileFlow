package co.adityarajput.fileflow.data.models

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.utils.Logger
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
    ) : Action(srcFileNamePattern) {
        override fun toString() = when (this) {
            is MOVE -> "MOVE\n$src\n$srcFileNamePattern\n$dest\n$destFileNameTemplate\n$keepOriginal\n$overwriteExisting"
        }
    }

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

    companion object {
        fun fromString(value: String) = when {
            value.startsWith("MOVE") -> {
                val args = value.split("\n")

                MOVE(args[1], args[2], args[3], args[4], args[5].toBoolean(), args[6].toBoolean())
            }

            else -> {
                Logger.e("Action", value)
                throw IllegalArgumentException("Can't convert value to Action, unknown value: $value")
            }
        }
    }
}
