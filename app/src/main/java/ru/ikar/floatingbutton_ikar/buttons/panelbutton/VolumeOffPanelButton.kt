package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.content.Context
import android.media.AudioManager
import android.view.View
import com.google.android.material.slider.Slider
import ru.ikar.floatingbutton_ikar.service.FloatingButtonService
import ru.ikar.floatingbutton_ikar.service.MuteStateListener

class VolumeOffPanelButton(
    private val context: Context,
    private val muteListener: MuteStateListener,
) : PanelButton(context) {

    override fun onClick() {
        if (muteListener.isMuted()) {
            muteListener.onMusic()
            // Включаем звук
//            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0)
//            volumeSlider.value = currentVolume.toFloat()
//            isMuted = false
//            muteListener.updateMuteButtonState()
        } else {
            // Выключаем звук
            muteListener.offMusic()
//            currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
//            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
//            volumeSlider.value = 0f
//            isMuted = true
//            muteListener.updateMuteButtonState()
        }
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }
}