# Tasks: Freeplane Document Types Support and Editing

**Input**: Design documents from `/specs/006-support-freeplane-types/`  
**Prerequisites**: `plan.md` (required), `spec.md` (required), `research.md`, `data-model.md`, `contracts/`, `quickstart.md`  
**Tests**: Automated tests are included per Constitution Principle III (Test-Driven Verification) using `kotlin.test` and Kotest.  
**Organization**: Tasks are grouped by user story (US1 through US6) to enable independent implementation, node inspector editing, visual rendering integration, and testing of each story.  

## Format: `- [ ] [ID] [P?] [Story?] Description with file path`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., `[US1]`, `[US2]`)
- Include exact file paths in descriptions

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Update XML schema constants, attributes, UI resources, and Help mindmap required for Freeplane document types and visual styling

- [x] T001 Extend XML schema constants and tags in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/NodeTag.kt` and `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/NodeAttribute.kt` adding `CLOUD`, `EDGE`, `ATTRIBUTE`, `HOOK`, `STYLE`, `COLOR`, `BACKGROUND_COLOR`, `FOLDED`, `HGAP`, `VGAP`, `VSHIFT`
- [x] T002 [P] Add localized string resources and icons for inspector tabs, styling controls, and property labels in `app/src/main/res/values/strings.xml`
- [x] T003 [P] Update Help mindmap in `app/src/main/res/raw/example.mm` and `data/src/main/java/fr/julien/quievreux/droidplane2/data/FakeDataSource.kt` with comprehensive demonstrative usages of all new Freeplane attributes (shapes, colors, clouds, edges, details, notes, attributes, icons, LaTeX, external objects)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core domain models, `NodeManager` mutations, and adaptive inspector shell that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this foundational phase is complete

- [x] T004 [P] Create `NodeAttributeEntry` model in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/NodeAttributeEntry.kt` with fields `name: String`, `value: String`, and `type: String? = null`
- [x] T005 [P] Create `CloudProperties` model in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/CloudProperties.kt` with fields `color: String? = null`, `width: Int? = null`, and `shape: String? = null`
- [x] T006 [P] Create `EdgeProperties` model in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/EdgeProperties.kt` with fields `color: String? = null`, `style: String? = null`, and `width: String? = null`
- [x] T007 [P] Create `ConnectorLink` model in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/ConnectorLink.kt` with fields `destinationId: String`, `color: String? = null`, `startArrow: String? = null`, `endArrow: String? = null`, `startInclination: String? = null`, `endInclination: String? = null`, `sourceLabel: String? = null`, `middleLabel: String? = null`, `targetLabel: String? = null`, and `edgeLike: Boolean = false`
- [x] T008 [P] Create `ExternalObjectProperties` model in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/ExternalObjectProperties.kt` with fields `uri: String` and `size: Float? = null`
- [x] T009 [P] Create `GenericHookElement` model in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/GenericHookElement.kt` with fields `name: String`, `attributes: Map<String, String> = emptyMap()`, `textContent: String? = null`, and `rawXml: String? = null`
- [x] T010 Extend `Node` domain class in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/Node.kt` with fields `richText: String? = null`, `detailsText: String? = null`, `noteText: String? = null`, `attributes: MutableList<NodeAttributeEntry> = mutableListOf()`, `cloud: CloudProperties? = null`, `edge: EdgeProperties? = null`, `connectors: MutableList<ConnectorLink> = mutableListOf()`, `externalObject: ExternalObjectProperties? = null`, `latexEquation: String? = null`, `genericHooks: MutableList<GenericHookElement> = mutableListOf()`, `color: String? = null`, `backgroundColor: String? = null`, `style: String? = null`, `fontName: String? = null`, `fontSize: Int? = null`, `isFolded: Boolean = false`, `hgap: Int? = null`, `vgap: Int? = null`, and `vshift: Int? = null`, with comprehensive `equals` and `hashCode` implementations
- [x] T011 Update `NodeManagerContract` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt` adding `suspend fun updateNode(updatedNode: Node): Boolean`
- [x] T012 Implement `updateNode` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to update `rootNode` and child references, rebuild indexes via `nodeUtils.loadAndIndexNodesByIds(rootNode)`, and emit the updated list on `_allNodes` StateFlow
- [x] T013 [P] Create unit test in `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerUpdateTest.kt` verifying `updateNode` updates in-memory indexes and propagates through parent ancestors
- [x] T014 Add `DialogType.NodePropertiesInspector(val node: Node, val initialTab: InspectorTab = InspectorTab.CONTENT)` and `InspectorTab` enum (`CONTENT`, `ATTRIBUTES_LINKS`, `STYLING_CLOUD`, `EDGES_CONNECTORS`, `ICONS_MEDIA`, `SCRIPTS`) in `app/src/main/java/fr/julien/quievreux/droidplane2/MainUiState.kt`
- [x] T015 Add `onOpenNodeInspector`, `onSaveNodeProperties`, and `onDismissNodeInspector` handlers in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- [x] T016 [P] Create foundation adaptive inspector container `NodePropertiesInspector` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/NodePropertiesInspector.kt` supporting `ModalBottomSheet` on compact screens (< 600dp) and side panel dialog on expanded screens (>= 600dp) with `@Preview`
- [x] T017 Wire context menu actions in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt`, `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt`, and `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt` to open and host `NodePropertiesInspector`

**Checkpoint**: Foundation ready — user story implementation can now begin independently.

---

## Phase 3: User Story 1 - Core Text, Details, Notes, and Rich Content Editing (Priority: P1) 🎯 MVP

**Goal**: Users can view, edit, format, and render the main node text, expandable details, and extended notes using a hybrid editor with instant preview and raw source mode, with visual indicators in node cards.

**Independent Test**: Load a mind map containing plain and rich text, details, and notes. Edit each text element on a node via the node inspector, save the map, and reload to verify that all content, rich markup, and visual details persist and render correctly.

### Tests for User Story 1 ⚠️

- [x] T018 [P] [US1] Unit test for parsing and serializing `richText` (`TYPE="NODE"`), `detailsText` (`TYPE="DETAILS"`), and `noteText` (`TYPE="NOTE"`) in `data/src/test/java/fr/julien/quievreux/droidplane2/data/RichContentXmlTest.kt`
- [x] T019 [P] [US1] Unit test in `app/src/test/java/fr/julien/quievreux/droidplane2/ui/inspector/ContentTabViewModelTest.kt` verifying content edits propagate to `onSaveNodeProperties`

### Implementation for User Story 1

- [x] T020 [US1] Update `parseRichContent` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt` to inspect `TYPE` and populate `node.richText` (`TYPE="NODE"`), `node.detailsText` (`TYPE="DETAILS"`), and `node.noteText` (`TYPE="NOTE"`)
- [x] T021 [US1] Update `NodeManager.serializeNode` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to serialize `<richcontent TYPE="NODE">`, `<richcontent TYPE="DETAILS">`, and `<richcontent TYPE="NOTE">` in canonical sequence
- [x] T022 [P] [US1] Create `HybridRichContentEditor` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/HybridRichContentEditor.kt` with visual preview toolbar (Bold, Italic, Color) and toggleable raw HTML/source editor with `@Preview`
- [x] T023 [US1] Create `ContentTab` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/ContentTab.kt` allowing editing of node title (plain/rich), details, notes, and LaTeX equation using `HybridRichContentEditor` with `@Preview`
- [x] T024 [US1] Integrate `ContentTab` into `NodePropertiesInspector` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/NodePropertiesInspector.kt`

**Checkpoint**: User Story 1 (MVP) is fully functional and testable independently.

---

## Phase 4: User Story 2 - Key-Value Attributes and Metadata Management (Priority: P1)

**Goal**: Users can manage structured key-value attributes (add, edit, delete), assign/remove icons, and configure hyperlinks via the node inspector, with visual icon and link badges on nodes.

**Independent Test**: Add custom attributes, assign icons, and set a hyperlink on a node. Save and reload the document to verify all attributes, icons, and links persist accurately and display appropriately on nodes.

### Tests for User Story 2 ⚠️

- [x] T025 [P] [US2] Unit test for parsing and serializing `<attribute NAME="..." VALUE="..." TYPE="..."/>` in `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeAttributeXmlTest.kt`
- [x] T026 [P] [US2] Unit test in `app/src/test/java/fr/julien/quievreux/droidplane2/ui/inspector/AttributesTabTest.kt` verifying attribute additions, edits, and deletions persist

### Implementation for User Story 2

- [x] T027 [US2] Implement `parseAttribute` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt` to parse `<attribute>` tags into `node.attributes`
- [x] T028 [US2] Update `NodeManager.serializeNode` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to serialize all `NodeAttributeEntry` elements
- [x] T029 [P] [US2] Create `AttributesTab` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/AttributesTab.kt` with add/edit/delete rows for key-value attributes and hyperlink input field (URL and internal `#ID_`) with `@Preview`
- [x] T030 [P] [US2] Create `IconsMediaTab` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/IconsMediaTab.kt` with Freeplane icon catalog grid selection and removal with `@Preview`
- [x] T031 [US2] Integrate `AttributesTab` and `IconsMediaTab` into `NodePropertiesInspector` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/NodePropertiesInspector.kt`

