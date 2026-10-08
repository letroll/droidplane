# Research & Technical Decisions: Freeplane Document Types Support and Editing

**Feature Branch**: `006-support-freeplane-types`  
**Feature Directory**: `specs/006-support-freeplane-types`  
**Status**: Completed  

## Overview

This document establishes the technical decisions, architecture, and trade-off evaluations for supporting all elements and data types defined by the Freeplane document format (`.mm` XML schema) and providing full editing capabilities within Droidplane.

---

## Decision 1: Domain Model Architecture for Comprehensive Freeplane Node Elements

### Context
Droidplane's current `Node` model in `:data` (`fr.julien.quievreux.droidplane2.data.model.Node`) only contains partial attributes:
- A single `richContentType: RichContentType?` and a list of strings `richTextContents`. This is insufficient because a single Freeplane node can simultaneously contain rich node text (`TYPE="NODE"`), secondary expandable details (`TYPE="DETAILS"`), and extended notes (`TYPE="NOTE"`).
- Missing Freeplane attributes: `COLOR`, `BACKGROUND_COLOR`, `STYLE` (shape: fork, bubble, oval, etc.), `FOLDED`, `HGAP`, `VGAP`, `VSHIFT`.
- Missing child structures: key-value attributes (`<attribute>`), clouds (`<cloud>`), custom edges (`<edge>`), external object hooks (`<hook NAME="ExternalObject">`), LaTeX formula hooks (`<hook NAME="plugins/latex/LatexNodeHook.properties">`), and generic/unknown hooks.

### Decision
Enhance and structure the domain models in `:data`:
1. **Separate Rich Content Annotations**:
   Provide explicit, dedicated properties for each rich content type on `Node`:
   - `richText: String?` (formatted node title, `TYPE="NODE"`)
   - `detailsText: String?` (expandable details, `TYPE="DETAILS"`)
   - `noteText: String?` (extended note annotation, `TYPE="NOTE"`)
   - Preserve raw `richTextContents: MutableList<String>` for backward compatibility where needed.
2. **Key-Value Attributes**:
   Introduce `NodeAttributeEntry(val name: String, val value: String, val type: String? = null)`. A node maintains `val attributes: List<NodeAttributeEntry> = emptyList()`.
3. **Visual Properties**:
   Add typed styling properties to `Node`:
   - `color: String?` (foreground hex color `#rrggbb`)
   - `backgroundColor: String?` (background hex color `#rrggbb`)
   - `style: String?` (node shape, e.g. "fork", "bubble", "as_parent")
   - `fontName: String?`, `fontSize: Int?`, `isBold: Boolean`, `isItalic: Boolean`
   - `isFolded: Boolean`
4. **Cloud Grouping**:
   Introduce `CloudProperties(val color: String? = null, val width: Int? = null, val shape: String? = null)` (`val cloud: CloudProperties? = null`).
5. **Parent Edge**:
   Introduce `EdgeProperties(val color: String? = null, val style: String? = null, val width: String? = null)` (`val edge: EdgeProperties? = null`).
6. **Connectors / ArrowLinks**:
   Enhance connector data: `ConnectorLink(val destinationId: String, val color: String? = null, val startArrow: String? = null, val endArrow: String? = null, val startInclination: String? = null, val endInclination: String? = null, val sourceLabel: String? = null, val middleLabel: String? = null, val targetLabel: String? = null, val edgeLike: Boolean = false)`.
7. **Hooks & Extensions**:
   - `externalObject: ExternalObjectProperties?` (`val uri: String, val size: Float?`)
   - `latexEquation: String?`
   - `unrecognizedHooks: List<GenericHookElement>` (captures name, attributes, and inner text/sub-nodes for 100% round-trip preservation of desktop Freeplane extensions like `MapStyle`, scripts, etc.).
8. **Layout Offsets**:
   `hgap: Int?`, `vgap: Int?`, `vshift: Int?`.

### Rationale
- Strictly models the Freeplane specification (from `https://docs.freeplane.org/attic/old-mediawiki-content/Document_Format.html` and `Current_Freeplane_File_Format.html`).
- Eliminates collision between node text, details, and notes.
- Guarantees non-destructive round-trip editing (Constitution Principle IV).

