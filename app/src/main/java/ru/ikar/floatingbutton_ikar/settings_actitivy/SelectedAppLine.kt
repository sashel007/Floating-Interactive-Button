package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.media.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SelectedAppLine(
    buttonResourcesState: MutableState<List<ImageResource>>,
    onResourcesUpdated: (List<ImageResource>) -> Unit,
    onAppSelected: (ImageResource) -> Unit  // Эта функция будет добавлять иконку приложения
) {
    val buttonResources = buttonResourcesState.value
    var showRemoveDialog by remember { mutableStateOf(false) }
    var selectedButtonIndex by remember { mutableStateOf(-1) }

    val heightSize = 60.dp
    val spacerSize = 15.dp

    Text(
        text = "Выбранные элементы плавающей кнпоки: ",
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.size(spacerSize))
    Box(
        modifier = Modifier
            .height(heightSize)
            .fillMaxWidth()
            .background(Color.Gray)
    ) {
        LazyRow(modifier = Modifier.background(Color.Gray)){
            items(buttonResources.size) { index ->
                val resource = buttonResources[index]
                when (resource) {
                    is ImageResource.ButtonResource -> {
                        Image(
                            painter = painterResource(id = resource.resId),
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        selectedButtonIndex = index
                                        showRemoveDialog = true
                                    }
                                )
                        )
                    }
                    is ImageResource.AppIcon -> {
                        Image(
                            bitmap = resource.bitmap,
                            contentDescription = null,
                            modifier = Modifier
                                .size(24.dp)
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = {
                                        selectedButtonIndex = index
                                        showRemoveDialog = true
                                    }
                                )
                        )
                    }
                }
            }

        }
    }
    if (showRemoveDialog) {
        AlertDialog(
            title = { Text("Удалить кнопку?") },
            confirmButton = {
                TextButton(onClick = {
                    val updatedList = buttonResources.filterIndexed { i, _ -> i != selectedButtonIndex }
                    buttonResourcesState.value = updatedList
                    onResourcesUpdated(updatedList)  // Здесь мы уведомляем об изменении
                    showRemoveDialog = false
                }) {
                    Text("Удалить")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveDialog = false }) {
                    Text("Отмена")
                }
            },
            onDismissRequest = { showRemoveDialog = false }
        )
    }

}
