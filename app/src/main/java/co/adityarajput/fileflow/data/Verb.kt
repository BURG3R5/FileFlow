package co.adityarajput.fileflow.data

import co.adityarajput.fileflow.R

enum class Verb(val forRules: Int, val forExecutions: Int = forRules) {
    MOVE(R.string.move),
    COPY(R.string.copy),
    DELETE_STALE(R.string.delete_stale),
    ZIP(R.string.zip),
    EMIT_CHANGES(R.string.watch, R.string.emit);
}
