# Implementation Plan: Add Child Node to a Node in NodeList

**Branch**: `003-add-child-in-nodelist` | **Date**: 2026-10-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-add-child-in-nodelist/spec.md`

## Summary

Enable users to add a child node directly to any item listed in `NodeList` via its context menu. The technical approach wires the selected child node from `DialogType.AddChildNode(parentNode)` through to `MainViewModel.addNode(newValue, parentNode)`, ensures `NodeManager.addNodeToMindmap` propagates the updated child and ancestor chain up to `rootNode`, and updates the active `nodeCurrentlyDisplayed` so that child indicators and navigation reflect the new hierarchy immediately.

## Technical Context

**Language/Version**: Kotlin 2.0+ (JVM target 21, Java 21 toolchain)

**Primary Dependencies**: Jetpack Compose, Material 3, AndroidX Lifecycle / ViewModel, Kotlin Coroutines & StateFlow, Koin

**Storage**: Freeplane XML (`.mm` format) managed in memory via `NodeManager` and persisted on user save

**Testing**: JUnit, Kotest (`io.kotest`), MockK, kotlinx-coroutines-test, Turbine

**Target Platform**: Android (Min SDK 26, Target SDK 35)

**Project Type**: Multi-module Android application (`:core`, `:data`, `:app`)

**Performance Goals**: Instant dialog response (<16ms frame budget), 60/120 fps smooth scrolling in Compose `LazyColumn`

**Constraints**: Immutability of domain state in UI; atomic updates in `NodeManager`; no architectural leakage across module boundaries

**Scale/Scope**: Mindmaps with hundreds to thousands of nodes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Modular Separation)**: PASS — UI logic remains strictly in `:app` (`MainActivity`, `MainViewModel`), Freeplane tree data manipulation remains in `:data` (`NodeManager`), and no illegal cross-dependencies are introduced.
- **Principle II (Unidirectional Data Flow)**: PASS — State mutations flow through `MainViewModel.addNode` to `NodeManager` and propagate via `StateFlow<MainUiState>`.
- **Principle III (Test-Driven Verification)**: PASS — Dedicated unit tests in `:data` and integration tests in `:app` verify the new functionality and guard against regression.
- **Principle IV (Freeplane Compatibility)**: PASS — Standard `.mm` node XML structure and timestamp attributes are preserved without introducing proprietary tags.
- **Principle V (Performance & Simplicity)**: PASS — Background execution on `Dispatchers.IO`, standard library collections, and existing Compose dialogs are reused without unnecessary dependencies.

## Project Structure

### Documentation (this feature)

```text
specs/003-add-child-in-nodelist/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── node-manager-contract.md
│   └── ui-contracts.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
data/
└── src/
    ├── main/java/fr/julien/quievreux/droidplane2/data/
    │   └── NodeManager.kt          # Propagate ancestor updates in addNodeToMindmap
    └── test/java/fr/julien/quievreux/droidplane2/data/
        └── NodeManagerTest.kt      # Unit tests for non-root child node addition

app/
└── src/
    ├── main/java/fr/julien/quievreux/droidplane2/
    │   ├── MainActivity.kt         # Pass dialog.parentNode to viewModel.addNode()
    │   └── MainViewModel.kt        # Support target parentNode in addNode()
    └── test/java/fr/julien/quievreux/droidplane2/
        └── MainViewModelTest.kt    # Integration tests for adding child to listed node
```

## Phase 0: Outline & Research

Phase 0 research is complete and documented in [research.md](./research.md). Key resolutions:
1. Target `parentNode` is passed directly from `DialogType.AddChildNode` to `MainViewModel.addNode`.
2. `NodeManager.addNodeToMindmap` propagates updated parent nodes through ancestors to keep `rootNode` and parent listings synchronized.
3. The active screen stays on the current parent while indicators update, and subsequent navigation into the target node exposes the new child.

## Phase 1: Design & Contracts

Phase 1 design is complete:
- **Data Model**: Documented in [data-model.md](./data-model.md).
- **Contracts**: Defined in [contracts/node-manager-contract.md](./contracts/node-manager-contract.md) and [contracts/ui-contracts.md](./contracts/ui-contracts.md).
- **Quickstart Guide**: Documented in [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Evaluation

All design decisions strictly comply with the Droidplane Constitution:
- Module boundaries between `:data` and `:app` are cleanly preserved.
- Unidirectional state management and single source of truth are maintained.
- Full test coverage is planned across `:data` and `:app` test suites.
- Freeplane XML serialization compatibility is 100% preserved.
