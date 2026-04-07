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

class ShortcutReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Logger.d("ShortcutReceiver", "Received intent with action: ${intent.action}")

        if (intent.action != Constants.ACTION_EXECUTE_GROUP)
            return

        CoroutineScope(Dispatchers.IO).launch {
            val repository = AppContainer(context).repository
            val groupId = intent.getIntExtra(Constants.EXTRA_GROUP_ID, -1)
            val (group, rules) = repository.group(groupId)

            if (group == null)
                return@launch

            Logger.d("ShortcutReceiver", "Executing $group")
            for (rule in rules) {
                Logger.d("ShortcutReceiver", "Executing $rule")
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
