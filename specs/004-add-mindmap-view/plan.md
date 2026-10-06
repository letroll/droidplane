# Implementation Plan: Mind Map View with View Mode Toggle

**Branch**: `004-add-mindmap-view` | **Date**: 2026-10-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/004-add-mindmap-view/spec.md`

## Summary

Implement a 2D spatial Mind Map visualization mode in Droidplane similar to desktop Freeplane, along with an options menu action allowing users to seamlessly toggle between the existing hierarchical List view and the new Mind Map view. The technical approach creates a native Jetpack Compose 2D canvas (`MindMapCanvasScreen`) driven by a deterministic layout engine (`MindMapLayoutEngine`) that positions nodes horizontally (left and right of root) connected by cubic Bezier branch curves. Visual node components (`MindMapNodeCard`) dynamically resize according to their text content, line wrapping, and interactive indicators (fold toggles, formatting). Central state management in `MainViewModel` and `MainUiState` tracks `DisplayMode`, active selection, and collapsed branches, ensuring zero data loss and instantaneous view transitions.

## Technical Context

**Language/Version**: Kotlin 2.1+ (JVM target 17/21, Java 21 toolchain)

**Primary Dependencies**: Jetpack Compose (BOM 2024.09.03), Material 3, AndroidX Lifecycle & ViewModel, Kotlin Coroutines & StateFlow, Koin (BOM 4.0.0+)

**Storage**: Freeplane XML (`.mm` format) managed in memory via `NodeManager` and persisted atomically on user save

**Testing**: JUnit, Kotest (`io.kotest`), MockK, kotlinx-coroutines-test, Turbine, Compose UI Test

**Target Platform**: Android (Min SDK 26, Target SDK 35)

**Project Type**: Multi-module Android application (`:core`, `:data`, `:app`)

**Performance Goals**: 60/120 fps fluid panning and pinch-to-zoom gestures; <16ms layout recalculation for subtrees; <1s instant view mode toggling

**Constraints**: Memory-efficient Compose layout passes; content-driven dynamic node card sizing (measuring text and wrapping within readable bounds); no UI state desynchronization across view modes; 100% data preservation of unsaved edits; strict compliance with Freeplane XML specification

**Scale/Scope**: Mindmaps spanning from single-node ideas to large hierarchies with hundreds or thousands of nodes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Modular Separation)**: PASS — All presentation logic (canvas composables, gestures, layout algorithms) resides strictly in `:app` (`fr.julien.quievreux.droidplane2.ui.mindmap`). Core utilities remain in `:core`, and Freeplane domain models and XML processing remain untouched in `:data`.
- **Principle II (Unidirectional Data Flow & State Consistency)**: PASS — `MainUiState` serves as the single source of truth for `displayMode`, `selectedNodeId`, and `collapsedNodeIds`. View transitions and user interactions emit events to `MainViewModel` and propagate downstream via `StateFlow<MainUiState>`.
- **Principle III (Test-Driven Verification)**: PASS — Comprehensive test coverage planned: pure unit tests for `MindMapLayoutEngine` (geometry, side partitioning, subtree bounding), ViewModel tests for view mode toggling and selection synchronization, and Compose UI previews for dark/light themes.
- **Principle IV (Freeplane Format Compatibility & Data Integrity)**: PASS — Reads existing node attributes (`TEXT`, `POSITION`, `BOLD`, `ITALIC`) without altering XML serialization or corrupting `.mm` files.
- **Principle V (Performance, Large Map Responsiveness & Simplicity)**: PASS — Pure Jetpack Compose canvas avoids legacy View-interop overhead. Avoids heavyweight 3rd-party dependencies in favor of standard Compose pointer inputs, canvas drawing, and standard Kotlin collections.

## Project Structure

### Documentation (this feature)

```text
specs/004-add-mindmap-view/
├── spec.md              # Feature specification
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output: technical choices & trade-offs
├── data-model.md        # Phase 1 output: UI models & state transitions
├── quickstart.md        # Phase 1 output: end-to-end validation scenarios
├── contracts/           # Phase 1 output: UI and ViewModel contracts
│   ├── ui-contracts.md
│   └── viewmodel-contract.md
└── checklists/
    └── requirements.md  # Quality checklist
