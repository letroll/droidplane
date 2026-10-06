# Feature Specification: Add Child Node to a Node in NodeList

**Feature Branch**: `003-add-child-in-nodelist`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "ajoute à présent la capacité d'ajouter un node enfant à l'un des nodes de la liste d'enfant affiché dans nodelist"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Add a Child Node to a Specific Listed Node (Priority: P1)

As a user browsing a mindmap node's children list in NodeList, I want to add a child node directly to one of the listed child nodes via its context menu so that I can expand and structure deeper levels of my thoughts without having to navigate into each node first.

**Why this priority**: This is the core functionality requested by the user. It enables direct, hierarchical expansion of the mindmap from the current list view.

**Independent Test**: Can be tested by opening the context menu on any node displayed in NodeList, selecting "Add child node", typing text, confirming, and verifying that the target node now has the new child node attached to it.

**Acceptance Scenarios**:

1. **Given** a mindmap is displayed where parent node "Project" has child nodes "Design" and "Development" in the list, **When** the user opens the context menu on "Design" and selects "Add child node", enters "Wireframes", and confirms, **Then** "Wireframes" is created as a child of "Design" (not as a child of "Project").
2. **Given** a listed node receives a new child node via its context menu, **When** the user subsequently navigates into that listed node, **Then** the newly created child node is visible in its children list directly after the node creation.
3. **Given** a listed node previously had no children, **When** a child node is added to it, **Then** the listed node's visual indicator (expand/children toggle indicator) reflects that it now contains child nodes.

---

### User Story 2 - Floating Action Button vs Item Context Menu Distinction (Priority: P2)

As a user, I want a clear and consistent distinction between adding a child to the currently displayed parent node (via the main action button) and adding a child to a specific listed item (via its item context menu) so that I never accidentally add a node to the wrong parent.

**Why this priority**: Guarantees usability and prevents regression of existing node addition workflows.

**Independent Test**: Test adding a node via the main add button (adds to current view parent) and test adding a node via item context menu (adds to selected item). Verify that both target the intended parent correctly.

**Acceptance Scenarios**:

1. **Given** the user is viewing node "Topic A" with listed children "Subtopic 1" and "Subtopic 2", **When** the user clicks the Floating Action Button and adds "Subtopic 3", **Then** "Subtopic 3" is added as a child of "Topic A" and immediately appears in the active list.
2. **Given** the user is viewing node "Topic A" with listed child "Subtopic 1", **When** the user opens the context menu on "Subtopic 1" and adds "Detail 1A", **Then** "Detail 1A" is added as a child of "Subtopic 1" and "Subtopic 1" remains in the list of "Topic A".

---

### User Story 3 - Input Validation and Cancellation (Priority: P3)

As a user, I want the node creation dialog to handle empty or canceled inputs safely so that no invalid or blank nodes are added to my mindmap.

**Why this priority**: Data hygiene and error prevention for editing operations.

**Independent Test**: Attempt to submit empty or whitespace-only text, or dismiss/cancel the dialog; verify that the tree and target node are unaffected.

**Acceptance Scenarios**:

1. **Given** the "Add child node" dialog is open for a listed node, **When** the user presses Cancel or dismisses the dialog, **Then** no new node is created and the target node remains unchanged.
2. **Given** the "Add child node" dialog is open for a listed node, **When** the user submits an empty or whitespace-only string, **Then** the creation request is ignored or rejected without modifying the mindmap.

---

### Edge Cases

- **Node with existing children**: Adding a child to a listed node that already has children must append the new child to its existing child collection without reordering or losing existing children.
- **Node without prior children**: Adding a child to a leaf node must update that node's state so that child indicators and navigation into that node are immediately active.
- **Deep hierarchy preservation**: Adding a child to a listed node must keep all ancestor links up to the root node consistent and valid, maintaining document integrity for subsequent navigation and saves.
- **Concurrent search or filter state**: If an item in the list is filtered or highlighted during search, adding a child to it through its context menu must preserve the search query and result indexing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST allow the user to trigger child node creation for any specific node displayed in the NodeList via its item context menu.
- **FR-002**: When child node creation is initiated from a list item, the system MUST prompt the user for the new node text.
- **FR-003**: The system MUST create the new node as a direct child of the selected list item, rather than as a child of the currently displayed screen parent.
- **FR-004**: The system MUST update the target node's child collection and mark the document as having unsaved changes.
- **FR-005**: The system MUST maintain bidirectional tree references (`parentNode` and `childNodes`) for the target node, the newly created child, and all affected ancestors up to the root.
- **FR-006**: The system MUST update list item indicators (e.g. child count or expand icon) to reflect the target node's new child count.
- **FR-007**: The system MUST ignore or disallow blank or whitespace-only node text submissions.
- **FR-008**: The system MUST preserve existing node addition through the Floating Action Button, which adds children to the currently displayed screen parent.

### Key Entities

- **Target Node**: The existing node in the displayed list that receives the new child node.
- **New Child Node**: The newly created node containing user-entered text, unique identifiers, creation timestamp, and a reference pointing to the Target Node as its parent.
- **Parent View Node**: The node currently displayed as the active screen context, whose `childNodes` collection contains the Target Node.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can add a child node to any item in the displayed list in 3 interactions or fewer (open menu -> select add child -> confirm text).
- **SC-002**: 100% of nodes added via list item context menus are attached to the selected list item and never misattributed to the parent screen node.
- **SC-003**: Navigating into the target node immediately displays the newly created child node with zero UI glitches or reloads required.
- **SC-004**: Mindmaps saved after adding children to list items preserve the new hierarchical relationship upon reload with 100% fidelity.

## Assumptions

- The existing context menu option for adding a child node (`R.string.add_child_node`) will be retained and connected to the target list item.
- The default text entry dialog format used across the application will be reused for entering new child node text.
- Adding a child to a list item does not automatically navigate the screen into that item; the user remains on the current list view, with visual indicators updating.
