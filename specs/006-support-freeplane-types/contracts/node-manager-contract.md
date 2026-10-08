# NodeManager Contract: Freeplane Types Support & Mutation

**Feature Branch**: `006-support-freeplane-types`  
**Feature Directory**: `specs/006-support-freeplane-types/contracts`  
**Status**: Completed  

## Overview

This contract defines the public methods, guarantees, and state transitions of `NodeManagerContract` in the `:data` module for accessing and updating all Freeplane node elements, attributes, styles, and annotations.

---

## Interface Definition (`NodeManagerContract`)

```kotlin
package fr.julien.quievreux.droidplane2.data

import android.net.Uri
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttributeEntry
import fr.julien.quievreux.droidplane2.data.model.CloudProperties
import fr.julien.quievreux.droidplane2.data.model.EdgeProperties
import fr.julien.quievreux.droidplane2.data.model.ConnectorLink
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import kotlinx.coroutines.flow.StateFlow
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.io.OutputStream

interface NodeManagerContract {

    val allNodes: StateFlow<List<Node>>
    val rootNode: Node?

    // Node Lookup
    fun getNodeByID(id: String?): Node?
    fun getNodeByNumericId(nodeId: Int): Node?
    fun getNodeParent(childNodeId: Int): Node?
    fun getNodeText(node: Node?): String?

    // Node Mutations
    suspend fun updateNodeText(node: Node, newText: String)

    /**
     * Updates an entire node with new properties (text, details, notes,
     * attributes, visual styling, cloud, edge, connectors, etc.),
     * updating internal indexes and propagating changes through ancestors.
     */
    suspend fun updateNode(updatedNode: Node): Boolean

    suspend fun addNodeToMindmap(newValue: String, parentNode: Node?): Int?
    suspend fun deleteNode(node: Node): Boolean

    // Persistence & Lifecycle
    suspend fun loadMindMap(inputStream: InputStream): Boolean
    suspend fun loadMindMapFromXml(
        xpp: XmlPullParser,
        onParentNodeUpdate: (Node) -> Unit,
        onError: (Exception) -> Unit,
        onReadFinish: () -> Unit,
    )
    suspend fun serializeMindmap(
        outputStream: OutputStream,
        onError: (Exception) -> Unit,
        onSaveFinished: () -> Unit,
    )
}
```

---

## Method Contract Details

### `updateNode(updatedNode: Node): Boolean`
- **Purpose**: Atomically replace the existing node identified by `updatedNode.id` with the new node state containing modified text, details, note, attributes, visual style, cloud, edge, or connectors.
- **Preconditions**:
  - `updatedNode.id` must exist in `MindmapIndexes.nodesByIdIndex`.
  - Caller provides the updated immutable `Node` instance with refreshed `modificationDate`.
- **Postconditions**:
  - Index map updated: `nodesByIdIndex[updatedNode.id] = updatedNode`, `nodesByNumericIndex[updatedNode.numericId] = updatedNode`.
  - Ancestor references updated: the node's parent has its `childNodes` updated to hold `updatedNode`, repeating up to `rootNode`.
  - `allNodes` StateFlow emits the refreshed list of nodes.
  - Returns `true` on success, `false` if the node ID was not found.

---

## Thread Safety & Dispatching

- All mutation methods (`updateNode`, `addNodeToMindmap`, `deleteNode`, `serializeMindmap`) run on `Dispatchers.Default` or `Dispatchers.IO` to prevent UI thread starvation.
- State is exposed as immutable Kotlin collections and `StateFlow` primitives.
