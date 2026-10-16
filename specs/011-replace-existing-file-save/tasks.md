# Tasks: Replace Existing File on Save (`011-replace-existing-file-save`)

**Input**: Design documents from `/specs/011-replace-existing-file-save/`  
**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/file-save-contract.md`

**Tests**: Unit tests using `kotlin.test` and `KStringSpec` are included in accordance with Constitution Principle III (Test-Driven Verification) and user rules.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Exact file paths are included in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Localization resources and SAF helper extensions required for existing-file replacement and confirmation UI.

- [X] T001 [P] Add English overwrite confirmation dialog string resources (`overwrite_file_title`, `overwrite_file_message`, `overwrite_replace`) in `app/src/main/res/values/strings.xml`
- [X] T002 [P] Add French overwrite confirmation dialog string resources (`overwrite_file_title`, `overwrite_file_message`, `overwrite_replace`) in `app/src/main/res/values-fr/strings.xml`
- [X] T003 Update `OpenDocument` intent flags (`FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION`), `launchFileSavingIntent` (`DocumentsContract.EXTRA_INITIAL_URI`), and add `deleteSafDocument(context, uri)` helper in `core/src/main/java/fr/julien/quievreux/droidplane2/core/extensions/ActivityExtensions.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Active document URI + display name tracking and pure-Kotlin save target resolution model required by all user stories.

**⚠️ CRITICAL**: No user story work can begin until this phase is complete.

- [X] T004 [P] Track `currentMindMapDisplayName: String?` alongside `currentMindMapUri: Uri?` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` (update `setMapUri(data: Uri?, displayName: String? = null)`, `getMindmapFileName()`, and reset both to `null` in `createNewMindmap()`)
- [X] T005 [P] Update `FileRegister` interface to accept optional `initialUri: Uri? = null` in `app/src/main/java/fr/julien/quievreux/droidplane2/helper/FileRegister.kt`
- [X] T006 [P] Create `SaveResolution` sealed interface (`WriteNewFile`, `DirectOverwrite`, `ConfirmOverwrite`) and `SaveTargetResolver` (including `(n)` auto-incremented filename pattern matching `^${Regex.escape(baseName)}\s*\([1-9]\d*\)(?:\.${Regex.escape(extension)})?$` and `^${Regex.escape(originalFileName)}\s*\([1-9]\d*\)$`) in `app/src/main/java/fr/julien/quievreux/droidplane2/helper/SaveTargetResolver.kt`
- [X] T007 [P] Write unit tests for `NodeManager` display name and URI tracking/reset in `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt`
- [X] T008 [P] Write unit tests (using `kotlin.test`) for `SaveTargetResolver` covering `WriteNewFile`, `DirectOverwrite`, `ConfirmOverwrite`, and all `(n)` suffix edge cases in `app/src/test/java/fr/julien/quievreux/droidplane2/helper/SaveTargetResolverTest.kt`

**Checkpoint**: Foundation ready — user story implementation can now begin.

---

## Phase 3: User Story 1 - Replace Existing File on Save with Confirmation (Priority: P1) 🎯 MVP

**Goal**: When editing an existing `.mm` file and validating the save in the system file picker, prompt for overwrite confirmation and replace the original file in place without leaving `(n)` duplicate files.

**Independent Test**: Open an existing `.mm` file, edit a node, trigger Save, validate in the system picker, confirm overwrite in `OverwriteConfirmationDialog`, and verify the original URI is updated with `"wt"` truncation mode, any temporary `(n)` document is deleted, and `RecentFilesRepository` has no `(n)` duplicate entry.

### Tests for User Story 1

- [X] T009 [P] [US1] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying `getSaveFilename()` returns the exact display name of an open user file, `launchSaveFile()` passes `initialUri` to `FileRegister`, and `showOverwriteConfirmation()` emits `DialogType.OverwriteConfirmation`

### Implementation for User Story 1

- [X] T010 [P] [US1] Add `DialogType.OverwriteConfirmation(val fileName: String, val onConfirm: () -> Unit, val onCancel: () -> Unit)` (with non-blank `fileName`) to `app/src/main/java/fr/julien/quievreux/droidplane2/MainUiState.kt`
- [X] T011 [P] [US1] Create stateless `OverwriteConfirmationDialog` composable and `PreviewOverwriteConfirmationDialog` (`@Preview`) in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/OverwriteConfirmationDialog.kt`
- [X] T012 [US1] Update `MainViewModel` (`setMapUri(data: Uri?, displayName: String? = null)`, `getMapUri()`, `getSaveFilename()`, `launchSaveFile()`, `showOverwriteConfirmation()`, and `markFileSaved()`) in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- [X] T013 [US1] Update `MainActivity` to persist read+write URI permissions on open, pass resolved display names to `viewModel.setMapUri(uri, displayName)`, render `OverwriteConfirmationDialog`, and handle `SaveResolution.ConfirmOverwrite` / `DirectOverwrite` (truncating `originalUri` with `"wt"`, deleting `duplicatePickerUri`, updating `RecentFilesRepository`, and running `onPostSaveAction`) in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: User Story 1 is functional and testable independently.

