package ru.ikar.floatingbutton_ikar

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

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

    Column {
        Switch(
            checked = isFloatingButtonOn,
            onCheckedChange = { isChecked ->
                isFloatingButtonOn = isChecked
                if (isChecked) {
                    // Start FloatingButtonService
                    context.startService(Intent(context, FloatingButtonService::class.java))
                } else {
                    // Stop FloatingButtonService
                    context.stopService(Intent(context, FloatingButtonService::class.java))
                }
            }
        )
        Text(text = if (isFloatingButtonOn) "Floating Button is ON" else "Floating Button is OFF")

        Button(onClick = {}) {
            Text("Button 1")
        }

        Button(onClick = {}) {
            Text("Button 2")
        }

        Button(onClick = {}) {
            Text("Button 3")
        }
    }
}