package co.adityarajput.fileflow.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Execution
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class ExecutionsViewModel(repository: Repository) : ViewModel() {
    data class State(val executions: List<Execution>? = null)

    val state: StateFlow<State> =
        repository.executions()
            .map { State(it) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())
}
