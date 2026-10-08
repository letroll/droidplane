# Feature Specification: Freeplane Document Types Support and Editing

**Feature Branch**: `006-support-freeplane-types`

**Created**: 2026-10-06

**Status**: Draft

**Input**: User description: "je désire à présent ajouter le support de tous les types supporté par le format de document freeplane, comme décrit à l'url suivante et que l'application permet leur édition : https://docs.freeplane.org/attic/old-mediawiki-content/Document_Format.html"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Core Text, Details, Notes, and Rich Content Editing (Priority: P1)

Users need to capture and edit all textual and formatted content supported by Freeplane. This includes the main node text, expandable secondary details, extended rich text note annotations, and formatted rich text (HTML styling). Users can open any node, view existing text, details, and notes, modify their content or formatting, and save changes back to the document.

**Why this priority**: Text, details, and notes form the core informational payload of any mind map. Without full support and editing for details, notes, and rich formatting, users lose critical thoughts and notes created in desktop Freeplane.

**Independent Test**: Load a mind map containing plain text nodes, rich text nodes, details, and notes. Edit each text element on a node, add new details and notes to a node that lacked them, save the map, and reopen it to verify that all content and rich markup persist without truncation.

**Acceptance Scenarios**:

1. **Given** a node with plain text, **When** a user edits the text, **Then** the updated text is displayed immediately and saved to the document.
2. **Given** a node with or without details, **When** a user adds or edits the details content, **Then** the details are associated with the node, visible in the interface, and stored as a details rich content block.
3. **Given** a node with or without a note annotation, **When** a user creates or edits the note text, **Then** the note is saved as a note rich content block and visually indicated on the node.
4. **Given** a node formatted with rich text (HTML/CSS), **When** the user edits the content, **Then** the rich formatting structure is preserved upon saving and can be reloaded in desktop Freeplane.

---

### User Story 2 - Key-Value Attributes and Metadata Management (Priority: P1)

Users need to view, add, edit, and delete structured key-value attributes assigned to nodes. In addition, users must be able to assign or remove icons from the standard Freeplane icon catalog and configure hyperlinks (both external web URLs and internal cross-references to other nodes in the map).

**Why this priority**: Attributes and links are primary structured data elements in Freeplane maps, frequently used for task tracking, data categorization, and cross-linking between concepts.

**Independent Test**: Select a node, add two custom attributes (e.g., "Priority" = "High", "Status" = "In Progress"), attach an icon, set a web link, save the file, and reload to confirm all attributes, icons, and links persist accurately.

**Acceptance Scenarios**:

1. **Given** an existing node, **When** a user adds an attribute with a name and a value, **Then** the attribute is listed in the node's attribute table.
2. **Given** an existing attribute on a node, **When** the user modifies its name or value or deletes it, **Then** the changes are reflected in the table and persisted upon saving.
3. **Given** a node, **When** a user selects one or more icons from the icon selector, **Then** the icons are displayed alongside the node title and saved in the document.
4. **Given** a node, **When** a user adds or updates an external link (URL) or an internal node link, **Then** the link is recorded and can be activated from the node.

---

### User Story 3 - Visual Styling, Shapes, Colors, and Cloud Grouping (Priority: P2)

Users need to customize the visual presentation of nodes to reflect visual hierarchy and emphasis. This includes configuring font properties (bold, italic, font size, font family), node shapes and styles (such as bubble, fork, rectangle), node text color, background fill color, and enclosing branches in visual clouds (with custom cloud color and shape).

**Why this priority**: Mind maps rely heavily on visual cues, color coding, and spatial groupings to convey meaning and hierarchy.

**Independent Test**: Select a node, set its shape to bubble, change text color to blue and background to light yellow, apply a cloud around its subtree with an orange tint, save the file, and verify the visual properties persist upon reloading.

**Acceptance Scenarios**:

1. **Given** a node, **When** a user toggles bold, italic, or adjusts font size, **Then** the node's text formatting updates in the view and persists in the file.
2. **Given** a node, **When** a user selects a node shape (e.g., bubble or fork) and colors (text color, background color), **Then** the node renders with the chosen style and colors.
3. **Given** a node with descendants, **When** a user enables a cloud, selects a cloud color and shape, **Then** the cloud encloses the node and its subtree visually and is saved in the document.
4. **Given** a node with an existing cloud, **When** the user removes or edits the cloud properties, **Then** the cloud is updated or removed accordingly.

---

### User Story 4 - Relationship Connectors and Edge Styling (Priority: P2)

Users need to customize connection edges linking child nodes to parents (line style, line thickness, and color) and create or edit directional connector arrows (arrow links) between arbitrary nodes across the map, including setting colors, arrow directions, and text labels (source, middle, target).

