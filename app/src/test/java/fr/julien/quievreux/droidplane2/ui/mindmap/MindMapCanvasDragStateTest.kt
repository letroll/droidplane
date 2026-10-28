package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.ui.geometry.Offset
import fr.julien.quievreux.droidplane2.data.model.Node
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MindMapCanvasDragStateTest {

    private fun buildSampleTree(): Triple<Node, Node, Node> {
        val root = Node(
            parentNode = null,
            id = "ID_ROOT",
            numericId = 1,
            text = "Root",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val childA = Node(
            parentNode = root,
            id = "ID_A",
            numericId = 2,
            text = "Branch A",
            position = "right",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val childB = Node(
            parentNode = root,
            id = "ID_B",
            numericId = 3,
            text = "Branch B",
            position = "right",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val subA1 = Node(
            parentNode = childA,
            id = "ID_A1",
            numericId = 4,
            text = "Sub A1",
            creationDate = 0L,
            modificationDate = 0L,
        )
        childA.childNodes.add(subA1)
        root.childNodes.addAll(listOf(childA, childB))
        return Triple(root, childA, childB)
    }

    private fun buildSampleLayouts(root: Node, childA: Node, childB: Node): List<MindMapNodeLayout> {
        val subA1 = childA.childNodes.first()
        return listOf(
            MindMapNodeLayout(
                node = root,
                x = 0f,
                y = 0f,
                width = 160f,
                height = 60f,
                branchDirection = BranchDirection.ROOT,
                isCollapsed = false,
                isSelected = false,
            ),
            MindMapNodeLayout(
                node = childA,
                x = 220f,
                y = -60f,
                width = 140f,
                height = 48f,
                branchDirection = BranchDirection.RIGHT,
                isCollapsed = false,
                isSelected = false,
            ),
            MindMapNodeLayout(
                node = childB,
                x = 220f,
                y = 60f,
                width = 140f,
                height = 48f,
                branchDirection = BranchDirection.RIGHT,
                isCollapsed = false,
                isSelected = false,
            ),
            MindMapNodeLayout(
                node = subA1,
                x = 420f,
                y = -60f,
                width = 120f,
                height = 40f,
                branchDirection = BranchDirection.RIGHT,
                isCollapsed = false,
                isSelected = false,
            ),
        )
    }

    @Test
    fun `collectSubtreeIds includes node and all recursive descendants`() {
        val (root, childA, _) = buildSampleTree()

        val subtreeA = collectSubtreeIds(childA)
        assertEquals(setOf("ID_A", "ID_A1"), subtreeA)

        val subtreeRoot = collectSubtreeIds(root)
        assertEquals(setOf("ID_ROOT", "ID_A", "ID_A1", "ID_B"), subtreeRoot)
    }

    @Test
    fun `resolveCanvasDropTarget resolves top 25 percent as BEFORE_SIBLING`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        // childB spans y = [36..84], height = 48. Top 25% is y < 48.
        val target = resolveCanvasDropTarget(
            probeCanvasPos = Offset(220f, 40f),
            layoutNodes = layouts,
            excludedNodeIds = collectSubtreeIds(childA),
        )

        assertNotNull(target)
        assertEquals("ID_B", target.hoveredNodeId)
        assertEquals(DropZoneType.BEFORE_SIBLING, target.zoneType)
        assertEquals("ID_ROOT", target.targetParentId)
        assertEquals("ID_B", target.targetSiblingId)
        assertEquals(false, target.insertAfter)
        assertEquals("right", target.targetPosition)
    }

    @Test
    fun `resolveCanvasDropTarget resolves center 50 percent as AS_CHILD`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        // childB center is y = 60 (relY = 0.5).
        val target = resolveCanvasDropTarget(
            probeCanvasPos = Offset(220f, 60f),
            layoutNodes = layouts,
            excludedNodeIds = collectSubtreeIds(childA),
        )

        assertNotNull(target)
        assertEquals("ID_B", target.hoveredNodeId)
        assertEquals(DropZoneType.AS_CHILD, target.zoneType)
        assertEquals("ID_B", target.targetParentId)
        assertNull(target.targetSiblingId)
        assertEquals(true, target.insertAfter)
        assertNull(target.targetPosition)
    }

    @Test
    fun `resolveCanvasDropTarget resolves bottom 25 percent as AFTER_SIBLING`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        // childB spans y = [36..84], height = 48. Bottom 25% is y > 72.
        val target = resolveCanvasDropTarget(
            probeCanvasPos = Offset(220f, 80f),
            layoutNodes = layouts,
            excludedNodeIds = collectSubtreeIds(childA),
        )

        assertNotNull(target)
        assertEquals("ID_B", target.hoveredNodeId)
        assertEquals(DropZoneType.AFTER_SIBLING, target.zoneType)
        assertEquals("ID_ROOT", target.targetParentId)
        assertEquals("ID_B", target.targetSiblingId)
        assertEquals(true, target.insertAfter)
        assertEquals("right", target.targetPosition)
    }

    @Test
    fun `resolveCanvasDropTarget on root node resolves AS_CHILD with left or right targetPosition`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        val excluded = collectSubtreeIds(childA)

        val leftRootTarget = resolveCanvasDropTarget(
            probeCanvasPos = Offset(-30f, 0f),
            layoutNodes = layouts,
            excludedNodeIds = excluded,
        )
        assertNotNull(leftRootTarget)
        assertEquals("ID_ROOT", leftRootTarget.hoveredNodeId)
        assertEquals(DropZoneType.AS_CHILD, leftRootTarget.zoneType)
        assertEquals("ID_ROOT", leftRootTarget.targetParentId)
        assertNull(leftRootTarget.targetSiblingId)
        assertEquals("left", leftRootTarget.targetPosition)

        val rightRootTarget = resolveCanvasDropTarget(
            probeCanvasPos = Offset(30f, 0f),
            layoutNodes = layouts,
            excludedNodeIds = excluded,
        )
        assertNotNull(rightRootTarget)
        assertEquals("ID_ROOT", rightRootTarget.hoveredNodeId)
        assertEquals(DropZoneType.AS_CHILD, rightRootTarget.zoneType)
        assertEquals("ID_ROOT", rightRootTarget.targetParentId)
        assertNull(rightRootTarget.targetSiblingId)
        assertEquals("right", rightRootTarget.targetPosition)
    }

    @Test
    fun `resolveCanvasDropTarget rejects self and descendant nodes to prevent cycles`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        val excluded = collectSubtreeIds(childA) // {"ID_A", "ID_A1"}

        // Hovering over childA itself -> null
        assertNull(
            resolveCanvasDropTarget(
                probeCanvasPos = Offset(220f, -60f),
                layoutNodes = layouts,
                excludedNodeIds = excluded,
            )
        )

        // Hovering over childA's descendant subA1 -> null
        assertNull(
            resolveCanvasDropTarget(
                probeCanvasPos = Offset(420f, -60f),
                layoutNodes = layouts,
                excludedNodeIds = excluded,
            )
        )
    }

    @Test
    fun `stationary long press enters ReadyToDrag without firing toolbar and fires onNodeLongPress on release`() {
        val (_, childA, _) = buildSampleTree()
        val longPressedNodes = mutableListOf<Node>()
        var moveCount = 0

        val state = MindMapCanvasDragState(
            onNodeLongPressProvider = { { node -> longPressedNodes.add(node) } },
            onMoveNodeProvider = { { _, _, _, _, _ -> moveCount++ } },
        )

        state.onLongPressConfirmed(childA, initialCenter = Offset(220f, -60f))

        // While finger is still held down in ReadyToDrag, toolbar callback MUST NOT fire (FR-002)
        assertEquals(CanvasDragPhase.ReadyToDrag, state.phase)
        assertEquals("ID_A", state.activeNodeId)
        assertTrue(longPressedNodes.isEmpty())
        assertEquals(0, moveCount)

        // Lifting the finger without moving triggers onNodeLongPress once and resets to Idle
        state.onRelease()

        assertEquals(CanvasDragPhase.Idle, state.phase)
        assertNull(state.activeNodeId)
        assertEquals(listOf(childA), longPressedNodes)
        assertEquals(0, moveCount)
    }

    @Test
    fun `moving past touch slop transitions to Dragging suppresses onNodeLongPress and commits onMoveNode on release`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        val longPressedNodes = mutableListOf<Node>()
        val moves = mutableListOf< MoveInvocation>()

        val state = MindMapCanvasDragState(
            onNodeLongPressProvider = { { node -> longPressedNodes.add(node) } },
            onMoveNodeProvider = {
                { draggedId, parentId, siblingId, insertAfter, pos ->
                    moves.add(MoveInvocation(draggedId, parentId, siblingId, insertAfter, pos))
                }
            },
        )

        // Start stationary hold on childA at (220, -60)
        state.onLongPressConfirmed(childA, initialCenter = Offset(220f, -60f))
        assertEquals(CanvasDragPhase.ReadyToDrag, state.phase)

        // Drag down by +120px on Y to hover center of childB at (220, +60)
        state.onDragMove(
            deltaCanvasPx = Offset(0f, 120f),
            viewportPointerPx = Offset(500f, 600f),
            layoutNodes = layouts,
        )
        assertEquals(CanvasDragPhase.Dragging, state.phase)
        assertNotNull(state.activeDropTarget)
        assertEquals("ID_B", state.activeDropTarget?.hoveredNodeId)
        assertEquals(DropZoneType.AS_CHILD, state.activeDropTarget?.zoneType)

        // Release finger -> commits onMoveNode once and never invokes onNodeLongPress
        state.onRelease()

        assertEquals(CanvasDragPhase.Idle, state.phase)
        assertTrue(longPressedNodes.isEmpty())
        assertEquals(
            listOf(
                MoveInvocation(
                    draggedNodeId = "ID_A",
                    targetParentId = "ID_B",
                    targetSiblingId = null,
                    insertAfter = true,
                    targetPosition = null,
                )
            ),
            moves,
        )
    }

    @Test
    fun `onCancel resets state from ReadyToDrag or Dragging without invoking callbacks`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        var longPressCalled = false
        var moveCalled = false

        val state = MindMapCanvasDragState(
            onNodeLongPressProvider = { { longPressCalled = true } },
            onMoveNodeProvider = { { _, _, _, _, _ -> moveCalled = true } },
        )

        state.onLongPressConfirmed(childA, initialCenter = Offset(220f, -60f))
        state.onDragMove(
            deltaCanvasPx = Offset(0f, 120f),
            viewportPointerPx = Offset(500f, 600f),
            layoutNodes = layouts,
        )
        assertEquals(CanvasDragPhase.Dragging, state.phase)

        state.onCancel()

        assertEquals(CanvasDragPhase.Idle, state.phase)
        assertNull(state.activeNodeId)
        assertNull(state.activeDropTarget)
        assertEquals(false, longPressCalled)
        assertEquals(false, moveCalled)
    }

    @Test
    fun `root node supports stationary long-press release toolbar but cancels if dragged`() {
        val (root, childA, childB) = buildSampleTree()
        val layouts = buildSampleLayouts(root, childA, childB)
        val longPressedNodes = mutableListOf<Node>()
        var moveCalled = false

        val state = MindMapCanvasDragState(
            onNodeLongPressProvider = { { node -> longPressedNodes.add(node) } },
            onMoveNodeProvider = { { _, _, _, _, _ -> moveCalled = true } },
        )

        // 1. Stationary long-press on root node -> release triggers onNodeLongPress(root)
        state.onLongPressConfirmed(root, initialCenter = Offset.Zero)
        assertEquals(CanvasDragPhase.ReadyToDrag, state.phase)
        assertTrue(state.isActiveNodeRoot)
        state.onRelease()
        assertEquals(listOf(root), longPressedNodes)

        // 2. Long-press on root node followed by drag movement -> cancels without entering Dragging
        longPressedNodes.clear()
        state.onLongPressConfirmed(root, initialCenter = Offset.Zero)
        state.onDragMove(
            deltaCanvasPx = Offset(50f, 50f),
            viewportPointerPx = Offset(300f, 300f),
            layoutNodes = layouts,
        )
        assertEquals(CanvasDragPhase.Idle, state.phase)
        state.onRelease()
        assertTrue(longPressedNodes.isEmpty())
        assertEquals(false, moveCalled)
    }

    private data class MoveInvocation(
        val draggedNodeId: String,
        val targetParentId: String,
        val targetSiblingId: String?,
        val insertAfter: Boolean,
        val targetPosition: String?,
    )
}
