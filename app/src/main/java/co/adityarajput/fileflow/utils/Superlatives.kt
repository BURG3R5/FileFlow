package co.adityarajput.fileflow.utils

import co.adityarajput.fileflow.R

enum class FileSuperlative(val displayName: Int, val selector: (File) -> Long) {
    EARLIEST(R.string.earliest, { -it.lastModified() }),
    LATEST(R.string.latest, { it.lastModified() }),
    SMALLEST(R.string.smallest, { -it.length() }),
    LARGEST(R.string.largest, { it.length() }),
}
