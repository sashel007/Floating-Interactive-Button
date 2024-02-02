package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.View

class WifiPanelButton(private val context: Context) : PanelButton(context) {

    override fun onClick() {
        val wifiIntent = Intent(Settings.Panel.ACTION_WIFI)
        wifiIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Добавление флага
        context.startActivity(wifiIntent)
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }

}