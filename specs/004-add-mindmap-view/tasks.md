# Tasks: Mind Map View with View Mode Toggle

**Input**: Design documents from `/specs/004-add-mindmap-view/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `.specify/memory/constitution.md`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify development environment and baseline build health

- [X] T001 Verify project build and test baseline across all modules via `./gradlew test --quiet`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core state representation, menu strings, and ViewModel additions required before ANY user story can be implemented

**⚠️ CRITICAL**: Must be completed before User Story implementation

- [X] T002 [P] Create `DisplayMode` enum with `LIST` and `MIND_MAP` entries in `app/src/main/java/fr/julien/quievreux/droidplane2/model/DisplayMode.kt`
- [X] T003 [P] Add string resources `switch_to_mindmap_view` and `switch_to_list_view` in `app/src/main/res/values/strings.xml`
- [X] T004 Extend `MainUiState` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainUiState.kt` with `displayMode: DisplayMode = DisplayMode.LIST`, `selectedNodeId: String? = null`, and `collapsedNodeIds: Set<String> = emptySet()`
- [X] T005 Implement display mode and selection management methods (`toggleDisplayMode()`, `setDisplayMode()`, `selectNode()`, `toggleNodeCollapse()`, `clearNodeSelection()`, and `getRootNode()`) in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`

**Checkpoint**: Foundation ready — `MainUiState` and `MainViewModel` support display mode tracking, node selection, and branch folding state.

---

## Phase 3: User Story 1 - 2D Mind Map Visual Representation (Priority: P1) 🎯 MVP

**Goal**: Render nodes on a 2D spatial canvas where the root node is at center and child branches radiate horizontally (left/right) connected by smooth cubic Bezier curves, with fluid pan and zoom gestures.

**Independent Test**: Open a mindmap document in Mind Map mode; verify root is centered, branches extend outward with curved connectors, and dragging/pinching pans and zooms the canvas.

### Tests for User Story 1

- [X] T006 [P] [US1] Create unit tests for layout calculation, dynamic content-based node dimension estimation, and left/right side partitioning in `app/src/test/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngineTest.kt`

### Implementation for User Story 1

- [X] T007 [US1] Implement `MindMapLayoutEngine` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngine.kt` to calculate 2D coordinates `(x, y)`, bounds, side directions, branch connectors, and content-measured node dimensions
- [X] T008 [P] [US1] Implement `MindMapNodeCard` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt` with dynamic auto-resizing based on text content and wrapping, style formatting (bold, italic), selection highlight, and `@Preview` functions (dark/light)
- [X] T009 [P] [US1] Implement `MindMapBranchDrawer` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapBranchDrawer.kt` drawing cubic Bezier curves (`Path.cubicTo`) connecting parent and child nodes with horizontal tangents
- [X] T010 [US1] Implement `MindMapCanvasScreen` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasScreen.kt` integrating layout engine, branch drawing, node placement, pan/zoom gesture handling (`detectTransformGestures`), and `@Preview` functions

**Checkpoint**: User Story 1 fully functional — nodes render on an interactive 2D canvas with Freeplane-style branching and fluid pan/zoom.

---

## Phase 4: User Story 2 - Seamless View Mode Switching via Options Menu (Priority: P1)

**Goal**: Allow users to toggle between hierarchical List view and 2D Mind Map view from the top bar options menu with instant switching and 100% state preservation.

**Independent Test**: Tap the options menu; verify the menu item displays "Mind Map View" when in List view and "List View" when in Mind Map view; toggle back and forth and verify active document and edits are preserved.

### Tests for User Story 2

- [X] T011 [P] [US2] Add unit tests for `toggleDisplayMode()` and state preservation in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

### Implementation for User Story 2

- [X] T012 [US2] Add `AppTopBarAction.ToggleDisplayMode` and the dynamic view mode menu item in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt`
- [X] T013 [US2] Wire `ToggleDisplayMode` action in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt` to `viewModel.toggleDisplayMode()`
- [X] T014 [US2] Route content display between `LazyColumn(nodeList)` and `MindMapCanvasScreen` based on `state.value.displayMode` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: User Stories 1 and 2 functional — users can seamlessly switch between List view and Mind Map view via the menu with full state preservation.

---

## Phase 5: User Story 3 - Interactive Navigation & Branch Collapsing in Mind Map View (Priority: P2)

**Goal**: Allow users to tap nodes to select them and collapse/expand branches using a fold indicator `[-]`/`[+]`, updating layout dynamically.

**Independent Test**: Tap a node to select it (observe visual highlight); tap fold indicator to collapse children (verify children hide and layout rebalances); expand branch and verify children reappear; switch to List view and verify list view focuses on the selected node.

### Tests for User Story 3

- [X] T015 [P] [US3] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngineTest.kt` verifying collapsed subtrees are excluded from layout calculation and bounds adjust accordingly
- [X] T016 [P] [US3] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` for `selectNode`, `toggleNodeCollapse`, and synchronizing `nodeCurrentlyDisplayed` when transitioning to `DisplayMode.LIST`

### Implementation for User Story 3

- [X] T017 [US3] Add fold indicator toggle (`[-]`/`[+]`) to `MindMapNodeCard` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt` to emit collapse/expand events
- [X] T018 [US3] Wire node click and fold indicator click events in `MindMapCanvasScreen` to `onNodeSelect` and `onNodeToggleCollapse` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasScreen.kt`
- [X] T019 [US3] Update `MainViewModel.toggleDisplayMode()` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt` to focus `nodeCurrentlyDisplayed` on `selectedNodeId` when switching from `MIND_MAP` to `LIST`

