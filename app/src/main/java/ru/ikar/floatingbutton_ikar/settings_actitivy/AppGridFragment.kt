//package ru.ikar.floatingbutton_ikar.settings_actitivy
//
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.lazy.grid.GridCells
//import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
//import androidx.compose.material3.Button
//import androidx.compose.material3.Card
//import androidx.compose.material3.Text
//import androidx.compose.runtime.Composable
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.text.font.FontWeight
//import androidx.compose.ui.text.style.TextAlign
//import androidx.compose.ui.unit.dp
//import androidx.compose.ui.unit.sp
//
//@Composable
//fun AppGridFragment() {
//
//    val list = (1..10).map { it.toString() }
//
//    Text("__ANY__")
//
//    LazyVerticalGrid(
//        columns = GridCells.Adaptive(120.dp),
//        contentPadding = PaddingValues(16.dp)
//        )},
//        content = {
//            items(list.size) { index ->
//                Card(
//                    modifier = Modifier
//                        .padding(4.dp)
//                        .fillMaxWidth(),
//                    elevation = 8.dp,
//                ) {
//                    Text(
//                        text = list[index],
//                        fontWeight = FontWeight.Bold,
//                        fontSize = 30.sp,
//                        color = Color(0xFFFFFFFF),
//                        textAlign = TextAlign.Center,
//                        modifier = Modifier.padding(16.dp)
//                    )
//                }
//            }
//        })
//}