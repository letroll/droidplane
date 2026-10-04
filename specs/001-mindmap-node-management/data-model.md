# Phase 1 Data Model: Mindmap Viewing & Node Management

## 1. Domain Entities (`:data` module)

### `Node`
Represents an individual thought, note, or entry within the hierarchical mindmap structure.

| Field | Type | Required | Description |
|---|---|---|---|
| `id` | `String` | Yes | Unique node identifier prefixed with `ID_` (e.g. `ID_1049281`). |
| `numericId` | `Int` | Yes | Integer portion of the node ID used for rapid numeric lookup. |
| `text` | `String?` | No | Plain text representation of the node content. |
| `parentNode` | `Node?` | No | Direct reference to parent node (`null` for the root node). |
| `childNodes` | `List<Node>` | Yes | Ordered collection of direct descendants. |
| `link` | `Uri?` | No | External URL or internal anchor fragment (`#ID_...`). |
| `treeIdAttribute` | `String?` | No | Target node ID if this node is a Freeplane clone node. |
| `richTextContents`| `List<String>`| Yes | Raw HTML rich text content if node uses formatted text. |
| `richContentType` | `RichContentType?` | No | Enum indicating rich content variant (`NODE`, `NOTE`, etc.). |
| `iconNames` | `List<String>`| Yes | Names of Freeplane built-in icons assigned to this node. |
| `creationDate` | `Long?` | No | Millisecond timestamp when node was created. |
| `modificationDate`| `Long?` | No | Millisecond timestamp when node was last edited. |
| `isBold` | `Boolean` | Yes | Typography flag: whether text is rendered in bold. |
| `isItalic` | `Boolean` | Yes | Typography flag: whether text is rendered in italics. |
| `position` | `String?` | No | Layout side indicator (`left`, `right`) used by root branches. |
| `arrowLinkDestinationIds` | `List<String>` | Yes | IDs of target nodes connected via visual arrows. |

#### Validation Rules
- `id` MUST NOT be blank and MUST follow the Freeplane XML ID pattern (`ID_[0-9]+`).
- When modifying `text`, `modificationDate` MUST be updated to `System.currentTimeMillis()`.
- Newly added child nodes MUST have non-empty text, a unique `id`, and inherit the parent's ID in `parentNode`.

---

### `MindmapIndexes`
An in-memory resolution cache providing fast $O(1)$ lookup for all nodes in the document.

| Field | Type | Description |
|---|---|---|
| `nodesByIdIndex` | `Map<String, Node>` | Lookup map indexing each node by its string `id`. |
| `nodesByNumericIndex` | `Map<Int, Node>` | Lookup map indexing each node by its `numericId`. |

---

## 2. UI State Entities (`:app` module)

### `MainUiState`
Immutable snapshot of the primary presentation screen.

| Field | Type | Default | Description |
|---|---|---|---|
| `loading` | `Boolean` | `false` | Indicates whether mindmap I/O or parsing is ongoing. |
| `title` | `String` | `""` | Title displayed in the primary top bar (active node name). |
| `nodeCurrentlyDisplayed` | `Node?` | `null` | The active node whose children are listed on screen. |
| `navigationStack` | `List<String>` | `emptyList()` | Breadcrumb history of visited node IDs for back navigation. |
| `canGoBack` | `Boolean` | `false` | True when navigation stack depth > 1 or parent exists. |
| `error` | `String` | `""` | User-facing error message, empty if no error. |
| `searchUiState` | `SearchUiState` | `SearchUiState()` | Sub-state controlling search toolbar and results. |
| `dialogUiState` | `DialogUiState` | `DialogUiState()` | Sub-state controlling active dialogs (edit, add node). |
| `viewIntentNode` | `ViewIntentNode?`| `null` | External link or file intent ready for OS dispatch. |
| `contentNodeType` | `ContentNodeType`| `Classic` | View rendering mode (`Classic`, `RelativeFile`). |

---

### `SearchUiState`
Represents the active search state inside the top bar.

| Field | Type | Default | Description |
|---|---|---|---|
| `isSearchActive` | `Boolean` | `false` | True when top bar is switched to search mode. |
| `searchQuery` | `String` | `""` | Current search query entered by the user. |
| `currentResultIndex` | `Int` | `0` | 0-based index of the currently highlighted result. |
| `totalResults` | `Int` | `0` | Total count of matching nodes found. |

---

### `DialogUiState`
Controls modal overlays displayed over the mindmap view.

```kotlin
sealed interface DialogType {
    data object None : DialogType
    data class EditNodeDescription(val node: Node, val oldValue: String) : DialogType
    data class AddChildNode(val parentNode: Node) : DialogType
}
```

---

## 3. State Transitions

```
[Start]
   │
   ▼
Load Mindmap XML ──► [Populate MindmapIndexes & RootNode]
                            │
                            ▼
                     [Display Root Node]
                            │
         ┌──────────────────┼──────────────────┐
         │ (Tap Child)      │ (Search Match)   │ (Edit / Add)
         ▼                  ▼                  ▼
[Navigate to Child]  [Jump to Match]    [NodeManager Mutation]
         │                  │                  │
         │ (Navigate Up)    │ (Exit Search)    ▼
         ▼                  ▼           [Update Indexes &]
[Navigate to Parent] [Restore View]     [Sync All Views ]
```

### Mutation Flow (Node Editing)
1. User clicks **Edit** from context menu on a node.
2. `MainViewModel` emits `DialogType.EditNodeDescription(node, oldValue)`.
3. User edits text and submits.
4. `MainViewModel` calls `NodeManager.updateNodeText(nodeId, newText)`.
5. `NodeManager`:
   - Updates target node text and `modificationDate`.
   - Updates target node in its parent's `childNodes` collection.
   - Refreshes `MindmapIndexes` (`nodesByIdIndex` and `nodesByNumericIndex`).
   - Emits updated node stream via `_allNodes`.
6. `MainViewModel` reloads the active displayed node from `NodeManager.getNodeByID()`.
7. UI updates synchronously: both active view and child listings display updated text without desynchronization.
