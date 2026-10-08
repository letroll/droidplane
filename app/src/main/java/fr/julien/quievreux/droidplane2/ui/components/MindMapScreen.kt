package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.ui.view.DeleteConfirmationDialog

@Composable
fun MindMapScreen(viewModel: MainViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsState()

    val nodeCurrentlyDisplayed = uiState.nodeCurrentlyDisplayed

    // Show DeleteConfirmationDialog when dialog state is DeleteConfirmation
    uiState.dialogUiState.dialogType.let {
        if (it is DeleteConfirmation) {
            DeleteConfirmationDialog(confirmation = it)
        }
    }

    nodeCurrentlyDisplayed?.let { node ->
        NodeListScreen(
            node = node,
            fetchText = viewModel::getNodeText,
            fetchTextForCopy = viewModel::getNodeTextForCopy,
            onNodeClick = viewModel::onNodeClick,
            onNodeContextMenuClick = viewModel::onNodeContextMenuClick,
            searchUiState = uiState.searchUiState,
            searchResults = viewModel.getSearchResultFlow().collectAsState().value,
            currentlyDisplayedNodeId = uiState.nodeCurrentlyDisplayed?.id,
        )
    }
}

@Composable
private fun NodeListScreen(
    node: Node,
    fetchText: (Node) -> String?,
    fetchTextForCopy: (Node) -> String?,
    onNodeClick: (Node) -> Unit,
    onNodeContextMenuClick: (ContextMenuAction) -> Unit,
    searchUiState: fr.julien.quievreux.droidplane2.MainUiState.SearchUiState,
    searchResults: List<Node>,
    currentlyDisplayedNodeId: String?,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        nodeList(
            node = node,
            fetchText = fetchText,
            fetchTextForCopy = fetchTextForCopy,
            updateClipBoard = { /* handled by activity */ },
            onNodeClick = onNodeClick,
            onNodeContextMenuClick = onNodeContextMenuClick,
            searchResultToShow = if (searchUiState.isSearchActive && searchResults.isNotEmpty() && searchUiState.currentResultIndex in searchResults.indices) {
                searchResults[searchUiState.currentResultIndex]
            } else {
                null
            },
            currentlyDisplayedNodeId = currentlyDisplayedNodeId,
        )
    }
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun PreviewMindMapScreen() {
    ContrastAwareReplyTheme {
        MindMapScreen()
    }
}