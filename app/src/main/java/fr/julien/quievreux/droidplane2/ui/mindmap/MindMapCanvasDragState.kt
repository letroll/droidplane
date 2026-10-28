package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import fr.julien.quievreux.droidplane2.data.model.Node

/**
 * Mutually exclusive lifecycle phases of a long-press / drag gesture on a [MindMapNodeCard].
 */
enum class CanvasDragPhase {
    /** No active long-press or drag interaction. */
    Idle,

    /**
     * Finger has held stationary on a node card past the long-press timeout without exceeding touch slop.
     * - The NodeBottomToolbar is NOT shown while in this state (FR-002).
     * - Lifting the finger in this state triggers `onNodeLongPress(node)` on release (FR-002).
     * - Moving the finger past touch slop on a non-root node transitions to [Dragging] (FR-001, FR-003).
     */
    ReadyToDrag,

    /**
     * User is actively dragging a non-root node across the 2D canvas.
     * - `onNodeLongPress` is suppressed for the remainder of the gesture (FR-003).
     * - Releasing the finger in this state commits `onMoveNode` if [MindMapCanvasDragState.activeDropTarget]
     *   is non-null, then resets to [Idle] (FR-004, FR-006, FR-007).
     */
    Dragging,
}

/**
 * Freeplane-style relative drop zones on a target node `T` (`FR-004`).
 */
enum class DropZoneType {
    /** Top 25% of a non-root target node T -> insert as sibling immediately before T. */
    BEFORE_SIBLING,

    /** Middle 50% of a non-root target node T (or 100% of root node) -> attach as last child of T. */
    AS_CHILD,

    /** Bottom 25% of a non-root target node T -> insert as sibling immediately after T. */
    AFTER_SIBLING,
}

/**
 * Describes the resolved drop target under the dragged node on the 2D canvas.
 */
data class CanvasDropTarget(
    /** The visual node card currently hovered by the dragged node. */
    val hoveredNodeId: String,
    /** The drop zone active on [hoveredNodeId] (Top = BEFORE_SIBLING, Center = AS_CHILD, Bottom = AFTER_SIBLING). */
    val zoneType: DropZoneType,
    /** ID of the parent node that will receive the dragged node on drop. */
    val targetParentId: String,
    /** ID of the reference sibling node when [zoneType] is BEFORE_SIBLING or AFTER_SIBLING; null when AS_CHILD. */
    val targetSiblingId: String?,
    /** True when inserting after [targetSiblingId] (or appending as last child); false when inserting before [targetSiblingId]. */
    val insertAfter: Boolean,
    /** Branch side ("left" or "right") when [targetParentId] is the root node; null otherwise. */
    val targetPosition: String?,
)

/**
 * Recursively collects the IDs of [root] and all of its descendants to prevent cycle drops (`FR-007`).
 */
internal fun collectSubtreeIds(root: Node): Set<String> {
    val ids = mutableSetOf<String>()
    fun walk(current: Node) {
        if (ids.add(current.id)) {
            current.childNodes.toList().forEach { walk(it) }
        }
    }
    walk(root)
    return ids
}

/**
 * Pure function that resolves the active [CanvasDropTarget] for a dragged node at [probeCanvasPos]
 * (in unscaled canvas coordinates relative to the root node at `(0, 0)`).
 */
internal fun resolveCanvasDropTarget(
    probeCanvasPos: Offset,
    layoutNodes: List<MindMapNodeLayout>,
    excludedNodeIds: Set<String>,
): CanvasDropTarget? {
    val hovered = layoutNodes.firstOrNull { layout ->
        !excludedNodeIds.contains(layout.node.id) &&
            probeCanvasPos.x >= layout.x - layout.width / 2f &&
            probeCanvasPos.x <= layout.x + layout.width / 2f &&
            probeCanvasPos.y >= layout.y - layout.height / 2f &&
            probeCanvasPos.y <= layout.y + layout.height / 2f
    } ?: return null

    val isRoot = hovered.branchDirection == BranchDirection.ROOT || hovered.node.parentNode == null
    if (isRoot) {
        val side = if (probeCanvasPos.x < hovered.x) "left" else "right"
        return CanvasDropTarget(
            hoveredNodeId = hovered.node.id,
            zoneType = DropZoneType.AS_CHILD,
            targetParentId = hovered.node.id,
            targetSiblingId = null,
            insertAfter = true,
            targetPosition = side,
        )
    }

    val parentId = hovered.node.parentNode?.id ?: return null
    val topY = hovered.y - hovered.height / 2f
    val relY = if (hovered.height > 0f) {
        (probeCanvasPos.y - topY) / hovered.height
    } else {
        0.5f
    }
    val parentIsRoot = hovered.node.parentNode?.parentNode == null
    val siblingPosition = if (parentIsRoot) {
        hovered.node.position ?: if (hovered.branchDirection == BranchDirection.LEFT) "left" else "right"
    } else {
        null
    }

    return when {
        relY < 0.25f -> CanvasDropTarget(
            hoveredNodeId = hovered.node.id,
            zoneType = DropZoneType.BEFORE_SIBLING,
            targetParentId = parentId,
            targetSiblingId = hovered.node.id,
            insertAfter = false,
            targetPosition = siblingPosition,
        )
        relY > 0.75f -> CanvasDropTarget(
            hoveredNodeId = hovered.node.id,
            zoneType = DropZoneType.AFTER_SIBLING,
            targetParentId = parentId,
            targetSiblingId = hovered.node.id,
            insertAfter = true,
            targetPosition = siblingPosition,
        )
        else -> CanvasDropTarget(
            hoveredNodeId = hovered.node.id,
            zoneType = DropZoneType.AS_CHILD,
            targetParentId = hovered.node.id,
            targetSiblingId = null,
            insertAfter = true,
            targetPosition = null,
        )
    }
}

