package ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons

import android.content.Context
import android.content.Intent
import android.view.View
import ru.ikar.floatingbutton_ikar.service.FloatingButtonService
import ru.ikar.floatingbutton_ikar.service.buttons.Button
import ru.ikar.floatingbutton_ikar.service.buttons.ButtonAnimator

class SettingsButton(private val context: Context) : Button {
    private val animator = ButtonAnimator(context)
    override fun onClick() {
        val navSetIntent = Intent(FloatingButtonService.navigationSettings)
        context.sendBroadcast(navSetIntent)
    }

    override fun animateButton(button: View) {
        animator.animateButton(button)
    }
}