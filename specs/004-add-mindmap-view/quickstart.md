# Quickstart & Validation Guide: Mind Map View with View Mode Toggle

**Feature Branch**: `004-add-mindmap-view`
**Feature Directory**: `specs/004-add-mindmap-view`
**Status**: Ready for Validation

## Prerequisites

- Android SDK installed with `targetSdk = 35` and `minSdk = 26`
- Java 21 toolchain configured
- Emulator or connected physical device running Android 8.0+

## Build & Test Commands

```bash
# Run JVM unit tests across all modules
./gradlew test

# Run Android Compose UI & instrumentation tests
./gradlew connectedAndroidTest

# Verify code formatting and linting
./gradlew lint
```

---

## Validation Scenarios

### Scenario 1: Toggle Display Mode from TopBar Menu (US2)

**Objective**: Verify user can switch between List view and Mind Map view via the options menu without data loss.

1. **Launch App**: Open Droidplane with the default sample mindmap (`example.mm`).
2. **Inspect Initial State**:
   - Verify document renders in the standard hierarchical `nodeList` view.
   - Tap the overflow menu (⋮) in the top right.
   - Confirm menu item displays **"Mind Map View"** (or localized "Vue Carte mentale").
3. **Switch to Mind Map View**:
   - Tap the menu item.
   - **Expected Result**: Screen switches immediately to `MindMapCanvasScreen` showing the 2D spatial mindmap with the root node centered and branches extending to the left and right.
4. **Switch Back to List View**:
   - Tap the overflow menu (⋮) again.
   - Confirm menu item displays **"List View"** (or localized "Vue Liste").
   - Tap the menu item.
   - **Expected Result**: Screen returns immediately to the hierarchical `nodeList` view.
5. **State Preservation**:
   - Edit a node or add a child in either view.
   - Toggle view modes.
   - **Expected Result**: Unsaved changes persist with 100% fidelity; no file reload occurs.

---

### Scenario 2: 2D Canvas Exploration with Pan & Zoom (US1)

**Objective**: Verify smooth 2D navigation on the spatial mindmap canvas.

1. **Open Mind Map View**: Switch to 2D Mind Map view.
2. **Pan Navigation**:
   - Drag a single finger across the screen in diagonal directions.
   - **Expected Result**: The canvas translates smoothly following touch gestures without stutter or clipping.
3. **Pinch-to-Zoom**:
   - Perform a two-finger pinch gesture to zoom out to 0.5x, then zoom in to 2.0x.
   - **Expected Result**: Canvas smoothly scales between 0.25x and 3.0x. Nodes and connector lines remain sharp and legible.
4. **Double-Tap Reset**:
   - Double-tap on empty canvas space.
   - **Expected Result**: Viewport centers back onto the root node at 1.0x scale.

---

### Scenario 3: Branch Collapsing & Node Selection (US3)

**Objective**: Verify folding/unfolding branches and selecting nodes.

1. **Select Node**:
   - Tap on any child node on the canvas.
   - **Expected Result**: Node is highlighted with a distinct outline indicating active selection.
2. **Collapse Branch**:
   - Tap the fold toggle indicator `[-]` on a branch node with children.
   - **Expected Result**: The node's child subtrees collapse out of view, and the indicator toggles to `[+]`. Neighboring branches adjust vertically to reclaim space.
3. **Expand Branch**:
   - Tap the fold indicator `[+]`.
   - **Expected Result**: Child subtrees reappear in their correct 2D positions connected by branch curves.

---

### Scenario 4: Contextual Actions & Authoring (US4)

**Objective**: Verify node operations (edit, add child, delete) executed from Mind Map view.

1. **Context Menu**:
   - Long-press a node on the Mind Map canvas.
   - **Expected Result**: Context menu appears with standard actions: Edit, Add child node, Delete, Copy text.
2. **Edit Text**:
   - Select "Edit" and change text.
   - **Expected Result**: Node text updates immediately on the canvas, and the document is marked as having unsaved changes.
3. **Add Child Node**:
   - Tap FAB or select "Add child node" from the context menu. Enter "New Child" and confirm.
   - **Expected Result**: New node is appended to the target node's children and renders with an attached branch curve.
4. **Switch to List View**:
   - Switch to List view.
   - **Expected Result**: The newly created and edited nodes appear in their exact hierarchical positions in the list.