---

## Phase 4: User Story 2 - Cancel Overwrite Confirmation Safely (Priority: P2)

**Goal**: Allow the user to cancel the overwrite confirmation dialog without modifying the original file on disk, removing the temporary 0-byte `(n)` placeholder created by the system picker, and preserving unsaved changes in memory.

**Independent Test**: Trigger Save on an edited existing file, validate in the system picker, cancel `OverwriteConfirmationDialog`, and verify the temporary `(n)` URI is deleted, `hasUnsavedChanges` remains `true`, and `pendingPostSaveAction` is cleared without executing.

### Tests for User Story 2

- [X] T014 [P] [US2] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying that cancelling `DialogType.OverwriteConfirmation` dismisses the dialog, preserves `hasUnsavedChanges == true`, and cancels any pending post-save action

### Implementation for User Story 2

- [X] T015 [US2] Implement cancel handling in `MainViewModel` (`cancelPendingPostSaveAction()` / `dismissDialog()`) and `MainActivity` (`onCancel` callback deleting the 0-byte `duplicatePickerUri` via `deleteSafDocument` and keeping `hasUnsavedChanges = true`) in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt` and `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: User Stories 1 and 2 work independently and safely.

---

## Phase 5: User Story 3 - Save New Mindmap or Save Under a New Name (Priority: P3)

**Goal**: When saving a newly created mindmap, the demo mindmap, or explicitly entering a new filename in the system picker, write to the newly created URI without redundant confirmation and update the active document URI/display name to that new file.

**Independent Test**: Create a new mindmap (or change the filename in the system picker to a different base name), save, and verify `SaveResolution.WriteNewFile` writes to the new URI and updates `viewModel.getMapUri()` and `getSaveFilename()` to the newly saved file.

### Tests for User Story 3

- [X] T016 [P] [US3] Add unit tests in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying that after saving a new mindmap or saving under a new name (`setMapUri(newUri, newDisplayName)`), subsequent `getSaveFilename()` calls return `newDisplayName` and `getMapUri()` returns `newUri`

### Implementation for User Story 3

- [X] T017 [US3] Ensure `SaveResolution.WriteNewFile` in `MainActivity.saveFileLauncher` writes to `pickedUri` with mode `"wt"`, updates `viewModel.setMapUri(pickedUri, pickedDisplayName)`, and records `pickedUri` in `RecentFilesRepository` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: All user stories (US1, US2, US3) are independently functional.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: End-to-end test suite verification and lint/build checks across all modules.

- [X] T018 Run full unit test suite (`./gradlew test`) across `:core`, `:data`, and `:app` and verify 0 failures

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories.
- **User Stories (Phase 3–5)**: Depend on Foundational phase completion (`NodeManager`, `FileRegister`, `SaveTargetResolver`).
- **Polish (Phase 6)**: Depends on all user stories being complete.

### Parallel Opportunities

- `T001` and `T002` (EN and FR string resources) can run in parallel.
- `T004`, `T005`, `T006`, `T007`, and `T008` in Phase 2 touch distinct files in `:data` and `:app` and can be implemented in parallel.
- `T010` (`MainUiState.kt`) and `T011` (`OverwriteConfirmationDialog.kt`) can be implemented in parallel before `T012`/`T013`.

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup (`T001`–`T003`)
2. Complete Phase 2: Foundational (`T004`–`T008`)
3. Complete Phase 3: User Story 1 (`T009`–`T013`)
4. Validate User Story 1 unit tests
5. Complete Phase 4 (`T014`–`T015`), Phase 5 (`T016`–`T017`), and Phase 6 (`T018`)
