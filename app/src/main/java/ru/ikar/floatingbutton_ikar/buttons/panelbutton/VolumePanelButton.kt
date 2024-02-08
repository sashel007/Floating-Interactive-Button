package ru.ikar.floatingbutton_ikar.service.buttons.panelbutton

import android.content.Context
import android.view.View
import ru.ikar.floatingbutton_ikar.service.VolumeControllerListener

class VolumePanelButton(
    context: Context,
    private val volumeSliderController: VolumeControllerListener
) : PanelButton(context) {

    override fun onClick() {
        volumeSliderController.run {
            if (isVolumeSliderShown()) {
                showOrHideVolumeSlider()
            } else {
                showOrHideVolumeSlider()
            }
        }
    }

    override fun animateButton(button: View) {
        super.animateButton(button)
    }
}