package ru.ikar.floatingbutton_ikar.sharedpreferences

object ButtonKeys {
    const val HOME_BUTTON_KEY = "home_button"
    const val BACK_BUTTON_KEY = "back_button"
    const val RECENT_APPS_BUTTON_KEY = "show_all_running_apps_button"
    const val SETTINGS_BUTTON_KEY = "settings_button"
    const val ADDITIONAL_SETTINGS_BUTTON_KEY = "additional_settings_button"

    // Список всех ключей для удобства перебора при сбросе
    val keysFromButtonKeysObject = listOf(
        HOME_BUTTON_KEY,
        BACK_BUTTON_KEY,
        RECENT_APPS_BUTTON_KEY,
        SETTINGS_BUTTON_KEY,
        ADDITIONAL_SETTINGS_BUTTON_KEY
    )
}

