package co.adityarajput.fileflow.data

import android.content.Context
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
            FileFlowDatabase.getDatabase(context).groupDao(),
        )
    }

    fun seedDemoData() {
        runBlocking {
            if (repository.rules().first().isEmpty()) {
                repository.upsert(
                    Rule(
                        Action.MOVE(
                            "/storage/emulated/0/AntennaPod",
                            "AntennaPodBackup-\\d{4}-\\d{2}-\\d{2}\\.db",
                            "/storage/emulated/0/Backups",
                            "AntennaPod.db",
                            overwriteExisting = true,
                        ),
                        executions = 2,
                        interval = null,
                        cronString = "00 10 * * 0",
                    ),
                    Rule(
                        Action.MOVE(
                            "/storage/emulated/0/Backups",
                            "TubularData-\\d{8}_\\d{6}\\.zip",
                            "/storage/emulated/0/Backups",
                            "Tubular.zip",
                            keepOriginal = false,
                            overwriteExisting = true,
                        ),
                        executions = 3,
                        interval = null,
                    ),
                    Rule(
                        Action.DELETE_STALE(
                            "/storage/emulated/0/Download",
                            "Alarmetrics_v[\\d\\.]+\\.apk",
                            scanSubdirectories = true,
                        ),
                        enabled = false,
                        interval = 86_400_000,
                    ),
                    Rule(
                        Action.ZIP(
                            "/storage/emulated/0/Documents/Notes",
                            "(.*)\\.md",
                            "/storage/emulated/0/Backups",
                            "Notes.zip",
                            true,
                        ),
                        executions = 5,
                        interval = null,
                        cronString = "30 13 * * *",
                    ),
                    Rule(
                        Action.EMIT_CHANGES(
                            "/storage/emulated/0/Movies",
                            ".*\\.mp4",
                            "org.amoradi.syncopoli.SYNC_PROFILE",
                            "org.amoradi.syncopoli",
                            """{"profile_name": "Movies Backup"}""",
                            true,
                            3_600_000L * 3,
                        ),
                        executions = 7,
                        interval = 3_600_000L * 3,
                    ),
                )
                repository.upsert(
                    Execution(
                        "TubularData-20251201_113745.zip",
                        Verb.MOVE,
                        System.currentTimeMillis() - 86400000L * 66,
                    ),
                    Execution(
                        "TubularData-20260101_141634.zip",
                        Verb.MOVE,
                        System.currentTimeMillis() - 86400000L * 35,
                    ),
                    Execution(
                        "TubularData-20260201_160604.zip",
                        Verb.MOVE,
                        System.currentTimeMillis() - 86400000L * 4,
                    ),
                    Execution(
                        "AntennaPodBackup-2026-02-02.db",
                        Verb.COPY,
                        System.currentTimeMillis() - 86400000L * 3,
                    ),
                    Execution(
                        "Notes.zip",
                        Verb.ZIP,
                        System.currentTimeMillis() - 86400000L * 1,
                    ),
                    Execution(
                        "AntennaPodBackup-2026-02-05.db",
                        Verb.COPY,
                        System.currentTimeMillis() - 60_000L * 5,
                    ),
                )
            }
        }
    }
}
