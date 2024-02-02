package ru.ikar.floatingbutton_ikar.buttons.panelbutton

import android.content.Context
import android.view.View
import ru.ikar.floatingbutton_ikar.projector.Projector
import ru.ikar.floatingbutton_ikar.service.buttons.panelbutton.PanelButton

class ProjectorPanelButton(
    private val context: Context,
    private val projector: Projector
): PanelButton(context) {
    override fun onClick() {
        projector.show()
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }
}