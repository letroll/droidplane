# Implementation Plan: 2D Canvas Drag & Drop, Long-Press Release Toolbar, and Legacy `NodeList` Removal

**Branch**: `014-nodelist-drag-drop` | **Date**: 2026-04-12 | **Spec**: [spec.md](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/specs/014-nodelist-drag-drop/spec.md)
**Input**: Feature specification from `/specs/014-nodelist-drag-drop/spec.md` (updated after `/speckit-clarify`)

## Summary

Fix the long-press action toolbar timing and implement full 2D drag-and-drop node reorganization on the primary interactive 2D Mind Map Canvas ([`MindMapCanvasScreen.kt`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasScreen.kt) and [`MindMapNodeCard.kt`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt)), while removing the obsolete list-view components (`NodeList.kt`, `MindMapScreen.kt`, `NodeListDragState.kt`, and their tests).

Specifically:
1. **Long-Press Release Toolbar (`US1`, `FR-001`..`FR-003`)**: Replace `.combinedClickable` on [`MindMapNodeCard`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt) with a custom pointer gesture detector (`awaitEachGesture`) so holding a finger stationary past the long-press timeout enters `CanvasDragPhase.ReadyToDrag` (elevating non-root cards) **without** opening [`NodeBottomToolbar`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/NodeBottomToolbar.kt); `onNodeLongPress(node)` is triggered **only when the user lifts their finger** without exceeding touch slop.
2. **2D Canvas Drag & Drop with Freeplane Drop Zones (`US2`, `FR-004`..`FR-010`, `FR-012`)**: Moving the finger past touch slop after the long-press timeout on any non-root node transitions to `CanvasDragPhase.Dragging`, translates the dragged card across the 2D canvas, auto-pans the canvas near viewport edges, and resolves Freeplane-style relative drop zones on any valid target node `T` (excluding self and descendants):
   - **Top 25% (`BEFORE_SIBLING`)**: Insert as sibling immediately before `T` (inheriting `T.position`).
   - **Center 50% (`AS_CHILD`)**: Attach as last child of `T` (unfolding `T` if collapsed; on the root node, left/right half sets `position = "left"` or `"right"`).
   - **Bottom 25% (`AFTER_SIBLING`)**: Insert as sibling immediately after `T` (inheriting `T.position`).
3. **Domain Tree Mutation (`:data`)**: Add `moveNode` to [`NodeManagerContract`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt) and [`NodeManager`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt) and expose `onMoveNode` in [`MainViewModel`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt) + [`MainActivity`](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt).
4. **Dead Code Removal (`FR-013`)**: Delete unused `NodeList.kt`, `MindMapScreen.kt`, `NodeListDragState.kt`, `NodeListTest.kt`, and `NodeListDragStateTest.kt`.

## Technical Context

**Language/Version**: Kotlin 2.2.10, Java 17 target
**Primary Dependencies**: Jetpack Compose (Material 3, Foundation `pointerInput` / `awaitEachGesture`), AndroidX Lifecycle (`ViewModel`, `StateFlow`), Hilt (`2.57.2`), Coroutines (`1.10.2`)
**Storage**: Freeplane `.mm` XML files parsed and serialized in-memory via `MindMapParser`, `MindMapSerializer`, and `NodeManager` (`:data` module)
**Testing**: JUnit 4 + `kotlin.test` + MockK (`:data` and `:app` unit tests), Compose UI Test (`createComposeRule` in `:app` `androidTest`)
**Target Platform**: Android SDK 26+ (minSdk 26, targetSdk/compileSdk 36)
**Project Type**: Multi-module Android mobile application (`:app`, `:data`, `:core`)
**Performance Goals**: 60 fps (`<=16ms` frame time) during 2D node dragging and viewport edge auto-panning; zero premature `NodeBottomToolbar` triggers while finger remains pressed down (`SC-001`)
**Constraints**: Root node cannot be dragged; dropping onto self or any descendant of the dragged node must be rejected (`FR-012`); canvas background pan/zoom (`detectTransformGestures`) must not conflict with active node dragging (`FR-004`); `@Preview` required on all Compose composables; `kotlin.test` prioritized for unit tests
**Scale/Scope**: Typical mind maps with 10–500 nodes rendered on `MindMapCanvasScreen` via `MindMapLayoutEngine`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Module Separation & Clean Architecture (`:app`, `:data`, `:core`)**: **PASS** — Tree re-parenting, sibling ordering, branch `position` normalization, and cycle prevention reside in `:data` (`NodeManagerContract` / `NodeManager.moveNode`), while 2D gesture disambiguation, drop zone hit-testing, and visual overlays reside in `:app` (`ui/mindmap/`).
- **II. Jetpack Compose & Material 3 UI Standards (incl. `@Preview` rule)**: **PASS** — All modified/new composables in `MindMapCanvasScreen.kt` and `MindMapNodeCard.kt` retain and include `@Preview` functions.
- **III. Unidirectional Data Flow (UDF)**: **PASS** — `MindMapCanvasDragState` holds ephemeral 2D drag offset and hovered drop zone during the gesture and invokes `onMoveNode` exactly once on finger release, updating `MainUiState.treeVersion` and `collapsedNodeIds`.
- **IV. Freeplane `.mm` Format Fidelity**: **PASS** — `NodeManager.moveNode` updates `parentNode`, `childNodes` order, `isFolded`, and root-child `position` (`"left"` / `"right"` / `null`), which `MindMapSerializer` writes directly to `.mm` XML.
- **V. Verification & Testing Discipline (`kotlin.test` rule)**: **PASS** — Unit tests in `NodeManagerTest`, `MindMapCanvasDragStateTest`, and `MainViewModelTest` use `kotlin.test` assertions (`assertEquals`, `assertTrue`, `assertFalse`, `assertNull`).

