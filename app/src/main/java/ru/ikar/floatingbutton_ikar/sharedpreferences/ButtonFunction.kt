package ru.ikar.floatingbutton_ikar.sharedpreferences

data class ButtonFunction(val name: String, val value: String)

val availableFunctions = listOf(
    ButtonFunction("Домой", "home_value"),
    ButtonFunction("Назад", "back_value"),
    ButtonFunction("Недавние приложения", "recentapps_value"),
    ButtonFunction("Настройки", "settings_value"),
    ButtonFunction("Панель настроек", "additionalsettings_value")
)