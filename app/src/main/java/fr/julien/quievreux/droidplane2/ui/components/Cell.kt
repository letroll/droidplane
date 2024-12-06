package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Composable for a cell
@Composable
fun Cell(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(100.dp)
            .background(Color.LightGray, shape = RoundedCornerShape(8.dp))
            .border(2.dp, Color.Black, shape = RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, color = Color.Black, fontSize = 16.sp)
    }
}


@Preview(showBackground = true, widthDp = 200, heightDp = 200)
@Composable
fun PreviewCell() {
    Cell(text = "Sample Cell")
}
