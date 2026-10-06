# Contract: ViewModel & State Management

- **Module**: `:app`
- **Classes**: `MainViewModel`, `MainUiState`

## MainViewModel Interface Additions

```kotlin
package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.DisplayMode

class MainViewModel(...) {

    /**
     * Toggles between [DisplayMode.LIST] and [DisplayMode.MIND_MAP].
     * Synchronizes the active node context so orientation is preserved.
     */
    fun toggleDisplayMode()

    /**
     * Sets the active display mode explicitly.
     */
    fun setDisplayMode(mode: DisplayMode)

    /**
     * Updates the currently selected node on the mindmap canvas.
     */
    fun selectNode(node: Node)

    /**
     * Toggles the collapsed/expanded state of a branch for [node].
     */
    fun toggleNodeCollapse(node: Node)

    /**
     * Clears any active node selection.
     */
    fun clearNodeSelection()

    /**
     * Returns the root node of the active mindmap, if loaded.
     */
    fun getRootNode(): Node?
}
```

## Behavior Specification

1. **`toggleDisplayMode()`**:
   - If current mode is `DisplayMode.LIST`, updates state to `DisplayMode.MIND_MAP`.
   - If current mode is `DisplayMode.MIND_MAP`, updates state to `DisplayMode.LIST`.
   - If `selectedNodeId` is non-null when switching from `MIND_MAP` to `LIST`, updates `nodeCurrentlyDisplayed` to the selected node (or its parent if the selected node has no children) and synchronizes `navigationStack`.
   - In-memory document and unsaved changes state (`hasUnsavedChangesState`) remain completely unaffected.

2. **`selectNode(node: Node)`**:
   - Updates `_uiState.value = _uiState.value.copy(selectedNodeId = node.id)`.

3. **`toggleNodeCollapse(node: Node)`**:
   - If `node.id` is in `collapsedNodeIds`, removes it from the set (expanding the branch).
   - If `node.id` is not in `collapsedNodeIds`, adds it to the set (collapsing the branch).
   - Subtree layout updates immediately on the next composition pass.
