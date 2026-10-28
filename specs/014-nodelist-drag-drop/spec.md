# Feature Specification: Drag and Drop Reordering on MindMap Canvas (and Dead NodeList Removal)

**Feature Branch**: `014-nodelist-drag-drop`  
**Created**: 2026-10-28  
**Status**: Draft  
**Input**: User description: "j'aimerai pouvoir à présent faire du drag & drop sur les nodes de nodeList" (clarified to target the active `MindMapCanvasScreen` / `MindMapNodeCard` view and remove unused `NodeList` / `MindMapScreen` components).

## Clarifications

### Session 2026-10-28

- Q: How should the user start a drag-and-drop operation on a node item? (FR-002) → A: Option B - Long-press and move anywhere on the card (holding without moving opens the context menu/toolbar on release; moving after long-press starts dragging).
- Q: When a user long-presses a node card without moving, when should the context menu/toolbar appear? (FR-002) → A: Option A - On finger release after a stationary long-press (during the hold, the card enters a "ready to drag" state; if the user moves, it drags; if they release without moving, the node toolbar opens).
- Q: How should the view visually indicate where the dragged node will be placed while the user is moving it? (FR-003) → A: Option A - Live item shifting (the dragged card is elevated and follows the finger while neighbor cards shift in real time to open the target slot).
- Q: When should the underlying mindmap data model commit the new child node order during a drag operation? (FR-004) → A: Option A - On drop (finger release) only (neighbor shifts during the drag are visual in the UI; the mindmap model and unsaved-changes state update once when the finger is lifted at a new index).
- Q: Sur quel écran et composant cible souhaitez-vous appliquer le drag & drop et l'ouverture du menu uniquement au relâchement du doigt ? (FR-001) → A: Vue Canevas active (`MindMapCanvasScreen` / `MindMapNodeCard`) et suppression de `NodeList` / `MindMapScreen` qui ne sont plus utilisés.
- Q: Sur le canevas 2D (`MindMapCanvasScreen`), quelle action de réorganisation le drag & drop d'un nœud doit-il permettre ? (FR-001) → A: Option C - Réordonnancement complet 2D + changement de parent (re-parenting) : permet de déposer un nœud sur n'importe quel autre nœud du canevas pour en changer le parent, le côté (gauche/droite sous la racine) ou l'ordre parmi les frères.
- Q: Lorsqu'on glisse un nœud vers un autre nœud T sur le canevas 2D, comment distinguer l'insertion comme frère (au-dessus/en dessous de T) du rattachement comme enfant de T ? (FR-003) → A: Option A - Zones Haut / Centre / Bas sur le nœud cible : survoler la zone centrale d'un nœud T rattache le nœud glissé comme enfant de T (Gauche/Droite selon le côté pour la racine) avec mise en évidence de T, tandis que survoler la zone haute ou basse d'un nœud non-racine T (ou l'espace entre deux frères) l'insère comme frère avant ou après T avec un indicateur visuel d'insertion.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reorder and Re-parent Nodes via 2D Drag and Drop on the Canvas (Priority: P1)

As a user organizing a mindmap on the 2D mindmap canvas (`MindMapCanvasScreen`), I want to drag and drop any non-root node (`MindMapNodeCard`) across the 2D canvas so that I can reorder it among siblings, move it to another parent node (re-parenting), or switch its branch side (left/right of the root node) directly on the canvas.

**Why this priority**: This is the primary capability requested by the user on the active mindmap screen. It provides full 2D manipulation of the mindmap hierarchy and sibling order.

**Independent Test**: Can be fully tested by opening a mindmap on the canvas, long-pressing and dragging a child node either above/below a sibling to reorder it, or onto another node to change its parent (or across the root node to switch between left and right branches), releasing it, and verifying that the canvas immediately reflects and retains the new hierarchy and order.

**Acceptance Scenarios**:

1. **Given** a parent node displays child nodes "Task A", "Task B", and "Task C" in that order on the canvas, **When** the user long-presses "Task C", drags it above "Task A", and drops it, **Then** the mindmap model commits the reorder once upon release to display "Task C", "Task A", "Task B" and marks the mindmap as having unsaved changes.
2. **Given** two branches "Branch 1" (with child "Task A") and "Branch 2" on the canvas, **When** the user long-presses "Task A", drags it onto "Branch 2", and drops it, **Then** "Task A" (along with its entire subtree) becomes a child of "Branch 2", is removed from "Branch 1", and the mindmap is marked as having unsaved changes.
3. **Given** a first-level node on the right side of the root node, **When** the user drags it to the left side of the root node (or re-parents a deeper node onto the root node's left/right side) and drops it, **Then** its `position` attribute is updated (`"left"` or `"right"`) and it renders on the corresponding side of the root node.
4. **Given** the user has reordered or re-parented nodes on the canvas and saves the mindmap, **When** the mindmap is closed and reopened, **Then** the nodes appear in the newly saved parent hierarchy, branch side, and sibling order.

---

### User Story 2 - Clear Visual Feedback and Gesture Coexistence on MindMapNodeCard (Priority: P2)

As a user interacting with nodes on the mindmap canvas, I want clear visual feedback while holding and dragging a node and seamless coexistence between long-press-and-move (to drag) and stationary long-press-and-release (to open `NodeBottomToolbar`), as well as single-tap (select node), double-tap (open properties), and canvas pan/zoom interactions.

**Why this priority**: Nodes on the canvas already support single-tap, double-tap, fold/unfold indicator tap, and long-press bottom toolbar actions. Drag and drop must feel responsive and predictable while preserving all existing interactions without prematurely showing the toolbar while the finger is still held down.

**Independent Test**: Can be tested by long-pressing and moving a `MindMapNodeCard` to verify visual elevation, drop zone indicators (Top/Bottom insertion indicator vs Center child-attachment highlight), and that `NodeBottomToolbar` never appears during the hold or drag, performing a stationary long-press and releasing to verify `NodeBottomToolbar` opens only upon finger release, and performing single-tap and double-tap actions to confirm no regression.

**Acceptance Scenarios**:

1. **Given** child nodes are displayed on `MindMapCanvasScreen`, **When** the user long-presses on a `MindMapNodeCard` and begins moving their finger, **Then** the card enters a "ready to drag" elevated state at the long-press threshold, follows the user's finger without opening `NodeBottomToolbar`, and highlights the active drop zone on any hovered target node `T` (top/bottom zone for sibling insertion before/after `T`, or center zone to attach as a child of `T`) without mutating the underlying mindmap model until release.
2. **Given** nodes are displayed on `MindMapCanvasScreen`, **When** the user long-presses on a `MindMapNodeCard` and releases their finger without moving beyond the drag threshold, **Then** `NodeBottomToolbar` opens upon finger release (and NOT while the finger is still pressed down) without modifying the node hierarchy or order.
3. **Given** nodes are displayed on `MindMapCanvasScreen`, **When** the user single-taps or double-taps on a `MindMapNodeCard`, **Then** the existing selection or properties inspector behavior triggers normally without starting an unintended drag.
4. **Given** a drag operation is in progress, **When** the user returns the node to its original slot or drops it in an empty/invalid area (or cancels the gesture), **Then** the node hierarchy and order remain unchanged and the mindmap model is never modified or marked as having unsaved changes.

---

### User Story 3 - Cleanup of Obsolete List View Components (Priority: P3)

As a maintainer of Droidplane, since the list display mode was removed from `MainActivity`, I want obsolete list-only UI files (`NodeList.kt`, `MindMapScreen.kt`, `NodeListDragState.kt`, and their list-only tests) removed so the codebase does not retain dead UI components.

**Why this priority**: Keeps the `:app` presentation layer clean and aligned with Principle V (Simplicity and YAGNI).

**Independent Test**: Verify that `NodeList.kt` and `MindMapScreen.kt` (and associated obsolete test files) are deleted and the project compiles and passes all unit, lint, and UI tests cleanly.

**Acceptance Scenarios**:

1. **Given** `MainActivity` exclusively uses `MindMapCanvasScreen`, **When** unused list view files (`NodeList.kt`, `MindMapScreen.kt`, `NodeListDragState.kt`, `NodeListTest.kt`, `NodeListDragStateTest.kt`) are removed, **Then** no broken references remain and all tests pass.

---

### Edge Cases

- **Root node and cycle prevention (self/descendant drop)**: Long-pressing and releasing the root node opens `NodeBottomToolbar` upon finger release, and dragging the root node does not move it or mutate the mindmap model. Similarly, dropping any non-root node onto itself or onto one of its own descendants is rejected as a no-op without mutating the mindmap model or marking it as having unsaved changes.
- **Nodes with subtrees and rich content**: Reordering or re-parenting a node that contains its own nested descendants, clouds, connectors, notes, icons, hyperlinks, or formatting must preserve the entire subtree and all attached attributes intact.
- **Drag interrupted by system event**: If a drag gesture is interrupted (e.g., incoming call, screen rotation, app backgrounded, or gesture cancellation), the visual drag state resets and the dragged node snaps back to its original position without mutating the mindmap model or opening `NodeBottomToolbar`.
- **Canvas pan/zoom coexistence**: Dragging a node after a long-press on `MindMapNodeCard` must move the node without simultaneously panning the background `MindMapCanvasScreen`, while dragging on the canvas background (or pinch-to-zoom) continues to pan/zoom the canvas normally.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow users to reorder any non-root node among its siblings OR re-parent it onto another target node (including switching between left and right branches of the root node) on `MindMapCanvasScreen` (`MindMapNodeCard`) via a 2D long-press drag-and-drop gesture. Dropping a node onto itself or onto any of its own descendants MUST be rejected (no-op) to prevent cycles in the mindmap tree.
- **FR-002**: Long-pressing on a `MindMapNodeCard` MUST NOT open `NodeBottomToolbar` while the finger is still pressed down; instead, holding past the long-press threshold MUST place the node into a "ready to drag" elevated state. If the user moves their finger beyond the touch slop threshold, active dragging MUST begin without showing `NodeBottomToolbar`; if the user releases their finger without moving beyond the threshold, `onNodeLongPress` (`NodeBottomToolbar`) MUST trigger on finger release. Single-tap (`onNodeClick`), double-tap (`onNodeDoubleClick`), and fold-indicator tap (`onToggleCollapse`) MUST remain unaffected.
- **FR-003**: While a node is being dragged on `MindMapCanvasScreen`, the dragged card MUST appear visually elevated and follow the user's 2D pointer movement (accounting for canvas `zoom`), using target card zones (`Top` / `Center` / `Bottom`, Freeplane style) to disambiguate drop actions:
  - Hovering the **Center** zone of a valid target node `T` MUST highlight `T` and indicate attaching the dragged node as a **child** at the end of `T`'s children (for the root node, the left or right half of the root node determines `position = "left"` or `"right"`).
  - Hovering the **Top** or **Bottom** zone of a valid non-root target node `T` (or the vertical gap between sibling nodes) MUST display a top or bottom insertion indicator and indicate inserting the dragged node as a **sibling** immediately before (`Top`) or after (`Bottom`) `T` under `T`'s parent (matching `T`'s branch side if `T` is a direct child of the root node).
- **FR-004**: Intermediate visual indicators during an active drag MUST be visual-only in the canvas UI; the system MUST commit the new sibling order or new parent/branch side to the underlying mindmap data model and mark the mindmap document as having unsaved changes once upon drop (finger release) when the final placement differs from the starting placement.
- **FR-005**: Dropping a node back into its original position, in an empty area with no valid target change, onto itself/its descendants, or cancelling the drag gesture MUST reset the visual drag state without modifying the mindmap data model or marking the document as having unsaved changes.
- **FR-006**: Reordering or re-parenting a node MUST preserve its unique identity, all attributes (text, notes, icons, links, clouds, connectors, timestamps, folding state), and its entire descendant subtree. When a node becomes a direct child of the root node (or switches sides of the root node), its `position` attribute MUST be set to `"left"` or `"right"` matching the target side of the root node; when re-parented under a non-root node, `position` MUST be cleared (`null`) per Freeplane format conventions. If the target parent node was folded (`folded == "true"`), attaching a child node onto it MUST unfold it (`folded = null`) so the moved node is visible.
- **FR-007**: When dragging a node near the edge of the `MindMapCanvasScreen` viewport, the canvas MUST automatically pan so the user can drop the node onto off-screen target nodes or sibling positions.
- **FR-008**: When the mindmap is saved, the new hierarchy, branch side, and sibling order produced by drag-and-drop on the canvas MUST be persisted in the underlying `.mm` mindmap file format so that reloading the file restores the exact structure.
- **FR-009**: Unused list-view components (`NodeList.kt`, `MindMapScreen.kt`, and list-only drag/test files) MUST be removed from the codebase.

### Key Entities

- **Dragged Node**: Any non-root mindmap `Node` selected and moved by the user across `MindMapCanvasScreen`, along with its entire descendant subtree.
- **Target Node (`T`)**: Another node on the canvas (excluding the dragged node itself and any of its descendants) whose bounds are hovered during the drag.
- **Drop Zone (`Top` / `Center` / `Bottom`)**: The active region on Target Node `T` determining whether the dragged node is inserted as a sibling before `T` (`Top`), attached as a child of `T` (`Center`, with left/right side detection on the root node), or inserted as a sibling after `T` (`Bottom`).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can reorder a node among siblings or re-parent it onto another node on the canvas in a single continuous gesture taking under 3 seconds.
- **SC-002**: 100% of reordered or re-parented nodes retain their full descendant subtrees, notes, icons, clouds, connectors, and links without data loss, cycle creation, or duplication.
- **SC-003**: 100% of existing `MindMapNodeCard` and `MindMapCanvasScreen` gestures (single-tap selection, double-tap properties, stationary long-press-and-release bottom toolbar, fold/unfold toggle, and canvas pan/zoom) continue to function without conflict or premature toolbar display during long-press hold/drag.
- **SC-004**: Mindmap files saved after reordering or re-parenting nodes on the canvas reload with the exact updated hierarchy, branch sides, and child sequence 100% of the time.

## Assumptions

- Drag-and-drop on `MindMapCanvasScreen` applies to any non-root node and supports both sibling reordering (Top/Bottom zones of a non-root target node) and re-parenting (Center zone of any valid target node, with left/right branch side assignment when dropping onto the root node).
- Holding a `MindMapNodeCard` past the long-press threshold prepares the item for dragging without opening `NodeBottomToolbar`; releasing without moving opens `NodeBottomToolbar`, whereas moving transitions directly into 2D drag-and-drop.
- Existing mindmap persistence and unsaved-changes tracking mechanisms in `MainViewModel` and `NodeManager` are used to commit the structural update once on drop.
