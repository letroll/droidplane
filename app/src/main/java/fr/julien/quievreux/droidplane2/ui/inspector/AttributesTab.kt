package fr.julien.quievreux.droidplane2.ui.inspector

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttributeEntry
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@Composable
fun AttributesTab(
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
        // Hyperlink Section
        Text(
            text = stringResource(R.string.hyperlink),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = node.link?.toString().orEmpty(),
            onValueChange = { newLink ->
                val uri = if (newLink.isBlank()) null else Uri.parse(newLink)
                onNodeChanged(node.copy(link = uri))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("URL or #ID_target") },
            placeholder = { Text("https://example.com or #ID_123456") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Attributes Table Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.attributes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = {
                    val updatedAttributes = node.attributes.toMutableList().apply {
                        add(NodeAttributeEntry(name = "", value = ""))
                    }
                    onNodeChanged(node.copy(attributes = updatedAttributes))
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.add_attribute), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text(stringResource(R.string.add_attribute))
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (node.attributes.isEmpty()) {
            Text(
                text = "No attributes defined on this node.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        } else {
            node.attributes.forEachIndexed { index, attr ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = attr.name,
                        onValueChange = { newName ->
                            val updated = node.attributes.toMutableList()
                            updated[index] = attr.copy(name = newName)
                            onNodeChanged(node.copy(attributes = updated))
                        },
                        label = { Text(stringResource(R.string.attribute_name)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = attr.value,
                        onValueChange = { newValue ->
                            val updated = node.attributes.toMutableList()
                            updated[index] = attr.copy(value = newValue)
                            onNodeChanged(node.copy(attributes = updated))
                        },
                        label = { Text(stringResource(R.string.attribute_value)) },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )

                    IconButton(
                        onClick = {
                            val updated = node.attributes.toMutableList()
                            updated.removeAt(index)
                            onNodeChanged(node.copy(attributes = updated))
                        }
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.delete_attribute),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "AttributesTab Preview")
@Composable
fun PreviewAttributesTab() {
    ContrastAwareReplyTheme {
        Surface {
            AttributesTab(
                node = Node(
                    parentNode = null,
                    id = "ID_123",
                    numericId = 123,
                    text = "Sample Node Title",
                    attributes = mutableListOf(
                        NodeAttributeEntry("Priority", "High"),
                        NodeAttributeEntry("Status", "In Progress")
                    ),
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                onNodeChanged = {}
            )
        }
    }
}
