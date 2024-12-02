package fr.julien.quievreux.droidplane2.data.search

import fr.julien.quievreux.droidplane2.core.async.uiStateIn
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.MindmapNode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update

class SearchManager(
    scope : CoroutineScope,
    private val logger: Logger,
    nodesSource : StateFlow<List<MindmapNode>>,
    private val fetchText: (MindmapNode) -> String?,
) {

    //first state whether the search is happening or not
    private val _isSearching = MutableStateFlow(false)

    //second state the text typed by the user
    private val _searchText = MutableStateFlow("")
    private val searchText = _searchText.asStateFlow()

    private val nodeFindList = getSearchResultStateFlow(nodesSource, scope)

    private fun getSearchResultStateFlow(
        nodesSource: StateFlow<List<MindmapNode>>,
        scope: CoroutineScope,
    ) = searchText
            .combine(nodesSource) { text, nodes ->//combine searchText with _nodeFindList
                if (text.isBlank()) { //return the entery list of nodes if not is typed
                    nodes
                } else {
                    nodes.filter { node ->// filter and return a list of nodes based on the text the user typed
                        fetchText(node)?.contains(text.trim(), ignoreCase = true) == true
                    }//.reversed()
                }
            }.uiStateIn( //basically convert the Flow returned from combine operator to StateFlow
                scope = scope,
                initialValue = nodesSource.value
            )

    fun getResultCount() = nodeFindList.value.size

    fun getSearchResult() = nodeFindList

    fun search(
        query: String,
        onResultFound: () -> Unit,
    ) {
        _searchText.update {
            query
        }
        _isSearching.update {
            query.isNotEmpty()
        }
        logger.e(
            "toto", """
query:$query 
isSearching:${_isSearching.value} 
find:${nodeFindList.value.joinToString(separator = "|", transform = { fetchText(it).orEmpty() })} 
        """.trimIndent()
        )

        if (nodeFindList.value.size == 1) {
            onResultFound()
        }
    }
}