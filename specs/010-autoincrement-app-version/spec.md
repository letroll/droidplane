# Feature Specification: Define App Version in Gradle and Auto-Increment on Master Push

**Feature Branch**: `010-autoincrement-app-version`

**Created**: 2026-10-14

**Status**: Ready for Planning

**Input**: User description: "la feature de recherche de mise à jour automatique de l'application utilise la version définit dans le manifest, j'aimerai qu'elle le soit à présent dans le script build.gradle.kts du projet et qu'elle soit auto incrémenté au push sur develop" -> Révisé selon choix utilisateur: auto-incrémentation sur la branche `master`.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Version Definition in Gradle Build Script (Priority: P1)

Developers and build pipelines need the application version (`versionCode` and `versionName`) to be defined authoritatively within `app/build.gradle.kts` under `defaultConfig` rather than hardcoded in `AndroidManifest.xml`, ensuring modern Android Gradle Plugin standards, single source of truth, and consistent build configurations.

**Why this priority**: Defining version properties in Gradle is standard practice for modern Android development. It allows programmatic inspection, automated increments, build flavor overrides, and clean separation between build metadata and manifest declaration.

**Independent Test**: Build the project with `./gradlew assembleDebug` and inspect the generated APK and `BuildConfig`. Verify that the installed app's `versionName` and `versionCode` match the values configured in `app/build.gradle.kts`, and that `AndroidManifest.xml` no longer contains hardcoded version attributes.

**Acceptance Scenarios**:

1. **Given** version properties are configured directly in `app/build.gradle.kts`, **When** compiling any build variant (debug or release), **Then** the resulting application manifest and `BuildConfig` receive the exact version values defined in Gradle.
2. **Given** the application runs on a device or emulator, **When** the in-app update checker queries the installed version via package manager, **Then** it receives the exact `versionName` defined in `app/build.gradle.kts`.
3. **Given** `AndroidManifest.xml` is inspected, **When** reviewing the `<manifest>` tag, **Then** `android:versionCode` and `android:versionName` are absent, avoiding duplication and conflicts.

---

### User Story 2 - Automated Version Increment on Push to Master (Priority: P1)

When changes are pushed or merged into the `master` branch, the CI/CD pipeline automatically increments `versionCode` by 1 and synchronizes `versionName` to `1.0.<versionCode>` in `app/build.gradle.kts`, commits the change back to `master` with `[skip ci]`, and builds the release APK with this new version.

**Why this priority**: Eliminates human error, forgotten manual version bumps, duplicate version codes, and guarantees that every release pushed to production on `master` has a unique, sequentially advancing version.

**Independent Test**: Trigger a push/merge to `master`. Verify that the CI workflow executes, increments `versionCode` and updates `versionName` to `1.0.<versionCode>` in `app/build.gradle.kts`, commits the change back with `[skip ci]`, and compiles the release APK.

**Acceptance Scenarios**:

1. **Given** a commit is pushed to the `master` branch, **When** the CI workflow triggers, **Then** `versionCode` is incremented by 1 (e.g. from `18` to `19`) and `versionName` is set to `1.0.<versionCode>` (e.g. `1.0.19`) in `app/build.gradle.kts`.
2. **Given** the version has been incremented, **When** the workflow commits the changes back to `master`, **Then** it uses an automated commit message containing `[skip ci]` to prevent recursive pipeline runs.
3. **Given** a local Gradle build, **When** running a dedicated version increment task or script, **Then** `versionCode` and `versionName` update reliably without corrupting the Gradle build file.

---

### User Story 3 - Synchronized Release Tagging and In-App Update Verification (Priority: P2)

When the release workflow on `master` finishes compiling the APK, it publishes a GitHub Release tagged with `v<versionName>` (matching the newly incremented version in `app/build.gradle.kts`), ensuring 100% alignment between the installed app's version and GitHub release tags for seamless in-app update detection.

**Why this priority**: In-app update detection compares the installed app's `versionName` against GitHub release tags. Having both derived from the exact same `versionName` in `app/build.gradle.kts` guarantees that update checks operate with 100% accuracy.

**Independent Test**: Trigger a release build on `master`. Confirm that the published GitHub Release tag matches `v${versionName}` as defined in `app/build.gradle.kts`, and that an installed app running an earlier version detects this release as an available update.

**Acceptance Scenarios**:

1. **Given** a release workflow runs on `master`, **When** creating the GitHub release, **Then** the release tag name and title are derived from the newly incremented `versionName` in `app/build.gradle.kts` (e.g. `v1.0.19`).
2. **Given** an installed application whose `versionName` is lower than the release tag, **When** the in-app update check executes, **Then** it successfully detects the new release as an available update.

---

### Edge Cases

- **File Formatting Preservation in `build.gradle.kts`**: Automated version bumping must surgically update the version variables/properties without stripping or reformatting surrounding Gradle DSL blocks, comments, or dependencies.
- **Concurrent Git Push**: The GitHub Action must pull/rebase before pushing the bumped version commit to avoid non-fast-forward push rejections.
- **Malformed Version String**: The script parsing the version must validate that `versionCode` is a positive integer before incrementing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST define `versionCode` and `versionName` directly in `app/build.gradle.kts`, establishing Gradle as the single source of truth for application versioning.
- **FR-002**: System MUST remove hardcoded `android:versionCode` and `android:versionName` attributes from `app/src/main/AndroidManifest.xml`.
- **FR-003**: System MUST provide an automated version bump mechanism in the CI workflow (`.github/workflows/android_release.yml`) triggered upon push to the `master` branch.
- **FR-004**: System MUST increment `versionCode` by 1 and set `versionName` to `"1.0.<versionCode>"` on each release build.
- **FR-005**: System MUST commit and push the updated `app/build.gradle.kts` back to `master` with `[skip ci]` in the commit message.
- **FR-006**: System MUST publish the signed APK to GitHub Releases using the tag name `v<versionName>` matching the incremented version in `app/build.gradle.kts`.
- **FR-007**: System MUST provide a local script or task allowing developers to increment or preview the next version code and name locally.

### Key Entities

- **Version Code (`versionCode`)**: Positive integer representing the internal build sequence number in Android.
- **Version Name (`versionName`)**: User-facing semantic version string synchronized with the version code (`1.0.<versionCode>`).
- **Release Workflow**: The GitHub Actions workflow (`android_release.yml`) that increments the version, commits to `master`, compiles the signed APK, and publishes the GitHub release.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of compiled APKs receive their `versionName` and `versionCode` directly from `app/build.gradle.kts` with 0 reliance on `AndroidManifest.xml`.
- **SC-002**: Every push to `master` produces a release tag matching `v<versionName>` identical to the compiled APK's internal version name.
- **SC-003**: 0 recursive CI trigger loops occur following automated version bumps.
- **SC-004**: In-app update detection resolves update availability with 100% accuracy using the aligned versions.

## Assumptions

- The repository uses `master` as the primary production release branch.
- GitHub Actions has write access permissions enabled (`permissions: contents: write`) to push version bump commits to `master`.
- The version format adheres to `1.0.<versionCode>`.

---

## Resolved Clarifications

- **Q1 (Version Increment Strategy)**: Handled via Option B — `versionCode` is incremented by 1, and the patch segment of `versionName` directly reflects `versionCode` (`1.0.<versionCode>`).
- **Q2 (Version Storage Location)**: Handled via Option A — Stored directly in `app/build.gradle.kts`.
- **Q3 (Branch and Persistence)**: Executed on the `master` branch during the release workflow, committed back to `master` with `[skip ci]`.
