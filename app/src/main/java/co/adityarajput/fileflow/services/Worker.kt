package co.adityarajput.fileflow.services

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.scheduleAlarmsFor

class Worker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val repository = AppContainer(context).repository
        val rule = repository.rule(inputData.getInt(Constants.RULE_ID, -1))

        if (rule != null && rule.enabled) {
            if (rule.cronString != null) {
                context.scheduleAlarmsFor(rule)
            } else if (rule.interval != null) {
                Logger.d("Worker", "Executing $rule")
                rule.action.execute(context) {
                    repository.registerExecution(
                        rule,
                        Execution(it, rule.action.verb),
                    )
                }
            }
        }

        return Result.success()
    }
}
