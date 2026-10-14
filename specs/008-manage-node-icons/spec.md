# Feature Specification: Manage and Display Freeplane Node Icons

**Feature Branch**: `008-manage-node-icons`

**Created**: 2026-10-14

**Status**: Ready for Planning

**Input**: User description: "freeplane permet l'affichage d'icon dans les nodes et la gestion qui va avec. Adapte cela au projet avec les icons déjà disponible et demande moi comment faire pour les associations manquantes éventuelles"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Visual Display of Assigned Node Icons (Priority: P1)

Users browsing a mindmap on mobile (whether in the canvas view or the node hierarchy list view) need to see Freeplane icons attached to a node, rendered in their exact defined sequence alongside the node title, so that visual cues (priorities, flags, checkmarks, progress) are immediately recognizable just as they are in desktop Freeplane.

**Why this priority**: Displaying icons is the foundation of Freeplane visual communication. Users rely on icons to scan and triage tasks, priorities, and categories. Without accurate rendering of icons on nodes, mindmaps lose critical contextual meaning.

**Independent Test**: Open a mindmap containing nodes with various Freeplane icons (e.g. `button_ok`, `priority-1`, `flag-red`). Confirm that the icons appear adjacent to each node label in both the interactive canvas view and the outline list view in the correct order.

**Acceptance Scenarios**:

1. **Given** a node has one or more assigned icons (e.g. `yes`, `flag-blue`), **When** viewing the node in the mindmap canvas, **Then** all assigned icons are rendered next to the node title in the order they were defined.
2. **Given** a node has multiple assigned icons, **When** viewing the node in the node hierarchy list view, **Then** up to 3 icons are rendered adjacent to the node item title, and if more than 3 icons are present, an overflow badge (e.g. `+N`) is displayed to indicate additional icons while preserving title readability.
3. **Given** a node has both an attached link/rich-text indicator and user-assigned icons, **When** viewing the node, **Then** the user-assigned icons are clearly distinguishable from structural indicators (links, rich text, notes).

---

### User Story 2 - Managing Node Icons: Adding, Removing, and Reordering (Priority: P1)

Users editing a node need a visual management interface (within the node properties inspector or an icon picker dialog) to inspect all currently attached icons, remove unwanted icons, rearrange their display order, and pick new icons from a visual catalog of available icons with graphical previews.

**Why this priority**: Enabling icon management allows users to update task states, change priorities, and categorize thoughts directly on mobile without needing desktop Freeplane.

**Independent Test**: Select a node, open the icon management interface, browse the catalog to add two icons (e.g. `flag-green` and `checked`), remove an existing icon, save changes, and verify the updated icon set is reflected in the mindmap and saved to the document.

**Acceptance Scenarios**:

1. **Given** a node is selected in the editor, **When** the user opens the icon management interface, **Then** all active icons for that node are shown with their visual graphics and names, each with an option to remove it.
2. **Given** the user is viewing the icon catalog, **When** the user taps an icon from the catalog, **Then** that icon is immediately appended to the node's assigned icons and displayed in the active list.
3. **Given** a node has active icons, **When** the user removes an icon or clears all icons, **Then** the active icon list updates immediately and changes persist to the document upon confirmation.
4. **Given** a node has multiple icons, **When** the user reorders the icons, **Then** the new sequence is preserved and reflected in the mindmap view.

---

### User Story 3 - Unified Icon Catalog and Unmapped Icon Handling (Priority: P2)

The application unifies all existing icon resources (bundled Freeplane drawables, vector icons, and font symbols) into a standardized catalog mapped to standard Freeplane icon identifiers (normalizing differences such as dashes and underscores). For any Freeplane icon name present in a document that lacks an exact visual asset match, the application applies a short textual badge fallback without corrupting the document XML.

**Why this priority**: Freeplane maps may contain dozens of legacy or custom icon identifiers. The application must gracefully resolve standard variants (e.g., `flag-red` vs `flag_red`) and have a clear, predictable behavior for unmapped icons.

**Independent Test**: Load a mindmap referencing both standard mapped icons (e.g., `button_ok`, `full-1`) and an unrecognized icon identifier (e.g., `unknown_custom_icon`). Verify that mapped icons render with their proper graphic and the unrecognized icon is rendered as a compact textual badge (`[unknown_custom_icon]`) without throwing errors or dropping the icon on save.

**Acceptance Scenarios**:

1. **Given** a Freeplane icon identifier using dashes or underscores (e.g. `flag-red` or `flag_red`, `button_cancel`), **When** looking up the visual representation, **Then** the system normalizes the name and resolves the appropriate visual graphic.
2. **Given** an icon identifier in a mindmap has no direct visual asset in the project, **When** the node is displayed (on canvas, in list view, or in the inspector), **Then** the system renders a compact textual badge displaying the unmapped icon's name (e.g., `[icon_name]`), clearly indicating its presence.
3. **Given** a document with unmapped icon identifiers is edited and saved, **When** saving to file, **Then** the original icon identifiers are preserved verbatim in the document format.

