# Implementation Plan: Back Navigation to Recent Files

**Branch**: `007-back-nav-recent-files` | **Date**: 2026-10-12 | **Spec**: specs/007-back-nav-recent-files/spec.md

**Input**: Feature specification from `/specs/007-back-nav-recent-files/spec.md`

## Summary

Modify the back navigation behavior to show a Recent Files dialog instead of closing the app when no unsaved changes exist. The dialog reuses the existing StartupChooserDialog structure and RecentFilesRepository. Add "Exit App" button to the dialog and "Recent Files" menu item to toolbar overflow menu. Preserve existing Exit Confirmation dialog for unsaved changes.

## Technical Context

**Language/Version**: Kotlin (target JVM 21, Java 21 toolchain)

**Primary Dependencies**: Jetpack Compose Material 3, Koin (BOM 4.0.0+), AndroidX Activity/BackHandler, AndroidX Lifecycle ViewModel, SharedPreferences

**Storage**: SharedPreferences (for recent files list via RecentFilesRepository)

**Testing**: Kotest, MockK, Turbine (JVM unit tests), Compose UI tests, Jetpack Compose Previews

**Target Platform**: Android SDK 26+ (min), Target SDK 35 (Android 15)

**Project Type**: Mobile app (Android) - multi-module Gradle project (:core, :data, :app)

**Performance Goals**: Dialog loads within 500ms; 60/120 fps scrolling in recent files list; no ANRs

**Constraints**: 
- Must not exceed cyclomatic complexity of 20 (Constitution Principle VI)
- Must maintain module boundaries (:app depends on :data and :core; :data depends on :core only)
- Must use StateFlow for UI state (Constitution Principle II)
- Must use Logger from :core (Constitution Technical Constraints)
- Must use Koin for DI (Constitution Technical Constraints)

**Scale/Scope**: Single feature affecting MainActivity, MainViewModel, MainUiState, AppTopBar, and dialog components

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Check | Result |
|-----------|-------|--------|
| I. Modular Separation | New dialog in :app, uses :data RecentFilesRepository | ✅ PASS |
| II. Unidirectional Data Flow | ViewModel handles dialog state via StateFlow | ✅ PASS |
| III. Test-Driven Verification | Unit tests for ViewModel logic, Compose tests for dialog | ✅ PASS |
| IV. Freeplane Compatibility | No XML changes - pure UI/navigation feature | ✅ PASS |
| V. Performance/Simplicity | Reuse existing components, virtualized LazyColumn | ✅ PASS |
| VI. SOLID/Complexity | Small focused changes, complexity ≤ 20 | ✅ PASS |

