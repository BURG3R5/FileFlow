package co.adityarajput.fileflow.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import co.adityarajput.fileflow.R
import java.util.concurrent.TimeUnit

val TimeUnit.inMillis: Long
    get() = when (this) {
        TimeUnit.MINUTES -> 60_000
        TimeUnit.HOURS -> 3_600_000
        TimeUnit.DAYS -> 86_400_000
        else -> throw IllegalArgumentException("Unsupported TimeUnit: $this")
    }

@Composable
fun TimeUnit.text(value: Int) =
    pluralStringResource(
        when (this) {
            TimeUnit.MINUTES -> R.plurals.minute
            TimeUnit.HOURS -> R.plurals.hour
            TimeUnit.DAYS -> R.plurals.day
            else -> throw IllegalArgumentException("Unsupported TimeUnit: $this")
        },
        value, 0,
    ).substringAfter(' ')
