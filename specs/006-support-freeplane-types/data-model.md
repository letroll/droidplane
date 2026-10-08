# Data Model: Freeplane Document Types & Node Properties

**Feature Branch**: `006-support-freeplane-types`  
**Feature Directory**: `specs/006-support-freeplane-types`  
**Status**: Completed  

## Overview

This document specifies the domain data model for representing, mutating, and serializing all Freeplane document format elements and node types within Droidplane. The model resides strictly in the `:data` module, preserving separation of concerns (Constitution Principle I).

---

## Entity Definitions

### 1. `Node` (Extended Domain Model)

The central domain model representing a mind map topic node.

```kotlin
package fr.julien.quievreux.droidplane2.data.model

import android.net.Uri

data class Node(
    val parentNode: Node?,
    val id: String,
    val numericId: Int,

    // Text & Rich Content
    val text: String?,
    val richText: String? = null,           // <richcontent TYPE="NODE">
    val detailsText: String? = null,       // <richcontent TYPE="DETAILS">
    val noteText: String? = null,          // <richcontent TYPE="NOTE">
    val richTextContents: MutableList<String> = mutableListOf(),
    var richContentType: RichContentType? = null,

    // Hyperlinks & Clones
    val link: Uri? = null,
    val treeIdAttribute: String? = null,

    // Hierarchy
    val childNodes: MutableList<Node> = mutableListOf(),

    // Attributes & Icons
    val attributes: List<NodeAttributeEntry> = emptyList(),
    val iconNames: MutableList<String> = mutableListOf(),

    // Dates & State
    val creationDate: Long?,
    val modificationDate: Long?,
    val isFolded: Boolean = false,

    // Visual Presentation
    val color: String? = null,             // Text color hex (#RRGGBB)
    val backgroundColor: String? = null,   // Background fill color hex (#RRGGBB)
    val style: String? = null,             // Shape / Style: "fork", "bubble", "as_parent", etc.
    val fontName: String? = null,
    val fontSize: Int? = null,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,

    // Grouping & Connections
    val cloud: CloudProperties? = null,
    val edge: EdgeProperties? = null,
    val connectors: List<ConnectorLink> = emptyList(),

    // Layout
    val position: String? = null,          // "left", "right"
    val hgap: Int? = null,
    val vgap: Int? = null,
    val vshift: Int? = null,

    // Extensions & Hooks
    val externalObject: ExternalObjectProperties? = null,
    val latexEquation: String? = null,
    val genericHooks: List<GenericHookElement> = emptyList(),

    // Legacy arrow link destination compatibility
    val arrowLinkDestinationIds: MutableList<String> = mutableListOf(),
    val arrowLinkDestinationNodes: MutableList<Node> = mutableListOf(),
    val arrowLinkIncomingNodes: MutableList<Node> = mutableListOf(),
)
```

---

### 2. Supporting Data Classes

#### `NodeAttributeEntry`
Represents an entry in a node's key-value attribute table (`<attribute NAME="..." VALUE="..." [TYPE="..."]/>`).

```kotlin
data class NodeAttributeEntry(
    val name: String,
    val value: String,
    val type: String? = null,
)
```

#### `CloudProperties`
Represents a visual cloud enclosing a node and its descendants (`<cloud COLOR="..." WIDTH="..." [SHAPE="..."]/>`).

```kotlin
data class CloudProperties(
    val color: String? = null,    // Hex color string (#RRGGBB)
    val width: Int? = null,       // Line width
    val shape: String? = null,    // "ARC", "STAR", "ROUND_RECT", "RECT"
)
```

#### `EdgeProperties`
Represents branch edge styling connecting a child node to its parent (`<edge COLOR="..." STYLE="..." WIDTH="..."/>`).

```kotlin
data class EdgeProperties(
    val color: String? = null,    // Hex color string (#RRGGBB)
    val style: String? = null,    // "linear", "bezier", "sharp-linear", "sharp-bezier", "horizontal", "hide_edge"
    val width: String? = null,    // "thin", "1", "2", "4", "8", or integer
)
```

#### `ConnectorLink` (ArrowLink)
Represents a directional cross-link connecting two arbitrary nodes (`<arrowlink .../>`).

```kotlin
data class ConnectorLink(
    val destinationId: String,
    val color: String? = null,
    val startArrow: String? = null,       // "Default", "None", etc.
    val endArrow: String? = null,         // "Default", "None", etc.
    val startInclination: String? = null, // "dx;dy;"
    val endInclination: String? = null,   // "dx;dy;"
    val sourceLabel: String? = null,
    val middleLabel: String? = null,
    val targetLabel: String? = null,
    val edgeLike: Boolean = false,
)
```

#### `ExternalObjectProperties`
Represents an embedded or linked external media/image reference (`<hook NAME="ExternalObject" URI="..." SIZE="..."/>`).

```kotlin
data class ExternalObjectProperties(
    val uri: String,
    val size: Float? = null,
)
```

#### `GenericHookElement`
Captures unmodeled desktop hooks and scripts verbatim for 100% round-trip preservation.

```kotlin
data class GenericHookElement(
    val name: String,
    val attributes: Map<String, String> = emptyMap(),
    val textContent: String? = null,
    val rawXml: String? = null,
)
```

---

## Validation & Business Rules

1. **Node Identity**:
   - Every node MUST have a non-empty `id` (e.g., `ID_123456789`).
   - `numericId` is derived from `id` for fast array/map indexing.
2. **Text & Content Precedence**:
   - If `richText` is present, it is formatted XHTML (`<html><body>...</body></html>`).
   - `text` contains the plain text label (or plain text representation of `richText`).
   - If both are present, `richText` governs rich rendering, while `text` serves as fallback and plain search target.
3. **Attribute Uniqueness**:
   - Attribute names should ideally be non-blank. Duplicate names within a node are allowed (Freeplane schema allows unbounded choices), but the UI presents them as an ordered list.
4. **Color Format**:
   - Color strings must conform to `#RRGGBB` or `#AARRGGBB` hex representation. Blank or null colors inherit parent/theme styles.
5. **Connector Validity**:
   - `destinationId` must not be blank. If the target node cannot be found in the current index (e.g. cross-map link or deleted node), the connector is retained in data but handled gracefully by the renderer.
6. **Round-Trip Cleanliness**:
   - When a property is cleared or set to null/empty (e.g., cloud disabled, details cleared), its corresponding XML element/attribute is omitted from serialization.

---

## State Transitions & Mutation Flow

```text
[User Edits Node in UI]
         │
         ▼
[NodePropertiesInspector emits NodeUpdate]
         │
         ▼
[MainViewModel.onUpdateNodeProperties(updatedNode)]
         │
         ▼
[NodeManager.updateNode(updatedNode)]
         │
         ├── Update in-memory MindmapIndexes (nodesById, nodesByNumericId)
         ├── Propagate parent/child references up the ancestor chain
         └── Emit updated Node list on StateFlow<List<Node>>
         │
         ▼
[MainUiState reflects new Node properties in Canvas & List views]
```
