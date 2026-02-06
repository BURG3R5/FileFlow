package co.adityarajput.fileflow.data

import android.content.Context
import co.adityarajput.fileflow.R
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class AppContainer(private val context: Context) {
    val repository: Repository by lazy {
        Repository(
            FileFlowDatabase.getDatabase(context).ruleDao(),
            FileFlowDatabase.getDatabase(context).executionDao(),
        )
    }

    fun seedDemoData() {
        runBlocking {
            if (
                repository.rules().first().isEmpty() &&
                repository.executions().first().isEmpty()
            ) {
                repository.upsert(
                    Rule(
                        Action.MOVE(
                            "content://com.android.externalstorage.documents/tree/primary%3AAntennaPod",
                            "AntennaPodBackup-\\d{4}-\\d{2}-\\d{2}.db",
                            "content://com.android.externalstorage.documents/tree/primary%3ABackups",
                            "AntennaPod.db",
                            overwriteExisting = true,
                        ),
                        executions = 2,
                    ),
                    Rule(
                        Action.MOVE(
                            "content://com.android.externalstorage.documents/tree/primary%3ABackups",
                            "TubularData-\\d{8}_\\d{6}.zip",
                            "content://com.android.externalstorage.documents/tree/primary%3ABackups",
                            "Tubular.zip",
                            keepOriginal = false,
                            overwriteExisting = true,
                        ),
                        executions = 3,
                    ),
                )
                repository.upsert(
                    Execution(
                        "TubularData-20251201_113745.zip",
                        R.string.move,
                        System.currentTimeMillis() - 86400000L * 66,
                    ),
                    Execution(
                        "TubularData-20260101_141634.zip",
                        R.string.move,
                        System.currentTimeMillis() - 86400000L * 35,
                    ),
                    Execution(
                        "TubularData-20260201_160604.zip",
                        R.string.move,
                        System.currentTimeMillis() - 86400000L * 4,
                    ),
                    Execution(
                        "AntennaPodBackup-2026-02-02.db",
                        R.string.copy,
                        System.currentTimeMillis() - 86400000L * 3,
                    ),
                    Execution(
                        "AntennaPodBackup-2026-02-05.db",
                        R.string.copy,
                        System.currentTimeMillis() - 60_000L * 5,
                    ),
                )
            }
        }
    }
}
