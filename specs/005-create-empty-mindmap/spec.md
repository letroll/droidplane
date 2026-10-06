# Feature Specification: Create Empty Mindmap with Demo Access

**Feature Branch**: `005-create-empty-mindmap`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "créer à présent la possibilité dans l'application de créer une mindmap vide tout en préservant la possibilité d'accéder au fichier d'aide servant aussi de démo"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create a New Empty Mindmap (Priority: P1) 🎯 MVP

As a user starting a new thinking or brainstorming session, I want to create a new empty mindmap directly from the application's options menu, so that I can immediately start organizing my thoughts on a fresh canvas without having to manually delete nodes from an existing or demo mindmap.

**Why this priority**: Essential core functionality for any mind mapping tool. Without the ability to create a clean mindmap, users are forced to repurpose the demo tutorial file.

**Independent Test**: Open the application, tap the top bar menu (⋮), choose "New Mindmap" ("Nouvelle carte"), and verify that a clean mindmap with a single central root node appears on screen, ready for adding children and editing.

**Acceptance Scenarios**:

1. **Given** the user is viewing any mindmap in the application, **When** the user opens the top bar options menu and selects "New Mindmap", **Then** the application initializes and displays a new mindmap consisting of a single root node with a standard default title (e.g. "Central Idea").
2. **Given** a new empty mindmap is created, **When** displayed on the screen, **Then** the root node is centered, selected, previous navigation history is cleared, and search results are reset.
3. **Given** the active mindmap has unsaved modifications, **When** the user selects "New Mindmap", **Then** the system displays a confirmation dialog warning the user that unsaved changes will be discarded, with options to proceed or cancel.
4. **Given** the confirmation dialog is displayed, **When** the user cancels, **Then** the current mindmap and unsaved edits remain intact without creating a new map.

---

### User Story 2 - Access Help & Demo Mindmap at Any Time (Priority: P1)

As a user exploring or learning Droidplane, I want dedicated access to the bundled help & tutorial mindmap (which also serves as a comprehensive demo), so that I can consult user guidance, discover features, or inspect an example mindmap structure at any time, even after creating a new mindmap.

**Why this priority**: Directly requested by the user ("tout en préservant la possibilité d'accéder au fichier d'aide servant aussi de démo"). Preserves the educational and illustrative value of the bundled `example.mm` file.

**Independent Test**: From an empty or user-created mindmap, open the options menu, select "Help & Demo", and verify that the full tutorial mindmap (`example.mm`) loads and displays properly with all nodes and formatting intact.

**Acceptance Scenarios**:

1. **Given** the user is viewing an empty or user-created mindmap, **When** the user opens the top bar options menu and selects "Help & Demo" (or "Help"), **Then** the bundled tutorial mindmap (`example.mm`) is loaded and rendered on screen.
2. **Given** the user has unsaved modifications on their current mindmap, **When** the user selects "Help & Demo", **Then** the system displays a confirmation dialog warning of unsaved changes before discarding them to load the demo.
3. **Given** the demo mindmap is loaded, **When** the user explores it, **Then** all tutorial nodes, internal links, icons, and formatted content are fully browsable and interactive.

---

### User Story 3 - Customizing the Central Topic of a New Mindmap (Priority: P2)

As a user who just created an empty mindmap, I want to easily rename the central root node, so that my new document is clearly identified by its main subject.

**Why this priority**: High user value for personalization and document identification right after creation.

**Independent Test**: Create an empty mindmap, tap or long-press the central root node, select "Edit", enter a new title (e.g., "Project Roadmap"), confirm, and verify the top bar title and root card update immediately.

**Acceptance Scenarios**:

1. **Given** an empty mindmap is displayed, **When** the user edits the root node text via the context menu or edit action, **Then** the edit dialog is displayed with the current root text pre-filled.
2. **Given** the user enters a new title and confirms, **When** the update is applied, **Then** the central root node on the canvas and the application top bar title reflect the new text, and the document is marked as having unsaved changes.

---

### User Story 4 - Startup Chooser: Create New or Open Recent Files (Priority: P1)

As a user launching Droidplane, I want the application to present a startup chooser offering to create a new empty mindmap, pick from recently edited files (if they still exist), browse for a file, or open the demo, so that I can immediately resume my work or start fresh without being forced into a demo file.

**Why this priority**: Solves the initial friction of app launch. Allows users to jump directly into their recent work or start an empty mindmap immediately.

**Independent Test**: Launch the application without intent arguments; verify that a startup chooser appears with options to create a new mindmap, browse files, access help & demo, and lists existing recent files; verify tapping a recent file opens it, and missing files are excluded from the list.

**Acceptance Scenarios**:

1. **Given** the application is launched from the app launcher (without an external file intent), **When** the startup screen loads, **Then** a startup chooser is displayed presenting options to "Create New Mindmap", "Browse File...", "Help & Demo", and a list of recently edited files.
2. **Given** recent files exist in storage and have valid read access, **When** the startup chooser is shown, **Then** those files are displayed with their names and last-opened dates.
3. **Given** a recently edited file was deleted or moved externally, **When** the startup chooser is populated, **Then** the missing file is automatically filtered out from the recent files list.
4. **Given** the user taps "Create New Mindmap" in the startup chooser, **When** confirmed, **Then** an empty mindmap with a central root node is opened and the chooser is dismissed.
5. **Given** the user taps an existing recent file in the list, **When** selected, **Then** the file is loaded into the workspace and the chooser is dismissed.
6. **Given** the application is launched with an external file intent (`ACTION_VIEW`, `ACTION_EDIT`, or `ACTION_OPEN_DOCUMENT`), **When** the activity opens, **Then** the startup chooser is bypassed and the specified file is loaded directly.

---

### Edge Cases

- **Unsaved Changes on Current Map**: If the user has made edits to the current document, triggering "New Mindmap" or "Help & Demo" must show a confirmation dialog preventing accidental loss of unsaved work.
- **Saving a Newly Created Mindmap**: An in-memory new mindmap does not yet have an on-disk file path; triggering "Save" must safely initiate the file creation or export flow without crashing.
- **Deleted or Moved Recent Files**: Files listed in recent history that no longer exist on storage or whose permissions were revoked must be gracefully detected, filtered out, and omitted from the list.
- **Rapid Navigation / Document Switching**: Creating a new map or switching to the demo file must cancel any running search operations, reset search results, and clear the undo/redo stack associated with the previous document.
- **Display Mode Preservation**: Creating a new empty mindmap or loading the demo file must retain the user's active display mode (Mind Map view or List view).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide an action in the top bar options menu to create a new empty mindmap.
- **FR-002**: Creating a new empty mindmap MUST initialize an in-memory document containing exactly one root node with a default title (e.g., "Central Idea").
- **FR-003**: Creating a new empty mindmap MUST reset previous navigation stack, search query, search results, active selection, and branch folding state.
- **FR-004**: If the current mindmap has unsaved modifications, the system MUST display a confirmation dialog before creating a new mindmap, giving the user the choice to proceed (discarding changes) or cancel.
- **FR-005**: The system MUST provide an action in the top bar options menu to load the bundled help & demo mindmap document (`example.mm`).
- **FR-006**: If the current mindmap has unsaved modifications, the system MUST display a confirmation dialog before loading the help & demo mindmap, giving the user the choice to proceed or cancel.
- **FR-007**: Loading the help & demo mindmap MUST populate the workspace with the complete tutorial mindmap from bundled resources, resetting prior document state cleanly.
- **FR-008**: Newly created empty mindmaps MUST support full editing capabilities (editing root text, adding child nodes, deleting nodes, undoing deletions, and saving).
- **FR-009**: The display mode (Mind Map view vs. List view) MUST be preserved when creating a new mindmap or loading the demo mindmap.
- **FR-010**: The system MUST display a startup chooser upon launching the application from the launcher without an external file intent.
- **FR-011**: The startup chooser MUST offer actions to create a new empty mindmap, browse for a file via the system picker, access the bundled Help & Demo, and list recently edited files.
- **FR-012**: The system MUST persist a history of recently opened and saved mindmap files.
- **FR-013**: When presenting recent files, the system MUST check that each file still exists and is accessible, filtering out deleted or inaccessible files.
- **FR-014**: Selecting an existing recent file from the startup chooser MUST load that document directly into the workspace.

### Key Entities

- **Mindmap Document**: An in-memory hierarchical tree of `Node` elements originating from an external file, a bundled resource (`example.mm`), or a freshly initialized single root node.
- **Root Node**: The top-level ancestor node (`parentNode = null`) serving as the central subject of the mindmap.
- **Discard Confirmation**: A confirmation dialogue state ensuring users do not inadvertently lose unsaved edits when switching documents or creating a new map.
- **Recent File**: An entry recording a previously accessed mindmap with its storage URI, display name, and last accessed timestamp.
- **Startup Chooser**: A startup dialog presented on app open offering quick actions and recent file shortcuts.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can create and display a new empty mindmap in under 1 second from the options menu or startup chooser.
- **SC-002**: 100% of attempts to create a new mindmap or load the demo file while unsaved modifications exist display an unsaved changes warning dialog.
- **SC-003**: Users can load the complete bundled help & demo mindmap in under 2 seconds from the options menu or startup chooser at any time.
- **SC-004**: 100% of standard authoring actions (add child, edit root, delete, undo) function successfully on newly created empty mindmaps.
- **SC-005**: 100% of deleted or inaccessible recent files are filtered out so that only existing files appear in the startup chooser.

## Assumptions

- A newly created mindmap initializes with a single root node having default text "Central Idea" (localized as "Nouvelle carte"), allowing immediate branching.
- The bundled `example.mm` file in raw resources remains the permanent source for the Help & Demo mindmap.
- The standard confirmation dialog (`DialogType`) is utilized for the unsaved changes warning before creating a new map or loading demo content.
- Recent files are stored persistently using SharedPreferences and verified against device storage before display.
