# Feature Specification: Foldable Media Sections (Icons & Images) and Interactive Node Images

**Feature Branch**: `017-node-media-sections-images`

**Created**: 2026-11-09

**Status**: Draft

**Input**: User description: "Dans le menu d'édition d'un node, à la section Media.  J'aimerais avoir dans la liste scrollable deux sections foldable. Icons et Images. avec pour le titre icon, les icons eventuellement assigné pour le node à la suite du titre s'il en possède. Ainsi que des sous-section foldable pour toutes les catégories autres que all. Enfin pour la partie image. Le champ uri doit aussi inclure en plus un file picker pour remplir l'uri aisement. Et la mindmap afficher l'image si celle-ci est renseigné et que ce n'est pas déjà le cas. Elle doit pouvoir aussi être resizable en terme de visualisation grâce à des ancres placées aux coins de l'image. Et l'image être supprimable ou remplaçable depuis un click long sur celle-ci."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Foldable Icons & Images Sections with Categorized Icon Subsections in Media Tab (Priority: P1)

When editing a node in the Node Properties Inspector under the **Media** tab, the user sees a scrollable view organized into two top-level foldable (collapsible/expandable) sections: **Icons** and **Images**. The **Icons** section header displays the "Icons" title followed inline by the visual preview of all icons currently assigned to the node (if any), allowing the user to see assigned icons even when the section is collapsed. When expanded, the **Icons** section provides the assigned icons management controls and organizes the icon catalog into foldable subsections for each icon category other than "All" (Priority, Status, Progress, Flags, Emotion, Office, Time, Signs, Misc).

**Why this priority**: Structuring the Media tab into foldable sections and category subsections eliminates excessive scrolling in the inspector while keeping assigned icons immediately visible in the header.

**Independent Test**: Can be independently tested by opening the Node Properties Inspector on a node with and without assigned icons, switching to the Media tab, toggling the "Icons" and "Images" foldable headers, verifying the inline assigned icons preview next to the "Icons" title, and expanding/collapsing individual icon category subsections to add or remove icons.

**Acceptance Scenarios**:

1. **Given** a node has assigned icons and the user opens the Media tab in the Node Properties Inspector, **When** the **Icons** section header is rendered (expanded or collapsed), **Then** the assigned icons are visually displayed inline immediately after the "Icons" section title.
2. **Given** a node has no assigned icons, **When** the **Icons** section header is rendered, **Then** only the "Icons" title and fold/unfold indicator are shown without trailing icons.
3. **Given** the user taps the **Icons** or **Images** section header, **When** the section is expanded or collapsed, **Then** its body smoothly collapses or expands within the scrollable Media tab.
4. **Given** the **Icons** section is expanded, **When** the user browses the icon catalog, **Then** every specific icon category (Priority, Status, Progress, Flags, Emotion, Office, Time, Signs, Misc — excluding the global "All" category) is presented as its own foldable subsection that can be expanded or collapsed independently to pick and assign icons to the node.

---

### User Story 2 - Image File Picker in Media Tab and Image Rendering on Mind Map (Priority: P1)

In the **Images** foldable section of the Media tab, the image URI field includes an integrated file picker action button so the user can select a local image file from their device without typing a path manually. When a node has an image URI configured (either selected via the file picker, entered manually, or loaded from a Freeplane `.mm` file), the mind map canvas renders the image inside the node.

**Why this priority**: Selecting an image via a file picker and seeing the image directly on the mind map canvas are the core capabilities required to use images in mind maps.

**Independent Test**: Can be independently tested by expanding the "Images" section in the Media tab, picking a local image file via the file picker button, confirming the URI field is populated, viewing the mind map canvas, and verifying the image is rendered on the node.

**Acceptance Scenarios**:

1. **Given** the **Images** section in the Media tab is expanded, **When** the user activates the file picker action next to the URI field and selects an image file (e.g., PNG, JPG, WEBP, GIF, BMP), **Then** the URI field is automatically populated with the selected file's URI and the node's attached image is updated (with default scale `1.0` if no scale was previously set).
2. **Given** a node has a valid image URI pointing to a readable image, **When** the mind map canvas renders that node, **Then** the image is visually displayed within the node alongside its title and icons, and the node's layout bounds expand to accommodate the image so sibling nodes and branch connectors do not overlap.
3. **Given** a node has a blank or empty image URI, **When** the mind map canvas renders that node, **Then** no image area or placeholder is shown on the node.
4. **Given** a node has an image URI that cannot be loaded (missing file or invalid path), **When** the mind map canvas renders that node, **Then** the node displays a non-crashing broken-image indicator that still supports long-press to replace or delete the image.

---

### User Story 3 - Interactive Image Resizing via Corner Anchors and Long-Press Context Actions (Priority: P2)

On the mind map canvas, when a node displays an image, the user can resize the image visually by dragging anchor handles positioned at the four corners of the image, which updates the node's image scale factor in real time and persists it. Additionally, performing a long-press directly on the displayed image opens a contextual menu allowing the user to **Replace** the image (via the file picker) or **Delete** the image from the node without opening the full Node Properties Inspector.

**Why this priority**: Direct manipulation on the mind map canvas (corner-handle resizing and long-press replace/delete) provides a fluid, intuitive visual editing experience consistent with desktop mind-mapping tools.

**Independent Test**: Can be independently tested on a node with an attached image on the mind map canvas by dragging any of the 4 corner anchors to enlarge or shrink the image, verifying the updated scale persists, and long-pressing the image to either replace it via the file picker or delete it.

**Acceptance Scenarios**:

