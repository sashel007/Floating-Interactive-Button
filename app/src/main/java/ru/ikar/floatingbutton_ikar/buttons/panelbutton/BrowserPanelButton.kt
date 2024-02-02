package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.view.View

class BrowserPanelButton(
    private val context: Context,
    private val packageManager: PackageManager) : PanelButton(context) {

    override fun onClick() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("http://www.google.com"))
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // Добавление флага
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
        }
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }
}