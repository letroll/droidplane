package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer. pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Image
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.res.stringResource
import fr.julien.quievreux.droidplane2.ui.media.DEFAULT_IMAGE_BASE_HEIGHT_DP
import fr.julien.quievreux.droidplane2.ui.media.DEFAULT_IMAGE_BASE_WIDTH_DP
import fr.julien.quievreux.droidplane2.ui.media.DEFAULT_IMAGE_SCALE
import fr.julien.quievreux.droidplane2.ui.media.ImageResizeCorner
import fr.julien.quievreux.droidplane2.ui.media.MAX_IMAGE_SCALE
import fr.julien.quievreux.droidplane2.ui.media.MIN_IMAGE_SCALE
import fr.julien.quievreux.droidplane2.ui.media.computeResizedImageScale
import fr.julien.quievreux.droidplane2.ui.media.rememberImageFilePicker
import fr.julien.quievreux.droidplane2.ui.media.rememberNodeImageBitmap
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

internal const val TEST_TAG_NODE_IMAGE_ATTACHMENT = "node_image_attachment"
internal const val TEST_TAG_NODE_IMAGE_RESIZE_ANCHOR_PREFIX = "node_image_resize_anchor_"

/**
 * Renders a node's attached external image with 4 interactive corner resize anchors
 * and a long-press context menu for replacing or deleting the image.
 */
@Composable
fun NodeImageAttachmentView(
    externalObject: ExternalObjectProperties,
    onScaleChange: (Float) -> Unit,
    onReplaceImage: (String) -> Unit,
    onDeleteImage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val persistedScale = (externalObject.size ?: DEFAULT_IMAGE_SCALE)
        .coerceIn(MIN_IMAGE_SCALE, MAX_IMAGE_SCALE)
    var liveScale by remember(externalObject.uri, persistedScale) {
        mutableFloatStateOf(persistedScale)
    }
    var showContextMenu by remember { mutableStateOf(false) }

    val imageBitmap = rememberNodeImageBitmap(externalObject.uri)
    val launchReplacePicker = rememberImageFilePicker { selectedUri ->
        if (selectedUri.isNotBlank()) {
            onReplaceImage(selectedUri)
        }
    }

    val displayWidthDp = (DEFAULT_IMAGE_BASE_WIDTH_DP * liveScale).dp
    val displayHeightDp = (DEFAULT_IMAGE_BASE_HEIGHT_DP * liveScale).dp

    Box(
        modifier = modifier
            .size(width = displayWidthDp, height = displayHeightDp)
            .testTag(TEST_TAG_NODE_IMAGE_ATTACHMENT)
            .pointerInput(externalObject.uri) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = true)
                    val longPress = awaitLongPressOrCancellation(down.id)
                    if (longPress != null) {
                        longPress.consume()
                        showContextMenu = true
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == longPress.id } ?: break
                            change.consume()
                            if (!change.pressed) break
                        }
                    }
                }
            }
    ) {
        if (imageBitmap != null) {
            Image(
                bitmap = imageBitmap,
                contentDescription = externalObject.uri,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            BrokenNodeImagePlaceholder(
                modifier = Modifier
                    .matchParentSize()
                    .padding(4.dp)
            )
        }

        CornerResizeAnchor(
            corner = ImageResizeCorner.TOP_LEFT,
            currentScale = liveScale,
            onLiveScaleChange = { liveScale = it },
            onCommitScale = onScaleChange,
            modifier = Modifier.align(Alignment.TopStart)
        )
        CornerResizeAnchor(
            corner = ImageResizeCorner.TOP_RIGHT,
            currentScale = liveScale,
            onLiveScaleChange = { liveScale = it },
            onCommitScale = onScaleChange,
            modifier = Modifier.align(Alignment.TopEnd)
        )
        CornerResizeAnchor(
            corner = ImageResizeCorner.BOTTOM_LEFT,
            currentScale = liveScale,
            onLiveScaleChange = { liveScale = it },
            onCommitScale = onScaleChange,
            modifier = Modifier.align(Alignment.BottomStart)
        )
        CornerResizeAnchor(
            corner = ImageResizeCorner.BOTTOM_RIGHT,
            currentScale = liveScale,
            onLiveScaleChange = { liveScale = it },
            onCommitScale = onScaleChange,
            modifier = Modifier.align(Alignment.BottomEnd)
        )

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.replace_image)) },
                onClick = {
                    showContextMenu = false
                    launchReplacePicker()
                }
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete_image)) },
                onClick = {
                    showContextMenu = false
                    onDeleteImage()
                }
            )
        }
    }
}

@Composable
fun CornerResizeAnchor(
    corner: ImageResizeCorner,
    currentScale: Float,
    onLiveScaleChange: (Float) -> Unit,
    onCommitScale: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val latestScale by rememberUpdatedState(currentScale)
    val latestOnLiveScaleChange by rememberUpdatedState(onLiveScaleChange)
    val latestOnCommitScale by rememberUpdatedState(onCommitScale)

    Box(
        modifier = modifier
            .size(20.dp)
            .testTag("$TEST_TAG_NODE_IMAGE_RESIZE_ANCHOR_PREFIX${corner.name}")
            .pointerInput(corner) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    down.consume()
                    val startScale = latestScale
                    var totalDxPx = 0f
                    var totalDyPx = 0f
                    var lastComputedScale = startScale

                    while (true) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) {
                            change.consume()
                            break
                        }
                        val delta = change.positionChange()
                        if (delta != Offset.Zero) {
                            change.consume()
                            totalDxPx += delta.x
                            totalDyPx += delta.y
                            val dxDp = totalDxPx / density.density
                            val dyDp = totalDyPx / density.density
                            lastComputedScale = computeResizedImageScale(
                                initialScale = startScale,
                                corner = corner,
                                dragDeltaXDp = dxDp,
                                dragDeltaYDp = dyDp
                            )
                            latestOnLiveScaleChange(lastComputedScale)
                        }
                    }
                    latestOnCommitScale(lastComputedScale)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary)
                .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.onPrimary), CircleShape)
        )
    }
}

@Composable
fun BrokenNodeImagePlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .border(
                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(6.dp)
            )
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = FontAwesomeIcons.Solid.Image,
                contentDescription = stringResource(R.string.broken_image_placeholder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = stringResource(R.string.broken_image_placeholder),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview
@Composable
fun PreviewNodeImageAttachmentView() {
    ContrastAwareReplyTheme {
        Surface {
            NodeImageAttachmentView(
                externalObject = ExternalObjectProperties(
                    uri = "file:/preview/sample.png",
                    size = 1.0f
                ),
                onScaleChange = {},
                onReplaceImage = {},
                onDeleteImage = {},
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Preview
@Composable
fun PreviewCornerResizeAnchor() {
    ContrastAwareReplyTheme {
        Surface {
            CornerResizeAnchor(
                corner = ImageResizeCorner.BOTTOM_RIGHT,
                currentScale = 1.0f,
                onLiveScaleChange = {},
                onCommitScale = {}
            )
        }
    }
}

@Preview
@Composable
fun PreviewBrokenNodeImagePlaceholder() {
    ContrastAwareReplyTheme {
        Surface {
            BrokenNodeImagePlaceholder(
                modifier = Modifier.size(width = 120.dp, height = 90.dp)
            )
        }
    }
}
