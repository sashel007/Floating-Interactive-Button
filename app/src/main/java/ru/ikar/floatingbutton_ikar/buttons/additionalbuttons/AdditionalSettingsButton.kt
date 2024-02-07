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
    private val xOffset = 100
    private val yOffset = 100
    override fun onClick() {
        // Проверяем, отображена ли панель настроек
        if (!panelController.isSettingsPanelShown()) {
            panelController.showSettingsPanel(xOffset, yOffset)
        } else {
            // Скрываем панель настроек
            panelController.hideSettingsPanel()
        }

//        // Загружаем анимацию прозрачности
//        val fadeInAnim = AnimationUtils.loadAnimation(context, R.anim.fade_in_animation)
//
//        // Анимация исчезновения (fade out) - если нужна
//        val fadeOutAnim = AnimationUtils.loadAnimation(context, R.anim.fade_out_animation)
//
//        // Проверяем, отображена ли панель
//        if (!isSettingsPanelShown) {
//            // Устанавливаем позицию и размеры панели
//            val offset = convertDpToPixel(100, context)
//            val layoutParams = settingsPanelLayout.layoutParams as WindowManager.LayoutParams
//            layoutParams.x = xTrackingDotsForPanel + offset
//            layoutParams.y = yTrackingDotsForPanel
//            windowManager.updateViewLayout(settingsPanelLayout, layoutParams)
//
//            // Показываем панель и применяем анимацию появления
//            settingsPanelLayout.visibility = View.VISIBLE
//            isSettingsPanelShown = true
//            settingsPanelLayout.startAnimation(fadeInAnim)
//        } else {
//            // Применяем анимацию исчезновения и скрываем панель
//            settingsPanelLayout.startAnimation(fadeOutAnim)
//            settingsPanelLayout.visibility = View.GONE
//            isSettingsPanelShown = false
//        }
    }

    override fun animateButton(button: View) {
        animator.animateButton(button)
    }

    private fun convertDpToPixel(dp: Int, context: Context): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

}