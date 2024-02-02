package ru.ikar.floatingbutton_ikar.service.buttons

import android.content.Context
import android.view.View
import android.view.animation.AnimationUtils
import ru.ikar.floatingbutton_ikar.R

class ButtonAnimator(private val context: Context) {
    fun animateButton(button: View) {
        button.startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.button_animation)
        )
    }
}