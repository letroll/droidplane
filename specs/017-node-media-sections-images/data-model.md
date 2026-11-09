# Data Model: Foldable Media Sections (Icons & Images) and Interactive Node Images (`017-node-media-sections-images`)

## 1. Domain Entities (`:data` — Existing, Unchanged)

### `ExternalObjectProperties` (`data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/ExternalObjectProperties.kt`)

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `uri` | `String` | required | Image resource URI (`file:/...`, `content://...`, or local path). Serialized as `URI` attribute on `<hook NAME="ExternalObject"/>`. |
| `size` | `Float?` | `null` (treated as `1.0f`) | Uniform display scale factor (`1.0f` = 100% base size). Clamped to `0.2f..4.0f` during interactive corner resizing. Serialized as `SIZE` attribute on `<hook NAME="ExternalObject"/>`. |

### `Node` (`data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/Node.kt`)

| Field | Type | Description |
|-------|------|-------------|
| `iconNames` | `MutableList<String>` | Ordered list of Freeplane `BUILTIN` icon identifiers assigned to the node. Displayed inline in the `Icons` foldable section header and on the node card. |
| `externalObject` | `ExternalObjectProperties?` | Optional attached image metadata (`uri`, `size`). When `null` or `uri.isBlank()`, no image is displayed on the node card. |

## 2. Presentation Models & Constants (`:app`)

### `NodeIconCategory` (`app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/model/NodeIconRegistry.kt`)

- Categorized subsections rendered in `IconCatalogPicker` exclude `NodeIconCategory.ALL`:
  - `PRIORITY`, `STATUS`, `PROGRESS`, `FLAGS`, `EMOTION`, `OFFICE`, `TIME`, `SIGNS`, `MISC`.

### `ImageResizeCorner` (`app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/NodeImageAttachmentView.kt`)

| Corner | Horizontal Sign | Vertical Sign | Description |
|--------|-----------------|---------------|-------------|
| `TOP_LEFT` | `-1f` | `-1f` | Dragging top-left outward (`dx < 0` or `dy < 0`) increases scale; inward decreases scale. |
| `TOP_RIGHT` | `+1f` | `-1f` | Dragging top-right outward (`dx > 0` or `dy < 0`) increases scale; inward decreases scale. |
| `BOTTOM_LEFT` | `-1f` | `+1f` | Dragging bottom-left outward (`dx < 0` or `dy > 0`) increases scale; inward decreases scale. |
| `BOTTOM_RIGHT` | `+1f` | `+1f` | Dragging bottom-right outward (`dx > 0` or `dy > 0`) increases scale; inward decreases scale. |

### Validation & Scale Rules

- **Scale Clamping**: `MIN_IMAGE_SCALE = 0.2f`, `MAX_IMAGE_SCALE = 4.0f`, `DEFAULT_IMAGE_SCALE = 1.0f`.
- **Base Canvas Dimensions**: `DEFAULT_IMAGE_BASE_WIDTH_DP = 120f`, `DEFAULT_IMAGE_BASE_HEIGHT_DP = 90f`, `IMAGE_VERTICAL_SPACING_DP = 8f`.
- **Deletion**: Removing an image sets `node.externalObject = null`.
- **Replacement**: Selecting a new file in the Replace flow updates `node.externalObject = (node.externalObject ?: ExternalObjectProperties(uri = newUri, size = 1.0f)).copy(uri = newUri)`.
