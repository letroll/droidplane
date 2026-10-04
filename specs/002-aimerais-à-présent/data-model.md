# Phase 1 Data Model: Delete Mindmap Nodes

## 1. Domain Entities (`:data` module)

### `Node` (Extended)
Existing entity with deletion-relevant fields:

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Unique identifier (`ID_` prefix) |
| `parentNode` | `Node?` | Direct parent reference (`null` for root) |
| `childNodes` | `List<Node>` | Ordered descendants |
| `link` | `Uri?` | External URL or internal anchor |
| `arrowLinkDestinationIds` | `List<String>` | Target node IDs for outgoing arrow links |
| `arrowLinkDestinationNodes` | `List<Node>` | Resolved outgoing arrow link nodes |
| `arrowLinkIncomingNodes` | `List<Node>` | Nodes with arrow links pointing to this node |

#### Deletion Behavior
- `deleteNode(nodeId)` removes node from parent's `childNodes`
- Removes `nodeId` from all nodes' `arrowLinkDestinationIds`
- Clears `arrowLinkDestinationNodes` and `arrowLinkIncomingNodes`
- Updates `MindmapIndexes` (both maps)

---

### `MindmapIndexes`
In-memory $O(1)$ lookup cache:

| Field | Type | Description |
|---|---|---|
| `nodesByIdIndex` | `Map<String, Node>` | Lookup by string `id` |
| `nodesByNumericIndex` | `Map<Int, Node>` | Lookup by numeric ID |

#### Deletion Updates
- `nodesByIdIndex.remove(nodeId)`
- `nodesByNumericIndex.remove(numericId)`

---

### `DeleteSnapshot` (New)
Captures state for undo functionality:

| Field | Type | Description |
|---|---|---|
| `deletedNodeId` | `String` | ID of root deleted node |
| `deletedSubtree` | `List<Node>` | All nodes in deleted subtree (deep copy) |
| `parentNodeId` | `String` | Parent node ID |
| `parentChildIndex` | `Int` | Position in parent's `childNodes` before deletion |
| `timestamp` | `Long` | Creation time for ordering |

#### Validation Rules
- `deletedSubtree` MUST include all descendants recursively
- `parentChildIndex` MUST be valid index at time of deletion

---

## 2. UI State Entities (`:app` module)

### `MainUiState` (Extended)

| Field | Type | Default | Description |
|---|---|---|---|
| `dialogUiState` | `DialogUiState` | `DialogUiState()` | Includes new `DeleteConfirmation` variant |

---

### `DialogUiState` (Extended)

```kotlin
sealed interface DialogType {
    data object None : DialogType
    data class EditNodeDescription(val node: Node, val oldValue: String) : DialogType
    data class AddChildNode(val parentNode: Node) : DialogType
    data class DeleteConfirmation(
        val node: Node,
        val descendantCount: Int,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit
    ) : DialogType
}
```

---

## 3. State Transitions

```
[Delete Initiated]
       │
       ▼
[Node has children?] ──No──► [Execute Deletion]
       │
      Yes
       ▼
[Show DeleteConfirmation Dialog]
       │
       ├─ Cancel ──► [Dismiss, No Action]
       │
       ▼
    Confirm
       ▼
[Capture DeleteSnapshot]
       ▼
[NodeManager.deleteNode(nodeId)]
       ▼
[Update UI: showNode(parent)]
       ▼
[Push Snapshot to Undo Stack]

[Undo Requested (Ctrl+Z / Menu)]
       ▼
[Pop Snapshot]
       ▼
[NodeManager.restoreSubtree(snapshot)]
       ▼
[Update UI: showNode(restoredNode)]
```

---

## 4. Key Relationships

- **Node → Parent**: Single, nullable (root has none)
- **Node → Children**: One-to-many, ordered
- **Node → Arrow Links**: Many-to-many (via ID lists, resolved to node refs at load)
- **DeleteSnapshot → Node**: One-to-many (subtree)
- **Undo Stack**: LIFO, single active snapshot per session