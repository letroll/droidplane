# Implementation Plan: Create Empty Mindmap with Demo Access

**Branch**: `005-create-empty-mindmap` | **Date**: 2026-10-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/005-create-empty-mindmap/spec.md`

## Summary

Enable users to create a clean, empty mindmap directly from the top bar options menu, while preserving access to the bundled tutorial/demo mindmap (`example.mm`). Additionally, on app startup, display a startup chooser offering to create a new empty mindmap, pick from recently edited files (filtered to those still existing on storage), browse for another file, or load the demo. The implementation introduces `NodeManager.createNewMindmap(title)` to programmatically initialize an in-memory document with a single root node and clean index tables. `RecentFilesRepository` tracks previously accessed files using persistent preferences. `MainViewModel` orchestrates document creation, resets navigation history, selection, and dirty state, and introduces `DiscardConfirmation` and `StartupChooser` dialogs. The top bar menu in `AppTopBar.kt` is updated with a "New Mindmap" action and an enhanced "Help & Demo" item.

## Technical Context

**Language/Version**: Kotlin 2.1+ (JVM target 17/21, Java 21 toolchain)

**Primary Dependencies**: Jetpack Compose (BOM 2024.09.03), Material 3, AndroidX Lifecycle & ViewModel, Kotlin Coroutines & StateFlow, Koin (BOM 4.0.0+)

**Storage**: In-memory `NodeManager` tree initialized with a programmatic root node; persisted to Freeplane XML (`.mm` format) on user save

**Testing**: JUnit, Kotest (`io.kotest`), MockK, kotlinx-coroutines-test, Turbine

**Target Platform**: Android (Min SDK 26, Target SDK 35)

**Project Type**: Multi-module Android application (`:core`, `:data`, `:app`)

**Performance Goals**: <100ms instant creation of new mindmaps; 0ms lag on menu navigation; 100% preservation of unsaved edits via guard dialog

**Constraints**: Strict compliance with Freeplane XML specification; no memory leaks or stale node references when resetting the document in memory

**Scale/Scope**: Creation and management of empty documents ready for immediate authoring

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Modular Separation)**: PASS — Core data structures and programmatic node tree generation reside in `:data` (`NodeManager`). Presentation state, guard dialogs, and menu wiring reside in `:app` (`MainViewModel`, `AppTopBar`, `MainActivity`).
- **Principle II (Unidirectional Data Flow & State Consistency)**: PASS — `MainViewModel` remains the single source of truth for active document state, dirty flags, dialogs, and navigation stacks.
- **Principle III (Test-Driven Verification)**: PASS — Unit tests planned for `NodeManager.createNewMindmap` in `:data` and `MainViewModel` new map request / discard confirmation / demo loading in `:app`.
- **Principle IV (Freeplane Format Compatibility & Data Integrity)**: PASS — Discard confirmation guard prevents accidental loss of unsaved edits; newly created mindmaps serialize to valid Freeplane XML upon save.
- **Principle V (Performance, Large Map Responsiveness & Simplicity)**: PASS — Programmatic initialization avoids unnecessary file I/O or XML parsing overhead when starting empty maps.

## Project Structure

### Documentation (this feature)

```text
specs/005-create-empty-mindmap/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output: technical choices & trade-offs
├── data-model.md        # Phase 1 output: data models & state transitions
├── quickstart.md        # Phase 1 output: end-to-end validation scenarios
├── contracts/           # Phase 1 output: UI and ViewModel contracts
│   ├── ui-contracts.md
│   └── viewmodel-contract.md
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code (repository root)

```text
data/
└── src/
    ├── main/java/fr/julien/quievreux/droidplane2/data/
    │   ├── NodeManagerContract.kt                   # Add createNewMindmap interface method
    │   └── NodeManager.kt                           # Implement programmatic root initialization
    └── test/java/fr/julien/quievreux/droidplane2/data/
        └── NodeManagerTest.kt                       # Unit tests for createNewMindmap

app/
└── src/
    ├── main/
    │   ├── java/fr/julien/quievreux/droidplane2/
    │   │   ├── MainActivity.kt                      # Wire NewMindmap, StartupChooser, and DiscardConfirmation
    │   │   ├── MainViewModel.kt                     # Manage new mindmap requests, demo load & recent files
    │   │   ├── MainUiState.kt                       # Add DiscardConfirmation and StartupChooser to DialogType
    │   │   ├── model/
    │   │   │   └── RecentFile.kt                    # Value object for recent file entries
    │   │   ├── data/
    │   │   │   └── RecentFilesRepository.kt         # Persistent storage and accessibility checker for recent files
    │   │   └── ui/
    │   │       ├── components/
    │   │       │   └── AppTopBar.kt                 # Add NewMindmap action and menu items
    │   │       └── view/
    │   │           ├── DiscardConfirmationDialog.kt # Guard dialog for unsaved changes
    │   │           └── StartupChooserDialog.kt      # Startup dialog for New Map vs. Recent Files
    │   └── res/values/
    │       └── strings.xml                          # Add strings (new_mindmap, recent_files, browse, help_demo)
    └── test/java/fr/julien/quievreux/droidplane2/
        ├── MainViewModelTest.kt                     # Unit tests for new map, demo, and startup chooser
        └── data/
            └── RecentFilesRepositoryTest.kt         # Unit tests for recent files persistence and filtering
```

## Phase 0: Outline & Research

Phase 0 research is complete and documented in [research.md](./research.md). Key resolutions:
1. **Programmatic Root Node Initialization**: `NodeManager.createNewMindmap(title)` builds a clean root `Node` in memory, resets internal index tables, updates `_allNodes`, and clears URI state without XML parsing overhead.
2. **Dedicated Demo Access**: Menu item "Help & Demo" enables reloading `R.raw.example` at any point.
3. **Unsaved Changes Guard**: `DialogType.DiscardConfirmation` prevents data loss when switching documents or creating a new map while dirty.
4. **Startup Chooser & Recent Files Tracking**: `RecentFilesRepository` stores recently opened/saved file entries; on app launch from launcher, `StartupChooserDialog` prompts user to create an empty map or open an existing recent file.

## Phase 1: Design & Contracts

Phase 1 design is complete:
- **Data Model**: Specified in [data-model.md](./data-model.md) (`DialogType.DiscardConfirmation`, `DialogType.StartupChooser`, `RecentFile`, `NodeManagerContract.createNewMindmap`).
- **Contracts**: Defined in [contracts/ui-contracts.md](./contracts/ui-contracts.md) and [contracts/viewmodel-contract.md](./contracts/viewmodel-contract.md).
- **Validation Guide**: Documented in [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Evaluation

All design artifacts fully comply with the Droidplane Constitution:
- **Principle I**: Clean boundary between `:data` and `:app` preserved.
- **Principle II**: Immutable UI state and UDF preserved in `MainViewModel`.
- **Principle III**: Pure JVM unit tests planned across `:data` and `:app`.
- **Principle IV**: Protection against data loss enforced via confirmation dialogues.
- **Principle V**: Lean implementation avoiding redundant abstractions.
