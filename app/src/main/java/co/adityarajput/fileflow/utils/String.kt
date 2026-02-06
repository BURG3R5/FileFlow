package co.adityarajput.fileflow.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import co.adityarajput.fileflow.R
import java.net.URLDecoder

@Composable
fun Long.toShortHumanReadableTime(): String {
    val now = System.currentTimeMillis()
    val delta = now - this

    val seconds = delta / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 1000 -> stringResource(R.string.many_days_ago)
        days > 0 -> pluralStringResource(R.plurals.day, days.toInt(), days)
        hours > 0 -> pluralStringResource(R.plurals.hour, hours.toInt(), hours)
        minutes > 0 -> pluralStringResource(R.plurals.minute, minutes.toInt(), minutes)
        seconds > 0 -> pluralStringResource(R.plurals.second, seconds.toInt(), seconds)
        else -> stringResource(R.string.just_now)
    }
}

@Composable
fun Boolean.getToggleString(): String =
    stringResource(if (this) R.string.disable else R.string.enable)

fun String.getGetDirectoryFromUri() = URLDecoder.decode(this, "UTF-8").split(":").last()
