package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.content.Context
import android.view.View
import androidx.compose.animation.animateColorAsState
import ru.ikar.floatingbutton_ikar.service.buttons.Button
import ru.ikar.floatingbutton_ikar.service.buttons.ButtonAnimator

abstract class PanelButton(private val context: Context) : Button {
    private val animator = ButtonAnimator(context)

    override fun onClick() {}
    override fun animateButton(button: View) {
        animator.animateButton(button)
    }

}