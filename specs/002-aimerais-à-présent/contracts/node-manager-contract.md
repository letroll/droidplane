# Interface Contract: `NodeManager` Extension for Deletion (`:data` Module)

## Purpose
Extends the existing `NodeManagerContract` with deletion-specific operations including recursive deletion, link cleanup, and undo support.

## Additional Interface Contract

```kotlin
package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.Node
import kotlinx.coroutines.flow.StateFlow

interface NodeManagerDeletionContract {

    /**
     * Deletes a node and all its descendants from the mindmap.
     * Cleans up all link references (external link, arrow links).
     * Updates in-memory indexes.
     * @return true if node was found and deleted, false otherwise
     */
    suspend fun deleteNode(nodeId: String): Boolean

    /**
     * Restores a previously deleted subtree from a deletion snapshot.
     * Re-inserts nodes into hierarchy at original position.
     * Rebuilds link references and updates indexes.
     * @return true if restore succeeded
     */
    suspend fun restoreSubtree(snapshot: DeleteSnapshot): Boolean

    /**
     * Creates a deep copy snapshot of a node and all its descendants
     * for undo purposes.
     */
    fun createDeleteSnapshot(nodeId: String): DeleteSnapshot?

    /**
     * Reactive stream of all active nodes (includes after deletion/restore).
     */
    val allNodes: StateFlow<List<Node>>
}
```

## `DeleteSnapshot` Data Class

```kotlin
package fr.julien.quievreux.droidplane2.data.model

data class DeleteSnapshot(
    val deletedNodeId: String,
    val deletedSubtree: List<Node>,        // Deep copy of all deleted nodes
    val parentNodeId: String,              // Parent node ID
    val parentChildIndex: Int,             // Original index in parent's childNodes
    val timestamp: Long = System.currentTimeMillis()
)
```

## Invariants & Guarantees

1. **Recursive Deletion**: `deleteNode` removes the entire subtree rooted at `nodeId`.
2. **Link Cleanup**: All `link`, `arrowLinkDestinationIds`, `arrowLinkDestinationNodes`, `arrowLinkIncomingNodes` references to/from deleted nodes are removed.
3. **Index Consistency**: After deletion, `nodesByIdIndex` and `nodesByNumericIndex` do not contain any deleted node IDs.
4. **Parent Integrity**: Parent's `childNodes` list no longer contains deleted node; indices of subsequent siblings shift.
5. **Undo Fidelity**: `restoreSubtree` recreates exact original structure (node IDs, order, links, timestamps).
6. **Thread Safety**: All operations on `Dispatchers.IO` or `Dispatchers.Default`; StateFlow emissions on main dispatcher.
7. **Root Protection**: `deleteNode(rootId)` returns `false` without mutation (enforced by caller).