package ru.ikar.floatingbutton_ikar.service.buttons.additionalbuttons

import android.content.Context
import android.view.View
import ru.ikar.floatingbutton_ikar.service.SettingsPanelController
import ru.ikar.floatingbutton_ikar.service.buttons.Button
import ru.ikar.floatingbutton_ikar.service.buttons.ButtonAnimator

class AdditionalSettingsButton(
    private val context: Context,
    private val panelController: SettingsPanelController
) : Button {
    private val animator = ButtonAnimator(context)
    private val xOffset = 300
    private val yOffset = 100
    override fun onClick() {
        // Проверяем, отображена ли панель настроек
        if (!panelController.isSettingsPanelShown()) {
            panelController.showSettingsPanel(xOffset, yOffset)
        } else {
            // Скрываем панель настроек
            panelController.showSettingsPanel(xOffset, yOffset)
        }
    }

    override fun animateButton(button: View) {
        animator.animateButton(button)
    }

}