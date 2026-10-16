# Feature Specification: Replace Existing File on Save

**Feature Branch**: `011-replace-existing-file-save`

**Created**: 2026-10-16

**Status**: Draft

**Input**: User description: "J'aimerai à la sauvegarde que si je suis dans l'édition d'un fichier existant, à la validation dans le filepicker système le fichier existant soit remplacer avec éventuellement le flow de confirmation. Actuellement un nouveau fichier est créer avec le nom du fichier d'origine et le postfix (n) où n est un incrément"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Replace Existing File When Saving Edits (Priority: P1)

As a user editing an existing mindmap file, when I save my changes and validate the save operation, I want the original file to be replaced with my updated mindmap (with a confirmation step before overwriting) rather than having a duplicate file created with an incremented `(n)` suffix.

**Why this priority**: Core user request. Creating duplicate files with `(1)`, `(2)` suffixes every time an existing mindmap is saved clutters the user's storage and causes confusion about which file is the authoritative version.

**Independent Test**: Open an existing `.mm` file, modify a node, trigger the save action, validate the save and confirm the replacement if prompted, and verify that the original file contains the updated changes while no new `filename (1).mm` file exists in the directory.

**Acceptance Scenarios**:

1. **Given** the user has opened an existing mindmap file and made modifications, **When** the user triggers the save action and validates saving to the existing file (confirming the overwrite prompt if presented), **Then** the existing file is updated in place with the new mindmap content and no duplicate file with a `(n)` suffix is created.
2. **Given** the user is prompted to confirm replacing the existing file during the save flow, **When** the user cancels or declines the confirmation, **Then** the existing file on storage remains untouched and the user retains their unsaved changes in the active workspace.
3. **Given** the user has opened an existing mindmap file and saves changes multiple times in a row, **When** each save is validated and confirmed, **Then** the same original file is updated each time without creating `(1)`, `(2)`, or `(n)` suffixed copies.

---

### User Story 2 - Save New Mindmap or Save Under a New Name (Priority: P2)

As a user working on a newly created mindmap (or wishing to save a copy under a different name), I want to choose a destination and file name in the system file picker so that a new file is created when no existing file is being replaced.

**Why this priority**: Preserves the ability to save brand-new mindmaps (or Help & Demo maps) that do not correspond to an existing user file, as well as saving under a distinct filename when desired.

**Independent Test**: Create a new empty mindmap, add a node, trigger Save, enter a new filename in the system file picker, validate, and verify that the new file is created at the chosen location and becomes the active document.

**Acceptance Scenarios**:

1. **Given** the user is editing a newly created mindmap or the bundled demo mindmap (which has no existing user file on storage), **When** the user triggers the save action, **Then** the system file picker opens allowing the user to select a destination folder and filename.
2. **Given** the system file picker is open for saving, **When** the user enters a new filename that does not match an existing file and validates, **Then** a new file is created with that exact name and becomes the active file for subsequent edits.
3. **Given** the user is editing an existing file, **When** the user explicitly chooses to save under a different filename in the system file picker, **Then** a new file is created with the chosen name while the original file remains unchanged.

---

### User Story 3 - Maintain Accurate Recent Files History After Replacement (Priority: P3)

As a user who saves updates to an existing mindmap file, I want my recent files list to keep referencing the original file without being polluted by incremented `(n)` copies, so that I always reopen the latest version of my mindmap.

**Why this priority**: Ensures continuity across sessions so users do not accidentally reopen an outdated file or see multiple numbered duplicates in the startup chooser.

**Independent Test**: Open a file from the recent files list, edit and save it by replacing the existing file, return to the startup chooser, and verify that only the single original file entry appears with an updated last-accessed timestamp.

**Acceptance Scenarios**:

1. **Given** an existing mindmap file is replaced during save, **When** the save completes, **Then** the recent files history updates the timestamp of the original file entry and does not add a new `(n)` suffixed entry.
2. **Given** an existing mindmap file is replaced during save, **When** the save completes, **Then** the document's unsaved changes indicator is cleared and the user can exit or switch documents without an "unsaved changes" warning.

---

### Edge Cases

