# Feature Specification: GitHub In-App Update Detection and Installation

**Feature Branch**: `009-check-app-updates`

**Created**: 2026-10-14

**Status**: Ready for Planning

**Input**: User description: "ajoute la détection de mise à jour disponible dans l'application en allant vérifier depuis le dépot github de l'application et permet de la m.a.j à partir de là"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Checking for Available Updates (Priority: P1)

Users want the application to verify whether a newer version of Droidplane has been released on GitHub automatically upon each application launch (when a network connection is available) as well as manually on demand from the app menu, so that they are always kept informed of new releases and bug fixes.

**Why this priority**: Without an app store (Google Play) dependency, users running sideloaded or GitHub-distributed open-source APKs have no automatic notification when new releases are available. In-app update detection is essential for keeping mobile users on supported versions.

**Independent Test**: Trigger an update check when the installed app version (e.g. `1.0.18`) is older than the latest GitHub release tag (e.g. `v1.0.25`). Verify that an update is successfully detected and presented to the user. When the app is on the latest version, verify that a manual check informs the user that the app is up to date.

**Acceptance Scenarios**:

1. **Given** a new release is available on GitHub (`letroll/droidplane/releases/latest`), **When** the application starts up or the user taps "Check for Updates" in the menu, **Then** the application queries GitHub in the background and presents an update dialog if a newer version is available.
2. **Given** the app is already on the latest available version, **When** the user manually checks for updates from the app menu, **Then** the application informs the user with a confirmation that they are using the latest version.
3. **Given** the device has no internet connection or GitHub is unreachable, **When** an update check occurs, **Then** background startup checks fail silently without UI disruption, and manual checks display an informative message explaining that the network check failed.

---

### User Story 2 - Viewing Update Information and Release Notes (Priority: P1)

When a new version is detected, users want to review the update details (new version number, release date, and changelog/release notes) before deciding to download and install the update.

**Why this priority**: Users want to know what changes, fixes, or new capabilities are included in the new version before downloading it.

**Independent Test**: Trigger an update available notification, open the update details dialog, and verify that the target version name, published date, and release notes are displayed clearly.

**Acceptance Scenarios**:

1. **Given** an update is detected, **When** the update dialog is presented, **Then** it clearly displays the current installed version, the new available version, and the release notes extracted from the GitHub release.
2. **Given** the update dialog is open, **When** the user chooses "Later" or dismisses the dialog, **Then** the dialog closes and the user can continue using the application normally without forced interruptions.

---

### User Story 3 - Downloading and Installing the APK via DownloadManager (Priority: P1)

Users want to download and install the updated application version with a single tap, delegating the file download to the Android system `DownloadManager` with a system notification upon completion allowing direct package installation.

**Why this priority**: Delegating download to Android's `DownloadManager` ensures reliable background downloads, system notification progress, and native handoff to the package installer without requiring complex in-app background download workers.

**Independent Test**: When an update is detected, tap the "Update" action. Confirm that the application enqueues the APK download into Android's `DownloadManager`, displays a confirmation snackbar/toast, and once downloaded, tapping the system download notification prompts the Android package installer.

**Acceptance Scenarios**:

1. **Given** an update dialog is shown with an available APK asset, **When** the user taps "Download & Install", **Then** the application enqueues the APK download into Android's `DownloadManager` with visible notifications in the Android notification shade.
2. **Given** the download completes in the system notification shade, **When** the user taps the completed download notification, **Then** the Android package installer opens to install the updated APK.
3. **Given** a release has no APK asset attached or the system `DownloadManager` is unavailable, **When** the user requests the update, **Then** the application provides a direct fallback button to open the GitHub release webpage in the system web browser.

---

### Edge Cases

- **No APK Asset in Release**: A GitHub release was tagged without an attached `.apk` asset; the application detects this and offers a fallback action to open the GitHub release web page in the browser.
- **Offline / Flight Mode**: Automatic launch checks fail silently; manual checks display an explanatory snackbar.
- **Semantic Version Formatting**: GitHub release tags may be formatted as `v1.0.25`, `1.0.25`, or `v1.0.25.1`; version comparison must reliably parse and normalize tags.
- **Older or Equal Version on GitHub**: If the GitHub latest release tag is equal to or numerically lower than the current app version, the app must not prompt for an update.
- **DownloadManager Disabled**: If the system DownloadManager is disabled by the user or OS profile, the app gracefully falls back to opening the APK download URL in the external browser.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST query the GitHub repository releases API (`https://api.github.com/repos/letroll/droidplane/releases/latest`) to check for the latest official release.
- **FR-002**: System MUST compare the latest GitHub release version tag against the installed application version using semantic versioning.
- **FR-003**: System MUST execute an automatic background update check upon every application launch when network connectivity is available.
- **FR-004**: System MUST provide a manual "Check for Updates" action in the application menu.
- **FR-005**: System MUST display an update dialog showing the new version number, release date, and release notes when a newer version is found.
- **FR-006**: System MUST delegate the APK download to Android's `DownloadManager`, configuring a title, description, and completion notification allowing direct package installation.
- **FR-007**: System MUST handle GitHub release tags whether prefixed with `v` or not (e.g. `v1.0.25` and `1.0.25`).
- **FR-008**: System MUST consider only official stable releases via the `/releases/latest` endpoint, excluding draft or pre-release builds.
- **FR-009**: System MUST inform the user with a confirmation message when a manual update check finds no newer version available.
- **FR-010**: System MUST provide a fallback action allowing users to open the GitHub release web page or direct APK URL in an external browser.

### Key Entities

- **Release Information**: Details of the latest GitHub release, including tag name, parsed version, release title, published date, changelog body, html URL, and APK download asset URL.
- **Update Status**: State machine representing the update lifecycle (`Idle`, `Checking`, `UpdateAvailable`, `UpToDate`, `DownloadingEnqueued`, `Error`).
- **App Version**: Normalized representation of the application version (major, minor, patch, run number) enabling accurate comparison.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Update check completes and resolves availability status in under 3 seconds on standard mobile network connections.
- **SC-002**: 100% of valid release APK assets published on the repository can be downloaded via Android DownloadManager without requiring manual file lookup.
- **SC-003**: Background startup update checks never block, delay, or degrade UI interactions or mindmap rendering performance.
- **SC-004**: Users are able to initiate an update in 2 or fewer taps once notified of an available version.
- **SC-005**: Manual update check provides clear feedback to the user in 100% of scenarios (update available, up to date, or offline).

## Assumptions

- The application release artifacts on GitHub are standard signed Android `.apk` files published under repository `letroll/droidplane`.
- The device running the application has Android `DownloadManager` available and enabled.
- The GitHub API rate limit for unauthenticated release queries (60 requests per hour per IP) is sufficient for client-side release checking.

---

## Resolved Clarifications

- **Q1 (Trigger & Frequency)**: Handled via Option C — Automatic background check on every app startup + manual check action in the menu.
- **Q2 (Download & Install Flow)**: Handled via Option B — Delegation to Android's `DownloadManager` with system completion notification for one-tap package installation, plus web browser fallback.
- **Q3 (Pre-Releases / Bêtas)**: Handled via Option A — Official stable releases only (`/releases/latest`).
