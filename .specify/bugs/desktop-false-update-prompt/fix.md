# Bug Fix: Sync Desktop version from Gradle build config

- **Slug**: `desktop-false-update-prompt`
- **Fixed**: 2026-11-17
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Replaced the stale hardcoded `DESKTOP_APP_VERSION = "1.0.31"` constant in Desktop UI with a build-time generated `AppBuildInfo` object derived directly from `appVersionCode` and `appVersionName` in `app/build.gradle.kts`. As a result, Desktop startup and manual update checks now report the actual build version (`"1.0.38"`) matching `app/build.gradle.kts`, stopping false update dialogs on the latest release.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/build.gradle.kts` | modified | Added `generateAppBuildInfo` task and wired its output directory into `commonMain.kotlin.srcDir(...)` to generate `fr.julien.quievreux.droidplane2.AppBuildInfo`. |
| `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/DesktopDroidplaneApp.kt` | modified | Removed `private const val DESKTOP_APP_VERSION = "1.0.31"` and updated `triggerUpdateCheck` to read `AppBuildInfo.VERSION_NAME`. |
| `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/VersionConfigurationTest.kt` | modified | Added test asserting generated `AppBuildInfo` matches `appVersionCode` / `appVersionName` and `DesktopDroidplaneApp.kt` uses `AppBuildInfo.VERSION_NAME` without `DESKTOP_APP_VERSION`. |

## Diff Highlights

`app/build.gradle.kts`:
```kotlin
val generateAppBuildInfo = tasks.register("generateAppBuildInfo") {
    val outputDir = layout.buildDirectory.dir("generated/source/buildInfo/commonMain/kotlin")
    val versionNameValue = appVersionName
    val versionCodeValue = appVersionCode
    inputs.property("versionName", versionNameValue)
    inputs.property("versionCode", versionCodeValue)
    outputs.dir(outputDir)
    ...
}
```

`app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/DesktopDroidplaneApp.kt`:
```kotlin
    fun triggerUpdateCheck(isManual: Boolean) {
        viewModel.checkForUpdates(
            currentVersion = AppBuildInfo.VERSION_NAME,
            isManual = isManual,
            ...
            upToDateMessage = R.getString(R.string.update_up_to_date, AppBuildInfo.VERSION_NAME),
            errorMessage = R.getString(R.string.update_error)
        )
    }
```

## Tests Added or Updated

- `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/VersionConfigurationTest.kt::generated AppBuildInfo matches gradle and desktop uses it without hardcoded version` — verifies generated `AppBuildInfo.VERSION_CODE` and `AppBuildInfo.VERSION_NAME` equal the values in `app/build.gradle.kts`, confirms `DESKTOP_APP_VERSION` is absent from `DesktopDroidplaneApp.kt`, and verifies `AppBuildInfo.VERSION_NAME` is referenced.

## Local Verification

- Commands run:
  - `./gradlew --no-daemon --offline :app:desktopTest :app:compileKotlinDesktop :app:compileDebugKotlinAndroid` → `BUILD SUCCESSFUL in 26s` (`:app:desktopTest`, `:app:compileKotlinDesktop`, `:app:compileDebugKotlinAndroid` succeeded).
  - Test result XML `app/build/test-results/desktopTest/TEST-fr.julien.quievreux.droidplane2.VersionConfigurationTest.xml`: `tests="2" skipped="0" failures="0" errors="0"`.
- Generated file check:
  - `app/build/generated/source/buildInfo/commonMain/kotlin/fr/julien/quievreux/droidplane2/AppBuildInfo.kt` generated with `VERSION_CODE: Int = 38` and `VERSION_NAME: String = "1.0.38"`.

## Deviations from Assessment

None.

## Follow-ups

- Consider aligning `compose.desktop.application.nativeDistributions.packageVersion` with `appVersionName` if native Desktop installers (`packageDmg` / `packageMsi` / `packageDeb`) are configured in the future.
- Run `/speckit-bug-test slug=desktop-false-update-prompt` to formally verify the bug resolution.
