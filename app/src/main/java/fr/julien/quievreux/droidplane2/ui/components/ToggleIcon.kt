package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun ToggleIcon(
    isOpen: Boolean = false,
    size: Dp = 24.dp,
    modifier: Modifier = Modifier,
) {
        Icon(
            imageVector =
            if (isOpen) {
                Icons.Filled.KeyboardArrowUp
            }else {
                Icons.Filled.KeyboardArrowDown
            },
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = null,
            modifier = modifier.size(size)
        )
}

@Preview
@Composable
private fun ToggleIconPreview() {
    ToggleIcon(
        isOpen = false,
        modifier = Modifier
    )
}
