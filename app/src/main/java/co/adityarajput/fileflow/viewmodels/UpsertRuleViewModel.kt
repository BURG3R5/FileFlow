package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.PeriodicWorkRequest
import co.adityarajput.fileflow.BuildConfig
import co.adityarajput.fileflow.Constants
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.RemoteAction
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.data.models.Server
import co.adityarajput.fileflow.utils.*
import co.adityarajput.fileflow.views.components.FolderPickerState
import co.adityarajput.fileflow.views.components.RemoteFolderPickerState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
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
        val intent: String = "",
        val packageName: String = "",
        val extras: String = "{}",
        val modifiedWithin: Long = Constants.ONE_HOUR_IN_MILLIS,
        val isRemoteAction: Boolean = false,
        val srcServer: Server? = null,
        val destServer: Server? = null,
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

                is Action.EMIT_CHANGES -> Values(
                    rule.id, rule.action.base, rule.action.src,
                    rule.action.srcFileNamePattern,
                    scanSubdirectories = rule.action.scanSubdirectories,
                    interval = rule.interval,
                    cronString = rule.cronString,
                    intent = rule.action.intent,
                    packageName = rule.action.packageName,
                    extras = rule.action.extras,
                    modifiedWithin = rule.action.modifiedWithin,
                )

                is RemoteAction.MOVE ->
                    Values(
                        rule.id, rule.action.base, rule.action.src,
                        rule.action.srcFileNamePattern, rule.action.dest,
                        rule.action.destFileNameTemplate, rule.action.superlative,
                        rule.action.keepOriginal, rule.action.overwriteExisting,
                        rule.action.scanSubdirectories, rule.action.preserveStructure,
                        interval = rule.interval,
                        cronString = rule.cronString,
                        isRemoteAction = true,
                        srcServer = rule.action.srcServer,
                        destServer = rule.action.destServer,
                    )

                is RemoteAction.DELETE_STALE ->
                    Values(
                        rule.id, rule.action.base, rule.action.src,
                        rule.action.srcFileNamePattern,
                        scanSubdirectories = rule.action.scanSubdirectories,
                        retentionDays = rule.action.retentionDays,
                        interval = rule.interval,
                        cronString = rule.cronString,
                        isRemoteAction = true,
                        srcServer = rule.action.srcServer,
                    )

                is RemoteAction.ZIP -> Values(
                    rule.id, rule.action.base, rule.action.src,
                    rule.action.srcFileNamePattern, rule.action.dest,
                    rule.action.destFileNameTemplate,
                    overwriteExisting = rule.action.overwriteExisting,
                    scanSubdirectories = rule.action.scanSubdirectories,
                    preserveStructure = rule.action.preserveStructure,
                    interval = rule.interval,
                    cronString = rule.cronString,
                    isRemoteAction = true,
                    srcServer = rule.action.srcServer,
                    destServer = rule.action.destServer,
                )

                is RemoteAction.EMIT_CHANGES -> Values(
                    rule.id, rule.action.base, rule.action.src,
                    rule.action.srcFileNamePattern,
                    scanSubdirectories = rule.action.scanSubdirectories,
                    interval = rule.interval,
                    cronString = rule.cronString,
                    intent = rule.action.intent,
                    packageName = rule.action.packageName,
                    extras = rule.action.extras,
                    modifiedWithin = rule.action.modifiedWithin,
                    isRemoteAction = true,
                    srcServer = rule.action.srcServer,
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

                is Action.EMIT_CHANGES ->
                    Action.EMIT_CHANGES(
                        src, srcFileNamePattern, intent, packageName, extras, scanSubdirectories,
                        modifiedWithin,
                    )

                is RemoteAction.MOVE ->
                    RemoteAction.MOVE(
                        srcServer, src, srcFileNamePattern, destServer, dest, destFileNameTemplate,
                        scanSubdirectories, keepOriginal, overwriteExisting, superlative,
                        preserveStructure,
                    )

                is RemoteAction.DELETE_STALE ->
                    RemoteAction.DELETE_STALE(
                        srcServer!!, src, srcFileNamePattern, retentionDays, scanSubdirectories,
                    )

                is RemoteAction.ZIP ->
                    RemoteAction.ZIP(
                        srcServer, src, srcFileNamePattern, destServer, dest, destFileNameTemplate,
                        scanSubdirectories, overwriteExisting, preserveStructure,
                    )

                is RemoteAction.EMIT_CHANGES ->
                    RemoteAction.EMIT_CHANGES(
                        srcServer!!, src, srcFileNamePattern, intent, packageName, extras,
                        scanSubdirectories, modifiedWithin,
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

    var servers by mutableStateOf<List<Server>>(emptyList())
        private set

    var folderPickerState by mutableStateOf<FolderPickerState?>(null)
    var remoteFolderPickerState by mutableStateOf<RemoteFolderPickerState?>(null)

    init {
        updateForm(context)
        viewModelScope.launch {
            servers = repository.servers().first()
        }
    }

    fun updateForm(context: Context, values: Values = state.values, page: FormPage = state.page) {
        var currentSrcFiles: List<File>? = null
        try {
            if (values.src.isNotBlank() && values.srcServer == null)
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
                when (values.actionBase) {
                    is Action.MOVE if (matchingSrcFiles != null) ->
                        predictedDestFileNames = matchingSrcFiles
                            .map { (values.toRule().action as Action.MOVE).getDestFileName(it) }
                            .distinct()

                    is RemoteAction.MOVE if (matchingSrcFiles != null) ->
                        predictedDestFileNames = matchingSrcFiles
                            .map { (values.toRule().action as RemoteAction.MOVE).getDestFileName(it) }
                            .distinct()

                    is Action.ZIP -> {
                        predictedDestFileNames =
                            listOf((values.toRule().action as Action.ZIP).getDestFileName())
                    }

                    is RemoteAction.ZIP -> {
                        predictedDestFileNames =
                            listOf((values.toRule().action as RemoteAction.ZIP).getDestFileName())
                    }

                    else -> {}
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

@Suppress("KotlinConstantConditions")
enum class RuleFormError {
    BLANK_FIELDS, INVALID_REGEX, INVALID_TEMPLATE, MUST_END_IN_ZIP, INVALID_JSON,
    REMOTE_ACTION_WITHOUT_SERVER,
    INTERVAL_TOO_SHORT, INTERVAL_TOO_LONG, INVALID_CRON_STRING, CRON_TOO_FREQUENT;

    companion object {
        fun from(values: UpsertRuleViewModel.Values): RuleFormError? {
            if (values.src.isBlank() || values.srcFileNamePattern.isBlank())
                return BLANK_FIELDS

            try {
                if (Regex(values.srcFileNamePattern).pattern != values.srcFileNamePattern)
                    return INVALID_REGEX
            } catch (_: Exception) {
                return INVALID_REGEX
            }

            when (values.actionBase) {
                is Action.MOVE, is RemoteAction.MOVE -> {
                    if (values.dest.isBlank() || values.destFileNameTemplate.isBlank())
                        return BLANK_FIELDS
                    if (values.predictedDestFileNames == null && !values.isRemoteAction)
                        return INVALID_TEMPLATE
                }

                is Action.ZIP, is RemoteAction.ZIP -> {
                    if (values.dest.isBlank() || values.destFileNameTemplate.isBlank())
                        return BLANK_FIELDS
                    if (values.predictedDestFileNames?.size != 1)
                        return INVALID_TEMPLATE
                    if (!Regex("^.+\\.(zip|ZIP)$").matches(values.predictedDestFileNames.first()))
                        return MUST_END_IN_ZIP
                }

                is Action.EMIT_CHANGES, is RemoteAction.EMIT_CHANGES -> {
                    if (values.intent.isBlank() || values.packageName.isBlank())
                        return BLANK_FIELDS
                    try {
                        if (Json.parseToJsonElement(values.extras.ifBlank { "{}" }) !is JsonObject)
                            return INVALID_JSON
                    } catch (_: Exception) {
                        return INVALID_JSON
                    }
                }

                else -> {}
            }

            if (BuildConfig.HAS_NETWORK_FEATURE) {
                when (values.actionBase) {
                    is RemoteAction.MOVE, is RemoteAction.ZIP -> {
                    if (values.srcServer == null && values.destServer == null)
                        return REMOTE_ACTION_WITHOUT_SERVER
                }

                    is RemoteAction.DELETE_STALE, is RemoteAction.EMIT_CHANGES -> {
                        if (values.srcServer == null)
                            return REMOTE_ACTION_WITHOUT_SERVER
                    }

                    else -> {}
                }
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
