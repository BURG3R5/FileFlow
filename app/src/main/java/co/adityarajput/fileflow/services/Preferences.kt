package co.adityarajput.fileflow.services

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import androidx.core.content.edit
import co.adityarajput.fileflow.Constants

object Preferences {
    private lateinit var state: SharedPreferences
    private lateinit var settings: SharedPreferences

    fun init(context: Context) {
        state = context.getSharedPreferences(Constants.STATE, MODE_PRIVATE)
        settings = context.getSharedPreferences(Constants.SETTINGS, MODE_PRIVATE)
    }

    private const val IS_FIRST_RUN = "is_first_run"
    var isFirstRun
        get() = state.getBoolean(IS_FIRST_RUN, true)
        set(value) = state.edit { putBoolean(IS_FIRST_RUN, value) }

    private const val DISPLAY_FULL_PATHS = "display_full_paths"
    var displayFullPaths
        get() = settings.getBoolean(DISPLAY_FULL_PATHS, false)
        set(value) = settings.edit { putBoolean(DISPLAY_FULL_PATHS, value) }

    private const val ENABLE_RULE_NAMES = "enable_rule_names"
    var enableRuleNames
        get() = settings.getBoolean(ENABLE_RULE_NAMES, false)
        set(value) = settings.edit { putBoolean(ENABLE_RULE_NAMES, value) }

    private const val BRIGHTNESS = "brightness"
    var brightness
        get() = settings.getInt(BRIGHTNESS, 1)
        set(value) = settings.edit { putInt(BRIGHTNESS, value) }
}
