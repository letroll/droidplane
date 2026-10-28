# UI & Domain Contracts: 2D Canvas Drag & Drop and Long-Press Release Toolbar

**Feature**: `014-nodelist-drag-drop`
**Date**: 2026-04-12

## 1. Domain Contract (`NodeManagerContract.kt` / `NodeManager.kt`)

### `moveNode`
```kotlin
/**
 * Moves [draggedNodeId] to [targetParentId], either as a sibling relative to [targetSiblingId]
 * (when [targetSiblingId] != null) or as the last child of [targetParentId] (when [targetSiblingId] == null).
 *
 * @param draggedNodeId ID of the non-root node being moved.
 * @param targetParentId ID of the target parent node.
 * @param targetSiblingId Optional ID of an existing child of [targetParentId] to insert before/after.
 * @param insertAfter When [targetSiblingId] is provided, true inserts immediately after [targetSiblingId],
 *                    false inserts immediately before [targetSiblingId].
 * @param targetPosition Optional branch side ("left" or "right") when [targetParentId] is the root node;
 *                       cleared to null automatically when [targetParentId] is a non-root node.
 * @return true if the tree structure, sibling order, or branch position changed; false if invalid or no-op.
 */
fun moveNode(
    draggedNodeId: String,
    targetParentId: String,
    targetSiblingId: String? = null,
    insertAfter: Boolean = true,
    targetPosition: String? = null,
): Boolean
```

---

## 2. ViewModel Contract (`MainViewModel.kt`)

### `onMoveNode`
```kotlin
/**
 * Moves a node across the mind map tree (reordering among siblings, re-parenting onto a new parent,
 * or switching root branch side) and increments [MainUiState.treeVersion] + unfolds [targetParentId].
 */
fun onMoveNode(
    draggedNodeId: String,
    targetParentId: String,
    targetSiblingId: String? = null,
    insertAfter: Boolean = true,
    targetPosition: String? = null,
)
```
**Post-conditions when `nodeManager.moveNode(...)` returns `true`**:
- `hasUnsavedChanges = true`
- `collapsedNodeIds = currentState.collapsedNodeIds - targetParentId` (ensures target parent is expanded on the canvas so the moved node remains visible)
- `treeVersion = currentState.treeVersion + 1` (triggers recomputation of `MindMapLayoutEngine.computeLayout`)

---

## 3. Canvas Drag State & Hit-Testing Contract (`MindMapCanvasDragState.kt`)

### `resolveCanvasDropTarget`
```kotlin
/**
 * Pure function that resolves the active [CanvasDropTarget] for a dragged node at [probeCanvasPos]
 * (in unscaled canvas coordinates relative to the root node at (0, 0)).
 *
 * Rules:
 * - Excludes any node in [excludedNodeIds] (the dragged node itself and all of its descendants).
 * - Finds the first node layout whose bounding box [x - width/2 .. x + width/2, y - height/2 .. y + height/2]
 *   contains [probeCanvasPos].
 * - If the hovered node is the root node (`isRoot == true`):
 *   - Returns `DropZoneType.AS_CHILD` with `targetParentId = hovered.node.id`, `targetSiblingId = null`,
 *     `insertAfter = true`, and `targetPosition = if (probeCanvasPos.x < hovered.x) "left" else "right"`.
 * - If the hovered node is a non-root node (`isRoot == false`):
 *   - Relative vertical fraction `relY = (probeCanvasPos.y - (hovered.y - hovered.height / 2f)) / hovered.height`
 *   - `relY < 0.25f` -> `DropZoneType.BEFORE_SIBLING` (`targetParentId = parent.id`, `targetSiblingId = hovered.node.id`, `insertAfter = false`, `targetPosition = hovered.node.position`)
 *   - `relY > 0.75f` -> `DropZoneType.AFTER_SIBLING` (`targetParentId = parent.id`, `targetSiblingId = hovered.node.id`, `insertAfter = true`, `targetPosition = hovered.node.position`)
 *   - `0.25f..0.75f` -> `DropZoneType.AS_CHILD` (`targetParentId = hovered.node.id`, `targetSiblingId = null`, `insertAfter = true`, `targetPosition = null`)
 */
internal fun resolveCanvasDropTarget(
    probeCanvasPos: Offset,
    layoutNodes: List<MindMapNodeLayout>,
    excludedNodeIds: Set<String>,
): CanvasDropTarget?
```

### `collectSubtreeIds`
```kotlin
/**
 * Recursively collects the IDs of [root] and all of its descendants to prevent cycle drops (FR-012).
 */
internal fun collectSubtreeIds(root: Node): Set<String>
```

---

## 4. Composable Contracts (`MindMapCanvasScreen.kt` & `MindMapNodeCard.kt`)

### `MindMapCanvasScreen`
Adds `onMoveNode` parameter (defaulting to no-op for existing previews/callers) and hosts `rememberMindMapCanvasDragState`:
```kotlin
@Composable
fun MindMapCanvasScreen(
    rootNode: Node,
    selectedNodeId: String?,
    collapsedNodeIds: Set<String>,
    treeVersion: Int,
    onNodeClick: (Node) -> Unit,
    onNodeDoubleClick: (Node) -> Unit,
    onNodeLongPress: (Node) -> Unit,
    onToggleFold: (Node) -> Unit,
    onClearSelection: () -> Unit,
    modifier: Modifier = Modifier,
    focusNodeId: String? = null,
    focusTrigger: Int = 0,
    onOpenNoteModal: (Node) -> Unit = {},
    onOpenLink: (Node) -> Unit = {},
    onMoveNode: (
        draggedNodeId: String,
        targetParentId: String,
        targetSiblingId: String?,
        insertAfter: Boolean,
        targetPosition: String?,
    ) -> Unit = { _, _, _, _, _ -> },
)
```

### `MindMapNodeCard`
Replaces `.combinedClickable` with custom pointer gesture handling that supports:
- Single tap -> `onNodeClick()`
- Double tap -> `onNodeDoubleClick()`
- Stationary long-press:
  - Entering long-press timeout without exceeding touch slop invokes `onLongPressStart()` (entering `ReadyToDrag`, **not** opening `NodeBottomToolbar`).
  - Releasing finger (`UP`) while still within touch slop invokes `onNodeLongPress(node)` on release.
  - Moving finger past touch slop on a non-root node invokes `onDragMove(deltaCanvasPx, viewportPointerPx)` and suppresses `onNodeLongPress(node)`.
  - Releasing finger (`UP`) during drag invokes `onDragEnd()`.
  - Gesture cancellation invokes `onDragCancel()`.
- Visual states on `MindMapNodeCard`:
  - `isDraggingOrReady: Boolean = false` — Applies elevated shadow (`8.dp`), slight scale (`1.04f`), and alpha (`0.92f`) while dragged.
  - `activeDropZone: DropZoneType? = null` — Renders top insertion line (`BEFORE_SIBLING`), bottom insertion line (`AFTER_SIBLING`), or child-attachment border highlight (`AS_CHILD`) on the target card.

---

## 5. Legacy Files Removed (`FR-013`)

The following unused list-view files are deleted from the repository:
- `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt`
- `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/MindMapScreen.kt`
- `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeListDragState.kt`
- `app/src/androidTest/java/fr/julien/quievreux/droidplane2/ui/components/NodeListTest.kt`
- `app/src/test/java/fr/julien/quievreux/droidplane2/ui/components/NodeListDragStateTest.kt`
