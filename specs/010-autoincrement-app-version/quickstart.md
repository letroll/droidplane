# Quickstart & Verification Guide: Version Auto-Increment

**Feature Branch**: `010-autoincrement-app-version`
**Created**: 2026-10-14
**Status**: Ready

## 1. Automated Verification (JVM Unit Tests)

Verify version configuration in Gradle and manifest cleanup:

```bash
./gradlew :app:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.VersionConfigurationTest"
```

Verify all project tests pass:

```bash
./gradlew test
```

---

## 2. Interactive & Local Script Verification

### Step 1: Verify Current Gradle Version
1. Open `app/build.gradle.kts`:
   - Verify `val appVersionCode = 18` and `val appVersionName = "1.0.18"` are defined.
   - Verify `defaultConfig` has `versionCode = appVersionCode` and `versionName = appVersionName`.
2. Open `app/src/main/AndroidManifest.xml`:
   - Verify that `android:versionCode` and `android:versionName` are completely removed.

### Step 2: Test the Bump Script Locally
1. Run:
   ```bash
   chmod +x scripts/bump-version.sh
   ./scripts/bump-version.sh
   ```
2. Verify output displays:
   `Bumping version: 18 -> 19 (versionName: 1.0.19)`
3. Check `git diff app/build.gradle.kts`:
   Confirm that `appVersionCode = 19` and `appVersionName = "1.0.19"` were updated cleanly without modifying any other part of the file.
4. Revert test bump:
   ```bash
   git checkout app/build.gradle.kts
   ```

### Step 3: Verify APK Build Output
1. Run `./gradlew assembleDebug`
2. Check that the build completes and `BuildConfig.VERSION_CODE` and `BuildConfig.VERSION_NAME` reflect the configured values.
