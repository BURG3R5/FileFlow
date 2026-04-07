package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.work.PeriodicWorkRequest
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.*
import co.adityarajput.fileflow.views.components.FolderPickerState
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

class UpsertRuleViewModel(
    context: Context,
    rule: Rule?,
    private val repository: Repository,
) : ViewModel() {
    data class State(
        val page: FormPage = FormPage.ACTION,
        val values: Values = Values(),
        val error: RuleFormError? = RuleFormError.from(values),
        val warning: RuleFormWarning? = null,
    )

    data class Values(
        val ruleId: Int = 0,
        val actionBase: Action = Action.entries[0],
        val src: String = "",
        val srcFileNamePattern: String = "",
        val dest: String = "",
        val destFileNameTemplate: String = "",
        val superlative: FileSuperlative = FileSuperlative.LATEST,
        val keepOriginal: Boolean = true,
        val overwriteExisting: Boolean = false,
        val scanSubdirectories: Boolean = false,
        val preserveStructure: Boolean = false,
        val currentSrcFileNames: List<String>? = null,
        val predictedDestFileNames: List<String>? = null,
        val retentionDays: Int = 30,
        val interval: Long? = Constants.ONE_HOUR_IN_MILLIS,
        val cronString: String? = null,
        val predictedExecutionTimes: List<ZonedDateTime>? = null,
    ) {
        companion object {
            fun from(rule: Rule) = when (rule.action) {
                is Action.MOVE ->
                    Values(
                        rule.id, rule.action.base, rule.action.src,
                        rule.action.srcFileNamePattern, rule.action.dest,
                        rule.action.destFileNameTemplate, rule.action.superlative,
                        rule.action.keepOriginal, rule.action.overwriteExisting,
                        rule.action.scanSubdirectories, rule.action.preserveStructure,
                        interval = rule.interval,
                        cronString = rule.cronString,
                    )

                is Action.DELETE_STALE ->
                    Values(
                        rule.id, rule.action.base, rule.action.src,
                        rule.action.srcFileNamePattern,
                        scanSubdirectories = rule.action.scanSubdirectories,
                        retentionDays = rule.action.retentionDays,
                        interval = rule.interval,
                        cronString = rule.cronString,
                    )

                is Action.ZIP -> Values(
                    rule.id, rule.action.base, rule.action.src,
                    rule.action.srcFileNamePattern, rule.action.dest,
                    rule.action.destFileNameTemplate,
                    overwriteExisting = rule.action.overwriteExisting,
                    scanSubdirectories = rule.action.scanSubdirectories,
                    preserveStructure = rule.action.preserveStructure,
                    interval = rule.interval,
                    cronString = rule.cronString,
                )
            }
        }

        fun toRule() = Rule(
            when (actionBase) {
                is Action.MOVE ->
                    Action.MOVE(
                        src, srcFileNamePattern, dest, destFileNameTemplate,
                        scanSubdirectories, keepOriginal, overwriteExisting, superlative,
                        preserveStructure,
                    )

                is Action.DELETE_STALE ->
                    Action.DELETE_STALE(src, srcFileNamePattern, retentionDays, scanSubdirectories)

                is Action.ZIP ->
                    Action.ZIP(
                        src, srcFileNamePattern, dest, destFileNameTemplate,
                        scanSubdirectories, overwriteExisting, preserveStructure,
                    )
            },
            interval = interval,
            cronString = cronString,
            id = ruleId,
        )
    }

    var state by mutableStateOf(
        if (rule == null) State()
        else State(values = Values.from(rule)),
    )

    var folderPickerState by mutableStateOf<FolderPickerState?>(null)

    init {
        updateForm(context)
    }

    fun updateForm(context: Context, values: Values = state.values, page: FormPage = state.page) {
        var currentSrcFiles: List<File>? = null
        try {
            if (values.src.isNotBlank())
                currentSrcFiles = File.fromPath(context, values.src)!!
                    .listChildren(values.scanSubdirectories)
                    .filter { it.isFile && it.name != null }
        } catch (e: Exception) {
            Logger.e("UpsertRuleViewModel", "Couldn't fetch files in ${values.src}", e)
        }

        var predictedDestFileNames: List<String>? = null
        var warning: RuleFormWarning? = null
        try {
            val regex = Regex(values.srcFileNamePattern)

            val matchingSrcFiles = currentSrcFiles
                ?.filter { regex.matches(it.name!!) }
                ?.also { if (it.isEmpty()) warning = RuleFormWarning.NO_MATCHES_IN_SRC }

            if (values.destFileNameTemplate.isNotBlank()) {
                if (matchingSrcFiles != null && values.actionBase is Action.MOVE) {
                    predictedDestFileNames = matchingSrcFiles
                        .map { (values.toRule().action as Action.MOVE).getDestFileName(it) }
                        .distinct()
                }
                if (values.actionBase is Action.ZIP) {
                    predictedDestFileNames =
                        listOf((values.toRule().action as Action.ZIP).getDestFileName())
                }
            }
        } catch (_: Exception) {
        }

        val values = values.copy(
            currentSrcFileNames = currentSrcFiles.orEmpty().mapNotNull { it.name }.distinct(),
            predictedDestFileNames = predictedDestFileNames,
            predictedExecutionTimes = values.cronString?.getExecutionTimes(4),
        )
        state = State(page, values, warning = warning)
    }

    suspend fun submitForm(context: Context) {
        if (RuleFormError.from(state.values) == null) {
            val rule = state.values.toRule()
            Logger.d(
                "UpsertRuleViewModel",
                "${if (state.values.ruleId == 0) "Adding" else "Updating"} $rule",
            )
            repository.upsert(rule)
            context.scheduleWork()
        }
    }
}

