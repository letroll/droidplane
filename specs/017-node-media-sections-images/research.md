# Research: Foldable Media Sections (Icons & Images) and Interactive Node Images (`017-node-media-sections-images`)

## 1. Foldable Sections in `IconsMediaTab` & Categorized Subsections in `IconCatalogPicker`

- **Decision**: Replace the flat layout in `IconsMediaTab` with two stateful foldable sections (`Icons` and `Images`, both expanded by default so existing inspector workflows remain immediately accessible while allowing one-tap collapse). In the `Icons` section header, render the section title followed inline by `NodeIconsRow(iconNames = node.iconNames)` whenever `node.iconNames.isNotEmpty()`. In `IconCatalogPicker`, replace the horizontal `FilterChip` bar (which included `NodeIconCategory.ALL`) with independent foldable subsections for each category in `NodeIconCategory.entries.filter { it != NodeIconCategory.ALL }`.
- **Rationale**:
  - Reuses the existing `NodeIconsRow` composable (`app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/components/NodeIconView.kt`) for the inline assigned-icons preview in the `Icons` header.
  - Reuses `NodeIconRegistry.filterIcons(query, category)` for each non-`ALL` category without modifying `:data` or icon definitions.
  - Keeping category subsections collapsed by default (and auto-expanded when `searchQuery.isNotBlank()`) makes the Media tab compact and eliminates long scrolling.
- **Alternatives considered**:
  - Keeping the `ALL` filter chip alongside foldable subsections: rejected because the user explicitly requested foldable subsections for all categories other than `ALL`.

## 2. Multiplatform Image File Picker & Bitmap Decoding (`androidMain` / `desktopMain`)

- **Decision**: Introduce a lightweight `expect`/`actual` pair in `:app` (`app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/media/NodeImageSupport.kt`):
  - `@Composable expect fun rememberImageFilePicker(onUriSelected: (String) -> Unit): () -> Unit`
    - **Android (`androidMain`)**: Uses `rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument())` with `arrayOf("image/*")` and requests persistable read permission (`FLAG_GRANT_READ_URI_PERMISSION`) before passing `uri.toString()`.
    - **Desktop (`desktopMain`)**: Uses `java.awt.FileDialog` in `FileDialog.LOAD` mode filtered to common image extensions (`.png`, `.jpg`, `.jpeg`, `.webp`, `.gif`, `.bmp`) and converts the chosen `File` to a Freeplane-compatible `file:/...` URI (`file.toURI().toString()`).
  - `@Composable expect fun rememberNodeImageBitmap(uri: String): ImageBitmap?`
    - Decodes local `file:` URIs, filesystem paths, and (on Android) `content://` URIs asynchronously on `Dispatchers.IO` using `BitmapFactory` (Android) and `loadImageBitmap` / `ImageIO` (Desktop), returning `null` safely if the URI is unreadable or invalid.
- **Rationale**:
  - Follows Constitution Principle V (Simplicity & YAGNI): uses standard Android Jetpack Activity Result APIs and JDK/Compose Desktop primitives already in the project without adding third-party image/file-picker dependencies.
- **Alternatives considered**:
  - Adding a third-party KMP file picker or Coil/Kamel dependency: rejected per Constitution Principle V because standard platform APIs (`OpenDocument` + `FileDialog`, `BitmapFactory` + `loadImageBitmap`) are already present in `:app`.

## 3. Mind Map Canvas Image Rendering, Layout Sizing, Corner Resize Handles & Long-Press Menu

- **Decision**:
  - **Layout sizing (`MindMapLayoutEngine.measureNode`)**: When `!node.externalObject?.uri.isNullOrBlank()`, compute scaled image dimensions from `DEFAULT_IMAGE_BASE_WIDTH_DP = 120f` and `DEFAULT_IMAGE_BASE_HEIGHT_DP = 90f` multiplied by `scale = (node.externalObject?.size ?: 1.0f).coerceIn(0.2f, 4.0f)`. Expand the node card width to `maxOf(textAndBadgesWidthDp, imageWidthDp + horizontalPaddingDp)` and add `imageHeightDp + IMAGE_VERTICAL_SPACING_DP` to the node card height.
  - **Gesture isolation (`MindMapNodeCard` / `NodeImageAttachmentView`)**:
    - Each of the 4 corner resize handles (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`) uses its own `Modifier.pointerInput` with `detectDragGestures`, consuming drag deltas to update a live preview scale (`0.2f..4.0f`) and invoking `onImageScaleChanged(finalScale)` on release. Because child `pointerInput` consumes the events, the parent `MindMapNodeCard` gesture detector and `MindMapCanvasScreen` pan/zoom detector do not trigger during handle resizing.
    - The image surface uses `Modifier.pointerInput` to detect a long-press specifically on the image, consuming the event and opening a contextual `DropdownMenu` with **Replace** (launching `rememberImageFilePicker`) and **Delete** (clearing `externalObject = null`), without triggering node drag-and-drop.
- **Rationale**:
  - Computing node bounds deterministically in `MindMapLayoutEngine.measureNode` guarantees that branch connectors, clouds, and sibling nodes automatically reflow around scaled node images.
  - Consuming pointer events on the image and its 4 corner anchors prevents conflicts with feature `015-mindmap-node-drag-drop`.
- **Alternatives considered**:
  - Allowing free non-proportional aspect distortion on corner drag: rejected because Freeplane's `<hook NAME="ExternalObject" URI="..." SIZE="..."/>` stores a single uniform `SIZE` scale multiplier (`Float`).
