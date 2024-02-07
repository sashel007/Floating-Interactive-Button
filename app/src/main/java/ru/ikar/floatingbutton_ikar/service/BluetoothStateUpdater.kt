package ru.ikar.floatingbutton_ikar.service

interface BluetoothStateUpdater {
    fun updateBluetoothButtonState(isBluetoothEnabled: Boolean)
}