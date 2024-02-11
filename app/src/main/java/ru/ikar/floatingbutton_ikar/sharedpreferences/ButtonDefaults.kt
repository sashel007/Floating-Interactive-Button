package ru.ikar.floatingbutton_ikar.sharedpreferences

object ButtonDefaults {
    val defaultValue = mapOf(
        ButtonKeys.HOME_BUTTON_KEY to "home_value",
        ButtonKeys.BACK_BUTTON_KEY to "back_value",
        ButtonKeys.RECENT_APPS_BUTTON_KEY to "recentapps_value",
        ButtonKeys.SETTINGS_BUTTON_KEY to "settings_value",
        ButtonKeys.ADDITIONAL_SETTINGS_BUTTON_KEY to "additionalsettings_value"
    )
}