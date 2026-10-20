# Implementation Plan: Auto Release Changelog & In-App Update Preview

**Branch**: `013-auto-release-changelog` | **Date**: 2026-10-20 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/013-auto-release-changelog/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command; its definition describes the execution workflow.

## Summary

Implement automated GitHub release note generation from conventional commits and enhance the in-app update dialog to display the full changelog before download. The solution has two parts: (1) a GitHub Actions workflow that generates structured release notes on tag push using conventional commit parsing, and (2) modifications to the existing `UpdateCheckerService` and update dialog to fetch and display the GitHub release body (changelog) with scrollable content and "Update Now/Later" actions.

## Technical Context

**Language/Version**: Kotlin 2.1.10 (JVM target 21, Java 21 toolchain), Android Gradle Plugin 8.7.3

**Primary Dependencies**: Jetpack Compose BOM 2025.02.00 (Material 3), Koin BOM 4.0.0, AndroidX Core/Activity/Lifecycle, GitHub REST API (via HTTP client), GitHub Actions for CI/CD

**Storage**: No local storage for changelog (fetched on-demand from GitHub API); existing Preferences DataStore for recent files

**Testing**: JUnit 5 Platform, Kotest 5.9.1 (`KStringSpec`), MockK 1.13.13, Turbine 1.2.0, Compose UI Test (`androidTest`)

**Target Platform**: Android Min SDK 26 (Android 8.0) — Target/Compile SDK 35 (Android 15)

**Project Type**: Multi-module Android mobile application (`:core`, `:data`, `:app`)

**Performance Goals**: Update check + changelog fetch < 3 seconds; changelog display < 5000 chars without truncation; zero manual intervention for release notes

**Constraints**: Offline-first for mindmap features (update check requires network); GitHub API rate limits (5000 req/hour); conventional commit format enforcement; semantic versioning tags (vX.Y.Z)

**Scale/Scope**: Single repository, automated on every release tag push; update dialog used by all app users

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Pre-Design Check | Post-Design Check | Notes |
|-----------|------------------|-------------------|-------|
| **I. Modular Separation of Concerns** (`:core`, `:data`, `:app`) | ✅ PASS | | New code: GitHub Actions workflow (`.github/workflows/`) in root; update dialog changes in `:app` UI layer; release note parsing logic in `:core` or `:app` utility. No cross-module violations. |
| **II. Unidirectional Data Flow & State Consistency** | ✅ PASS | | Update dialog state managed via `MainViewModel`/`MainUiState`; changelog fetched via `UpdateCheckerService` emitting to UI state. No direct mutation. |
| **III. Test-Driven Verification (NON-NEGOTIABLE)** | ✅ PASS | | Unit tests for release note parser, update dialog Compose preview/test, `UpdateCheckerService` integration test. |
| **IV. Freeplane Format Compatibility & Data Integrity** | ✅ PASS | | No changes to `.mm` parsing/serialization. |
| **V. Performance, Large Map Responsiveness & Simplicity** | ✅ PASS | | Network call off main thread (`Dispatchers.IO`); changelog display uses virtualized Compose text; no heavy libs. |
| **VI. SOLID Principles & Cyclomatic Complexity (≤20)** | ✅ PASS | | Parser as pure function; UI as composable; complexity ≤20. |

## Project Structure

### Documentation (this feature)

```text
specs/013-auto-release-changelog/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
.github/
└── workflows/
    └── release.yml          # GitHub Actions workflow for automated release notes

app/
├── build.gradle.kts
├── src/main/
│   ├── AndroidManifest.xml
│   └── java/fr/julien/quievreux/droidplane2/
│       ├── MainActivity.kt
│       ├── MainViewModel.kt
│       ├── MainUiState.kt
│       ├── update/
│       │   ├── UpdateCheckerService.kt
│       │   ├── GitHubUpdateCheckerServiceImpl.kt
│       │   ├── GitHubRelease.kt
│       │   └── UpdateCheckResult.kt
│       └── ui/
│           ├── components/
│           │   └── UpdateDialog.kt          # Enhanced with changelog display
│           └── state/MainUiState.kt
├── src/test/
└── src/androidTest/

core/
├── build.gradle.kts
└── src/main/java/fr/julien/quievreux/droidplane2/core/
    ├── util/
    │   └── ReleaseNoteParser.kt             # Conventional commit parser
    └── log/

data/
└── (no changes for this feature)
```

**Structure Decision**: Retain the existing 3-module Android architecture (`:core`, `:data`, `:app`) defined in Constitution Principle I. New release note parsing logic goes in `:core` as a generic utility. GitHub Actions workflow in `.github/workflows/`. Update dialog and `UpdateCheckerService` enhancements in `:app`. No changes to `:data`.

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| [e.g., 4th project] | [current need] | [why 3 projects insufficient] |
| [e.g., Repository pattern] | [specific problem] | [why direct DB access insufficient] |
