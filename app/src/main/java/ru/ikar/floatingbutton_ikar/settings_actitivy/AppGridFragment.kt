package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap

@Composable
fun SystemAppList(
    appsState: MutableState<List<ImageResource>>,
    onAppSelected: (ImageResource) -> Unit
) {
    val context = LocalContext.current
    val apps = getSystemApps(context)
    val colorList = listOf(Color.Red, Color.Blue, Color.Red)

    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .border(4.dp, Brush.radialGradient(colors = colorList), RectangleShape)
            .fillMaxSize()
    )
    {
        items(apps.size) { index ->
            SystemAppItem(apps[index], onAppSelected)
        }
    }
}

@Composable
fun SystemAppItem(
    app: ApplicationInfo,
    onAppSelected: (ImageResource) -> Unit
) {
    val context = LocalContext.current
    val iconBitmap = app.loadIcon(context.packageManager).toBitmap().asImageBitmap()
    val appName = app.loadLabel(context.packageManager).toString()
    var showDialog by remember { mutableStateOf(false) }
    val squaredSize = 40.dp
    val colorList = listOf(Color.Red, Color.Green, Color.Blue)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(8.dp)
            .width(squaredSize)
            .height(squaredSize)
            .background(color = Color.LightGray)
            .clickable { showDialog = true }
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                bitmap = iconBitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(40.dp)
            )
            Text(appName, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            text = { Text("Добавить приложение к кнопке?") },
            confirmButton = {
                Button(onClick = {
                    // Здесь передаём иконку приложения в SelectedAppLine
                    onAppSelected(ImageResource.AppIcon(iconBitmap))
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

fun getSystemApps(context: Context): List<ApplicationInfo> {
    val intent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolveInfos = context.packageManager.queryIntentActivities(intent, 0)
    return resolveInfos.map { it.activityInfo.applicationInfo }
        .filter { (it.flags and ApplicationInfo.FLAG_SYSTEM) != 0 }
}

