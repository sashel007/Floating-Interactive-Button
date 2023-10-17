package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.Context
import android.content.SharedPreferences
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Composable
fun SystemAppList(
    apps: (context: Context) -> List<AppInfo>,
    sharedPreferences: SharedPreferences,
    updateAppIcons: () -> Unit
) {
    val context = LocalContext.current
    val appsList = apps(context)
    val colorList = listOf(Color.Red, Color.Blue, Color.Red)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .border(4.dp, Brush.radialGradient(colors = colorList), RectangleShape)
            .fillMaxSize()
    ) {
        items(appsList.size) { index ->
            SystemAppItem(appsList[index], sharedPreferences, updateAppIcons)
        }
    }
}

@Composable
fun SystemAppItem(
    app: AppInfo, sharedPreferences: SharedPreferences, updateAppIcons: () -> Unit

) {
    val iconBitmap = app.icon
    val appName = app.packageName
    var showDialog by remember { mutableStateOf(false) }
    val squaredSize = 40.dp
    val colorList = listOf(Color.Red, Color.Green, Color.Blue)

    Box(contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(8.dp)
            .width(squaredSize)
            .height(squaredSize)
            .background(color = Color.LightGray)
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
        AlertDialog(onDismissRequest = { showDialog = false },
            text = { Text("Добавить приложение к кнопке?") },
            confirmButton = {
                Button(onClick = {
                    val jsonString = sharedPreferences.getString("selected_packages", "")
                    val currentPackageNames: MutableList<String> = if (jsonString != "") {
                        Gson().fromJson(
                            jsonString, object : TypeToken<MutableList<String>>() {}.type
                        )
                    } else {
                        mutableListOf()
                    }

                    // Добавляем новое имя пакета
                    currentPackageNames.add(appName)

                    // Сохраняем обновленный список обратно в SharedPreferences
                    val newJsonString = Gson().toJson(currentPackageNames)
                    sharedPreferences.edit().putString("selected_packages", newJsonString).apply()

                    // Обновляем иконки
                    updateAppIcons()


                    showDialog = false
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


