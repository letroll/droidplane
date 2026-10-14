# Research & Architectural Decisions: GitHub In-App Updates

**Feature Branch**: `009-check-app-updates`
**Created**: 2026-10-14
**Status**: Completed

## 1. GitHub API Client & JSON Parsing

### Decision
Use standard Android platform components (`java.net.HttpURLConnection` with Kotlin coroutines `Dispatchers.IO` and `org.json.JSONObject`) to query the GitHub releases endpoint:
`https://api.github.com/repos/letroll/droidplane/releases/latest`

### Rationale
- **Zero Overhead**: Does not add third-party networking or serialization libraries (Retrofit, OkHttp, Ktor, Moshi), keeping APK size minimal.
- **Simplicity**: The GitHub `/releases/latest` endpoint returns a single JSON object. Extracting `tag_name`, `name`, `published_at`, `body`, `html_url`, and the `.apk` download asset URL from `assets` array requires under 30 lines of code.
- **Testability**: Pure JVM testable using mock JSON responses and mock response streams.

### Alternatives Considered
- *Add Retrofit / OkHttp*: Rejected as over-engineering for a single read-only HTTP GET query.
- *Add Ktor Client*: Rejected to prevent adding unnecessary KMP dependencies and bloat.

---

## 2. Version Normalization & Semantic Comparison

### Decision
Implement an immutable `AppVersion` class implementing `Comparable<AppVersion>`:
- Strips leading `v` or `V` (e.g. `v1.0.25` -> `1.0.25`).
- Strips pre-release/metadata suffixes (e.g. `-beta`, `+build`).
- Splits by `.` and compares segment by segment numerically (`major`, `minor`, `patch`, `build`). Missing trailing segments default to `0`.
- An update is available if and only if `latestReleaseVersion > currentAppVersion`.

### Rationale
- GitHub Actions CI/CD generates tags in the format `v1.0.${github.run_number}` (e.g. `v1.0.19`), while `AndroidManifest.xml` / `packageInfo.versionName` uses `1.0.18`.
- Normalizing prefixes and comparing numeric segments prevents lexicographical sorting errors (e.g. ensuring `1.0.20` is recognized as newer than `1.0.9`).

### Alternatives Considered
- *String equality / lexicographical comparison*: Rejected because `"1.0.9" > "1.0.10"` lexicographically.
- *External SemVer library*: Rejected to avoid extra dependencies.

---

## 3. APK Download & Installation Delegation

### Decision
As resolved via user choice (**Option B**), delegate the APK download to Android's native `DownloadManager`:
- Set destination to `Environment.DIRECTORY_DOWNLOADS` with file name `droidplane-v<version>.apk`.
- Set notification visibility to `Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED`.
- Set MIME type to `application/vnd.android.package-archive`.
- Upon download completion in the system shade, tapping the notification opens the system package installer.
- In-app fallback: A button to open the release webpage or direct APK URL in the external browser if `DownloadManager` is unavailable or disabled.

### Rationale
- Native background downloading handled safely by the Android OS, surviving app process termination.
- Zero need for custom foreground download services or complex in-app progress workers.
- Avoids strict runtime installation permission requirements on newer Android versions until the user explicitly taps the system installer prompt.

### Alternatives Considered
- *Option A (In-app custom download + FileProvider + REQUEST_INSTALL_PACKAGES)*: More complex, requires declaring sensitive permissions in manifest and managing download stream states.
- *Option C (Browser redirect only)*: Less seamless for mobile users.

---

## 4. Triggering Strategy & User Notification

### Decision
As resolved via user choice (**Option C**):
1. **Startup Check**:
   - Asynchronous non-blocking check initiated during app launch on `Dispatchers.IO`.
   - If an update is detected, emit `UpdateState.UpdateAvailable(releaseInfo)` to display the update dialog.
   - If no update is found or a network error occurs, fail silently without disrupting the user's workflow.
2. **Manual Check**:
   - User taps "Check for Updates" in the top bar overflow menu.
   - Shows feedback for all outcomes:
     - Update available: Displays the update dialog.
     - Up to date: Displays an informative snackbar ("You are using the latest version").
     - Network error: Displays an error snackbar ("Unable to check for updates. Please check your connection").
