# Tasks: Define App Version in Gradle and Auto-Increment on Master Push

**Input**: Design documents from `/specs/010-autoincrement-app-version/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Test-driven verification (Principle III) included for JVM unit tests.
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Exact file paths are provided for each task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create reusable local bump script for version management

- [X] T001 [P] Create local bump script `scripts/bump-version.sh` extracting `appVersionCode` from `app/build.gradle.kts`, incrementing by 1, updating `appVersionName` to `"1.0.<versionCode>"`, and making it executable per ci-workflow-contract.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Automated test verifying version declaration purity and manifest cleanup

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 [P] Create unit test `VersionConfigurationTest` in `app/src/test/java/fr/julien/quievreux/droidplane2/VersionConfigurationTest.kt` verifying that `app/build.gradle.kts` declares `val appVersionCode` and `val appVersionName`, and that `app/src/main/AndroidManifest.xml` does not contain `android:versionCode` or `android:versionName`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Version Definition in Gradle Build Script (Priority: P1) 🎯 MVP

**Goal**: Establish `app/build.gradle.kts` as the single authoritative source of truth for versioning, removing hardcoded version attributes from `AndroidManifest.xml`.

**Independent Test**: Compile the project with `./gradlew assembleDebug` and verify that `BuildConfig.VERSION_CODE` and `BuildConfig.VERSION_NAME` match `app/build.gradle.kts`.

### Implementation for User Story 1

- [X] T003 [US1] Define `val appVersionCode = 18` and `val appVersionName = "1.0.18"` at top of `app/build.gradle.kts` and configure `versionCode = appVersionCode` and `versionName = appVersionName` in `defaultConfig`
- [X] T004 [US1] Remove hardcoded `android:versionCode` and `android:versionName` attributes from `<manifest>` in `app/src/main/AndroidManifest.xml`

**Checkpoint**: User Story 1 complete and independently testable (MVP)

---

## Phase 4: User Story 2 - Automated Version Increment on Push to Master (Priority: P1)

**Goal**: Automatically increment version on `master` pushes in GitHub Actions, committing back with `[skip ci]`.

**Independent Test**: Execute `scripts/bump-version.sh` locally and verify `appVersionCode` increments (+1) and `appVersionName` updates to `"1.0.<versionCode>"` in `app/build.gradle.kts`.

### Implementation for User Story 2

- [X] T005 [US2] Add automated version bump and git commit step with `[skip ci]` in `.github/workflows/android_release.yml` before building the release APK

**Checkpoint**: User Stories 1 AND 2 complete and independently testable

---

## Phase 5: User Story 3 - Synchronized Release Tagging and In-App Update Verification (Priority: P2)

**Goal**: Synchronize the GitHub Releases publication step in `.github/workflows/android_release.yml` to use `v${{ env.APP_VERSION_NAME }}`.

**Independent Test**: Verify `.github/workflows/android_release.yml` publishes releases tagged with `v${{ env.APP_VERSION_NAME }}` matching the incremented Gradle version.

### Implementation for User Story 3

- [X] T006 [US3] Update release action in `.github/workflows/android_release.yml` to use `tag_name: v${{ env.APP_VERSION_NAME }}` and `name: "Release v${{ env.APP_VERSION_NAME }}"`

**Checkpoint**: All user stories complete and independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Verification and regression testing across the repository

- [X] T007 Run full test suite `./gradlew test` to ensure zero regressions across all modules
- [X] T008 Execute validation scenarios per `specs/010-autoincrement-app-version/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - starts immediately (T001)
- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user stories (T002)
- **User Story 1 (Phase 3)**: Depends on Foundational Phase (T002). Delivers MVP.
- **User Story 2 (Phase 4)**: Depends on Phase 3 (T003).
- **User Story 3 (Phase 5)**: Depends on Phase 4 (T005).
- **Polish (Phase 6)**: Depends on US1, US2, and US3 completion.

---

## Parallel Opportunities

- **Phase 1 & 2**: T001 and T002 can run in parallel as they touch different files (`scripts/bump-version.sh` and `VersionConfigurationTest.kt`).
- **Phase 4 & 5**: T005 and T006 are sequential edits in `.github/workflows/android_release.yml`.

---

## Implementation Strategy

### MVP First (User Story 1)
1. Complete Phase 1: Setup (`scripts/bump-version.sh`).
2. Complete Phase 2: Foundational (`VersionConfigurationTest.kt`).
3. Complete Phase 3: User Story 1 (`app/build.gradle.kts`, `AndroidManifest.xml`).
4. **STOP and VALIDATE**: Verify local build and tests pass.

### Incremental Delivery
1. Foundation + US1 -> Version is declared in Gradle and clean in Manifest (MVP).
2. Add US2 -> GitHub Actions auto-increments version and commits with `[skip ci]`.
3. Add US3 -> GitHub Release tags synchronize with `v${versionName}`.
4. Polish -> Full test suite verification.
