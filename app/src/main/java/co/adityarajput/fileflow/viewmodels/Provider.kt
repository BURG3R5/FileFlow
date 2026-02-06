package co.adityarajput.fileflow.viewmodels

import android.content.Context.MODE_PRIVATE
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import co.adityarajput.fileflow.Constants.SETTINGS
import co.adityarajput.fileflow.FileFlowApplication
import co.adityarajput.fileflow.data.models.Rule
import kotlinx.serialization.json.Json

object Provider {
    val Factory = viewModelFactory {
        initializer {
            AppearanceViewModel(
                fileFlowApplication().getSharedPreferences(
                    SETTINGS,
                    MODE_PRIVATE,
                ),
            )
        }
        initializer { RulesViewModel(fileFlowApplication().container.repository) }
        initializer { ExecutionsViewModel(fileFlowApplication().container.repository) }
    }

    fun createURVM(ruleString: String) = viewModelFactory {
        initializer {
            UpsertRuleViewModel(
                Json.decodeFromString<Rule?>(ruleString),
                fileFlowApplication().container.repository,
            )
        }
    }
}

fun CreationExtras.fileFlowApplication() =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as FileFlowApplication)
