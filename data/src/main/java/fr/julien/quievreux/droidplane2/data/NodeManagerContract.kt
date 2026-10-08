package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.DeleteSnapshot
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

    /**
     * Initializes a fresh empty mindmap in memory with a single root node.
     * Clears previous index structures, resets search, and emits the new root to [allNodes].
     */
    fun createNewMindmap(rootTitle: String = "Central Idea"): Node

    /**
     * Loads a Freeplane XML (.mm) input stream into the in-memory tree and builds indexes.
     */
    suspend fun loadMindMapFromInputStream(
        inputStream: InputStream,
        onError: (Exception) -> Unit,
        onParentNodeUpdate: (Node) -> Unit,
        onLoadFinished: (() -> Unit)? = null,
    )

    /**
     * Updates an existing node's text and modification timestamp.
     * Synchronously updates the node in parent collections and in-memory indexes.
     * Returns the updated Node, or null if node not found.
     */
    suspend fun updateNodeText(nodeId: String, newText: String): Node?

    /**
     * Updates an entire node with new properties (text, details, notes,
     * attributes, visual styling, cloud, edge, connectors, etc.),
     * updating internal indexes and propagating changes through ancestors.
     * Returns true if node was found and updated, false otherwise.
     */
    suspend fun updateNode(updatedNode: Node): Boolean

    /**
     * Creates and attaches a new child node to parentNode with the provided text.
     * Returns the numeric ID of the newly created node, or null if creation failed.
     */
    suspend fun addNodeToMindmap(newValue: String, parentNode: Node? = null): Int?

    /**
     * Deletes a node by its ID from the tree and indexes.
     * Returns true if node was found and deleted, false otherwise.
     */
    suspend fun deleteNode(nodeId: String): Boolean

    /**
     * Creates a snapshot of the node and its subtree for undo functionality.
     * Returns a DeleteSnapshot containing the node and all its descendants,
     * or null if the node is not found.
     */
    suspend fun createDeleteSnapshot(nodeId: String): DeleteSnapshot?

    /**
     * Restores a previously deleted subtree from a DeleteSnapshot.
     * Re-inserts nodes into the hierarchy at the original position.
     * Rebuilds link references and updates indexes.
     * Returns true if restore succeeded.
     */
    suspend fun restoreSubtree(snapshot: DeleteSnapshot): Boolean

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
        onSaveFinished: ((java.io.File) -> Unit)? = null,
    )
}