**Why this priority**: Non-hierarchical connections and custom branch edges are fundamental Freeplane capabilities for representing complex networks, dependencies, and associations between disparate topics.

**Independent Test**: Connect two non-adjacent nodes with an arrow link, specify a middle label and red color, change a branch edge to linear with a distinct color, save, and reload to verify all connector and edge attributes are intact.

**Acceptance Scenarios**:

1. **Given** a child node, **When** a user modifies its branch edge style (e.g., linear, bezier, sharp-linear), width, or color, **Then** the connection line to its parent updates visually and persists in the document.
2. **Given** two arbitrary nodes in the map, **When** a user creates a connector link between them, **Then** a directional connector is established with optional arrowheads and labels.
3. **Given** an existing connector link, **When** a user edits its labels, color, or removes it, **Then** the connector updates or disappears and changes are saved.

---

### User Story 5 - External Objects and Mathematical Formulas (Priority: P3)

Users need to view and configure embedded or referenced external media (such as images with URI and scaling parameters) and mathematical formulas (LaTeX syntax).

**Why this priority**: Media attachments and mathematical notation enrich academic, technical, and research mind maps, but are secondary to core text, attributes, and visual styles.

**Independent Test**: Add an external image reference with URI and scale factor to a node, add a LaTeX formula to another node, save the document, and reload to confirm the external object and formula hooks are retained and formatted properly.

**Acceptance Scenarios**:

1. **Given** a node, **When** a user attaches an external image reference specifying a URI and scale, **Then** the reference is stored as an external object hook on the node.
2. **Given** a node, **When** a user inputs a LaTeX formula, **Then** the mathematical equation is stored as a formula hook and formatted according to Freeplane standards.

---

### User Story 6 - High-Fidelity Preservation of Advanced Map Structures (Priority: P3)

When editing mind maps on mobile, any advanced or specialized Freeplane document elements not directly modified by the user (such as map style definitions, layout gap coordinates, automatic edge color hooks, or custom script hooks) must be strictly preserved without corruption or data loss upon saving.

**Why this priority**: Cross-platform interoperability with desktop Freeplane is essential. Opening, editing, and saving a document on mobile must never degrade or strip desktop-only configurations.

**Independent Test**: Load a comprehensive desktop Freeplane map with embedded map styles, script hooks, and custom layouts. Edit several node titles and attributes, save the file, and perform a structural comparison to verify that all untouched elements and hooks are preserved byte-for-byte.

**Acceptance Scenarios**:

1. **Given** a map containing desktop map style configurations and layout attributes, **When** a user modifies unrelated node content and saves the file, **Then** all map styles and layout attributes are preserved without modification.
2. **Given** a map containing unsupported or custom third-party hooks, **When** the file is saved after edits, **Then** the unknown hooks remain intact and are not stripped.

---

### Edge Cases

