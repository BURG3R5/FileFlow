package co.adityarajput.fileflow.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.adityarajput.fileflow.services.Preferences
import co.adityarajput.fileflow.views.Brightness

class AppearanceViewModel : ViewModel() {
    var brightness by mutableStateOf(Brightness.entries[Preferences.brightness])
}
