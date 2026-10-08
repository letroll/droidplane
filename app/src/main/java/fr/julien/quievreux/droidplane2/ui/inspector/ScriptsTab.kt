package fr.julien.quievreux.droidplane2.ui.inspector

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.GenericHookElement
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@Composable
fun ScriptsTab(
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
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.scripts_hooks),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Button(
                onClick = {
                    val updated = node.genericHooks.toMutableList().apply {
                        add(GenericHookElement(name = "ScriptHook", textContent = ""))
                    }
                    onNodeChanged(node.copy(genericHooks = updated))
                }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Hook", modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.size(4.dp))
                Text("Add Hook")
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (node.genericHooks.isEmpty()) {
            Text(
                text = stringResource(R.string.no_scripts),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            node.genericHooks.forEachIndexed { index, hook ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = hook.name,
                                onValueChange = { newName ->
                                    val updated = node.genericHooks.toMutableList()
                                    updated[index] = hook.copy(name = newName)
                                    onNodeChanged(node.copy(genericHooks = updated))
                                },
                                label = { Text("Hook Name") },
                                modifier = Modifier.weight(1f),
                                singleLine = true
                            )
                            IconButton(
                                onClick = {
                                    val updated = node.genericHooks.toMutableList()
                                    updated.removeAt(index)
                                    onNodeChanged(node.copy(genericHooks = updated))
                                }
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete hook", tint = MaterialTheme.colorScheme.error)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = hook.textContent.orEmpty(),
                            onValueChange = { newText ->
                                val updated = node.genericHooks.toMutableList()
                                updated[index] = hook.copy(textContent = newText.ifBlank { null })
                                onNodeChanged(node.copy(genericHooks = updated))
                            },
                            label = { Text(stringResource(R.string.script_content)) },
                            placeholder = { Text("// Enter script code or parameter...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "ScriptsTab Preview")
@Composable
fun PreviewScriptsTab() {
    ContrastAwareReplyTheme {
        Surface {
            ScriptsTab(
                node = Node(
                    parentNode = null,
                    id = "ID_123",
                    numericId = 123,
                    text = "Sample Node Title",
                    genericHooks = mutableListOf(
                        GenericHookElement(name = "accessories/plugins/AutomaticLayout.properties"),
                        GenericHookElement(name = "ScriptHook", textContent = "println('hello from freeplane');")
                    ),
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                onNodeChanged = {}
            )
        }
    }
}