/**
 * State holder hoisted in [MindMapCanvasScreen] to coordinate long-press release toolbar (`FR-002`)
 * and 2D Freeplane-style drag-and-drop (`FR-001`, `FR-003`..`FR-008`).
 */
@Stable
class MindMapCanvasDragState(
    private val onNodeLongPressProvider: () -> ((Node) -> Unit),
    private val onMoveNodeProvider: () -> ((
        draggedNodeId: String,
        targetParentId: String,
        targetSiblingId: String?,
        insertAfter: Boolean,
        targetPosition: String?,
    ) -> Unit),
) {
    var phase: CanvasDragPhase by mutableStateOf(CanvasDragPhase.Idle)
        private set

    var activeNode: Node? by mutableStateOf(null)
        private set

    val activeNodeId: String?
        get() = activeNode?.id

    val isActiveNodeRoot: Boolean
        get() = activeNode?.let { it.parentNode == null } ?: false

    var draggedSubtreeIds: Set<String> by mutableStateOf(emptySet())
        private set

    var dragOffsetPx: Offset by mutableStateOf(Offset.Zero)
        private set

    var pointerViewportPx: Offset? by mutableStateOf(null)
        private set

    var activeDropTarget: CanvasDropTarget? by mutableStateOf(null)
        private set

    private var initialCanvasCenter: Offset = Offset.Zero

    /**
     * Enters [CanvasDragPhase.ReadyToDrag] when the finger holds stationary past the long-press timeout.
     * Does NOT invoke `onNodeLongPress` while the finger is still pressed down (`FR-002`).
     */
    fun onLongPressConfirmed(node: Node, initialCenter: Offset) {
        activeNode = node
        initialCanvasCenter = initialCenter
        dragOffsetPx = Offset.Zero
        pointerViewportPx = null
        activeDropTarget = null
        draggedSubtreeIds = if (node.parentNode != null) collectSubtreeIds(node) else emptySet()
        phase = CanvasDragPhase.ReadyToDrag
    }

    /**
     * Updates 2D drag offset and resolves the hovered drop target when the finger moves past touch slop.
     * Root nodes cannot be dragged; moving past touch slop on the root node cancels the stationary hold.
     */
    fun onDragMove(
        deltaCanvasPx: Offset,
        viewportPointerPx: Offset?,
        layoutNodes: List<MindMapNodeLayout>,
    ) {
        val current = activeNode ?: return
        if (current.parentNode == null) {
            reset()
            return
        }
        if (phase == CanvasDragPhase.ReadyToDrag || phase == CanvasDragPhase.Dragging) {
            phase = CanvasDragPhase.Dragging
            dragOffsetPx += deltaCanvasPx
            pointerViewportPx = viewportPointerPx
            val probePos = initialCanvasCenter + dragOffsetPx
            activeDropTarget = resolveCanvasDropTarget(
                probeCanvasPos = probePos,
                layoutNodes = layoutNodes,
                excludedNodeIds = draggedSubtreeIds,
            )
        }
    }

    /**
     * Called when the user lifts their finger (`UP`):
     * - If still in [CanvasDragPhase.ReadyToDrag], triggers `onNodeLongPress(node)` on release (`FR-002`).
     * - If in [CanvasDragPhase.Dragging] with a non-null [activeDropTarget], commits `onMoveNode` (`FR-006`).
     */
    fun onRelease() {
        val currentPhase = phase
        val currentNode = activeNode
        val currentTarget = activeDropTarget
        reset()

        when (currentPhase) {
            CanvasDragPhase.ReadyToDrag -> {
                if (currentNode != null) {
                    onNodeLongPressProvider().invoke(currentNode)
                }
            }
            CanvasDragPhase.Dragging -> {
                if (currentNode != null && currentTarget != null) {
                    onMoveNodeProvider().invoke(
                        currentNode.id,
                        currentTarget.targetParentId,
                        currentTarget.targetSiblingId,
                        currentTarget.insertAfter,
                        currentTarget.targetPosition,
                    )
                }
            }
            CanvasDragPhase.Idle -> Unit
        }
    }

    /**
     * Cancels any active long-press or drag without firing `onNodeLongPress` or `onMoveNode`.
     */
    fun onCancel() {
        reset()
    }

    private fun reset() {
        phase = CanvasDragPhase.Idle
        activeNode = null
        draggedSubtreeIds = emptySet()
        dragOffsetPx = Offset.Zero
        pointerViewportPx = null
        activeDropTarget = null
        initialCanvasCenter = Offset.Zero
    }
}

@Composable
fun rememberMindMapCanvasDragState(
    onNodeLongPress: (Node) -> Unit,
    onMoveNode: (
        draggedNodeId: String,
        targetParentId: String,
        targetSiblingId: String?,
        insertAfter: Boolean,
        targetPosition: String?,
    ) -> Unit,
): MindMapCanvasDragState {
    val currentOnNodeLongPress by rememberUpdatedState(onNodeLongPress)
    val currentOnMoveNode by rememberUpdatedState(onMoveNode)
    return remember {
        MindMapCanvasDragState(
            onNodeLongPressProvider = { currentOnNodeLongPress },
            onMoveNodeProvider = { currentOnMoveNode },
        )
    }
}
