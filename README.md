# Droidplane

An Android application to view and edit [Freeplane](https://docs.freeplane.org/) Mind Maps (`.mm`).

<!-- TOC -->
* [Droidplane](#droidplane)
    * [Features](#features)
    * [Freeplane Document Types Supported](#freeplane-document-types-supported)
    * [Author](#author)
    * [Contributor](#contributor)
<!-- TOC -->

### Features

* [X] Mind Map Canvas View (Pan, Zoom, 2D Tree Layout)
* [X] List View with Breadcrumb Navigation
* [X] Full Text Search with Match Navigation
* [X] Node Creation (Add Child, Add Sibling)
* [X] Node Deletion with Undo
* [X] Comprehensive Node Properties Inspector (Bottom sheet on phone, side panel on tablet)
* [X] Safe Atomic XML Save (`.mm.tmp` -> `.mm`) with 100% Round-Trip Preservation

### Freeplane Document Types Supported

* **Core & Rich Text**: Plain text titles and formatted rich XHTML (`<richcontent TYPE="NODE">`) with visual formatting preview and raw source editing.
* **Details & Notes**: Expandable secondary node details (`TYPE="DETAILS"`) and extended note annotations (`TYPE="NOTE"`).
* **Metadata & Attributes**: Key-value metadata tables (`<attribute NAME="..." VALUE="...">`) and hyperlinks (`LINK` - URLs and internal `#ID_` targets).
* **Visual Presentation**: Node shapes (`STYLE`: `bubble`, `oval`, `rectangle`, `fork`), text colors (`COLOR`), surface fill colors (`BACKGROUND_COLOR`), font sizing and styles (`BOLD`, `ITALIC`).
* **Cloud Grouping**: Subtree enclosing clouds (`<cloud>`) with custom tint colors and outline shapes (`ROUND_RECT`, `ARC`, etc.).
* **Branch Edge Styling**: Custom edge lines connecting nodes to parents (`<edge>`), supporting `linear` straight lines, `bezier` curves, hidden edges (`hide_edge`), line widths, and stroke colors.
* **Connectors**: Cross-topic arrow links (`<arrowlink>`) with custom colors, arrowheads, and middle text labels.
* **Formulas & External Objects**: LaTeX mathematical notation (`LatexNodeHook`) and external image attachments (`ExternalObject`).
* **Desktop Hooks & Script Preservation**: Preserves unedited desktop Freeplane hooks (`MapStyle`, scripts, automatic layout) byte-for-byte on round-trip saves, with editable script text.

### Author
**Benedikt Köppel**

### Contributor
**Julien Quiévreux**