**Checkpoint**: User Stories 1 AND 2 work independently.

---

## Phase 5: User Story 3 - Visual Styling, Shapes, Colors, and Cloud Grouping (Priority: P2)

**Goal**: Newly parsed visual attributes (`COLOR`, `BACKGROUND_COLOR`, `STYLE`, `font`, `<cloud>`) are editable in the node inspector and directly drive element rendering in Freeplane desktop style: custom node shapes (`bubble`/`oval`, `rectangle`, `fork`), foreground/background colors, font size and typography, and cloud grouping hulls.

**Independent Test**: Change node shape to bubble in inspector, set text color to `#003366` and background to `#FFF4CC`, apply a cloud with shape `ROUND_RECT` and color `#FFCC99`, save, and reload to verify visual properties persist and render in `MindMapNodeCard`.

### Tests for User Story 3 ⚠️

- [x] T032 [P] [US3] Unit test for parsing and serializing `<cloud COLOR="..." WIDTH="..." SHAPE="..."/>` and node styling attributes (`COLOR`, `BACKGROUND_COLOR`, `STYLE`, `font`) in `data/src/test/java/fr/julien/quievreux/droidplane2/data/VisualPropertiesXmlTest.kt`

### Implementation for User Story 3

- [x] T033 [US3] Implement `parseCloud` and update `parseNodeTag` / `parseFont` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt` and `NodeUtilsDefaultImpl.kt` to extract cloud, colors, shapes (`STYLE`), and font sizes
- [x] T034 [US3] Update `NodeManager.serializeNode` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to serialize `<cloud>`, `<font>`, and node styling attributes (`COLOR`, `BACKGROUND_COLOR`, `STYLE`)
- [x] T035 [P] [US3] Create `StylingTab` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/StylingTab.kt` with text color picker, background fill color picker, shape selector (Fork, Bubble, Oval, Rectangle), font controls (bold, italic, size), and cloud toggle/configurator with `@Preview`
- [x] T036 [US3] Integrate `StylingTab` into `NodePropertiesInspector` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/NodePropertiesInspector.kt`
- [x] T037 [US3] Update `MindMapNodeCard.kt` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt` to render shapes (`bubble`/`oval` as pill/ellipse, `rectangle` as 2dp corners, `fork` as branch bracket), apply `node.color` as text color, `node.backgroundColor` as surface fill, `node.fontSize` as font size, and `node.cloud?.color` as cloud-colored outline

