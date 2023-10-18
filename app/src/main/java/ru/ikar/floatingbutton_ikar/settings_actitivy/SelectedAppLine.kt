package ru.ikar.floatingbutton_ikar.settings_actitivy

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SelectedAppLine(
    appIcons: List<ImageBitmap>
) {
    val heightSize = 60.dp
    val spacerSize = 15.dp
    val boxSize = 50.dp // размер квадратного Box
    val boxBackground = Color.LightGray // фон для Box

    Text(
        text = "Выбранные элементы плавающей кнпоки: ", fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.size(spacerSize))
    Box(
        modifier = Modifier
            .height(heightSize)
            .fillMaxWidth()
            .background(Color.Gray)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.Gray),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            appIcons.take(4).forEach { bitmap ->
                Box(
                    modifier = Modifier
                        .size(boxSize)
                        .background(boxBackground)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                            onClick = {
                                // Обработка клика по изображению
                            }
                        )
                ) {
                    Image(
                        bitmap = bitmap,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
