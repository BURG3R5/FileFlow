package co.adityarajput.fileflow

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import co.adityarajput.fileflow.data.AppContainer
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.utils.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.d("AlarmReceiver", "Received intent with action: ${intent.action}")

        if (intent.action == Constants.ACTION_EXECUTE_RULE) {
            CoroutineScope(Dispatchers.IO).launch {
                val repository = AppContainer(context).repository
                val rule = repository.rule(intent.getIntExtra(Constants.EXTRA_RULE_ID, -1))

                if (rule == null || !rule.enabled)
                    return@launch

                Logger.d("Worker", "Executing $rule")
                rule.action.execute(context) {
                    repository.registerExecution(
                        rule,
                        Execution(it, rule.action.verb),
                    )
                }
            }
        }
    }
}
