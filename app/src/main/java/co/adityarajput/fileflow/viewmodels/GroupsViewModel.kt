package co.adityarajput.fileflow.viewmodels

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.adityarajput.fileflow.data.Repository
import co.adityarajput.fileflow.data.models.Group
import co.adityarajput.fileflow.utils.Logger
import co.adityarajput.fileflow.utils.upsertShortcuts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroupsViewModel(private val repository: Repository) : ViewModel() {
    data class State(val groups: List<Group>? = null)

    val state: StateFlow<State> = repository.groups()
        .map { State(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), State())

    var showDeleteDialog by mutableStateOf(false)

    var selectedGroup by mutableStateOf<Group?>(null)

    fun deleteGroup(context: Context) {
        viewModelScope.launch {
            Logger.d("GroupsViewModel", "Deleting $selectedGroup")
            repository.delete(selectedGroup!!)
            context.upsertShortcuts()
        }
    }
}
