# Quickstart: 2D Canvas Drag & Drop, Long-Press Release Toolbar, and `NodeList` Removal

**Feature**: `014-nodelist-drag-drop`
**Date**: 2026-04-12

## 1. Automated Verification Commands

### Run `:data` Unit Tests (`NodeManager.moveNode`)
Verifies sibling reordering, cross-parent re-parenting, root left/right branch switching, parent unfolding, cycle rejection, and no-op handling:
```bash
./gradlew :data:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.data.NodeManagerTest"
```

### Run `:app` Unit Tests (`MindMapCanvasDragStateTest` & `MainViewModelTest`)
Verifies `CanvasDragPhase` transitions (`Idle` -> `ReadyToDrag` -> `Dragging` -> `Idle`), Freeplane-style `Top` (`BEFORE_SIBLING`), `Center` (`AS_CHILD`), and `Bottom` (`AFTER_SIBLING`) drop zone hit-testing, cycle exclusion (`collectSubtreeIds`), and `MainViewModel.onMoveNode`:
```bash
./gradlew :app:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.ui.mindmap.MindMapCanvasDragStateTest" --tests "fr.julien.quievreux.droidplane2.ui.MainViewModelTest"
```

### Run Full Unit Test Suite & Debug APK Build
Verifies that removing `NodeList.kt`, `MindMapScreen.kt`, `NodeListDragState.kt`, `NodeListTest.kt`, and `NodeListDragStateTest.kt` leaves zero broken references across `:app` and `:data`:
```bash
./gradlew testDebugUnitTest assembleDebug assembleDebugAndroidTest
```

### Run Instrumented UI Tests on Connected Device / Emulator (`MindMapCanvasScreenTest`)
```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=fr.julien.quievreux.droidplane2.ui.mindmap.MindMapCanvasScreenTest
```

---

## 2. Manual Verification Walkthrough on `MindMapCanvasScreen`

1. **Stationary Long-Press Opens `NodeBottomToolbar` Only on Finger Release (US1)**:
   - Open any mind map on `MindMapCanvasScreen`.
   - Press and hold your finger stationary on a node card for >1 second **without lifting your finger**.
   - **Expected**: The card elevates slightly (`ReadyToDrag` feedback on non-root nodes), and `NodeBottomToolbar` does **not** appear while your finger remains pressed down.
   - Lift your finger without moving.
   - **Expected**: `NodeBottomToolbar` appears immediately upon finger release.

2. **2D Drag & Drop Reordering Among Siblings (`Top` / `Bottom` Drop Zones — US2)**:
   - Press and hold a non-root node card until it elevates, then drag it over the **top 25%** or **bottom 25%** of a sibling node.
   - **Expected**: `NodeBottomToolbar` never opens; a horizontal insertion indicator line highlights the top or bottom edge of the target sibling.
   - Release your finger.
   - **Expected**: The dragged node moves before or after the target sibling on the canvas.

3. **2D Drag & Drop Re-Parenting (`Center` Drop Zone) & Root Branch Switching (US2)**:
   - Press and hold a node card and drag it over the **middle 50%** of another valid node `T`.
   - **Expected**: Target node `T` shows a child-attachment border highlight.
   - Release your finger.
   - **Expected**: The dragged node (and its entire subtree) becomes the last child of `T` (unfolding `T` if it was collapsed).
   - Drag a node onto the **left half** or **right half** of the **root node** and release.
   - **Expected**: The node attaches as a direct child of the root node on the left (`position = "left"`) or right (`position = "right"`) side of the canvas.

4. **Cycle Prevention & Tap Preservation (US3)**:
   - Attempt to drag the root node -> root node does not drag.
   - Attempt to drag a parent node onto one of its own children/descendants -> no drop zone highlights on the descendant, and releasing returns the parent node to its original position unchanged.
   - Single-tap a node -> selects the node. Double-tap a node -> opens inline rename / properties as before.
