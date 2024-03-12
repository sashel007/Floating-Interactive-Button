package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.app.ActivityManager
import android.bluetooth.BluetoothAdapter
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
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import ru.ikar.floatingbutton_ikar.service.FloatingButtonService
import ru.ikar.floatingbutton_ikar.sharedpreferences.SharedPrefHandler
import ru.ikar.floatingbutton_ikar.sharedpreferences.SharedPreferencesLogger

class SettingsActivity : ComponentActivity() {
    private val overlayPermissionReqCode = 1001  // ваш код запроса для этого разрешения
    private lateinit var selectedLineSharedPrefObj: SharedPrefHandler
    private lateinit var selectedLineSharedPref: SharedPreferences
    private lateinit var buttonManagerSharedPrefObj: SharedPrefHandler
    private lateinit var buttonManagerSharedPref: SharedPreferences
    private val accessibilityServiceIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    private lateinit var accessibilityPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var bluetoothEnableLauncher: ActivityResultLauncher<Intent>
    private val requestScreenCapture = 1002
    private lateinit var sharedPreferenceLogger: SharedPreferencesLogger

    companion object {
        const val selectedLineSharedPrefName = "app_package_names"
        const val buttonManagerSharedPrefName = "button_manager_names"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        selectedLineSharedPrefObj = SharedPrefHandler(this, selectedLineSharedPrefName)
        buttonManagerSharedPrefObj = SharedPrefHandler(this, buttonManagerSharedPrefName)
        selectedLineSharedPref = selectedLineSharedPrefObj.sharedPref
        buttonManagerSharedPref = buttonManagerSharedPrefObj.sharedPref

        logSharedPreferences(buttonManagerSharedPref)

        // Инициализация лаунчера для запроса разрешения на использование службы доступности
        accessibilityPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Пользователь предоставил разрешение
                Toast.makeText(this, "Разрешение на доступность предоставлено", Toast.LENGTH_SHORT)
                    .show()
            } else {
                // Пользователь не предоставил разрешение
                Toast.makeText(
                    this,
                    "Разрешение на доступность не предоставлено",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        // Проверка, предоставлено ли разрешение на использование службы доступности
        if (!isAccessibilityServiceEnabled()) {
            // Если разрешение не предоставлено, предложите пользователю предоставить его
            requestAccessibilityPermission()
        }

        // Разрешение на захват экрана
//        val mediaProjectionManager =
//            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
//        val captureIntent = mediaProjectionManager.createScreenCaptureIntent()
//        startActivityForResult(captureIntent, requestScreenCapture)

        bluetoothEnableLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                // Bluetooth был включен пользователем
            } else {
                // Пользователь отказался включать Bluetooth
            }
        }

        checkAndRequestBluetoothEnable()

        setContent {
//            MyScreen()
            // Отображаем экран настроек
            SettingsScreen(
                getAllApps = { context -> getAllApps(context) },
                selectedLineSharedPref = selectedLineSharedPref,
                buttonManagerSharedPref = buttonManagerSharedPref,
                buttonManagerSharedPrefHandler = buttonManagerSharedPrefObj,
                getAppIconsFromKeys = { context ->
                    getAppIconsFromKeys(
                        context,
                        selectedLineSharedPrefObj.keys
                    )
                }
            )
        }

        sharedPreferenceLogger = SharedPreferencesLogger(this, buttonManagerSharedPrefName)

