package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MindMapCanvasScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private data class MoveCommand(
        val draggedNodeId: String,
        val targetParentId: String,
        val targetSiblingId: String?,
        val insertAfter: Boolean,
        val targetPosition: String?,
    )

    private fun createSampleMindMap(): Node {
        val root = Node(
            parentNode = null,
            id = "ID_ROOT",
            numericId = 1,
            text = "Root Map",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val taskA = Node(
            parentNode = root,
            id = "ID_A",
            numericId = 2,
            text = "Task A",
            position = "right",
            creationDate = 0L,
            modificationDate = 0L,
        )
        val taskB = Node(
            parentNode = root,
            id = "ID_B",
            numericId = 3,
            text = "Task B",
            position = "right",
            creationDate = 0L,
            modificationDate = 0L,
        )
        root.childNodes.addAll(listOf(taskA, taskB))
        return root
    }

    @Test
    fun stationaryLongPressDoesNotInvokeOnNodeLongPressUntilFingerRelease() {
        val root = createSampleMindMap()
        val longPressedNodeIds = mutableListOf<String>()
        val moveCommands = mutableListOf<MoveCommand>()

        composeRule.setContent {
            ContrastAwareReplyTheme {
                MindMapCanvasScreen(
                    rootNode = root,
                    selectedNodeId = null,
                    collapsedNodeIds = emptySet(),
                    onNodeSelect = {},
                    onNodeToggleCollapse = {},
                    onNodeLongPress = { longPressedNodeIds.add(it.id) },
                    onMoveNode = { draggedId, parentId, siblingId, insertAfter, pos ->
                        moveCommands.add(MoveCommand(draggedId, parentId, siblingId, insertAfter, pos))
                    },
                    modifier = Modifier.size(800.dp, 600.dp),
                )
            }
        }

        // Hold finger down past longPressTimeoutMillis WITHOUT lifting up
        composeRule.onNodeWithText("Task A").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 150L)
        }
        composeRule.waitForIdle()

        // While finger is still held down, onNodeLongPress MUST NOT be triggered (FR-002)
        assertTrue(
            "onNodeLongPress must not fire while finger is still pressed down",
            longPressedNodeIds.isEmpty()
        )

        // Now lift the finger without dragging
        composeRule.onNodeWithText("Task A").performTouchInput {
            up()
        }
        composeRule.waitForIdle()

        assertEquals(listOf("ID_A"), longPressedNodeIds)
        assertTrue(moveCommands.isEmpty())
    }

    @Test
    fun longPressAndDragOntoSiblingNodeInvokesOnMoveNodeOnReleaseWithoutOpeningToolbar() {
        val root = createSampleMindMap()
        val longPressedNodeIds = mutableListOf<String>()
        val moveCommands = mutableListOf<MoveCommand>()

        composeRule.setContent {
            ContrastAwareReplyTheme {
                MindMapCanvasScreen(
                    rootNode = root,
                    selectedNodeId = null,
                    collapsedNodeIds = emptySet(),
                    onNodeSelect = {},
                    onNodeToggleCollapse = {},
                    onNodeLongPress = { longPressedNodeIds.add(it.id) },
                    onMoveNode = { draggedId, parentId, siblingId, insertAfter, pos ->
                        moveCommands.add(MoveCommand(draggedId, parentId, siblingId, insertAfter, pos))
                    },
                    modifier = Modifier.size(800.dp, 600.dp),
                )
            }
        }

        val taskABounds = composeRule.onNodeWithText("Task A").fetchSemanticsNode().boundsInRoot
        val taskBBounds = composeRule.onNodeWithText("Task B").fetchSemanticsNode().boundsInRoot
        val deltaToTaskBCenter = taskBBounds.center - taskABounds.center

        composeRule.onNodeWithText("Task A").performTouchInput {
            down(center)
            advanceEventTime(viewConfiguration.longPressTimeoutMillis + 150L)
            moveBy(deltaToTaskBCenter)
            up()
        }
        composeRule.waitForIdle()

        assertTrue(
            "onNodeLongPress must not be called when dragging",
            longPressedNodeIds.isEmpty()
        )
        assertEquals(1, moveCommands.size)
        val cmd = moveCommands.single()
        assertEquals("ID_A", cmd.draggedNodeId)
        assertEquals("ID_B", cmd.targetParentId)
    }

    @Test
    fun singleTapAndDoubleTapOnCanvasNodeInvokeExpectedCallbacks() {
        val root = createSampleMindMap()
        val selectedIds = mutableListOf<String>()
        val doubleClickedIds = mutableListOf<String>()

        composeRule.setContent {
            ContrastAwareReplyTheme {
                MindMapCanvasScreen(
                    rootNode = root,
                    selectedNodeId = null,
                    collapsedNodeIds = emptySet(),
                    onNodeSelect = { selectedIds.add(it.id) },
                    onNodeToggleCollapse = {},
                    onNodeDoubleClick = { doubleClickedIds.add(it.id) },
                    modifier = Modifier.size(800.dp, 600.dp),
                )
            }
        }

        composeRule.onNodeWithText("Task A").performTouchInput {
            click(center)
        }
        composeRule.mainClock.advanceTimeBy(400L)
        composeRule.waitForIdle()
        assertEquals(listOf("ID_A"), selectedIds)

        composeRule.onNodeWithText("Task B").performTouchInput {
            doubleClick(center)
        }
        composeRule.waitForIdle()
        assertEquals(listOf("ID_B"), doubleClickedIds)
    }
}
