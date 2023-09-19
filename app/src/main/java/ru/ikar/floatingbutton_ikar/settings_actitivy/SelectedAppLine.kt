package ru.ikar.floatingbutton_ikar.settings_actitivy

import android.view.View
import android.widget.ImageButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.modifier.modifierLocalConsumer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.ikar.floatingbutton_ikar.FloatingButtonService

@Composable
fun SelectedAppLine(buttonResources: List<Int>) {

    val heightSize = 60.dp
    val spacerSize = 15.dp

    Text(
        text = "Выбранные элементы плавающей кнпоки: ",
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.size(spacerSize))
    Box(
        modifier = Modifier
            .height(heightSize)
            .fillMaxWidth()
            .background(Color.Gray)
    ) {
        LazyRow(modifier = Modifier.background(Color.Gray)){
            items(buttonResources.size) { index ->
                val resource = buttonResources[index]
                Image(
                    painter = painterResource(id = resource),
                    contentDescription = null,
                    modifier =  Modifier.size(24.dp)
                )
            }
        }
    }
}

//private fun getSelectedButtons(buttons: List<ImageButton>): List<ImageButton> {
//
//    return List<ImageButton>
//}