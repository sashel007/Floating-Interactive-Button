package ru.ikar.floatingbutton_ikar.sharedpreferences

import android.content.Context
import android.content.SharedPreferences

class ButtonPreferenceUtil(private val context: Context) {
    companion object {
        private const val PREFERENCES_FILE_KEY = "button_manager_names"
        private const val DEFAULT_ACTION = "default_action"
    }

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFERENCES_FILE_KEY, Context.MODE_PRIVATE)
    }

    fun getButtonAssignments(): Map<String, String> {
        return mapOf(
            ButtonKeys.SETTINGS_BUTTON_KEY to (sharedPreferences.getString(ButtonKeys.SETTINGS_BUTTON_KEY, DEFAULT_ACTION) ?: DEFAULT_ACTION),
            ButtonKeys.HOME_BUTTON_KEY to (sharedPreferences.getString(ButtonKeys.HOME_BUTTON_KEY, DEFAULT_ACTION) ?: DEFAULT_ACTION),
            ButtonKeys.BACK_BUTTON_KEY to (sharedPreferences.getString(ButtonKeys.BACK_BUTTON_KEY, DEFAULT_ACTION) ?: DEFAULT_ACTION),
            ButtonKeys.RECENT_APPS_BUTTON_KEY to (sharedPreferences.getString(ButtonKeys.RECENT_APPS_BUTTON_KEY, DEFAULT_ACTION) ?: DEFAULT_ACTION),
            ButtonKeys.ADDITIONAL_SETTINGS_BUTTON_KEY to (sharedPreferences.getString(ButtonKeys.ADDITIONAL_SETTINGS_BUTTON_KEY, DEFAULT_ACTION) ?: DEFAULT_ACTION)
        )
    }
}