---

### Edge Cases

- **Empty Icon List**: A node has no icons assigned; the UI shows no icon placeholders or empty gaps, and the inspector displays a clear empty state prompting the user to add an icon.
- **Large Number of Icons**: A node has many assigned icons (e.g. 10+); on canvas they wrap or align neatly, and in the list view at most 3 icons plus an overflow badge (`+N`) are displayed to prevent obscuring the node title.
- **Duplicate Icons on Same Node**: Freeplane permits multiple instances of the same icon (e.g. two stars or two checkmarks); the system must support duplicate icon identifiers without key collisions in UI lists.
- **Legacy and Variant Naming**: Mindmaps created in different versions of FreeMind/Freeplane might use `priority-1` vs `full-1`, or `button_ok` vs `button-ok`, or `flag-green` vs `flag_green`. Normalization must resolve equivalent variants.
- **Missing or Corrupted Icon Attribute**: A node has an `<icon>` tag without a `BUILTIN` attribute or with an empty value; the system safely ignores the invalid entry during display without crashing.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST render all assigned Freeplane node icons alongside the node title in the interactive mindmap canvas view.
- **FR-002**: System MUST render assigned Freeplane node icons in the hierarchical node list view with a maximum of 3 icons displayed inline, followed by an overflow indicator (e.g. `+N`) when more than 3 icons are assigned.
- **FR-003**: System MUST display assigned icons in the exact order in which they appear in the node's data model.
- **FR-004**: System MUST provide an icon management interface allowing users to view all currently assigned icons with visual graphic previews.
- **FR-005**: System MUST allow users to remove individual icons, reorder assigned icons, or clear all icons from a node.
- **FR-006**: System MUST allow users to add icons to a node by selecting them from an icon catalog featuring both a search filter by name and categorized groupings (e.g. Status, Priorities, Flags, Emoticons, Objects).
- **FR-007**: System MUST map standard Freeplane icon names to the existing project icon assets (including existing drawable resources and vector icon libraries), normalizing punctuation differences (e.g., matching both `flag-red` and `flag_red`, `button_ok` and `button-ok`).
- **FR-008**: System MUST handle unmapped or missing icon names by displaying a compact textual badge (e.g., `[iconName]`) on the node and in the inspector, while preserving the exact Freeplane icon identifier verbatim in the document.
- **FR-009**: System MUST preserve all icon names verbatim during document loading, editing, and saving, ensuring full two-way compatibility with desktop Freeplane.
- **FR-010**: System MUST visually differentiate between user-assigned Freeplane icons and structural node badges (such as hyperlink indicators, rich-text markers, or note icons).

### Key Entities

- **Node Icon Identifier**: A string identifier representing a built-in Freeplane icon name (e.g., `yes`, `button_ok`, `flag-red`, `priority-1`).
- **Icon Descriptor / Mapping**: A presentation entity linking a Freeplane icon identifier (and its alias variations) to its visual representation (drawable resource or vector graphic), human-readable label, and category.
- **Icon Catalog**: The complete collection of available icons browsable by the user, organized by category with real-time search filtering.
- **Assigned Icons List**: An ordered collection of icon identifiers attached to a specific node.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the standard Freeplane icons currently available as project assets (80+ drawable resources and configured vector icons) are viewable and selectable in the icon catalog.
- **SC-002**: When a user selects an icon from the catalog, it is attached to the node and visually appears on the node within 200 milliseconds.
- **SC-003**: Mindmap files containing icons round-trip through loading, editing, and saving with 0% data loss or identifier alteration.
- **SC-004**: Users can add, reorder, or remove an icon from a node in 3 or fewer taps once the node inspector is open.
- **SC-005**: Canvas scrolling and list scrolling remain fluid (maintaining 60 fps) when rendering nodes with multiple icons.

## Assumptions

- The project's existing 80+ icon drawables in `res/drawable/` and vector icons in `compose.icons` provide sufficient coverage for the most common Freeplane mindmap icons.
- Node icons are ordered linearly and displayed horizontally before or alongside the node text.
- Changes made to node icons in the inspector are persisted as part of the normal document save workflow.
- No remote network requests are required to fetch icons; all supported icons are bundled locally in the application.

---

## Resolved Clarifications

- **Q1 (Unmapped / Missing Icons)**: Handled via Option B — Render a compact textual badge displaying the unmapped icon's name (e.g. `[name]`), keeping the identifier 100% preserved in the XML.
- **Q2 (NodeList Display Constraints)**: Handled via Option B — Display up to 3 icons inline, followed by an overflow indicator (e.g. `+N`) to preserve row space and title readability.
- **Q3 (Icon Catalog Organization)**: Handled via Option C — Hybrid interface with a real-time text search bar and categorized filter sections/chips.
