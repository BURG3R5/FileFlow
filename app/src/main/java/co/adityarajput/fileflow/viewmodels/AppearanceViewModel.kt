package co.adityarajput.fileflow.viewmodels

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import co.adityarajput.fileflow.Constants.BRIGHTNESS
import co.adityarajput.fileflow.views.Brightness

class AppearanceViewModel(sharedPreferences: SharedPreferences) : ViewModel() {
    var brightness by mutableStateOf(Brightness.entries[sharedPreferences.getInt(BRIGHTNESS, 1)])
}
