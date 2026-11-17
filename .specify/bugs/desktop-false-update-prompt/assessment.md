# Bug Assessment: desktop-false-update-prompt

- **Slug**: `desktop-false-update-prompt`
- **Assessed**: 2026-11-17
- **Reporter Input**: `sur desktop même en étant sur les dernières versions, la proposition de mise à jour m'est proposé`
- **Verdict**: `valid`
- **Severity**: `medium`

---

## Symptom

On the Desktop target, launching the application (or manually checking for updates) displays the `UpdateAvailableDialog` prompting the user to update, even when the user is already running the latest released version of the application.

## Reproduction

1. Build or run the Desktop application from the current codebase (`app/build.gradle.kts` configures `appVersionName = "1.0.38"`, `appVersionCode = 38`).
2. Ensure GitHub releases contain a version newer than `1.0.31` (for example `1.0.38`).
3. Launch the Desktop app (`Main.kt` -> `DesktopDroidplaneApp`).
4. Observe `LaunchedEffect(Unit)` calling `triggerUpdateCheck(isManual = false)`, which passes `currentVersion = "1.0.31"` instead of `"1.0.38"`.
5. `GitHubUpdateCheckerServiceImpl` compares the latest GitHub release tag against `"1.0.31"`, evaluates `latestReleaseVersion > 1.0.31` as `true`, and triggers the update dialog even though the installed Desktop build is already up to date.

## Suspected Code Paths

- `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/DesktopDroidplaneApp.kt`
  - Line 95: `private const val DESKTOP_APP_VERSION = "1.0.31"` — manually hardcoded version constant, out of sync with `appVersionName` (`"1.0.38"`).
  - Lines 182–195 (`triggerUpdateCheck`): passes `currentVersion = DESKTOP_APP_VERSION` to `viewModel.checkForUpdates(...)` and formats `upToDateMessage` with `DESKTOP_APP_VERSION`.
  - Lines 197–211 (`LaunchedEffect(Unit)`): calls `triggerUpdateCheck(isManual = false)` on Desktop startup.
- `app/build.gradle.kts`
  - Lines 8–9: defines `val appVersionCode = 38` and `val appVersionName = "1.0.38"`.
  - Lines 16–17: wires `versionCode = appVersionCode` and `versionName = appVersionName` into `android.defaultConfig` only; nothing injects or generates this version for `desktopMain` / `compose.desktop`.
- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/update/UpdateCheckerService.kt`
  - `GitHubUpdateCheckerServiceImpl`: compares `AppVersion.parse(release.tagName) > AppVersion.parse(currentVersion)` and returns `UpdateCheckResult.UpdateAvailable` whenever the GitHub release tag is greater than the supplied `currentVersion`.
- `app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/MainActivity.kt`
  - Line 256: reads `packageInfo?.versionName.orEmpty()` dynamically from Android package metadata (which receives `appVersionName` from `app/build.gradle.kts`), explaining why Android is unaffected while Desktop drifts.
- `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/VersionConfigurationTest.kt`
  - Validates `appVersionCode` / `appVersionName` in `app/build.gradle.kts` and checks `AndroidManifest.xml`, but has no check preventing a hardcoded stale version in `desktopMain`.

## Root Cause Hypothesis

- **Confidence**: `high`
- `DesktopDroidplaneApp.kt` hardcodes `private const val DESKTOP_APP_VERSION = "1.0.31"` at line 95 instead of reading a build-time version generated from `app/build.gradle.kts` (`appVersionName = "1.0.38"`).
- While Android reads `versionName` dynamically from `PackageManager` (`MainActivity.kt:256`), `app/build.gradle.kts` never generates a shared Kotlin constant or Desktop resource from `appVersionName`.
- Because `DESKTOP_APP_VERSION` remained pinned at `"1.0.31"` as releases progressed to `"1.0.32+"` (up to `"1.0.38"`), every Desktop startup check queries GitHub with `currentVersion = "1.0.31"` and receives `UpdateCheckResult.UpdateAvailable`.

## Proposed Remediation

### Preferred Approach

Generate the application version at Gradle build time from `app/build.gradle.kts` (`appVersionName` and `appVersionCode`) so both Android and Desktop consume the single source of truth without manual duplication:

1. Register a Gradle code-generation task in `app/build.gradle.kts` (for `commonMain` or `desktopMain`) that writes a small Kotlin object (e.g., `BuildConfig` / `AppBuildInfo` containing `const val VERSION_NAME = "1.0.38"` and `const val VERSION_CODE = 38`) into `layout.buildDirectory.dir("generated/source/buildInfo/...")` and wire that directory as a source directory (`srcDir` + `dependsOn` or `tasks.withType<KotlinCompile>` / `srcDir(generateTask)`).
2. Remove `private const val DESKTOP_APP_VERSION = "1.0.31"` from `DesktopDroidplaneApp.kt` and reference the generated constant in `triggerUpdateCheck`.
3. Update `VersionConfigurationTest.kt` to verify that `DesktopDroidplaneApp.kt` (and `desktopMain` sources generally) does not declare a hardcoded `DESKTOP_APP_VERSION` string literal, ensuring future releases stay in sync.

### Alternative Approach

Move `appVersionName` and `appVersionCode` into `gradle.properties`, read them from `app/build.gradle.kts`, and write/filter a `version.properties` resource into `desktopMain` resources during `processDesktopMainResources` (reading it at runtime via `ClassLoader.getResourceAsStream` with a safe fallback). This avoids generating Kotlin source files but introduces runtime resource loading and fallback handling.

### Files Likely to Change

- `app/build.gradle.kts`
- `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/DesktopDroidplaneApp.kt`
- `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/VersionConfigurationTest.kt`

### Tests to Add / Update

- Update `VersionConfigurationTest.kt` to assert that `DesktopDroidplaneApp.kt` does not contain a hardcoded `DESKTOP_APP_VERSION` literal and consumes the build-injected version.
- Run the Gradle test suite (including `commonTest` / `desktopTest` and Desktop compilation) to verify code generation works across clean and incremental builds.

## Risks & Considerations

- If the Gradle generation task is not properly wired as an input directory provider (`files(generateTask)` / `srcDir(generateTask)`), incremental builds, IDE sync, or Desktop packaging (`packageDmg` / `packageMsi` / `packageDeb` / `desktopRun`) could miss the generated source or fail to regenerate when `appVersionName` changes.
- Verify whether `compose.desktop.application.nativeDistributions.packageVersion` in `app/build.gradle.kts` also needs or uses `appVersionName` for consistency (separate from the update-check bug, keep any edit minimal unless explicitly desired).

## Open Questions

None.
