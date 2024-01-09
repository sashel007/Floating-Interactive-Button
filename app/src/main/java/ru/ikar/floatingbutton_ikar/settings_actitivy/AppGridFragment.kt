package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties

@Composable
fun SystemAppList(
    key: String,
    apps: (context: Context) -> List<AppInfo>,
    sharedPreferences: SharedPreferences,
    updateAppIcons: () -> Unit,
    onClose: () -> Unit  // Добавляем этот параметр
) {
    val context = LocalContext.current
    val appsList = apps(context)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(15.dp))
            .background(Color.White) // или любой другой цвет фона
    ) {
        items(appsList.size) { index ->
            SystemAppItem(key, appsList[index], sharedPreferences, updateAppIcons, onClose)
        }
    }
}

@Composable
fun SystemAppItem(
    key: String,
    app: AppInfo,
    sharedPreferences: SharedPreferences,
    updateAppIcons: () -> Unit,
    onClose: () -> Unit  // Добавляем этот параметр
) {
    val iconBitmap = app.icon
    val appName = app.packageName
    var showDialog by remember { mutableStateOf(false) }
    val squaredSize = 40.dp
    val context = LocalContext.current

    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(8.dp)
            .width(squaredSize)
            .height(squaredSize)
            .clickable { showDialog = true }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp)
            )
            Text(
                appName,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = { Text("Добавить приложение к кнопке?") },
            confirmButton = {
                Button(onClick = {
                    // Сохраняем имя пакета с уникальным ключом в SharedPreferences
                    sharedPreferences.edit().putString(key, appName).apply()

                    // Обновляем иконки
                    updateAppIcons()
                    Toast.makeText(
                        context,
                        "Перезапустите сервис для корректного добавления кнопки",
                        Toast.LENGTH_SHORT
                    ).show()

                    showDialog = false
                    onClose()
                }) {
                    Text("Да")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Закрыть")
                }
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        )
    }
}


