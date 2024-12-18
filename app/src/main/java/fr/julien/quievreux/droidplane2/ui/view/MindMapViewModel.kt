package fr.julien.quievreux.droidplane2.ui.view

import androidx.lifecycle.ViewModel
import com.mindsync.library.MindMapManager
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.ui.model.Node
import fr.julien.quievreux.droidplane2.ui.view.MindMapUiState.DialogType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MindMapViewModel(
   val logger: Logger,
): ViewModel() {

    private var mindMapManager: MindMapManager? = null

    private var _selectedNode = MutableStateFlow<Node?>(null)
    val selectedNode: StateFlow<Node?> = _selectedNode

    private var _nodeToAdd = MutableStateFlow<String?>(null)
    val nodeToAdd: StateFlow<String?> = _nodeToAdd

    private val _uiState: MutableStateFlow<MindMapUiState> = MutableStateFlow(MindMapUiState())
    val uiState: StateFlow<MindMapUiState> = _uiState

    fun setSelectedNode(selectNode: Node?) {
        logger.e("setSelectedNode $selectNode")
        _selectedNode.value = null
        _selectedNode.value = selectNode
    }

    fun setManager(mindMapManager: MindMapManager) {
        this.mindMapManager = mindMapManager
    }

    fun setDialogState(dialogType: DialogType) {
//        _selectedNode.value = null
//        this._uiState.value = _uiState.value.copy(
//            dialogUiState = MindMapUiState.DialogUiState(
//                dialogType = DialogType.None
//            )
//        )
        this._uiState.value = _uiState.value.copy(
            dialogUiState = MindMapUiState.DialogUiState(
                dialogType = dialogType
            )
        )
    }

    fun addNode(newValue: String?) {
        _nodeToAdd.value = newValue
    }
}