**Checkpoint**: User Stories 1, 2, and 3 functional — canvas supports branch folding, node selection, and cross-view focus synchronization.

---

## Phase 6: User Story 4 - Contextual Node Actions in Mind Map View (Priority: P3)

**Goal**: Provide access to standard node operations (edit description, add child node, delete node, copy text) directly from Mind Map view via long press and FAB.

**Independent Test**: Long-press a node on the canvas to open its context menu, select "Edit" or "Add child", confirm, and verify immediate canvas update and unsaved changes tracking.

### Tests for User Story 4

- [X] T020 [P] [US4] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying node addition and editing while in `DisplayMode.MIND_MAP`

### Implementation for User Story 4

- [X] T021 [US4] Implement node long-press detection and context menu dropdown in `MindMapNodeCard.kt` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt` (supporting Edit, Add Child, Delete, Copy)
- [X] T022 [US4] Update FAB action in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt` to target `selectedNodeId` (or root node) when in `DisplayMode.MIND_MAP`

**Checkpoint**: All user stories functional — complete authoring parity achieved between Mind Map view and List view.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates, full test verification, and documentation validation

- [X] T023 [P] Add Compose `@Preview` functions and dark/light theme validation for `MindMapCanvasScreen` and `MindMapNodeCard` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/`
- [X] T024 Run full regression test suite across all modules via `./gradlew test --rerun-tasks`
- [X] T025 Verify complete application build and APK assembly via `./gradlew assembleDebug`
- [X] T026 Execute quickstart validation scenarios from `specs/004-add-mindmap-view/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Independent, run first.
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2. Core MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 2 & Phase 3 (enables switching to canvas).
- **User Story 3 (Phase 5)**: Depends on Phase 3 & Phase 4 (builds on canvas and view switching).
- **User Story 4 (Phase 6)**: Depends on Phase 5 (builds on node selection).
- **Polish (Phase 7)**: Depends on all user stories being completed.

### User Story Dependencies

- **US1 (P1)**: Foundational prerequisites only.
- **US2 (P1)**: Depends on US1 (requires `MindMapCanvasScreen` to route content).
- **US3 (P2)**: Extends US1 and US2 with branch folding and selection synchronization.
- **US4 (P3)**: Extends US3 with contextual node actions on selected nodes.

---

## Parallel Opportunities

- Within Phase 2: `T002` (DisplayMode.kt) and `T003` (strings.xml) can run in parallel.
- Within Phase 3: `T006` (tests), `T008` (MindMapNodeCard.kt), and `T009` (MindMapBranchDrawer.kt) can run in parallel before `T010` (MindMapCanvasScreen.kt).
- Within Phase 4: `T011` (ViewModelTest.kt) can run in parallel with `T012` (AppTopBar.kt).
- Within Phase 5: `T015` (LayoutEngineTest.kt) and `T016` (ViewModelTest.kt) can run in parallel.
- Within Phase 6: `T020` (ViewModelTest.kt) can run in parallel with `T021` (MindMapNodeCard.kt).
- Within Phase 7: `T023` (Compose Previews) can run in parallel with test suite runs.

---

## Implementation Strategy

### MVP First (User Story 1 & 2)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (Canvas rendering and pan/zoom)
4. Complete Phase 4: User Story 2 (Menu toggle and routing)
5. **STOP and VALIDATE**: Verify view mode switching and canvas visualization (MVP ready!)

### Incremental Delivery

1. Add User Story 3 (Branch folding and node selection) → Validate independently.
2. Add User Story 4 (Context menu and FAB node authoring) → Validate authoring loops.
3. Complete Phase 7 (Polish, Compose Previews, regression testing, quickstart scenarios).
