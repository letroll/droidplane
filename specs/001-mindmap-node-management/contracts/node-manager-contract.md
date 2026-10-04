# Interface Contract: `NodeManager` (`:data` Module)

## Purpose
`NodeManager` is the single source of truth for all mindmap operations in Droidplane. It manages the document tree, XML parsing/serialization, in-memory indexes, search, and node CRUD mutations.

## Public Interface Contract

```kotlin
package fr.julien.quievreux.droidplane2.data

import android.net.Uri
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.io.InputStream

interface NodeManagerContract {

    /** The root node of the active mindmap document. */
    val rootNode: Node?

    /** Reactive stream of all active nodes in the document. */
    val allNodes: StateFlow<List<Node>>

    /** Rapid O(1) lookup by string node ID (e.g. "ID_12345"). */
    fun getNodeByID(id: String?): Node?

    /** Rapid O(1) lookup by numeric ID. */
    fun getNodeByNumericId(nodeId: Int): Node?

    /** Returns direct parent of the given child node ID. */
    fun getNodeParent(childNodeId: Int): Node?

    /** Returns human-readable text for a node, resolving clones and rich-text HTML if present. */
    fun getNodeText(node: Node): String?

    /** Returns full text content for copying to clipboard, including raw HTML for rich text nodes. */
    fun getNodeTextForCopy(node: Node): String?

    /**
     * Loads a Freeplane XML (.mm) input stream into the in-memory tree and builds indexes.
     */
    suspend fun loadMindMapFromInputStream(
        inputStream: InputStream,
        onError: (Exception) -> Unit,
        onParentNodeUpdate: (Node) -> Unit,
        onLoadFinished: (() -> Unit)? = null
    )

    /**
     * Updates an existing node's text and modification timestamp.
     * Synchronously updates the node in parent collections and in-memory indexes.
     * Returns the updated Node, or null if node not found.
     */
    suspend fun updateNodeText(nodeId: String, newText: String): Node?

    /**
     * Creates and attaches a new child node to parentNode with the provided text.
     * Returns the numeric ID of the newly created node, or null if creation failed.
     */
    suspend fun addNodeToMindmap(newValue: String, parentNode: Node? = null): Int?

    /**
     * Deletes a node by its ID from the tree and indexes.
     */
    suspend fun deleteNode(nodeId: String): Boolean

    /**
     * Initiates a full-text search across all nodes.
     */
    fun search(query: String, onResultFound: () -> Unit)

    /** Returns current search match results. */
    fun getSearchResult(): List<Node>

    /** Returns count of search matches. */
    fun getSearchResultCount(): Int

    /**
     * Atomically serializes the active mindmap tree back to Freeplane XML format.
     */
    suspend fun serializeMindmap(
        filePath: String,
        filename: String,
        onError: (Exception) -> Unit,
        onSaveFinished: ((File) -> Unit)? = null
    )
}
```

## Invariants & Guarantees
1. **Thread Safety**: All mutations occur within controlled coroutines dispatching on `Dispatchers.IO` or `Dispatchers.Default`.
2. **Index Consistency**: At no point may `mindmapIndexes` disagree with the node hierarchy in `rootNode`. Any edit to a node automatically updates its parent's child list and the index maps.
3. **Lossless Round-tripping**: Calling `serializeMindmap` after loading an unmodified `.mm` file MUST produce an equivalent XML document preserving all tags and attributes.
