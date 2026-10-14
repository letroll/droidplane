# Implementation Plan: Define App Version in Gradle and Auto-Increment on Master Push

**Branch**: `010-autoincrement-app-version` | **Date**: 2026-10-14 | **Spec**: [specs/010-autoincrement-app-version/spec.md](spec.md)

**Input**: Feature specification from `specs/010-autoincrement-app-version/spec.md`

## Summary

Migrate application version definition to Gradle and automate sequential version bumping:
1. Define `val appVersionCode = 18` and `val appVersionName = "1.0.18"` directly in `app/build.gradle.kts` under `defaultConfig`, removing hardcoded version attributes from `app/src/main/AndroidManifest.xml`.
2. Provide a robust bash utility `scripts/bump-version.sh` that increments `appVersionCode` (+1) and sets `appVersionName` to `"1.0.<versionCode>"`.
3. Update `.github/workflows/android_release.yml` to automatically execute the version bump on `master` pushes, commit and push back with `[skip ci]`, compile the release APK with the new version, and publish a GitHub release tagged as `v<versionName>`.
4. Add unit test `VersionConfigurationTest` ensuring Gradle remains the single source of truth and manifest remains clean.

## Technical Context

**Language/Version**: Kotlin 2.1.0, Gradle 8.10.2 (Kotlin DSL), Android Gradle Plugin 8.7.3

**Primary Dependencies**: None (build scripting and GitHub Actions)

**Storage**: `app/build.gradle.kts`

**Testing**: `kotlin.test`, JUnit 4

**Target Platform**: Android & GitHub Actions CI/CD (Ubuntu runner)

**Project Type**: Android build configuration and continuous integration workflow

**Performance Goals**: Script execution in < 1 second; 0 overhead on compilation

**Constraints**: Preserves all existing Gradle DSL blocks; prevents recursive CI loops with `[skip ci]`; zero hardcoded versions in `AndroidManifest.xml`

---

## Constitution Check

| Principle | Status | Evaluation & Architectural Alignment |
|---|---|---|
| **I. Modular Separation of Concerns** | **PASSED** | Versioning logic lives strictly in build configurations (`app/build.gradle.kts`, `scripts/`, and `.github/workflows/`). Core and Data modules are untouched. |
| **II. Unidirectional Data Flow & State Consistency** | **PASSED** | Single source of truth in Gradle; propagated down to `BuildConfig` and `PackageInfo` at build time. |
| **III. Test-Driven Verification** | **PASSED** | Fast JVM unit test verifying presence of version properties in Gradle and absence in `AndroidManifest.xml`. |
| **IV. Freeplane Format Compatibility & Data Integrity** | **PASSED** | Mindmap files and data models remain completely unaffected. |
| **V. Performance & Simplicity** | **PASSED** | Lightweight regex-based shell script; zero additional external plugins or dependencies. |
| **VI. SOLID Principles & Cyclomatic Complexity** | **PASSED** | Single responsibility per script and step. |

---

## Project Structure

### Documentation (this feature)

```text
specs/010-autoincrement-app-version/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Architectural research & decisions
├── data-model.md        # Version data model & state transitions
├── quickstart.md        # Verification & test guide
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── contracts/
    └── ci-workflow-contract.md # CI workflow and bump script contract
```

### Source Code & Configuration Changes

```text
app/
├── build.gradle.kts                   # Declare appVersionCode & appVersionName in defaultConfig
└── src/main/
    └── AndroidManifest.xml            # Remove android:versionCode and android:versionName

scripts/
└── bump-version.sh                    # Automated version increment script

.github/workflows/
└── android_release.yml                # Auto-bump on master push, commit with [skip ci], tag release

app/src/test/java/fr/julien/quievreux/droidplane2/
└── VersionConfigurationTest.kt       # Unit test verifying Gradle versioning and manifest purity
```

---

## Complexity Tracking

*No violations identified. Design adheres fully to all Constitution principles.*
