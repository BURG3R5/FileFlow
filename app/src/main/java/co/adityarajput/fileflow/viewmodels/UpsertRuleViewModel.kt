package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.File
import co.adityarajput.fileflow.utils.FileSuperlative
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.views.components.FolderPickerState

class UpsertRuleViewModel(
    rule: Rule?,
    private val repository: Repository,
) : ViewModel() {
    data class State(
        val values: Values = Values(),
        val error: FormError? = null,
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
                    )

                is Action.DELETE_STALE ->
                    Values(
                        rule.id, rule.action.base, rule.action.src,
                        rule.action.srcFileNamePattern,
                        scanSubdirectories = rule.action.scanSubdirectories,
                        retentionDays = rule.action.retentionDays,
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
                    id = ruleId,
                )

            is Action.DELETE_STALE ->
                Rule(
                    Action.DELETE_STALE(src, srcFileNamePattern, retentionDays, scanSubdirectories),
                    id = ruleId,
                )
        }
    }

    var state by mutableStateOf(
        if (rule == null) State()
        else State(Values.from(rule), null),
    )

    var folderPickerState by mutableStateOf<FolderPickerState?>(null)

    fun updateForm(context: Context, values: Values) {
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
                predictedDestFileNames = matchingSrcFiles.map {
                    it.name!!.replace(
                        regex,
                        values.destFileNameTemplate.replace(
                            $$"${folder}",
                            it.parent?.name ?: "",
                        ),
                    )
                }.distinct()
            }
        } catch (_: Exception) {
        }

        val values = values.copy(
            currentSrcFileNames = currentSrcFiles.orEmpty().mapNotNull { it.name }.distinct(),
            predictedDestFileNames = predictedDestFileNames,
        )
        state = State(values, getError(values), warning)
    }

    private fun getError(values: Values): FormError? {
        try {
            if (values.src.isBlank() || values.srcFileNamePattern.isBlank())
                return FormError.BLANK_FIELDS

            if (Regex(values.srcFileNamePattern).pattern != values.srcFileNamePattern)
                return FormError.INVALID_REGEX

            if (values.actionBase is Action.MOVE) {
                if (values.dest.isBlank() || values.destFileNameTemplate.isBlank())
                    return FormError.BLANK_FIELDS
                if (values.predictedDestFileNames == null)
                    return FormError.INVALID_TEMPLATE
            }
        } catch (_: Exception) {
            Logger.d("UpsertRuleViewModel", "Invalid regex")
            return FormError.INVALID_REGEX
        }
        return null
    }

    suspend fun submitForm() {
        if (getError(state.values) == null) {
            val rule = state.values.toRule()
            Logger.d(
                "UpsertRuleViewModel",
                "${if (state.values.ruleId == 0) "Adding" else "Updating"} $rule",
            )
            repository.upsert(rule)
        }
    }
}

enum class FormError { BLANK_FIELDS, INVALID_REGEX, INVALID_TEMPLATE }
enum class FormWarning { NO_MATCHES_IN_SRC }
