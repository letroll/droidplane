package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.data.model.Node

data class MainUiState(
    val title: String = "",
    val defaultTitle: String = "",
    val loading: Boolean = false,
    val leaving: Boolean = false,
    val canGoBack: Boolean = false,
    val rootNode: Node? = null,
    val error: String = "",
    val errorAction: ErrorAction? = null,
    val viewIntentNode: ViewIntentNode? = null,
    val contentNodeType: ContentNodeType = Classic,
    val searchUiState: SearchUiState = SearchUiState(),
    val dialogUiState: DialogUiState = DialogUiState(),
) {
    data class ErrorAction(
        val actionLabel: Int,
        val action: () -> Unit,
    )
    data class SearchUiState(
        val lastSearchString: String = "",
        val currentSearchResultIndex: Int = 0,
    )

    data class DialogUiState(
        val dialogType: DialogType = DialogType.None
    )

    sealed class DialogType{
        data object None:DialogType()
        data class Edit(
            val node: Node,
            val oldValue: String,
        ):DialogType()
    }

}