# Feature Specification: Mindmap Viewing and Node Management

**Feature Branch**: `001-mindmap-node-management`

**Created**: 2026-10-03

**Status**: Draft

**Input**: User description: "Formalize the mindmap viewing and node management specifications from project_specs.md"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Mindmap Exploration & Child Node Navigation (Priority: P1)

As a mindmap user, I want to view the current node title and an organized listing of its immediate child nodes, and tap any child to navigate deeper into the hierarchy, so that I can explore and inspect my notes naturally on a mobile screen.

**Why this priority**: Navigation is the fundamental requirement for browsing mindmaps. Without clear hierarchy navigation, no other mindmap features can be used.

**Independent Test**: Load a mindmap, view the root node and its immediate children with their styles (bold, italic) and icons, and tap a child to confirm the view transitions to that child as the new active node with its own children displayed.

**Acceptance Scenarios**:

1. **Given** a mindmap is loaded with an active root node, **When** the mindmap screen is displayed, **Then** the top navigation bar displays the application icon and the root node's title, and the main area lists each direct child node with its text, visual styles (bold/italic), associated icons, and an indicator if that child has sub-children.
2. **Given** a list of child nodes is visible, **When** the user taps a child node, **Then** the top bar title updates to the selected node's text and the listing transitions to show that selected node's direct children.
3. **Given** the user is viewing a nested child node, **When** the user triggers the "Up" navigation action, **Then** the view ascends one level to the direct parent node.
4. **Given** the user is multiple levels deep in a branch, **When** the user triggers the "Top" navigation action, **Then** the view returns immediately to the root node of the mindmap.

---

### User Story 2 - Node Search & Map Navigation (Priority: P2)

As a user with large mindmaps, I want to search across all node titles and descriptions and cycle through matches sequentially, so that I can quickly find topics without manually traversing deep branches.

**Why this priority**: Users frequently work with large mindmaps containing hundreds of nodes; rapid search is essential for discovering and locating information efficiently.

**Independent Test**: Enter search mode from the top bar, submit a keyword, observe match indicators, and tap next/previous navigation buttons to jump across matching nodes.

**Acceptance Scenarios**:

1. **Given** a user is viewing a mindmap, **When** the user taps the search icon in the top navigation bar, **Then** the toolbar enters search mode showing a search text input, a back button to exit search mode, and previous/next navigation buttons.
2. **Given** search mode is active, **When** the user enters a search term that matches nodes in the map, **Then** the application highlights the first match, navigates the active view to that node, and indicates the match position.
3. **Given** multiple search matches are active, **When** the user taps the next button, **Then** the view transitions to the subsequent matching node; tapping previous returns to the preceding match.
4. **Given** search mode is active, **When** the user taps the search back button, **Then** the toolbar exits search mode, cleans up search highlighting, and returns to standard navigation mode.

---

### User Story 3 - Node Management & Content Editing (Priority: P3)

As a user managing thoughts and ideas, I want to create new child nodes, edit existing node descriptions, copy node text, and follow linked references, so that I can maintain and update my mindmaps on the go.

**Why this priority**: Mindmap workflows require creating new ideas and refining existing ones directly within the mobile application.

**Independent Test**: Add a new child node to the current view, edit an existing node's description, copy description text to the clipboard, and verify that all modifications are preserved upon navigation and document saving.

**Acceptance Scenarios**:

1. **Given** an active node is displayed, **When** the user selects the action to add a new child node and provides text, **Then** a new child node is appended to the current node's children list and its creation timestamp is set.
2. **Given** a node is displayed, **When** the user opens the node editor, modifies the description text, and confirms, **Then** the node's updated text is immediately reflected in the active view, in the parent node's children list, and its modification timestamp is updated.
3. **Given** a node containing text or an attached link, **When** the user selects "Copy", **Then** the full description is copied to the system clipboard; when selecting a link, the external resource or target node is opened.
4. **Given** changes have been made to nodes in the active mindmap, **When** the user selects "Save" from the menu, **Then** all additions and edits are saved to persistent storage without altering unaffected nodes or formatting.

