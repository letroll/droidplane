# Bug Assessment: CI Release Deployment Broken Since KMP Migration

- **Slug**: ci-deploy-kmp-broken
- **Created**: 2026-11-03
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: critical

## Report (verbatim or summarized)

> le déploiement ci est cassé depuis le passage au multiplateforme

## Symptom

The GitHub Actions release deployment workflow (`.github/workflows/android_release.yml`) fails since the Kotlin Multiplatform / Compose Multiplatform migration (`015-kmp-compose-migration`, commit `18118c8`). Specifically, `./gradlew assembleRelease` fails in three places:
1. `:core:lintVitalAnalyzeRelease` (and `:app:lintVitalAnalyzeRelease`) fails because `androidx-ui-tooling-preview` is declared in `androidMain` without a `platform(libs.androidx.compose.bom)` or an explicit version in `libs.versions.toml` (`Could not find androidx.compose.ui:ui-tooling-preview:.`).
2. `:app:minifyReleaseWithR8` fails with `ERROR: R8: Library class android.content.res.XmlResourceParser implements program class org.xmlpull.v1.XmlPullParser (origin: ...jetified-xpp3-1.1.4c.jar:org/xmlpull/v1/XmlPullParser.class)` (plus a missing `BlockHoundIntegration` warning from `kotlinx-coroutines-test` being leaked into `core`'s `commonMain` dependencies).
3. Even if `:app:assembleRelease` succeeds, the signed APK is no longer signed in CI because `.github/workflows/android_release.yml` sets `KEYSTORE_FILE: "release-key.jks"`, whereas `app/build.gradle.kts` checks `System.getenv("KEYSTORE_PATH")` (and `file("release-key.jks")` resolves to `app/release-key.jks`, while look-up / signing config env var names are mismatched).

## Reproduction

1. Run `./gradlew assembleRelease` (the exact command from step `📦 Compiler l'APK Release Signé` in `.github/workflows/android_release.yml`).
2. Observe task `:core:lintVitalAnalyzeRelease` failing with:
   `Could not find androidx.compose.ui:ui-tooling-preview:.`
3. Run `./gradlew minifyReleaseWithR8` to observe the R8 step failing with:
   `ERROR: R8: Library class android.content.res.XmlResourceParser implements program class org.xmlpull.v1.XmlPullParser (origin: ...jetified-xpp3-1.1.4c.jar:org/xmlpull/v1/XmlPullParser.class)`
   and `WARNING: ... R8: Unexpected reference to missing service class: META-INF/services/reactor.blockhound.integration.BlockHoundIntegration.`

## Suspected Code Paths

- `core/build.gradle.kts:47` & `app/build.gradle.kts:121` — `androidMain` declares `implementation(libs.androidx.ui.tooling.preview)`, which has no `version.ref` in `gradle/libs.versions.toml:45` (`androidx-ui-tooling-preview = { module = "androidx.compose.ui:ui-tooling-preview" }`). During the KMP migration (`18118c8`), `implementation(platform(libs.androidx.compose.bom))` was removed from `core/build.gradle.kts` and `app/build.gradle.kts`, leaving `androidx.compose.ui:ui-tooling-preview` without a version when AGP resolves `:core:releaseLintChecksClasspath` and `:app:releaseLintChecksClasspath`.
- `data/build.gradle.kts:29` — During the KMP migration, `api(libs.xpp3)` was added to `commonMain` (previously `xpp3` was only in `testImplementation`, because Android provides `org.xmlpull.v1.*` in `android.jar`). Shipping `xpp3:1.1.4c` on the Android runtime/compile classpath bundles `org.xmlpull.v1.XmlPullParser` as a program class in the APK, which conflicts with the Android framework class `android.content.res.XmlResourceParser` implementing `org.xmlpull.v1.XmlPullParser` during R8 minification (`:app:minifyReleaseWithR8`), or requires `xpp3` to only be included in `desktopMain` / `commonTest` / `androidUnitTest` (or `xmlpull` classes excluded from the Android APK / R8).
- `core/build.gradle.kts:34-37` — Test-only libraries (`libs.kotlinx.coroutines.test`, `libs.kotest`, `libs.kotest.assertions`, `libs.kotest.runner.junit5`) were placed in `commonMain` of `:core` (to compile `KStringSpec.kt` in `core/src/commonMain`), causing `kotlinx-coroutines-test` (and its `BlockHoundIntegration` service) and Kotest to be packaged into production release builds and analyzed by R8.
- `.github/workflows/android_release.yml:42-57` & `91-98` — Two CI workflow issues:
  1. Step `🔢 Auto-incrémenter la version` commits and pushes the version bump to `master` *before* running the unit tests (`🧪 Lancer les tests unitaires`) and building the release APK (`📦 Compiler l'APK Release Signé`). Every time the build fails, `master` still gets a version bump commit pushed.
  2. Step `📦 Compiler l'APK Release Signé` sets `KEYSTORE_FILE: "release-key.jks"`, whereas `app/build.gradle.kts:32` reads `System.getenv("KEYSTORE_PATH")` (though fallback `file("release-key.jks")` happens to match `app/release-key.jks`, aligning `KEYSTORE_PATH` avoids ambiguity).

## Root Cause Hypothesis

**Confidence: high.**
When migrating to Kotlin Multiplatform (`18118c8`), two major changes broke `./gradlew assembleRelease`:
1. **Unversioned `androidx.compose.ui:ui-tooling-preview` in `androidMain`**: The AndroidX Compose BOM (`platform(libs.androidx.compose.bom)`) was removed in favor of JetBrains Compose Multiplatform dependencies (`compose.runtime`, `compose.ui`, `compose.components.uiToolingPreview`), but `implementation(libs.androidx.ui.tooling.preview)` was left in `core` and `app`'s `androidMain` without the Compose BOM or a version in `libs.versions.toml`. While Kotlin compilation succeeded using transitive version resolution from JetBrains Compose, the isolated `releaseLintChecksClasspath` configuration in `:core:lintVitalAnalyzeRelease` cannot resolve `androidx.compose.ui:ui-tooling-preview:` without a version.
2. **`xpp3` (`org.xmlpull.v1.*`) added to `commonMain` in `:data`**: Pre-KMP, `data` and `app` used `org.xmlpull.v1.XmlPullParser` provided by the Android SDK (`android.jar`) at runtime, and only added `xpp3` to `testImplementation` for JVM unit tests. In KMP, `api(libs.xpp3)` was added to `data`'s `commonMain` so `commonMain` and `desktopMain` could compile against `org.xmlpull.v1.*`. Consequently, `xpp3-1.1.4c.jar` is now packaged into the Android release build as program classes. R8 strictly forbids a library class (`android.content.res.XmlResourceParser` in `android.jar`) from implementing a program class (`org.xmlpull.v1.XmlPullParser` in `xpp3.jar`), aborting `:app:minifyReleaseWithR8`.

## Proposed Remediation

**Preferred**:
1. **Fix `androidx-ui-tooling-preview` resolution in `:core` and `:app`**:
   - Add `implementation(project.dependencies.platform(libs.androidx.compose.bom))` to `androidMain` in `core/build.gradle.kts` and `app/build.gradle.kts` (or set explicit version / use `compose.uiTooling` / `compose.components.uiToolingPreview`). Adding the Compose BOM platform in `androidMain` (or `version.ref = "composeUiTest"` on `androidx-ui-tooling-preview` in `libs.versions.toml`) ensures `:core:lintVitalAnalyzeRelease` and `:app:lintVitalAnalyzeRelease` resolve `androidx.compose.ui:ui-tooling-preview` cleanly.
2. **Fix `org.xmlpull.v1` R8 conflict between `xpp3` and `android.jar`**:
   - In `data/build.gradle.kts`, keep `org.xmlpull.v1` available at compile-time for `commonMain` and at runtime for `desktopMain` and unit tests (`commonTest` / `androidUnitTest`), **without** packaging `org.xmlpull.v1.*` duplicate classes from `xpp3` into the Android APK runtime, OR exclude `org/xmlpull/v1/**` from `xpp3` on the Android runtime / configure `compileOnly` for Android and `implementation(libs.xpp3)` for `desktopMain`.
   - Specifically, in KMP: `commonMain` can use `implementation(libs.xpp3)` while `androidMain` (or `configurations.matching { it.name.contains("android", ignoreCase = true) && (it.name.endsWith("RuntimeClasspath") || it.name.contains("ReleaseRuntime") || it.name.contains("DebugRuntime")) }`) excludes `group = "xpp3"` or `xmlpull` (since Android runtime natively provides `org.xmlpull.v1.XmlPullParser` and `XmlPullParserFactory` in `android.jar`!).Wait: let's check if Android's `android.jar` provides `XmlPullParserFactory` at runtime — yes, `org.xmlpull.v1.XmlPullParser`, `XmlSerializer`, `XmlPullParserFactory`, and `XmlPullParserException` are part of the public Android SDK (`android.jar`) since API 1! Only JVM unit tests (`androidUnitTest`) and Desktop (`desktopMain`) need `xpp3` at runtime.
   - Additionally, in `app/proguard-rules.pro`, add `-dontwarn reactor.blockhound.**` (or clean up the `BlockHoundIntegration` warning) and remove `-keep class org.xmlpull.v1.** { *; }` if `org.xmlpull.v1` is a library class from `android.jar`.
3. **Improve `.github/workflows/android_release.yml` ordering**:
   - Move the `🔢 Auto-incrémenter la version` git commit & push step (or only push the version bump) *after* unit tests and `assembleRelease` succeed (or bump locally before `assembleRelease` so the APK has the new `versionCode`/`versionName`, and only `git commit` + `git push` once tests and `assembleRelease` pass!), preventing broken builds from pushing empty version bump commits to `master`.

**Alternatives** (optional):
- Instead of excluding `xpp3` from Android runtime configurations, keep `xpp3` only in `desktopMain` and `commonTest` and provide `expect`/`actual` or `compileOnly` for `commonMain`. Trade-off: KMP `commonMain` does not support `compileOnly` directly without warnings, whereas excluding `xpp3` from Android runtime configurations (`androidReleaseRuntimeClasspath`, `androidDebugRuntimeClasspath`) in `data/build.gradle.kts` and `app/build.gradle.kts` keeps `commonMain`, `desktopMain`, `desktopTest`, and `androidUnitTest` working seamlessly while letting the Android APK use the built-in `android.jar` `org.xmlpull.v1` classes.

**Files likely to change**:
- `gradle/libs.versions.toml`
- `core/build.gradle.kts`
- `data/build.gradle.kts`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `.github/workflows/android_release.yml`

**Tests to add or update**:
- Verify `./gradlew testDebugUnitTest :core:desktopTest :data:desktopTest :app:desktopTest` passes.
- Verify `./gradlew assembleRelease` (including `:core:lintVitalAnalyzeRelease`, `:app:lintVitalAnalyzeRelease`, and `:app:minifyReleaseWithR8`) completes with `BUILD SUCCESSFUL` and produces the release APK in `app/build/outputs/apk/release/`.

## Risks & Considerations

- Excluding `xpp3` from Android runtime classpaths (`*RuntimeClasspath`) must not remove `xpp3` from `androidUnitTestRuntimeClasspath` (because local JVM unit tests on Android use stubbed `android.jar` and require `xpp3` at test runtime). Filtering specifically `matching { it.name == "releaseRuntimeClasspath" || it.name == "debugRuntimeClasspath" }` in `app` and `data` preserves `xpp3` in `UnitTest` and `desktop` configurations while stripping it from the packaged Android APK.
- Bumping the version in `app/build.gradle.kts` before `assembleRelease` is required so the built APK embeds the incremented `versionCode` and `versionName`, while deferring `git commit` and `git push origin master` until after `assembleRelease` succeeds avoids polluting `master` if tests or the build fail.

## Open Questions

- None — both build failures (`:core:lintVitalAnalyzeRelease` and `:app:minifyReleaseWithR8`) were reproduced locally with `./gradlew assembleRelease`.
