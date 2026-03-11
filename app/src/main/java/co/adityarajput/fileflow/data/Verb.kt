package co.adityarajput.fileflow.data

import co.adityarajput.fileflow.R

enum class Verb(val resource: Int) {
    MOVE(R.string.move),
    COPY(R.string.copy),
    DELETE_STALE(R.string.delete_stale),
}
