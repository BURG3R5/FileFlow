package co.adityarajput.fileflow.viewmodels

import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.adityarajput.fileflow.FileFlowApplication
import co.adityarajput.fileflow.data.models.Group
import co.adityarajput.fileflow.data.models.Rule
import co.adityarajput.fileflow.viewmodels.servers.CreateServerViewModel
import co.adityarajput.fileflow.viewmodels.servers.ServersViewModel
import kotlinx.serialization.json.Json

object Provider {
    val Factory = viewModelFactory {
        initializer { AppearanceViewModel() }
        initializer { RulesViewModel(fileFlowApplication().container.repository) }
        initializer { ExecutionsViewModel(fileFlowApplication().container.repository) }
        initializer { GroupsViewModel(fileFlowApplication().container.repository) }
        initializer { ServersViewModel(fileFlowApplication().container.repository) }
        initializer { CreateServerViewModel(fileFlowApplication().container.repository) }
    }

    fun createURVM(ruleString: String) = viewModelFactory {
        initializer {
            UpsertRuleViewModel(
                fileFlowApplication(),
                Json.decodeFromString<Rule?>(ruleString),
                fileFlowApplication().container.repository,
            )
        }
    }

    fun createUGVM(groupString: String) = viewModelFactory {
        initializer {
            UpsertGroupViewModel(
                Json.decodeFromString<Group?>(groupString),
                fileFlowApplication().container.repository,
            )
        }
    }
}

fun CreationExtras.fileFlowApplication() =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as FileFlowApplication)
