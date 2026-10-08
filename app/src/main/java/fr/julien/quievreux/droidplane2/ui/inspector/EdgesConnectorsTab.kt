package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.ConnectorLink
import fr.julien.quievreux.droidplane2.data.model.EdgeProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

private val EDGE_PALETTE_COLORS = listOf(
    "#000000" to Color.Black,
    "#0033AA" to Color(0xFF0033AA),
    "#D32F2F" to Color(0xFFD32F2F),
    "#E65100" to Color(0xFFE65100),
    "#388E3C" to Color(0xFF388E3C),
    "#7B1FA2" to Color(0xFF7B1FA2),
)

@Composable
fun EdgesConnectorsTab(
    node: Node,
    onNodeChanged: (Node) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 8.dp)
    ) {
        // Parent Branch Edge Section
        Text(
            text = stringResource(R.string.edge_styling),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Text(text = stringResource(R.string.edge_style), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val edgeStyles = listOf("bezier", "linear", "sharp-linear", "hide_edge")
            edgeStyles.forEach { styleKey ->
                FilterChip(
                    selected = node.edge?.style == styleKey,
                    onClick = {
                        val currentEdge = node.edge ?: EdgeProperties()
                        val newStyle = if (currentEdge.style == styleKey) null else styleKey
                        onNodeChanged(node.copy(edge = currentEdge.copy(style = newStyle)))
                    },
                    label = { Text(styleKey) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = stringResource(R.string.edge_color), style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EDGE_PALETTE_COLORS.forEach { (hex, color) ->
                val isSelected = node.edge?.color.equals(hex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            shape = CircleShape
                        )
                        .clickable {
                            val currentEdge = node.edge ?: EdgeProperties()
                            val newColor = if (isSelected) null else hex
                            onNodeChanged(node.copy(edge = currentEdge.copy(color = newColor)))
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = node.edge?.width.orEmpty(),
            onValueChange = { newWidth ->
                val currentEdge = node.edge ?: EdgeProperties()
                onNodeChanged(node.copy(edge = currentEdge.copy(width = newWidth.ifBlank { null })))
            },
            label = { Text(stringResource(R.string.edge_width)) },
            placeholder = { Text("1, 2, thin, etc.") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Connectors (Arrow Links) Section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.connectors),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = {
                    val updatedConnectors = node.connectors.toMutableList().apply {
                        add(ConnectorLink(destinationId = "", color = "#FF0000", endArrow = "Default"))
                    }
                    onNodeChanged(node.copy(connectors = updatedConnectors))
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_connector), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text(stringResource(R.string.add_connector))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (node.connectors.isEmpty()) {
            Text(
                text = "No outgoing connectors on this node.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            node.connectors.forEachIndexed { index, connector ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = connector.destinationId,
                        onValueChange = { newDestId ->
                            val updated = node.connectors.toMutableList()
                            updated[index] = connector.copy(destinationId = newDestId)
                            onNodeChanged(node.copy(connectors = updated))
                        },
                        label = { Text(stringResource(R.string.destination_node)) },
                        modifier = Modifier.weight(1.2f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = connector.middleLabel.orEmpty(),
                        onValueChange = { newLabel ->
                            val updated = node.connectors.toMutableList()
                            updated[index] = connector.copy(middleLabel = newLabel.ifBlank { null })
                            onNodeChanged(node.copy(connectors = updated))
                        },
                        label = { Text(stringResource(R.string.middle_label)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val updated = node.connectors.toMutableList()
                            updated.removeAt(index)
                            onNodeChanged(node.copy(connectors = updated))
                        }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "Delete connector",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "EdgesConnectorsTab Preview")
@Composable
fun PreviewEdgesConnectorsTab() {
    ContrastAwareReplyTheme {
        Surface {
            EdgesConnectorsTab(
                node = Node(
                    parentNode = null,
                    id = "ID_123",
                    numericId = 123,
                    text = "Sample Node Title",
                    edge = EdgeProperties(color = "#0033AA", style = "linear", width = "2"),
                    connectors = mutableListOf(
                        ConnectorLink(destinationId = "ID_456", middleLabel = "relates to", color = "#FF0000")
                    ),
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                onNodeChanged = {}
            )
        }
    }
}
