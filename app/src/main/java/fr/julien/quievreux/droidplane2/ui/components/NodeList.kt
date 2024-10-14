package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.model.MindmapNode
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

fun LazyListScope.nodeList(
    nodes: List<MindmapNode>,
    fetchText: (MindmapNode) -> String?,
    onNodeClick: (MindmapNode) -> Unit,
    searchResultToShow: MindmapNode?,
) {
    var isFoundInList = searchResultToShow == null

    items(nodes) { node ->
        if (node.id == searchResultToShow?.id) {
            isFoundInList = true
        }
        fetchText(node)?.let { text ->
            Card(
                modifier = Modifier.padding(4.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isFoundInList) {
                        MaterialTheme.colorScheme.surfaceContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    }
                )
            ) {
                Row(
                    modifier = Modifier
                        .clickable {
                            onNodeClick(node)
                        },
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    //TODO handle icon
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(
                                horizontal = 8.dp,
                                vertical = 10.dp
                            ),
                        text = text
                    )
//                    Log.e("toto", "node:$text childes:${node.childMindmapNodes.size}")
                    if (node.childMindmapNodes.size > 0) {
                        ToggleIcon(
                            isOpen = node.isSelected,
                            modifier = Modifier.padding(end = 8.dp)

                        )
                    }
                }
            }
        }
    }
    if (!isFoundInList && searchResultToShow != null) {
        onNodeClick(searchResultToShow)
    }
}

@Composable
@Preview
private fun NodeListPreview() {
    val node1 = MindmapNode(
        parentNode = null,
        id = "sumo",
        numericId = 1196,
        text = "root",
        link = null,
        treeIdAttribute = null
    )

    val node2 = MindmapNode(
        parentNode = null,
        id = "sumo",
        numericId = 1196,
        text = "root",
        link = null,
        treeIdAttribute = null
    )

    node2.addChildMindmapNode(node1)

    val node3 = MindmapNode(
        parentNode = null,
        id = "sumo",
        numericId = 1196,
        text = "root",
        link = null,
        treeIdAttribute = null
    )

    val nodes = listOf(node1, node2, node3)
    ContrastAwareReplyTheme {
//        val context = LocalContext.current
//        val viewModel = MainViewModel()
        LazyColumn(
            modifier = Modifier.height(300.dp),
        ) {
            nodeList(
                nodes = nodes,
                fetchText = { node ->
//                    node.getNodeText(viewModel)
                    "node"
                },
                onNodeClick = {},
                searchResultToShow = node3,
            )
        }
    }
}