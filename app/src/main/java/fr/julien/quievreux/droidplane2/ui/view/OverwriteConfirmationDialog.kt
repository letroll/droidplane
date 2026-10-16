package fr.julien.quievreux.droidplane2.ui.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.OverwriteConfirmation
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@Composable
fun OverwriteConfirmationDialog(
    confirmation: OverwriteConfirmation,
) {
    AlertDialog(
        onDismissRequest = confirmation.onCancel,
        title = {
            Text(
                text = stringResource(R.string.overwrite_file_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.overwrite_file_message, confirmation.fileName),
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        confirmButton = {
            Button(
                onClick = confirmation.onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(stringResource(R.string.overwrite_replace))
            }
        },
        dismissButton = {
            Button(onClick = confirmation.onCancel) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview
@Composable
fun PreviewOverwriteConfirmationDialog() {
    ContrastAwareReplyTheme {
        OverwriteConfirmationDialog(
            confirmation = OverwriteConfirmation(
                fileName = "my_mindmap.mm",
                onConfirm = {},
                onCancel = {},
            )
        )
    }
}
