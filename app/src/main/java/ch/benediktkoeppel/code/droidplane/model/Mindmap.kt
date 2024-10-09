package ch.benediktkoeppel.code.droidplane.model

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Mindmap handles the loading and storing of a mind map document.
 */
class Mindmap : ViewModel() {
   data class State(val loading:Boolean){
       internal companion object{
           internal fun defaults() = State(
               loading = true
           )
       }
   }
    private val _state: MutableStateFlow<State> = MutableStateFlow(State.defaults())
    public val state: StateFlow<State> = _state

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

    fun loadMindMap(isAnExternalMindMapEdit: Boolean) {
        viewModelScope.launch {
           _state.update {
               it.copy(
                   loading = true
               )
           }
        }
    }
}

