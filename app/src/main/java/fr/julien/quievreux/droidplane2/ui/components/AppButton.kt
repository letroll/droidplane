package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@Composable
fun AppButton(
    text: String,
    onClick: () -> Unit={},
    modifier: Modifier = Modifier,
) {
    Button(
        modifier = modifier,
        onClick = onClick
    ) {
        Text(
            text = text
        )
    }
//        Icon(
//            imageVector =
//            if (isOpen) {
//                Icons.Filled.KeyboardArrowUp
//            }else {
//                Icons.Filled.KeyboardArrowDown
//            },
//            tint = MaterialTheme.colorScheme.primary,
//            contentDescription = null,
//        )
}

@Preview
@Composable
private fun AppButtonPreview() {
    ContrastAwareReplyTheme {
        Surface(
            modifier = Modifier
                .size(width = 200.dp, height = 100.dp)
        ) {
            AppButton(
                text = "test",
                modifier = Modifier
                    .padding(20.dp)
            )
        }
    }
}
