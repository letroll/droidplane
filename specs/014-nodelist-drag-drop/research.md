# Research: 2D Canvas Drag & Drop, Long-Press Release Toolbar, and Legacy `NodeList` Removal

**Feature**: `014-nodelist-drag-drop`
**Date**: 2026-04-12
**Status**: Complete (Updated after `/speckit-clarify` pivot to 2D Canvas)

## 1. Gesture Disambiguation on `MindMapNodeCard` (Stationary Long-Press Release vs. 2D Drag)

### Context
[`MindMapNodeCard`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt) currently uses `.combinedClickable(onClick = onNodeClick, onDoubleClick = onNodeDoubleClick, onLongClick = { onNodeLongPress(node) })`. This causes two problems reported by the user:
1. `onLongClick` fires immediately upon reaching the long-press timeout while the user's finger is still held down, opening [`NodeBottomToolbar`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/NodeBottomToolbar.kt) prematurely.
2. Once `combinedClickable` consumes the long-press, subsequent drag movement is not routed to a 2D node drag handler, or conflicts with the parent canvas's `detectTransformGestures` pan/zoom handler.

### Decision
Replace `.combinedClickable` on [`MindMapNodeCard`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt) with a custom Compose `pointerInput` gesture detector (`awaitEachGesture`) coordinated with a hoisted [`MindMapCanvasDragState`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasDragState.kt):
- **Single tap** (`onNodeClick`): Triggered when the pointer is released before `viewConfiguration.longPressTimeoutMillis` and no second tap arrives within `viewConfiguration.doubleTapTimeoutMillis` (or when `onNodeDoubleClick` is not applicable).
- **Double tap** (`onNodeDoubleClick`): Triggered when a second tap arrives within `viewConfiguration.doubleTapTimeoutMillis`.
- **Stationary long-press (`ReadyToDrag`)**: When the primary pointer remains down past `viewConfiguration.longPressTimeoutMillis` without exceeding `viewConfiguration.touchSlop`:
  - `dragState.onLongPressDetected(node)` transitions the card to `CanvasDragPhase.ReadyToDrag` (visual elevation/scale feedback on non-root cards), **without** invoking `onNodeLongPress(node)` yet.
  - If the user **lifts their finger (`UP`) while still in `ReadyToDrag`** (movement never exceeded `touchSlop`), `dragState.onRelease()` resets the state to `Idle` and invokes `onNodeLongPress(node)` on release, opening `NodeBottomToolbar`.
  - If the user **moves their finger past `touchSlop` while in `ReadyToDrag`**:
    - For a **non-root node** (`!isRoot`), `dragState.onDragStart()` transitions to `CanvasDragPhase.Dragging`, consumes all pointer position changes (`change.consume()`), updates `dragOffsetPx`, evaluates drop targets, and suppresses `onNodeLongPress` for the entire gesture.
    - For the **root node** (`isRoot`), dragging is disabled (`FR-012`); exceeding `touchSlop` cancels `ReadyToDrag` back to `Idle` without opening the toolbar.
- **Cancel (`CANCEL` / multi-touch interruption)**: Resets `dragState.onCancel()` to `Idle` without invoking `onNodeLongPress` or `onMoveNode`.

### Rationale
- Consuming pointer changes inside `MindMapNodeCard`'s `pointerInput` while in `ReadyToDrag` / `Dragging` prevents the parent `MindMapCanvasScreen` `detectTransformGestures` from panning the canvas background while a node is being dragged (`FR-004`).
- Because `MindMapNodeCard` is placed inside the inner `Box` that has `graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = panOffset.x; translationY = panOffset.y }`, pointer movement deltas measured inside `MindMapNodeCard`'s local coordinate space are already scaled to **canvas coordinates (px)**—so adding `dragDelta` directly to `dragOffsetPx` moves the dragged node 1:1 with the user's finger at any zoom level.

### Alternatives Considered
- **Keeping `combinedClickable` + separate `detectDragGesturesAfterLongPress`**: Rejected because `combinedClickable`'s `onLongClick` fires immediately on timeout before finger release, which directly violates `FR-001` and `FR-002`.

---

## 2. Freeplane-Style 2D Drop Zone Hit-Testing (`Top` / `Center` / `Bottom`)

