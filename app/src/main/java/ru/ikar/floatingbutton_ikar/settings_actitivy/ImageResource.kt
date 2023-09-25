package ru.ikar.floatingbutton_ikar.settings_actitivy

import androidx.compose.ui.graphics.ImageBitmap

sealed class ImageResource {
    data class ButtonResource(val resId: Int) : ImageResource()
    data class AppIcon(val bitmap: ImageBitmap) : ImageResource()
}