package ru.ikar.floatingbutton_ikar.service.buttons

import android.view.View

interface Button {
    fun onClick()
    fun animateButton(button: View)
}