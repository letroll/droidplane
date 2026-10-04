# Feature Specification: Delete Mindmap Nodes

**Feature Branch**: `002-delete-nodes`

**Created**: 2026-10-04

**Status**: Draft

**Input**: User description: "j'aimerais à présent pouvoir supprimer des nodes"

## User Scenarios & Testing *(mandatory)*

<!--
  IMPORTANT: User stories should be PRIORITIZED as user journeys ordered by importance.
  Each user story/journey must be INDEPENDENTLY TESTABLE - meaning if you implement just ONE of them,
  you should still have a viable MVP (Minimum Viable Product) that delivers value.

  Assign priorities (P1, P2, P3, etc.) to each story, where P1 is the most critical.
  Think of each story as a standalone slice of functionality that can be:
  - Developed independently
  - Tested independently
  - Deployed independently
  - Demonstrated to users independently
-->

### User Story 1 - Delete Mindmap Nodes (Priority: P1)

As a user managing a mindmap, I want to delete nodes (and their descendants) so that I can remove unwanted ideas or restructure my mindmap.

**Why this priority**: Core content management capability - users need to remove nodes as part of editing workflows.

**Independent Test**: Can be tested by creating a node, deleting it, and verifying it disappears from the hierarchy.

**Acceptance Scenarios**:

1. **Given** I have a node with children visible, **When** I delete that node, **Then** the node and all its descendants are removed from the view and hierarchy.
2. **Given** I am viewing a child node, **When** I delete it, **Then** the view displays the parent node's children list with no specific node selected, and the deleted node is no longer listed.

---

### User Story 2 - Delete Confirmation Flow (Priority: P2)

As a user, I want to be prompted with a clear confirmation dialog showing the node title and descendant count before deleting a node with children, so that I avoid accidental data loss.

**Why this priority**: Safety feature to prevent unintended deletions, especially of large subtrees.

**Independent Test**: Verify confirmation dialog appears when deleting a node with children; verify cancel dismisses without deletion; verify "Delete" confirms and executes deletion.

**Acceptance Scenarios**:

1. **Given** a node with 3 children is selected, **When** I choose "Delete" from context menu, **Then** a dialog shows "Delete [Title] and 3 descendants? This cannot be undone." with Delete/Cancel buttons.
2. **Given** the confirmation dialog is shown, **When** I press "Cancel", **Then** the dialog closes and the node remains unchanged.
3. **Given** the confirmation dialog is shown, **When** I press "Delete", **Then** the node and all descendants are deleted.

---

### User Story 3 - UI Integration & Accessibility (Priority: P3)

As a user, I want the delete option to be discoverable in the node context menu, accessible via keyboard navigation, and announced by screen readers, so that all users can delete nodes regardless of input method.

**Why this priority**: Inclusive design and consistent UX with existing context menu actions (Edit, Copy, Add Child).

**Independent Test**: Verify Delete appears in context menu for non-root nodes; verify keyboard navigation to Delete option; verify TalkBack/VoiceOver announces "Delete node [Title]".

**Acceptance Scenarios**:

1. **Given** a non-root node is focused, **When** I open the context menu, **Then** "Delete" appears as the last option before "Cancel" with a trash icon.
2. **Given** context menu is open, **When** I navigate with arrow keys, **Then** I can reach "Delete" and activate with Enter.
3. **Given** TalkBack is enabled, **When** I focus "Delete", **Then** it announces "Delete node [Title]" with action hint.

---


### Edge Cases

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right edge cases.
-->

- **Root Node**: Deletion of root node should be prevented to maintain document integrity.
- **Node with Many Descendants**: Large subtrees must be deleted efficiently without blocking UI.
- **Undo/Cancel**: Cancellation should not delete the node.
- **Undo After Deletion**: Deleted nodes and their descendants can be restored via undo (Ctrl+Z / menu) within the current session.
- **Link Cleanup on Deletion**: All link and arrow link references to/from the deleted node are automatically removed to prevent dangling references.

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: System MUST allow users to delete the currently selected node from the mindmap.
- **FR-002**: System MUST delete all descendant nodes when deleting a parent node (recursive deletion).
- **FR-003**: System MUST prevent deletion of the root node.
- **FR-004**: System MUST update the mindmap state immediately in memory and persist changes on next explicit save. If the user attempts to close the mindmap or exit the app while unsaved deletions exist, the system MUST prompt the user to confirm discarding or saving changes.
- **FR-005**: System MUST prompt for confirmation before deleting a node that has children, showing the node title and number of descendants with a warning that the action cannot be undone. Cancellation dismisses the dialog with no action taken.
- **FR-006**: System MUST support undo of node deletion within the current session via Ctrl+Z or menu action, restoring the deleted subtree and selection.

> **Session Definition**: "Current session" = from mindmap load until mindmap is closed or app process terminates. Undo stack is cleared when a new mindmap is loaded or app is restarted.
- **FR-007**: After deletion, the view MUST display the parent node's children list with no specific node selected (user can then navigate).
- **FR-008**: When deleting a node, the system MUST clean up all associated link references: remove the node's external link, remove it from other nodes' arrow link destination/incoming lists, and clear all visual connection data.



### Key Entities *(include if feature involves data)*

- **Node**: Individual mindmap node that can be deleted (with cascade to descendants)
- **Mindmap Document**: Contains the node hierarchy being modified



## Assumptions

- Undo stack persists until mindmap close or app restart; cleared on new mindmap load
- Deletion is available from node context menu (consistent with existing UX patterns)
- Confirmation dialog is shown when deleting nodes with children to prevent accidental data loss
- Changes are persisted only when user saves the mindmap (existing save workflow)
## Clarifications

### Session 2026-10-04

- Q: When a user attempts to delete a node with children, what exactly should the confirmation dialog display and what happens if the user cancels? → A: Show dialog with "Delete [Node Title] and X descendants? This cannot be undone." Buttons: "Delete" / "Cancel". Cancel dismisses with no action.
- Q: Should the app support an "Undo" action after node deletion, and if so, what is the time window or scope for undo? → A: Support full undo (Ctrl+Z or menu action) within the current session, restoring deleted subtree and selection.
- Q: After a node is deleted, what should be the new active/selected node - the parent of the deleted node, or the next sibling, or something else? → A: Stay on the parent view but keep no specific node selected (show parent's children list).
- Q: User Stories 2 and 3 in the spec are duplicate placeholders identical to US1. Should they be removed, replaced with distinct stories, or kept? → A: Replace US2 with "Delete Confirmation Flow" (P2) and US3 with "UI Integration & Accessibility" (P3) with proper acceptance criteria.
- Q: When a node with external links, internal links, or arrow links is deleted, should those links be automatically cleaned up, or should they remain as dangling references? → A: Clean up all link references on deletion: remove deleted node's link, remove it from other nodes' arrow link lists, and clear connections.
## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Node deletion completes in under 100ms even for subtrees with 100+ nodes.

> **Performance Note**: "Under 100ms" measured on mid-range Android device (e.g., Snapdragon 7-series equivalent) with 200+ node subtree. Tested via automated benchmark in NodeManagerTest.
- **SC-002**: 100% of deleted nodes and their descendants are removed from the hierarchy and indexes.
- **SC-003**: Root node cannot be deleted.