## Project Structure

### Documentation (this feature)

```text
specs/014-nodelist-drag-drop/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/
│   └── ui-contracts.md  # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
data/
├── src/main/java/fr/julien/quievreux/droidplane2/data/
│   ├── NodeManagerContract.kt                       # [MODIFY] Add moveNode(...) contract
│   └── NodeManager.kt                               # [MODIFY] Implement moveNode(...) with cycle check, sibling insertion, and position update
└── src/test/java/fr/julien/quievreux/droidplane2/data/
    └── NodeManagerTest.kt                           # [MODIFY] Add kotlin.test unit tests for moveNode (re-parent, sibling before/after, left/right root position, cycle prevention)

app/
├── src/main/java/fr/julien/quievreux/droidplane2/
│   ├── MainActivity.kt                              # [MODIFY] Wire viewModel::onMoveNode into MindMapCanvasScreen
│   ├── MainViewModel.kt                             # [MODIFY] Add onMoveNode(...) and remove unused onReorderChildNode if superseded
│   ├── ui/mindmap/
│   │   ├── MindMapCanvasDragState.kt                # [NEW] CanvasDragPhase, DropZoneType, CanvasDropTarget, MindMapCanvasDragState, resolveCanvasDropTarget, collectSubtreeIds
│   │   ├── MindMapCanvasScreen.kt                   # [MODIFY] Integrate rememberMindMapCanvasDragState, 2D drag offset, drop zone overlays, and edge auto-panning
│   │   └── MindMapNodeCard.kt                       # [MODIFY] Replace combinedClickable with custom pointer gesture (tap, double-tap, long-press release toolbar, 2D drag) + drop zone indicators & @Preview
│   └── ui/components/
│       ├── NodeList.kt                              # [DELETE] Unused legacy list component (FR-013)
│       ├── MindMapScreen.kt                         # [DELETE] Unused legacy list screen wrapper (FR-013)
│       └── NodeListDragState.kt                     # [DELETE] Unused list drag state (FR-013)
├── src/test/java/fr/julien/quievreux/droidplane2/
│   ├── ui/MainViewModelTest.kt                      # [MODIFY] Add kotlin.test unit tests for onMoveNode
│   ├── ui/mindmap/MindMapCanvasDragStateTest.kt     # [NEW] kotlin.test unit tests for CanvasDragPhase transitions, resolveCanvasDropTarget (Top/Center/Bottom + root left/right), and collectSubtreeIds
│   └── ui/components/NodeListDragStateTest.kt       # [DELETE] Obsolete list drag state unit tests (FR-013)
└── src/androidTest/java/fr/julien/quievreux/droidplane2/
    ├── ui/mindmap/MindMapCanvasScreenTest.kt        # [NEW] Compose UI tests for stationary long-press release toolbar and 2D drag-and-drop on MindMapCanvasScreen
    └── ui/components/NodeListTest.kt                # [DELETE] Obsolete NodeList UI tests (FR-013)
```

**Structure Decision**: Multi-module Android architecture (`:data` + `:app`). All 2D canvas UI components and state holders live under `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/`, while tree mutation invariants live in `data/src/main/java/fr/julien/quievreux/droidplane2/data/`.

## Phase 0: Research Summary

See [research.md](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/specs/014-nodelist-drag-drop/research.md) for full details:
1. **Gesture Disambiguation on `MindMapNodeCard`**: Custom `awaitEachGesture` pointer handler on `MindMapNodeCard` separates tap, double-tap, stationary long-press release (`ReadyToDrag` -> `UP` invokes `onNodeLongPress(node)`), and 2D node drag (`ReadyToDrag` -> movement `> touchSlop` enters `Dragging`, consuming pointer events so `MindMapCanvasScreen`'s `detectTransformGestures` does not pan the background).
2. **Freeplane-Style Drop Zones (`Top` / `Center` / `Bottom`)**: Pure hit-testing function `resolveCanvasDropTarget` divides non-root target nodes into Top 25% (`BEFORE_SIBLING`), Center 50% (`AS_CHILD`), and Bottom 25% (`AFTER_SIBLING`), and divides the root node horizontally into Left 50% (`AS_CHILD`, `position = "left"`) and Right 50% (`AS_CHILD`, `position = "right"`).
3. **Domain Tree Mutation (`NodeManager.moveNode`)**: Handles sibling reordering, cross-parent re-parenting, root branch switching (`"left"` / `"right"` / `null`), automatic unfolding of collapsed target parents, and cycle prevention.
4. **Viewport Edge Auto-Panning**: Frame-synced loop in `MindMapCanvasScreen` pans `panOffset` when the dragged pointer enters within `56.dp` of any viewport edge while keeping the dragged node locked under the pointer.
5. **Legacy `NodeList` Removal**: Confirmed zero production callers of `NodeList` and `MindMapScreen`; scheduled all 5 legacy files for deletion.

## Phase 1: Design & Contracts Summary

- **Data Model**: Documented in [data-model.md](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/specs/014-nodelist-drag-drop/data-model.md) (`CanvasDragPhase`, `DropZoneType`, `CanvasDropTarget`, `MindMapCanvasDragState`, and `NodeManager.moveNode` invariants).
- **Contracts**: Documented in [contracts/ui-contracts.md](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/specs/014-nodelist-drag-drop/contracts/ui-contracts.md).
- **Quickstart**: Documented in [quickstart.md](file:///Users/julienquievreux/Documents/dev_android/perso/letroll/droidplane/specs/014-nodelist-drag-drop/quickstart.md).

## Complexity Tracking

No constitution violations requiring justification.
