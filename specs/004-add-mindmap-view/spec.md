# Feature Specification: Mind Map View with View Mode Toggle

**Feature Branch**: `004-add-mindmap-view`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "ajoute une vue type mapmind similaire à ce que l'on trouve dans freeplane, avec une option dans le menu pour pouvoir passer de ce mode d'affichage à celui actuel."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - 2D Mind Map Visual Representation (Priority: P1)

As a user exploring a mindmap document, I want to view my nodes in a 2D spatial tree layout (similar to Freeplane desktop) where the root node is at the center and branches radiate outwards to child nodes connected by curved lines, so that I can visually understand the relationships and structure of my ideas at a glance.

**Why this priority**: This is the core capability requested by the user. It transforms Droidplane from a purely list-based navigator into a true visual mind mapping application compatible with Freeplane's visual metaphor.

**Independent Test**: Can be tested independently by opening any mindmap document in Mind Map view mode, verifying that nodes are laid out on a 2D spatial canvas with branches connecting parents to children, and using touch gestures to pan and zoom smoothly around the canvas.

**Acceptance Scenarios**:

1. **Given** a mindmap document is loaded, **When** displayed in Mind Map view mode, **Then** nodes are rendered on a 2D spatial canvas as visual elements connected to their parent nodes with branch lines.
2. **Given** a mindmap with a root node and multiple child nodes, **When** viewed in Mind Map mode, **Then** child nodes branch out horizontally from the root node (respecting left/right branch attributes when specified in the document), displaying each node's text clearly.
3. **Given** a mindmap canvas displayed in Mind Map mode, **When** the user drags with one finger or pinches with two fingers, **Then** the canvas pans and zooms smoothly without visual stutter or disorientation.

---

### User Story 2 - Seamless View Mode Switching via Options Menu (Priority: P1)

As a user, I want an option in the top bar menu to seamlessly toggle between the traditional hierarchical List view and the new visual Mind Map view, so that I can flexibly alternate between fast sequential list reading and spatial diagram exploration.

**Why this priority**: Essential to the user's explicit request. Allows users to switch between the existing interface and the new visual view without disrupting their workflow or risking data loss.

**Independent Test**: Can be tested by opening the top bar menu, choosing the switch view option, verifying that the display mode changes immediately while keeping the active document and node context intact, and then toggling back to confirm full reversibility.

**Acceptance Scenarios**:

1. **Given** the user is viewing a document in hierarchical List view, **When** the user opens the top bar options menu, **Then** a menu item is present indicating the ability to switch to Mind Map view (e.g., "Switch to Mind Map view").
2. **Given** the user selects the "Switch to Mind Map view" menu item, **When** the selection is confirmed, **Then** the screen switches immediately to the 2D Mind Map view, displaying the active document.
3. **Given** the user is in 2D Mind Map view, **When** the user opens the top bar options menu and selects "Switch to List view", **Then** the screen returns to the hierarchical List view.
4. **Given** the user has made unsaved changes or edits to the mindmap, **When** the user switches between view modes, **Then** all unsaved changes and document state are preserved exactly without reloading from storage.

---

### User Story 3 - Interactive Navigation & Branch Collapsing in Mind Map View (Priority: P2)

As a user navigating a complex visual mindmap, I want to select nodes and collapse or expand branches directly on the canvas, so that I can explore deep hierarchies progressively without visual clutter.

**Why this priority**: Navigating large mindmaps requires folding and unfolding branches to maintain clarity and focus on relevant topics.

**Independent Test**: Tap on a node with children to collapse its branch; verify child branches hide and a fold indicator appears. Tap the indicator to expand; verify children reappear in their correct positions.

**Acceptance Scenarios**:

1. **Given** a node on the Mind Map canvas has child nodes, **When** the user taps its fold/expand indicator, **Then** its child sub-branches are collapsed, hiding them and updating the fold indicator.
2. **Given** a collapsed node on the Mind Map canvas, **When** the user taps its fold/expand indicator, **Then** its child sub-branches re-expand and are laid out in their proper canvas positions.
3. **Given** any node on the Mind Map canvas, **When** the user taps directly on the node, **Then** the node is highlighted as the selected node.
4. **Given** a node is selected in Mind Map view, **When** the user switches to List view, **Then** the List view centers or displays that selected node as the active view context.

---

### User Story 4 - Contextual Node Actions in Mind Map View (Priority: P3)

As a user interacting with a selected node in Mind Map view, I want to access standard node operations (edit text, add child node, delete node, view details) directly from the visual canvas, so that I can edit my mindmap without being forced to switch back to list view.

**Why this priority**: Completes the authoring loop in the visual view, ensuring feature parity with the list view for core editing workflows.

