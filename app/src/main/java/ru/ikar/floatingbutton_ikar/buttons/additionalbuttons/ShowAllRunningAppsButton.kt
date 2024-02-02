package ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons

import android.content.Context
import android.content.Intent
import android.view.View
import ru.ikar.floatingbutton_ikar.service.buttons.Button
import ru.ikar.floatingbutton_ikar.service.buttons.ButtonAnimator

class ShowAllRunningAppsButton(private val context: Context) : Button {
    private val animator = ButtonAnimator(context)
    override fun onClick() {
        val intent = Intent("com.xbh.action.RECENT_TASK")
        context.sendBroadcast(intent)
    }

    override fun animateButton(button: View) {
        animator.animateButton(button)
    }
}