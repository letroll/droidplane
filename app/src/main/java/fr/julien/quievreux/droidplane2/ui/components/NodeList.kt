package fr.julien.quievreux.droidplane2.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.setValue

import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Clipboard
import compose.icons.fontawesomeicons.regular.Edit
import compose.icons.fontawesomeicons.solid.Link
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Trash
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.helper.DateUtils
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.CopyText
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.NodeLink
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.AddChildNode
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.DeleteNode
import fr.julien.quievreux.droidplane2.model.ContextMenuDropDownItem
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.OpenLink
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.Properties
import fr.julien.quievreux.droidplane2.model.NodeIcons.Link
import fr.julien.quievreux.droidplane2.model.NodeIcons.RicheText
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

fun LazyListScope.nodeList(
    node: Node,
    fetchText: (Node) -> String?,
    fetchTextForCopy: (Node) -> String?,
    updateClipBoard: (String) -> Unit,
    onNodeClick: (Node) -> Unit,
    onNodeContextMenuClick: (ContextMenuAction) -> Unit,
    searchResultToShow: Node?,
    currentlyDisplayedNodeId: String? = null,
    onNodeDoubleClick: (Node) -> Unit = {},
) {
    var isFoundInList = searchResultToShow == null

    val childNodes = node.childNodes
    if (childNodes.isEmpty()) {
        item {
            EmptyNodeItem()
        }
    } else {
        items(
            items = childNodes,
            key = { child ->
                // Use both parent and child id as key to force recomposition when parent changes
                "${node.id}_${child.id}"
            }
        ) { child ->
            if (child.id == searchResultToShow?.id) {
                isFoundInList = true
            }
            NodeItem(
                fetchText = fetchText,
                fetchTextForCopy = fetchTextForCopy,
                updateClipBoard = updateClipBoard,
                isFoundInList = isFoundInList,
                onNodeClick = onNodeClick,
                onNodeContextMenuClick = onNodeContextMenuClick,
                node = child,
                currentlyDisplayedNodeId = currentlyDisplayedNodeId,
                onNodeDoubleClick = onNodeDoubleClick,
            )
        }
    }
}

@Composable
fun EmptyNodeItem() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            text = "This node has no children. Tap the + button to add one.",
            style = androidx.compose.ui.text.TextStyle(
                fontSize = 16.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun NodeItem(
    fetchText: (Node) -> String?,
    fetchTextForCopy: (Node) -> String?,
    updateClipBoard: (String) -> Unit,
    isFoundInList: Boolean,
    onNodeClick: (Node) -> Unit,
    onNodeContextMenuClick: (ContextMenuAction) -> Unit,
    node: Node,
    currentlyDisplayedNodeId: String? = null,
    onNodeDoubleClick: (Node) -> Unit = {},
) {
    val text = fetchText(node) ?: ""
    val copyText = fetchTextForCopy(node) ?: text
    val contextMenuDropDownItems = getContextMenuDropDownItems(text, copyText, node)

    AddDeleteMenuIfNeeded(node, contextMenuDropDownItems)

    AddLinkMenuIfNeeded(node, contextMenuDropDownItems)

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

    val currentNode by rememberUpdatedState(node)
    val currentOnNodeClick by rememberUpdatedState(onNodeClick)

    Card(
        modifier = Modifier
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
                .pointerInput(node.id) {
                    detectTapGestures(
                        onDoubleTap = {
                            onNodeDoubleClick(currentNode)
                        },
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
                            currentOnNodeClick(currentNode)
                        }
                    )
                },
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AddIconsIfNeeded(node)

            DisplayLinkIfNeeded(node)

            DisplayRichTextIndicatorIfNeeded(node)

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
            if (node.childNodes.isNotEmpty()) {
                ToggleIcon(
                    isOpen = node.id == currentlyDisplayedNodeId,
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
                        item.action?.let { action ->
                            when (action) {
                                is CopyText -> updateClipBoard(action.text)
                                is NodeLink, is AddChildNode, is ContextMenuAction.OpenLink, is DeleteNode, is ContextMenuAction.Properties -> onNodeContextMenuClick(action)
                            }
                        }
                        isContextMenuVisble = false
                    },
                    text = {
                        Text(text = item.text)
                    },
                    leadingIcon = { item.action?.let { GetLeadingIcon(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                )
            }

        }
    }
}

@Composable
private fun DisplayRichTextIndicatorIfNeeded(node: Node) {
    if (node.richTextContents.isNotEmpty()) {
        Icon(
            imageVector = RicheText.imageVector,
            tint = MaterialTheme.colorScheme.secondary,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 6.dp)
                .size(16.dp)
        )
    }
}

@Composable
private fun DisplayLinkIfNeeded(node: Node) {
    if (node.link != null) {
        Icon(
            imageVector = Link.imageVector,
            tint = MaterialTheme.colorScheme.primary,
            contentDescription = stringResource(R.string.hyperlink),
            modifier = Modifier
                .padding(start = 6.dp)
                .size(16.dp)
        )
    }
}