### Context
During 2D canvas drag-and-drop (`Option C` + `Option A` from clarifications), the user can drop a dragged node `D` onto any valid target node `T` visible in `layoutResult.nodes`:
- **Sibling before (`Top` zone)**: Top 25% of a non-root node `T` -> inserts `D` into `T.parentNode`'s children immediately before `T`, inheriting `T.position`.
- **Child attachment (`Center` zone)**: Middle 50% of a non-root node `T` (or 100% vertical height of the root node) -> attaches `D` as the last child of `T`. When `T` is the root node, the horizontal half of `T` (left half `pointerCanvasX < T.x` vs. right half `pointerCanvasX >= T.x`) sets `position = "left"` or `"right"`. When `T` is a non-root node, `D.position` is cleared to `null`.
- **Sibling after (`Bottom` zone)**: Bottom 25% of a non-root node `T` -> inserts `D` into `T.parentNode`'s children immediately after `T`, inheriting `T.position`.

### Decision
Implement pure, unit-testable hit-testing in [`MindMapCanvasDragState.kt`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasDragState.kt) (`resolveCanvasDropTarget`):
1. Given the dragged node's current center in canvas coordinates `dragCenter = Offset(initialLayout.x + dragOffsetPx.x, initialLayout.y + dragOffsetPx.y)` (or pointer position in canvas coordinates):
2. Filter `layoutResult.nodes` to exclude:
   - The dragged node itself (`candidate.node.id == draggedNodeId`)
   - Any descendant of the dragged node (`candidate.node.id in draggedSubtreeIds`) to prevent cycles (`FR-012`).
3. Find the candidate node `T` whose bounding box `[T.x - T.width/2, T.x + T.width/2] x [T.y - T.height/2, T.y + T.height/2]` contains the probe point.
4. Compute relative vertical fraction `relY = (probeY - (T.y - T.height / 2f)) / T.height` in `[0f, 1f]`:
   - If `T.isRoot`:
     - Always `DropZoneType.AS_CHILD` (`targetParentId = T.node.id`, `targetSiblingId = null`, `insertAfter = true`).
     - `targetPosition = if (probeX < T.x) "left" else "right"`.
   - If `!T.isRoot`:
     - `relY < 0.25f` -> `DropZoneType.BEFORE_SIBLING` (`targetParentId = T.node.parentNode.id`, `targetSiblingId = T.node.id`, `insertAfter = false`, `targetPosition = T.node.position`).
     - `relY > 0.75f` -> `DropZoneType.AFTER_SIBLING` (`targetParentId = T.node.parentNode.id`, `targetSiblingId = T.node.id`, `insertAfter = true`, `targetPosition = T.node.position`).
     - `0.25f..0.75f` -> `DropZoneType.AS_CHILD` (`targetParentId = T.node.id`, `targetSiblingId = null`, `insertAfter = true`, `targetPosition = null`).
5. Render clear visual feedback on the target node `T` in [`MindMapCanvasScreen`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasScreen.kt) / [`MindMapNodeCard`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt):
   - `BEFORE_SIBLING`: Highlighted horizontal insertion indicator bar along the top edge of `T`.
   - `AFTER_SIBLING`: Highlighted horizontal insertion indicator bar along the bottom edge of `T`.
   - `AS_CHILD`: Highlighted container border/tint around `T` (with left/right side accent when `T` is root).

### Rationale
- 25% / 50% / 25% vertical split gives generous hit targets for both re-parenting (50% center) and sibling insertion (25% top + 25% bottom) on touch screens.
- Keeping `resolveCanvasDropTarget` pure and independent of Compose UI state makes it 100% testable with `kotlin.test` in JVM unit tests.

---

## 3. Domain Layer Tree Mutation: `NodeManager.moveNode` (`:data` Module)

### Context
Currently, [`NodeManager`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt) only has `reorderChildNode(parentId, draggedChildId, targetNeighborId, insertAfter): Boolean`, which requires `draggedNode` and `targetNeighbor` to already share the same `parentId`. In 2D canvas drag-and-drop, a node can be:
- Reordered within the same parent (sibling drop on same parent),
- Moved to another parent's children list at a specific sibling position (`BEFORE_SIBLING` / `AFTER_SIBLING` on a node with a different parent),
- Attached as the last child of a new or existing parent (`AS_CHILD`),
- Switched between `"left"` and `"right"` branches of the root node.