- **System File Picker Auto-Increment Behavior**: When the system file picker automatically appends a `(n)` suffix (such as `map (1).mm`) because a file with the default name (`map.mm`) already exists in the selected directory while editing `map.mm`, the system must ensure the original file is replaced (with confirmation) and any temporary suffixed file created by the picker is cleaned up or avoided so no `(n)` duplicate remains.
- **Read-Only or Revoked Write Permission on Original File**: If the original file was opened in read-only mode or write access to the original file location is unavailable, the system must fall back to the system file picker and inform the user if the original file cannot be overwritten directly.
- **Interrupted or Failed Write**: If an error occurs while replacing the existing file (e.g., storage full or access denied), the original file must not be left in a corrupted or empty state, and the user must be shown a clear error message while keeping their in-memory edits intact.
- **User Cancels System File Picker**: If the user dismisses the system file picker without validating, no file is modified or created, and the workspace remains in its current state with unsaved modifications preserved.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: When the user is editing an existing mindmap file and initiates a save, the system MUST allow replacing the existing file in place rather than leaving a new file with an incremented `(n)` suffix.
- **FR-002**: Before overwriting an existing file during the save workflow, the system MUST present a confirmation flow (either via an in-app confirmation dialog or the system file picker's overwrite confirmation) so the user explicitly confirms replacing the existing file.
- **FR-003**: If the system file picker creates a new file entry with an incremented `(n)` suffix when the user validates saving with the original file's name, the system MUST write the updated content to the original existing file and remove the duplicate `(n)` file so only the updated original file remains.
- **FR-004**: If the user cancels the overwrite confirmation or dismisses the system file picker, the system MUST abort the save operation without modifying the existing file and MUST preserve all unsaved changes in the active workspace.
- **FR-005**: When the user is editing a mindmap that does not yet have an associated writable file on storage (such as a newly created empty mindmap or the bundled demo mindmap), triggering Save MUST open the system file picker to create a new file.
- **FR-006**: Once a newly created mindmap is saved to a file for the first time, subsequent saves during the same or future sessions MUST treat that file as an existing file eligible for in-place replacement.
- **FR-007**: Replacing an existing file MUST preserve data integrity so that an interrupted or failed save does not corrupt or truncate the existing file without recovery.
- **FR-008**: Upon successfully replacing an existing file, the system MUST clear the unsaved modifications state for the active mindmap and update the original file's entry in the recent files list without adding duplicate `(n)` entries.
- **FR-009**: If replacing the existing file fails due to storage errors or missing permissions, the system MUST notify the user with an error message and keep the in-memory mindmap state intact.

### Key Entities *(include if feature involves data)*

- **Active Mindmap File Reference**: The reference to the currently opened mindmap file on device storage, including its display name, location identifier, and whether it supports direct replacement.
- **Overwrite Confirmation**: The user-facing confirmation prompt asking whether to replace the existing file or cancel/save as a new file.
- **Recent File Entry**: The persisted record of a recently opened or saved mindmap file, uniquely identified by its storage location so that in-place updates refresh the existing entry rather than creating duplicates.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of saves confirmed for an already-opened existing file update the original file in place with zero unwanted `(n)` suffixed duplicate files remaining in storage.
- **SC-002**: Users can complete saving updates to an existing mindmap file in 2 interactions or fewer (triggering save and confirming replacement).
- **SC-003**: 100% of cancelled overwrite confirmations leave the original file completely unmodified on disk and preserve all in-memory user edits.
- **SC-004**: 0 duplicate `(n)` file entries appear in the Recent Files list after repeatedly editing and saving an existing mindmap.

## Assumptions

- When editing an existing file that already has a valid writable storage reference, the user's primary intent when pressing "Save" is to update that existing file (after confirming replacement) rather than creating incremented copies (`filename (1).mm`, `filename (2).mm`).
- If the user goes through the system file picker and validates without changing the suggested base name (which the OS file picker may automatically turn into `filename (1).mm`), or selects the existing file, the user intends to replace the existing file they are currently editing.
- If the user explicitly types a *different* base filename in the system file picker (not matching the original file name), the user intends to save a new file under that new name.
- Newly created mindmaps and the bundled Help & Demo mindmap do not have an existing user file on disk until saved for the first time via the system file picker.
