# Implementation Plan: Mindmap Viewing and Node Management

**Branch**: `001-mindmap-node-management` | **Date**: 2026-10-03 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/001-mindmap-node-management/spec.md`

## Summary

This feature delivers core mindmap browsing, searching, and node management capabilities for Droidplane. The technical approach resolves the previous state desynchronization issue by establishing `NodeManager` in `:data` as the singular source of truth for all node mutations (addition, text updates, deletion). The presentation layer in `:app` observes immutable `MainUiState` via Kotlin `StateFlow`, rendering styled hierarchical card listings with Jetpack Compose (`LazyColumn`) and providing reactive search traversal and atomic Freeplane `.mm` XML persistence.

## Technical Context

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
- Sub-100ms transition time when navigating between parent and child nodes for mindmaps with up to 2,000 nodes
- Sub-500ms initial search result response time across 1,000+ nodes
- Fluid 60/120 fps list scrolling using virtualized `LazyColumn` components

**Constraints**:
- Strictly offline-first operation
- Full backward and forward compatibility with desktop Freeplane `.mm` schemas
- Non-blocking main thread: All file I/O, XML parsing, and tree traversal execute on coroutine dispatchers (`Dispatchers.IO` / `Dispatchers.Default`)

**Scale/Scope**: Single mindmap documents containing up to 2,000+ nodes; 3 core user journeys (Navigation, Search, Editing).

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Gate Status | Compliance Details |
|---|---|---|
| **I. Modular Separation of Concerns** | **PASSED** | `:core` retains domain-agnostic helpers; `:data` exclusively manages Freeplane XML parsing, domain models, and `NodeManager`; `:app` handles Compose UI and ViewModels without leaking data parsing into the UI. |
| **II. Unidirectional Data Flow & State Consistency** | **PASSED** | Eliminated fragmented state mutation in `MainViewModel`. `NodeManager` acts as the single source of truth, updating nodes, parent references, and in-memory indexes simultaneously. `MainUiState` is emitted immutably via `StateFlow`. |
| **III. Test-Driven Verification (NON-NEGOTIABLE)** | **PASSED** | Data layer mutations and XML serialization are covered by JVM Kotest unit tests. ViewModels and state emissions are verified with Turbine and MockK. |
| **IV. Freeplane Format Compatibility & Data Integrity** | **PASSED** | Serialization preserves all existing Freeplane tags (`<font>`, `<icon>`, `<richcontent>`, `<arrowlink>`). Safe atomic writes via temporary files prevent document truncation or corruption. |
| **V. Performance, Responsiveness & Simplicity (YAGNI)** | **PASSED** | Search and navigation operate directly on in-memory indexes (`MindmapIndexes`). Avoids unnecessary SQLite/Room abstraction layers. Virtualized `LazyColumn` ensures smooth UI rendering. |

## Project Structure

### Documentation (this feature)

```text
specs/001-mindmap-node-management/
├── spec.md              # Feature specification
├── plan.md              # This implementation plan
├── research.md          # Phase 0 technical research & decisions
├── data-model.md        # Phase 1 domain and UI entity models
├── quickstart.md        # Phase 1 runnable validation scenarios
├── contracts/           # Phase 1 interface & UI contracts
│   ├── node-manager-contract.md
│   └── ui-contracts.md
└── checklists/
    └── requirements.md  # Specification quality checklist
```

### Source Code (repository root)

```text
core/
├── src/main/java/fr/julien/quievreux/droidplane2/core/
│   ├── log/Logger.kt                      # Centralized logging abstraction
│   ├── di/CoreKoinModule.kt               # Core DI definitions
│   └── ui/component/                      # Atomic Compose primitives (Button, Dialog)
└── src/test/java/                         # Core unit tests

data/
├── src/main/java/fr/julien/quievreux/droidplane2/data/
│   ├── model/
│   │   ├── Node.kt                        # Domain node model
│   │   ├── MindmapIndexes.kt              # In-memory O(1) lookup index
│   │   └── RichContent.kt                 # Rich text content definitions
│   ├── search/SearchManager.kt            # In-memory reactive search engine
│   ├── NodeManager.kt                     # Single source of truth (CRUD, XML I/O)
│   ├── XmlParseUtils.kt                   # Freeplane XML parser utilities
│   └── di/DataKoinModule.kt               # Data module DI definitions
└── src/test/java/                         # Data layer Kotest tests (NodeManagerTest)

app/
├── src/main/java/fr/julien/quievreux/droidplane2/
│   ├── MainViewModel.kt                   # Presentation ViewModel coordinating UI state
│   ├── MainUiState.kt                     # Immutable presentation state models
│   ├── ui/
│   │   ├── components/
│   │   │   ├── AppTopBar.kt               # Main toolbar with search & navigation
│   │   │   ├── MindMapScreen.kt           # Screen scaffold container
│   │   │   ├── NodeList.kt                # Virtualized child node LazyColumn
│   │   │   └── Cell.kt                    # Formatted node card composable
│   │   └── view/EditDescriptionDialog.kt  # In-app node description editor dialog
│   └── di/AppKoinModule.kt                # App module DI definitions
└── src/test/java/                         # App layer Kotest & Turbine tests (MainViewModelTest)
```

**Structure Decision**: Multi-module Android architecture (`:app` depends on `:data` and `:core`; `:data` depends on `:core`). Preserves clean layer separation and allows testing all parsing and mutation logic directly on the JVM without Android emulator overhead.

## Complexity Tracking

> **No architectural violations detected. All constitution principles satisfied.**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| *None* | N/A | Architecture strictly adheres to existing 3-module separation and YAGNI. |
