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
2. **Given** I am viewing a child node, **When** I delete it, **Then** I navigate back to its parent and the node is no longer listed.

---

### User Story 2 - [Brief Title] (Priority: P2)

As a user managing a mindmap, I want to delete nodes (and their descendants) so that I can remove unwanted ideas or restructure my mindmap.

**Why this priority**: Core content management capability - users need to remove nodes as part of editing workflows.

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

### User Story 3 - [Brief Title] (Priority: P3)

As a user managing a mindmap, I want to delete nodes (and their descendants) so that I can remove unwanted ideas or restructure my mindmap.

**Why this priority**: Core content management capability - users need to remove nodes as part of editing workflows.

**Independent Test**: [Describe how this can be tested independently]

**Acceptance Scenarios**:

1. **Given** [initial state], **When** [action], **Then** [expected outcome]

---

[Add more user stories as needed, each with an assigned priority]

### Edge Cases

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right edge cases.
-->

- **Root Node**: Deletion of root node should be prevented to maintain document integrity.
- **Node with Many Descendants**: Large subtrees must be deleted efficiently without blocking UI.
- **Undo/Cancel**: Cancellation should not delete the node.

## Requirements *(mandatory)*

<!--
  ACTION REQUIRED: The content in this section represents placeholders.
  Fill them out with the right functional requirements.
-->

### Functional Requirements

- **FR-001**: System MUST allow users to delete the currently selected node from the mindmap.
- **FR-002**: System MUST delete all descendant nodes when deleting a parent node (recursive deletion).
- **FR-003**: System MUST prevent deletion of the root node.
- **FR-004**: System MUST update the mindmap state immediately in memory and persist changes on next save.
- **FR-005**: System MUST prompt for confirmation before deleting a node that has children.

*Example of marking unclear requirements:*

- **FR-006**: System MUST authenticate users via [NEEDS CLARIFICATION: auth method not specified - email/password, SSO, OAuth?]
- **FR-007**: System MUST retain user data for [NEEDS CLARIFICATION: retention period not specified]

### Key Entities *(include if feature involves data)*

- **Node**: Individual mindmap node that can be deleted (with cascade to descendants)
- **Mindmap Document**: Contains the node hierarchy being modified



## Assumptions

- Deletion is available from node context menu (consistent with existing UX patterns)
- Confirmation dialog is shown when deleting nodes with children to prevent accidental data loss
- Changes are persisted only when user saves the mindmap (existing save workflow)


## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Node deletion completes in under 100ms even for subtrees with 100+ nodes.
- **SC-002**: 100% of deleted nodes and their descendants are removed from the hierarchy and indexes.
- **SC-003**: Root node cannot be deleted.
