package fr.julien.quievreux.droidplane2.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.MainApplication
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
                    val context = LocalContext.current
                    val iconNames = node.iconNames
                    val iconResourceIds = mutableListOf<Int>()
                    for (iconName in iconNames) {
                        val drawableName = getDrawableNameFromMindmapIcon(iconName,context)
                        iconResourceIds.add(context.resources.getIdentifier("@drawable/$drawableName", "id", context.packageName))
                    }

                    // set link icon if node has a link. The link icon will be the first icon shown
                    if (node.link != null) {
                        iconResourceIds.add(0, context.resources.getIdentifier("@drawable/link", "id", context.packageName))
                    }

                    // set the rich text icon if it has
                    if (node.richTextContents.isNotEmpty()) {
                        iconResourceIds.add(0, context.resources.getIdentifier("@drawable/richtext", "id", context.packageName))
                    }

                    if(iconResourceIds.isNotEmpty()) {
                        Icon(
                            painter = painterResource(iconResourceIds.first()),
                            contentDescription = null,
                            modifier = Modifier.padding(start = 8.dp).size(24.dp)
                        )
                        if(iconResourceIds.size>1) {
                            Icon(
                                painter = painterResource(iconResourceIds[1]),
                                contentDescription = null,
                                modifier = Modifier.padding(start = 8.dp).size(24.dp)
                            )
                        }
                    }
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
    //TODO create bug fix it
//    if (!isFoundInList && searchResultToShow != null) {
//        onNodeClick(searchResultToShow)
//    }
}

/**
 * MainViewModel icons have names such as 'button-ok', but resources have to have names with pattern [a-z0-9_.]. This
 * method translates the MainViewModel icon names to Android resource names.
 *
 * @param iconName the icon name as it is specified in the XML
 * @return the name of the corresponding android resource icon
 */
private fun getDrawableNameFromMindmapIcon(iconName: String, context: Context): String {
    var name = "icon_" + iconName.lowercase(context.resources.configuration.locale).replace("[^a-z0-9_.]".toRegex(), "_")
    name = name.replace("_$".toRegex(), "")
    Log.e(MainApplication.TAG, "converted icon name $iconName to $name")
    return name
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