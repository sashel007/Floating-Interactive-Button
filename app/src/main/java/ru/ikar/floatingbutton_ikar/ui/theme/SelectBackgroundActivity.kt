package ru.ikar.floatingbutton_ikar.ui.theme

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import ru.ikar.floatingbutton_ikar.R

class SelectBackgroundActivity : ComponentActivity() {

    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SelectBackgroundScreen()
        }
    }

    @Composable
    fun SelectBackgroundScreen() {
        // Sample list of background images (you can replace it with your actual images)
        val backgroundImages = listOf(
            R.drawable.back_2,
            R.drawable.back_3,
            R.drawable.back_4,
            // Add more image resources as needed
        )

        var selectedImageResId by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(GridCells.Fixed(3)) {
                itemsIndexed(backgroundImages) { _, imageResId ->
                    val isSelected = selectedImageResId == imageResId

                    Image(
                        painter = painterResource(id = imageResId),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .padding(8.dp)
                            .clickable {
                                // Set the selected image resource ID
                                selectedImageResId = imageResId
                            }
                            .border(
                                width = 2.dp,
                                color = if (isSelected) Color.Blue else Color.Transparent)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    // Show the confirmation dialog here, you can use AlertDialog or a custom DialogFragment
                    // and handle the button clicks accordingly to set the selected image as the background
                    // or do nothing.
                    // For simplicity, let's just print the selected image resource ID here.
                    if (selectedImageResId != null) {
                        val resultIntent = Intent()
                        val selectedImageUri = Uri.parse("android.resource://$packageName/${selectedImageResId!!}")
                        resultIntent.data = selectedImageUri

                        // Set the result and finish the activity
                        setResult(Activity.RESULT_OK, resultIntent)
                        finish()
                    } else {
                        println("No image selected.")
                    }
                }
            ) {
                Text(text = "Установить фон")
            }
        }
    }
}