@Composable
private fun AddIconsIfNeeded(node: Node) {
    // Display assigned Freeplane icons (up to 3 inline with +N overflow)
    if (node.iconNames.isNotEmpty()) {
        NodeIconsRow(
            iconNames = node.iconNames,
            maxInlineIcons = 3,
            iconSize = 18.dp,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun AddDeleteMenuIfNeeded(node: Node, contextMenuDropDownItems: MutableList<ContextMenuDropDownItem>) {
    if (node.parentNode != null) {
        contextMenuDropDownItems.add(
            ContextMenuDropDownItem(
                text = stringResource(id = R.string.delete),
                action = DeleteNode(node = node)
            )
        )
    }
}

@Composable
private fun AddLinkMenuIfNeeded(node: Node, contextMenuDropDownItems: MutableList<ContextMenuDropDownItem>) {
    node.link?.let {
        contextMenuDropDownItems.add(
            ContextMenuDropDownItem(
                text = stringResource(id = R.string.open_link),
                action = OpenLink(node = node)
            )
        )
    }
}

@Composable
private fun getContextMenuDropDownItems(text: String, copyText: String, node: Node): MutableList<ContextMenuDropDownItem> = mutableListOf(
    ContextMenuDropDownItem(
        text = "copy $text",
        action = CopyText(copyText)
    ),
    ContextMenuDropDownItem(
        text = stringResource(
            id = R.string.node_information,
            node.creationDate?.let { DateUtils.formatDate(it) }.orEmpty(),
            node.modificationDate?.let { DateUtils.formatDate(it) }.orEmpty(),
        ),
        action = CopyText(
            stringResource(
                id = R.string.node_information,
                node.creationDate?.let { DateUtils.formatDate(it) }.orEmpty(),
                node.modificationDate?.let { DateUtils.formatDate(it) }.orEmpty(),
            )
        )
    ),
    ContextMenuDropDownItem(
        text = stringResource(id = R.string.node_properties),
        action = Properties(node = node)
    ),
    ContextMenuDropDownItem(
        text = stringResource(id = R.string.add_child_node),
        action = AddChildNode(parentNode = node)
    ),
)

@Composable fun GetLeadingIcon(action: ContextMenuAction) =
    Icon(
        imageVector = when (action) {
            is CopyText -> FontAwesomeIcons.Regular.Clipboard
            is ContextMenuAction.Properties -> FontAwesomeIcons.Regular.Edit
            is NodeLink -> FontAwesomeIcons.Solid.Link
            is AddChildNode -> FontAwesomeIcons.Solid.Plus
            is DeleteNode -> FontAwesomeIcons.Solid.Trash
            is ContextMenuAction.OpenLink -> FontAwesomeIcons.Solid.Link
        },
        tint = MaterialTheme.colorScheme.primary,
        contentDescription = when (action) {
            is CopyText -> "Copy node text"
            is ContextMenuAction.Properties -> "Node properties"
            is NodeLink -> "Follow link to ${(action as NodeLink).node.text.orEmpty()}"
            is AddChildNode -> "Add child node"
            is DeleteNode -> "Delete node ${(action as DeleteNode).node.text.orEmpty()}"
            is ContextMenuAction.OpenLink -> "Open link"
        },
        modifier = Modifier.size(24.dp),
    )

fun applyTextStyle(node: Node) = TextStyle(
    fontStyle = if (node.isItalic) FontStyle.Italic else FontStyle.Normal,
    fontWeight = if (node.isBold) FontWeight.Bold else FontWeight.Normal,
)

@Composable
@Preview
private fun NodeListPreview() {

    var nodeParent = Node(
        parentNode = null,
        id = "sqddqfsdfqsfqsd",
        numericId = 119123123,
        text = "Parent",
        link = null,
        treeIdAttribute = null,
        creationDate = 1728740019071,
        modificationDate = 1728740023499,
        position = null,
    )

    val node1 = Node(
        parentNode = nodeParent,
        id = "sumo",
        numericId = 11963,
        text = "node1 (2 icons)",
        iconNames = mutableListOf("button_ok", "flag-red"),
        link = null,
        treeIdAttribute = null,
        creationDate = 1728740019071,
        modificationDate = 1728740023499,
        position = null,
    )

    val node2 = Node(
        parentNode = nodeParent,
        id = "dqfqsd",
        numericId = 11962,
        text = "node2 (5 icons - shows +2)",
        iconNames = mutableListOf("full-1", "priority-2", "idea", "attach", "help"),
        link = null,
        treeIdAttribute = null,
        creationDate = 1728740019071,
        modificationDate = 1728740023499,
        position = null,
    )


    node2.addChildMindmapNode(node1)

    val node3 = Node(
        parentNode = nodeParent,
        id = "sqdfqsd",
        numericId = 1196,
        text = "node3 (unmapped badge)",
        iconNames = mutableListOf("custom_unknown_badge"),
        link = null,
        treeIdAttribute = null,
        creationDate = 1728740019071,
        modificationDate = 1728740023499,
        position = null,
    )

   nodeParent = nodeParent.copy(childNodes = mutableListOf(node1, node2, node3))

    ContrastAwareReplyTheme {
        LazyColumn(
            modifier = Modifier.height(300.dp),
        ) {
            nodeList(
                node = nodeParent,
                fetchText = { node ->
                    node.text
                },
                fetchTextForCopy = { node ->
                    node.text
                },
                updateClipBoard = {},
                onNodeClick = {},
                onNodeContextMenuClick = {},
                searchResultToShow = null,
            )
        }
    }
}