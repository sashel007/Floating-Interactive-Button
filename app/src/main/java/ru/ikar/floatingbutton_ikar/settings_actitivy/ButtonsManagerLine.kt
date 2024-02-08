package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.ikar.floatingbutton_ikar.R
import ru.ikar.floatingbutton_ikar.sharedpreference.ButtonFunction
import ru.ikar.floatingbutton_ikar.sharedpreference.availableFunctions

@Composable
fun ButtonsManagerLine(sharedPreferences: SharedPreferences) {

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
    var expanded by remember { mutableStateOf(false) }
    val context = LocalContext.current
    var expandedMenuIndex by remember {
        mutableStateOf<Int?>(null)
    }

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
            Spacer(modifier = Modifier.width(spaceBetweenBoxes))

            Box(
                modifier = Modifier
                    .size(boxSize)
                    .background(boxBackground)
                    .padding(2.dp)
            ) {
                Image(painterResource(id = imageResId),
                    contentDescription = "Базовая кнопка",
                    modifier = Modifier
                        .size(45.dp)
                        .clickable {
                            expandedMenuIndex = if (expandedMenuIndex == index) null else index
                        })

                Image(
                    painter = painterResource(id = R.drawable.back_button_cross_icon), // Идентификатор ресурса крестика
                    contentDescription = "Remove",
                    modifier = Modifier
                        .size(crossIconSize)
                        .align(Alignment.TopEnd)
                        .clickable(onClick = {
                            // Обработка нажатия на крестик
                        }),
                    contentScale = ContentScale.Fit
                )

                if (expandedMenuIndex != null) {
                    // Получаем текущую функцию кнопки из SharedPreferences
                    val currentFunctionKey = "function_${expandedMenuIndex}"
                    val currentFunction = sharedPreferences.getString(currentFunctionKey, "Не назначено") ?: "Не назначено"

                    FunctionSelectionMenu(
                        expandedMenuIndex = expandedMenuIndex,
                        currentFunction = currentFunction,
                        onFunctionSelected = { index, function ->
                            expandedMenuIndex = null // Скрыть меню после выбора
                            sharedPreferences.edit().putString(currentFunctionKey, function.value).apply()
                            Toast.makeText(context, "Функция '${function.name}' выбрана для кнопки $index", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun FunctionSelectionMenu(
    expandedMenuIndex: Int?,
    onFunctionSelected: (Int, ButtonFunction) -> Unit,
    currentFunction: String
) {
    if (expandedMenuIndex != null) {
        DropdownMenu(
            expanded = true,
            onDismissRequest = { onFunctionSelected(expandedMenuIndex, ButtonFunction("", "")) }
        ) {
            // Используем Text напрямую в параметре text DropdownMenuItem для заголовка
            DropdownMenuItem(
                onClick = { /* Обработка нажатия не требуется для заголовка */ },
                text = { Text("Назначение кнопки: $currentFunction", fontSize = 12.sp) }
            )
            // Добавляем разделитель после заголовка
            DropdownMenuItem(
                onClick = {}, // Пустая лямбда для заглушки, разделитель не должен быть кликабельным
                text = { Divider() }
            )
            availableFunctions.forEach { function ->
                DropdownMenuItem(
                    onClick = { onFunctionSelected(expandedMenuIndex, function) },
                    text = { Text(function.name, fontSize = 16.sp) } // Правильное использование text
                )
            }
        }
    }
}

