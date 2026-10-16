# Implementation Plan: Codebase Audit Remediation & Quality Hardening

**Branch**: `012-audit-remediation` | **Date**: 2026-10-16 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/012-audit-remediation/spec.md`

## Summary

Remediate all 17 verified findings from the repository-wide technical audit across `:core`, `:data`, and `:app`:
1. **Release & Build Hygiene (`US1`)**: Isolate `KStringSpec` and `FakeDataSource` into Gradle `testFixtures`, move all test frameworks (Kotest, MockK, JUnit, Espresso, Turbine) out of production `main` configurations, enable R8 shrinking (`isMinifyEnabled = true`) with `app/proguard-rules.pro`, and remove 7 unused library dependencies.
2. **XML Serialization & Rich Content Fidelity (`US2`)**: Return early on invalid paths/filenames in `NodeManager.serializeMindmap`, replace destructive line-by-line `<![CDATA[` / `]]>` stripping with atomic `tempFile` replacement, and escape XML entities using `StringBuilder` in `NodeUtilsDefaultImpl.loadRichContent`.
3. **Lifecycle Safety & Off-Main-Thread I/O (`US3`)**: Replace `FileRegister` in `MainViewModel` with observable `MainUiState.pendingSaveTempFile` state, stream `ContentResolver` saves and startup `refreshRecentFiles` checks on `Dispatchers.IO`, wrap `state.value.leaving` handling in `LaunchedEffect`, and use `FileProvider` for opening relative files.
4. **Tree Consistency & Layout Performance (`US4`)**: Complete `Node.equals`/`hashCode` for all styling/hook fields, invoke `updateDescendantParents` in `propagateUpdatedParentToRoot`, eliminate $O(N^2)$ `_allNodes.update` calls during XML load, memoize subtree heights in `MindMapLayoutEngine`, decouple `selectedNodeId` from layout recomputation in `MindMapCanvasScreen`, and clean up `SearchManager` logging.
5. **Dead Code Removal (`US5`)**: Delete 6 dead legacy source/layout files (`MindMapViewModel.kt`, `MindMapUiState.kt`, `EditDescriptionViewModel.kt`, `ui/model/Node.kt`, `PermissionUtils.kt`, `activity_main.xml`, `dialog_edit_description.xml`), remove dead helper functions, align `:core` Compose BOM with the version catalog, and clean up `MainActivity.kt` imports and annotations.

## Technical Context

**Language/Version**: Kotlin 2.1.10 (JVM target 21, Java 21 toolchain)

**Primary Dependencies**: Android Gradle Plugin 8.7.3, Jetpack Compose BOM 2025.02.00 (Material 3), Koin BOM 4.0.0, AndroidX Core/Activity/Lifecycle/DataStore, XmlPullParser

**Storage**: Local filesystem & Android Storage Access Framework (`ContentResolver` document URIs) for Freeplane `.mm` XML files; Preferences DataStore for recent files

**Testing**: JUnit 5 Platform, Kotest 5.9.1 (`KStringSpec`), MockK 1.13.13, Turbine 1.2.0, Compose UI Test (`androidTest`)

**Target Platform**: Android Min SDK 26 (Android 8.0) — Target/Compile SDK 35 (Android 15)

**Project Type**: Multi-module Android mobile application (`:app`, `:data`, `:core`)

**Performance Goals**: 60/120 fps UI rendering on 2D canvas and node lists; zero main-thread disk/URI I/O; $O(N)$ mind map XML loading and single-pass layout height memoization

**Constraints**: Offline-first, 100% Freeplane `.mm` format round-trip fidelity, cyclomatic complexity $\le 20$ per method

**Scale/Scope**: 3 Gradle modules (`:core`, `:data`, `:app`), mind maps ranging from 10 to 5,000+ nodes

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-Design Check | Post-Design Check | Notes |
|-----------|------------------|-------------------|-------|
| **I. Modular Separation of Concerns** (`:core`, `:data`, `:app`) | ✅ PASS | ✅ PASS | Moving `KStringSpec` to `:core` `testFixtures` and `FakeDataSource` to `:data` `testFixtures` strengthens module boundaries and removes test leakage from `:core` and `:data` main artifacts. |
| **II. Unidirectional Data Flow & State Consistency** | ✅ PASS | ✅ PASS | Replacing `FileRegister` callback interface with `MainUiState.pendingSaveTempFile` and wrapping `leaving` in `LaunchedEffect` brings `MainActivity`/`MainViewModel` into strict UDF compliance. |
| **III. Test-Driven Verification (NON-NEGOTIABLE)** | ✅ PASS | ✅ PASS | All XML parser/serializer fixes, `Node.equals`, deep parent propagation, and `MainViewModel` changes are covered by Kotest unit tests in `:data` and `:app`. |
| **IV. Freeplane Format Compatibility & Data Integrity** | ✅ PASS | ✅ PASS | Atomic save via `tempFile` move is preserved while removing destructive CDATA string replacement and fixing HTML entity escaping in `<richcontent>`. |
| **V. Performance, Large Map Responsiveness & Simplicity** | ✅ PASS | ✅ PASS | Moves `writeMindMapToUri` and `refreshRecentFiles` to `Dispatchers.IO`, makes XML node loading $O(N)$, memoizes 2D layout subtree heights, and removes 7 unused dependencies and 6 dead files. |
| **VI. SOLID Principles & Cyclomatic Complexity ($\le 20$)** | ✅ PASS | ✅ PASS | Decomposing `loadRichContent` entity escaping and removing dead responsibilities reduces class/method complexity across `NodeManager`, `MainViewModel`, and `MainActivity`. |

## Project Structure

### Documentation (this feature)

```text
specs/012-audit-remediation/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── module-and-state-contracts.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
gradle/
└── libs.versions.toml

core/
├── build.gradle.kts
├── src/main/java/fr/julien/quievreux/droidplane2/core/
│   ├── AppVersion.kt
│   ├── IntentHelper.kt
│   └── log/
└── src/testFixtures/java/fr/julien/quievreux/droidplane2/core/testutils/
    └── KStringSpec.kt

data/
├── build.gradle.kts
├── src/main/java/fr/julien/quievreux/droidplane2/data/
│   ├── NodeManager.kt
│   ├── NodeUtilsDefaultImpl.kt
│   ├── SearchManager.kt
│   ├── model/Node.kt
│   └── repository/RecentFilesRepository.kt
├── src/testFixtures/java/fr/julien/quievreux/droidplane2/data/
│   └── FakeDataSource.kt
└── src/test/java/fr/julien/quievreux/droidplane2/data/
    ├── RichContentXmlTest.kt
    └── FreeplaneXmlRoundTripTest.kt

app/
├── build.gradle.kts
├── proguard-rules.pro
├── src/main/
│   ├── AndroidManifest.xml
│   ├── res/xml/file_paths.xml
│   └── java/fr/julien/quievreux/droidplane2/
│       ├── MainActivity.kt
│       ├── MainViewModel.kt
│       └── ui/
│           ├── state/MainUiState.kt
│           └── mindmap/
│               ├── MindMapCanvasScreen.kt
│               └── MindMapLayoutEngine.kt
├── src/test/java/fr/julien/quievreux/droidplane2/
└── src/androidTest/java/fr/julien/quievreux/droidplane2/
```

**Structure Decision**: Retain the existing 3-module Android architecture (`:core`, `:data`, `:app`) defined in Constitution Principle I, adding standard Android Gradle `src/testFixtures` source sets in `:core` and `:data` to isolate shared test utilities from production code.

## Complexity Tracking

> No Constitution Check violations exist; all changes directly enforce and restore compliance with Principles I–VI.