1. **Given** a node with an attached image is displayed on the mind map canvas, **When** the image is rendered (or selected for editing), **Then** 4 resize anchor handles are visible at the four corners of the image (top-left, top-right, bottom-left, bottom-right).
2. **Given** the user drags any of the 4 corner anchor handles of a node's image on the mind map canvas, **When** the drag gesture moves outward or inward, **Then** the displayed image size scales proportionally in real time (clamped to a safe minimum and maximum scale range, `0.2x` to `4.0x`) and persists the updated scale value when released, without triggering canvas pan or node drag-and-drop re-parenting.
3. **Given** a node displays an image on the mind map canvas, **When** the user performs a long-press directly on the image, **Then** a contextual action menu appears with options to **Replace** the image or **Delete** the image (without initiating node drag-and-drop).
4. **Given** the image long-press menu is open, **When** the user selects **Delete**, **Then** the node's attached image is removed, the image disappears from the node immediately, and the mind map layout reflows.
5. **Given** the image long-press menu is open, **When** the user selects **Replace**, **Then** the image file picker opens and selecting a new file updates the node's image URI and refreshes the displayed image on the mind map.

---

### Edge Cases

- **Gesture disambiguation on node**: Dragging a corner resize anchor or long-pressing the image itself MUST consume pointer events so it does not accidentally trigger node drag-and-drop re-parenting or mind map canvas panning.
- **Scale bounds**: Resizing via corner anchors or typing in the scale field MUST clamp or validate scale values to a sensible positive range (`0.2` minimum to `4.0` maximum for interactive drag, and rejecting `<= 0` values) so the image never vanishes to 0 pixels or overflows canvas bounds.
- **Search interaction with foldable icon category subsections**: When the user enters a non-empty search query in the Icon Catalog search bar, categories containing matching icons automatically display their matching icons so the user does not have to manually expand every category to see search results.
- **Unreadable or moved local image file**: If a referenced image URI cannot be decoded or no longer exists on disk, the application MUST NOT crash; it displays a compact broken-image fallback inside the node that still responds to long-press (to replace or delete).
- **Freeplane file format round-trip**: Changes to the node's image URI and scale made via the file picker, corner resize handles, or long-press menu MUST persist in the `.mm` file format without data loss.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The Media tab in the Node Properties Inspector MUST organize its scrollable content into two top-level foldable (collapsible/expandable) sections: **Icons** and **Images**.
- **FR-002**: The **Icons** foldable section header MUST display the section title followed inline by the visual icons currently assigned to the node whenever the node has at least one assigned icon.
- **FR-003**: Inside the **Icons** section, the icon catalog MUST group icons into independent foldable subsections for every specific icon category other than the global "All" category (Priority, Status, Progress, Flags, Emotion, Office, Time, Signs, Misc), removing the redundant "All" filter tab.
- **FR-004**: When a search query is entered in the icon catalog search field, only categories with matching icons MUST be shown (expanded automatically while the query is active), and clearing the search query MUST restore normal foldable category navigation.
- **FR-005**: Inside the **Images** foldable section, the URI input row MUST provide both manual text editing and a File Picker action that opens a native file selector filtered to image files and populates the node's image URI with the chosen file.
- **FR-006**: When a node has a non-blank image URI, the mind map canvas MUST display the image on the node and compute the node's layout dimensions to include the scaled image height and width so adjacent nodes and branches do not overlap.
- **FR-007**: The displayed image on a mind map node MUST provide 4 interactive corner resize anchors (top-left, top-right, bottom-left, bottom-right) allowing the user to visually resize the image by dragging any corner, updating the node's image scale factor proportionally within safe bounds (`0.2x` to `4.0x`).
- **FR-008**: Long-pressing on the displayed image in a mind map node MUST open a contextual menu offering two actions: **Replace** (launching the image file picker to replace the image URI) and **Delete** (removing the image from the node).
- **FR-009**: Pointer gestures on the image's corner resize anchors and long-press on the image MUST be isolated from node drag-and-drop and canvas pan/zoom gestures so they do not conflict.
- **FR-010**: All image URI and scale updates made from the Media tab, corner resize anchors, or long-press menu MUST be saved in the mind map model and preserved across Freeplane `.mm` file save and load operations.

### Key Entities

- **Node Image Attachment (External Object)**: Represents an image attached to a mind map node, defined by its resource location (`URI`) and display scale multiplier (`size`, defaulting to `1.0` for 100% scale).
- **Icon Category**: Represents a thematic group of Freeplane icons (Priority, Status, Progress, Flags, Emotion, Office, Time, Signs, Misc) displayed as a foldable subsection inside the **Icons** section.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can view a node's assigned icons directly in the **Icons** section header and collapse/expand both top-level Media sections (**Icons**, **Images**) and all 9 icon category subsections in a single tap.
- **SC-002**: Users can attach a local image to a node in under 10 seconds using the integrated file picker in the **Images** section or replace/remove an existing image in 2 taps via long-press on the mind map canvas.
- **SC-003**: Dragging any of the 4 corner anchors on a node's image smoothly resizes the image and updates the node's layout and persisted scale attribute (`0.2x` to `4.0x`) with zero accidental node re-parenting or canvas jumps.
- **SC-004**: 100% of image URI and scale changes round-trip cleanly when saving and reopening a Freeplane `.mm` mind map file.

## Assumptions

- Both **Icons** and **Images** top-level sections in the Media tab default to expanded (or expand on demand) while individual icon category subsections default to collapsed so the Media tab remains compact on open.
- Base (`1.0x`) display dimensions for a node image on the mind map canvas use a readable default bounding box scaled proportionally by the node's image scale factor so unscaled images fit harmoniously inside mind map nodes.
- Selecting a local image via the file picker stores a standard file URI compatible with Freeplane's external object representation across mobile and desktop.
