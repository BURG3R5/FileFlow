package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Group
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.upsertShortcuts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class UpsertGroupViewModel(
    group: Group?,
    private val repository: Repository,
) : ViewModel() {
    data class State(
        val values: Values = Values(),
        val error: GroupFormError? = GroupFormError.from(values),
    )

    data class Values(
        val groupId: Int = 0,
        val name: String = "",
        val ruleIds: List<Int> = emptyList(),
        val allRules: Boolean = false,
    ) {
        companion object {
            fun from(group: Group) =
                Values(group.id, group.name, group.ruleIds, group.ruleIds.isEmpty())
        }

        fun toGroup() = Group(name, if (allRules) emptyList() else ruleIds, id = groupId)
    }

    var state by mutableStateOf(
        if (group == null) State()
        else State(values = Values.from(group)),
    )

    val rules: StateFlow<List<Rule>?> = repository.rules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun updateForm(values: Values = state.values) {
        state = State(values, GroupFormError.from(values))
    }

    suspend fun submitForm(context: Context) {
        if (GroupFormError.from(state.values) == null) {
            val group = state.values.toGroup()
            Logger.d(
                "UpsertRuleViewModel",
                "${if (state.values.groupId == 0) "Adding" else "Updating"} $group",
            )
            repository.upsert(group)
            context.upsertShortcuts()
        }
    }
}

enum class GroupFormError {
    BLANK_NAME, NO_RULES;

    companion object {
        fun from(values: UpsertGroupViewModel.Values): GroupFormError? {
            if (values.name.isBlank())
                return BLANK_NAME
            if (values.ruleIds.isEmpty() && !values.allRules)
                return NO_RULES

            return null
        }
    }
}
