# Tasks: Delete Mindmap Nodes

**Input**: Design documents from `/specs/002-delete-nodes/`

**Prerequisites**: plan.md (required), spec.md (required for user stories)

**Tests**: Tests are included per TDD approach per Constitution III.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: US1, US2, or US3
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create project structure per implementation plan
- [X] T002 Initialize Kotlin/Android project with dependencies (already configured)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story

**Checkpoint**: Foundation ready - no new foundational tasks needed (NodeManager.deleteNode already exists)

---

## Phase 3: User Story 1 - Delete Mindmap Nodes (Priority: P1) 🎯 MVP

**Goal**: Users can delete nodes and their descendants from the mindmap.

**Independent Test**: Delete a node with children and verify all are removed.

### Tests for User Story 1 (TDD - Constitution III)

- [X] T003 [P] [US1] Unit test for recursive deletion in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt
- [X] T004 [P] [US1] Unit test for link/arrow-link cleanup on deletion in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt
- [X] T005 [P] [US1] Unit test for DeleteSnapshot creation and restore in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt

### Implementation for User Story 1

- [X] T006 [US1] Verify and enhance NodeManager.deleteNode() for recursive deletion and link cleanup per contracts/node-manager-contract.md (data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt)
- [X] T007 [US1] Add DeleteSnapshot data class and createDeleteSnapshot() method in NodeManager (data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt)
- [X] T008 [US1] Add restoreSubtree() method in NodeManager for undo support (data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt)
- [X] T009 [US1] Add delete action to ContextMenuAction in app/src/main/java/fr/julien/quievreux/droidplane2/model/ContextMenuDropDownItem.kt
- [X] T010 [US1] Add delete string resource in app/src/main/res/values/strings.xml
- [X] T011 [US1] Implement onDeleteNode(), onConfirmDelete(), onCancelDelete(), onUndoDelete(), canUndoDelete in MainViewModel (app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt)
- [X] T012 [US1] Add DeleteConfirmation variant to DialogUiState in MainUiState (app/src/main/java/fr/julien/quievreux/droidplane2/MainUiState.kt)
- [X] T013 [US1] Wire delete action in node context menu handling in MainViewModel.onNodeContextMenuClick() (app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt)
- [X] T014 [US1] Handle UI navigation after deletion (show parent's children list, no selection) per FR-007 (app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt)
- [X] T015 [US1] Prevent deletion of root node (FR-003) in MainViewModel.onDeleteNode() (app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt)

**Checkpoint**: US1 complete - basic deletion works.

---

## Phase 4: User Story 2 - Delete Confirmation (Priority: P2)

**Goal**: Prompt user before deleting nodes with children.

**Independent Test**: Verify confirmation dialog appears when deleting a node with children; verify cancel dismisses without deletion; verify "Delete" confirms and executes deletion.

### Tests for User Story 2 (TDD - Constitution III)

- [X] T016 [P] [US2] Unit test for confirmation dialog state in MainViewModelTest (app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt)
- [X] T017 [P] [US2] Compose UI test for DeleteConfirmationDialog rendering and actions (app/src/test/java/fr/julien/quievreux/droidplane2/ui/components/DeleteConfirmationDialogTest.kt)

### Implementation for User Story 2

- [X] T018 [US2] Create DeleteConfirmationDialog composable using :core CustomDialog (app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/DeleteConfirmationDialog.kt)
- [X] T019 [US2] Integrate DeleteConfirmationDialog in MindMapScreen/NodeList when DialogUiState is DeleteConfirmation (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/MindMapScreen.kt)
- [X] T020 [US2] Verify dialog shows correct descendant count, node title, and irreversible warning per FR-005 (app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/DeleteConfirmationDialog.kt)
- [X] T021 [US2] Wire "Delete" button (error color) and "Cancel" button actions (app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/DeleteConfirmationDialog.kt)
- [X] T022 [US2] Add accessibility semantics for TalkBack/VoiceOver announcement (app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/DeleteConfirmationDialog.kt)

**Checkpoint**: US2 complete - confirmation dialog fully functional and accessible.

---

## Phase 5: User Story 3 - UI Integration & Accessibility (Priority: P3)

**Goal**: Delete option is discoverable in context menu, accessible via keyboard, and announced by screen readers.

**Independent Test**: Verify Delete appears in context menu for non-root nodes; verify keyboard navigation to Delete option; verify TalkBack announces "Delete node [Title]".

### Tests for User Story 3 (TDD - Constitution III)

- [X] T023 [P] [US3] Integration test for context menu Delete option visibility (app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt)
- [X] T024 [P] [US3] Compose UI test for keyboard navigation to Delete option (app/src/test/java/fr/julien/quievreux/droidplane2/ui/components/NodeListTest.kt)

### Implementation for User Story 3

- [X] T025 [US3] Ensure Delete option only appears for non-root nodes in NodeList context menu (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt)
- [X] T026 [US3] Add leading trash icon for Delete action in NodeList.GetLeadingIcon() (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt)
- [X] T027 [US3] Verify context menu handles DeleteNode action in onNodeContextMenuClick (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt)
- [X] T028 [US3] Add accessibility role/description for Delete menu item (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt)

**Checkpoint**: All user stories complete.

---

## Phase 6: Polish & Cross-Cutting Concerns

- [X] T029 Run all unit tests (./gradlew test)
- [X] T030 Run quickstart.md validation scenarios (Scenarios 1-7)
- [X] T031 Code cleanup and verification against constitution
- [X] T032 [P] Add additional unit tests for edge cases in NodeManagerTest (data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt)
- [X] T033 [P] Add Compose UI tests for Delete flow in NodeListTest (app/src/test/java/fr/julien/quievreux/droidplane2/ui/components/NodeListTest.kt)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3+)**: All depend on Foundational phase completion
  - User stories can then proceed in parallel (if staffed)
  - Or sequentially in priority order (P1 → P2 → P3)
