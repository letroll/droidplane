# Tasks: Add Child Node to a Node in NodeList

**Input**: Design documents from `/specs/003-add-child-in-nodelist/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `.specify/memory/constitution.md`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify development environment and baseline build health

- [X] T001 Verify project build and test baseline across all modules via `./gradlew testDebugUnitTest`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core tree propagation infrastructure in `:data` module required before any UI story can attach children to non-root nodes

**⚠️ CRITICAL**: Must be completed before User Story implementation

- [X] T002 Add unit test for non-root child addition in `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt`
- [X] T003 Update `addNodeToMindmap` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to propagate updated parent nodes through ancestors to `rootNode` using `propagateUpdatedParentToRoot`

**Checkpoint**: Foundation ready — `NodeManager` supports adding children to arbitrary nodes in the tree with full bidirectional ancestor synchronization.

---

## Phase 3: User Story 1 - Add a Child Node to a Specific Listed Node (Priority: P1) 🎯 MVP

**Goal**: Allow users to open the context menu on any child node in `NodeList`, select "Add child node", enter text, and attach the child directly to that item without navigating away.

**Independent Test**: Add a child to a listed node via its context menu; verify the target node's children list contains the new node, its visual expand indicator updates in the current view, and navigating into it displays the new child.

### Tests for User Story 1

- [X] T004 [US1] Add integration test for adding child to listed node and verifying UI state in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

### Implementation for User Story 1

- [X] T005 [US1] Update `addNode` signature and implementation in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt` to accept `parentNode: Node?` and refresh `nodeCurrentlyDisplayed` when adding to a child node
- [X] T006 [US1] Update `DialogType.AddChildNode` handling in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt` to pass `dialog.parentNode` to `viewModel.addNode`
- [X] T007 [US1] Verify and ensure `ToggleIcon` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt` renders when a listed node gains child nodes

**Checkpoint**: User Story 1 fully functional — users can add children directly to any listed node in `NodeList`.

---

## Phase 4: User Story 2 - FAB vs Item Context Menu Distinction (Priority: P2)

**Goal**: Guarantee clear separation between top-level FAB (adds sibling to current view) and item context menu (adds child to selected item).

**Independent Test**: Use FAB to add a node (verifying it attaches to screen parent) and use item context menu to add a node (verifying it attaches to target item).

### Tests for User Story 2

- [X] T008 [US2] Add unit test in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying FAB targeting screen node vs context menu targeting list item node

### Implementation for User Story 2

- [X] T009 [US2] Verify and ensure Floating Action Button wiring in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt` explicitly passes `nodeCurrentlyDisplayed` to `DialogType.AddChildNode`

**Checkpoint**: User Stories 1 and 2 functional — both addition entry points correctly target their intended parents.

---

## Phase 5: User Story 3 - Input Validation and Cancellation (Priority: P3)

**Goal**: Ensure empty or whitespace-only submissions are rejected and dialog dismissal causes no mutation or side-effects.

**Independent Test**: Attempt to confirm blank text or cancel dialog; verify target node and mindmap tree remain unaltered.

### Tests for User Story 3

- [X] T010 [US3] Add unit test in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying blank/whitespace input rejection for targeted child node addition

### Implementation for User Story 3

- [X] T011 [US3] Add blank/whitespace validation check in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt` before initiating coroutine dispatch and loading state

**Checkpoint**: All user stories complete and protected against invalid inputs.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates, full test verification, and documentation validation

- [X] T012 Run full regression test suite across all modules via `./gradlew testDebugUnitTest --rerun-tasks`
- [X] T013 Verify complete application build and APK assembly via `./gradlew assembleDebug`
- [X] T014 Execute quickstart validation scenarios from `specs/003-add-child-in-nodelist/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Independent, run first.
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS User Stories 1, 2, and 3.
- **User Story 1 (Phase 3)**: Depends on Phase 2. Core MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 3 (builds on US1 dialog plumbing).
- **User Story 3 (Phase 5)**: Depends on Phase 3.
- **Polish (Phase 6)**: Depends on all user stories being completed.

### User Story Dependencies

- **US1 (P1)**: Foundational prerequisites only.
- **US2 (P2)**: Extends US1 to verify FAB coexistence.
- **US3 (P3)**: Adds input validation guard to US1 methods.

---

## Parallel Opportunities

- In Phase 3: T004 (test) can be written before T005 and T006.
- In Phase 4: T008 (test) and T009 (wiring check) can run in parallel.
- In Phase 5: T010 (test) and T011 (validation check) can be structured in TDD sequence.
- In Phase 6: T012 and T013 can run sequentially or in parallel execution scripts.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1 (Setup) and Phase 2 (Foundational ancestor propagation in `NodeManager`).
2. Implement Phase 3 (User Story 1: `MainViewModel.addNode(newValue, parentNode)` + `MainActivity` wiring).
3. Validate User Story 1 independently with `:app:testDebugUnitTest`.

### Incremental Delivery
1. Phase 1 + 2 → Tree hierarchy propagation verified in `:data`.
2. Phase 3 → User Story 1 complete (MVP: can add child to listed node).
3. Phase 4 → User Story 2 complete (FAB vs context menu distinction confirmed).
4. Phase 5 → User Story 3 complete (Input validation and cancellation robust).
5. Phase 6 → Full regression and build verified.
