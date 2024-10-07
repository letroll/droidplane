package ch.benediktkoeppel.code.droidplane.model

import android.net.Uri
import androidx.lifecycle.ViewModel

/**
 * Mindmap handles the loading and storing of a mind map document.
 */
class Mindmap : ViewModel() {
    /**
     * Returns the Uri which is currently loaded in document.
     *
     * @return Uri
     */
    /**
     * Set the Uri after loading a new document.
     *
     * @param uri
     */
    /**
     * The currently loaded Uri
     */
    @JvmField var uri: Uri? = null

    /**
     * Returns the root node of the currently loaded mind map
     *
     * @return the root node
     */
    /**
     * The root node of the document.
     */
    @JvmField var rootNode: MindmapNode? = null

    /**
     * A map that resolves node IDs to Node objects
     */
    @JvmField var mindmapIndexes: MindmapIndexes? = null

    // whether the mindmap has finished loading
    @JvmField var isLoaded: Boolean = false

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    fun getNodeByID(id: String?): MindmapNode? = mindmapIndexes?.nodesByIdIndex?.get(id)

    fun getNodeByNumericID(numericId: Int?): MindmapNode? = mindmapIndexes?.nodesByNumericIndex?.get(numericId)
}
