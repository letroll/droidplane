package fr.julien.quievreux.droidplane2.ui.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.ExitConfirmation

@Composable
fun ExitConfirmationDialog(
    confirmation: ExitConfirmation,
) {
    val onConfirm = confirmation.onConfirm
    val onCancel = confirmation.onCancel

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
                    text = "Unsaved Changes",
                    style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "You have unsaved changes. Are you sure you want to exit without saving?",
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
                )
            ) {
                Text(text = "Exit Without Saving", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(onClick = onCancel) {
                Text(text = "Cancel")
            }
        }
    )
}

@Composable
@androidx.compose.ui.tooling.preview.Preview
fun PreviewExitConfirmationDialog() {
    fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme {
        ExitConfirmationDialog(
            confirmation = ExitConfirmation(
                onConfirm = {},
                onCancel = {},
            )
        )
    }
}
