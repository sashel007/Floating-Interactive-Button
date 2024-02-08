package ru.ikar.floatingbutton_ikar.service

interface VolumeControllerListener {
    fun showOrHideVolumeSlider()
    fun isVolumeSliderShown(): Boolean
}