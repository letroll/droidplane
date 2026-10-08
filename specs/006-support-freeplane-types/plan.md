# Implementation Plan: Freeplane Document Types Support and Editing

**Branch**: `006-support-freeplane-types` | **Date**: 2026-10-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/006-support-freeplane-types/spec.md`

## Summary

Add full parsing, editing, and serialization support for all elements and data types defined by the Freeplane document format (`.mm` XML schema). The technical approach expands the domain models in `:data` (`Node`, `NodeAttributeEntry`, `CloudProperties`, `EdgeProperties`, `ConnectorLink`, `ExternalObjectProperties`) with dedicated fields for rich text, details, notes, key-value attributes, visual styles, clouds, edges, connectors, and hooks. The XML parsing and serialization engine in `:data` (`XmlParseUtilsDefaultImpl`, `NodeManager`) is extended to parse and write all elements in Freeplane's canonical order with atomic file persistence.

Crucially, the newly parsed attributes and elements must directly drive the visual presentation of mind map elements according to the principles established by desktop Freeplane:
- **Node presentation**: Node foreground text color (`COLOR`), background fill color (`BACKGROUND_COLOR`), shape styles (`STYLE`: fork, bubble, oval, rectangle), and font properties (`BOLD`, `ITALIC`, `SIZE`, `NAME`) directly alter node card rendering.
- **Branch edges**: Parent-to-child edges adapt their line style (`linear`, `bezier`, `sharp-linear`, `hide_edge`), stroke width (`WIDTH`), and stroke color (`COLOR`).
- **Cloud grouping**: Cloud definitions (`<cloud>`) enclose the node and its subtree with custom background tint, outline width, and boundary shape (`ROUND_RECT`, `ARC`, etc.).
- **Content & Metadata badges**: Expandable details (`<richcontent TYPE="DETAILS">`), note indicators (`TYPE="NOTE"`), key-value attribute tables (`<attribute>`), inline icons (`<icon>`), link badges (`LINK`), and external images (`<hook NAME="ExternalObject">`) visually appear on or beneath the node.

In `:app`, an adaptive `NodePropertiesInspector` provides touch-friendly editing (compact multi-tab bottom sheet on mobile, expanded panel on tablets/landscape) with a hybrid visual preview and raw source editor for rich content and LaTeX formulas.

## Freeplane Visual Rendering Principles

The application must reflect the visual rendering semantics of desktop Freeplane across both Mind Map Canvas view and List view:

1. **Node Shape & Geometry**:
   - `bubble` / `oval`: Rendered as rounded pills or circular/elliptical containers.
   - `rectangle`: Rendered with sharp or subtly rounded rectangular corners (2dp).
   - `fork`: Rendered as an open bracket or underlined topic bar typical of Freeplane/FreeMind tree branches.
   - Default: Standard rounded card (8dp child, 16dp root).

2. **Colors & Typography**:
   - `node.color`: Overrides default theme text color with the specified `#RRGGBB` hex color.
   - `node.backgroundColor`: Overrides default surface fill with the specified `#RRGGBB` hex color.
   - `node.fontSize`: Custom font size in sp/pt (defaults to 13sp child, 15sp root).
   - `node.isBold` / `node.isItalic`: Typography style applied to title and plain text content.

3. **Branch Edge Styling (`<edge>`)**:
   - `style == "linear"` or `"sharp-linear"`: Direct straight line from parent anchor to child anchor.
   - `style == "bezier"` or `"sharp-bezier"`: Cubic bezier spline smoothing the transition.
   - `style == "hide_edge"`: Edge is completely omitted/hidden.
   - `color`: Stroke drawn using custom hex color (falling back to theme outline).
   - `width`: Stroke width scaled to density (thin, 1, 2, 4, 8dp).

4. **Cloud Enclosures (`<cloud>`)**:
   - Encloses the target node and all its visual descendants.
   - Renders with the specified tint `color` and boundary shape (`ROUND_RECT`, `ARC`, `STAR`, `RECT`).

