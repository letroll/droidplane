# Phase 1 Data Model: Add Child Node to a Node in NodeList

- **Feature**: Add Child Node to a Node in NodeList
- **Branch**: `003-add-child-in-nodelist`
- **Date**: 2026-10-06

## Entities & Relationships

### 1. `Node` (Domain Entity in `:data`)
- **Package**: `fr.julien.quievreux.droidplane2.data.model.Node`
- **Attributes**:
  - `id: String`: Unique ID in the Freeplane format (e.g. `"ID_1042"`).
  - `numericId: Int`: Numeric key for fast integer indexing.
  - `text: String?`: Node label or title text.
  - `parentNode: Node?`: Reference to the direct parent node. `null` for root.
  - `childNodes: MutableList<Node>`: Direct children of this node.
  - `creationDate: Long?`: Unix timestamp when created.
  - `modificationDate: Long?`: Unix timestamp when last edited.
- **Relationships**:
  - `Target Node (parent)` 1 ── * `New Child Node (child)`
  - `Current Screen Node` 1 ── * `Target Node` (when target is in the displayed list)
  - All nodes form a directed tree rooted at `rootNode`.

### 2. `MainUiState.DialogType.AddChildNode` (UI State Model in `:app`)
- **Package**: `fr.julien.quievreux.droidplane2.MainUiState.DialogType`
- **Attributes**:
  - `parentNode: Node`: The node to which the new child will be appended.
- **Usage**:
  - When triggered from top-level FAB: `parentNode = currentState.nodeCurrentlyDisplayed`.
  - When triggered from list item context menu: `parentNode = clickedItemNode`.

## State Transitions

```text
[NodeList Displayed]
        │
        ├─ User opens context menu on child item & clicks "Add child node"
        ▼
[DialogState: AddChildNode(parentNode = selectedItem)]
        │
        ├─ User enters text & clicks Confirm
        ▼
[NodeManager: addNodeToMindmap(text, selectedItem)]
        │
        ├─ New node appended to selectedItem.childNodes
        ├─ SelectedItem updated in parentNode.childNodes (up to rootNode)
        ├─ Indexes & StateFlow updated
        ▼
[UI Refreshed: Current NodeList remains active; selectedItem displays expand icon]
```