---

### Edge Cases

- **Leaf Nodes**: When a user navigates to a node that has no children, the screen MUST display an informative empty state explaining that the node has no sub-nodes and offering an action to add one.
- **Empty or Whitespace Node Names**: Creating or renaming a node with empty or blank text MUST be prevented, displaying validation feedback to the user.
- **Missing or Corrupt Files**: If a mindmap file cannot be opened or contains unreadable contents, the application MUST present a clear error notification and allow the user to select another file without crashing.
- **Search Yielding No Matches**: If a search query does not match any node in the mindmap, the interface MUST indicate zero results found and disable previous/next navigation.
- **Large Node Hierarchies**: Viewing or searching mindmaps containing over 2,000 nodes MUST maintain fluid scrolling and not freeze the user interface.
- **Unsaved Changes on Exit**: If the user attempts to close the mindmap or exit the app while unsaved modifications exist, the system MUST prompt the user to confirm discarding or saving changes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST display the current node title in the primary application bar at all times during navigation.
- **FR-002**: System MUST render each direct child node with its customized text appearance (such as bold and italic styles) and associated visual icons.
- **FR-003**: System MUST display an expandable indicator (chevron) on any child node that contains further descendant nodes.
- **FR-004**: System MUST transition the active view to display a child node's descendants when the user taps on that child.
- **FR-005**: System MUST provide hierarchical navigation controls including "Up" (navigate to immediate parent) and "Top" (navigate to root node).
- **FR-006**: System MUST provide an integrated search mode in the primary toolbar allowing case-insensitive full-text search across all node titles and descriptions.
- **FR-007**: System MUST allow cycling forward and backward through search results, dynamically updating the active view to show each matching node.
- **FR-008**: System MUST support creating new child nodes under the active node with custom descriptions.
- **FR-009**: System MUST support editing the text description of existing nodes and synchronizing the changes across all visible hierarchy levels.
- **FR-010**: System MUST automatically record and update creation and modification timestamps whenever a node is created or modified.
- **FR-011**: System MUST support copying a node's full description to the system clipboard.
- **FR-012**: System MUST support navigating to linked targets when a node contains an internal link or an external URL.
- **FR-013**: System MUST support saving mindmap document modifications persistently while preserving original document structure, unsupported attributes, and desktop compatibility.
- **FR-014**: System MUST provide a top-level menu with actions for Up, Top, Open, Save, and Help.

### Key Entities *(include if feature involves data)*

- **Mindmap Document**: Represents the entire mindmap file, including document metadata, the unique root node, and indexed access to all constituent nodes.
- **Node**: An individual concept or item within the mindmap hierarchy. Attributes include a unique identifier, description text, visual styling flags (bold, italic), associated icons, creation timestamp, last modification timestamp, optional link reference, reference to its parent node, and an ordered collection of child nodes.
- **Search Session**: Represents active user search state, including the search query string, the ordered list of matching node identifiers, and the index of the currently highlighted result.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can navigate between parent and child nodes in under 100 milliseconds across mindmaps of up to 2,000 nodes.
- **SC-002**: Search across a mindmap of 1,000+ nodes displays initial matching results in under 500 milliseconds.
- **SC-003**: 100% of node edits (text changes, added children) are immediately visible in both the current view and parent-level listings without requiring a reload or restart.
- **SC-004**: Saved mindmaps can be reopened in desktop Freeplane without error or data loss in 100% of test cases.
- **SC-005**: 95% of users can locate a specific node or add a new child node on their first attempt without instructions.
- **SC-006**: Zero data corruption occurs when saving mindmap files during interrupted or background operations.

## Assumptions

- Mindmaps adhere to the standard Freeplane hierarchical document format.
- Mobile viewing is centered on list-based hierarchical navigation rather than 2D canvas zooming, optimizing for mobile ergonomics and one-handed operation.
- Users have local storage read and write permissions to open and save mindmap files.
- Advanced desktop features not yet supported on mobile (such as embedded groovy scripts or conditional styles) will be preserved without modification during save operations.
