package co.adityarajput.fileflow.utils

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.Intent
import android.os.SystemClock
import androidx.core.net.toUri
import androidx.work.*
import co.adityarajput.fileflow.AlarmReceiver
import co.adityarajput.fileflow.BuildConfig
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.services.Worker
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

@Suppress("KotlinConstantConditions", "SimplifyBooleanWithConstants")
suspend fun Context.scheduleWork() {
    // INFO: Delete work scheduled by the old scheduling system
    WorkManager.getInstance(this).cancelUniqueWork(Constants.WORKER_NAME)

    AppContainer(this).repository.rules().first()
        .forEach {
            if (!it.enabled || (it.interval == null && it.cronString == null))
                return@forEach

            Logger.d("Background", "Scheduling work for $it")

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "${Constants.WORKER_NAME}_${it.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<Worker>(
                    // INFO: For cron-like schedules, set/update alarms for upcoming executions every hour
                    it.interval ?: Constants.ONE_HOUR_IN_MILLIS,
                    TimeUnit.MILLISECONDS,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                    TimeUnit.MILLISECONDS,
                ).setInputData(workDataOf(Constants.EXTRA_RULE_ID to it.id)).apply {
                    if (BuildConfig.HAS_NETWORK_FEATURE && it.action.isRemote)
                        setConstraints(
                            Constraints.Builder()
                                .setRequiredNetworkType(NetworkType.CONNECTED)
                                .build(),
                        )
                }.build(),
            )
        }
}

fun Context.deleteWorkFor(rule: Rule) {
    Logger.d("Background", "Deleting work for $rule")

    WorkManager.getInstance(this)
        .cancelUniqueWork("${Constants.WORKER_NAME}_${rule.id}")
}

@SuppressLint("MissingPermission")
fun Context.scheduleAlarmsFor(rule: Rule) {
    val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

    Logger.d("Background", "Scheduling alarms for $rule")
    rule.cronString
        ?.getExecutionTimes(Constants.MAX_CRON_EXECUTIONS_PER_HOUR)
        ?.forEach { executionTime ->
            val delay = Duration.between(ZonedDateTime.now(), executionTime).toMillis()
            if (delay < 1000 || delay > 5 * (Constants.ONE_HOUR_IN_MILLIS)) return@forEach

            Logger.d("Background", "Setting exact alarm in ${delay}ms")
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + delay,
                PendingIntent.getBroadcast(
                    this, (rule.id to executionTime.toEpochSecond()).hashCode(),
                    Intent(this, AlarmReceiver::class.java).apply {
                        data =
                            "fileflow://execute/${rule.id}/${executionTime.toEpochSecond()}".toUri()
                        action = Constants.ACTION_EXECUTE_RULE
                        putExtra(Constants.EXTRA_RULE_ID, rule.id)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
}
