package co.adityarajput.fileflow.viewmodels.servers

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Server
import co.adityarajput.fileflow.utils.Logger
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ServersViewModel(private val repository: Repository) : ViewModel() {
    data class State(val servers: List<Server>? = null)

    val state: StateFlow<State> = repository.servers()
        .map { State(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    var showDeleteDialog by mutableStateOf(false)

    var selectedServer by mutableStateOf<Server?>(null)

    fun deleteServer() {
        viewModelScope.launch {
            Logger.d("ServersViewModel", "Deleting $selectedServer")
            repository.delete(selectedServer!!)
        }
    }
}
