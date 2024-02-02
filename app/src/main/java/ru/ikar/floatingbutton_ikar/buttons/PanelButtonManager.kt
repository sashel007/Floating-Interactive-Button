package ru.ikar.floatingbutton_ikar.buttons

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.material.slider.Slider
import ru.ikar.floatingbutton_ikar.R
import ru.ikar.floatingbutton_ikar.buttons.panelbutton.ProjectorPanelButton
import ru.ikar.floatingbutton_ikar.projector.Projector
import ru.ikar.floatingbutton_ikar.service.FloatingButtonService
import ru.ikar.floatingbutton_ikar.service.MuteStateListener
import ru.ikar.floatingbutton_ikar.service.buttons.panelbutton.BluetoothPanelButton
import ru.ikar.floatingbutton_ikar.service.buttons.panelbutton.BrowserPanelButton
import ru.ikar.floatingbutton_ikar.service.buttons.panelbutton.VolumeOffPanelButton
import ru.ikar.floatingbutton_ikar.service.buttons.panelbutton.VolumePanelButton
import ru.ikar.floatingbutton_ikar.service.buttons.panelbutton.WifiPanelButton

class PanelButtonManager(
    private val panelButtons: List<View>,
    context: Context,
    bluetoothAdapter: BluetoothAdapter,
    packageManager: PackageManager,
    muteStateListener: MuteStateListener,
    audioManager: AudioManager,
    floatingButtonService: FloatingButtonService,
    isMuted: Boolean,
    volumeSlider: Slider,
    currentVolume: Int,
    projector: Projector,
    brightnessSliderLayout: FrameLayout,
    isBrightnessSliderShown: Boolean,
    isVolumeSliderShown: Boolean,
    updateVolumeSliderPosition: () -> Unit,
    volumeSliderLayout: FrameLayout,
    volumeSliderParams: WindowManager.LayoutParams,
    xTrackingDotsForPanel: Int,
    yTrackingDotsForPanel: Int
) {
    private val wifiPanelButton = WifiPanelButton(context)
    private val bluetoothPanelButton = BluetoothPanelButton(context, bluetoothAdapter)
    private val volumePanelButton = VolumePanelButton(
        context,
        isVolumeSliderShown,
        isBrightnessSliderShown,
        brightnessSliderLayout,
        xTrackingDotsForPanel,
        yTrackingDotsForPanel,
        volumeSliderParams,
        volumeSlider,
        audioManager,
        volumeSliderLayout,
        updateVolumeSliderPosition
    )
    private val browserPanelButton = BrowserPanelButton(context, packageManager)
    private val volumeOffPanelButton = VolumeOffPanelButton(
        context,
        muteStateListener,
        floatingButtonService,
        isMuted,
        audioManager,
        volumeSlider,
        currentVolume
    )
    private val projectorPanelButton = ProjectorPanelButton(context, projector)

    private fun setListenersForPanelButtons() {
        for (button in panelButtons) {
            button.setOnClickListener {
                when (button.id) {
                    R.id.wifi_panel_btn -> button.setOnClickListener {
                        wifiPanelButton.apply {
                            onClick()
                            animateButton(button)
                        }
                    }

                    R.id.bluetooth_panel_btn -> button.setOnClickListener {
                        bluetoothPanelButton.apply {
                            onClick()
                            animateButton(button)
                        }
                    }

                    R.id.volume_panel_btn -> button.setOnClickListener {
                        volumePanelButton.apply {
                            onClick()
                            animateButton(button)
                        }
                    }

                    R.id.browser_panel_btn -> button.setOnClickListener {
                        browserPanelButton.apply {
                            onClick()
                            animateButton(button)
                        }
                    }

//                    R.id.brightness_panel_btn -> {
//                        animateButton(button)
//                        handleBrightnessBtn()
//                    }

                    R.id.volume_off_panel_btn -> button.setOnClickListener {
                        volumeOffPanelButton.apply {
                            onClick()
                            animateButton(button)
                        }
                    }

                    R.id.projector_panel_btn -> button.setOnClickListener {
                        projectorPanelButton.apply {
                            onClick()
                            animateButton(button)
                        }
                    }
                }
            }
        }
    }
}