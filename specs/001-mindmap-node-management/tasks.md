# Implementation Tasks: Mindmap Viewing and Node Management

**Branch**: `001-mindmap-node-management` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md)

This document contains the complete, dependency-ordered task breakdown for implementing mindmap viewing, search traversal, and synchronized node management in Droidplane.

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, module configuration, and shared test fixtures.

- [X] T001 Verify multi-module Gradle configuration and dependencies in build.gradle.kts and settings.gradle.kts
- [X] T002 [P] Configure shared test fixtures and sample Freeplane .mm XML resources in data/src/test/resources/test_map.mm

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data layer primitives that MUST be complete before ANY user story can be implemented.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T003 [P] Verify and update domain Node model and MindmapIndexes in data/src/main/java/fr/julien/quievreux/droidplane2/data/model/Node.kt and data/src/main/java/fr/julien/quievreux/droidplane2/data/model/MindmapIndexes.kt per data-model.md
- [X] T004 Implement NodeManagerContract interface in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt per contracts/node-manager-contract.md
- [X] T005 [P] Implement centralized logging wrapper verification in core/src/main/java/fr/julien/quievreux/droidplane2/core/log/Logger.kt
- [X] T006 Implement Koin dependency injection bindings for NodeManager in data/src/main/java/fr/julien/quievreux/droidplane2/data/di/DataKoinModule.kt and app/src/main/java/fr/julien/quievreux/droidplane2/di/AppKoinModule.kt

**Checkpoint**: Foundation ready - user story implementation can now begin.

---

## Phase 3: User Story 1 - Mindmap Exploration & Child Node Navigation (Priority: P1) 🎯 MVP

**Goal**: Users can open a mindmap, view the root node title and styled direct child cards (with icons and chevrons), drill down into sub-trees, and navigate up to parent or top to root.

**Independent Test**: Load a sample `.mm` document; verify root title in top bar, observe styled child list (bold, italic, icons, chevrons), tap child to navigate down, tap Up to ascend, tap Top to return to root.

### Tests for User Story 1 (TDD Mandatory per Constitution)

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation**

- [X] T007 [P] [US1] Unit test for node hierarchy lookup and parent traversal in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeUtilsTest.kt
- [X] T008 [P] [US1] Unit test for XML node hierarchy parsing and root node loading in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt
- [X] T009 [P] [US1] ViewModel unit test for hierarchy navigation (child selection, Up, Top) and StateFlow emissions in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt

### Implementation for User Story 1

- [X] T010 [US1] Update NodeUtilsDefaultImpl.kt in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeUtilsDefaultImpl.kt to ensure complete indexing and parent linking during initial tree load
- [X] T011 [US1] Refactor loadMindMapFromInputStream in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt to populate rootNode and _allNodes StateFlow reliably
- [X] T012 [P] [US1] Implement navigation stack state and handlers (onChildNodeClicked, onNavigateUp, onNavigateToTop) in app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt
- [X] T013 [P] [US1] Update AppTopBar.kt in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt to display active node title and Up/Top menu actions
- [X] T014 [P] [US1] Enhance Cell.kt in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/Cell.kt with styled text (bold, italic), icon row, and child chevron indicator
- [X] T015 [US1] Update NodeList.kt in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt using LazyColumn with stable keys (key = { it.id }) and empty state message for leaf nodes
- [X] T016 [US1] Connect MindMapScreen.kt in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/MindMapScreen.kt to observe MainUiState and dispatch navigation events to MainViewModel

**Checkpoint**: At this point, User Story 1 is fully functional and delivers an independently testable MVP. ✅ **COMPLETED**

---

## Phase 4: User Story 2 - Node Search & Map Navigation (Priority: P2)

**Goal**: Users can enter search mode from the toolbar, search across all nodes case-insensitively, jump directly to matches, and cycle forward/backward through results.

**Independent Test**: Enter search mode from toolbar, submit query, observe match count, and cycle through matching nodes.

### Tests for User Story 2 (TDD Mandatory per Constitution)

- [X] T017 [P] [US2] Unit test for reactive full-text search and match positioning in data/src/test/java/fr/julien/quievreux/droidplane2/data/SearchManagerTest.kt
- [X] T018 [P] [US2] ViewModel unit test for search mode toggling and match cycling in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt

### Implementation for User Story 2

- [X] T019 [US2] Refactor SearchManager.kt in data/src/main/java/fr/julien/quievreux/droidplane2/data/search/SearchManager.kt to execute case-insensitive search across node titles and HTML-stripped rich content
- [X] T020 [US2] Expose search methods (search, getSearchResult, getSearchResultCount) in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt per contract
- [X] T021 [P] [US2] Implement search UI state handling (onSearchQueryChanged, onNextSearchMatch, onPreviousSearchMatch, onExitSearchMode) in app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt
- [X] T022 [P] [US2] Implement search mode in AppTopBar.kt in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt with back arrow, search query text field, and prev/next action icons

