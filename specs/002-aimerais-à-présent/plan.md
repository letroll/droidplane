# Implementation Plan: Delete Mindmap Nodes

**Branch**: `002-delete-nodes` | **Date**: 2026-10-04 | **Spec**: spec.md

**Input**: Feature specification from `/specs/002-delete-nodes/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

This feature adds the ability to delete mindmap nodes (and their descendants) from within the Droidplane application. The implementation leverages the existing `NodeManager.deleteNode()` method in the `:data` module and wires it through `MainViewModel` to the node context menu in the UI. Key design decisions include: confirmation dialog showing descendant count, full session undo support via Ctrl+Z/menu, automatic link/arrow-link cleanup, and post-deletion navigation to parent's children list.

## Technical Context

<!--
  ACTION REQUIRED: Replace the content in this section with the technical details
  for the project. The structure here is presented in advisory capacity to guide
  the iteration process.
-->

**Language/Version**: Kotlin 2.1.0, Java 21 (JVM Target 21)

**Primary Dependencies**:
- Jetpack Compose (BOM `2024.09.03`, Material 3, Activity Compose `1.10.0`)
- Koin Dependency Injection (BOM `4.0.0`, `koin-android`, `koin-compose`, `koin-compose-viewmodel`)
- Kotlinx Coroutines & Flow (`1.9.0+`)

**Storage**: Local file system (Freeplane XML `.mm` documents) via standard Android content resolvers and file streams; temporary-file atomic staging for safe writes.

**Testing**:
- JVM Unit Tests: Kotest (`5.9.0`), MockK (`1.13.13`), Turbine (`1.2.0`), JUnit 5 (JUnit Platform runner)
- Android UI Tests: Compose UI Test (`androidx.compose.ui:ui-test-junit4`)

**Target Platform**: Android 8.0+ (API level 26 minimum SDK, API level 35 target SDK)

**Project Type**: Multi-module Android mobile application (`:app`, `:data`, `:core`)

**Performance Goals**:
- Sub-100ms deletion time for subtrees with 100+ nodes
- 100% of deleted nodes and descendants removed from hierarchy and indexes

**Constraints**:
- Strictly offline-first operation
- Full backward and forward compatibility with desktop Freeplane `.mm` schemas
- Non-blocking main thread: All file I/O, XML parsing, and tree traversal execute on coroutine dispatchers (`Dispatchers.IO` / `Dispatchers.Default`)
- Follow existing architecture: `:core` (utilities), `:data` (domain/parsing), `:app` (UI)

**Scale/Scope**: Single mindmap documents containing up to 2,000+ nodes; 3 user stories (Deletion, Confirmation, UI Integration).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate Status | Compliance Details |
|---|---|---|
| **I. Modular Separation of Concerns** | **PASSED** | Deletion logic remains in `NodeManager` (`:data`); UI in `:app`; no new cross-module dependencies. |
| **II. Unidirectional Data Flow & State Consistency** | **PASSED** | `MainViewModel` delegates to `NodeManager`; state updates flow through `StateFlow`; undo uses state snapshots. |
| **III. Test-Driven Verification (NON-NEGOTIABLE)** | **PASSED** | Deletion, undo, and link cleanup covered by JVM unit tests (`NodeManagerTest`); ViewModel tests with Turbine/MockK. |
| **IV. Freeplane Format Compatibility & Data Integrity** | **PASSED** | Deletion preserves document structure; atomic writes; link cleanup prevents dangling references. |
| **V. Performance, Responsiveness & Simplicity (YAGNI)** | **PASSED** | Deletion operates on in-memory indexes; no new dependencies; virtualized UI unaffected.

## Project Structure

### Documentation (this feature)

```text
specs/002-delete-nodes/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
core/
├── src/main/java/fr/julien/quievreux/droidplane2/core/
│   ├── log/Logger.kt
│   ├── di/CoreKoinModule.kt
│   └── ui/component/              # Atomic Compose primitives
└── src/test/java/

data/
├── src/main/java/fr/julien/quievreux/droidplane2/data/
│   ├── model/
│   │   ├── Node.kt
│   │   ├── MindmapIndexes.kt
│   │   └── RichContent.kt
│   ├── search/SearchManager.kt
│   ├── NodeManager.kt
│   ├── XmlParseUtils.kt
│   └── di/DataKoinModule.kt
└── src/test/java/                 # NodeManagerTest, etc.

app/
├── src/main/java/fr/julien/quievreux/droidplane2/
│   ├── MainViewModel.kt
│   ├── MainUiState.kt
│   ├── ui/
│   │   ├── components/
│   │   │   ├── AppTopBar.kt
│   │   │   ├── MindMapScreen.kt
│   │   │   ├── NodeList.kt
│   │   │   ├── Cell.kt
│   │   │   └── BarIcon.kt
│   │   └── view/                  # Dialogs
│   ├── model/ContextMenuDropDownItem.kt
│   └── di/AppKoinModule.kt
└── src/test/java/                 # MainViewModelTest
```

**Structure Decision**: Multi-module Android architecture (`:app` depends on `:data` and `:core`; `:data` depends on `:core`). No new modules needed - deletion feature extends existing components.

└── tests/

frontend/
├── src/
│   ├── components/
│   ├── pages/
│   └── services/
└── tests/

# [REMOVE IF UNUSED] Option 3: Mobile + API (when "iOS/Android" detected)
api/
└── [same as backend above]

ios/ or android/
└── [platform-specific structure: feature modules, UI flows, platform tests]
```

**Structure Decision**: 

## Additional Notes

No constitutional violations for this feature. Architecture follows existing patterns.------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |

No constitutional violations for this feature. Architecture follows existing patterns.
