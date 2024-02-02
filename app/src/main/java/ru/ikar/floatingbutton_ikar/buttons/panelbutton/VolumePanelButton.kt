package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.content.Context
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.material.slider.Slider

class VolumePanelButton(
    private val context: Context,
    private var isVolumeSliderShown: Boolean,
    private var isBrightnessSliderShown: Boolean,
    private val brightnessSliderLayout: FrameLayout,
    private var xTrackingDotsForPanel: Int,
    private var yTrackingDotsForPanel: Int,
    private val volumeSliderParams: WindowManager.LayoutParams,
    private val volumeSlider: Slider,
    private val audioManager: AudioManager,
    private val volumeSliderLayout: FrameLayout,
    private val updateVolumeSliderPosition: () -> Unit
) : PanelButton(context) {

    override fun onClick() {
        if (!isVolumeSliderShown) { // Условие, если ползунок еще не отображен
            // Сначала проверяем, отображается ли brightnessSlider
            if (isBrightnessSliderShown) {
                brightnessSliderLayout.visibility = View.GONE
                isBrightnessSliderShown = false
            }
            // Определяем параметры макета для ползунка громкости
            val volumeParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
            )

            // Устанавливаем позицию макета volumeSliderLayout
            volumeSliderParams.x = xTrackingDotsForPanel
            volumeSliderParams.y = yTrackingDotsForPanel
            // Обновляем ползунок согласно системным значениям громкости
            // Устанавливаем максимальное значение слайдера
            volumeSlider.apply {
                valueTo =
                    audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat()
                // Устанавливаем текущее значение слайдера
                value = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat()
            }
            volumeSliderLayout.visibility = View.VISIBLE
            isVolumeSliderShown = true
            updateVolumeSliderPosition()
//            resetHideVolumeSliderTimer()
        } else {
            // Если ползунок уже отображен, удаляем с экрана
            volumeSliderLayout.visibility = View.GONE
            isVolumeSliderShown = false
//            windowManager.removeView(volumeSliderLayout)
        }
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }
}