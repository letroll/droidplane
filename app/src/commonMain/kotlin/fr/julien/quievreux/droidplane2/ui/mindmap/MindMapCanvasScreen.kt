package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import org.jetbrains.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val AUTO_PAN_EDGE_THRESHOLD = 56.dp
private val AUTO_PAN_STEP_PER_FRAME = 8.dp
private const val AUTO_PAN_FRAME_DELAY_MS = 16L

@Composable
fun MindMapCanvasScreen(
    rootNode: Node,
    selectedNodeId: String?,
    collapsedNodeIds: Set<String>,
    modifier: Modifier = Modifier,
    treeVersion: Long = 0L,
    onNodeSelect: (Node) -> Unit,
    onNodeToggleCollapse: (Node) -> Unit,
    onNodeDoubleClick: (Node) -> Unit = {},
    onNodeLongPress: (Node) -> Unit = {},
    onMoveNode: (
        draggedNodeId: String,
        targetParentId: String,
        targetSiblingId: String?,
        insertAfter: Boolean,
        targetPosition: String?,
    ) -> Unit = { _, _, _, _, _ -> },
    fetchText: (Node) -> String? = { it.text },
) {
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current
    val dragState = rememberMindMapCanvasDragState(
        onNodeLongPress = onNodeLongPress,
        onMoveNode = onMoveNode,
    )

    val layoutResult = remember(rootNode, treeVersion, collapsedNodeIds, density.density) {
        MindMapLayoutEngine.computeLayout(
            rootNode = rootNode,
            collapsedNodeIds = collapsedNodeIds,
            density = density.density,
            fetchText = fetchText,
        )
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clipToBounds()
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        zoom = 1.0f
                        panOffset = Offset.Zero
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    if (dragState.phase == CanvasDragPhase.Idle) {
                        zoom = (zoom * zoomChange).coerceIn(0.25f, 3.0f)
                        panOffset += pan
                    }
                }
            }
    ) {
        val viewportWidthPx = constraints.maxWidth.toFloat()
        val viewportHeightPx = constraints.maxHeight.toFloat()
        val centerX = viewportWidthPx / 2f
        val centerY = viewportHeightPx / 2f
        val edgeThresholdPx = with(density) { AUTO_PAN_EDGE_THRESHOLD.toPx() }
        val stepPx = with(density) { AUTO_PAN_STEP_PER_FRAME.toPx() }

        LaunchedEffect(dragState.phase) {
            while (dragState.phase == CanvasDragPhase.Dragging) {
                val pointerPos = dragState.pointerViewportPx
                if (pointerPos != null) {
                    val panStep = computeEdgeAutoPanStep(
                        pointerViewportPx = pointerPos,
                        viewportWidthPx = viewportWidthPx,
                        viewportHeightPx = viewportHeightPx,
                        edgeThresholdPx = edgeThresholdPx,
                        stepPx = stepPx,
                    )
                    if (panStep != Offset.Zero) {
                        panOffset += panStep
                        dragState.onDragMove(
                            deltaCanvasPx = -panStep / zoom,
                            viewportPointerPx = pointerPos,
                            layoutNodes = layoutResult.nodes,
                        )
                    }
                }
                delay(AUTO_PAN_FRAME_DELAY_MS)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {
            val centeredClouds = remember(layoutResult.nodes, centerX, centerY, density.density) {
                computeCenteredClouds(layoutResult.nodes, centerX, centerY, density.density)
            }

            MindMapCloudLayer(
                clouds = centeredClouds,
                modifier = Modifier.fillMaxSize(),
            )

            val centeredConnectors = remember(layoutResult.connectors, centerX, centerY) {
                layoutResult.connectors.map { c ->
                    c.copy(
                        startX = centerX + c.startX,
                        startY = centerY + c.startY,
                        endX = centerX + c.endX,
                        endY = centerY + c.endY,
                    )
                }
            }

            MindMapBranchLayer(
                connectors = centeredConnectors,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.fillMaxSize(),
            )

            val centeredArrowLinks = remember(layoutResult.nodes, centerX, centerY) {
                computeCenteredArrowLinks(layoutResult.nodes, centerX, centerY)
            }

            MindMapArrowLinkLayer(
                arrowLinks = centeredArrowLinks,
                modifier = Modifier.fillMaxSize(),
            )

            val activeDropTarget = dragState.activeDropTarget
            val activeDraggedId = dragState.activeNodeId

            layoutResult.nodes.forEach { nodeLayout ->
                val isCurrentActiveNode = nodeLayout.node.id == activeDraggedId
                val dragDelta = if (isCurrentActiveNode && !dragState.isActiveNodeRoot) {
                    dragState.dragOffsetPx
                } else {
                    Offset.Zero
                }
                val nodeLeftPx = centerX + nodeLayout.x - (nodeLayout.width / 2f) + dragDelta.x
                val nodeTopPx = centerY + nodeLayout.y - (nodeLayout.height / 2f) + dragDelta.y

                val nodeWidthDp = with(density) { nodeLayout.width.toDp() }
                val nodeHeightDp = with(density) { nodeLayout.height.toDp() }
                val isHoveredTarget = activeDropTarget?.hoveredNodeId == nodeLayout.node.id

                MindMapNodeCard(
                    node = nodeLayout.node,
                    isSelected = nodeLayout.node.id == selectedNodeId,
                    isCollapsed = nodeLayout.isCollapsed,
                    branchDirection = nodeLayout.branchDirection,
                    onNodeClick = { onNodeSelect(nodeLayout.node) },
                    onToggleCollapse = { onNodeToggleCollapse(nodeLayout.node) },
                    onNodeDoubleClick = { onNodeDoubleClick(nodeLayout.node) },
                    onNodeLongPress = { onNodeLongPress(nodeLayout.node) },
                    onLongPressConfirmed = {
                        dragState.onLongPressConfirmed(
                            node = nodeLayout.node,
                            initialCenter = Offset(nodeLayout.x, nodeLayout.y),
                        )
                    },
                    onDragMove = { deltaLocalPx, pointerLocalPx ->
                        val currentDragOffset = dragState.dragOffsetPx + deltaLocalPx
                        val unscaledCardX = centerX + nodeLayout.x - (nodeLayout.width / 2f) + currentDragOffset.x + pointerLocalPx.x
                        val unscaledCardY = centerY + nodeLayout.y - (nodeLayout.height / 2f) + currentDragOffset.y + pointerLocalPx.y
                        val viewportPos = Offset(
                            x = centerX + (unscaledCardX - centerX) * zoom + panOffset.x,
                            y = centerY + (unscaledCardY - centerY) * zoom + panOffset.y,
                        )
                        dragState.onDragMove(
                            deltaCanvasPx = deltaLocalPx,
                            viewportPointerPx = viewportPos,
                            layoutNodes = layoutResult.nodes,
                        )
                    },
                    onGestureRelease = { dragState.onRelease() },
                    onGestureCancel = { dragState.onCancel() },
                    isDraggingOrReady = isCurrentActiveNode && dragState.phase != CanvasDragPhase.Idle,
                    activeDropZone = if (isHoveredTarget) activeDropTarget?.zoneType else null,
                    dropTargetPosition = if (isHoveredTarget) activeDropTarget?.targetPosition else null,
                    fetchText = fetchText,
                    modifier = Modifier
                        .zIndex(if (isCurrentActiveNode && dragState.phase != CanvasDragPhase.Idle) 10f else 0f)
                        .offset {
                            IntOffset(nodeLeftPx.roundToInt(), nodeTopPx.roundToInt())
                        }
                        .size(width = nodeWidthDp, height = nodeHeightDp)
                )
            }
        }
    }
}

internal fun computeEdgeAutoPanStep(
    pointerViewportPx: Offset,
    viewportWidthPx: Float,
    viewportHeightPx: Float,
    edgeThresholdPx: Float,
    stepPx: Float,
): Offset {
    val dx = when {
        pointerViewportPx.x < edgeThresholdPx -> stepPx
        pointerViewportPx.x > viewportWidthPx - edgeThresholdPx -> -stepPx
        else -> 0f
    }
    val dy = when {
        pointerViewportPx.y < edgeThresholdPx -> stepPx
        pointerViewportPx.y > viewportHeightPx - edgeThresholdPx -> -stepPx
        else -> 0f
    }
    return Offset(dx, dy)
}

private fun computeCenteredClouds(
    nodes: List<MindMapNodeLayout>,
    centerX: Float,
    centerY: Float,
    density: Float,
): List<CloudEnclosure> {
    val cloudsList = mutableListOf<CloudEnclosure>()
    val padding = 12f * density

    nodes.forEach { nodeLayout ->
        val cloud = nodeLayout.node.cloud
        if (cloud != null) {
            val descendantIds = collectSubtreeIds(nodeLayout.node)
            val subtreeLayouts = nodes.filter { descendantIds.contains(it.node.id) }
            if (subtreeLayouts.isNotEmpty()) {
                val minX = subtreeLayouts.minOf { it.x - it.width / 2f }
                val maxX = subtreeLayouts.maxOf { it.x + it.width / 2f }
                val minY = subtreeLayouts.minOf { it.y - it.height / 2f }
                val maxY = subtreeLayouts.maxOf { it.y + it.height / 2f }

                cloudsList.add(
                    CloudEnclosure(
                        node = nodeLayout.node,
                        cloud = cloud,
                        left = centerX + minX - padding,
                        top = centerY + minY - padding,
                        right = centerX + maxX + padding,
                        bottom = centerY + maxY + padding,
                    )
                )
            }
        }
    }
    return cloudsList
}

private fun computeCenteredArrowLinks(
    nodes: List<MindMapNodeLayout>,
    centerX: Float,
    centerY: Float,
): List<ArrowLinkConnector> {
    val links = mutableListOf<ArrowLinkConnector>()
    val nodeLayoutsById = nodes.associateBy { it.node.id }

    nodes.forEach { srcLayout ->
        srcLayout.node.connectors.forEach { connector ->
            val destLayout = nodeLayoutsById[connector.destinationId]
            if (destLayout != null) {
                links.add(
                    ArrowLinkConnector(
                        sourceId = srcLayout.node.id,
                        destinationId = connector.destinationId,
                        startX = centerX + srcLayout.x,
                        startY = centerY + srcLayout.y,
                        endX = centerX + destLayout.x,
                        endY = centerY + destLayout.y,
                        color = connector.color,
                        middleLabel = connector.middleLabel,
                        startArrow = connector.startArrow != null && connector.startArrow != "None",
                        endArrow = connector.endArrow != "None",
                    )
                )
            }
        }
    }
    return links
}

@Preview
@Preview
@Composable
fun PreviewMindMapCanvasScreen() {
    ContrastAwareReplyTheme {
        val root = Node(
            parentNode = null,
            id = "root",
            numericId = 1,
            text = "Droidplane Architecture",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val leftNode = Node(
            parentNode = root,
            id = "left",
            numericId = 2,
            text = "Data Module (:data)",
            position = "left",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val rightNode = Node(
            parentNode = root,
            id = "right",
            numericId = 3,
            text = "Presentation (:app)",
            position = "right",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val rightSubNode = Node(
            parentNode = rightNode,
            id = "right_sub",
            numericId = 4,
            text = "Jetpack Compose Canvas",
            creationDate = 0L,
            modificationDate = 0L,
        )
        rightNode.childNodes.add(rightSubNode)
        root.childNodes.addAll(listOf(leftNode, rightNode))

        MindMapCanvasScreen(
            rootNode = root,
            selectedNodeId = "right",
            collapsedNodeIds = emptySet(),
            onNodeSelect = {},
            onNodeToggleCollapse = {},
        )
    }
}