### Alternatives Considered
- *Store all new elements as untyped XML strings / generic bags*: Rejected because it prevents typed manipulation, validation, and clean Jetpack Compose UI binding.
- *Separate sub-models into distinct relational entities in SQLite/Room*: Rejected per Constitution Principle V (Simplicity & YAGNI); mind maps are loaded in-memory from `.mm` XML and persisted back to `.mm` files.

---

## Decision 2: Parsing & Serialization Engine Optimization

### Context
XML parsing is performed via `XmlPullParser` in `XmlParseUtilsDefaultImpl.kt`, and serialization via `XmlSerializer` in `NodeManager.kt`.
Currently, several tags (`<attribute>`, `<cloud>`, `<edge>`, `<hook>`) are skipped or discarded during parsing, and `serializeNode` only outputs a subset of attributes.

### Decision
1. **Parser Extension (`XmlParseUtilsDefaultImpl`)**:
   - When encountering `NodeTag.NODE`: parse all node attributes (`ID`, `TEXT`, `COLOR`, `BACKGROUND_COLOR`, `STYLE`, `FOLDED`, `HGAP`, `VGAP`, `VSHIFT`, `LINK`, `POSITION`, `CREATED`, `MODIFIED`).
   - When encountering `NodeTag.RICH_CONTENT`: inspect `TYPE` attribute:
     - `TYPE == "NODE"` -> set `richText`
     - `TYPE == "DETAILS"` -> set `detailsText`
     - `TYPE == "NOTE"` -> set `noteText`
   - When encountering `NodeTag.ATTRIBUTE`: extract `NAME`, `VALUE`, `TYPE` and append to parent's `attributes`.
   - When encountering `NodeTag.CLOUD`: extract `COLOR`, `WIDTH`, `SHAPE` and assign to parent's `cloud`.
   - When encountering `NodeTag.EDGE`: extract `COLOR`, `STYLE`, `WIDTH` and assign to parent's `edge`.
   - When encountering `NodeTag.HOOK`:
     - If `NAME == "ExternalObject"`, parse `URI` and `SIZE` into `externalObject`.
     - If `NAME == "plugins/latex/LatexNodeHook.properties"`, parse `EQUATION` into `latexEquation`.
     - Otherwise, parse into `GenericHookElement` to retain name and attributes for round-trip fidelity.
2. **Canonical Serialization Order (`NodeManager`)**:
   Follow Freeplane desktop XML tag sequencing inside `<node>`:
   1. Node attributes (`ID`, `CREATED`, `MODIFIED`, `TEXT`, `POSITION`, `STYLE`, `COLOR`, `BACKGROUND_COLOR`, `FOLDED`, `LINK`, `HGAP`, `VGAP`, `VSHIFT`).
   2. `<edge .../>` (if configured).
   3. `<font .../>` (if font properties or bold/italic present).
   4. `<cloud .../>` (if cloud configured).
   5. `<icon .../>` (for each icon).
   6. `<richcontent TYPE="NODE">...` (if rich node text present).
   7. `<richcontent TYPE="DETAILS">...` (if details present).
   8. `<richcontent TYPE="NOTE">...` (if note present).
   9. `<attribute .../>` (for each attribute entry).
   10. `<arrowlink .../>` (for each connector).
   11. `<hook .../>` (external objects, LaTeX, and preserved generic hooks).
   12. Child `<node>` elements recursively.
3. **Atomic File Write (Safe Save)**:
   Write to a temporary file (`.mm.tmp`) and atomically replace/rename to the destination file to prevent partial file corruption on interruption (Constitution Principle IV).

### Rationale
- Matches desktop Freeplane XML structure precisely.
- Atomic staging guarantees map integrity even if an unexpected crash or power loss occurs during save.

---

## Decision 3: Hybrid Rich Content & Formula Editing Interface

### Context
Users specified a preference for a **Hybrid** rich text editing mode:
- Visual preview with quick formatting controls (bold, italic, colors, bullet points).
- Toggleable raw HTML / LaTeX markup source editor for full fidelity and fine-grained tag control.