- **Polish (Final Phase)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P2)**: Can start after Foundational (Phase 2) - Integrates with US1 but independently testable
- **User Story 3 (P3)**: Can start after Foundational (Phase 2) - Integrates with US1/US2 but independently testable

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- Models before services
- Services before endpoints
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- All Setup tasks marked [P] can run in parallel
- All Foundational tasks marked [P] can run in parallel (within Phase 2)
- Once Foundational phase completes, all user stories can start in parallel (if team capacity allows)
- All tests for a user story marked [P] can run in parallel
- Models within a story marked [P] can run in parallel
- Different user stories can be worked on in parallel by different team members

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Unit test for recursive deletion in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt"
Task: "Unit test for link cleanup in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt"
Task: "Unit test for DeleteSnapshot in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt"

# Launch implementation tasks that touch different files:
Task: "Verify NodeManager.deleteNode() in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt"
Task: "Add DeleteSnapshot in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt"
Task: "Add delete action to ContextMenuAction in app/src/main/java/fr/julien/quievreux/droidplane2/model/ContextMenuDropDownItem.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: Test User Story 1 independently (Scenario 1, 3, 4, 5, 6 from quickstart.md)
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1
   - Developer B: User Story 2
   - Developer C: User Story 3
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
---

## Phase 7: Convergence - Missing Critical Features

**Purpose**: Address gaps found during convergence analysis between spec/plan/tasks and implementation

### CRITICAL - Undo Functionality (FR-006)

- [X] T034 [US1] Add DeleteSnapshot data class in data/src/main/java/fr/julien/quievreux/droidplane2/data/model/DeleteSnapshot.kt per data-model.md
- [X] T035 [US1] Implement NodeManager.createDeleteSnapshot(nodeId) to capture deleted subtree for undo (data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt)
- [X] T036 [US1] Implement NodeManager.restoreSubtree(snapshot) for undo support (data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt)
- [X] T037 [US1] Add undo stack (MutableList<DeleteSnapshot>) and canUndoDelete property to MainViewModel (app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt)
- [X] T038 [US1] Implement onUndoDelete() in MainViewModel to restore last deleted subtree (app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt)
- [X] T039 [US1] Add Ctrl+Z handler in MainActivity for undo (app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt)
- [X] T040 [US1] Add undo menu action in AppTopBar when canUndoDelete is true (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt)

