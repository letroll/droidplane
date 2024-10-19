package fr.julien.quievreux.droidplane2.ui.components

import android.content.Context
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction.Press
import androidx.compose.foundation.interaction.PressInteraction.Release
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
import androidx.compose.material3.CardDefaults.cardElevation
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Clipboard
import compose.icons.fontawesomeicons.regular.Edit
import compose.icons.fontawesomeicons.solid.Link
import fr.julien.quievreux.droidplane2.MainApplication
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.CopyText
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.Edit
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.NodeLink
import fr.julien.quievreux.droidplane2.model.ContextMenuDropDownItem
import fr.julien.quievreux.droidplane2.model.MindmapNode
import fr.julien.quievreux.droidplane2.model.NodeIcons.Link
import fr.julien.quievreux.droidplane2.model.NodeIcons.RicheText
import fr.julien.quievreux.droidplane2.model.getNodeFontIconsFromName
import fr.julien.quievreux.droidplane2.model.getNodeIconsResIdFromName
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

fun LazyListScope.nodeList(
    nodes: List<MindmapNode>,
    fetchText: (MindmapNode) -> String?,
    updateClipBoard: (String) -> Unit,
    onNodeClick: (MindmapNode) -> Unit,
    onNodeContextMenuClick: (ContextMenuAction) -> Unit,
    searchResultToShow: MindmapNode?,
) {
    var isFoundInList = searchResultToShow == null

    items(
        items = nodes,
        key = { node ->
           node.id
        }
    ) { node ->
        if (node.id == searchResultToShow?.id) {
            isFoundInList = true
        }
        fetchText(node)?.let { text ->
            NodeItem(
                fetchText = fetchText,
                updateClipBoard = updateClipBoard,
                isFoundInList = isFoundInList,
                onNodeClick = onNodeClick,
                onNodeContextMenuClick = onNodeContextMenuClick,
                node = node,
                text = text,

                )
        }
    }
    //TODO create bug fix it
//    if (!isFoundInList && searchResultToShow != null) {
//        onNodeClick(searchResultToShow)
//    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NodeItem(
    fetchText: (MindmapNode) -> String?,
    updateClipBoard: (String) -> Unit,
    isFoundInList: Boolean,
    onNodeClick: (MindmapNode) -> Unit,
    onNodeContextMenuClick: (ContextMenuAction) -> Unit,
    node: MindmapNode,
    text: String,
) {

    val contextMenuDropDownItems = mutableListOf(
        ContextMenuDropDownItem(
            text = "copy $text",
            action = CopyText(text)
        )
    )

    node.arrowLinks.forEach { link ->
        fetchText(link)?.let { text ->
            contextMenuDropDownItems.add(
                ContextMenuDropDownItem(
                    text = text,
                    action = NodeLink(
                        node = link,
                    )
                )
            )
        }
    }

    val density = LocalDensity.current
    val interactionSource = remember {
        MutableInteractionSource()
    }
    var isContextMenuVisble by rememberSaveable {
        mutableStateOf(false)
    }

    var pressOffset by remember {
        mutableStateOf(DpOffset.Zero)
    }

    var itemHeight by remember {
        mutableStateOf(0.dp)
    }

    Card(
        modifier = Modifier
//TODO use it when available            .animateItem()
            .onSizeChanged {
                itemHeight = with(density) { it.height.toDp() }
            }
            .padding(4.dp),
        elevation = cardElevation(defaultElevation = 10.dp),
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
                .indication(interactionSource, LocalIndication.current)
                .pointerInput(true) {
                    detectTapGestures(
                        onLongPress = {
                            isContextMenuVisble = true
                            pressOffset = DpOffset(it.x.toDp(), it.y.toDp())
                        },
                        onPress = {
                            val press = Press(it)
                            interactionSource.emit(press)
                            tryAwaitRelease()
                            interactionSource.emit(Release(press))
                        },
                        onTap = {
                            onNodeClick(node)
                        }
                    )
                },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val context = LocalContext.current
            val iconNames = node.iconNames
            val iconResourceIds = mutableListOf<Int>()
            val imageVectors = mutableListOf<ImageVector>()

            for (iconName in iconNames) {
                getNodeFontIconsFromName(iconName)?.let {
                    Log.e(MainApplication.TAG, "converted icon from enum to font icon")
                    imageVectors.add(it)
                }

                getNodeIconsResIdFromName(iconName)?.let {
                    Log.e(MainApplication.TAG, "converted icon from enum")
                    iconResourceIds.add(it)
                } ?: run {
                    val drawableName = getDrawableNameFromMindmapIcon(iconName, context)
                    iconResourceIds.add(context.resources.getIdentifier("@drawable/$drawableName", "id", context.packageName))
                }
            }

            // set link icon if node has a link. The link icon will be the first icon shown
            if (node.link != null) {
                imageVectors.add(Link.imageVector)
            }

            // set the rich text icon if it has
            if (node.richTextContents.isNotEmpty()) {
                imageVectors.add(RicheText.imageVector)
            }

            if (imageVectors.isNotEmpty()) {
                Icon(
                    imageVector = imageVectors.first(),
                    tint = MaterialTheme.colorScheme.primary,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(24.dp)
                )
            } else {
                if (iconResourceIds.isNotEmpty()) {
                    Icon(
                        painter = painterResource(iconResourceIds.first()),
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(24.dp)
                    )
                    if (iconResourceIds.size > 1) {
                        Icon(
                            painter = painterResource(iconResourceIds[1]),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .size(24.dp)
                        )
                    }
                }
            }
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        horizontal = 8.dp,
                        vertical = 10.dp,
                    ),
                text = text.trim(),
                style = applyTextStyle(node),
                color = MaterialTheme.colorScheme.primary
            )
//                    Log.e("toto", "node:$text childes:${node.childMindmapNodes.size}")
            if (node.childMindmapNodes.size > 0) {
                ToggleIcon(
                    isOpen = node.isSelected,
                    modifier = Modifier.padding(end = 8.dp)

                )
            }
        }
        DropdownMenu(
            expanded = isContextMenuVisble,
            onDismissRequest = { isContextMenuVisble = false },
            offset = pressOffset.copy(
                y = pressOffset.y - itemHeight
            )
        ) {
            contextMenuDropDownItems.forEach { item ->
                DropdownMenuItem(
                    onClick = {
                        when(item.action){
                            is CopyText -> updateClipBoard(item.action.text)
                            else -> onNodeContextMenuClick(item.action)
                        }
                        isContextMenuVisble = false
                    },
                    text = {
                        Text(text = item.text)
                    },
//                    modifier = TODO(),
                    leadingIcon = { GetLeadingIcon(item.action) },
//                    trailingIcon = TODO(),
//                    enabled = TODO(),
//                    colors = TODO(),
//                    contentPadding = TODO(),
//                    interactionSource = TODO()
                )
            }

        }
    }
}

@Composable fun GetLeadingIcon(action: ContextMenuAction) =
    Icon(
        imageVector = when (action) {
            is CopyText -> FontAwesomeIcons.Regular.Clipboard
            Edit -> FontAwesomeIcons.Regular.Edit
            is NodeLink -> FontAwesomeIcons.Solid.Link
        },
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
    )

fun applyTextStyle(node: MindmapNode) = TextStyle(
    fontStyle = if (node.isItalic) FontStyle.Italic else FontStyle.Normal,
    fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
)

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
                updateClipBoard = {},
                onNodeClick = {},
                onNodeContextMenuClick = {},
                searchResultToShow = node3,
            )
        }
    }
}