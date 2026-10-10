package fr.julien.quievreux.droidplane2.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@Composable
fun DeleteConfirmationDialog(
    confirmation: DeleteConfirmation,
) {
    val node = confirmation.node
    val descendantCount = confirmation.descendantCount
    val onConfirm = confirmation.onConfirm
    val onCancel = confirmation.onCancel

    val descendantText = when {
        descendantCount == 0 -> "no descendants"
        descendantCount == 1 -> "1 descendant"
        else -> "$descendantCount descendants"
    }

    val dialogMessage = "Delete \"${node.text.orEmpty()}\" and $descendantText? This action cannot be undone."
    val deleteButtonContentDescription = "Delete ${node.text.orEmpty()} and $descendantText"
    val cancelButtonContentDescription = "Cancel deletion"

    AlertDialog(
        onDismissRequest = onCancel,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.Warning,
                    contentDescription = "Warning",
                    modifier = Modifier
                        .width(28.dp)
                        .height(28.dp)
                        .padding(end = 8.dp),
                    tint = androidx.compose.material3.MaterialTheme.colorScheme.error
                )
                Text(
                    text = "Delete Node?",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dialogMessage,
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onError
                ),
                modifier = Modifier
                    .semantics {
                        contentDescription = deleteButtonContentDescription
                        // Role and stateDescription not available in this Compose version
                    }
            ) {
                Text(text = "Delete", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = onCancel,
                modifier = Modifier
                    .semantics {
                        contentDescription = cancelButtonContentDescription
                        // Role not available in this Compose version
                    }
            ) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun PreviewDeleteConfirmationDialog() {
    ContrastAwareReplyTheme {
        DeleteConfirmationDialog(
            confirmation = DeleteConfirmation(
                node = Node(
                    parentNode = null,
                    id = "ID_1",
                    numericId = 1,
                    text = "Test Node",
                    childNodes = mutableListOf(),
                    link = null,
                    treeIdAttribute = null,
                    richTextContents = mutableListOf(),
                    richContentType = null,
                    iconNames = mutableListOf(),
                    creationDate = 0,
                    modificationDate = 0,
                    isBold = false,
                    isItalic = false,
                    position = null,
                    arrowLinkDestinationIds = mutableListOf(),
                    arrowLinkDestinationNodes = mutableListOf(),
                    arrowLinkIncomingNodes = mutableListOf(),
                ),
                descendantCount = 3,
                onConfirm = {},
                onCancel = {},
            )
        )
    }
}