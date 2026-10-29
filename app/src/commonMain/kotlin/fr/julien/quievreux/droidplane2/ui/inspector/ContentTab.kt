package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.util.HtmlTextExtractor
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.res.stringResource
import fr.julien.quievreux.droidplane2.ui.components.HybridRichContentEditor
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ContentTab(
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
        // Title Section: Plain vs Rich Text Toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.node_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Row(horizontalArrangement = spacedBy(4.dp)) {
                FilterChip(
                    selected = node.richText == null,
                    onClick = {
                        val plain = node.richText?.let { source ->
                            HtmlTextExtractor.extractPlainText(source).trim()
                        } ?: node.text
                        onNodeChanged(node.copy(richText = null, text = plain))
                    },
                    label = { Text(stringResource(R.string.plain_text), fontSize = 12.sp) }
                )
                FilterChip(
                    selected = node.richText != null,
                    onClick = {
                        val initialRich = node.richText
                            ?: "<html><body><p>${node.text.orEmpty()}</p></body></html>"
                        onNodeChanged(node.copy(richText = initialRich))
                    },
                    label = { Text(stringResource(R.string.rich_text), fontSize = 12.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        if (node.richText != null) {
            HybridRichContentEditor(
                value = node.richText.orEmpty(),
                onValueChange = { newRich ->
                    val strippedText = HtmlTextExtractor.extractPlainText(newRich).trim()
                    onNodeChanged(node.copy(richText = newRich.ifBlank { null }, text = strippedText))
                },
                label = stringResource(R.string.node_title)
            )
        } else {
            OutlinedTextField(
                value = node.text.orEmpty(),
                onValueChange = { newText ->
                    onNodeChanged(node.copy(text = newText))
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.node_title)) },
                singleLine = true
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Details Editor
        HybridRichContentEditor(
            value = node.detailsText.orEmpty(),
            onValueChange = { newDetails ->
                onNodeChanged(node.copy(detailsText = newDetails.ifBlank { null }))
            },
            label = stringResource(R.string.details)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Notes Editor
        HybridRichContentEditor(
            value = node.noteText.orEmpty(),
            onValueChange = { newNotes ->
                onNodeChanged(node.copy(noteText = newNotes.ifBlank { null }))
            },
            label = stringResource(R.string.notes)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // LaTeX Equation Field
        OutlinedTextField(
            value = node.latexEquation.orEmpty(),
            onValueChange = { newEq ->
                onNodeChanged(node.copy(latexEquation = newEq.ifBlank { null }))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.latex_formula)) },
            placeholder = { Text("\\sum_{i=1}^n x_i") }
        )
    }
}

@Preview
@Composable
fun PreviewContentTab() {
    ContrastAwareReplyTheme {
        Surface {
            ContentTab(
                node = Node(
                    parentNode = null,
                    id = "ID_123",
                    numericId = 123,
                    text = "Sample Node Title",
                    detailsText = "These are details",
                    noteText = "These are extended notes",
                    latexEquation = "\\alpha + \\beta",
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                onNodeChanged = {}
            )
        }
    }
}
