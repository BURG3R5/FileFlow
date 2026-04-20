package co.adityarajput.fileflow

object Constants {
    const val STATE = "state"
    const val IS_FIRST_RUN = "is_first_run"

    const val SETTINGS = "settings"
    const val BRIGHTNESS = "brightness"
    const val ENABLE_RULE_NAMES = "enable_rule_names"

    const val WORKER_NAME = "fileflow_worker"
    const val RULE_ID = "ruleId"
    const val ACTION_EXECUTE_RULE = "co.adityarajput.fileflow.EXECUTE_RULE"
    const val EXTRA_RULE_ID = "extra_rule_id"
    const val MAX_CRON_EXECUTIONS_PER_HOUR = 4

    const val ACTION_EXECUTE_GROUP = "co.adityarajput.fileflow.EXECUTE_GROUP"
    const val EXTRA_GROUP_ID = "extra_group_id"

    const val LOG_SIZE = 100

    const val ONE_HOUR_IN_MILLIS = 3_600_000L

    /**
     * Max amount of app shortcuts visible when launcher app icon is long-pressed.
     *
     * There *is* a `ShortcutManagerCompat.getMaxShortcutCountPerActivity` method, but it *lies*.
     */
    const val MAX_SHORTCUTS = 4

    val MEDIA_PREFIXES = listOf("image/", "video/", "audio/")
}
