package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import ru.ikar.floatingbutton_ikar.R
import ru.ikar.floatingbutton_ikar.sharedpreferences.ButtonDefaults.defaultValue
import ru.ikar.floatingbutton_ikar.sharedpreferences.ButtonFunction
import ru.ikar.floatingbutton_ikar.sharedpreferences.ButtonKeys
import ru.ikar.floatingbutton_ikar.sharedpreferences.SharedPrefHandler
import ru.ikar.floatingbutton_ikar.sharedpreferences.availableFunctions

@Composable
fun ButtonsManagerLine(
    sharedPreferences: SharedPreferences,
    sharedPrefHandler: SharedPrefHandler
) {
    val spaceBetweenBoxes = 16.dp
    val boxSize = 50.dp
    val crossIconSize = 16.dp
    val boxBackground = Color.LightGray
    val spacerSize = 8.dp
    val buttonsImageList = listOf(
        R.drawable.home_button,
        R.drawable.back_button,
        R.drawable.show_all_running_apps_button,
        R.drawable.settings_button_icon,
        R.drawable.additional_settings_icon
    )
    var expandedMenuIndex by remember { mutableStateOf<Int?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var currentFunction by remember { mutableStateOf("") }
    val setCurrentFunction: (String) -> Unit = { newValue ->
        currentFunction = newValue
    }
    val functionValueToNameMap = availableFunctions.associateBy({ it.value }, { it.name })
    val buttonKeysList = listOf(
        ButtonKeys.HOME_BUTTON_KEY,
        ButtonKeys.BACK_BUTTON_KEY,
        ButtonKeys.RECENT_APPS_BUTTON_KEY,
        ButtonKeys.SETTINGS_BUTTON_KEY,
        ButtonKeys.ADDITIONAL_SETTINGS_BUTTON_KEY
    )
    Text(
        text = "Определите назначение кнопок: ",
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.size(spacerSize))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = spaceBetweenBoxes)
            .wrapContentWidth(), horizontalArrangement = Arrangement.SpaceBetween
    ) {
        buttonsImageList.forEachIndexed { index, imageResId ->
            val buttonKey = buttonKeysList[index]
            val buttonFunction = sharedPreferences.getString(buttonKey, defaultValue[buttonKey]) ?: "Не найдено"
            val isVisible = remember { mutableStateOf(sharedPrefHandler.getButtonVisibility(buttonKey)) }

            Spacer(modifier = Modifier.width(spaceBetweenBoxes))

            Box(
                modifier = Modifier
                    .size(boxSize)
                    .background(boxBackground)
                    .padding(2.dp)
            ) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = "Базовая кнопка",
                    modifier = Modifier
                        .size(45.dp)
                        .alpha(if (isVisible.value) 1f else 0.5f) // Изменение прозрачности
                        .clickable {
                            if (!isVisible.value) {
                                // Если кнопка скрыта, делаем её видимой и обновляем SharedPreferences
                                isVisible.value = true
                                sharedPrefHandler.setButtonVisibility(buttonKey, true)
                            } else {
                                // Если кнопка видима, открываем диалог
                                expandedMenuIndex = index
                                currentFunction = buttonFunction
                                showDialog = true
                            }
                        }
                )

                if (isVisible.value) {
                    // Скрыть кнопку
                    Image(
                        painter = painterResource(id = R.drawable.back_button_cross_icon), // Идентификатор ресурса крестика
                        contentDescription = "Remove",
                        modifier = Modifier
                            .size(crossIconSize)
                            .align(Alignment.TopEnd)
                            .clickable(onClick = {
                                isVisible.value = false // Обновление состояния видимости
                                sharedPrefHandler.setButtonVisibility(buttonKeysList[index], false)
                            }),
                        contentScale = ContentScale.Fit
                    )
                }

                if (expandedMenuIndex == index) {
                    val buttonKey = buttonKeysList[expandedMenuIndex!!]
                    // Получаем текущую функцию кнопки из SharedPreferences используя правильный ключ
                    val functionValue = sharedPreferences.getString(buttonKey, defaultValue[buttonKey]) ?: "Не найдено"
                    val currentFunctionName = functionValueToNameMap[functionValue] ?: "Не назначено"
                    val imageResId = buttonsImageList.getOrNull(expandedMenuIndex!!) ?: R.drawable.any_button_icon

                    FunctionSelectionDialog(
                        showDialog = showDialog,
                        onDismissRequest = { showDialog = false },
                        initialFunction = currentFunctionName,
                        onFunctionSelected = { function ->
                            // Сохраняем выбранную функцию используя правильный ключ
                            sharedPreferences.edit().putString(buttonKey, function.value).apply()
                            // Обновляем текущую функцию для отображения
                            currentFunction = function.name // Используем имя функции для отображения
                        },
                        setCurrentFunction = setCurrentFunction,
                        imageResId = imageResId
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.width(spaceBetweenBoxes))

    ElevatedButton(
        onClick = {  sharedPrefHandler.resetButtonAssignmentsToDefault() }
    ) {
        Text(
            text = "Восстановить кнопки до исходных значений",
            fontSize = 8.sp
        )
    }
}

@Composable
fun FunctionSelectionDialog(
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
    initialFunction: String,
    onFunctionSelected: (ButtonFunction) -> Unit,
    setCurrentFunction: (String) -> Unit,
    imageResId: Int
) {
    if (showDialog) {
        var currentFunction by remember {
            mutableStateOf(initialFunction)
        }

        Dialog(onDismissRequest = { onDismissRequest() }) {
            Surface(
                shape = RoundedCornerShape(12.dp), // Закругленные углы для Surface
                shadowElevation = 8.dp, // Эффект поднятия через тень
            ) {
                Column(
                    modifier = Modifier
                        .background(Color.White)
                        .padding(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(id = imageResId),
                            contentDescription = "Выбранная кнопка",
                            modifier = Modifier
                                .size(40.dp)
                                .padding(end = 8.dp)
                        )

                        Text("Назначение кнопки: $currentFunction", fontSize = 12.sp)
                    }
                    Divider()
                    availableFunctions.forEach { function ->
                        Text(
                            text = function.name,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // Обновляем currentFunction, используя название функции, а не её техническое значение
                                    currentFunction =
                                        function.name // Используйте название функции напрямую
                                    onFunctionSelected(function)
                                    setCurrentFunction(function.name) // Отправляем название функции в ButtonsManagerLine
                                }
                                .padding(8.dp)
                        )
                    }
                }
            }
        }

    }
}