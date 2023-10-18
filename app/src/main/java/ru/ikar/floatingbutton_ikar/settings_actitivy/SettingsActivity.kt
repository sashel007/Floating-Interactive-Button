package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
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
import androidx.annotation.RequiresApi
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

    private val OVERLAY_PERMISSION_REQ_CODE = 1001  // ваш код запроса для этого разрешения
    private lateinit var sharedPreferences: SharedPreferences

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = getSharedPreferences("app_package_names", Context.MODE_PRIVATE)
        val keys = listOf(
            "package_name_key_1",
            "package_name_key_2",
            "package_name_key_3",
            "package_name_key_4"
        )
        setContent {
            // Отображаем экран настроек.
            SettingsScreen(getAllApps = { context -> getAllApps(context) },
                sharedPreferences = sharedPreferences,
                getAppIconsFromKeys = { context -> getAppIconsFromKeys(context, keys) } )
        }
        logSharedPreferencesContents(sharedPreferences)


        // Проверка разрешения на отображение поверх других приложений.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            // Если у нас нет разрешения и версия ОС >= Marshmallow, то создаем намерение для запроса разрешения.
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName)
            )
            // Запускаем активность для результата (для получения ответа о предоставлении разрешения).
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
            // Если разрешение уже предоставлено, то запускаем службу с плавающей кнопкой.
            startFloatingButtonService()
        }
    }

    // Этот метод будет вызван после того, как пользователь предоставит или отклонит разрешение
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == OVERLAY_PERMISSION_REQ_CODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startFloatingButtonService()
            } else {
                // Оповестите пользователя о том, что разрешение не предоставлено и почему оно необходимо
                Toast.makeText(
                    this,
                    "Нам нужно это разрешение для работы плавающей кнопки.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun startFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        startService(intent)
    }

    private fun getAllApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        return apps.map { appInfo ->
            val iconDrawable = appInfo.loadIcon(pm)
            AppInfo(appInfo.packageName, iconDrawable.toImageBitmap())
        }
    }

    private fun Drawable.toImageBitmap(): ImageBitmap {
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

    private fun getAppIconsFromKeys(context: Context, keys: List<String>): List<ImageBitmap> {
        val pm = context.packageManager

        return keys.mapNotNull { key ->
            val packageName = sharedPreferences.getString(key, null)
            if (packageName != null) {
                try {
                    val appInfo = pm.getApplicationInfo(packageName, 0)
                    val iconDrawable = appInfo.loadIcon(pm)
                    iconDrawable.toImageBitmap()
                } catch (e: PackageManager.NameNotFoundException) {
                    null
                }
            } else {
                null
            }
        }
    }
    fun logSharedPreferencesContents(sharedPreferences: SharedPreferences) {
        val allEntries = sharedPreferences.all
        for ((key, value) in allEntries) {
            Log.d("SharedPreferences__", key + ": " + value.toString())
        }
    }


}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SettingsScreen(
    getAllApps: (context: Context) -> List<AppInfo>,
    sharedPreferences: SharedPreferences,
    getAppIconsFromKeys: (context: Context) -> List<ImageBitmap>
) {
    val context = LocalContext.current
    var isFloatingButtonOn by remember { mutableStateOf(false) }
    val buttonWidth = 200.dp
    val paddings = 20.dp
    val spacingSize = 50.dp


    var appIcons by remember {
        mutableStateOf(
            getAppIconsFromKeys(context)
        )
    }
    val updateAppIcons = {
        appIcons = getAppIconsFromKeys(context)
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(paddings),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Switch(checked = isFloatingButtonOn, onCheckedChange = { isChecked ->
            isFloatingButtonOn = isChecked
            if (isChecked) {
                context.startService(Intent(context, FloatingButtonService::class.java))
            } else {
                context.stopService(Intent(context, FloatingButtonService::class.java))
            }
        })
        Text(text = if (isFloatingButtonOn) "Floating Button is ON" else "Floating Button is OFF")

        Spacer(modifier = Modifier.size(spacingSize))

        ElevatedButton(
            onClick = {}, modifier = Modifier.width(buttonWidth)
        ) {
            Text("Добавить кнопку")
        }

        ElevatedButton(
            onClick = {}, modifier = Modifier.width(buttonWidth)
        ) {
            Text("__ANY__")
        }

        ElevatedButton(
            onClick = {}, modifier = Modifier.width(buttonWidth)
        ) {
            Text("__ANY__")
        }

        Spacer(modifier = Modifier.size(spacingSize))

        SelectedAppLine(
            appIcons = appIcons,
            apps = getAllApps,
            sharedPreferences = sharedPreferences,
            updateAppIcons = updateAppIcons
        )

        Spacer(modifier = Modifier.height(spacingSize))

//        SystemAppList(getAllApps, sharedPreferences, updateAppIcons)
    }
}