- **Mixed Text and Rich Content**: When a node possesses both a plain text attribute and rich HTML content, editing must clearly define which content is being modified without corrupting or accidentally overwriting the alternate format.
- **Malformed or Incomplete HTML**: When users or imported files provide malformed HTML markup in rich text, details, or notes, the system must sanitize and repair the XML structure to prevent parsing failures or application crashes during subsequent saves.
- **Dangling Connector References**: If a node targeted by an arrow link is deleted or unavailable, the connector must handle the missing destination gracefully without crashing the view or corrupting the connector definitions of other nodes.
- **Empty or Whitespace-Only Fields**: When details, notes, or attribute values are cleared by the user, the system must cleanly remove the corresponding XML element or attribute rather than writing empty invalid tags.
- **Deeply Nested Subtrees with Clouds**: When multiple nested clouds overlap across parent and child nodes, the system must parse and serialize each cloud boundary independently according to Freeplane hierarchy rules.
- **Character Encoding & Special Characters**: Rich content, LaTeX formulas, URLs, and attribute values containing XML special characters (`<`, `>`, `&`, `"`, `'`, non-breaking spaces) must be escaped safely according to Freeplane standards.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST support parsing and saving all standard Freeplane node elements, including plain text, rich content (HTML), details, notes, attributes, icons, font styles, colors, shapes, clouds, edges, connectors (arrow links), external objects, and formula hooks.
- **FR-002**: System MUST allow users to view, add, edit, and remove secondary node **Details** (`<richcontent TYPE="DETAILS">`).
- **FR-003**: System MUST allow users to view, add, edit, and remove extended node **Notes** (`<richcontent TYPE="NOTE">`).
- **FR-004**: System MUST allow users to view, edit, and create rich text formatted node content (`<richcontent TYPE="NODE">`) via a hybrid editing mode combining a visual preview with quick formatting controls (bold, italic, colors, lists) and a toggleable raw HTML / LaTeX markup source editor.
- **FR-005**: System MUST provide an interface for managing node **Attributes**, enabling users to view the list of key-value attributes, add new attributes, modify existing attribute names and values, and delete attributes.
- **FR-006**: System MUST allow users to assign one or multiple **Icons** to a node from the standard Freeplane icon catalog and remove existing icons.
- **FR-007**: System MUST allow users to view and edit **Hyperlinks**, supporting both external web URLs and internal document fragment links (`#ID_...`) pointing to other nodes in the map.
- **FR-008**: System MUST allow users to edit node visual presentation, including foreground text color, background fill color, font style (bold, italic, font size), and node bounding shape (fork, bubble, rectangle, oval).
- **FR-009**: System MUST allow users to add, configure, and remove a **Cloud** grouping on any node, including specifying cloud background color and cloud border shape.
- **FR-010**: System MUST allow users to configure **Edge** styling connecting a node to its parent, including edge color, line style (linear, bezier, sharp-linear, horizontal, hide), and stroke width.
- **FR-011**: System MUST allow users to create, edit, and delete **Connectors** (arrow links) between any two nodes in the map, including destination node selection, arrow direction, color, and labels (source, middle, target).
- **FR-012**: System MUST allow users to attach and configure **External Objects** (image references with URI and scale factor) and **LaTeX Formulas** (equation syntax).
- **FR-013**: System MUST provide access to all node editing features through an adaptive node inspector interface that automatically adjusts to available screen size: displaying a consolidated multi-tab bottom sheet on compact/mobile screens and expanding into a dedicated multi-section panel or full-screen inspector on larger screens/tablets.
- **FR-014**: System MUST handle embedded executable scripts and plugin hooks by allowing users to view and edit embedded script hook text and plugin hook parameters directly as text without executing the scripts on the mobile device, while preserving all script content with 100% round-trip fidelity.
- **FR-015**: System MUST preserve all unmodified Freeplane elements, map styles (`MapStyle`), layout coordinates (`HGAP`, `VGAP`, `VSHIFT`), and unedited attributes with 100% round-trip fidelity during file saves.

### Key Entities *(include if feature involves data)*

- **MindMapNode**: Core hierarchical tree element representing a topic, holding text, identifier, timestamps, position, parent link, and collections of child elements.
- **RichContentBlock**: Formatted XHTML content associated with a node, specialized into Node core content (`TYPE="NODE"`), Details (`TYPE="DETAILS"`), or Note (`TYPE="NOTE"`).
- **NodeAttribute**: Key-value metadata pair attached to a node, containing a name, a value, and an optional data type.
- **NodeVisualProperties**: Encapsulates visual styling attributes including shape style, foreground color, background fill color, font name, font size, bold, and italic flags.
- **CloudDefinition**: Enclosing visual hull around a node and its descendants, defined by background color, line width, and outline shape.
- **EdgeDefinition**: Connection branch between parent and child node, defined by color, line style, and width.
- **ConnectorLink**: Explicit directional cross-link between two nodes, defined by source ID, destination ID, start/end arrows, inclination coordinates, color, and text labels.
- **ExternalObject**: Reference to an embedded or linked media file, defined by target URI and display scaling factor.
- **FormulaHook**: Mathematical equation definition adhering to Freeplane LaTeX notation.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of standard Freeplane document elements and types defined in the Freeplane format specification are parsable and editable without data loss.
- **SC-002**: Round-trip fidelity: saving an edited mind map preserves all unedited Freeplane attributes, hooks, and structures without schema degradation or unintended reformatting.
- **SC-003**: Users can inspect, edit, and save any supported node property (details, notes, attributes, styles, clouds, connectors) in under 4 taps from the active mind map view.
- **SC-004**: Loading, rendering, and editing mind maps with rich content, attributes, clouds, and connectors maintains responsive UI interactions (under 100ms response time when opening the property editing interface).
- **SC-005**: Zero data loss or application crashes when processing legacy, complex, or richly formatted Freeplane maps.

## Assumptions

- Freeplane mind maps use XML serialization (`.mm` extension) compliant with Freeplane 1.1.x+ standards.
- Mobile rendering provides clear visual indicators when nodes contain rich content, details, notes, attributes, clouds, or connectors.
- Color values in Freeplane are represented in standard hexadecimal format (`#RRGGBB`).
- All user modifications are performed locally on the active document state and persisted atomically upon saving.
- Editing actions support standard discard/cancel behavior without altering the active document if the user dismisses the editor without confirming.
