# Tasks: Back Navigation to Recent Files

**Input**: Design documents from `/specs/007-back-nav-recent-files/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, quickstart.md

**Tests**: The examples below include test tasks. Tests are OPTIONAL - only include them if explicitly requested in the feature specification.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

- **Android multi-module**: `app/src/main/java/fr/julien/quievreux/droidplane2/` for source, `app/src/test/` for unit tests, `app/src/androidTest/` for UI tests

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure - already complete for this existing project

- [ ] T001 Verify Gradle project builds and tests pass in current state

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T002 Add `DialogType.RecentFiles` sealed class variant to `MainUiState.kt` with fields: recentFiles (List<RecentFile>), onOpenRecent, onNewMindmap, onBrowse, onOpenDemo, onExitApp, onDismiss per data-model.md
- [ ] T003 Add string resources for "Recent Files" menu item, "Exit App" button, and any new labels in `app/src/main/res/values/strings.xml`

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Back Navigation Shows Recent Files When No Unsaved Changes (Priority: P1) 🎯 MVP

**Goal**: When user presses back (system or toolbar) on a mindmap with no unsaved changes, show Recent Files dialog instead of closing app. Dialog shows recent files list, allows selecting file, creating new mindmap, browsing, opening demo.

**Independent Test**: Open mindmap, make no changes, press system back → Recent Files dialog opens with recent files list. Select file → loads mindmap. Press New Mindmap → creates empty mindmap.

### Implementation for User Story 1

- [ ] T004 [US1] Create `RecentFilesDialog` component in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/RecentFilesDialog.kt` reusing `StartupChooserDialog` structure with added "Exit App" button (replacing Cancel with dismiss) - must display recentFiles list from DialogType.RecentFiles, max 8 items, filtered by createStorageFileChecker, show "No recent files found" when empty
- [ ] T005 [US1] Add `showRecentFilesDialog()` method to `MainViewModel.kt` that fetches recent files via RecentFilesRepository, filters with createStorageFileChecker, and calls `setDialogState(DialogType.RecentFiles(...))` with all callbacks
- [ ] T006 [US1] Modify `MainViewModel.up(force: Boolean)` in `MainViewModel.kt` to check `hasUnsavedChangesState`: if force && !hasUnsavedChangesState → call `showRecentFilesDialog()` instead of `leaveApp()`
- [ ] T007 [US1] Modify `MainViewModel.upOrClose()` in `MainViewModel.kt` to delegate to `up(true)` (already does, verify logic flows correctly)
- [ ] T008 [US1] Update `MainActivity.kt` BackHandler logic to call `viewModel.upOrClose()` (already does, verify) and render `RecentFilesDialog` when `dialogUiState.dialogType is DialogType.RecentFiles`
- [ ] T009 [US1] Add string resource `recent_files_dialog_title` and `exit_app` in `strings.xml`
- [ ] T010 [US1] Implement `RecentFilesDialog` callbacks: onOpenRecent loads mindmap via `viewModel.setMapUri()` + `viewModel.loadMindMap()`, onNewMindmap calls `viewModel.onNewMindmapRequested()`, onBrowse launches file picker, onOpenDemo loads demo mindmap, onExitApp calls `finish()`, onDismiss calls `viewModel.setDialogState(DialogType.None)`

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently - pressing back with no unsaved changes shows Recent Files dialog with all buttons working

---

## Phase 4: User Story 2 - Exit App Button on Recent Files Dialog (Priority: P1)

**Goal**: Recent Files dialog includes an explicit "Exit App" button that closes the application when pressed.

**Independent Test**: Open Recent Files dialog (via back press with no unsaved changes), press Exit App button → app closes completely. Press dialog dismiss → returns to mindmap.

### Implementation for User Story 2

- [ ] T011 [US2] Verify `RecentFilesDialog` already includes "Exit App" button with `onExitApp` callback calling `finish()` on Activity (implemented in T010)
- [ ] T012 [US2] Verify dialog dismiss action (back press on dialog, click outside) calls `onDismiss` → `viewModel.setDialogState(DialogType.None)` (implemented in T010)

**Checkpoint**: At this point, User Story 2 should be fully functional - Exit App button closes app, dismiss returns to mindmap

---

## Phase 5: User Story 3 - Toolbar Menu Access to Recent Files Dialog (Priority: P2)

**Goal**: Users can access the Recent Files dialog from the toolbar overflow menu (three-dot menu) on all screen sizes.

**Independent Test**: Open toolbar overflow menu, select "Recent Files" → Recent Files dialog opens with correct recent files list. All buttons work as in US1.

### Implementation for User Story 3

