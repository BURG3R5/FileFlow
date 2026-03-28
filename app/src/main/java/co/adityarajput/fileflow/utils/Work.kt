package co.adityarajput.fileflow.utils

import android.content.Context
import androidx.work.*
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.services.Worker
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

suspend fun Context.scheduleWork() {
    // INFO: Delete work scheduled by the old scheduling system.
    WorkManager.getInstance(this).cancelUniqueWork(Constants.WORKER_NAME)

    AppContainer(this).repository.rules().first()
        .forEach {
            if (!it.enabled || it.interval == null)
                return@forEach

            Logger.d("Work", "Scheduling work for $it")

            WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "${Constants.WORKER_NAME}_${it.id}",
                ExistingPeriodicWorkPolicy.UPDATE,
                PeriodicWorkRequestBuilder<Worker>(
                    it.interval,
                    TimeUnit.MINUTES,
                    PeriodicWorkRequest.MIN_PERIODIC_FLEX_MILLIS,
                    TimeUnit.MILLISECONDS,
                ).setInputData(workDataOf("ruleId" to it.id)).build(),
            )
        }
}

fun Context.deleteWorkFor(rule: Rule) {
    Logger.d("Work", "Deleting work for $rule")

    WorkManager.getInstance(this)
        .cancelUniqueWork("${Constants.WORKER_NAME}_${rule.id}")
}
