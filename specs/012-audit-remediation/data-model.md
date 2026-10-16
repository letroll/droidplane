# Data Model: Codebase Audit Remediation & Quality Hardening (`012-audit-remediation`)

**Date**: 2026-10-16  
**Feature**: [spec.md](spec.md)

## Entities & State Models

### 1. `Node` (`:data` — `fr.julien.quievreux.droidplane2.data.model.Node`)

Represents a single topic in the Freeplane mind map hierarchy.

#### Fields Included in `equals` / `hashCode`
To guarantee Jetpack Compose recomposition whenever any visual or structural property changes (**FR-010**), `equals` and `hashCode` MUST evaluate all of the following fields (excluding `parentNode` direct object recursion to avoid stack overflow, using `parentNode?.id` instead):

| Field | Type | Role | Previously in `equals`/`hashCode`? |
|-------|------|------|------------------------------------|
| `id` | `String` | Unique Freeplane node ID (`ID_...`) | Yes |
| `numericId` | `Int` | Numeric suffix of node ID | Yes |
| `parentNode?.id` | `String?` | Parent node identifier | No (added for structural accuracy) |
| `text` | `String?` | Plain text content | Yes |
| `richText` | `String?` | HTML body for `TYPE="NODE"` | Yes |
| `detailsText` | `String?` | HTML body for `TYPE="DETAILS"` | Yes |
| `noteText` | `String?` | HTML body for `TYPE="NOTE"` | Yes |
| `position` | `String?` | Branch side (`"left"` / `"right"`) | Yes |
| `creationDate` | `Long?` | Creation timestamp (ms) | Yes |
| `modificationDate` | `Long?` | Last modification timestamp (ms) | Yes |
| `link` | `Uri?` | Hyperlink or relative file URI | Yes |
| `isFolded` | `Boolean` | Whether children are folded in `.mm` | **No -> Added** |
| `color` | `Int?` | Foreground text color (`COLOR`) | Yes |
| `backgroundColor` | `Int?` | Node fill color (`BACKGROUND_COLOR`) | Yes |
| `fontSize` | `Int?` | Custom font size (`<font SIZE="..."/>`) | **No -> Added** |
| `fontName` | `String?` | Custom font family (`<font NAME="..."/>`) | **No -> Added** |
| `isBold` | `Boolean` | Bold font style (`BOLD="true"`) | **No -> Added** |
| `isItalic` | `Boolean` | Italic font style (`ITALIC="true"`) | **No -> Added** |
| `cloud` | `NodeCloud?` | Cloud enclosure metadata | Yes |
| `edge` | `NodeEdge?` | Connector edge styling | Yes |
| `latexEquation` | `String?` | LaTeX formula (`<hook EQUATION="..."/>`) | **No -> Added** |
| `externalObject` | `ExternalObject?` | Attached external image/object | **No -> Added** |
| `childNodes` | `MutableList<Node>` | Ordered child nodes | Yes |
| `attributes` | `MutableList<NodeAttributePair>` | Key-value node attributes | Yes |
| `iconNames` | `MutableList<String>` | Attached Freeplane icon names | Yes |
| `arrowLinks` | `MutableList<ArrowLink>` | Cross-branch arrow links | Yes |

#### Tree Invariants
- **Deep Parent Reference Invariant (`FR-011`)**: For every node $P$ in the tree and every child $C \in P.\text{childNodes}$, $C.\text{parentNode} === P$ MUST hold recursively across all depths after any call to `propagateUpdatedParentToRoot`.
- **Single-Emission Parse Invariant (`FR-012`)**: During `loadMindMapFromXml`, `_allNodes` is not mutated per tag; all parsed nodes are collected locally and emitted once to `_allNodes.value` when parsing finishes.

---

### 2. `MainUiState` (`:app` — `fr.julien.quievreux.droidplane2.ui.state.MainUiState`)

Represents the single source of truth for the main screen UI state (**FR-006**, **FR-009**).

#### Added / Updated State Fields
| Field | Type | Default | Description |
|-------|------|---------|-------------|
| `pendingSaveTempFile` | `File?` | `null` | One-shot event holding the staged temporary `.mm` file ready to be written either to an existing `targetSaveUri` or via the system SAF document creator (`saveFileLauncher`). Replaces the leaked `FileRegister` Activity reference. |
| `leaving` | `Boolean` | `false` | Signals that the user requested to exit the screen; consumed inside `LaunchedEffect(state.value.leaving)` rather than directly inside composition. |

#### State Transitions for Saving
1. User triggers Save -> `MainViewModel.launchSaveFile(filesDirPath: String)` serializes mind map on `Dispatchers.IO` to `<filesDirPath>/temp_<filename>.mm` and atomically renames it to `<filesDirPath>/<filename>.mm`.
2. On serialization success -> `MainViewModel` updates `MainUiState(pendingSaveTempFile = savedFile)`.
3. `MainActivity` observes `pendingSaveTempFile != null` in `LaunchedEffect`:
   - Calls `viewModel.clearPendingSaveTempFile()`.
   - If `SaveTargetResolver` resolves `OverwriteExisting(uri)`: launches `lifecycleScope.launch(Dispatchers.IO)` to stream `savedFile.inputStream().use { it.copyTo(outputStream) }` and updates save feedback on `Dispatchers.Main`.
   - If `CreateNewDocument`: stores `savedFile` as the pending source file and launches `saveFileLauncher.launch(filename)`.

---

### 3. `MindMapLayout` & `MindMapNodeLayout` (`:app` — `fr.julien.quievreux.droidplane2.ui.mindmap`)

Represents the 2D spatial layout of the mind map canvas (**FR-013**).

- **Layout Memoization**: `MindMapLayoutEngine.computeLayout` maintains per-pass caches:
  - `subtreeHeightCache: MutableMap<String, Float>` keyed by `node.id`
  - `hasCloudCache: MutableMap<String, Boolean>` keyed by `node.id`
- **Selection Decoupling**: `MindMapCanvasScreen` computes `layout` inside `remember(rootNode, treeVersion, collapsedNodeIds, density.density)` (excluding `selectedNodeId`), and evaluates `isSelected = (nodeLayout.node.id == selectedNodeId)` when rendering each `MindMapNodeCard`.

---

### 4. Removed Obsolete Models & Files (**FR-016**)

The following unused classes and layout resources are removed from the codebase:
- `app/src/main/java/fr/julien/quievreux/droidplane2/ui/model/Node.kt` (duplicate unused UI Node model)
- `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapViewModel.kt`
- `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapUiState.kt`
- `app/src/main/java/fr/julien/quievreux/droidplane2/ui/EditDescriptionViewModel.kt`
- `app/src/main/java/fr/julien/quievreux/droidplane2/model/FileRegister.kt`
- `core/src/main/java/fr/julien/quievreux/droidplane2/core/PermissionUtils.kt`
- `app/src/main/res/layout/activity_main.xml`
- `app/src/main/res/layout/dialog_edit_description.xml`
