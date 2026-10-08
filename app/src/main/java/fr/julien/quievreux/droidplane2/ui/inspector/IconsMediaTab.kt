package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

private val STANDARD_FREEPLANE_ICONS = listOf(
    "yes", "button_ok", "button_cancel", "stop", "info", "help",
    "messagebox_warning", "idea", "knotify", "pencil", "bookmark",
    "flag-red", "flag-green", "flag-blue", "flag-orange", "flag-black",
    "priority-1", "priority-2", "priority-3", "priority-4", "priority-5",
    "attach", "mail", "calendar", "clock", "folder"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconsMediaTab(
    node: Node,
    onNodeChanged: (Node) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        // Active Icons
        Text(
            text = stringResource(R.string.icons),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))

        if (node.iconNames.isEmpty()) {
            Text(
                text = "No icons assigned to this node.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                node.iconNames.forEachIndexed { index, iconName ->
                    InputChip(
                        selected = true,
                        onClick = {
                            val updated = node.iconNames.toMutableList()
                            updated.removeAt(index)
                            onNodeChanged(node.copy(iconNames = updated))
                        },
                        label = { Text(iconName) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Remove $iconName",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Catalog of Freeplane Icons
        Text(
            text = "Add from Icon Catalog",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            STANDARD_FREEPLANE_ICONS.forEach { iconName ->
                AssistChip(
                    onClick = {
                        val updated = node.iconNames.toMutableList()
                        updated.add(iconName)
                        onNodeChanged(node.copy(iconNames = updated))
                    },
                    label = { Text(iconName) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // External Object / Media Section
        Text(
            text = stringResource(R.string.external_object),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
            value = node.externalObject?.uri.orEmpty(),
            onValueChange = { newUri ->
                val updated = if (newUri.isBlank()) {
                    null
                } else {
                    node.externalObject?.copy(uri = newUri) ?: ExternalObjectProperties(uri = newUri)
                }
                onNodeChanged(node.copy(externalObject = updated))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.image_uri)) },
            placeholder = { Text("file:/path/to/image.png or https://...") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = node.externalObject?.size?.toString().orEmpty(),
            onValueChange = { newSizeStr ->
                val sizeVal = newSizeStr.toFloatOrNull()
                val updated = node.externalObject?.copy(size = sizeVal)
                onNodeChanged(node.copy(externalObject = updated))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.image_scale)) },
            placeholder = { Text("1.0") },
            singleLine = true
        )
    }
}

@Preview(name = "IconsMediaTab Preview")
@Composable
fun PreviewIconsMediaTab() {
    ContrastAwareReplyTheme {
        Surface {
            IconsMediaTab(
                node = Node(
                    parentNode = null,
                    id = "ID_123",
                    numericId = 123,
                    text = "Sample Node Title",
                    iconNames = mutableListOf("button_ok", "flag-red"),
                    externalObject = ExternalObjectProperties(uri = "file:/image.png", size = 1.0f),
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                onNodeChanged = {}
            )
        }
    }
}
