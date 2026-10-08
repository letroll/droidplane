package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.components.HybridRichContentEditor
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

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
        // Plain Title
        OutlinedTextField(
            value = node.text.orEmpty(),
            onValueChange = { newText ->
                onNodeChanged(node.copy(text = newText))
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.node_title)) },
            singleLine = true
        )

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

@Preview(name = "ContentTab Preview")
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
