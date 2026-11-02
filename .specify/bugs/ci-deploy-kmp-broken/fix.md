# Bug Fix: CI Release Deployment Broken Since KMP Migration

- **Slug**: ci-deploy-kmp-broken
- **Fixed**: 2026-11-03
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Fixed the Android CI/CD release build after the Kotlin Multiplatform migration by restoring the AndroidX Compose BOM and explicit version refs for `androidx.compose.ui:ui-tooling-preview` (unblocking `:core:lintVitalAnalyzeRelease` and `:app:lintVitalAnalyzeRelease`), excluding `xpp3`/`xmlpull` from `releaseRuntimeClasspath` so R8 does not fail on duplicate `org.xmlpull.v1.XmlPullParser` program classes conflicting with `android.jar`, and deferring the version bump commit/push in `.github/workflows/android_release.yml` until after tests and `assembleRelease` succeed.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `gradle/libs.versions.toml` | modified | Added `version.ref = "composeUiTest"` to `androidx-ui-tooling` and `androidx-ui-tooling-preview` |
| `core/build.gradle.kts` | modified | Added `implementation(project.dependencies.platform(libs.androidx.compose.bom))` in `androidMain` |
| `data/build.gradle.kts` | modified | Excluded `xpp3` and `xmlpull` groups from `releaseRuntimeClasspath` |
| `app/build.gradle.kts` | modified | Added `implementation(project.dependencies.platform(libs.androidx.compose.bom))` in `androidMain` and excluded `xpp3` / `xmlpull` from `releaseRuntimeClasspath` |
| `app/proguard-rules.pro` | modified | Added `-dontwarn reactor.blockhound.**` to silence R8 service reference warning |
| `.github/workflows/android_release.yml` | modified | Provided `KEYSTORE_PATH: "release-key.jks"` and moved `git commit` + `git push origin master` after unit tests and `assembleRelease` succeed |

## Diff Highlights (optional)

```kotlin
// data/build.gradle.kts & app/build.gradle.kts
configurations.matching {
    it.name == "releaseRuntimeClasspath"
}.configureEach {
    exclude(group = "xpp3")
    exclude(group = "xmlpull")
}
```

```toml
# gradle/libs.versions.toml
androidx-ui-tooling = { module = "androidx.compose.ui:ui-tooling", version.ref = "composeUiTest" }
androidx-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview", version.ref = "composeUiTest" }
```

## Tests Added or Updated

- Verified existing Android and Desktop KMP unit test suites (`testDebugUnitTest`, `:core:desktopTest`, `:data:desktopTest`, `:app:desktopTest`) and release assembly (`assembleRelease`, including `:core:lintVitalAnalyzeRelease`, `:app:lintVitalAnalyzeRelease`, and `:app:minifyReleaseWithR8`).

## Local Verification

- Commands run:
  - `./gradlew assembleRelease` → `BUILD SUCCESSFUL` (`:core:lintVitalAnalyzeRelease`, `:app:lintVitalAnalyzeRelease`, `:app:minifyReleaseWithR8`, `:app:packageRelease` all passed; `app/build/outputs/apk/release/app-release-unsigned.apk` generated).
  - `./gradlew testDebugUnitTest :core:desktopTest :data:desktopTest :app:desktopTest` → `BUILD SUCCESSFUL` (all Android and Desktop unit tests passed).

## Deviations from Assessment

- In `data/build.gradle.kts` and `app/build.gradle.kts`, `xpp3` and `xmlpull` are excluded only from `releaseRuntimeClasspath` (rather than both `releaseRuntimeClasspath` and `debugRuntimeClasspath`), because AGP's `debugUnitTestRuntimeClasspath` extends `debugRuntimeClasspath` and local JVM unit tests (`testDebugUnitTest`) require `xpp3` on their runtime classpath to instantiate `XmlPullParserFactory`.

## Follow-ups

- Run `/speckit-bug-test slug=ci-deploy-kmp-broken` to record the verification report.
- Optionally move `KStringSpec.kt` and Kotest / `kotlinx-coroutines-test` dependencies out of `:core`'s `commonMain` into a dedicated test helper or `commonTest` source set in a future cleanup.
