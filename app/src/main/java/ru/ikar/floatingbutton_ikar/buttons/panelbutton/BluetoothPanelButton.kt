package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.view.View
import ru.ikar.floatingbutton_ikar.R

class BluetoothPanelButton(
    private val context: Context,
    private val bluetoothAdapter: BluetoothAdapter,
    private val button: View
) : PanelButton(context) {
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

    fun updateButtonBackground(isEnabled: Boolean) {
        val color = if (isEnabled) R.color.bluetooth_on else android.R.color.transparent
        button.setBackgroundColor(context.resources.getColor(color, null))
    }
}