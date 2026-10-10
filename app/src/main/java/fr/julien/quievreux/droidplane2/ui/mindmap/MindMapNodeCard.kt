package fr.julien.quievreux.droidplane2.ui.mindmap

import android.content.res.Configuration
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Link
import fr.julien.quievreux.droidplane2.model.getNodeFontIconsFromName
import fr.julien.quievreux.droidplane2.model.getNodeIconsResIdFromName
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MindMapNodeCard(
    node: Node,
    isSelected: Boolean,
    isCollapsed: Boolean,
    branchDirection: BranchDirection,
    onNodeClick: () -> Unit,
    onToggleCollapse: () -> Unit,
    onNodeDoubleClick: (() -> Unit)? = null,
    onContextMenuAction: (ContextMenuAction) -> Unit = {},
    fetchText: (Node) -> String? = { it.text },
    modifier: Modifier = Modifier,
) {
    var showMenu by remember { mutableStateOf(false) }

    val hasChildren = node.childNodes.isNotEmpty()
    val isRoot = branchDirection == BranchDirection.ROOT
    val displayText = fetchText(node)?.ifEmpty { " " } ?: node.text.orEmpty().ifEmpty { " " }

    val shape = when (node.style) {
        "bubble", "oval" -> CircleShape
        "rectangle" -> RoundedCornerShape(2.dp)
        "fork" -> RoundedCornerShape(topStart = 0.dp, bottomStart = 0.dp, topEnd = 8.dp, bottomEnd = 8.dp)
        else -> if (isRoot) RoundedCornerShape(16.dp) else RoundedCornerShape(8.dp)
    }

    val customBgColor: Color? = node.backgroundColor?.let { parseHexColor(it) }
    val customTextColor: Color? = node.color?.let { parseHexColor(it) }
    val cloudColor: Color? = node.cloud?.color?.let { parseHexColor(it) }

    val containerColor: Color = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        customBgColor != null -> customBgColor
        isRoot -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    val contentColor: Color = when {
        isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
        customTextColor != null -> customTextColor
        isRoot -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }

    val border = when {
        isSelected -> BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        cloudColor != null -> BorderStroke(2.dp, cloudColor)
        isRoot -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary)
        else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Box(modifier = modifier) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .combinedClickable(
                    onClick = onNodeClick,
                    onDoubleClick = onNodeDoubleClick,
                    onLongClick = { showMenu = true },
                ),
            shape = shape,
            color = containerColor,
            contentColor = contentColor,
            border = border,
            shadowElevation = if (isSelected) 4.dp else 1.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.weight(1f, fill = false),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (node.cloud != null) {
                        Text(
                            text = "☁",
                            fontSize = 12.sp,
                            color = cloudColor ?: MaterialTheme.colorScheme.primary
                        )
                    }

                    // Display assigned icons
                    node.iconNames.forEach { iconName ->
                        val imageVec = getNodeFontIconsFromName(iconName)
                        if (imageVec != null) {
                            Icon(
                                imageVector = imageVec,
                                contentDescription = iconName,
                                tint = contentColor,
                                modifier = Modifier.size(16.dp)
                            )
                        } else {
                            val resId = getNodeIconsResIdFromName(iconName)
                            if (resId != null && resId != 0 && resId != R.drawable.ic_launcher) {
                                Icon(
                                    painter = painterResource(resId),
                                    contentDescription = iconName,
                                    tint = contentColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Display link indicator badge
                    if (node.link != null) {
                        Icon(
                            imageVector = FontAwesomeIcons.Solid.Link,
                            contentDescription = stringResource(R.string.hyperlink),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Display note indicator
                    if (!node.noteText.isNullOrBlank()) {
                        Text(
                            text = "📝",
                            fontSize = 12.sp,
                        )
                    }

                    // Display details indicator
                    if (!node.detailsText.isNullOrBlank()) {
                        Text(
                            text = "💬",
                            fontSize = 12.sp,
                        )
                    }

                    Text(
                        text = displayText,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = node.fontSize?.sp ?: (if (isRoot) 15.sp else 13.sp),
                            fontWeight = if (node.isBold || isRoot) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (node.isItalic) FontStyle.Italic else FontStyle.Normal,
                            lineHeight = (node.fontSize?.let { (it * 1.35f).sp }) ?: (if (isRoot) 19.sp else 16.sp),
                        ),
                        maxLines = 100,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (hasChildren && !isRoot) {
                    FoldIndicator(
                        isCollapsed = isCollapsed,
                        onClick = onToggleCollapse,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.edit)) },
                onClick = {
                    showMenu = false
                    onContextMenuAction(ContextMenuAction.Edit(node))
                },
            )
            if (node.link != null) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.open_link)) },
                    onClick = {
                        showMenu = false
                        onContextMenuAction(ContextMenuAction.OpenLink(node))
                    },
                )
            }
            DropdownMenuItem(
                text = { Text(stringResource(R.string.node_properties)) },
                onClick = {
                    showMenu = false
                    onContextMenuAction(ContextMenuAction.Properties(node))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.add_child_node)) },
                onClick = {
                    showMenu = false
                    onContextMenuAction(ContextMenuAction.AddChildNode(node))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.copynodetext)) },
                onClick = {
                    showMenu = false
                    onContextMenuAction(ContextMenuAction.CopyText(node.text.orEmpty()))
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(R.string.delete)) },
                onClick = {
                    showMenu = false
                    onContextMenuAction(ContextMenuAction.DeleteNode(node))
                },
            )
        }
    }
}

@Composable
private fun FoldIndicator(
    isCollapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.size(20.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = if (isCollapsed) "+" else "−",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 11.sp,
            )
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewMindMapNodeCard() {
    ContrastAwareReplyTheme {
        val node = Node(
            parentNode = null,
            id = "preview_1",
            numericId = 1,
            text = "MindMap Idea Node",
            isBold = true,
            creationDate = 0L,
            modificationDate = 0L,
        )
        node.childNodes.add(
            Node(parentNode = node, id = "c1", numericId = 2, text = "Child", creationDate = 0L, modificationDate = 0L)
        )

        MindMapNodeCard(
            node = node,
            isSelected = true,
            isCollapsed = false,
            branchDirection = BranchDirection.RIGHT,
            onNodeClick = {},
            onToggleCollapse = {},
        )
    }
}

@Preview(name = "Root Node Preview")
@Composable
fun PreviewMindMapNodeCardRoot() {
    ContrastAwareReplyTheme {
        val rootNode = Node(
            parentNode = null,
            id = "root",
            numericId = 1,
            text = "Main Concept Root",
            creationDate = 0L,
            modificationDate = 0L,
        )

        MindMapNodeCard(
            node = rootNode,
            isSelected = false,
            isCollapsed = false,
            branchDirection = BranchDirection.ROOT,
            onNodeClick = {},
            onToggleCollapse = {},
        )
    }
}

private fun parseHexColor(hex: String): Color? {
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = when (clean.length) {
            6 -> 0xFF000000.toInt() or clean.toInt(16)
            8 -> clean.toLong(16).toInt()
            else -> return null
        }
        Color(colorInt)
    } catch (e: Exception) {
        null
    }
}

