package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.view.View

class BluetoothPanelButton(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter) : PanelButton(context) {
    override fun onClick() {
        if (bluetoothAdapter.isEnabled) {
            bluetoothAdapter.disable() // Выключить Bluetooth
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(enableBtIntent) // Запросить включение Bluetooth
        }
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }
}