package co.adityarajput.fileflow.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import co.adityarajput.fileflow.R
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import java.time.ZonedDateTime
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

val ZonedDateTime.isToday get() = toLocalDate() == ZonedDateTime.now().toLocalDate()

private val cronParser = CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX))

fun String.getExecutionTimes(count: Int): List<ZonedDateTime>? {
    try {
        val schedule = cronParser.parse(this)
        val executionTime = ExecutionTime.forCron(schedule)
        var next = executionTime.nextExecution(ZonedDateTime.now()).get()
        val executions = mutableListOf<ZonedDateTime>()
        repeat(count) {
            executions.add(next)
            next = executionTime.nextExecution(next).get()
        }
        return executions
    } catch (_: IllegalArgumentException) {
        Logger.d("Time", "Failed to parse $this")
        return null
    }
}
