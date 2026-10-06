# Research & Technical Decisions: Mind Map View with View Mode Toggle

**Feature Branch**: `004-add-mindmap-view`
**Feature Directory**: `specs/004-add-mindmap-view`
**Status**: Completed

## Overview

This document captures the architectural evaluation, technical decisions, and trade-off analysis for implementing a Freeplane-like 2D spatial Mind Map view alongside the existing hierarchical List view in Droidplane.

---

## Decision 1: Pure Jetpack Compose 2D Canvas vs. 3rd-Party `MindMapView` Library

### Context
Droidplane currently has an existing dependency on `com.github.hegleB:MindMapView:0.1.0` in `libs.versions.toml` and `app/build.gradle.kts`, and legacy model classes in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/model/`. We evaluated whether to wrap this library via `AndroidView` or build a pure Jetpack Compose 2D canvas component.

### Evaluation
- **Freeplane Metaphor & Layout**: Freeplane mindmaps have a central root node with branches extending horizontally to both the left and right sides, respecting node `POSITION` attributes (`left`, `right`, `bottom_or_right`, `top_or_left`). The external `MindMapView` library only supports right-sided branching (`MindMapRightLayoutManager`), failing the Freeplane similarity requirement.
- **Data Compatibility & Stability**: `MindMapView`'s internal `Tree` class hardcodes `ROOT_ID = "root"`. Freeplane documents use unique alphanumeric IDs (e.g., `ID_123456789`). Loading real Freeplane mindmaps into `MindMapView` throws `IllegalArgumentException: Root node does not exist`.
- **Architectural Alignment (Constitution Principle I & V)**: Jetpack Compose with Material 3 is Droidplane's official presentation standard. Native Compose enables `@Preview` support (mandated by workspace guidelines), automatic dark/light theme adaptation, and direct binding to Kotlin `StateFlow`.
- **Performance**: A pure Compose canvas using `Modifier.pointerInput` and custom drawing/layout passes avoids the overhead of View-Compose bridging and dual in-memory tree duplication.

### Decision
**Implement a native Jetpack Compose 2D MindMap Canvas (`MindMapCanvasScreen` / `MindMapCanvas`).** Reject legacy View interop with `com.github.hegleB:MindMapView`.

### Alternatives Considered
- *Wrap `com.github.hegleB:MindMapView` via `AndroidView`*: Rejected due to lack of left-side branching, hardcoded root ID crash, lack of theme adaptation, and Compose `@Preview` incompatibility.
- *WebView with web-based mindmap library (e.g., jsMind, D3)*: Rejected due to heavy runtime memory overhead, sluggish touch gestures on mobile, and loss of native look-and-feel.

---

## Decision 2: Display Mode State Management & Menu Switching

### Context
The user requested an option in the menu to switch between the visual mindmap display mode and the current hierarchical list display mode.

### Decision
1. Introduce a `DisplayMode` enum:
   ```kotlin
   enum class DisplayMode {
       LIST,
       MIND_MAP,
   }
   ```
2. Add `val displayMode: DisplayMode = DisplayMode.LIST` to `MainUiState`.
3. Add `AppTopBarAction.ToggleDisplayMode` to `AppTopBarAction` and wire it into the `DropdownMenu` in `AppTopBar.kt`:
   - When in `LIST` mode: shows "Switch to Mind Map view" (or localized resource `R.string.switch_to_mindmap_view`).
   - When in `MIND_MAP` mode: shows "Switch to List view" (or localized resource `R.string.switch_to_list_view`).
4. In `MainActivity.kt`, render `nodeList` when `state.value.displayMode == DisplayMode.LIST`, and render `MindMapCanvasScreen` when `state.value.displayMode == DisplayMode.MIND_MAP`.
5. Switching display modes does not reload the file or mutate the underlying `Node` hierarchy; it simply alternates the presentation strategy in `MainUiState`.

### Rationale
- Strictly follows Unidirectional Data Flow (Constitution Principle II): all state transitions occur in `MainViewModel` and flow downstream via immutable `StateFlow<MainUiState>`.
- Zero-latency switching with 100% preservation of in-memory edits and unsaved changes.

### Alternatives Considered
- *Separate Activity (`MindMapActivity`)*: Rejected because launching a new Activity introduces inter-process/intent serialization hurdles for dirty in-memory mindmaps and breaks the single-activity architecture.
- *Bottom Navigation Bar*: Rejected because it reduces vertical screen space on mobile devices and conflicts with the minimalist UI design of Droidplane.

---

## Decision 3: 2D MindMap Layout Engine (Freeplane Branching Algorithm)

### Context
In Freeplane, the root node is at the center of the canvas. First-level child nodes branch out to the left and right sides. Descendant nodes continue branching outwards in the direction of their parent branch. Connecting lines are rendered as smooth curves.

### Decision
Create a clean, decoupled layout engine (`fr.julien.quievreux.droidplane2.ui.mindmap.MindMapLayoutEngine` in `:app`) that computes 2D coordinates for visual nodes and connecting branch curves:
1. **Side Partitioning**:
   - First-level children with `position == "left"` or `"top_or_left"` -> Left side (`direction = -1`).
   - First-level children with `position == "right"` or `"bottom_or_right"` -> Right side (`direction = +1`).
   - If `position` is null or unspecified, balance first-level children evenly (first half right, second half left).
   - All descendants inherit the branch direction of their top-level ancestor.
2. **Subtree Geometry & Spacing**:
   - Leaf nodes have a measured size based on text length (with min/max width and padding).
   - A branch node's height is the sum of its visible (non-collapsed) child subtree heights plus vertical spacing (`verticalSpacing = 16.dp`).
   - Horizontal spacing between parent and child columns is fixed (`horizontalSpacing = 48.dp`).
   - The parent node is vertically centered relative to the vertical span of its children.
3. **Connecting Curves**:
   - Branches are drawn using cubic Bezier curves (`Path.cubicTo`):
     - Start point: right edge of parent (if right branch) or left edge (if left branch).
     - End point: left edge of child (if right branch) or right edge (if left branch).
     - Control points: smooth horizontal tangent midpoints between parent and child.

### Rationale
- Delivers an authentic Freeplane desktop appearance.
- Fast, deterministic $O(N)$ calculation suitable for instant layout and 60 fps interactions.

### Alternatives Considered
- *Physics / Force-Directed Graph Layout*: Rejected because Freeplane mindmaps have a strict hierarchical tree structure; physics engines cause node jitter and lack predictable branch alignment.
- *Purely right-directed tree*: Rejected because it does not match Freeplane's signature two-sided layout.

---

## Decision 4: Canvas Navigation (Pan & Zoom Gestures)

### Context
Mindmaps often exceed the physical screen dimensions of mobile devices. Users need seamless navigation across large canvases.

### Decision
Implement canvas viewport transformations using Jetpack Compose gesture detection:
1. Viewport state maintains:
   - `zoom: Float` (clamped between `0.25f` and `3.0f`, default `1.0f`).
   - `panOffset: Offset` (translation in X and Y).
2. Canvas container applies `Modifier.pointerInput` with `detectTransformGestures { centroid, pan, zoomChange, _ -> ... }` to update pan and zoom state smoothly.
3. Node rendering is performed inside a transformed coordinate layer (`graphicsLayer { scaleX = zoom; scaleY = zoom; translationX = panOffset.x; translationY = panOffset.y }`).
4. Double-tap gesture resets the viewport to center the root node.

### Rationale
- Native Compose gestures provide 60/120 fps hardware-accelerated pan and zoom.
- Prevents gesture collision with individual node click and long-press handlers.

### Alternatives Considered
- *Nested standard Compose `Modifier.verticalScroll` and `Modifier.horizontalScroll`*: Rejected because standard 1D scroll modifiers do not support simultaneous 2D diagonal dragging or pinch-to-zoom.

---

## Decision 5: Node Interaction, Branch Collapsing, and Action Parity

### Context
The user needs to navigate, fold/unfold branches, and execute node operations (edit, add child, delete) directly in the Mind Map view.

### Decision
1. **Selection**: Tapping a node sets `selectedNodeId` in `MainUiState`. The selected node displays a distinct outline or elevation.
2. **Branch Collapsing**: Nodes with children display a small fold indicator toggle (`[-]` when expanded, `[+]` when collapsed). Tapping the toggle adds/removes the node ID in `collapsedNodeIds: Set<String>` in `MainUiState`. Collapsed subtrees are omitted from the layout pass, keeping the map clean.
3. **Contextual Actions**:
   - Long-pressing a node triggers the existing context menu actions (`ContextMenuAction`: Edit, Add child node, Delete, Copy text).
   - Tapping the existing Floating Action Button (Add) creates a child on the currently selected node (or root node if none selected).
   - Dialogs (`EditNodeDescription`, `AddChildNode`, `DeleteConfirmation`) are completely reused from `MainUiState.DialogType`.
4. **View Switching Synchronization**:
   - If a node is selected in Mind Map view, switching to List view updates `nodeCurrentlyDisplayed` to that node, providing immediate contextual continuity.

### Rationale
- Reuses established dialogs and domain logic from features 001, 002, and 003, ensuring zero code duplication and zero regression risk.
- Preserves full data integrity and compatibility with Freeplane files.
