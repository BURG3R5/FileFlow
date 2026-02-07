package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.services.FlowExecutor
import co.adityarajput.fileflow.utils.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(private val repository: Repository) : ViewModel() {
    data class State(val rules: List<Rule>? = null)

    val state: StateFlow<State> = repository.rules()
        .map { State(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    var dialogState by mutableStateOf<DialogState?>(null)

    var selectedRule by mutableStateOf<Rule?>(null)

    fun executeRule(context: Context, showToast: (String) -> Unit) {
        viewModelScope.launch {
            val latestLogBeforeExecution = Logger.logs.lastOrNull()

            FlowExecutor(context).run(listOf(selectedRule!!))

            val recentErrorLog = Logger.logs
                .dropWhile { it != latestLogBeforeExecution }.drop(1)
                .firstOrNull { it.contains("[ERROR]") }
                ?: Logger.logs.lastOrNull { it.contains("[ERROR]") }
            if (recentErrorLog != null) {
                showToast("Error:" + recentErrorLog.substringAfter("[ERROR]"))
            }
        }
    }

    fun toggleRule() {
        viewModelScope.launch {
            Logger.d("RulesViewModel", "Toggling enabled state of $selectedRule")
            repository.toggle(selectedRule!!)
        }
    }

    fun deleteRule() {
        viewModelScope.launch {
            Logger.d("RulesViewModel", "Deleting $selectedRule")
            repository.delete(selectedRule!!)
        }
    }
}

enum class DialogState { EXECUTE, TOGGLE_RULE, DELETE }