enum class FormPage {
    ACTION, SCHEDULE;

    fun isFirstPage() = this == ACTION

    fun isFinalPage() = this == SCHEDULE

    fun next() = entries[ordinal + 1]

    fun previous() = entries[ordinal - 1]
}

enum class RuleFormError {
    BLANK_FIELDS, INVALID_REGEX, INVALID_TEMPLATE, MUST_END_IN_ZIP,
    INTERVAL_TOO_SHORT, INTERVAL_TOO_LONG, INVALID_CRON_STRING, CRON_TOO_FREQUENT;

    companion object {
        fun from(values: UpsertRuleViewModel.Values): RuleFormError? {
            try {
                if (values.src.isBlank() || values.srcFileNamePattern.isBlank())
                    return BLANK_FIELDS

                if (Regex(values.srcFileNamePattern).pattern != values.srcFileNamePattern)
                    return INVALID_REGEX

                if (values.actionBase is Action.MOVE) {
                    if (values.dest.isBlank() || values.destFileNameTemplate.isBlank())
                        return BLANK_FIELDS
                    if (values.predictedDestFileNames == null)
                        return INVALID_TEMPLATE
                }
                if (values.actionBase is Action.ZIP) {
                    if (values.dest.isBlank() || values.destFileNameTemplate.isBlank())
                        return BLANK_FIELDS
                    if (values.predictedDestFileNames?.size != 1)
                        return INVALID_TEMPLATE
                    if (!Regex("^.+\\.(zip|ZIP)$").matches(values.predictedDestFileNames.first()))
                        return MUST_END_IN_ZIP
                }
            } catch (_: Exception) {
                Logger.d("UpsertRuleViewModel", "Invalid regex")
                return INVALID_REGEX
            }

            if (values.interval != null) {
                if (values.interval < PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
                    return INTERVAL_TOO_SHORT
                if (values.interval > 7 * TimeUnit.DAYS.inMillis)
                    return INTERVAL_TOO_LONG
            }
            if (values.cronString != null) {
                val executionTimes =
                    values.cronString.getExecutionTimes(Constants.MAX_CRON_EXECUTIONS_PER_HOUR + 1)
                        ?: return INVALID_CRON_STRING

                if (
                    executionTimes.last().toEpochSecond() - executionTimes.first().toEpochSecond()
                    < Constants.ONE_HOUR_IN_MILLIS / 1000
                ) return CRON_TOO_FREQUENT
            }
            return null
        }
    }
}

enum class RuleFormWarning { NO_MATCHES_IN_SRC }
