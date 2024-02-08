package ru.ikar.floatingbutton_ikar.service

interface SettingsPanelController {
    fun showSettingsPanel(xOffset: Int, yOffset: Int)
    fun isSettingsPanelShown(): Boolean
}