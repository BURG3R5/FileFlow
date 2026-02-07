package co.adityarajput.fileflow.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile

fun Context.pathToFile(path: String) = DocumentFile.fromTreeUri(this, path.toUri())
