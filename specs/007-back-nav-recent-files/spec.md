# Feature Specification: Back Navigation to Recent Files

**Feature Branch**: `007-back-nav-recent-files`

**Created**: 2026-10-12

**Status**: Draft

**Input**: User description: "lorsqu'une carte est ouverte et n'a pas de changement à sauvegarder, lors d'un back ouvrir le menu des fichiers récent au lieu de fermer l'app, et sur ce dialogue ajouter un bouton pour fermer l'app. Ajouter aussi un accés à ce dialog dans les menus de la toolbar ou l'équivalent grand écran"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Back Navigation Shows Recent Files When No Unsaved Changes (Priority: P1)

When a user has a mindmap open with no unsaved changes and presses the system back button or the toolbar back button, instead of closing the app, a Recent Files dialog appears showing their recently accessed mindmaps.

**Why this priority**: This is the core behavioral change requested. It prevents accidental app closure and provides quick access to recent work.

**Independent Test**: Can be fully tested by opening a mindmap, making no changes, pressing back, and verifying the Recent Files dialog appears with the correct list of recent files.

**Acceptance Scenarios**:

1. **Given** a mindmap is open with no unsaved changes, **When** the user presses the system back button, **Then** the Recent Files dialog opens instead of closing the app
2. **Given** a mindmap is open with no unsaved changes, **When** the user presses the toolbar back button, **Then** the Recent Files dialog opens instead of navigating up
3. **Given** the Recent Files dialog is open, **When** the user selects a recent file, **Then** that mindmap loads and the dialog closes
4. **Given** the Recent Files dialog is open, **When** the user presses "New Mindmap", **Then** a new empty mindmap is created and the dialog closes

### User Story 2 - Exit App Button on Recent Files Dialog (Priority: P1)

The Recent Files dialog includes an explicit "Exit App" button that closes the application when pressed.

**Why this priority**: Users need a clear way to actually exit the app when they intend to, since back no longer closes it directly.

**Independent Test**: Can be fully tested by opening the Recent Files dialog (via back press with no unsaved changes) and pressing the Exit App button, verifying the app closes.

**Acceptance Scenarios**:

1. **Given** the Recent Files dialog is open, **When** the user presses "Exit App", **Then** the application closes completely
2. **Given** the Recent Files dialog is open, **When** the user presses the dialog's cancel/dismiss action, **Then** the dialog closes and the user returns to the mindmap

### User Story 3 - Toolbar Menu Access to Recent Files Dialog (Priority: P2)

Users can access the Recent Files dialog from the toolbar overflow menu (three-dot menu) on all screen sizes, and from an equivalent location on large screens/tablets.

**Why this priority**: Provides discoverable access to recent files without requiring the back gesture, especially important on large screens where back behavior may differ.

**Independent Test**: Can be fully tested by opening the toolbar menu and selecting "Recent Files", verifying the dialog opens with the correct recent files list.

**Acceptance Scenarios**:

1. **Given** a mindmap is open, **When** the user opens the toolbar overflow menu and selects "Recent Files", **Then** the Recent Files dialog opens
2. **Given** a mindmap is open on a large screen/tablet, **When** the user accesses the equivalent menu location, **Then** the Recent Files dialog opens
3. **Given** the Recent Files dialog is opened via the toolbar menu, **When** the user selects a file or creates new, **Then** the appropriate action occurs and dialog closes

### User Story 4 - Back Navigation Still Closes App When Unsaved Changes Exist (Priority: P1)

When a mindmap has unsaved changes, the existing Exit Confirmation dialog appears on back press, preserving current behavior.

**Why this priority**: Must not lose the existing safety net for unsaved work.

**Independent Test**: Can be fully tested by making changes to a mindmap, pressing back, and verifying the Exit Confirmation dialog (not Recent Files) appears.

**Acceptance Scenarios**:

1. **Given** a mindmap has unsaved changes, **When** the user presses back, **Then** the Exit Confirmation dialog appears
2. **Given** the Exit Confirmation dialog is open, **When** the user confirms exit, **Then** the app closes without saving
3. **Given** the Exit Confirmation dialog is open, **When** the user cancels, **Then** they return to the mindmap with changes preserved

---

### Edge Cases

- What happens when there are no recent files? The dialog should show "No recent files found" with options to create new or browse
- How does system handle rapid back presses? The dialog should not stack; subsequent back presses while dialog is open should dismiss it
- What if the user rotates the screen while the dialog is open? Dialog state should be preserved
- How does this interact with the existing Startup Chooser dialog? They should be mutually exclusive - only one dialog at a time

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST show the Recent Files dialog when the user presses back (system or toolbar) on a mindmap with no unsaved changes
- **FR-002**: System MUST show the Exit Confirmation dialog when the user presses back on a mindmap WITH unsaved changes (existing behavior preserved)
- **FR-003**: The Recent Files dialog MUST display a list of recently accessed mindmap files with timestamps
- **FR-004**: The Recent Files dialog MUST include a "New Mindmap" button to create a new empty mindmap
- **FR-005**: The Recent Files dialog MUST include a "Browse" button to open the file picker
- **FR-006**: The Recent Files dialog MUST include an "Exit App" button that closes the application
- **FR-007**: The Recent Files dialog MUST include a "Help & Demo" button to open the bundled demo mindmap
- **FR-008**: The Recent Files dialog MUST include a cancel/dismiss action to return to the current mindmap
- **FR-009**: The toolbar overflow menu MUST include a "Recent Files" menu item that opens the Recent Files dialog
- **FR-010**: On large screens/tablets, an equivalent menu access point MUST be available for opening the Recent Files dialog
- **FR-011**: Selecting a recent file from the dialog MUST load that mindmap and close the dialog
- **FR-012**: The Recent Files dialog MUST reuse the existing RecentFilesRepository for fetching recent files
- **FR-013**: The Recent Files dialog MUST validate file accessibility before showing (using existing createStorageFileChecker)

### Key Entities

- **RecentFile**: Represents a recently accessed mindmap file with URI, display name, and timestamp
- **RecentFilesDialog**: The dialog component showing recent files list and action buttons
- **RecentFilesRepository**: Existing data layer component for persisting and retrieving recent files list

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can access recent files from back navigation in ≤2 interactions (back press → select file)
- **SC-002**: Users can exit the app intentionally via the Exit App button in ≤2 interactions (back press → Exit App)
- **SC-003**: 100% of existing unsaved-changes exit flows continue to work unchanged (regression test)
- **SC-004**: Recent Files dialog loads and displays within 500ms on typical devices
- **SC-005**: Toolbar menu access to Recent Files is discoverable within 3 seconds for new users

## Assumptions

- The existing StartupChooserDialog component can be reused/extended for the Recent Files dialog since it already has the required UI structure
- The RecentFilesRepository and createStorageFileChecker utilities are sufficient for file accessibility checking
- The app uses standard Android back handling via BackHandler in Compose
- Large screen / tablet layouts use the same toolbar overflow menu pattern (no separate navigation rail needed)
- The "Exit App" action calls Activity.finish() similar to the existing ExitConfirmationDialog
- No new permissions or system-level integrations are required
- Existing dialog infrastructure (DialogType sealed class, DialogUiState) supports adding a new dialog type