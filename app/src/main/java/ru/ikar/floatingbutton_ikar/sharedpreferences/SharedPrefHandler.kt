package ru.ikar.floatingbutton_ikar.sharedpreferences

import android.content.Context
import android.content.SharedPreferences

class SharedPrefHandler(
    context: Context, sharedPrefName: String) {
    var sharedPref: SharedPreferences =
        context.getSharedPreferences(sharedPrefName, Context.MODE_PRIVATE)
    var keys: List<String>

    init {
        if (sharedPrefName == "app_package_names") {
            keys = listOf(
                "package_name_key_1",
                "package_name_key_2",
                "package_name_key_3",
                "package_name_key_4"
            )
        } else {
            keys = listOf(
                ButtonKeys.HOME_BUTTON_KEY,
                ButtonKeys.BACK_BUTTON_KEY,
                ButtonKeys.RECENT_APPS_BUTTON_KEY,
                ButtonKeys.SETTINGS_BUTTON_KEY,
                ButtonKeys.ADDITIONAL_SETTINGS_BUTTON_KEY
            )
            initializeDefaultButtonFunctions()
        }
    }

    private fun initializeDefaultButtonFunctions() {
        val defaultValue = mapOf(
            ButtonKeys.HOME_BUTTON_KEY to "home_value",
            ButtonKeys.BACK_BUTTON_KEY to "back_value",
            ButtonKeys.RECENT_APPS_BUTTON_KEY to "recentapps_value",
            ButtonKeys.SETTINGS_BUTTON_KEY to "settings_value",
            ButtonKeys.ADDITIONAL_SETTINGS_BUTTON_KEY to "additionalsettings_value"
        )

        with(sharedPref.edit()) {
            defaultValue.forEach { (key, value) ->
                if (!sharedPref.contains(key)) {
                    putString(key, value)
                }
            }
            apply()
        }
    }

    fun resetButtonAssignmentsToDefault() {
        with(sharedPref.edit()) {
            ButtonDefaults.defaultValue.forEach { (key, value) ->
                putString(key, value)
            }
            apply()
        }
    }

    fun saveSharedPrefValue(key: String, value: String) {
        sharedPref.edit().putString(key, value).apply()
    }

    fun getSharedPrefValue(key: String, value: String? = null): String? {
        return sharedPref.getString(key, value)
    }
}