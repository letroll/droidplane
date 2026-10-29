package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.julien.quievreux.droidplane2.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

/**
 * Inspector tab for managing assigned Freeplane node icons (visual list, reordering, removal)
 * and picking new icons from the searchable, categorized icon catalog.
 * Also manages external object media (image URI and scale).
 */
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
        // Active Icons Management (US2)
        ActiveIconsManager(
            iconNames = node.iconNames,
            onRemoveIcon = { index ->
                val updated = node.iconNames.toMutableList()
                if (index in updated.indices) {
                    updated.removeAt(index)
                    onNodeChanged(node.copy(iconNames = updated))
                }
            },
            onMoveIcon = { fromIndex, toIndex ->
                val updated = node.iconNames.toMutableList()
                if (fromIndex in updated.indices && toIndex in updated.indices) {
                    val item = updated.removeAt(fromIndex)
                    updated.add(toIndex, item)
                    onNodeChanged(node.copy(iconNames = updated))
                }
            },
            onClearAll = {
                onNodeChanged(node.copy(iconNames = mutableListOf()))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Full Freeplane Icon Catalog Browser (US3)
        IconCatalogPicker(
            onIconSelected = { iconId ->
                val updated = node.iconNames.toMutableList()
                updated.add(iconId)
                onNodeChanged(node.copy(iconNames = updated))
            }
        )

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

@Preview
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
                    iconNames = mutableListOf("button_ok", "flag-red", "full-1", "unmapped_icon"),
                    externalObject = ExternalObjectProperties(uri = "file:/image.png", size = 1.0f),
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                onNodeChanged = {}
            )
        }
    }
}
