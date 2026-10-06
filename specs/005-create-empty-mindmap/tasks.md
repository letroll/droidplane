# Tasks: Create Empty Mindmap with Demo Access

**Input**: Design documents from `/specs/005-create-empty-mindmap/`
**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `contracts/`, `quickstart.md`, `.specify/memory/constitution.md`

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify build baseline across all modules

- [X] T001 Verify project build and test baseline across all modules via `./gradlew test --quiet`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core model extensions, domain interface methods, and strings required before user stories can be implemented

**⚠️ CRITICAL**: Must be completed before User Story implementation

- [X] T002 [P] Add string resources `new_mindmap`, `help_demo`, `discard_changes_title`, and `discard_changes_message` in `app/src/main/res/values/strings.xml`
- [X] T003 [P] Add `createNewMindmap(rootTitle: String = "Central Idea"): Node` to `NodeManagerContract` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt`
- [X] T004 Implement `createNewMindmap` in `NodeManager` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` resetting root node, indexes, `_allNodes`, and file URI
- [X] T005 [P] Add `DialogType.DiscardConfirmation` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainUiState.kt`
- [X] T006 [P] Add unit test for `NodeManager.createNewMindmap` in `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt`

**Checkpoint**: Foundation ready — `NodeManager` can programmatically initialize empty mindmaps and `MainUiState` supports discard confirmation dialogues.

---

## Phase 3: User Story 1 - Create a New Empty Mindmap (Priority: P1) 🎯 MVP

**Goal**: Allow users to create a clean empty mindmap with a single root node from the options menu, guarded by an unsaved changes confirmation dialog.

**Independent Test**: Select "New Mindmap" from the menu; verify a clean single-node mindmap appears; verify that having unsaved edits triggers a confirmation dialog before proceeding.

### Tests for User Story 1

- [X] T007 [P] [US1] Add unit tests for `onNewMindmapRequested`, discard confirmation guard, and clean state reset in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

### Implementation for User Story 1

- [X] T008 [US1] Implement `onNewMindmapRequested` and `createEmptyMindmap` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- [X] T009 [US1] Add `AppTopBarAction.NewMindmap` and menu item in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt`
- [X] T010 [US1] Wire `NewMindmap` action and render `DialogType.DiscardConfirmation` dialog in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: User Story 1 fully functional — users can create empty mindmaps safely with unsaved changes protection.

---

## Phase 4: User Story 2 - Access Help & Demo Mindmap at Any Time (Priority: P1)

**Goal**: Allow users to reload the bundled tutorial/demo mindmap (`example.mm`) at any time from the options menu with unsaved changes protection.

**Independent Test**: Select "Help & Demo" from the options menu; verify the full tutorial mindmap loads and displays; verify unsaved edits trigger confirmation dialog.

### Tests for User Story 2

- [X] T011 [P] [US2] Add unit tests for `onHelpDemoRequested` and discard confirmation in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

### Implementation for User Story 2

- [X] T012 [US2] Implement `onHelpDemoRequested` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- [X] T013 [US2] Update `AppTopBar.kt` menu label to "Help & Demo" in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt`
- [X] T014 [US2] Wire `Help` action in `MainActivity.kt` to load `R.raw.example` demo mindmap via `viewModel.onHelpDemoRequested` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: User Stories 1 and 2 functional — users can create clean mindmaps and freely return to the tutorial demo.

---

## Phase 5: User Story 3 - Customizing the Central Topic of a New Mindmap (Priority: P2)

**Goal**: Ensure editing the central root node on a newly created empty mindmap immediately updates the document title, canvas label, and dirty state.

**Independent Test**: Create an empty mindmap, edit the central node text to "Project Roadmap", and verify top bar title and node card reflect the new text with unsaved changes flag enabled.

### Tests for User Story 3

- [X] T015 [P] [US3] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying root node editing and title synchronization on a newly created empty mindmap

### Implementation for User Story 3

- [X] T016 [US3] Verify and ensure root node title update properly synchronizes document title and dirty flag in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`

**Checkpoint**: All user stories functional — full workflow from creation to personalization validated.

---

## Phase 6: User Story 4 - Startup Chooser: Create New or Open Recent Files (Priority: P1)

**Goal**: Offer the choice on app launch to create a new empty mindmap or open an existing recent file, while providing browsing and demo access.

**Independent Test**: Launch the app normally; verify that the startup chooser opens with "New Mindmap", "Browse", "Help & Demo", and a list of existing recent files, filtering out any deleted files.

### Tests for User Story 4

- [X] T017 [P] [US4] Add unit tests for `RecentFilesRepository` in `app/src/test/java/fr/julien/quievreux/droidplane2/data/RecentFilesRepositoryTest.kt`
- [X] T018 [P] [US4] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` for startup chooser presentation and recent file selection

### Implementation for User Story 4

- [X] T019 [P] [US4] Create `RecentFile` model in `app/src/main/java/fr/julien/quievreux/droidplane2/model/RecentFile.kt`
- [X] T020 [P] [US4] Add string resources `startup_title`, `recent_files`, `browse_files`, and `no_recent_files` in `app/src/main/res/values/strings.xml`
- [X] T021 [US4] Implement `RecentFilesRepository` in `app/src/main/java/fr/julien/quievreux/droidplane2/data/RecentFilesRepository.kt` to persist, retrieve, and filter existing recent files
- [X] T022 [P] [US4] Add `DialogType.StartupChooser` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainUiState.kt`
- [X] T023 [P] [US4] Implement `StartupChooserDialog` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/StartupChooserDialog.kt` with `@Preview` functions
- [X] T024 [US4] Implement `showStartupChooser` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- [X] T025 [US4] Wire `StartupChooser` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt` on startup when not launched via external intent, recording opened/saved files to `RecentFilesRepository`

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates, full test verification, and documentation validation

- [X] T026 [P] Verify localized string resources and Compose preview functions in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/StartupChooserDialog.kt`
- [X] T027 Run full regression test suite across all modules via `./gradlew test --rerun-tasks`
- [X] T028 Verify complete application build and APK assembly via `./gradlew assembleDebug`
- [X] T029 Execute quickstart validation scenarios from `specs/005-create-empty-mindmap/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Independent, run first.
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2. Core MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 2 (shares discard dialog from Phase 2).
- **User Story 3 (Phase 5)**: Depends on Phase 3.
- **Polish (Phase 6)**: Depends on all user stories being completed.

### User Story Dependencies

- **US1 (P1)**: Foundational prerequisites only.
- **US2 (P1)**: Shares `DiscardConfirmation` dialog pattern with US1; can proceed in parallel once Phase 2 is complete.
- **US3 (P2)**: Validates editing on maps created by US1.

---

## Parallel Opportunities

- Within Phase 2: `T002` (strings.xml), `T003` (contract), and `T005` (MainUiState.kt) can run concurrently.
- Across Stories: `T007` (US1 tests) and `T011` (US2 tests) can run in parallel.
- Within Polish: `T017` (Preview checks) can run in parallel with test suite validation.

---

## Implementation Strategy

### MVP First (User Story 1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (`createNewMindmap`, strings, discard dialog)
3. Complete Phase 3: User Story 1 (New Mindmap action, ViewModel handler, dialog wiring)
4. **STOP and VALIDATE**: Verify empty mindmap creation and discard confirmation guard (MVP ready!)

### Incremental Delivery

1. Add User Story 2 (Help & Demo reload) → Validate demo accessibility.
2. Add User Story 3 (Personalize central topic) → Validate root title synchronization.
3. Complete Phase 6 (Polish, full regression test suite, quickstart validation).