5. **Annotations & Metadata Badges**:
   - **Details**: Rendered directly below the node title.
   - **Notes**: Visual indicator icon showing an attached note annotation.
   - **Attributes Table**: Key-value metadata table displayed within the expanded node inspector and card.
   - **Icons**: Predefined Freeplane built-in icon glyphs aligned horizontally with the node title.
   - **External Media & Formulas**: Embedded images scaled to scale factor and LaTeX equations rendered with math notation.

## Node Editor Requirements for Freeplane Attributes

The node editor (`NodePropertiesInspector`) must provide full editing capabilities for every newly supported Freeplane attribute and element, ensuring users can configure both content and visual styling directly from the mobile UI:

1. **Content Editing (Tab 1 - Content)**:
   - Primary node title with toggle between plain text and rich XHTML formatting (`<richcontent TYPE="NODE">`).
   - Secondary expandable details (`<richcontent TYPE="DETAILS">`).
   - Extended note annotations (`<richcontent TYPE="NOTE">`).
   - Mathematical equations via LaTeX notation (`<hook NAME="plugins/latex/LatexNodeHook.properties" EQUATION="...">`).
   - Hybrid editor offering instant visual preview formatting controls (Bold, Italic, Bullet lists, Color tags) and a raw source markup editor.

2. **Metadata & Attributes Editing (Tab 2 - Attributes & Links)**:
   - Dynamic key-value attributes table (`<attribute NAME="..." VALUE="...">`): add new entries, modify attribute names and values in-place, and remove entries.
   - Hyperlinks (`LINK`): input and edit external URLs or internal mindmap fragment targets (`#ID_...`).

3. **Visual Presentation Editing (Tab 3 - Styling & Cloud)**:
   - Node shapes: select between standard card, `bubble`, `oval`, `rectangle`, or `fork`.
   - Foreground text color (`COLOR`): select from color palette or custom hex code.
   - Background fill color (`BACKGROUND_COLOR`): select from surface fill palette or custom hex code.
   - Typography: toggle bold (`BOLD`), italic (`ITALIC`), and adjust font size in sp (`font SIZE="..."`).
   - Cloud Grouping (`<cloud>`): enable/disable cloud hull, select cloud fill tint (`COLOR`), and configure outline shape (`SHAPE`: `ROUND_RECT`, `ARC`, `STAR`, `RECT`).

4. **Edge & Connector Editing (Tab 4 - Connectors & Edges)**:
   - Branch edge to parent (`<edge>`): configure line style (`linear`, `bezier`, `sharp-linear`, `hide_edge`), stroke width (`WIDTH`), and custom edge stroke color (`COLOR`).
   - Connector Arrow Links (`<arrowlink>`): create and edit cross-links between arbitrary nodes, setting destination node ID, middle text label (`MIDDLE_LABEL`), arrowheads, and connector line color.

5. **Icons & Media Editing (Tab 5 - Icons & Media)**:
   - Freeplane icon catalog: browse and attach built-in icons (`<icon BUILTIN="...">`) and remove active icons.
   - External Object media (`<hook NAME="ExternalObject">`): set/update media image URI (`URI`) and display scaling factor (`SIZE`).

6. **Scripts & Hooks Editing (Tab 6 - Scripts)**:
   - Inspect and edit raw code for embedded Freeplane script hooks and plugin parameters without executing arbitrary code on mobile.

## Technical Context

**Language/Version**: Kotlin 2.0+ (JVM target 21, Java 21 toolchain)

**Primary Dependencies**: Jetpack Compose, Material 3, AndroidX Lifecycle / ViewModel, Kotlin Coroutines & StateFlow, Koin

**Storage**: Freeplane XML (`.mm` format) loaded into in-memory domain structures via `NodeManager` and persisted atomically to disk

**Testing**: JUnit, Kotest (`io.kotest`), MockK, kotlinx-coroutines-test, Turbine

