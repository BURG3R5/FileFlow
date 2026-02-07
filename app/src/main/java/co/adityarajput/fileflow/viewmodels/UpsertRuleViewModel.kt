package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Action
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.pathToFile

class UpsertRuleViewModel(
    rule: Rule?,
    private val repository: Repository,
) : ViewModel() {
    data class State(
        val values: Values = Values(),
        val error: FormError? = null,
    )

    data class Values(
        val ruleId: Int = 0,
        val src: String = "",
        val srcFileNamePattern: String = "",
        val dest: String = "",
        val destFileNameTemplate: String = "",
        val keepOriginal: Boolean = true,
        val overwriteExisting: Boolean = false,
    ) {
        constructor(rule: Rule) : this(
            rule.id, (rule.action as Action.MOVE).src, rule.action.srcFileNamePattern,
            rule.action.dest, rule.action.destFileNameTemplate, rule.action.keepOriginal,
            rule.action.overwriteExisting,
        )

        fun toRule() = Rule(
            Action.MOVE(
                src, srcFileNamePattern, dest, destFileNameTemplate, keepOriginal,
                overwriteExisting,
            ),
            id = ruleId,
        )
    }

    var state by mutableStateOf(
        if (rule == null) State()
        else State(Values(rule), null),
    )

    fun getFilesInSrc(context: Context): List<String>? {
        try {
            if (state.values.src.isBlank()) return null

            return context.pathToFile(state.values.src)!!
                .listFiles()
                .filter { it.isFile && it.name != null }
                .map { it.name!! }
        } catch (e: Exception) {
            Logger.e("UpsertRuleViewModel", "Couldn't fetch files in ${state.values.src}", e)
            return null
        }
    }

    fun updateForm(values: Values) {
        state = State(values, getError(values))
    }

    private fun getError(values: Values = state.values): FormError? {
        try {
            if (
                values.src.isBlank() ||
                values.srcFileNamePattern.isBlank() ||
                values.dest.isBlank() ||
                values.destFileNameTemplate.isBlank()
            ) return FormError.BLANK_FIELDS

            Regex(values.srcFileNamePattern).pattern == values.srcFileNamePattern
        } catch (_: Exception) {
            Logger.d("RulesViewModel", "Invalid regex")
            return FormError.INVALID_REGEX
        }
        return null
    }

    suspend fun submitForm() {
        if (getError() == null) {
            val rule = state.values.toRule()
            Logger.d(
                "RulesViewModel",
                "${if (state.values.ruleId == 0) "Adding" else "Updating"} $rule",
            )
            repository.upsert(rule)
        }
    }
}

enum class FormError { BLANK_FIELDS, INVALID_REGEX }
