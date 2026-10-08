package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.CloudProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

private val PALETTE_COLORS = listOf(
    "#000000" to Color.Black,
    "#003366" to Color(0xFF003366),
    "#D32F2F" to Color(0xFFD32F2F),
    "#E65100" to Color(0xFFE65100),
    "#1976D2" to Color(0xFF1976D2),
    "#388E3C" to Color(0xFF388E3C),
    "#7B1FA2" to Color(0xFF7B1FA2),
    "#FFF9C4" to Color(0xFFFFF9C4),
    "#E1F5FE" to Color(0xFFE1F5FE),
    "#F1F8E9" to Color(0xFFF1F8E9),
    "#FFFFFF" to Color.White,
)

@Composable
fun StylingTab(
    node: Node,
    onNodeChanged: (Node) -> Unit,
    modifier: Modifier = Modifier,
    onMoveCloudToParent: ((Node) -> Unit)? = null,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        // Node Shape Selector
        Text(
            text = stringResource(R.string.node_shape),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val shapes = listOf(
                "fork" to stringResource(R.string.shape_fork),
                "bubble" to stringResource(R.string.shape_bubble),
                "oval" to stringResource(R.string.shape_oval),
                "rectangle" to stringResource(R.string.shape_rectangle),
            )
            shapes.forEach { (shapeKey, shapeLabel) ->
                FilterChip(
                    selected = node.style == shapeKey,
                    onClick = {
                        val newStyle = if (node.style == shapeKey) null else shapeKey
                        onNodeChanged(node.copy(style = newStyle))
                    },
                    label = { Text(shapeLabel) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Colors
        Text(
            text = stringResource(R.string.text_color),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        ColorPalettePicker(
            currentColor = node.color,
            onColorSelected = { onNodeChanged(node.copy(color = it)) }
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.background_color),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        ColorPalettePicker(
            currentColor = node.backgroundColor,
            onColorSelected = { onNodeChanged(node.copy(backgroundColor = it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Font Styling (Bold, Italic, Size)
        Text(
            text = "Font",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = node.isBold,
                onClick = { onNodeChanged(node.copy(isBold = !node.isBold)) },
                label = { Text(stringResource(R.string.bold), fontWeight = FontWeight.Bold) }
            )
            FilterChip(
                selected = node.isItalic,
                onClick = { onNodeChanged(node.copy(isItalic = !node.isItalic)) },
                label = { Text(stringResource(R.string.italic)) }
            )

            OutlinedTextField(
                value = node.fontSize?.toString().orEmpty(),
                onValueChange = { sizeStr ->
                    onNodeChanged(node.copy(fontSize = sizeStr.toIntOrNull()))
                },
                label = { Text(stringResource(R.string.font_size)) },
                placeholder = { Text("12") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Cloud Grouping Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.cloud_grouping),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.cloud_enabled),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = node.cloud != null,
                onCheckedChange = { isChecked ->
                    val newCloud = if (isChecked) {
                        CloudProperties(color = "#FFF3E0", shape = "ROUND_RECT")
                    } else {
                        null
                    }
                    onNodeChanged(node.copy(cloud = newCloud))
                }
            )
        }

        if (node.cloud != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.cloud_color),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            ColorPalettePicker(
                currentColor = node.cloud?.color,
                onColorSelected = { newColor ->
                    onNodeChanged(node.copy(cloud = node.cloud?.copy(color = newColor)))
                }
            )

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.cloud_shape),
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val cloudShapes = listOf("ROUND_RECT", "ARC", "STAR", "RECT")
                cloudShapes.forEach { cShape ->
                    FilterChip(
                        selected = node.cloud?.shape == cShape,
                        onClick = {
                            onNodeChanged(node.copy(cloud = node.cloud?.copy(shape = cShape)))
                        },
                        label = { Text(cShape) }
                    )
                }
            }

            val parent = node.parentNode
            if (parent != null) {
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedButton(
                    onClick = { onMoveCloudToParent?.invoke(node) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.move_cloud_to_parent))
                }
            }
        } else {
            val parent = node.parentNode
            if (parent?.cloud != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.enclosed_in_parent_cloud, parent.text ?: parent.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ColorPalettePicker(
    currentColor: String?,
    onColorSelected: (String?) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PALETTE_COLORS.forEach { (hex, color) ->
            val isSelected = currentColor.equals(hex, ignoreCase = true)
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        shape = CircleShape
                    )
                    .clickable { onColorSelected(if (isSelected) null else hex) }
            )
        }
    }
}

@Preview(name = "StylingTab Preview")
@Composable
fun PreviewStylingTab() {
    ContrastAwareReplyTheme {
        Surface {
            StylingTab(
                node = Node(
                    parentNode = null,
                    id = "ID_123",
                    numericId = 123,
                    text = "Sample Node Title",
                    color = "#003366",
                    backgroundColor = "#FFF9C4",
                    style = "bubble",
                    isBold = true,
                    cloud = CloudProperties(color = "#FFF3E0", shape = "ROUND_RECT"),
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                onNodeChanged = {}
            )
        }
    }
}
