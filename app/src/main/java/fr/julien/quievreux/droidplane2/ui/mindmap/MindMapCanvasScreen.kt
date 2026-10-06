package fr.julien.quievreux.droidplane2.ui.mindmap

import android.content.res.Configuration
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import kotlin.math.roundToInt

@Composable
fun MindMapCanvasScreen(
    rootNode: Node,
    selectedNodeId: String?,
    collapsedNodeIds: Set<String>,
    onNodeSelect: (Node) -> Unit,
    onNodeToggleCollapse: (Node) -> Unit,
    onNodeContextMenuClick: (ContextMenuAction) -> Unit = {},
    fetchText: (Node) -> String? = { it.text },
    modifier: Modifier = Modifier,
) {
    var zoom by remember { mutableFloatStateOf(1.0f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    val density = LocalDensity.current

    val layoutResult = remember(rootNode, collapsedNodeIds, selectedNodeId, density.density) {
        MindMapLayoutEngine.computeLayout(
            rootNode = rootNode,
            collapsedNodeIds = collapsedNodeIds,
            selectedNodeId = selectedNodeId,
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
                        // Reset zoom and pan on double tap
                        zoom = 1.0f
                        panOffset = Offset.Zero
                    }
                )
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoomChange, _ ->
                    zoom = (zoom * zoomChange).coerceIn(0.25f, 3.0f)
                    panOffset += pan
                }
            }
    ) {
        val centerX = constraints.maxWidth / 2f
        val centerY = constraints.maxHeight / 2f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoom
                    scaleY = zoom
                    translationX = panOffset.x
                    translationY = panOffset.y
                }
        ) {
            // Draw branch connectors with coordinates offset from canvas center
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

            // Render positioned nodes
            layoutResult.nodes.forEach { nodeLayout ->
                val nodeLeftPx = centerX + nodeLayout.x - (nodeLayout.width / 2f)
                val nodeTopPx = centerY + nodeLayout.y - (nodeLayout.height / 2f)

                val nodeWidthDp = with(density) { nodeLayout.width.toDp() }
                val nodeHeightDp = with(density) { nodeLayout.height.toDp() }

                MindMapNodeCard(
                    node = nodeLayout.node,
                    isSelected = nodeLayout.isSelected,
                    isCollapsed = nodeLayout.isCollapsed,
                    branchDirection = nodeLayout.branchDirection,
                    onNodeClick = { onNodeSelect(nodeLayout.node) },
                    onToggleCollapse = { onNodeToggleCollapse(nodeLayout.node) },
                    onContextMenuAction = onNodeContextMenuClick,
                    fetchText = fetchText,
                    modifier = Modifier
                        .offset {
                            IntOffset(nodeLeftPx.roundToInt(), nodeTopPx.roundToInt())
                        }
                        .size(width = nodeWidthDp, height = nodeHeightDp)
                )
            }
        }
    }
}

@Preview(name = "MindMap Canvas Preview - Light")
@Preview(name = "MindMap Canvas Preview - Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
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
