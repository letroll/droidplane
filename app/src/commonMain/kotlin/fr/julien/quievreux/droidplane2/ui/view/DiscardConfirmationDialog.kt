package fr.julien.quievreux.droidplane2.ui.view

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import fr.julien.quievreux.droidplane2.res.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.DiscardConfirmation
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@Composable
fun DiscardConfirmationDialog(
    confirmation: DiscardConfirmation,
) {
    AlertDialog(
        onDismissRequest = confirmation.onCancel,
        title = {
            Text(
                text = stringResource(R.string.discard_changes_title),
                style = MaterialTheme.typography.titleMedium,
            )
        },
        text = {
            Text(
                text = stringResource(R.string.discard_changes_message),
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
                Text(stringResource(R.string.discard))
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
fun PreviewDiscardConfirmationDialog() {
    ContrastAwareReplyTheme {
        DiscardConfirmationDialog(
            confirmation = DiscardConfirmation(
                onConfirm = {},
                onCancel = {},
            )
        )
    }
}
