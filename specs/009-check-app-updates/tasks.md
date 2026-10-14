# Tasks: GitHub In-App Update Detection and Installation

**Input**: Design documents from `/specs/009-check-app-updates/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Test-driven verification (Principle III) included for JVM unit tests and Compose previews.
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Exact file paths are provided for each task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish localized strings and domain models

- [X] T001 [P] Add string resources in `app/src/main/res/values/strings.xml` for check for updates action, update dialog title, current version, new version, changelog, download and install, view on GitHub, later, up to date message, and network error message
- [X] T002 [P] Create domain models `AppVersion.kt` and `GitHubRelease.kt` in `app/src/main/java/fr/julien/quievreux/droidplane2/update/` per data-model.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core version comparison, GitHub release parser, and download helper that MUST be complete before user stories

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 [P] Create unit test suite `AppVersionTest` in `app/src/test/java/fr/julien/quievreux/droidplane2/update/AppVersionTest.kt` verifying semantic version parsing, prefix normalization (v), and version comparison using `kotlin.test`
- [X] T004 Implement `UpdateCheckerService` and `UpdateCheckResult` in `app/src/main/java/fr/julien/quievreux/droidplane2/update/UpdateCheckerService.kt` querying the GitHub releases API, parsing release JSON, comparing versions, and returning `UpdateCheckResult`
- [X] T005 [P] Create unit test suite `UpdateCheckerServiceTest` in `app/src/test/java/fr/julien/quievreux/droidplane2/update/UpdateCheckerServiceTest.kt` testing JSON parsing of GitHub releases, APK asset extraction, and version availability logic using `kotlin.test`
- [X] T006 [P] Implement `ApkDownloadHelper` in `app/src/main/java/fr/julien/quievreux/droidplane2/update/ApkDownloadHelper.kt` encapsulating Android `DownloadManager` request configuration with visible completion notification

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Checking for Available Updates (Priority: P1) 🎯 MVP

**Goal**: Enable automatic background update check upon startup and manual check via top bar menu action, emitting state through `MainViewModel`.

**Independent Test**: Tap "Check for updates" in the app menu; verify `UpdateCheckResult.UpToDate` or `UpdateAvailable` is triggered, and startup check triggers asynchronously.

### Implementation for User Story 1

- [X] T007 [US1] Add `AppTopBarAction.CheckForUpdates` and menu item in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/AppTopBar.kt`
- [X] T008 [US1] Extend `MainViewModel` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt` with `UpdateUiState`, `checkForUpdates(isManual: Boolean)`, and `dismissUpdateDialog()` state handling using `UpdateCheckerService`
- [X] T009 [US1] Hook startup update check into `MainActivity.kt` and handle `AppTopBarAction.CheckForUpdates` to trigger manual checks with feedback snackbars

**Checkpoint**: User Story 1 complete and independently testable (MVP)

---

## Phase 4: User Story 2 - Viewing Update Information and Release Notes (Priority: P1)

**Goal**: Display `UpdateAvailableDialog` with target version, current version, and formatted changelog body.

**Independent Test**: When an update is detected, verify `UpdateAvailableDialog` is shown with changelog, "Download & Install", "View on GitHub", and "Later" actions.

### Implementation for User Story 2

- [X] T010 [P] [US2] Create `UpdateAvailableDialog` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/dialog/UpdateAvailableDialog.kt` with title, version info, scrollable changelog, and action buttons, with `@Preview`
- [X] T011 [US2] Integrate `UpdateAvailableDialog` into `MainActivity.kt`, rendering when `uiState.updateDialogRelease != null`

**Checkpoint**: User Stories 1 AND 2 complete and independently testable

---

## Phase 5: User Story 3 - Downloading and Installing the APK via DownloadManager (Priority: P1)

**Goal**: Allow users to tap "Download & Install" to enqueue the APK in `DownloadManager` and provide a fallback link to the GitHub release page.

**Independent Test**: Tap "Download & Install" in the update dialog; verify `ApkDownloadHelper.enqueueDownload` is invoked, download starts, and fallback browser intent opens when requested.

### Implementation for User Story 3

- [X] T012 [US3] Wire "Download & Install" and "View on GitHub" actions in `MainActivity.kt` to trigger `ApkDownloadHelper.enqueueDownload` and system browser intent

**Checkpoint**: All user stories complete and independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Dependency injection, performance validation, and full test suite verification

- [X] T013 [P] Register `UpdateCheckerService` in Koin DI module in `app/src/main/java/fr/julien/quievreux/droidplane2/di/AppKoinModule.kt`
- [X] T014 Run full test suite `./gradlew test` to ensure zero regressions across all modules
- [X] T015 Execute validation scenarios per `specs/009-check-app-updates/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - starts immediately (T001, T002 in parallel)
- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user stories
  - T003 (`AppVersionTest`) depends on T002
  - T004 (`UpdateCheckerService`) depends on T002
  - T005 (`UpdateCheckerServiceTest`) depends on T004
  - T006 (`ApkDownloadHelper`) depends on T002
- **User Story 1 (Phase 3)**: Depends on Foundational Phase (T004). Delivers MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 3 (T008).
- **User Story 3 (Phase 5)**: Depends on Phase 4 (T011) and Foundational (T006).
- **Polish (Phase 6)**: Depends on US1, US2, and US3 completion.

---

## Parallel Opportunities

- **Phase 1**: T001 and T002 can run in parallel.
- **Phase 2**: T003, T005, and T006 can run in parallel once T002/T004 are ready.
- **Phase 4**: T010 (`UpdateAvailableDialog`) can be written in parallel with T008/T009.
- **Phase 6**: T013 can be completed in parallel with docs/previews.

---

## Implementation Strategy

### MVP First (User Story 1)
1. Complete Phase 1: Setup (`strings.xml`, `AppVersion.kt`, `GitHubRelease.kt`).
2. Complete Phase 2: Foundational (`UpdateCheckerService.kt`, `ApkDownloadHelper.kt`, unit tests).
3. Complete Phase 3: User Story 1 (`AppTopBar.kt`, `MainViewModel.kt`, `MainActivity.kt`).
4. **STOP and VALIDATE**: Test update checking independently on startup and via menu.

### Incremental Delivery
1. Foundation + US1 -> App detects updates and informs user via snackbars (MVP).
2. Add US2 -> Rich update dialog with changelog and release notes.
3. Add US3 -> 1-tap download and installation via system DownloadManager.
4. Polish -> Dependency injection cleanup and full regression testing.
