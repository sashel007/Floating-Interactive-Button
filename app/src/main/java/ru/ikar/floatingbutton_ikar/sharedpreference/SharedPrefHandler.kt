package ru.ikar.floatingbutton_ikar.sharedpreference

import android.content.Context
import android.content.SharedPreferences

class SharedPrefHandler(context: Context, sharedPrefName: String) {
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
                "home_button_key",
                "back_button_key",
                "recentapps_button_key",
                "settings_button_key",
                "additionalsettings_button_key"
            )
            initializeDefaultButtonFunctions()
        }
    }

    private fun initializeDefaultButtonFunctions() {
        val defaultValue = mapOf(
            "home_button_key" to "home_value",
            "back_button_key" to "back_value",
            "recentapps_button_key" to "recentapps_value",
            "settings_button_key" to "settings_value",
            "additionalsettings_button_key" to "additionalsettings_value"
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

    fun saveSharedPrefValue(key: String, value: String) {
        sharedPref.edit().putString(key, value).apply()
    }

    fun getSharedPrefValue(key: String, value: String? = null): String? {
        return sharedPref.getString(key, value)
    }
}