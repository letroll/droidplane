package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Regular
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.regular.Edit
import compose.icons.fontawesomeicons.solid.Link
import compose.icons.fontawesomeicons.solid.Plus
import compose.icons.fontawesomeicons.solid.Trash
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.ADD_CHILD
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.DELETE
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.OPEN_LINK
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.PROPERTIES

@Composable
fun NodeBottomToolbar(
    node: Node,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 8.dp)
            .padding(bottom = 56.dp) // 56dp for navigation bar
            .background(MaterialTheme.colorScheme.surface),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
    ) {
        ToolbarItem(
            action = PROPERTIES,
            icon = FontAwesomeIcons.Regular.Edit,
            onAction = onAction,
            onDismiss = onDismiss
        )
        // Show open_link only if node has a link
        if (node.link != null) {
            ToolbarItem(
                action = OPEN_LINK,
                icon = FontAwesomeIcons.Solid.Link,
                onAction = onAction,
                onDismiss = onDismiss
            )
        }
        ToolbarItem(
            action = ADD_CHILD,
            icon = FontAwesomeIcons.Solid.Plus,
            onAction = onAction,
            onDismiss = onDismiss
        )
        // Show delete only for non-root nodes
        if (node.parentNode != null) {
            ToolbarItem(
                action = DELETE,
                icon = FontAwesomeIcons.Solid.Trash,
                onAction = onAction,
                onDismiss = onDismiss
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ToolbarItem(
    action: String,
    icon: ImageVector,
    onAction: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val text = when (action) {
        PROPERTIES -> stringResource(R.string.node_properties)
        OPEN_LINK -> stringResource(R.string.open_link)
        ADD_CHILD -> stringResource(R.string.add_child_node)
        DELETE -> stringResource(R.string.delete)
        else -> ""
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onAction(action)
                onDismiss()
            },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp),
            contentDescription = null,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}