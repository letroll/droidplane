package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.MindmapNode

data class MainUiState(
    val title: String = "",
    val defaultTitle: String = "",
    val lastSearchString: String = "",
    val currentSearchResultIndex: Int = 0,
    val loading: Boolean = true,
    val leaving: Boolean = false,
    val canGoBack: Boolean = false,
    val rootNode: MindmapNode? = null,
    val selectedNode: MindmapNode? = null,
    val error: String = "",
    val errorAction: ErrorAction? = null,
    val viewIntentNode: ViewIntentNode? = null,
    val contentNodeType: ContentNodeType = Classic,
) {
    data class ErrorAction(val actionLabel:Int,val action:()->Unit)
}