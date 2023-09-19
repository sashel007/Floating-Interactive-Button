package ru.ikar.floatingbutton_ikar.settings_actitivy

import ButtonResources
import android.content.Intent
import android.media.Image
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.ikar.floatingbutton_ikar.FloatingButtonService
import ru.ikar.floatingbutton_ikar.R

class SettingsActivity : ComponentActivity() {

    val buttonResources = listOf(
        ButtonResources.backgroundButtonResource,
        ButtonResources.anyButtonResource,
        ButtonResources.brightnessButtonResource,
        ButtonResources.settingsButtonResource,
        ButtonResources.homeButtonResource,
        ButtonResources.volumeButtonResource
    )

    private val OVERLAY_PERMISSION_REQ_CODE = 1001  // ваш код запроса для этого разрешения

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen(buttonResources)
        }

        // Проверка и запрос разрешения
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + packageName))
            startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE)
        } else {
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
                Toast.makeText(this, "Нам нужно это разрешение для работы плавающей кнопки.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startFloatingButtonService() {
        val selectedButtons: List<Int> = buttonResources
        val intent = Intent(this, FloatingButtonService::class.java)
        intent.putIntegerArrayListExtra("selectedButtons", ArrayList(selectedButtons))
        startService(intent)
    }
}


@Composable
fun SettingsScreen(buttonResources: List<Int>) {
    val context = LocalContext.current
    var isFloatingButtonOn by remember { mutableStateOf(false) }
    val buttonWidth = 200.dp
    val paddings = 20.dp
    val spacingSize = 50.dp

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
            onClick = {

            },
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

        SelectedAppLine(buttonResources)

        Spacer(modifier = Modifier.size(spacingSize))

        SystemAppList()

    }
}

