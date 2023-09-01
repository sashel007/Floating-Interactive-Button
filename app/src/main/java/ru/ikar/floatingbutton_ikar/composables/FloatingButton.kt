package ru.ikar.floatingbutton_ikar.composables

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ru.ikar.floatingbutton_ikar.R
import ru.ikar.floatingbutton_ikar.ui.theme.SelectBackgroundActivity
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun FloatingButton() {
    val context = LocalContext.current
    val basicButtonSize = 40.dp
    val iconButtonSize = 30.dp
    var isMenuVisible by remember { mutableStateOf(false) } // State to control menu visibility
    var toucherPosition by remember { mutableStateOf(Offset(0f, 0f)) }
    var isDragging by remember { mutableStateOf(false) }
    var touchOffset by remember { mutableStateOf(Offset(0f, 0f)) }
    val buttonColors = Color(0xFF9B1E1E)
    var isVolumeSliderVisible by remember { mutableStateOf(false) }
    var isBrightnessSliderVisible by remember { mutableStateOf(false) }
    var volumeIconX by remember { mutableStateOf(0f) }
    var volumeIconY by remember { mutableStateOf(0f) }
    var sliderPosition by remember { mutableStateOf(0f) }
    var brightnessPosition by remember { mutableStateOf(0f) }

    fun getSystemIcon(index: Int): Int {
        return when (index) {
            0 -> R.drawable.baseline_settings_24
            1 -> R.drawable.baseline_home_24
            2 -> R.drawable.baseline_volume_up_24
            3 -> R.drawable.baseline_brightness_medium_24
            4 -> R.drawable.baseline_desktop_mac_24
            else -> R.drawable.baseline_settings_24 // Return null for any other index or add more cases as needed
        }
    }

    fun openSettings(context: Context) {
        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
        context.startActivity(settingsIntent)
    }
    Box(
        Modifier
            .fillMaxSize()
            .border(
                width = 10.dp,
                color = Color.Green
            )) {
        Box(
            modifier = Modifier
                .size(basicButtonSize)
                .graphicsLayer(
                    translationX = toucherPosition.x,
                    translationY = toucherPosition.y
                )
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        if (zoom == 1f) {
                            if (isDragging) {
                                toucherPosition = toucherPosition.plus(pan)
                            } else {
                                isDragging = true
                                touchOffset = Offset(
                                    toucherPosition.x - pan.x,
                                    toucherPosition.y - pan.y
                                )
                            }
                        }
                    }
                    detectTapGestures {
                        isMenuVisible = !isMenuVisible // Toggle the menu on button tap
                    }
                }
        ) {
            Button(
                onClick = {
                    // Handle main button click action here
                    isMenuVisible = !isMenuVisible // Toggle the menu on button click
                },
                modifier = Modifier.fillMaxSize(),
                colors = ButtonDefaults.buttonColors(buttonColors.copy(alpha = 0.8f))
            ) {
            }

            // Little buttons (5 buttons around the main button)
            if (isMenuVisible) {
                Box(Modifier.fillMaxSize()) {
                    for (i in 0 until 5) {
                        val angle = i * (360f / 5f)
                        val radius = 120.dp // Adjust the radius as needed
                        val x = cos(Math.toRadians(angle.toDouble())).toFloat() * radius.value
                        val y = sin(Math.toRadians(angle.toDouble())).toFloat() * radius.value

                        if (i == 2) {  // If the current button is the volume icon
                            volumeIconX = with(LocalDensity.current) { x.dp.toPx() }
                            volumeIconY = with(LocalDensity.current) { y.dp.toPx() }
                        }

                        Box(
                            modifier = Modifier
                                .size(iconButtonSize)
                                .offset { IntOffset(x.roundToInt(), y.roundToInt()) }
                        ) {
                            val systemIcon = painterResource(getSystemIcon(i))
                            Image(
                                painter = systemIcon,
                                contentDescription = "System Icon",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        onClick = {
                                            when (i) {
                                                0 -> openSettings(context)
                                                1 -> {
                                                    TODO()
                                                }

                                                2 -> {
                                                    isVolumeSliderVisible = !isVolumeSliderVisible
                                                }

                                                3 -> {
                                                    isBrightnessSliderVisible =
                                                        !isBrightnessSliderVisible
                                                }

                                                4 -> {
                                                    val intent = Intent(
                                                        context,
                                                        SelectBackgroundActivity::class.java
                                                    )
                                                    context.startActivity(intent)
                                                }

                                                else -> {
                                                    TODO()
                                                }
                                            }
                                        }
                                    ),
                                contentScale = ContentScale.Fit // Adjust content scale as needed
                            )
                        }
                    }
                }
            }
        }
        if (isVolumeSliderVisible) {
            Box(modifier = Modifier
                .border(2.dp, Color.Yellow)
                .semantics { contentDescription = "Localized Description" }
                .rotate(-90f)
                .width(150.dp)
                .align(Alignment.CenterEnd)
            )
            {
                VolumeSlider(
                    value = sliderPosition,
                    onValueChange = { newSliderPosition ->
                        //прикрутка громкости через аудиоменеджер
                        sliderPosition = newSliderPosition
                        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val newVolume = (newSliderPosition * maxVolume).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
                    }
                )
            }
        }

        if (isBrightnessSliderVisible) {
            context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
            Box(modifier = Modifier
                .border(2.dp, Color.Yellow)
                .semantics { contentDescription = "Localized Description" }
                .rotate(-90f)
                .width(150.dp)
                .align(Alignment.CenterEnd)
            )
            {
                BrightnessSlider(
                    value = brightnessPosition,
                    onValueChange = { newBrightnessPosition ->
                        //прикрутка яркости через Setting.SYSTEM
                        brightnessPosition = newBrightnessPosition
                        val contentResolver = context.contentResolver
                        val brightnessMode = Settings.System.getInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS_MODE
                        )
                        if (brightnessMode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) {
                            // If the brightness mode is set to automatic, switch to manual mode
                            Settings.System.putInt(
                                contentResolver,
                                Settings.System.SCREEN_BRIGHTNESS_MODE,
                                Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL
                            )
                        }
                        val maxBrightness = 255
                        val newBrightness = (newBrightnessPosition * maxBrightness).toInt()
                        Settings.System.putInt(
                            contentResolver,
                            Settings.System.SCREEN_BRIGHTNESS,
                            newBrightness
                        )
                    }
                )
            }
        }
    }


}