**Checkpoint**: User Stories 1, 2, and 3 work independently with accurate Freeplane visual styling and node inspector editing.

---

## Phase 6: User Story 4 - Relationship Connectors and Edge Styling (Priority: P2)

**Goal**: Newly parsed edge properties (`<edge>`) and arrow links (`<arrowlink>`) are editable in the node inspector and directly drive visual rendering: branch lines draw as linear straight lines, bezier splines, or hidden edges with custom thickness and color, and connectors draw directional arrows between arbitrary nodes.

**Independent Test**: Connect two non-adjacent nodes with an arrow link via the inspector, specify labels and colors, change branch edge to linear with custom color and width, save, and reload to verify all connector and edge attributes persist and render in `MindMapBranchDrawer`.

### Tests for User Story 4 ⚠️

- [x] T038 [P] [US4] Unit test for parsing and serializing `<edge COLOR="..." STYLE="..." WIDTH="..."/>` and `<arrowlink DESTINATION="..." .../>` in `data/src/test/java/fr/julien/quievreux/droidplane2/data/ConnectorsXmlTest.kt`

### Implementation for User Story 4

- [x] T039 [US4] Implement `parseEdge` and update `parseArrowLink` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt` to populate `node.edge` and `node.connectors` (with color, labels, arrows)
- [x] T040 [US4] Update `NodeManager.serializeNode` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to serialize `<edge>` and `<arrowlink>` elements
- [x] T041 [P] [US4] Create `EdgesConnectorsTab` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/EdgesConnectorsTab.kt` with edge line style, color, width selectors, and connector link creator/editor with `@Preview`
- [x] T042 [US4] Integrate `EdgesConnectorsTab` into `NodePropertiesInspector` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/NodePropertiesInspector.kt`
- [x] T043 [US4] Update `MindMapLayoutEngine.kt` and `MindMapBranchDrawer.kt` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/` to pass edge properties to `BranchConnector` and render `linear` straight paths, `bezier` curves, `hide_edge` omissions, custom stroke widths, and custom edge colors