```

### Source Code (repository root)

```text
app/
└── src/
    ├── main/
    │   ├── java/fr/julien/quievreux/droidplane2/
    │   │   ├── MainActivity.kt                       # Route content between List and MindMap view
    │   │   ├── MainViewModel.kt                      # Manage displayMode, selection, and branch toggles
    │   │   ├── MainUiState.kt                        # Add displayMode, selectedNodeId, collapsedNodeIds
    │   │   ├── model/
    │   │   │   └── DisplayMode.kt                    # LIST vs MIND_MAP enum
    │   │   └── ui/
    │   │       ├── components/
    │   │       │   ├── AppTopBar.kt                  # Add ToggleDisplayMode menu item and action
    │   │       │   └── MindMapScreen.kt              # Scaffold container
    │   │       └── mindmap/
    │   │           ├── MindMapCanvasScreen.kt        # 2D canvas with pan/zoom gestures
    │   │           ├── MindMapLayoutEngine.kt        # Freeplane horizontal tree layout calculator
    │   │           ├── MindMapNodeCard.kt            # Composable node card with fold indicator
    │   │           └── MindMapBranchDrawer.kt        # Cubic Bezier branch curve painter
    │   └── res/values/
    │       └── strings.xml                           # Localized menu strings (Mind Map View / List View)
    └── test/
        └── java/fr/julien/quievreux/droidplane2/
            ├── MainViewModelTest.kt                  # Unit tests for display mode toggling & selection
            └── ui/mindmap/
                └── MindMapLayoutEngineTest.kt        # Layout calculation, partitioning, and bounds tests
```

## Phase 0: Outline & Research

Phase 0 research is complete and documented in [research.md](./research.md). Key resolutions:
1. **Native Compose Canvas Chosen**: Evaluated `com.github.hegleB:MindMapView` and rejected it due to single-sided layout limitation and hardcoded root ID crash. Pure Compose canvas delivers authentic Freeplane two-sided branching, `@Preview` compliance, and seamless Material 3 integration.
2. **Display Mode State in MainUiState**: Managed via `DisplayMode` enum in `MainUiState`. Menu action `ToggleDisplayMode` toggles the active mode with 0ms delay and zero risk of data loss.
3. **Deterministic Layout Engine & Content-Driven Node Sizing**: Recursive $O(N)$ calculation partitions first-level children into left/right branches based on `Node.position`. The layout engine and graphical node component (`MindMapNodeCard`) dynamically compute node dimensions based on text length, font styling, multiline wrapping within maximum width constraints, and interactive indicators (fold toggles), centering parents vertically relative to their subtrees and connecting nodes via cubic Bezier curves.
4. **Hardware-Accelerated Pan & Zoom**: Uses `Modifier.pointerInput` with `detectTransformGestures` clamped between 0.25x and 3.0x scale.

## Phase 1: Design & Contracts

Phase 1 design is complete:
- **Data Model**: Specified in [data-model.md](./data-model.md) (`DisplayMode`, `MindMapNodeLayout` with dynamic content-measured width/height, `BranchConnector`, `MindMapCanvasViewport`).
- **Contracts**: Defined in [contracts/ui-contracts.md](./contracts/ui-contracts.md) (including `MindMapNodeCard` content-driven auto-resizing within minimum and maximum bounds) and [contracts/viewmodel-contract.md](./contracts/viewmodel-contract.md).
- **Validation Guide**: Documented in [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Evaluation

All design artifacts fully comply with the Droidplane Constitution:
- **Principle I**: Strict module encapsulation maintained.
- **Principle II**: Single source of truth preserved in `MainViewModel` and `MainUiState`.
- **Principle III**: Pure unit tests designed for `MindMapLayoutEngine` and `MainViewModel`; composables support previews.
- **Principle IV**: Zero changes to XML parsing or serialization; Freeplane attributes faithfully visualized.
- **Principle V**: Lean implementation avoiding superfluous dependencies; hardware-accelerated Compose rendering.