**Checkpoint**: User Stories 1 AND 2 are both functional and testable independently.

---

## Phase 5: User Story 3 - Node Management & Content Editing (Priority: P3)

**Goal**: Users can add new child nodes, edit existing node descriptions with instantaneous parent-child synchronization, copy node text to clipboard, follow links, and save changes safely to `.mm` files.

**Independent Test**: Edit a node description, verify parent listing reflects changes immediately, add a child node, copy text to clipboard, and save to `.mm` file without loss of formatting.

### Tests for User Story 3 (TDD Mandatory per Constitution)

- [X] T023 [P] [US3] Unit test for centralized updateNodeText and parent-child synchronization in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt — this test caught the real desync bug
- [X] T024 [P] [US3] Unit test for atomic Freeplane XML serialization round-tripping in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt
- [X] T025 [P] [US3] ViewModel unit test for node edit/add dialog workflows and synchronization in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt

### Implementation for User Story 3

- [X] T026 [US3] Implement updateNodeText(nodeId, newText) in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt — fixed: parent was re-indexed from the stale index, discarding the new `childNodes` (this was the README "not visible in children" desync)
- [X] T027 [US3] Refactor addNodeToMindmap(newValue, parentNode) in data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt — already immutable-copy + index consistent; now covered by tests
- [X] T028 [US3] Enhance atomic file persistence in serializeMindmap — already staged via temp file; verified by round-trip test preserving text, icon and font tags
- [X] T029 [P] [US3] Connect onUpdateNodeText and onAddChildNode in app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt to call NodeManager directly and reload displayed node — already delegated; contract-named wrappers added
- [X] T030 [P] [US3] Implement node editing and addition dialogs — already Compose `CustomDialog` from `:core`, wired in MainActivity for Edit and AddChildNode
- [X] T031 [US3] Wire context menu actions (Edit, Copy to clipboard, Open link) in app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt — `Cell.kt` reference dropped, that file was dead and has been removed
- [X] T032 [US3] Connect Save action in app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt and top menu in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt to trigger atomic file saving — verified wired (`Save -> viewModel.launchSaveFile()`)

**Checkpoint**: All three user stories are complete, independently verifiable, and fully synchronized.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end validation, performance checks, and codebase hygiene.

- [ ] T033 [P] Execute end-to-end validation scenarios documented in specs/001-mindmap-node-management/quickstart.md
- [X] T034 [P] Run full project test suite (./gradlew test) across core, data, and app
- [X] T035 Run code formatting and lint checks (./gradlew lint) across all modules
- [ ] T036 Remove deprecated test components and clean up unused code in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/MindMap.kt

---

## Phase 7: Convergence

**Purpose**: Remediate gaps between specification/plan and current implementation identified during convergence review.

### Constitution Violations (CRITICAL - must fix first)

- [X] T037 **CRITICAL** Fix `MainViewModel.updateNodeText()` to delegate to `NodeManager.updateNodeText()` per Constitution II (Unidirectional Data Flow) — currently bypasses single source of truth, causing UI desynchronization (contradicts)
- [X] T038 **CRITICAL** Fix `MainViewModel.addNode()` to refresh from `NodeManager` indexes instead of rebuilding tree via `findFilledNode()` per Constitution II (contradicts)

### User Story 2 - Search (HIGH)

- [X] T039 [US2] Implement `MainViewModel.onSearchQueryChanged()`, `onNextSearchMatch()`, `onPreviousSearchMatch()` (the contract names — task text said `NodeManager` and `node-manager-contract.md`, both incorrect; ui-contracts.md owns these) and wire to `SearchManager`, `onNextSearchMatch()`, `onPreviousSearchMatch()` per `node-manager-contract.md` and wire to `SearchManager` (missing)
- [X] T040 [US2] Fix `SearchManager.search()` to trigger `onResultFound` for all match counts (not just 1) and expose match position (partial)
- [X] T041 [US2] Add search result highlighting in `NodeList.kt` / `NodeItem.kt` — currently TODO in MainViewModel (missing)
- [X] T042 [US2] Fix search prev/next button visibility logic in `MainViewModel` / `AppTopBar` — currently TODO (partial)
- [X] T043 [P] [US2] Write unit test for reactive full-text search and match positioning in `SearchManagerTest.kt` per T017 (missing)
- [X] T044 [P] [US2] Write ViewModel unit test for search mode toggling and match cycling in `MainViewModelTest.kt` per T018 (missing)

### User Story 3 - Node Management (HIGH/MEDIUM)

