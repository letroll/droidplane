package fr.julien.quievreux.droidplane2.data.model

/**
 * Captures state for undo functionality when a node is deleted.
 * Contains all necessary information to restore a deleted subtree.
 */
data class DeleteSnapshot(
    /** ID of the root deleted node */
    val deletedNodeId: String,
    /** All nodes in the deleted subtree (deep copy) */
    val deletedSubtree: List<Node>,
    /** Parent node ID where the deleted node was attached */
    val parentNodeId: String,
    /** Position in parent's childNodes before deletion */
    val parentChildIndex: Int,
    /** Creation time for ordering */
    val timestamp: Long = System.currentTimeMillis()
) {

    /**
     * Creates a deep copy of the deleted subtree for safe restoration
     */
    fun copySubtree(): List<Node> = deletedSubtree.map { it.copy() }

    /**
     * Gets the root node of the deleted subtree
     */
    val rootDeletedNode: Node?
        get() = deletedSubtree.firstOrNull { it.id == deletedNodeId }

    /**
     * Total number of nodes in the deleted subtree (including root)
     */
    val totalNodesDeleted: Int
        get() = deletedSubtree.size

    companion object {
        /**
         * Creates a DeleteSnapshot from a node to be deleted
         */
        fun create(node: Node, parentNode: Node, parentChildIndex: Int): DeleteSnapshot {
            val subtree = mutableListOf<Node>()
            
            // Recursively collect all nodes in the subtree
            fun collectSubtree(node: Node) {
                subtree.add(node.copy())
                for (child in node.childNodes) {
                    collectSubtree(child)
                }
            }
            
            collectSubtree(node)
            
            return DeleteSnapshot(
                deletedNodeId = node.id,
                deletedSubtree = subtree,
                parentNodeId = parentNode.id,
                parentChildIndex = parentChildIndex,
                timestamp = System.currentTimeMillis()
            )
        }
    }
}
