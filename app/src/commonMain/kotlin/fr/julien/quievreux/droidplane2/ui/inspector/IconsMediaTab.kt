package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.FolderOpen
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.res.stringResource
import fr.julien.quievreux.droidplane2.ui.components.NodeIconsRow
import fr.julien.quievreux.droidplane2.ui.media.DEFAULT_IMAGE_SCALE
import fr.julien.quievreux.droidplane2.ui.media.rememberImageFilePicker
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

internal const val TEST_TAG_MEDIA_ICONS_SECTION = "media_icons_section"
internal const val TEST_TAG_MEDIA_IMAGES_SECTION = "media_images_section"
internal const val TEST_TAG_BROWSE_IMAGE_BUTTON = "browse_image_button"

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
    var isIconsSectionExpanded by remember { mutableStateOf(true) }
    var isImagesSectionExpanded by remember { mutableStateOf(true) }

    val launchImageFilePicker = rememberImageFilePicker { selectedUri ->
        if (selectedUri.isNotBlank()) {
            val currentSize = node.externalObject?.size ?: DEFAULT_IMAGE_SCALE
            onNodeChanged(
                node.copy(
                    externalObject = ExternalObjectProperties(
                        uri = selectedUri,
                        size = currentSize
                    )
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FoldableMediaSection(
            title = stringResource(R.string.icons_section),
            isExpanded = isIconsSectionExpanded,
            onToggleExpand = { isIconsSectionExpanded = !isIconsSectionExpanded },
            modifier = Modifier.testTag(TEST_TAG_MEDIA_ICONS_SECTION),
            headerInlineContent = {
                if (node.iconNames.isNotEmpty()) {
                    NodeIconsRow(
                        iconNames = node.iconNames,
                        maxInlineIcons = Int.MAX_VALUE,
                        iconSize = 18.dp
                    )
                }
            }
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
        }

        // External Object / Media Section
        FoldableMediaSection(
            title = stringResource(R.string.images_section),
            isExpanded = isImagesSectionExpanded,
            onToggleExpand = { isImagesSectionExpanded = !isImagesSectionExpanded },
            modifier = Modifier.testTag(TEST_TAG_MEDIA_IMAGES_SECTION)
        ) {
            Text(
                text = stringResource(R.string.external_object),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    modifier = Modifier.weight(1f),
                    label = { Text(stringResource(R.string.image_uri)) },
                    placeholder = { Text("file:/path/to/image.png or https://...") },
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilledTonalIconButton(
                    onClick = launchImageFilePicker,
                    modifier = Modifier.testTag(TEST_TAG_BROWSE_IMAGE_BUTTON)
                ) {
                    Icon(
                        imageVector = FontAwesomeIcons.Solid.FolderOpen,
                        contentDescription = stringResource(R.string.browse_image)
                    )
                }
            }

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
}

@Composable
fun FoldableMediaSection(
    title: String,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier,
    headerInlineContent: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    headerInlineContent()
                }
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = title
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, bottom = 14.dp)
                ) {
                    content()
                }
            }
        }
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

@Preview
@Composable
fun PreviewFoldableMediaSection() {
    ContrastAwareReplyTheme {
        Surface {
            FoldableMediaSection(
                title = "Icons",
                isExpanded = false,
                onToggleExpand = {},
                modifier = Modifier.padding(16.dp),
                headerInlineContent = {
                    NodeIconsRow(
                        iconNames = listOf("button_ok", "flag-red"),
                        maxInlineIcons = Int.MAX_VALUE
                    )
                }
            ) {
                Text("Section Body")
            }
        }
    }
}
