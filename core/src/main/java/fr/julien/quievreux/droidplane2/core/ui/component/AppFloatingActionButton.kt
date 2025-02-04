package fr.julien.quievreux.droidplane2.core.ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    iconContentDsc: String,
    icon: ImageVector,
) {
    FloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.secondary
    ) {
        Icon(icon, iconContentDsc)
    }
}

@Preview
@Composable
private fun AppFloatingActionButtonPreview() {
    AppFloatingActionButton(
        onClick = {},
        iconContentDsc = "Add",
        icon = Icons.Filled.Add
    )
}

@Composable
fun AppFloatingActionTextButton(
    onClick: () -> Unit,
    text: String,
    icon: ImageVector,
    iconContentDsc: String,
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.secondary,
        icon = { Icon(icon, iconContentDsc) },
        text = { Text(text) }
    )
}

@Preview
@Composable
private fun AppFloatingActionTextButtonPreview() {
    AppFloatingActionTextButton(
        onClick = {},
        iconContentDsc = "Add",
        text = "Add",
        icon = Icons.Filled.Add
    )
}
