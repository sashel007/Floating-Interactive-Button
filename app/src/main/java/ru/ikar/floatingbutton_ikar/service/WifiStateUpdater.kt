package ru.ikar.floatingbutton_ikar.service

interface WifiStateUpdater {
    fun updateWifiButtonState(isWifiEnabled: Boolean)
}