**Checkpoint**: User Stories 1 through 4 work independently with custom edge and connector rendering and editing.

---

## Phase 7: User Story 5 - External Objects and Mathematical Formulas (Priority: P3)

**Goal**: Users can attach or edit external media references (images with URI and scaling) and embedded mathematical equations (LaTeX formula syntax) via the node inspector, with visual formula and image support.

**Independent Test**: Add an external image reference with URI and scale factor to a node via the inspector, add a LaTeX formula to another node, save the document, and reload to confirm the external object and formula hooks are retained and formatted properly.

### Tests for User Story 5 ⚠️

- [x] T044 [P] [US5] Unit test for parsing and serializing `<hook NAME="ExternalObject" URI="..." SIZE="..."/>` and `<hook NAME="plugins/latex/LatexNodeHook.properties" EQUATION="..."/>` in `data/src/test/java/fr/julien/quievreux/droidplane2/data/HooksXmlTest.kt`

### Implementation for User Story 5

- [x] T045 [US5] Implement `parseHook` for `ExternalObject` and LaTeX formulas in `data/src/main/java/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt`
- [x] T046 [US5] Update `NodeManager.serializeNode` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to serialize external object hooks and formula hooks
- [x] T047 [US5] Add external object image URI and scale factor fields into `IconsMediaTab` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/IconsMediaTab.kt` with `@Preview`

**Checkpoint**: User Stories 1 through 5 work independently.

---

## Phase 8: User Story 6 - High-Fidelity Preservation of Advanced Map Structures (Priority: P3)

**Goal**: Any unedited desktop elements (map styles, layout gaps, script hooks) are strictly preserved with 100% round-trip fidelity, and embedded scripts are viewable/editable as raw text in the node inspector.

**Independent Test**: Load a comprehensive desktop Freeplane map with embedded map styles, script hooks, and custom layouts. Edit several node titles and attributes, save the file, and perform a structural comparison to verify that all untouched elements and hooks are preserved byte-for-byte.

### Tests for User Story 6 ⚠️

- [x] T048 [P] [US6] Unit test for round-trip preservation of arbitrary desktop hooks (`MapStyle`, scripts, layout coordinates) in `data/src/test/java/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt`

### Implementation for User Story 6

- [x] T049 [US6] Implement generic hook capture in `data/src/main/java/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt` populating `node.genericHooks`
- [x] T050 [US6] Update `NodeManager.serializeNode` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` to write preserved generic hooks and layout coordinates (`HGAP`, `VGAP`, `VSHIFT`)
- [x] T051 [P] [US6] Create `ScriptsTab` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/ScriptsTab.kt` displaying editable raw script hook text and parameters with `@Preview`
- [x] T052 [US6] Implement atomic safe write (`.mm.tmp` -> replace `.mm`) in `NodeManager.serializeMindmap` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt`