### Decision
Add `moveNode` to [`NodeManagerContract`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt) and implement it in [`NodeManager`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt):
```kotlin
fun moveNode(
    draggedNodeId: String,
    targetParentId: String,
    targetSiblingId: String? = null,
    insertAfter: Boolean = true,
    targetPosition: String? = null,
): Boolean
```
**Invariants enforced by `moveNode`**:
1. **Lookup & Root Guard**: `draggedNode` must exist and have a non-null `parentNode` (root node cannot be moved). `targetParent` must exist.
2. **Self & Cycle Guard**: `targetParentId != draggedNodeId` and `targetParent` must not be a descendant of `draggedNode` (walk `targetParent.parentNode` chain up to root; if `draggedNodeId` is encountered, return `false`).
3. **Sibling Validation**: If `targetSiblingId != null`, `targetSibling` must be a direct child of `targetParent` and `targetSiblingId != draggedNodeId`.
4. **No-Op Detection**:
   - If `oldParent.id == targetParentId`, `effectivePosition == draggedNode.position`, and the resulting child index in `targetParent.childNodes` would be identical to `oldIndex`, return `false` without mutating state.
5. **Branch Position Normalization**:
   - If `targetParent.parentNode == null` (target parent is root): `effectivePosition = targetPosition ?: draggedNode.position ?: "right"`.
   - If `targetParent.parentNode != null` (target parent is non-root): `effectivePosition = null` (Freeplane format only stores `POSITION="left"|"right"` on direct children of root).
6. **Tree Mutation & Index Synchronization**:
   - Remove `draggedNode` from `oldParent.childNodes`.
   - Update `draggedNode.parentNode = targetParent` and `draggedNode.position = effectivePosition`.
   - Insert `draggedNode` into `targetParent.childNodes` (before/after `targetSiblingId` if provided, or at the end of `targetParent.childNodes` if `targetSiblingId == null`).
   - If `targetParent.isFolded`, set `targetParent.isFolded = false` so the newly attached child is visible (`FR-006`).
   - Propagate both `oldParent` and `targetParent` up to `rootNode` (`propagateUpdatedParentToRoot`), update `_allNodes` and `mindmapIndexes`, and return `true`.
- Keep `reorderChildNode` delegating to `moveNode` (or preserved for backward compatibility with existing `:data` tests).

### Rationale
- Centralizing all tree move/re-parent/reorder invariants in `NodeManager.moveNode` guarantees `.mm` Freeplane XML serialization fidelity (`MindMapSerializer` serializes `childNodes` order and `position` attribute on root children) and keeps `MainViewModel` thin.

---

## 4. Viewport Edge Auto-Panning on `MindMapCanvasScreen`

### Context
When dragging a node near the top, bottom, left, or right edge of [`MindMapCanvasScreen`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasScreen.kt), the canvas should pan smoothly (`FR-010`) so the user can drop the node onto targets initially outside the visible viewport.

### Decision
- Track the dragged node's screen-space pointer coordinates relative to the `BoxWithConstraints` viewport (`viewportWidthPx`, `viewportHeightPx`).
- Define an edge threshold of `56.dp` (`edgeThresholdPx`).
- While `dragState.phase == CanvasDragPhase.Dragging` and the pointer is within `edgeThresholdPx` of any viewport edge (left, right, top, bottom), run a frame-synced `LaunchedEffect(dragState.phase)` loop (~60fps / 16ms) that increments `panOffset` in the opposite direction (panning the canvas to reveal off-screen nodes) and shifts `dragState.dragOffsetPx` by `-panDelta / zoom` so the dragged node stays locked under the user's finger in screen space while re-evaluating `resolveCanvasDropTarget`.

---

## 5. Dead Code Removal (`NodeList`, `MindMapScreen`, `NodeListDragState`)

### Context
During `/speckit-clarify`, the user explicitly requested: *"fait le sur la vue canvas et supprime nodelist si ce n'est plus utilisé"*.
A codebase-wide search (`grep -rn "NodeList\|MindMapScreen" app/src/ data/src/`) confirmed that [`MainActivity.kt`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt) exclusively renders `MindMapCanvasScreen`, and nothing in production code calls `MindMapScreen` or `NodeList`.

### Decision
Delete the following unused files (`FR-013`):
1. `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt`
2. `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/MindMapScreen.kt`
3. `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeListDragState.kt`
4. `app/src/androidTest/java/fr/julien/quievreux/droidplane2/ui/components/NodeListTest.kt`
5. `app/src/test/java/fr/julien/quievreux/droidplane2/ui/components/NodeListDragStateTest.kt`
And update the preview label string in [`NodeIconsRow.kt`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeIconsRow.kt) if needed so no stale references remain.
