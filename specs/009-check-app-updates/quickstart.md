# Quickstart & Verification Guide: GitHub In-App Updates

**Feature Branch**: `009-check-app-updates`
**Created**: 2026-10-14
**Status**: Ready

## 1. Automated Verification (JVM Unit Tests)

Verify version parsing, comparison, GitHub release JSON extraction, and update availability detection without emulator:

```bash
./gradlew :app:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.update.*"
```

Run all unit tests:

```bash
./gradlew test
```

---

## 2. Interactive Verification Scenarios

### Scenario 1: Manual Update Check (Up to Date)
1. Launch the app on an Android emulator or device connected to the internet:
   ```bash
   ./gradlew installDebug
   ```
2. Open the top bar overflow menu (three dots) and tap **Check for updates**.
3. Verify that an informative snackbar appears confirming: *"You are using the latest version"* (when installed version matches or exceeds GitHub release tag).

---

### Scenario 2: Update Available Notification & Changelog
1. Launch app with a mock lower version or when a newer GitHub release exists.
2. Verify that the **Update Available Dialog** appears:
   - Displays the target version (e.g. `v1.0.25`).
   - Displays the current version (e.g. `1.0.18`).
   - Displays formatted release notes / changelog.
   - Shows action buttons: **Download & Install**, **View on GitHub**, **Later**.
3. Tap **Later**: verify the dialog dismisses and mindmap editing continues smoothly.

---

### Scenario 3: Download & Installation Delegation
1. Open the Update Available dialog.
2. Tap **Download & Install**:
   - Verify a toast/snackbar appears confirming that the download has been enqueued.
   - Swipe down the Android notification shade: verify that Android's DownloadManager is actively downloading the APK file with a progress bar.
   - When download completes, tap the completed notification in the shade: verify the Android package installer opens to install the new version.
3. Tap **View on GitHub**: verify the device's web browser opens to the GitHub release page.

---

### Scenario 4: Offline / Network Error Handling
1. Put the device into Airplane Mode (disable Wi-Fi and mobile data).
2. Launch the app: verify no crashing occurs and no blocking dialog appears on startup.
3. Open the menu and tap **Check for updates**: verify an explanatory message appears (e.g. *"Unable to check for updates. Please check your connection."*).