- [x] T045 [US3] Verified `updateNodeText()` updates `modificationDate` on the edited node per data-model.md — data-model.md only requires the modified node, not the parent. Fixed the real bug instead: parent was re-indexed from the stale index, dropping the new `childNodes` (the README desync). Parent `modificationDate` not propagated; not spec'd
- [x] T046 [US3] Verified `addNodeToMindmap()` propagates to indexes and parent `childNodes`; parent `modificationDate` not updated — not required by data-model.md
- [X] T047 [P] [US3] Write unit test for centralized `updateNodeText` and parent-child synchronization in `NodeManagerTest.kt` per T023 (missing)
- [X] T048 [P] [US3] Write unit test for atomic Freeplane XML serialization round-tripping in `NodeManagerTest.kt` per T024 (missing)
- [X] T049 [P] [US3] Write ViewModel unit test for node edit/add dialog workflows and synchronization in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt per T025 (missing)
- [X] T050 [US3] Ensure context menu "Copy" action copies full description (including rich text HTML) to clipboard per FR-011 — verified wired end to end: `getNodeTextForCopy` (returns raw rich-text HTML) -> `updateClipBoard` -> `ClipboardManager`
- [ ] T051 [US3] Verify save flow end-to-end including storage permission handling per FR-013, FR-014 (partial)
- [X] T052 [US3] Implement Compose-based edit/add node dialog per `ui-contracts.md` and T030 — replace legacy `DialogFragment` (missing) — legacy `EditDescriptionDialog.kt` no longer exists; `:core` `CustomDialog` is used
- [ ] T053 [US3] Complete `NodeManager.serializeNode()` to include all Freeplane attributes (icon, link, format, etc.) per Principle IV (partial)

### Code Quality & Technical Debt (LOW)

- [ ] T054 Fix clone node text resolution in `NodeManager.getNodeText()` during initial load — currently TODO (partial)
- [X] T055 Fix NPE in `MainViewModel.downTo()` during device rotation — was TODO (partial)
- [X] T056 Review and remove or clarify unused `Cell.kt` — full implementation is in `NodeItem` (unrequested)
- [X] T057 Add empty/whitespace validation for node names in UI per Edge Case spec — `CustomDialog` rejected only `isEmpty()` and never rendered the error; now `isBlank()` + visible `isError`/`supportingText`

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately.
- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2 completion. Can proceed independently.
- **User Story 2 (Phase 4)**: Depends on Phase 2 completion. Operates on `_allNodes` stream established in Phase 2/3.
- **User Story 3 (Phase 5)**: Depends on Phase 2 completion. Implements central mutations in `NodeManager` and integrates with US1 UI components.
- **Polish (Phase 6)**: Depends on all user stories (Phases 3, 4, 5) being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Independent of US2 and US3. Delivers read-only hierarchy browsing MVP.
- **User Story 2 (P2)**: Independent of US3. Extends top bar and adds search result navigation.
- **User Story 3 (P3)**: Builds on the cell and dialog components from US1 to add node mutations and document saving.

### Within Each User Story

- Unit tests MUST be written and verified before finalizing implementations.
- Data layer service/model changes in `:data` precede presentation changes in `:app`.
- UI composables are connected to ViewModel actions once ViewModel state transitions pass tests.

---

## Parallel Opportunities

### Phase 1 & 2
```bash
# Setup parallel tasks:
Task: T002 "Configure shared test fixtures and sample Freeplane .mm XML resources in data/src/test/resources/test_map.mm"

# Foundational parallel tasks:
Task: T003 "Verify and update domain Node model and MindmapIndexes in data/src/main/java/fr/julien/quievreux/droidplane2/data/model/Node.kt and data/src/main/java/fr/julien/quievreux/droidplane2/data/model/MindmapIndexes.kt per data-model.md"
Task: T005 "Implement centralized logging wrapper verification in core/src/main/java/fr/julien/quievreux/droidplane2/core/log/Logger.kt"
```

### Phase 3 (User Story 1)
```bash
# Parallel test writing:
Task: T007 "Unit test for node hierarchy lookup and parent traversal in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeUtilsTest.kt"
Task: T008 "Unit test for XML node hierarchy parsing and root node loading in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt"
Task: T009 "ViewModel unit test for hierarchy navigation in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt"

# Parallel UI component implementation:
Task: T013 "Update AppTopBar.kt in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt"
Task: T014 "Enhance Cell.kt in app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/Cell.kt"
```

### Phase 4 (User Story 2)
```bash
# Parallel test writing:
Task: T017 "Unit test for reactive full-text search in data/src/test/java/fr/julien/quievreux/droidplane2/data/SearchManagerTest.kt"
Task: T018 "ViewModel unit test for search mode in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt"
```

