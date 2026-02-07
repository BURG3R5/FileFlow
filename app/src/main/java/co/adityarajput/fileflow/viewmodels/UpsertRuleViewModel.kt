package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.FileSuperlative
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.pathToFile

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
        val src: String = "",
        val srcFileNamePattern: String = "",
        val dest: String = "",
        val destFileNameTemplate: String = "",
        val superlative: FileSuperlative = FileSuperlative.LATEST,
        val keepOriginal: Boolean = true,
        val overwriteExisting: Boolean = false,
        val currentSrcFileNames: List<String>? = null,
        val predictedDestFileNames: List<String>? = null,
    ) {
        constructor(rule: Rule) : this(
            rule.id, (rule.action as Action.MOVE).src, rule.action.srcFileNamePattern,
            rule.action.dest, rule.action.destFileNameTemplate, rule.action.superlative,
            rule.action.keepOriginal, rule.action.overwriteExisting,
        )

        fun toRule() = Rule(
            Action.MOVE(
                src, srcFileNamePattern, dest, destFileNameTemplate,
                keepOriginal, overwriteExisting, superlative,
            ),
            id = ruleId,
        )
    }

    var state by mutableStateOf(
        if (rule == null) State()
        else State(Values(rule), null),
    )

    fun updateForm(context: Context, values: Values) {
        var currentSrcFileNames: List<String>? = null
        try {
            if (values.src.isNotBlank())
                currentSrcFileNames = context.pathToFile(values.src)!!.listFiles()
                    .filter { it.isFile && it.name != null }.map { it.name!! }
        } catch (e: Exception) {
            Logger.e("UpsertRuleViewModel", "Couldn't fetch files in ${values.src}", e)
        }

        var predictedDestFileNames: List<String>? = null
        var warning: FormWarning? = null
        try {
            val regex = Regex(values.srcFileNamePattern)

            if (values.destFileNameTemplate.isNotBlank())
                predictedDestFileNames = currentSrcFileNames
                    ?.filter { regex.matches(it) }
                    ?.also { if (it.isEmpty()) warning = FormWarning.NO_MATCHES_IN_SRC }
                    ?.map { regex.replace(it, values.destFileNameTemplate) }
                    ?.distinct()
        } catch (_: Exception) {
        }

        val values = values.copy(
            currentSrcFileNames = currentSrcFileNames,
            predictedDestFileNames = predictedDestFileNames,
        )
        state = State(values, getError(values), warning)
    }

    private fun getError(values: Values): FormError? {
        try {
            if (
                values.src.isBlank() ||
                values.srcFileNamePattern.isBlank() ||
                values.dest.isBlank() ||
                values.destFileNameTemplate.isBlank()
            ) return FormError.BLANK_FIELDS

            Regex(values.srcFileNamePattern).pattern == values.srcFileNamePattern

            if (values.predictedDestFileNames == null) return FormError.INVALID_TEMPLATE
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
