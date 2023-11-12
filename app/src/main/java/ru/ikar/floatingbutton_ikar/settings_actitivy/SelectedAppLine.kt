package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SelectedAppLine(
    appIcons: List<ImageBitmap>,
    apps: (context: Context) -> List<AppInfo>,
    sharedPreferences: SharedPreferences,
    updateAppIcons: () -> Unit,
    stopService: () -> Unit,
    startService: () -> Unit
) {
    val spacerSize = 15.dp
    val boxSize = 50.dp
    val boxBackground = Color.LightGray
    val spaceBetweenBoxes = 16.dp
    val packageManager = LocalContext.current.packageManager
    var showDialogWithButtonsIndex by remember { mutableStateOf<Int?>(null) }
    var activeDialog by remember { mutableStateOf<Int?>(null) }

    Text(
        text = "Нажмите на ячейку, \nчтобы добавить/удалить:",
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
        // Используем цикл, чтобы создать 4 блока
        for (index in 0..3) {
            val key = "package_name_key_${index}"
            val packageName = sharedPreferences.getString(key, null)
            var bitmap: ImageBitmap? = null

            if (packageName != null) {
                val drawable: Drawable? = try {
                    packageManager.getApplicationIcon(packageName)
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
                bitmap = when (drawable) {
                    is BitmapDrawable -> drawable.bitmap.asImageBitmap()
                    is AdaptiveIconDrawable -> {
                        val width = drawable.intrinsicWidth
                        val height = drawable.intrinsicHeight
                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = android.graphics.Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                        bitmap.asImageBitmap()
                    }
                    else -> null
                }
            }
            Spacer(modifier = Modifier.width(spaceBetweenBoxes))

            Box(
                modifier = Modifier
                    .size(boxSize)
                    .background(boxBackground)
                    .padding(spaceBetweenBoxes / 2) // Добавлено это
                    .clickable(indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = { showDialogWithButtonsIndex = index })
            ) {
                // Если у нас есть иконка для этого блока, отобразим её
                bitmap?.let {
                    Image(
                        bitmap = it, contentDescription = null, modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    showDialogWithButtonsIndex?.let { index ->
        val key = "package_name_key_${index}"
        val packageName = sharedPreferences.getString(key, null)
        AlertDialog(onDismissRequest = { showDialogWithButtonsIndex = null }, title = {
            Box(contentAlignment = Alignment.Center) {
                Text("Выберите действие с кнопкой:")
            }
        }, text = {
            Box(contentAlignment = Alignment.Center) {}
        }, confirmButton = {
            Button(onClick = {
                activeDialog = index
                showDialogWithButtonsIndex = null
//                stopService()
//                startService()
            }) {
                Text("Добавить")
            }
        }, dismissButton = if (packageName != null) {
            {
                Button(onClick = {
                    // Удаление ключа и значения из SharedPreferences
                    val editor = sharedPreferences.edit()
                    editor.remove("package_name_key_$showDialogWithButtonsIndex")
                    editor.apply()
                    // Обновление иконок приложения (если требуется)
                    updateAppIcons()
                    // Закрытие диалога
                    showDialogWithButtonsIndex = null
                }) {
                    Text("Удалить")
                }
            }
        } else null
        )
    }

    activeDialog?.let { index ->
        ShowDialog(index, { activeDialog = null }, apps, sharedPreferences, updateAppIcons)
    }
}

@Composable
fun ShowDialog(
    dialogId: Int,
    onClose: () -> Unit,
    apps: (context: Context) -> List<AppInfo>,
    sharedPreferences: SharedPreferences,
    updateAppIcons: () -> Unit
) {
    val key = "package_name_key_$dialogId"
    Dialog(onDismissRequest = onClose) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)  // 90% экрана по ширине
                .fillMaxHeight(0.8f) // 80% экрана по высоте
                .background(Color.White)
        ) {
            // Вставляем ваш список приложений
            SystemAppList(key, apps, sharedPreferences, updateAppIcons, onClose)
        }
    }
}