No violations detected. All gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/007-back-nav-recent-files/
├── plan.md              # This file
├── research.md          # Phase 0 output (no clarifications needed)
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (no external contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
# Android multi-module structure (Option 3: Mobile app)
app/
├── src/main/java/fr/julien/quievreux/droidplane2/
│   ├── MainActivity.kt           # BackHandler logic, dialog rendering
│   ├── MainViewModel.kt          # Dialog state, back navigation logic
│   ├── MainUiState.kt            # New DialogType.RecentFiles
│   ├── ui/components/
│   │   ├── AppTopBar.kt          # Add "Recent Files" menu item
│   │   └── RecentFilesDialog.kt  # New dialog component (or extend StartupChooserDialog)
│   ├── ui/view/
│   │   └── RecentFilesDialog.kt  # Alternative location
│   └── data/
│       └── RecentFilesRepository.kt  # Existing - reused
```

**Structure Decision**: Standard Android multi-module. Feature changes confined to `:app` module (presentation layer) with read-only use of `:data`'s RecentFilesRepository.

## Complexity Tracking

No constitution violations - no tracking needed.

---

## Phase 0: Research

No NEEDS CLARIFICATION markers in specification. All technical decisions derived from existing codebase patterns:

- **Dialog Implementation**: Reuse/extend `StartupChooserDialog` (already has recent files list, New Mindmap, Browse, Help & Demo buttons)
- **State Management**: Add `RecentFiles` variant to `DialogType` sealed class in `MainUiState.kt`
- **Back Handling**: Modify `MainViewModel.upOrClose()` / `up(force: Boolean)` to check `hasUnsavedChangesState` and route to appropriate dialog
- **Toolbar Menu**: Add `AppTopBarAction.RecentFiles` to enum and handle in `AppTopBar.kt` dropdown menu
- **Exit App**: Call `Activity.finish()` from dialog confirm callback (same as `ExitConfirmationDialog`)

All patterns exist in codebase. No external research required.

---

## Phase 1: Design & Contracts

### data-model.md

```markdown
# Data Model: Back Navigation to Recent Files

## Entities

### RecentFile (existing - from :data module)
Represents a recently accessed mindmap file.
- uriString: String - Content URI or file URI
- displayName: String - User-friendly filename
- timestamp: Long - Milliseconds since epoch

### DialogType.RecentFiles (new - in MainUiState.kt)
Sealed class variant for the Recent Files dialog state.
- recentFiles: List<RecentFile> - Filtered accessible recent files
- onOpenRecent: (RecentFile) -> Unit - Callback when user selects a file
- onNewMindmap: () -> Unit - Callback for New Mindmap button
- onBrowse: () -> Unit - Callback for Browse button
- onOpenDemo: () -> Unit - Callback for Help & Demo button
- onExitApp: () -> Unit - Callback for Exit App button (closes app)
- onDismiss: () -> Unit - Callback for cancel/dismiss

## Relationships
- MainViewModel owns RecentFilesRepository instance
- MainUiState.DialogUiState holds current DialogType
- RecentFilesDialog reads recentFiles from DialogType.RecentFiles
- RecentFilesRepository persists to SharedPreferences

## Validation Rules
- Recent files list filtered by createStorageFileChecker (accessibility)
- Max 8 recent files (RecentFilesRepository.MAX_RECENTS)
- Dialog only shown when hasUnsavedChangesState == false
- Exit App action finishes Activity (irreversible)
```

### contracts/

No external contracts (APIs, CLIs, grammars) - this is an internal Android app feature. The only "contract" is the UI behavior defined in the spec's acceptance scenarios.

### quickstart.md

```markdown
# Quickstart: Back Navigation to Recent Files

## Prerequisites
- Android Studio Ladybug+ or IntelliJ with Android plugin
- Android SDK 35, Build Tools 35
- JDK 21
- Physical device or emulator (API 26+)

## Setup
```bash
cd /home/letroll/AndroidStudioProjects/droidplane
./gradlew :app:assembleDebug
```

## Test Scenarios

### Scenario 1: Back shows Recent Files (no unsaved changes)
1. Launch app → opens empty mindmap (or recent file)
2. Make NO changes to mindmap
3. Press system back button OR toolbar back button
4. **Expected**: Recent Files dialog opens with recent files list
5. Select a recent file → mindmap loads, dialog closes
6. Press "New Mindmap" → new empty mindmap, dialog closes
7. Press "Exit App" → app closes completely
8. Press dialog dismiss → returns to mindmap

### Scenario 2: Back shows Exit Confirmation (unsaved changes)
1. Launch app → opens mindmap
2. Edit a node (creates unsaved changes)
3. Press system back button OR toolbar back button
4. **Expected**: Exit Confirmation dialog opens (not Recent Files)
5. Confirm exit → app closes without saving
6. Cancel → returns to mindmap with changes preserved

### Scenario 3: Toolbar menu access
1. Launch app → opens mindmap
2. Open toolbar overflow menu (three-dot)
3. Select "Recent Files"
4. **Expected**: Recent Files dialog opens
5. Verify all buttons work as in Scenario 1

### Scenario 4: Large screen / tablet
1. Run on tablet/emulator with large screen
2. Open toolbar menu or equivalent navigation
3. Select "Recent Files"
4. **Expected**: Dialog opens and functions identically

## Validation Commands
```bash
# Unit tests
./gradlew :app:test

# UI tests (requires device/emulator)
./gradlew :app:connectedAndroidTest

# Lint
./gradlew :app:lint

# All checks
./gradlew check
```

## Expected Outcomes
- All existing tests pass (regression)
- New tests for RecentFilesDialog and back navigation logic pass
- Manual verification matches acceptance scenarios in spec
```