**Target Platform**: Android (Min SDK 26, Target SDK 35)

**Project Type**: Multi-module Android application (`:core`, `:data`, `:app`)

**Performance Goals**: Responsive inspector opening (<100ms budget), 60/120 fps smooth scrolling in Compose `LazyColumn` and 2D canvas with rich annotations

**Constraints**: Immutability of domain state in UI; atomic updates in `NodeManager`; strict 100% round-trip preservation of desktop Freeplane attributes and hooks; no unverified script execution on Android

**Scale/Scope**: Mindmaps with thousands of nodes, rich formatting, multiple attributes, and complex branch styling

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **Principle I (Modular Separation)**: PASS — UI logic, inspector dialogs, and formatting toolbars remain strictly in `:app`. Domain models, XML parsing (`XmlPullParser`), and serialization (`XmlSerializer`) remain encapsulated in `:data`. No UI dependencies leak into `:data`.
- **Principle II (Unidirectional Data Flow)**: PASS — All node updates flow through `MainViewModel.onSaveNodeProperties` to `NodeManager.updateNode(updatedNode)` and propagate via `StateFlow<List<Node>>` to `MainUiState`. Direct state mutation is strictly forbidden.
- **Principle III (Test-Driven Verification)**: PASS — Extensive unit tests in `:data` verify parsing and round-trip serialization of every Freeplane type (details, notes, attributes, clouds, edges, connectors, external objects, formulas, unknown hooks). UI ViewModel integration tests verify inspector state and mutation flows.
- **Principle IV (Freeplane Compatibility & Data Integrity)**: PASS — XML structure and attribute naming strictly comply with the Freeplane schema. Unrecognized hooks and desktop map styles are retained in memory and written back identically. Saving uses safe atomic replacement to prevent corruption.
- **Principle V (Performance & Simplicity)**: PASS — XML parsing and serialization execute on `Dispatchers.IO`. UI uses virtualized Compose components and lightweight standard library collections, adhering to YAGNI.

## Project Structure

### Documentation (this feature)

```text
specs/006-support-freeplane-types/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output
│   ├── node-manager-contract.md
│   ├── ui-contracts.md
│   └── xml-schema-contract.md
└── checklists/
    └── requirements.md
```

### Source Code (repository root)

```text
data/
└── src/
    ├── main/java/fr/julien/quievreux/droidplane2/data/
    │   ├── model/
    │   │   ├── Node.kt                  # Extended with details, notes, attributes, styling, clouds, edges, connectors, hooks
    │   │   ├── NodeAttributeEntry.kt    # Key-value attribute entry
    │   │   ├── CloudProperties.kt       # Cloud grouping data model
    │   │   ├── EdgeProperties.kt        # Branch edge styling data model
    │   │   ├── ConnectorLink.kt         # ArrowLink connector model
    │   │   ├── ExternalObjectProperties.kt # Media/image hook model
    │   │   ├── GenericHookElement.kt    # Preserved desktop hooks & scripts
    │   │   ├── NodeAttribute.kt         # Extended attribute enum
    │   │   └── NodeTag.kt               # Extended XML tags enum
    │   ├── NodeManagerContract.kt       # Added updateNode(updatedNode) API
    │   ├── NodeManager.kt               # Extended serialization and atomic file write
    │   ├── NodeUtils.kt / NodeUtilsDefaultImpl.kt # Extended node attribute extraction
    │   └── XmlParseUtils.kt / XmlParseUtilsDefaultImpl.kt # Handlers for attributes, clouds, edges, hooks, rich content types
    └── test/java/fr/julien/quievreux/droidplane2/data/
        ├── NodeManagerTest.kt           # Tests for updateNode and comprehensive serialization
        └── FreeplaneXmlRoundTripTest.kt # Dedicated round-trip fidelity tests with all Freeplane types

app/
└── src/
    ├── main/java/fr/julien/quievreux/droidplane2/
    │   ├── MainUiState.kt               # Added DialogType.NodePropertiesInspector
    │   ├── MainViewModel.kt             # Added onOpenNodeInspector and onSaveNodeProperties
    │   ├── MainActivity.kt              # Host adaptive inspector based on window size
    │   └── ui/
    │       ├── components/
    │       │   ├── NodeList.kt          # Context menu hook for Inspector
    │       │   └── HybridRichContentEditor.kt # Visual preview + raw source toggle
    │       ├── inspector/
    │       │   ├── NodePropertiesInspector.kt # Adaptive root inspector
    │       │   ├── InspectorTabs.kt     # Tabs: Content, Attributes, Style, Edges, Icons, Scripts
    │       │   ├── ContentTab.kt        # Text, Details, Notes, LaTeX
    │       │   ├── AttributesTab.kt     # Key-value attribute table editor
    │       │   ├── StylingTab.kt        # Colors, shapes, fonts, cloud toggle
    │       │   ├── EdgesConnectorsTab.kt# Edge styling & ArrowLink connectors
    │       │   └── IconsMediaTab.kt     # Icon catalog & external images
    │       └── mindmap/
    │           ├── MindMapNodeCard.kt   # Context menu hook & rich visual rendering
    │           └── MindMapCanvasScreen.kt # Render clouds & custom edge styles
    └── test/java/fr/julien/quievreux/droidplane2/
        └── MainViewModelTest.kt         # Verify inspector state opening and node update handling
```

