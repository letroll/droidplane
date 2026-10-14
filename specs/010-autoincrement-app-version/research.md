# Research & Architectural Decisions: Version Definition and Auto-Increment

**Feature Branch**: `010-autoincrement-app-version`
**Created**: 2026-10-14
**Status**: Completed

## 1. Version Placement in Gradle vs. Manifest

### Decision
Define `versionCode` and `versionName` directly in `app/build.gradle.kts` under `android.defaultConfig`:
```kotlin
val appVersionCode = 18
val appVersionName = "1.0.18"

android {
    defaultConfig {
        applicationId = "fr.julien.quievreux.droidplane2"
        minSdk = libs.versions.minSdkVersion.get().toInt()
        targetSdk = libs.versions.targetSdkVersion.get().toInt()
        versionCode = appVersionCode
        versionName = appVersionName
    }
}
```
Remove `android:versionCode` and `android:versionName` from `app/src/main/AndroidManifest.xml`.

### Rationale
- **Modern AGP Standard**: Android Gradle Plugin considers manifest versioning deprecated and recommends defining versioning in `build.gradle.kts`.
- **Single Source of Truth**: Eliminates duplication and ensures that `BuildConfig.VERSION_CODE`, `BuildConfig.VERSION_NAME`, and `PackageInfo` at runtime are derived exclusively from Gradle.
- **Easy Scripting**: Clear variable declarations `val appVersionCode` and `val appVersionName` at the top of the file allow clean, regex-safe automated updating without risking corruption of surrounding DSL structures.

---

## 2. Version Increment Scheme

### Decision
As resolved via user choice (**Option B**):
- `versionCode` increments by +1 (e.g., `18` -> `19`).
- `versionName` synchronizes its patch component directly to `versionCode`: `"1.0." + appVersionCode` (e.g., `"1.0.19"`).

### Rationale
- Direct 1:1 mapping between Android internal build code and public semantic version tag.
- Perfectly compatible with the in-app update checker (`AppVersion` parses `1.0.19` and compares numerically against GitHub release tags `v1.0.19`).

---

## 3. GitHub Actions CI/CD Integration on Master

### Decision
As resolved via user choice (**on `master` branch**), integrate the auto-increment directly into `.github/workflows/android_release.yml`:
1. **Pre-build step**:
   - Extract `appVersionCode` from `app/build.gradle.kts`.
   - Calculate `NEW_CODE=$((appVersionCode + 1))` and `NEW_NAME="1.0.${NEW_CODE}"`.
   - Update `app/build.gradle.kts`.
   - Commit and push to `master` with commit message `chore(release): bump version to ${NEW_NAME} [skip ci]`.
   - Export `NEW_NAME` to `$GITHUB_ENV`.
2. **Build step**:
   - Build signed release APK using `./gradlew assembleRelease`. The resulting APK automatically embeds the new `versionCode` and `versionName`.
3. **Release step**:
   - Publish to GitHub Releases using `tag_name: v${{ env.NEW_NAME }}` and `name: Release v${{ env.NEW_NAME }}`.

### Rationale
- Guarantees that every commit merged to `master` automatically produces a clean version bump, signed APK, and synchronized GitHub release with 0 manual intervention.
- `[skip ci]` avoids recursive workflow executions.