- [ ] T013 [US3] Add `RecentFiles` case to `AppTopBarAction` enum in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt`
- [ ] T014 [US3] Add "Recent Files" dropdown menu item in `AppTopBar.kt` dropdown menu (always visible, not conditional) with `onClick` calling `onBarAction(AppTopBarAction.RecentFiles)`
- [ ] T015 [US3] Handle `AppTopBarAction.RecentFiles` in `MainActivity.kt` `onBarAction` when-expression to call `viewModel.showRecentFilesDialog()`
- [ ] T016 [US3] Add string resource `menu_recent_files` in `strings.xml` for the menu item label

**Checkpoint**: At this point, User Story 3 should be fully functional - toolbar menu shows "Recent Files" item that opens the dialog

---

## Phase 6: User Story 4 - Back Navigation Still Closes App When Unsaved Changes Exist (Priority: P1)

**Goal**: Preserve existing Exit Confirmation dialog behavior when mindmap has unsaved changes.

**Independent Test**: Edit a node (creates unsaved changes), press back → Exit Confirmation dialog appears (not Recent Files). Confirm exit → app closes without saving. Cancel → returns to mindmap with changes preserved.

### Implementation for User Story 4

- [ ] T017 [US4] Verify `MainViewModel.up(force: Boolean)` correctly routes to `leaveApp()` when `hasUnsavedChangesState == true` (logic in T006)
- [ ] T018 [US4] Verify `MainActivity.kt` renders `ExitConfirmationDialog` when `leaving == true` and `hasUnsavedChangesState == true` (existing logic, verify no regression)
- [ ] T019 [US4] Run regression test: existing Exit Confirmation flow works unchanged (manual or existing tests)

**Checkpoint**: At this point, User Story 4 verified - unsaved changes still trigger Exit Confirmation, not Recent Files dialog

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories, validation, cleanup

- [ ] T020 [P] Add unit tests for `MainViewModel.showRecentFilesDialog()` and `up(force: Boolean)` logic in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` using MockK/Turbine
- [ ] T021 [P] Add Compose UI test for `RecentFilesDialog` in `app/src/androidTest/java/fr/julien/quievreux/droidplane2/ui/view/RecentFilesDialogTest.kt` verifying all buttons and list rendering
- [ ] T022 [P] Add Compose Preview for `RecentFilesDialog` in `RecentFilesDialog.kt` with sample recent files
- [ ] T023 Run full test suite: `./gradlew :app:test :app:connectedAndroidTest` - verify all existing tests pass (regression)
- [ ] T024 Run lint: `./gradlew :app:lint` - verify no new warnings
- [ ] T025 Manual validation per quickstart.md Scenarios 1-4 on device/emulator
- [ ] T026 Verify cyclomatic complexity ≤ 20 for all new/modified methods (Detekt or manual review)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Stories (Phase 3-6)**: All depend on Foundational phase completion
  - US1 (Phase 3) can start after Foundational - core dialog and back handling
  - US2 (Phase 4) depends on US1 (same dialog component)
  - US3 (Phase 5) depends on US1 (needs showRecentFilesDialog())
  - US4 (Phase 6) depends on US1 (modified up() logic) - verify no regression
- **Polish (Phase 7)**: Depends on all desired user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories
- **User Story 2 (P1)**: Depends on US1 (same dialog) - can be implemented together with US1
- **User Story 3 (P2)**: Depends on US1 (needs showRecentFilesDialog()) - can start after US1 core logic
- **User Story 4 (P1)**: Depends on US1 (modified up() logic) - verify regression after US1

### Within Each User Story

- Models/state before UI components
- ViewModel logic before Activity rendering
- Core implementation before integration
- Story complete before moving to next priority

### Parallel Opportunities

- T002 and T003 (Foundational) can run in parallel - different files
- T004 (RecentFilesDialog) and T005 (ViewModel method) can run in parallel - different files
- T013, T014, T015 (US3 toolbar) can run in parallel with US1/US2 - different files
- T020, T021, T022 (tests/previews) can run in parallel - different files
- All user stories can be worked on in parallel by different team members once Foundational is done

---

## Parallel Example: User Story 1

```bash
# Launch foundational tasks together:
Task: "Add DialogType.RecentFiles to MainUiState.kt"
Task: "Add string resources in strings.xml"

# Launch US1 implementation together (different files):
Task: "Create RecentFilesDialog.kt in ui/view/"
Task: "Add showRecentFilesDialog() to MainViewModel.kt"
Task: "Modify up(force: Boolean) in MainViewModel.kt"

# Launch tests together:
Task: "Unit tests for MainViewModel in MainViewModelTest.kt"
Task: "UI tests for RecentFilesDialog in RecentFilesDialogTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (verify build)
2. Complete Phase 2: Foundational (DialogType + strings)
3. Complete Phase 3: User Story 1 (dialog, ViewModel, back handling, Activity rendering)
4. **STOP and VALIDATE**: Test User Story 1 independently per quickstart.md Scenario 1
5. Deploy/demo if ready

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy/Demo (MVP!)
3. Add User Story 2 → Test independently → Deploy/Demo
4. Add User Story 3 → Test independently → Deploy/Demo
5. Add User Story 4 → Verify regression → Deploy/Demo
6. Each story adds value without breaking previous stories

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (core dialog + back handling)
   - Developer B: User Story 3 (toolbar menu) - can start after T005
   - Developer C: User Story 4 (regression verification) - after US1
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (if writing tests first)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- All file paths use actual project structure from plan.md
- Constraints from constitution: cyclomatic complexity ≤ 20, module boundaries, StateFlow, Logger from :core, Koin for DI