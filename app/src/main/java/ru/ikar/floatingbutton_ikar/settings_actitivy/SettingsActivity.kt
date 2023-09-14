package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.ikar.floatingbutton_ikar.FloatingButtonService

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SettingsScreen()
        }
    }
}

@Composable
fun SettingsScreen() {
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

        SelectedAppLine()

        Spacer(modifier = Modifier.size(spacingSize))

        SystemAppList()

    }
}