### HIGH - Root Node Protection & Unsaved Changes (FR-003, FR-004)

- [X] T041 [US1] Add root node protection in NodeManager.deleteNode() - return false for root node (data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt)
- [X] T042 [US1] Add unsaved changes tracking (dirty flag) in MainViewModel (app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt)
- [X] T043 [US1] Add exit confirmation dialog for unsaved changes in MainActivity (app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt)

### HIGH - Link Cleanup (FR-008)

- [X] T044 [US1] Clean up Node.link (external link) on deletion in NodeManager.deleteNode() (data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt)

### MEDIUM - Accessibility & Icon (US3)

- [X] T045 [US3] Add trash/delete icon for Delete action in NodeList.GetLeadingIcon() (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt)
- [X] T046 [US3] Add accessibility contentDescription for Delete menu item in NodeList (app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt)

### MEDIUM - DeleteConfirmationDialog Accessibility

- [X] T047 [US2] Add accessibility semantics to DeleteConfirmationDialog for TalkBack/VoiceOver (app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/DeleteConfirmationDialog.kt)

## Phase 8: Convergence

- [X] T048 Configure Java 21 toolchain and target JVM 21 across module build scripts per Constitution Technical Constraints (contradicts)
- [X] T049 Implement Compose UI tests for DeleteConfirmationDialog and NodeList per Constitution III, US2, and US3 (missing)
- [X] T050 Enhance link cleanup in NodeManager.deleteNode() to remove dangling connectors and update _allNodes hierarchy per FR-008 and Constitution IV (partial)
- [X] T051 Wire ExitConfirmationDialog in MainActivity on exit with unsaved changes per FR-004 and T043 (partial)
- [X] T052 Add unit tests for link/connector cleanup, root node deletion protection, and deletion benchmark in NodeManagerTest per Constitution III, T004, T041, and SC-001 (missing)
- [X] T053 Clear active selection (selectedNodeId = null) after node deletion in MainViewModel.confirmDelete() per FR-007 and US1/AC2 (contradicts)
- [X] T054 Hide Delete context menu option for root node in MindMapNodeCard per FR-003, SC-003, and US3 (partial)
- [X] T055 Replace Minus icon with Trash/Delete icon in NodeList.GetLeadingIcon() per US3/AC1, T026, and T045 (partial)

## Phase 9: Convergence - Remaining Gaps

### CRITICAL - Constitution III: Test-Driven Verification (NON-NEGOTIABLE)

- [X] T056 [US2] Create Compose UI test for DeleteConfirmationDialog rendering, actions, and accessibility in app/src/androidTest/java/fr/julien/quievreux/droidplane2/ui/components/DeleteConfirmationDialogTest.kt per Constitution III, FR-005, US2/AC1-3
- [X] T057 [US3] Create Compose UI test for NodeList context menu Delete option visibility, keyboard navigation, and accessibility in app/src/androidTest/java/fr/julien/quievreux/droidplane2/ui/components/NodeListTest.kt per Constitution III, US3/AC1-3, FR-003

### HIGH - Accessibility (US3 - P3)

- [X] T058 [US2] Add accessibility semantics to DeleteConfirmationDialog (role, stateDescription, contentDescription for buttons) in app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/DeleteConfirmationDialog.kt per US3/AC3, FR-005, T022, T047
- [X] T059 [US3] Add accessibility contentDescription for all context menu items in NodeList.GetLeadingIcon() (not just DeleteNode) in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt per US3/AC3, T028, T046

### MEDIUM - Test Coverage & Polish

- [X] T060 [US1] Add unit test for unsaved changes tracking and exit confirmation flow in MainViewModelTest.kt per FR-004, Constitution III
- [X] T061 [US2] Add unit test for confirmation dialog state transitions (show → cancel, show → confirm → delete) in MainViewModelTest.kt per FR-005, Constitution III, T016