### Phase 5 (User Story 3)
```bash
# Parallel test writing:
Task: T023 "Unit test for centralized updateNodeText in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt"
Task: T024 "Unit test for atomic Freeplane XML serialization in data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt"
Task: T025 "ViewModel unit test for node edit/add dialogs in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1: Setup (T001 - T002).
2. Complete Phase 2: Foundational (T003 - T006).
3. Complete Phase 3: User Story 1 (T007 - T016).
4. **STOP and VALIDATE**: Verify Scenario 1 from `quickstart.md` on an Android emulator or device. This delivers a working MVP allowing users to explore their mindmaps.

### Incremental Delivery
1. Once US1 MVP is validated, implement User Story 2 (T017 - T022) to enable rapid keyword search across the entire mindmap.
2. Implement User Story 3 (T023 - T032) to resolve the active state synchronization bug, allowing seamless in-place editing and atomic Freeplane `.mm` document saving.
3. Execute Phase 6 (T033 - T036) for cross-module test passes and clean up.

---

## Phase 8: Convergence (Continued)

**Purpose**: Additional gaps identified in subsequent convergence review, including Constitution violations not previously captured and missing User Story 3 UI implementation.

### Constitution Violations (CRITICAL - must fix first)

- [X] T058 **CRITICAL** Remove direct mutation of `node.isSelected` in `MainViewModel` (lines 150, 271, 309, 322, 451) per Constitution II (Unidirectional Data Flow) — UI components must never mutate shared domain state directly; selection state should be derived from `navigationStack` or `SearchUiState` (contradicts)

### User Story 3 - Node Management UI (HIGH)

- [X] T059 [US3] Implement Compose-based node editing dialog per `ui-contracts.md` and T030 — replace legacy `DialogFragment` in `EditDescriptionDialog.kt` with Compose dialog using `CustomDialog` from `:core` (missing)
- [X] T060 [US3] Implement "Add Child Node" FAB/action in `MainViewModel`/`MindMapScreen` — wire `MainViewModel.addNode()` to UI with input dialog per FR-008 (missing)
- [X] T061 [US3] Implement "Copy to clipboard" action in `NodeItem` context menu to copy full description including rich text HTML per FR-011 (missing)
- [X] T062 [US3] Wire Save menu action in `AppTopBar` → `MainViewModel.launchSaveFile()` per FR-013, FR-014 (missing)
- [X] T063 [US3] Wire "Open link" context menu action in `NodeItem` to handle internal/external links per FR-012 (missing)

### User Story 3 - Tests (HIGH, Constitution III)

- [X] T064 [P] [US3] Write unit test for centralized `updateNodeText` and parent-child synchronization in `NodeManagerTest.kt` per T023 (missing)
- [X] T065 [P] [US3] Write unit test for atomic Freeplane XML serialization round-tripping in `NodeManagerTest.kt` per T024 (missing)
- [X] T066 [P] [US3] Write ViewModel unit test for node edit/add dialog workflows and synchronization in app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt per T025 (missing)

### User Story 2 - Search Polish (MEDIUM)

- [X] T067 [US2] Implement `NodeManager.onSearchQueryChanged()`, `onNextSearchMatch()`, `onPreviousSearchMatch()` per `node-manager-contract.md` and wire to `SearchManager` (missing)  (duplicate of T039)
- [X] T068 [US2] Add search result highlighting in `NodeList.kt` / `NodeItem.kt` — currently TODO in MainViewModel line 422 "highlight result in column" (missing)  (duplicate of T041)
- [X] T069 [US2] Fix search prev/next button visibility logic in `MainViewModel` / `AppTopBar` — currently TODO line 421 "Shows/hides the next/prev buttons" (partial)  (duplicate of T042)

### Data Layer Completeness (MEDIUM)

- [x] T070 [US3] Verify `NodeManager.updateNodeText()` updates `modificationDate` on parent node per data-model.md validation rule — NodeManager line 719-726 updates parent but test TODO at line 286 (partial)  (duplicate of T045)
- [x] T071 [US3] Verify `NodeManager.addNodeToMindmap()` updates parent `modificationDate` and propagates to indexes (partial)  (duplicate of T046)
- [ ] T072 [US3] Complete `NodeManager.serializeNode()` to include all Freeplane attributes (icon, link, format, etc.) per Principle IV — TODO line 534 (partial)  (duplicate of T053)
- [ ] T073 Fix clone node text resolution in `NodeManager.getNodeText()` during initial load — TODO line 268 (partial)  (duplicate of T054)
- [X] T074 Fix NPE in `MainViewModel.downTo()` during device rotation — TODO line 441 (partial)  (duplicate of T055)

### Code Quality & Technical Debt (LOW)

- [X] T075 Review and remove or clarify unused `Cell.kt` — full implementation is in `NodeItem` (unrequested)  (duplicate of T056)
- [X] T076 Add empty/whitespace validation for node names in UI per Edge Case spec (missing)  (duplicate of T057)

