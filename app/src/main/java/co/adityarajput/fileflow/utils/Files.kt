package co.adityarajput.fileflow.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import co.adityarajput.fileflow.R

fun Context.pathToFile(path: String) = DocumentFile.fromTreeUri(this, path.toUri())

enum class FileSuperlative(val displayName: Int, val selector: (DocumentFile) -> Long) {
    EARLIEST(R.string.earliest, { -it.lastModified() }),
    LATEST(R.string.latest, { it.lastModified() }),
    SMALLEST(R.string.smallest, { -it.length() }),
    LARGEST(R.string.largest, { it.length() }),
}
