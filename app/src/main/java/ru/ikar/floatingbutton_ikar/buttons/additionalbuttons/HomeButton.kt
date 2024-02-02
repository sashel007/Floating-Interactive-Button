package ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons

import android.content.Context
import android.content.Intent
import android.view.View
import androidx.core.content.ContextCompat
import ru.ikar.floatingbutton_ikar.service.buttons.Button
import ru.ikar.floatingbutton_ikar.service.buttons.ButtonAnimator

class HomeButton(private val context: Context) : Button {
    private val animator = ButtonAnimator(context)
    override fun onClick() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        ContextCompat.startActivity(context, homeIntent, null)
    }

    override fun animateButton(button: View) {
        animator.animateButton(button)
    }
}