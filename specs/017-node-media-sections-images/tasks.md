---

description: "Task list for Foldable Media Sections (Icons & Images) and Interactive Node Images"
---

# Tasks: Foldable Media Sections (Icons & Images) and Interactive Node Images

**Input**: Design documents from `/specs/017-node-media-sections-images/`

**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/media-inspector-and-canvas-image-contract.md`

**Tests**: Automated unit tests are included per Constitution Principle III (Test-Driven Verification).

**Organization**: Tasks are grouped by user story (`US1`, `US2`, `US3`) to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`, `[US2]`, `[US3]`)
- Exact file paths are included in every task description

---

## Phase 1: Setup (Shared Resources & String Keys)

**Purpose**: Localized string resources required across the Media tab and Mind Map node image actions

- [X] T001 Add string constants and localized EN/FR strings (`icons_section`, `images_section`, `browse_image`, `replace_image`, `delete_image`, `broken_image_placeholder`) in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/res/AppRes.kt`, `app/src/androidMain/res/values/strings.xml`, and `app/src/androidMain/res/values-fr/strings.xml`

---

## Phase 2: Foundational (Multiplatform Image Support, Scale Math & ViewModel Image Actions)

**Purpose**: Core multiplatform abstractions, scale calculation helpers, and `MainViewModel` state mutations that all user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T002 Create `NodeImageSupport.kt` in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/media/NodeImageSupport.kt` defining `@Composable expect fun rememberImageFilePicker(onUriSelected: (String) -> Unit): () -> Unit`, `@Composable expect fun rememberNodeImageBitmap(uri: String): ImageBitmap?`, `ImageResizeCorner` (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`), and pure scale helper `computeResizedImageScale` enforcing `MIN_IMAGE_SCALE = 0.2f`, `MAX_IMAGE_SCALE = 4.0f`, `DEFAULT_IMAGE_SCALE = 1.0f`, `DEFAULT_IMAGE_BASE_WIDTH_DP = 120f`, `DEFAULT_IMAGE_BASE_HEIGHT_DP = 90f`, and `IMAGE_VERTICAL_SPACING_DP = 8f`
- [X] T003 [P] Implement Android actual `rememberImageFilePicker` (`ActivityResultContracts.OpenDocument` with `image/*` and persistable read permission) and `rememberNodeImageBitmap` (`Dispatchers.IO` decoding for `file:` and `content://` URIs) in `app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/ui/media/NodeImageSupport.android.kt`
- [X] T004 [P] Implement Desktop actual `rememberImageFilePicker` (`java.awt.FileDialog` filtered to `.png`, `.jpg`, `.jpeg`, `.webp`, `.gif`, `.bmp` returning `file.toURI().toString()`) and `rememberNodeImageBitmap` (`Dispatchers.IO` decoding via `loadImageBitmap`) in `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/media/NodeImageSupport.desktop.kt`
- [X] T005 Add `updateNodeImageScale(nodeId: String, newScale: Float)`, `replaceNodeImage(nodeId: String, newUri: String)`, and `removeNodeImage(nodeId: String)` methods in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt` that clamp `newScale` to `0.2f..4.0f`, update `node.externalObject` via `nodeManager.updateNode`, and increment `treeVersion` (`FR-010`, `SC-004`)

**Checkpoint**: Foundation ready — multiplatform image file picker, bitmap loader, scale math, and ViewModel mutations are in place

---

## Phase 3: User Story 1 - Foldable Icons & Images Sections with Categorized Icon Subsections in Media Tab (Priority: P1) 🎯 MVP

**Goal**: Organize the Media tab (`IconsMediaTab`) into two top-level foldable sections (**Icons** and **Images**), display assigned node icons inline after the **Icons** title in the section header, and replace the flat icon catalog with foldable subsections for all categories other than `ALL` (`PRIORITY`, `STATUS`, `PROGRESS`, `FLAGS`, `EMOTION`, `OFFICE`, `TIME`, `SIGNS`, `MISC`).

**Independent Test**: Open the Node Properties Inspector on a node with and without assigned icons, switch to the Media tab, verify assigned icons appear inline after the **Icons** section header title even when collapsed, toggle the **Icons** and **Images** foldable headers, and expand/collapse individual icon category subsections to add icons.

### Tests for User Story 1

- [X] T006 [P] [US1] Add unit tests in `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/model/NodeIconRegistryTest.kt` verifying `NodeIconRegistry.categorizedEntries()` excludes `NodeIconCategory.ALL` and returns the 9 specific categories (`PRIORITY`, `STATUS`, `PROGRESS`, `FLAGS`, `EMOTION`, `OFFICE`, `TIME`, `SIGNS`, `MISC`) with matching icons filtered by search query (`FR-003`, `FR-004`)

### Implementation for User Story 1

- [X] T007 [US1] Update `NodeIconRegistry` in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/model/NodeIconRegistry.kt` and `IconCatalogPicker` in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/inspector/IconCatalogPicker.kt` to render foldable subsections for every category other than `NodeIconCategory.ALL` (auto-expanding categories with matches when `searchQuery.isNotBlank()`) and include `@Preview` functions (`FR-003`, `FR-004`, `SC-001`)
- [X] T008 [US1] Update `IconsMediaTab` in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/inspector/IconsMediaTab.kt` to organize the scrollable column into two foldable sections (**Icons** and **Images**) where the **Icons** header renders `NodeIconsRow(iconNames = node.iconNames)` inline right after the title when `node.iconNames.isNotEmpty()`, with `@Preview` functions (`FR-001`, `FR-002`, `SC-001`)

**Checkpoint**: User Story 1 is independently functional and testable

---

## Phase 4: User Story 2 - Image File Picker in Media Tab and Image Rendering on Mind Map (Priority: P1)

**Goal**: Provide an integrated File Picker button in the **Images** section URI field so users can easily select a local image file, and compute/render the attached image on the Mind Map node card (`MindMapLayoutEngine` + `MindMapNodeCard`).

**Independent Test**: Expand the **Images** section in the Media tab, click the File Picker button to populate the image URI, and verify that `MindMapLayoutEngine.measureNode` expands the node's layout bounds for the scaled image (`120f * scale` by `90f * scale` dp) and renders the image on the Mind Map canvas.

### Tests for User Story 2

- [X] T009 [P] [US2] Add unit tests in `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngineTest.kt` verifying `MindMapLayoutEngine.measureNode` expands node width and height when `node.externalObject?.uri` is non-blank according to `size` (`0.2f..4.0f`, defaulting to `1.0f` on base `120f x 90f` dp) and leaves nodes with null/blank `externalObject` unchanged (`FR-006`)

### Implementation for User Story 2

- [X] T010 [US2] Add the integrated File Picker button (`rememberImageFilePicker`) to the image URI row inside the **Images** foldable section of `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/inspector/IconsMediaTab.kt` to populate `node.externalObject.uri` (`size = node.externalObject?.size ?: 1.0f`) (`FR-005`, `SC-002`)
- [X] T011 [US2] Update `MindMapLayoutEngine.measureNode` in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngine.kt` to account for scaled node image dimensions (`DEFAULT_IMAGE_BASE_WIDTH_DP = 120f`, `DEFAULT_IMAGE_BASE_HEIGHT_DP = 90f`, `IMAGE_VERTICAL_SPACING_DP = 8f`, `scale = (node.externalObject?.size ?: 1.0f).coerceIn(0.2f, 4.0f)`) when `!node.externalObject?.uri.isNullOrBlank()` (`FR-006`)

**Checkpoint**: User Stories 1 and 2 are functional — users can pick an image in the Media tab and node layout bounds adapt to the attached image

---

## Phase 5: User Story 3 - Interactive Image Resizing via Corner Anchors and Long-Press Context Actions (Priority: P2)

**Goal**: Render the attached image inside `MindMapNodeCard` via `NodeImageAttachmentView` with 4 interactive corner resize anchors (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`) that visually scale the image (`0.2x..4.0x`) and a long-press context menu on the image to **Replace** (via file picker) or **Delete** the image without conflicting with node drag-and-drop or canvas pan/zoom.

**Independent Test**: On a node with an attached image on the Mind Map canvas, drag any of the 4 corner anchors to resize the image (`0.2x..4.0x`) and verify `externalObject.size` updates; long-press directly on the image to open the context menu and verify **Replace** and **Delete**.

### Tests for User Story 3

- [X] T012 [P] [US3] Create unit tests in `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/NodeImageResizeCalculationTest.kt` verifying `computeResizedImageScale` across all 4 `ImageResizeCorner` values (`TOP_LEFT`, `TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`) and clamping to `MIN_IMAGE_SCALE = 0.2f`..`MAX_IMAGE_SCALE = 4.0f` (`FR-007`, `SC-003`)
- [X] T013 [P] [US3] Add unit tests in `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` verifying `updateNodeImageScale`, `replaceNodeImage`, and `removeNodeImage` update `NodeManager`, clamp scale to `0.2f..4.0f`, and increment `treeVersion` (`FR-007`, `FR-008`, `FR-010`, `SC-004`)

### Implementation for User Story 3

- [X] T014 [US3] Create `NodeImageAttachmentView` in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/NodeImageAttachmentView.kt` rendering the node image via `rememberNodeImageBitmap` (with a non-crashing broken-image fallback), 4 corner resize anchor handles that consume pointer drag events to scale the image (`0.2f..4.0f`), and a long-press context menu with **Replace** (via `rememberImageFilePicker`) and **Delete** actions, plus `@Preview` functions (`FR-006`, `FR-007`, `FR-008`, `FR-009`, `SC-002`, `SC-003`)
- [X] T015 [US3] Integrate `NodeImageAttachmentView` into `MindMapNodeCard` in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt` and wire `onNodeImageScaleChange`, `onNodeImageReplace`, and `onNodeImageDelete` callbacks through `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasScreen.kt`, `app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/MainActivity.kt`, and `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/DesktopDroidplaneApp.kt` (`FR-006`, `FR-007`, `FR-008`, `FR-009`, `FR-010`)

**Checkpoint**: All user stories (`US1`, `US2`, `US3`) are complete and independently testable

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Full multiplatform test suite verification across `:data` and `:app`

- [X] T016 Run `./gradlew :data:testDebugUnitTest :data:desktopTest :app:testDebugUnitTest :app:desktopTest` to verify all unit tests, Freeplane `.mm` round-trip compatibility, and quickstart scenarios in `specs/017-node-media-sections-images/quickstart.md` (`SC-001`..`SC-004`)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1 — `T001`)**: No dependencies — starts immediately.
- **Foundational (Phase 2 — `T002`–`T005`)**: Depends on `T001`; `T003` and `T004` run in parallel `[P]` after `T002`. Blocks `US1`, `US2`, and `US3`.
- **User Story 1 (Phase 3 — `T006`–`T008`)**: Depends on Foundational phase completion.
- **User Story 2 (Phase 4 — `T009`–`T011`)**: Depends on Foundational phase completion (`T010` also builds on `IconsMediaTab.kt` from `T008`).
- **User Story 3 (Phase 5 — `T012`–`T015`)**: Depends on Foundational phase and `T011` (`MindMapLayoutEngine` sizing).
- **Polish (Phase 6 — `T016`)**: Depends on all user stories completing.

### Parallel Opportunities

- **Foundational Phase**: `T003` (`NodeImageSupport.android.kt`) and `T004` (`NodeImageSupport.desktop.kt`) can be implemented in parallel `[P]`.
- **Unit Test Authoring**: `T006` (`NodeIconRegistryTest.kt`), `T009` (`MindMapLayoutEngineTest.kt`), `T012` (`NodeImageResizeCalculationTest.kt`), and `T013` (`MainViewModelTest.kt`) target distinct test files and can run in parallel `[P]`.

---

## Parallel Example: Foundational & Test Tasks

```bash
# Parallel platform actual implementations after T002:
Task T003: "Implement Android actual rememberImageFilePicker and rememberNodeImageBitmap in app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/ui/media/NodeImageSupport.android.kt"
Task T004: "Implement Desktop actual rememberImageFilePicker and rememberNodeImageBitmap in app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/media/NodeImageSupport.desktop.kt"

# Parallel unit test tasks across stories:
Task T006: "Add unit tests in app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/model/NodeIconRegistryTest.kt"
Task T009: "Add unit tests in app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngineTest.kt"
Task T012: "Create unit tests in app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/ui/mindmap/NodeImageResizeCalculationTest.kt"
Task T013: "Add unit tests in app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 + User Story 2)

1. Complete Phase 1 (`T001`) and Phase 2 (`T002`–`T005`).
2. Complete Phase 3 (`US1`: `T006`–`T008`) for foldable **Icons** and **Images** sections and categorized icon subsections.
3. Complete Phase 4 (`US2`: `T009`–`T011`) for the image File Picker and layout sizing.
4. Complete Phase 5 (`US3`: `T012`–`T015`) for on-canvas image rendering, 4-corner resize handles, and long-press Replace/Delete menu.
5. Run Phase 6 (`T016`) full verification.
