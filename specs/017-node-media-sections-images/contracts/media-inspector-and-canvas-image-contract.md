# UI & Component Contracts: Media Tab & Mind Map Node Image (`017-node-media-sections-images`)

## 1. `IconsMediaTab` & `IconCatalogPicker` Contract (`ui/inspector`)

### `IconsMediaTab`
- Renders a vertically scrollable `Column` containing two `FoldableMediaSection` blocks:
  1. **Icons Section**:
     - Header displays `stringResource(R.string.icons_section)` followed inline by `NodeIconsRow(iconNames = node.iconNames)` when `node.iconNames.isNotEmpty()`.
     - Tapping the header toggles the section between expanded and collapsed states.
     - When expanded, renders `ActiveIconsManager` and `IconCatalogPicker`.
  2. **Images Section**:
     - Header displays `stringResource(R.string.images_section)`.
     - Tapping the header toggles the section between expanded and collapsed states.
     - When expanded, renders:
       - An image URI `OutlinedTextField` paired with a File Picker button (`onLaunchFilePicker`) that invokes `rememberImageFilePicker` and updates `node.externalObject`.
       - An image scale (`size`) `OutlinedTextField`.

### `IconCatalogPicker`
- Renders a search `OutlinedTextField` followed by foldable category subsections for each category in `NodeIconCategory.entries.filter { it != NodeIconCategory.ALL }`.
- When `searchQuery.isBlank()`: each category subsection header toggles its own expanded/collapsed state independently.
- When `searchQuery.isNotBlank()`: only categories with matching icons are shown, and their bodies are automatically expanded.

## 2. `MindMapLayoutEngine` & `MindMapNodeCard` Image Contract (`ui/mindmap`)

### `MindMapLayoutEngine.measureNode`
- If `!node.externalObject?.uri.isNullOrBlank()`:
  - `scale = (node.externalObject?.size ?: 1.0f).coerceIn(0.2f, 4.0f)`
  - `imageWidthDp = 120f * scale`
  - `imageHeightDp = 90f * scale`
  - Node width is `maxOf(textWidthDp, imageWidthDp + horizontalPaddingDp) * density`.
  - Node height is `(textHeightDp + imageHeightDp + 8f) * density`.

### `MindMapNodeCard` & `NodeImageAttachmentView`
- Parameters added to `MindMapNodeCard` (and forwarded from `MindMapCanvasScreen`):
  - `onNodeImageScaleChange: (Node, Float) -> Unit = { _, _ -> }`
  - `onNodeImageReplace: (Node, String) -> Unit = { _, _ -> }`
  - `onNodeImageDelete: (Node) -> Unit = {}`
- When `!node.externalObject?.uri.isNullOrBlank()`:
  - Renders `NodeImageAttachmentView` below `NodeCardContentRow`.
  - Displays 4 corner anchor handles (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`). Dragging any anchor scales the image preview in real time (`0.2f..4.0f`) and calls `onNodeImageScaleChange(node, updatedScale)` on drag end.
  - Long-pressing the image opens a contextual menu with:
    - **Replace** (`R.string.replace_image`): triggers `rememberImageFilePicker` and calls `onNodeImageReplace(node, selectedUri)`.
    - **Delete** (`R.string.delete_image`): calls `onNodeImageDelete(node)`.
