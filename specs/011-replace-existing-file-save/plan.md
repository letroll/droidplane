# Implementation Plan: Replace Existing File on Save (`011-replace-existing-file-save`)

**Branch**: `011-replace-existing-file-save` | **Date**: 2026-10-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/011-replace-existing-file-save/spec.md`

## Summary

When editing an existing `.mm` mindmap file and saving via the Android system file picker (`Intent.ACTION_CREATE_DOCUMENT`), Android's `DocumentsProvider` currently creates a new empty duplicate file with an incremented suffix (`filename (1).mm`, `filename (2).mm`) instead of replacing the original file.

This plan resolves the issue by:
1. Persisting both read and write URI permissions (`FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION`) when opening documents, and tracking both `currentMindMapUri` and `currentMindMapDisplayName` in `NodeManager` / `MainViewModel`.
2. Pre-filling `Intent.EXTRA_TITLE` with the exact existing filename and passing `DocumentsContract.EXTRA_INITIAL_URI` when launching the system file picker.
3. Introducing a pure-Kotlin `SaveTargetResolver` to detect when the file picker returns an auto-incremented `(n)` duplicate URI for the currently open file, presenting an `OverwriteConfirmationDialog` (`DialogType.OverwriteConfirmation` with `@Preview`), overwriting `originalUri` in place using `"wt"` truncation mode upon confirmation, and deleting the temporary `(n)` placeholder document via `DocumentsContract.deleteDocument` on both Confirm and Cancel.
4. Updating `currentMindMapUri` and `currentMindMapDisplayName` whenever a new file or "Save As" file is saved so subsequent saves in the same session target the newly saved document.

## Technical Context

**Language/Version**: Kotlin 2.1.10 / JDK 17  
**Primary Dependencies**: Jetpack Compose (Material 3), AndroidX Activity Result API (`ActivityResultContracts.CreateDocument`, `OpenDocument`), Android Storage Access Framework (`DocumentsContract`, `ContentResolver`), Koin 4.0.2 (DI), Coroutines (`StateFlow`)  
**Storage**: Android SAF `content://` URIs, local app cache (`cacheDir`) for atomic XML serialization staging, `SharedPreferences` (`RecentFilesRepository`)  
**Testing**: JUnit 4 / JUnit Platform, Kotest (`KStringSpec`), `kotlin.test` assertions, MockK  
**Target Platform**: Android API 26+ (Android 8.0+ through Android 15 / API 35)  
**Project Type**: Multi-module Android mobile application (`:core`, `:data`, `:app`)  
**Performance Goals**: Instantaneous dialog presentation (<16ms UI frame budget); file overwrite + placeholder cleanup <100ms on local flash storage  
**Constraints**: Must work within Android Scoped Storage / SAF constraints without requesting broad `MANAGE_EXTERNAL_STORAGE` permissions; must never corrupt existing `.mm` files if save is cancelled or fails  
**Scale/Scope**: Touches `:core` (1 file), `:data` (2 files + 1 test file), and `:app` (5 source files + strings + 2 test files)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-Design Check | Post-Design Check | Notes |
|-----------|------------------|-------------------|-------|
| **I. Modular Architecture** (`:core`, `:data`, `:app`) | PASS | PASS | `:core` provides generic SAF/Activity extensions (`ActivityExtensions.kt`), `:data` manages active mindmap URI & display name state (`NodeManager.kt`), and `:app` handles UI state (`MainUiState.kt`), `SaveTargetResolver`, `OverwriteConfirmationDialog`, and `MainActivity` orchestration. No reverse dependencies. |
| **II. Unidirectional Data Flow & Compose UI** | PASS | PASS | `OverwriteConfirmation` is modeled as a `MainUiState.DialogType` variant emitted via `MainViewModel.uiState: StateFlow<MainUiState>`. `OverwriteConfirmationDialog` is a stateless `@Composable` with `@Preview`. |
| **III. Test-Driven Verification** | PASS | PASS | Unit tests in `SaveTargetResolverTest.kt`, `MainViewModelTest.kt`, and `NodeManagerTest.kt` using `kotlin.test` and `KStringSpec` cover all `(n)` filename patterns, overwrite confirmation state transitions, and URI/filename tracking. |
| **IV. Freeplane Format Compatibility & Data Integrity** | PASS | PASS | Serializes to a staging file in `cacheDir` first, then writes to `originalUri` with `"wt"` (write + truncate) mode so existing files are never left with trailing XML garbage or half-written state on cancel. |
| **V. Defensive Logging & Error Handling** | PASS | PASS | Uses `AndLogger` and catches `SecurityException` / `FileNotFoundException` when taking persistable write permissions, deleting temporary placeholder URIs, or opening output streams, with graceful fallback. |
| **VI. Maintainability, SOLID & Complexity** | PASS | PASS | `SaveTargetResolver` isolates filename/URI resolution logic (Single Responsibility) with low cyclomatic complexity (<10 per function). |

## Project Structure

### Documentation (this feature)

```text
specs/011-replace-existing-file-save/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── file-save-contract.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
core/
└── src/main/java/fr/julien/quievreux/droidplane2/core/extensions/
    └── ActivityExtensions.kt                        # Update OpenDocument flags (read+write), EXTRA_INITIAL_URI support, and SAF document deletion helper

data/
├── src/main/java/fr/julien/quievreux/droidplane2/data/
│   ├── NodeManagerContract.kt                       # Expose map URI & display name methods if needed
│   └── NodeManager.kt                               # Track currentMindMapDisplayName alongside currentMindMapUri; reset on createNewMindmap()
└── src/test/java/fr/julien/quievreux/droidplane2/data/
    └── NodeManagerTest.kt                           # Unit tests for display name & URI tracking/reset

app/
├── src/main/java/fr/julien/quievreux/droidplane2/
│   ├── MainActivity.kt                              # Persist read+write URI permissions, resolve SaveResolution, write "wt" & delete (n) placeholder
│   ├── MainUiState.kt                               # Add DialogType.OverwriteConfirmation
│   ├── MainViewModel.kt                             # Add showOverwriteConfirmation(), pass initialUri to FileRegister, track display name
│   ├── helper/
│   │   ├── FileRegister.kt                          # Add optional initialUri parameter to registerFile
│   │   └── SaveTargetResolver.kt                    # NEW: Pure Kotlin resolver for WriteNewFile / DirectOverwrite / ConfirmOverwrite & (n) regex matching
│   └── ui/view/
│       └── OverwriteConfirmationDialog.kt           # NEW: Stateless Material 3 confirmation dialog composable + @Preview
├── src/main/res/
│   ├── values/strings.xml                           # Add overwrite confirmation dialog strings (EN)
│   └── values-fr/strings.xml                        # Add overwrite confirmation dialog strings (FR)
└── src/test/java/fr/julien/quievreux/droidplane2/
    ├── MainViewModelTest.kt                         # Unit tests for getSaveFilename(), launchSaveFile(), and OverwriteConfirmation dialog state
    └── helper/
        └── SaveTargetResolverTest.kt                # NEW: Unit tests (kotlin.test) for all (n) suffix patterns and SaveResolution cases
```

**Structure Decision**: Multi-module Android architecture (`:core`, `:data`, `:app`) following existing package conventions (`fr.julien.quievreux.droidplane2.*`).

## Complexity Tracking

No Constitution Check violations.
