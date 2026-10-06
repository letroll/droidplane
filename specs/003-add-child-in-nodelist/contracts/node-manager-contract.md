# Contract: NodeManager Interface

- **Module**: `:data`
- **Interface**: `fr.julien.quievreux.droidplane2.data.NodeManagerContract`

## Methods

```kotlin
/**
 * Adds a new node with [newValue] text to the mindmap under [parentNode].
 *
 * If [parentNode] is null, this node becomes a root candidate.
 * If [parentNode] is non-null, the node is appended to [parentNode.childNodes],
 * and the updated [parentNode] is propagated up to [rootNode].
 *
 * @param newValue Non-blank text for the new node. If blank, returns null.
 * @param parentNode The direct parent node to attach to.
 * @return The generated integer numericId, or null if invalid.
 */
suspend fun addNodeToMindmap(newValue: String, parentNode: Node?): Int?
```
