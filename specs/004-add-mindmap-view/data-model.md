# Data Model: Mind Map View with View Mode Toggle

**Feature Branch**: `004-add-mindmap-view`
**Feature Directory**: `specs/004-add-mindmap-view`
**Status**: Completed

## Overview

This document specifies the UI state models, layout representation structures, and state transitions required to render and interact with the 2D spatial Mind Map view alongside the existing hierarchical List view.

---

## Entities & Value Objects

### 1. `DisplayMode` (Enumeration)

Defines the presentation strategy currently active in the application.

```kotlin
package fr.julien.quievreux.droidplane2.model

enum class DisplayMode {
    /** Hierarchical linear list of child nodes (classic view) */
    LIST,

    /** 2D spatial graphical mindmap canvas with branching lines (Freeplane view) */
    MIND_MAP,
}
```

- **Validation Rules**:
  - Must never be null; defaults to `DisplayMode.LIST`.
- **Transitions**:
  - `LIST` → `MIND_MAP`: Triggered when user selects "Switch to Mind Map view" in the top bar menu.
  - `MIND_MAP` → `LIST`: Triggered when user selects "Switch to List view" in the top bar menu.

---

### 2. `MainUiState` Extensions

The centralized presentation state is updated with view mode tracking, active selection, and branch folding state.

```kotlin
data class MainUiState(
    // ... existing fields ...
    val title: String = "",
    val loading: Boolean = false,
    val leaving: Boolean = false,
    val canGoBack: Boolean = false,
    val nodeCurrentlyDisplayed: Node? = null,
    // ...
    val displayMode: DisplayMode = DisplayMode.LIST,
    val selectedNodeId: String? = null,
    val collapsedNodeIds: Set<String> = emptySet(),
)
```

- **Invariants**:
  - `collapsedNodeIds` contains only valid node IDs belonging to the active mindmap.
  - `selectedNodeId` references either an active node in the mindmap or is `null`.
  - When switching from `MIND_MAP` to `LIST`, if `selectedNodeId` is non-null, `nodeCurrentlyDisplayed` is synchronized to that node (or its parent if the node has no children) so the user does not lose orientation.

---

### 3. `MindMapNodeLayout` (Internal Layout Model)

Computed by `MindMapLayoutEngine` to represent the positioned layout of a node on the 2D canvas.

```kotlin
package fr.julien.quievreux.droidplane2.ui.mindmap

import fr.julien.quievreux.droidplane2.data.model.Node

data class MindMapNodeLayout(
    val node: Node,
    val x: Float,              // Top-left or center X coordinate on canvas (in pixels/dp)
    val y: Float,              // Top-left or center Y coordinate on canvas (in pixels/dp)
    val width: Float,          // Measured visual width of node bubble
    val height: Float,         // Measured visual height of node bubble
    val branchDirection: BranchDirection, // ROOT, LEFT, or RIGHT
    val isCollapsed: Boolean,  // Whether children are folded/hidden
    val isSelected: Boolean,   // Whether node is currently highlighted/focused
)

enum class BranchDirection {
    ROOT,
    LEFT,
    RIGHT,
}
```

---

### 4. `BranchConnector` (Connecting Curve Model)

Represents a visual connecting line drawn between a parent node and child node on the canvas.

```kotlin
package fr.julien.quievreux.droidplane2.ui.mindmap

data class BranchConnector(
    val parentId: String,
    val childId: String,
    val startX: Float,
    val startY: Float,
    val endX: Float,
    val endY: Float,
    val direction: BranchDirection,
)
```

- **Validation Rules**:
  - `startX` and `startY` connect to the departure edge of the parent node.
  - `endX` and `endY` connect to the arrival edge of the child node.
  - Curve tangents are strictly horizontal at both endpoints to replicate Freeplane bezier aesthetics.

---

### 5. `MindMapCanvasViewport` (Canvas Transformation State)

Maintains the user's current spatial position and scale on the canvas.

```kotlin
package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.ui.geometry.Offset

data class MindMapCanvasViewport(
    val panOffset: Offset = Offset.Zero,
    val zoom: Float = 1.0f,
) {
    companion object {
        const val MIN_ZOOM = 0.25f
        const val MAX_ZOOM = 3.0f
        val DEFAULT = MindMapCanvasViewport()
    }
}
```

---

## State Transitions & Event Flow

```text
[User taps TopBar menu]
       │
       ▼
[AppTopBarAction.ToggleDisplayMode]
       │
       ▼
[MainViewModel.toggleDisplayMode()]
       │
       ▼
[_uiState.update { it.copy(displayMode = nextMode) }]
       │
       ├───────────────────────────────┬───────────────────────────────┐
       ▼                               ▼                               ▼
[displayMode == LIST]         [displayMode == MIND_MAP]     [State Preserved]
  LazyColumn(nodeList)          MindMapCanvasScreen           In-memory tree intact
  Hierarchical drill-down       2D Spatial Pan & Zoom         Unsaved changes intact
```
