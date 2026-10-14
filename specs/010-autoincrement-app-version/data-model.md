# Data Model: Version Definition and Auto-Increment

**Feature Branch**: `010-autoincrement-app-version`
**Created**: 2026-10-14
**Status**: Completed

## 1. Entities & Configuration Structure

### `GradleVersionConfig` (in `app/build.gradle.kts`)
Represents the version state configured in Gradle.

| Property | Type | Default / Example | Description |
|---|---|---|---|
| `appVersionCode` | `Int` | `18` | Android version code, incremented monotonically |
| `appVersionName` | `String` | `"1.0.18"` | Semantic version string formatted as `"1.0." + appVersionCode` |

---

## 2. State Transition / Increment Model

```text
[Current Version in build.gradle.kts]
  appVersionCode = N
  appVersionName = "1.0.N"
           │
           ▼ (Push to master triggers android_release.yml)
[Increment Operation]
  NEW_CODE = N + 1
  NEW_NAME = "1.0." + NEW_CODE
           │
           ├─► Update app/build.gradle.kts
           ├─► Commit to master: "chore(release): bump version to 1.0.(N+1) [skip ci]"
           ├─► Compile signed APK (embeds versionCode N+1, versionName 1.0.(N+1))
           └─► Publish GitHub Release (tag: v1.0.(N+1))
```

---

## 3. Validation Rules

- `appVersionCode` must be a strictly positive integer (`> 0`).
- `appVersionName` must match the format `"1.0." + appVersionCode`.
- `AndroidManifest.xml` must not define conflicting `android:versionCode` or `android:versionName` attributes.
