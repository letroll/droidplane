# Data Model: GitHub In-App Updates

**Feature Branch**: `009-check-app-updates`
**Created**: 2026-10-14
**Status**: Completed

## 1. Models & Value Objects

### `AppVersion`
A normalized numerical representation of a semantic application version.

```kotlin
data class AppVersion(
    val parts: List<Int>,
    val raw: String
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val maxLen = maxOf(parts.size, other.parts.size)
        for (i in 0 until maxLen) {
            val p1 = parts.getOrElse(i) { 0 }
            val p2 = other.parts.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    companion object {
        fun parse(versionStr: String): AppVersion {
            val cleaned = versionStr.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .substringBefore('+')
            val parts = cleaned.split('.').mapNotNull { it.toIntOrNull() }
            return AppVersion(parts, versionStr)
        }
    }
}
```

---

### `GitHubRelease`
Domain entity representing a published release retrieved from the GitHub API.

| Field | Type | Description |
|---|---|---|
| `tagName` | `String` | Raw tag name (e.g. `"v1.0.25"`) |
| `name` | `String` | Release title (e.g. `"Release v1.0.25"`) |
| `publishedAt` | `String` | ISO 8601 published date/time |
| `body` | `String` | Changelog and release notes |
| `htmlUrl` | `String` | Browser URL pointing to the release page |
| `apkDownloadUrl` | `String?` | Direct download URL for the `.apk` asset (null if no APK attached) |
| `apkFileName` | `String?` | File name of the attached APK (e.g. `"app-release.apk"`) |
| `apkSize` | `Long?` | Size in bytes of the attached APK |

---

### `UpdateCheckResult` (Sealed Interface)
Result emitted by the update checking service.

```kotlin
sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val release: GitHubRelease
    ) : UpdateCheckResult

    data class UpToDate(
        val currentVersion: String
    ) : UpdateCheckResult

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : UpdateCheckResult
}
```

---

## 2. UI State (`UpdateUiState`)

Represents the presentation state in Compose:

```kotlin
data class UpdateUiState(
    val isChecking: Boolean = false,
    val updateDialogRelease: GitHubRelease? = null,
    val feedbackMessage: String? = null
)
```
