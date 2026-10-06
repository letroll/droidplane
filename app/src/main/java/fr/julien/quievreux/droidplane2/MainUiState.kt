package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContentNodeType
import fr.julien.quievreux.droidplane2.model.DisplayMode
import fr.julien.quievreux.droidplane2.model.ViewIntentNode

data class MainUiState(
    val title: String = "",
    val loading: Boolean = false,
    val leaving: Boolean = false,
    val canGoBack: Boolean = false,
    val nodeCurrentlyDisplayed: Node? = null,
    val error: String = "",
    val errorAction: ErrorAction? = null,
    val snackbarMessage: SnackbarMessage? = null,
    val canUndoDelete: Boolean = false,
    val viewIntentNode: ViewIntentNode? = null,
    val contentNodeType: ContentNodeType = Classic,
    val searchUiState: SearchUiState = SearchUiState(),
    val dialogUiState: DialogUiState = DialogUiState(),
    val navigationStack: List<String> = emptyList(),
    val displayMode: DisplayMode = DisplayMode.MIND_MAP,
    val selectedNodeId: String? = null,
    val collapsedNodeIds: Set<String> = emptySet(),
    val treeVersion: Long = 0L,
) {
    data class SnackbarMessage(
        val message: String,
        val actionLabel: Int? = null,
        val onAction: (() -> Unit)? = null,
        val eventId: Long = System.currentTimeMillis(),
    )
    data class ErrorAction(
        val actionLabel: Int,
        val action: () -> Unit,
    )
    data class SearchUiState(
        val isSearchActive: Boolean = false,
        val searchQuery: String = "",
        val currentResultIndex: Int = 0,
        val totalResults: Int = 0,
    )

    data class DialogUiState(
        val dialogType: DialogType = DialogType.None
    )

    sealed class DialogType{
        data object None:DialogType()
        data class EditNodeDescription(
            val node: Node,
            val oldValue: String,
        ):DialogType()

        data class AddChildNode(
            val parentNode: Node,
        ):DialogType()

        data class DeleteConfirmation(
            val node: Node,
            val descendantCount: Int,
            val onConfirm: () -> Unit,
            val onCancel: () -> Unit,
        ):DialogType()

    data class ExitConfirmation(
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit,
    ):DialogType()
}
}