## Phase 0: Outline & Research

Phase 0 research is complete and documented in [research.md](./research.md). Key resolutions:
1. **Domain Model**: Enhanced `Node` with dedicated fields for `richText`, `detailsText`, `noteText`, `attributes`, `cloud`, `edge`, `connectors`, `externalObject`, `latexEquation`, and `genericHooks`.
2. **Parsing & Serialization**: Canonical Freeplane XML sequencing with atomic temporary file staging on save to guarantee data integrity.
3. **Hybrid Rich Content Editor**: Visual formatting toolbar with instant preview and toggleable raw HTML / LaTeX markup editor.
4. **Adaptive Inspector**: Compact `ModalBottomSheet` on phone screens, sliding side panel / multi-section inspector on tablets/landscape.
5. **Script Hooks**: Read and edit script code as raw text; zero on-device execution to prevent security risks.
6. **Freeplane-Aligned Visual Rendering**: Parsed attributes and elements directly govern element rendering (node shapes, background/text colors, font styling, branch edge styles, cloud hulls, and annotation badges) mirroring desktop Freeplane presentation.

## Phase 1: Design & Contracts

Phase 1 design is complete:
- **Data Model**: Documented in [data-model.md](./data-model.md).
- **Contracts**:
  - [contracts/node-manager-contract.md](./contracts/node-manager-contract.md)
  - [contracts/ui-contracts.md](./contracts/ui-contracts.md)
  - [contracts/xml-schema-contract.md](./contracts/xml-schema-contract.md)
- **Quickstart Guide**: Documented in [quickstart.md](./quickstart.md).

## Post-Design Constitution Re-Evaluation

All design decisions strictly comply with the Droidplane Constitution:
- **Principle I (Separation of Concerns)**: Clear boundaries between `:data` (XML domain) and `:app` (inspector UI).
- **Principle II (Unidirectional Data Flow)**: Immutable state models, single source of truth in `NodeManager`.
- **Principle III (Test-Driven Verification)**: Dedicated test suite planned across `:data` and `:app`.
- **Principle IV (Freeplane Compatibility)**: Strict round-trip preservation of desktop Freeplane attributes, styles, and hooks.
- **Principle V (Performance & Simplicity)**: Non-blocking I/O, lean Compose architecture, no superfluous dependencies.

## Complexity Tracking

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| *None* | N/A | Design uses existing module structure and standard Android Jetpack primitives without adding new dependencies. |
