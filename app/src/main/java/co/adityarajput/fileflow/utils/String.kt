package co.adityarajput.fileflow.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import co.adityarajput.fileflow.R
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@Composable
fun Long.toShortHumanReadableTime(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 1000 -> stringResource(R.string.many_days)
        days > 0 -> pluralStringResource(R.plurals.day, days.toInt(), days)
        hours > 0 -> pluralStringResource(R.plurals.hour, hours.toInt(), hours)
        minutes > 0 -> pluralStringResource(R.plurals.minute, minutes.toInt(), minutes)
        seconds > 0 -> pluralStringResource(R.plurals.second, seconds.toInt(), seconds)
        else -> stringResource(R.string.just_now)
    }
}

@Composable
fun Long.toAccurateHumanReadableTime(): String {
    val minutes = this / 60_000f
    val hours = minutes / 60
    val days = hours / 24

    return when {
        days > 0 && days.toInt().toFloat() == days
            -> pluralStringResource(R.plurals.day, days.toInt(), days.toInt())

        hours > 0 && hours.toInt().toFloat() == hours
            -> pluralStringResource(R.plurals.hour, hours.toInt(), hours.toInt())

        else -> pluralStringResource(R.plurals.minute, minutes.toInt(), minutes.toInt())
    }
}

@Composable
fun Boolean.getToggleString(): String =
    stringResource(if (this) R.string.disable else R.string.enable)

@OptIn(ExperimentalUuidApi::class)
fun String.applyCustomReplacements() = this
    .replace(
        $$"${uuid}",
        Uuid.random().toString(),
    )
    .replace(
        $$"${date}",
        ZonedDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
    )
    .replace(
        $$"${time}",
        ZonedDateTime.now().withNano(0).format(DateTimeFormatter.ISO_LOCAL_TIME),
    )
    .replace(
        Regex("\\$\\{datetime:([^}]+)\\}"),
        { result ->
            ZonedDateTime.now().format(DateTimeFormatter.ofPattern(result.groupValues[1]))
        },
    )