**Independent Test**: Long-press or trigger the context menu on any node in Mind Map view; verify that actions such as "Edit text", "Add child node", and "Delete" are accessible, functional, and immediately update the canvas.

**Acceptance Scenarios**:

1. **Given** a node is displayed on the Mind Map canvas, **When** the user performs a long-press or activates the node's action trigger, **Then** a context menu or action options matching standard node operations are presented.
2. **Given** the user chooses "Edit text" from the node context menu, **When** the user saves the updated text, **Then** the node on the canvas immediately reflects the updated text and the document is marked as modified.
3. **Given** the user chooses "Add child node" from the node context menu, **When** the user confirms the new node text, **Then** a new child branch appears connected to the target node on the canvas.

---

### Edge Cases

- **Single Root Node Map**: Mindmaps containing only a single root node with no children must render cleanly at the center of the canvas without layout errors or division-by-zero bounds.
- **Very Large Mindmaps**: Maps with hundreds of nodes must remain responsive during pan and zoom gestures; off-screen nodes or branches should not degrade gesture tracking performance.
- **Deep Nesting Hierarchy**: Branches with deep nesting levels (10+ generations) must calculate horizontal offsets cleanly without overlapping sibling subtrees.
- **Long Multiline Text**: Nodes with extensive descriptions or multi-line text must constrain their visual width and wrap text gracefully to prevent excessive horizontal stretching.
- **Nodes with Icons or Formats**: Nodes containing Freeplane icons or formatting (bold, italic) must present these visual attributes alongside the text label on the canvas.
- **Orientation & Screen Resizing**: When rotating the device between portrait and landscape, the canvas viewport must retain the active view mode and re-center gracefully on the selected or root node.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a 2D spatial Mind Map visualization mode that presents mindmap nodes connected by directional branch lines.
- **FR-002**: In Mind Map view mode, the system MUST render the root node as the primary origin and arrange child nodes horizontally outwards (distributing branches to the left and right or according to Freeplane position attributes).
- **FR-003**: The Mind Map view MUST support continuous 2D pan (drag) and zoom (pinch-to-zoom) gestures across the canvas.
- **FR-004**: The system MUST provide a menu item in the top bar options menu allowing the user to toggle between the hierarchical List view and the 2D Mind Map view.
- **FR-005**: The menu item label MUST clearly reflect the target view mode (e.g., displaying "Switch to Mind Map view" when currently in List view, and "Switch to List view" when currently in Mind Map view).
- **FR-006**: When switching between display modes, the system MUST preserve document data integrity, active document context, unsaved modifications, and the currently selected node.
- **FR-007**: In Mind Map view mode, the system MUST allow users to collapse and expand branches that have child nodes.
- **FR-008**: In Mind Map view mode, the system MUST allow users to select a node by tapping on it.
- **FR-009**: In Mind Map view mode, the system MUST provide access to contextual node operations (edit text, add child node, delete node, view details) that produce identical domain updates to list view operations.
- **FR-010**: The system MUST remember the user's selected display mode for the current session, so navigating within the application preserves the chosen view until explicitly changed.

### Key Entities

- **Display Mode**: An enumeration representing the active visualization strategy: `LIST` (hierarchical linear list) and `MIND_MAP` (2D spatial tree diagram).
- **Mind Map Viewport**: The 2D viewing region defining the current pan offsets (X, Y) and zoom scale factor applied to the visual canvas.
- **Visual Mind Map Node**: The rendered representation of a domain `Node` on the 2D canvas, encapsulating position coordinates, dimensions, text content, formatting badges, selection state, and branch collapse state.
- **Branch Connector**: The visual connector (e.g. Bezier or curved line) drawn between a parent node and its child nodes on the canvas.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can toggle between List view and Mind Map view via the options menu in a single interaction taking under 1 second.
- **SC-002**: Switching between display modes achieves 100% state preservation with zero data loss and zero reload lag.
- **SC-003**: Pan and zoom interactions on the Mind Map canvas maintain smooth responsiveness (targeting 60 fps) with zero application freezes or ANRs.
- **SC-004**: 100% of node operations (edit, add child, delete) executed from the Mind Map view produce immediate, synchronized updates visible upon switching back to List view.

## Assumptions

- The default initial display mode is the 2D Mind Map view when a document is opened, providing immediate spatial visual representation, with the ability to switch to the traditional List view at any time via the options menu.
- The 2D mindmap layout arranges nodes horizontally (left-to-right and right-to-left branching from the root), matching Freeplane desktop conventions and utilizing existing node position attributes where available.
- Existing dialogs for node editing, child node creation, and deletion confirmations are reused to ensure UI consistency and reduce duplication across view modes.
- Pan and zoom gestures follow standard mobile multi-touch conventions (one finger drag to pan, two finger pinch to zoom).