**Checkpoint**: All 6 user stories are fully functional and verified.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Documentation updates, quickstart end-to-end verification, and final quality checks

- [x] T053 [P] Update `README.md` and project documentation reflecting full Freeplane document format and visual styling support
- [x] T054 Execute manual end-to-end quickstart validation scenarios defined in `specs/006-support-freeplane-types/quickstart.md`
- [x] T055 Run full project test suite (`./gradlew test`) and static analysis (`./gradlew lint`) ensuring zero failures and clean builds

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately.
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories.
- **User Stories (Phase 3+)**: All depend on Foundational phase completion.
  - User Story 1 (P1) and User Story 2 (P1) can proceed in parallel.
  - User Stories 3, 4, 5, 6 proceed in priority order.
- **Polish (Phase 9)**: Depends on all user stories being complete.

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories.
- **User Story 2 (P1)**: Can start after Foundational (Phase 2) — No dependencies on US1.
- **User Story 3 (P2)**: Can start after Foundational (Phase 2) — Integrates with UI inspector and `MindMapNodeCard`.
- **User Story 4 (P2)**: Can start after Foundational (Phase 2) — Integrates with UI inspector and `MindMapBranchDrawer`.
- **User Story 5 (P3)**: Can start after Foundational (Phase 2) — Integrates with Content and Media tabs.
- **User Story 6 (P3)**: Can start after Foundational (Phase 2) — Completes full round-trip preservation and atomic save.

---

## Parallel Opportunities

- All models in Phase 2 (`T004` to `T009`) can be created concurrently.
- All unit tests across stories (`T018`, `T025`, `T032`, `T038`, `T044`, `T048`) can be created in parallel.
- All Compose tabs (`ContentTab`, `AttributesTab`, `StylingTab`, `EdgesConnectorsTab`, `IconsMediaTab`, `ScriptsTab`) are separate files and can be built in parallel.

---

## Implementation Strategy

### MVP First (User Story 1 Only)
1. Complete Phase 1: Setup (`T001` - `T003`).
2. Complete Phase 2: Foundational (`T004` - `T017`) — CRITICAL blocking step.
3. Complete Phase 3: User Story 1 (`T018` - `T024`).
4. **STOP and VALIDATE**: Verify User Story 1 independently with `RichContentXmlTest` and inspector interaction.
5. Deliver MVP with full rich text, details, and note editing!

### Incremental Delivery
- Add User Story 2: Key-value attributes and icons (`T025` - `T031`).
- Add User Story 3: Visual shapes, colors, and clouds rendering (`T032` - `T037`).
- Add User Story 4: Connectors and custom branch edges rendering (`T038` - `T043`).
- Add User Story 5: External objects and formulas (`T044` - `T047`).
- Add User Story 6: High-fidelity preservation and scripts (`T048` - `T052`).
- Polish & Validation (`T053` - `T055`).
