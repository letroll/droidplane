# Implementation Plan: Foldable Media Sections (Icons & Images) and Interactive Node Images

**Branch**: `017-node-media-sections-images` | **Date**: 2026-11-09 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/017-node-media-sections-images/spec.md`

## Summary

Restructure the Node Properties Inspector's **Media** tab (`IconsMediaTab`) into two top-level foldable sections (**Icons** and **Images**) where the **Icons** section header displays assigned node icons inline after the title, and `IconCatalogPicker` groups icons into foldable subsections for all non-`ALL` categories (`PRIORITY`, `STATUS`, `PROGRESS`, `FLAGS`, `EMOTION`, `OFFICE`, `TIME`, `SIGNS`, `MISC`). Add a multiplatform image File Picker (`rememberImageFilePicker`) to the **Images** section URI field, render attached node images (`node.externalObject`) directly on the mind map canvas (`MindMapNodeCard` + `MindMapLayoutEngine`), and support interactive visual resizing via 4 corner anchor handles (`0.2x..4.0x`) as well as a long-press context menu on the image to **Replace** or **Delete** it.

## Technical Context

**Language/Version**: Kotlin 2.1.x (JVM target 21, Java 21 toolchain)

**Primary Dependencies**: Compose Multiplatform (Material 3, Foundation Gestures), AndroidX Activity Compose (`rememberLauncherForActivityResult`, `ActivityResultContracts.OpenDocument`), JDK AWT (`FileDialog`) & Compose Desktop `loadImageBitmap`, Koin 4.0+

**Storage**: Freeplane `.mm` XML files (`<hook NAME="ExternalObject" URI="..." SIZE="..."/>` already supported in `:data`)

**Testing**: `kotlin.test`, Kotest (`KStringSpec` / `StringSpec`), MockK, kotlinx-coroutines-test

**Target Platform**: Android (minSdk 26, targetSdk 35) and Desktop JVM

**Project Type**: Kotlin Multiplatform mobile + desktop application (`:core`, `:data`, `:app`)

**Performance Goals**: 60/120 fps canvas pan/zoom and corner-handle image resize preview; asynchronous off-main-thread bitmap decoding (`Dispatchers.IO`)

**Constraints**: Zero new third-party dependencies; cyclomatic complexity <= 20 per method; no `!!` operator; `@Preview` for all new Composables; private/internal `const val` for `when` keys and `testTag`s

**Scale/Scope**: `:app` presentation layer (`ui/inspector`, `ui/mindmap`, `ui/media`, `res`, `MainViewModel`) + unit tests in `:app`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Modular Separation of Concerns**: **PASS** — `:data` already models and serializes `ExternalObjectProperties(uri, size)` via `<hook NAME="ExternalObject"/>`. All UI, file picker, bitmap decoding, layout measurement, and gesture handling live exclusively in `:app`.
- **II. Unidirectional Data Flow & State Consistency**: **PASS** — Image URI/scale mutations from the Media tab, corner resize handles, or long-press menu dispatch to `MainViewModel` (`updateNodeImageScale`, `replaceNodeImage`, `removeNodeImage` / `saveNodeProperties`), which updates `NodeManager` and increments `treeVersion` so `StateFlow` state stays consistent.
- **III. Test-Driven Verification**: **PASS** — Unit tests in `:app` (`MindMapLayoutEngineTest`, `NodeImageResizeCalculationTest`, `MainViewModelTest`, `NodeIconRegistryTest`) cover layout sizing for nodes with images, corner-handle scale math and clamping, ViewModel image update/replace/delete operations, and non-`ALL` category subsections, plus `@Preview` annotations on all new Composables.
- **IV. Freeplane Format Compatibility & Data Integrity**: **PASS** — Uses existing `<hook NAME="ExternalObject" URI="..." SIZE="..."/>` XML serialization in `:data` without schema drift.
- **V. Performance, Large Map Responsiveness & Simplicity**: **PASS** — Image decoding runs asynchronously on `Dispatchers.IO` via `produceState`, and file picking uses built-in platform APIs without adding external libraries.
- **VI. SOLID Principles & Cyclomatic Complexity**: **PASS** — Corner resize math, foldable section UI, and node image rendering are decomposed into focused helper functions/composables with cyclomatic complexity well below 20.

## Project Structure

### Documentation (this feature)

```text
specs/017-node-media-sections-images/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── media-inspector-and-canvas-image-contract.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
app/
├── src/commonMain/kotlin/fr/julien/quievreux/droidplane2/
│   ├── MainViewModel.kt                                 # Image scale/replace/delete actions on Node
│   ├── res/
│   │   └── AppRes.kt                                    # Localized strings for Icons/Images sections, browse/replace/delete image
│   ├── ui/
│   │   ├── inspector/
│   │   │   ├── IconsMediaTab.kt                         # Foldable Icons (with inline assigned icons in header) & Images sections + file picker button
│   │   │   └── IconCatalogPicker.kt                     # Foldable subsections for every category != ALL
│   │   ├── media/
│   │   │   └── NodeImageSupport.kt                      # expect declarations: rememberImageFilePicker & rememberNodeImageBitmap + scale math
│   │   ├── mindmap/
│   │   │   ├── MindMapLayoutEngine.kt                   # Include scaled image dimensions in measureNode
│   │   │   ├── MindMapCanvasScreen.kt                   # Wire image scale/replace/delete callbacks to MindMapNodeCard
│   │   │   ├── MindMapNodeCard.kt                       # Render NodeImageAttachmentView inside node card when externalObject.uri is present
│   │   │   └── NodeImageAttachmentView.kt               # Composable rendering node image, 4 corner resize anchors, and long-press Replace/Delete menu
├── src/androidMain/
│   ├── kotlin/fr/julien/quievreux/droidplane2/
│   │   ├── MainActivity.kt                              # Forward image scale/replace/delete callbacks from MainViewModel to MindMapCanvasScreen
│   │   └── ui/media/
│   │       └── NodeImageSupport.android.kt              # Android OpenDocument launcher + BitmapFactory image loader
│   └── res/
│       ├── values/strings.xml                           # EN strings for foldable sections & image actions
│       └── values-fr/strings.xml                        # FR strings for foldable sections & image actions
├── src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/
│   ├── ui/DesktopDroidplaneApp.kt                       # Forward image scale/replace/delete callbacks from MainViewModel to MindMapCanvasScreen
│   └── ui/media/
│       └── NodeImageSupport.desktop.kt                  # Desktop AWT FileDialog image picker + loadImageBitmap loader
└── src/commonTest/kotlin/fr/julien/quievreux/droidplane2/
    ├── MainViewModelTest.kt                             # Tests for updateNodeImageScale, replaceNodeImage, removeNodeImage
    ├── model/NodeIconRegistryTest.kt                    # Tests for non-ALL foldable category grouping
    └── ui/mindmap/
        ├── MindMapLayoutEngineTest.kt                   # Tests for measureNode with externalObject image & scale
        └── NodeImageResizeCalculationTest.kt            # Tests for 4-corner resize delta math and clamping (0.2f..4.0f)
```

**Structure Decision**: Multiplatform `:app` module architecture using `commonMain` for shared Compose UI, layout calculation, and ViewModel orchestration, with minimal `expect`/`actual` implementations in `androidMain` and `desktopMain` for native image file picking and bitmap decoding.

## Complexity Tracking

No Constitution Check violations.
