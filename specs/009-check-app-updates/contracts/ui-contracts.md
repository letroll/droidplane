# UI & Service Contracts: GitHub In-App Updates

**Feature Branch**: `009-check-app-updates`
**Created**: 2026-10-14
**Status**: Completed

## 1. UI Components

### `UpdateAvailableDialog`
Composable dialog presented when a newer version of Droidplane is detected.

```kotlin
@Composable
fun UpdateAvailableDialog(
    currentVersion: String,
    release: GitHubRelease,
    onDownloadAndInstall: (apkUrl: String, fileName: String) -> Unit,
    onViewOnGitHub: (url: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
)
```
- **Title**: "Update Available: ${release.tagName}"
- **Version Info**: "Current: $currentVersion • New: ${release.tagName}"
- **Changelog Section**: Scrollable card displaying `release.body`.
- **Actions**:
  - Positive button: "Download & Install" (enabled if `release.apkDownloadUrl != null`).
  - Neutral button: "View on GitHub".
  - Dismiss button: "Later".

---

### `AppTopBarAction.CheckForUpdates`
New action item added to `AppTopBarAction`:
```kotlin
data object CheckForUpdates : AppTopBarAction
```
Added as a dropdown menu item in `AppTopBar.kt`:
```kotlin
DropdownMenuItem(
    onClick = {
        showMenu = false
        onBarAction(AppTopBarAction.CheckForUpdates)
    },
    text = { Text(stringResource(R.string.check_for_updates)) }
)
```

---

## 2. Service Contracts

### `UpdateCheckerService`
Interface for querying GitHub and checking version compatibility.

```kotlin
interface UpdateCheckerService {
    suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult
}
```

### `ApkDownloadHelper`
Helper for enqueuing downloads with Android `DownloadManager`.

```kotlin
object ApkDownloadHelper {
    fun enqueueDownload(
        context: Context,
        apkUrl: String,
        fileName: String,
        title: String = "Droidplane Update"
    ): Long
}
```
