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
import java.util.concurrent.TimeUnit

class UpsertRuleViewModel(
    rule: Rule?,
    private val repository: Repository,
) : ViewModel() {
    data class State(
        val page: FormPage = FormPage.ACTION,
        val values: Values = Values(),
        val error: FormError? = FormError.from(values),
        val warning: FormWarning? = null,
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
                    )

                is Action.DELETE_STALE ->
                    Values(
                        rule.id, rule.action.base, rule.action.src,
                        rule.action.srcFileNamePattern,
                        scanSubdirectories = rule.action.scanSubdirectories,
                        retentionDays = rule.action.retentionDays,
                        interval = rule.interval,
                    )
            }
        }

        fun toRule() = when (actionBase) {
            is Action.MOVE ->
                Rule(
                    Action.MOVE(
                        src, srcFileNamePattern, dest, destFileNameTemplate,
                        scanSubdirectories, keepOriginal, overwriteExisting, superlative,
                        preserveStructure,
                    ),
                    interval = interval,
                    id = ruleId,
                )

            is Action.DELETE_STALE ->
                Rule(
                    Action.DELETE_STALE(src, srcFileNamePattern, retentionDays, scanSubdirectories),
                    interval = interval,
                    id = ruleId,
                )
        }
    }

    var state by mutableStateOf(
        if (rule == null) State()
        else State(values = Values.from(rule)),
    )

    var folderPickerState by mutableStateOf<FolderPickerState?>(null)

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
        var warning: FormWarning? = null
        try {
            val regex = Regex(values.srcFileNamePattern)

            val matchingSrcFiles = currentSrcFiles
                ?.filter { regex.matches(it.name!!) }
                ?.also { if (it.isEmpty()) warning = FormWarning.NO_MATCHES_IN_SRC }

            if (matchingSrcFiles != null && values.destFileNameTemplate.isNotBlank()) {
                predictedDestFileNames = matchingSrcFiles
                    .map { (values.toRule().action as Action.MOVE).getDestFileName(it) }
                    .distinct()
            }
        } catch (_: Exception) {
        }

        val values = values.copy(
            currentSrcFileNames = currentSrcFiles.orEmpty().mapNotNull { it.name }.distinct(),
            predictedDestFileNames = predictedDestFileNames,
        )
        state = State(page, values, warning = warning)
    }

    suspend fun submitForm(context: Context) {
        if (FormError.from(state.values) == null) {
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

enum class FormError {
    BLANK_FIELDS, INVALID_REGEX, INVALID_TEMPLATE,
    INTERVAL_TOO_SHORT, INTERVAL_TOO_LONG;

    companion object {
        fun from(values: UpsertRuleViewModel.Values): FormError? {
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

                if (values.interval != null) {
                    if (values.interval < PeriodicWorkRequest.MIN_PERIODIC_INTERVAL_MILLIS)
                        return INTERVAL_TOO_SHORT
                    if (values.interval > 7 * TimeUnit.DAYS.inMillis)
                        return INTERVAL_TOO_LONG
                }
            } catch (_: Exception) {
                Logger.d("UpsertRuleViewModel", "Invalid regex")
                return INVALID_REGEX
            }
            return null
        }
    }
}

enum class FormWarning { NO_MATCHES_IN_SRC }
