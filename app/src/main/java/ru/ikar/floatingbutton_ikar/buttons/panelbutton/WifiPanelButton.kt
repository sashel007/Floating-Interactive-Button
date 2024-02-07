package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.provider.Settings
import android.view.View
import ru.ikar.floatingbutton_ikar.R
import ru.ikar.floatingbutton_ikar.service.WifiStateUpdater

class WifiPanelButton(
    private val context: Context,
    val wifiButton: View,
    private val stateUpdater: WifiStateUpdater
) :
    PanelButton(context) {

    override fun onClick() {
        val wifiManager =
            context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        wifiManager?.let {
            val isWifiEnabled = it.isWifiEnabled
            // Уведомляем внешний код о необходимости обновления состояния кнопки
            stateUpdater.updateWifiButtonState(isWifiEnabled)
        }

        val wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
        wifiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Добавление флага
        context.startActivity(wifiIntent)
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }

    fun updateButtonBackground(isWifiEnabled: Boolean) {
        val color = if (isWifiEnabled) R.color.bluetooth_on else android.R.color.transparent
        wifiButton.setBackgroundColor(context.resources.getColor(color, null))
    }

}