### Decision
Create a reusable composable component `HybridRichContentEditor`:
1. **Mode Toggle**:
   - `PREVIEW_FORMAT`: Displays formatted text preview using Android `Html.fromHtml` / Compose `AnnotatedString` with an action toolbar for common styling actions (`<b>`, `<i>`, `<font color="...">`, `<ul>/<li>`).
   - `RAW_SOURCE`: Full multi-line `OutlinedTextField` displaying the raw HTML markup (or LaTeX equation string) with syntax convenience helpers (insert tag, close tag, clear).
2. **LaTeX Equation Integration**:
   - For LaTeX formulas, the raw view provides direct equation input (e.g., `\frac{a}{b}`).
   - The preview provides rendered formula display (fallback text preview or math notation).
3. **HTML Sanitization**:
   - Ensure closing tags (`<html><body>...</body></html>`) are automatically maintained when switching from raw source mode to preview or saving.

### Rationale
- Offers effortless formatting for casual note-takers on touch devices.
- Guarantees advanced users can inspect and tweak complex HTML or mathematical formulas without being constrained by a mobile rich text abstraction.

---

## Decision 4: Adaptive Node Properties Inspector (Bottom Sheet vs. Expanded Panel)

### Context
Users specified: "un mix de la solution 1 et 2 en fonction de la taille d'écran disponible" (a mix of bottom sheet and expanded full-screen/side panel based on available screen space).

### Decision
Implement an adaptive inspector component `NodePropertiesInspector`:
1. **Responsive Container**:
   - **Compact Screens (Phones in portrait, width < 600dp)**:
     Render as a `ModalBottomSheet` anchored to the bottom of the screen.
   - **Medium / Expanded Screens (Tablets, foldables, phones in landscape, width >= 600dp)**:
     Render as a sliding side sheet or two-pane dialog positioned next to the canvas/list, maintaining full visibility of the mind map context.
2. **Categorized Tabs**:
   Organize properties into 5 clear tabs:
   - **Tab 1: Content**: Core Title (plain or rich text toggle), Details, Notes, LaTeX formula, with `HybridRichContentEditor`.
   - **Tab 2: Attributes & Links**: Table of key-value attributes (Add, Edit, Delete rows) and Hyperlink field (URL or internal `#ID_` reference).
   - **Tab 3: Styling & Cloud**: Text color picker, Background color picker, Node shape selector (Fork, Bubble, Oval, Rectangle), Font size & styling (Bold, Italic), Cloud grouping toggle (Cloud color, Cloud shape).
   - **Tab 4: Edges & Connectors**: Branch edge style (linear, bezier, width, color) and ArrowLink connectors (destination picker, labels, color, arrowheads).
   - **Tab 5: Icons & Media**: Freeplane icon catalog picker and External object (image URI and scale).
3. **State Management**:
   - Inspector state is managed in `MainUiState.DialogType.NodePropertiesInspector(val node: Node)`.
   - Confirming saves emits an update event to `MainViewModel`, which dispatches to `NodeManager.updateNodeProperties(updatedNode)`.
   - Dismissing cancels edits without side effects.

### Rationale
- Adapts naturally across phone, tablet, and foldable form factors.
- Organizes dozens of Freeplane properties cleanly into discoverable, touch-friendly categories without cluttering the screen.

---

## Decision 5: Read/Edit Raw Script Hooks Without Mobile Execution

### Context
Users selected: "Editable Text" for embedded scripts and plugin hooks.
Freeplane supports Groovy/Jython scripts and plugin properties attached to nodes and maps.

### Decision
1. **Preservation & Editing**:
   - Present embedded script hooks and plugin parameters in an "Advanced / Scripts" section within the Inspector as editable multi-line text fields.
   - Users can view and modify script text (e.g. script body or parameter strings).
   - On save, modified script text is serialized back into the exact `<hook NAME="...">` XML structure.
2. **No On-Device Execution**:
   - The application does NOT evaluate or execute Groovy, Jython, or arbitrary scripts on Android.
   - Scripts are treated purely as document payload.

### Rationale
- Guarantees 100% round-trip fidelity between desktop Freeplane and Droidplane.
- Eliminates severe security risks (arbitrary code execution from untrusted `.mm` files) and avoids bundling a heavy JVM Groovy interpreter on mobile.