        // Проверка разрешения на отображение поверх других приложений.
        if (!Settings.canDrawOverlays(this)) {
            // Если у нас нет разрешения и версия ОС >= Marshmallow, то создаем намерение для запроса разрешения.
            // Если у нас нет разрешения и версия ОС >= Marshmallow, то создаем намерение для запроса разрешения.
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")
            )
            // Запускаем активность для результата (для получения ответа о предоставлении разрешения).
            startActivityForResult(intent, overlayPermissionReqCode)
        }
    }

    // Этот метод будет вызван после того, как пользователь предоставит или отклонит разрешение
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == overlayPermissionReqCode) {
            if (Settings.canDrawOverlays(this)) {
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
        if (requestCode == requestScreenCapture && resultCode == RESULT_OK && data != null) {
            Log.d("_SettingsActivity_", "Screen capture resultCode: $resultCode")
            // Разрешение на захват экрана получено, можно передать данные в сервис
            val serviceIntent = Intent(this, FloatingButtonService::class.java).apply {
                putExtra(FloatingButtonService.EXTRA_RESULT_CODE, resultCode)
                putExtra(FloatingButtonService.EXTRA_RESULT_INTENT, data)
            }
            startService(serviceIntent)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val accessibilityServices =
            Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
        return accessibilityServices?.contains(packageName) == true
    }

    private fun requestAccessibilityPermission() {
        accessibilityPermissionLauncher.launch(accessibilityServiceIntent)
    }

    private fun startFloatingButtonService() {
        val intent = Intent(this, FloatingButtonService::class.java)
        startService(intent)
    }

    private fun stopService() {
        stopService(Intent(this, FloatingButtonService::class.java))
    }

    private fun getAllApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        // Создаем намерение для получения всех приложений, которые могут появляться в лаунчере
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        // Получаем список всех активностей, которые можно запустить с главного экрана
        val resolveInfoList = pm.queryIntentActivities(intent, 0)
        // Фильтруем полученные данные и преобразуем в список AppInfo
        return resolveInfoList.map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo.packageName
            val iconDrawable = activityInfo.loadIcon(pm)
            AppInfo(packageName, iconDrawable.toImageBitmap())
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
            val packageName = selectedLineSharedPrefObj.getSharedPrefValue(key, null)
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

    private fun checkAndRequestBluetoothEnable() {
        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            bluetoothEnableLauncher.launch(enableBtIntent)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        sharedPreferenceLogger.unregisterListener()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun SettingsScreen(
    getAllApps: (context: Context) -> List<AppInfo>,
    selectedLineSharedPref: SharedPreferences,
    buttonManagerSharedPref: SharedPreferences,
    buttonManagerSharedPrefHandler: SharedPrefHandler,
    getAppIconsFromKeys: (context: Context) -> List<ImageBitmap>
) {
    val context = LocalContext.current
    var isFloatingButtonOn by remember { mutableStateOf(false) }
    val paddings = 8.dp
    val spacingSize = 50.dp

    LaunchedEffect(key1 = context) {
        isFloatingButtonOn = isMyServiceRunning(context, FloatingButtonService::class.java)
    }

    var appIcons by remember {
        mutableStateOf(
            getAppIconsFromKeys(context)
        )
    }
    val updateAppIcons = {
        appIcons = getAppIconsFromKeys(context)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            modifier = Modifier.padding(paddings),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ButtonsManagerLine(buttonManagerSharedPref, buttonManagerSharedPrefHandler)

            Spacer(modifier = Modifier.size(spacingSize))

            /** Здесь назначаются дополнительные кнопки в ячейках */

            SelectedAppLine(
                apps = getAllApps,
                sharedPreferences = selectedLineSharedPref,
                updateAppIcons = updateAppIcons
            )

//            /** Дополнительные ячейки для дополнительных кнопок */
//
//            AddSelectedAppLine(apps = getAllApps) {
//
//            }


            Spacer(modifier = Modifier.height(spacingSize))

            /** Активация сервиса */

            Switch(checked = isFloatingButtonOn, onCheckedChange = { isChecked ->
                isFloatingButtonOn = isChecked
                if (isChecked) {
                    context.startService(Intent(context, FloatingButtonService::class.java))
                } else {
                    context.stopService(Intent(context, FloatingButtonService::class.java))
                }
            })
            Text(text = if (isFloatingButtonOn) "Кнопка включена" else "Кнопка выключена")

            AccessabilityButton()
        }
    }

}

fun isMyServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}

fun logSharedPreferences(sharedPreferences: SharedPreferences) {
    val allEntries = sharedPreferences.all
    for ((key, value) in allEntries) {
        Log.d("SharedPreferences", "$key: $value")
    }
}
