# Quickstart & Validation Guide: Foldable Media Sections & Interactive Node Images (`017-node-media-sections-images`)

## Prerequisites
- JDK 21 configured for Gradle.
- Repository checked out on branch `017-node-media-sections-images`.

## Automated Verification Commands

### 1. Run Unit Tests (`:data` and `:app` on Android & Desktop targets)
```bash
./gradlew :data:testDebugUnitTest :data:desktopTest :app:testDebugUnitTest :app:desktopTest
```

### 2. Expected Test Coverage
- **`MindMapLayoutEngineTest`**:
  - Verifies that a node with `externalObject = ExternalObjectProperties(uri = "file:/sample.png", size = 1.0f)` measures taller and wider than the same node without `externalObject`, and that changing `size` (e.g., `0.5f` vs `2.0f`) scales the node layout dimensions proportionally within `0.2f..4.0f`.
- **`NodeImageResizeCalculationTest` / `MainViewModelTest`**:
  - Verifies corner-drag scale calculation for all 4 corners (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`) with clamping to `0.2f..4.0f`.
  - Verifies `MainViewModel` actions for updating a node's image scale, replacing a node's image URI, and deleting a node's image (`externalObject = null`), as well as foldable category filtering (`NodeIconCategory.entries.filter { it != NodeIconCategory.ALL }`).

## Manual Verification Scenarios

1. **Foldable Media Tab Sections & Inline Assigned Icons**:
   - Open the Node Properties Inspector on a node and switch to the **Media** tab.
   - Verify two foldable sections appear: **Icons** and **Images**.
   - Assign 2 icons to the node from a category subsection (e.g., **Priority** or **Flags**) and collapse the **Icons** section; confirm the 2 assigned icons remain visible inline right after the **Icons** title.
2. **Image File Picker & Canvas Rendering**:
   - Expand the **Images** section, click the File Picker button next to the URI field, and choose a local PNG/JPG image.
   - Save the inspector changes and verify the image renders inside the node card on the Mind Map canvas.
3. **Corner Resize Handles & Long-Press Replace/Delete**:
   - Drag any of the 4 corner anchors on the node's image to resize it visually; save/reopen the `.mm` file to confirm `SIZE` is persisted.
   - Long-press directly on the image on the Mind Map canvas and test both **Replace** and **Delete**.
