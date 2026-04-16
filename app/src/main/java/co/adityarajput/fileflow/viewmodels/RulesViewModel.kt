package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Execution
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.deleteWorkFor
import co.adityarajput.fileflow.utils.scheduleWork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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

            withContext(Dispatchers.IO) {
                selectedRule!!.action.execute(context) {
                    repository.registerExecution(
                        selectedRule!!,
                        Execution(it, selectedRule!!.action.verb),
                    )
                }
            }

            val recentErrorLog = Logger.logs
                .dropWhile { it != latestLogBeforeExecution }.drop(1)
                .firstOrNull { it.contains("[ERROR]") }
            if (recentErrorLog != null) {
                showToast("Error:" + recentErrorLog.substringAfter("[ERROR]"))
            }
        }
    }

    fun toggleRule(context: Context) {
        viewModelScope.launch {
            Logger.d("RulesViewModel", "Toggling enabled state of $selectedRule")
            if (selectedRule!!.enabled) {
                context.deleteWorkFor(selectedRule!!)
            } else {
                context.scheduleWork()
            }
            repository.toggle(selectedRule!!)
        }
    }

    fun deleteRule(context: Context) {
        viewModelScope.launch {
            Logger.d("RulesViewModel", "Deleting $selectedRule")
            context.deleteWorkFor(selectedRule!!)
            repository.delete(selectedRule!!)
        }
    }
}

enum class DialogState { EXECUTE, TOGGLE_RULE, DELETE }
