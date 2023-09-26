package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.ikar.floatingbutton_ikar.FloatingButtonService

class SettingsActivity : ComponentActivity() {

    // Состояние, содержащее список ресурсов для плавающей кнопки.
    // Это список изображений, которые будут отображаться на плавающей кнопке.
    val buttonResourcesState: MutableState<List<ImageResource>> = mutableStateOf(listOf(
        ImageResource.ButtonResource(ButtonResources.backgroundButtonResource),
        ImageResource.ButtonResource(ButtonResources.anyButtonResource),
        ImageResource.ButtonResource(ButtonResources.brightnessButtonResource),
        ImageResource.ButtonResource(ButtonResources.settingsButtonResource),
        ImageResource.ButtonResource(ButtonResources.homeButtonResource),
        ImageResource.ButtonResource(ButtonResources.volumeButtonResource)
    ))

    private val OVERLAY_PERMISSION_REQ_CODE = 1001  // ваш код запроса для этого разрешения

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Отображаем экран настроек.
            SettingsScreen(buttonResourcesState, { updatedResources ->
                // Запускаем сервис с плавающей кнопкой с обновленными ресурсами.
                startFloatingButtonService(updatedResources)
            }, { context ->
                // Получаем список всех установленных приложений.
                getAllApps(context)
            })
        }

        // Проверка разрешения на отображение поверх других приложений.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Если у нас нет разрешения и версия ОС >= Marshmallow, то создаем намерение для запроса разрешения.
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            // Запускаем активность для результата (для получения ответа о предоставлении разрешения).
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            // Если разрешение уже предоставлено, то запускаем службу с плавающей кнопкой.
            startFloatingButtonService(buttonResourcesState.value)
        }
    }

    // Этот метод будет вызван после того, как пользователь предоставит или отклонит разрешение
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingButtonService(buttonResourcesState.value)
            } else {
                // Оповестите пользователя о том, что разрешение не предоставлено и почему оно необходимо
                Toast.makeText(this, "Нам нужно это разрешение для работы плавающей кнопки.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFloatingButtonService(selectedButtons: List<ImageResource>) {
        // Фильтруем список ресурсов, оставляя только те, которые являются ButtonResource,
        // и извлекаем из них идентификаторы ресурсов.
        val buttonIds = selectedButtons.filterIsInstance<ImageResource.ButtonResource>().map { it.resId }
        val intent = Intent(this, FloatingButtonService::class.java)
        // Передаем идентификаторы выбранных кнопок как дополнительные данные в интент
        intent.putIntegerArrayListExtra("selectedButtons", ArrayList(buttonIds))
        // Запускаем сервис.
        startService(intent)
    }

    fun Drawable.toImageBitmap(): ImageBitmap {
        // Если Drawable является экземпляром BitmapDrawable,
        // просто конвертируем содержащийся в нем Bitmap в ImageBitmap
        if (this is BitmapDrawable) {
            return this.bitmap.asImageBitmap()
        }
        // Создаем пустой Bitmap с размерами Drawable.
        val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
        // Используем Canvas для отрисовки Drawable на Bitmap.
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        // Конвертируем Bitmap в ImageBitmap.
        return bitmap.asImageBitmap()
    }

    fun getAllApps(context: Context): List<ImageResource> {
        // Получаем менеджер пакетов, который предоставляет
        // информацию о приложениях, установленных на устройстве.
        val pm = context.packageManager
        // Получаем список всех установленных приложений на устройстве.
        val apps = pm.getInstalledApplications(0)
        // Преобразуем этот список в список объектов ImageResource.
        return apps.map { appInfo ->
            // Для каждого приложения загружаем его иконку.
            val iconDrawable = appInfo.loadIcon(pm) // Получаем Drawable иконки
            // Преобразуем Drawable иконки приложения в ImageBitmap.
            // Это делается с помощью ранее предоставленной функции toImageBitmap.
            ImageResource.AppIcon(iconDrawable.toImageBitmap())
        }
    }
}

@Composable
fun SettingsScreen(
    buttonResourcesState: MutableState<List<ImageResource>>,
    onResourcesUpdated: (List<ImageResource>) -> Unit,
    getAllApps: (context: Context) -> List<ImageResource> // добавьте этот параметр
) {
    val context = LocalContext.current
    var isFloatingButtonOn by remember { mutableStateOf(false) }
    val buttonWidth = 200.dp
    val paddings = 20.dp
    val spacingSize = 50.dp
    val allApps = getAllApps(context)
    val allAppsState: MutableState<List<ImageResource>> = remember { mutableStateOf(allApps) }
    val selectedAppsState: MutableState<List<ImageResource>> = remember { mutableStateOf(listOf()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddings),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Switch(
            checked = isFloatingButtonOn,
            onCheckedChange = { isChecked ->
                isFloatingButtonOn = isChecked
                if (isChecked) {
                    context.startService(Intent(context, FloatingButtonService::class.java))
                } else {
                    context.stopService(Intent(context, FloatingButtonService::class.java))
                }
            }
        )
        Text(text = if (isFloatingButtonOn) "Floating Button is ON" else "Floating Button is OFF")

        Spacer(modifier = Modifier.size(spacingSize))

        ElevatedButton(
            onClick = {},
            modifier = Modifier.width(buttonWidth)
        ) {
            Text("Добавить кнопку")
        }

        ElevatedButton(
            onClick = {},
            modifier = Modifier.width(buttonWidth)
        ) {
            Text("__ANY__")
        }

        ElevatedButton(
            onClick = {},
            modifier = Modifier.width(buttonWidth)
        ) {
            Text("__ANY__")
        }

        Spacer(modifier = Modifier.size(spacingSize))

        SelectedAppLine(
            buttonResourcesState = buttonResourcesState,
            onResourcesUpdated = { updatedResources ->
                Log.d("selectedappline","selectedappline_enter")
                onResourcesUpdated(updatedResources)
            },
            onAppSelected = { selectedIcon ->
                val updatedResources = buttonResourcesState.value.toMutableList().apply {
                    add(selectedIcon)
                }
                Log.d("DEBUG_TAG", "Before updating resources")
                buttonResourcesState.value = updatedResources
                onResourcesUpdated(updatedResources)
                Log.d("DEBUG_TAG", "After updating resources")
            }
        )

        Spacer(modifier = Modifier.height(spacingSize))

        SystemAppList(allAppsState, onAppSelected = { selectedApp: ImageResource ->
            Log.d("selectedapp__","Before removal: ${allAppsState.value.size}")
            // Добавляем выбранное приложение в список selectedAppsState
            selectedAppsState.value = selectedAppsState.value + listOf(selectedApp)
            // Удаляем выбранное приложение из allAppsState
            allAppsState.value = allAppsState.value - listOf(selectedApp)
            Log.d("selected_appasd","After removal: ${allAppsState.value.size}")

            // This is the missing piece. Add the selected app to the buttonResourcesState and
            // notify the SelectedAppLine about the addition.
            val updatedResources = buttonResourcesState.value.toMutableList().apply {
                add(selectedApp)
            }
            buttonResourcesState.value = updatedResources
            onResourcesUpdated(updatedResources)
        })
    }
}

