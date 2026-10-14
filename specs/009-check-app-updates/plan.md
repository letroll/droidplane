# Implementation Plan: GitHub In-App Update Detection and Installation

**Branch**: `009-check-app-updates` | **Date**: 2026-10-14 | **Spec**: [specs/009-check-app-updates/spec.md](spec.md)

**Input**: Feature specification from `specs/009-check-app-updates/spec.md`

## Summary

Implement native in-app update detection and installation using the public GitHub Releases API for Droidplane:
1. Provide a lightweight, zero-dependency `UpdateCheckerService` querying `https://api.github.com/repos/letroll/droidplane/releases/latest` using native platform HTTP and JSON parsing.
2. Provide a robust `AppVersion` semantic versioning comparison class to determine whether a release is newer than the currently installed app version.
3. Automatically check for updates on app startup (asynchronously and silently on error or no-update) and provide an on-demand manual check action in the top bar menu with clear user feedback.
4. Present an `UpdateAvailableDialog` displaying the new version, date, and release notes, with actions to download and install, view on GitHub, or dismiss.
5. Delegate APK downloading to Android's native `DownloadManager` with completion notifications enabling direct package installer launch.

## Technical Context

**Language/Version**: Kotlin 2.1.0, JVM 21, Java 21 toolchain

**Primary Dependencies**: Jetpack Compose (BOM 2024.12.01, Material 3), Android SDK platform (`java.net.HttpURLConnection`, `org.json`, `android.app.DownloadManager`), Koin DI (BOM 4.0.0)

**Storage**: None (stateless queries); temporary APK files saved to system `DownloadManager` destination

**Testing**: `kotlin.test`, Kotest (`io.kotest`), MockK, Turbine, Compose UI tests

**Target Platform**: Android (Min SDK 26, Target SDK 35)

**Project Type**: Android mobile application

**Performance Goals**: Network query completes in < 3s; zero impact on startup time (runs in background on `Dispatchers.IO`)

**Constraints**: Zero added heavy third-party dependencies (no Retrofit, OkHttp, or Ktor); offline-tolerant; non-blocking; cyclomatic complexity strictly < 20 per method

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evaluation & Architectural Alignment |
|---|---|---|
| **I. Modular Separation of Concerns** | **PASSED** | `:core` and `:data` remain untouched. In `:app`, the `update` package encapsulates domain/service models (`AppVersion`, `GitHubRelease`, `UpdateCheckerService`, `ApkDownloadHelper`), while UI components reside in `ui/dialog/` and `ui/components/`. |
| **II. Unidirectional Data Flow & State Consistency** | **PASSED** | `MainViewModel` exposes `UpdateUiState` via `StateFlow`. UI events (check, dismiss, download) flow unidirectionally. |
| **III. Test-Driven Verification** | **PASSED** | Fast JVM unit tests for `AppVersion` and `UpdateCheckerService` parsing using `kotlin.test` and MockK. Compose `@Preview` on all UI components. |
| **IV. Freeplane Format Compatibility & Data Integrity** | **PASSED** | Does not manipulate or affect mindmap documents or XML files in any way. |
| **V. Performance & Simplicity** | **PASSED** | Native `HttpURLConnection` and `org.json` add 0 bytes of external dependency bloat. Background execution on `Dispatchers.IO` guarantees fluid 60 fps app startup. |
| **VI. SOLID Principles & Cyclomatic Complexity** | **PASSED** | Single responsibility: `UpdateCheckerService` (API), `AppVersion` (comparison), `ApkDownloadHelper` (download), `UpdateAvailableDialog` (presentation). All methods < 10 cyclomatic complexity. |

---

## Project Structure

### Documentation (this feature)

```text
specs/009-check-app-updates/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Architectural research & decisions
├── data-model.md        # Data models & state
├── quickstart.md        # Verification & test guide
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── contracts/
    ├── github-api-contract.md  # GitHub API endpoint contract
    └── ui-contracts.md         # UI components & service contracts
```

### Source Code (repository root)

```text
app/src/main/java/fr/julien/quievreux/droidplane2/
├── update/
│   ├── AppVersion.kt                  # Semantic versioning model & parser
│   ├── GitHubRelease.kt               # GitHub release model & JSON parser
│   ├── UpdateCheckerService.kt        # GitHub REST client & update detection
│   └── ApkDownloadHelper.kt           # Android DownloadManager delegation
├── ui/
│   ├── dialog/
│   │   └── UpdateAvailableDialog.kt   # Dialog displaying version, changelog & install action
│   └── components/
│       └── AppTopBar.kt               # Added "Check for updates" overflow action
├── MainViewModel.kt                   # UpdateUiState management & update check trigger
├── MainActivity.kt                    # Startup check & dialog hosting
└── res/
    └── values/
        └── strings.xml                # Localized strings for update features

app/src/test/java/fr/julien/quievreux/droidplane2/
└── update/
    ├── AppVersionTest.kt              # Unit tests for version comparison & normalization
    └── UpdateCheckerServiceTest.kt    # Unit tests for JSON release extraction & update logic
```

**Structure Decision**: Standard clean presentation architecture located in `:app`, grouping update logic in `fr.julien.quievreux.droidplane2.update`.

---

## Complexity Tracking

*No violations identified. Design adheres fully to all Constitution principles.*
