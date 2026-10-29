package fr.julien.quievreux.droidplane2.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.FontAwesomeIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Download
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.res.stringResource
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.update.GitHubRelease
import org.jetbrains.compose.ui.tooling.preview.Preview

/**
 * Dialog notifying the user of an available application update on GitHub.
 * Displays target version, current version, changelog, and actions to install or view on GitHub.
 */
@Composable
fun UpdateAvailableDialog(
    currentVersion: String,
    release: GitHubRelease,
    onDownloadAndInstall: (apkUrl: String, fileName: String) -> Unit,
    onViewOnGitHub: (url: String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        icon = {
            Icon(
                imageVector = FontAwesomeIcons.Solid.Download,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                text = stringResource(R.string.update_available_title, release.tagName),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.update_versions, currentVersion, release.tagName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (release.body.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.update_changelog_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ElevatedCard(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 200.dp, max = 360.dp)
                    ) {
                        Text(
                            text = release.body,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(8.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.update_changelog_title),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    ElevatedCard(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 150.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.update_no_changelog),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            if (release.apkDownloadUrl != null) {
                Button(
                    onClick = {
                        onDismiss()
                        onDownloadAndInstall(
                            release.apkDownloadUrl,
                            release.apkFileName ?: "droidplane-update.apk"
                        )
                    }
                ) {
                    Text(stringResource(R.string.update_download_install))
                }
            } else {
                Button(
                    onClick = {
                        onDismiss()
                        onViewOnGitHub(release.htmlUrl)
                    }
                ) {
                    Text(stringResource(R.string.update_view_on_github))
                }
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (release.apkDownloadUrl != null) {
                    TextButton(
                        onClick = {
                            onDismiss()
                            onViewOnGitHub(release.htmlUrl)
                        }
                    ) {
                        Text(stringResource(R.string.update_view_on_github))
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.update_later))
                }
            }
        }
    )
}

@Preview
@Composable
fun PreviewUpdateAvailableDialog() {
    ContrastAwareReplyTheme {
        UpdateAvailableDialog(
            currentVersion = "1.0.18",
            release = GitHubRelease(
                tagName = "v1.0.25",
                name = "Release v1.0.25",
                publishedAt = "2026-10-14",
                body = "• Support for Freeplane node icons\n• In-app GitHub update checker\n• Bug fixes and UI improvements",
                htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                apkFileName = "app-release.apk"
            ),
            onDownloadAndInstall = { _, _ -> },
            onViewOnGitHub = {},
            onDismiss = {}
        )
    }
}
