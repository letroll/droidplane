package fr.julien.quievreux.droidplane2.ui.view

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.StartupChooser
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.model.RecentFile
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import java.text.DateFormat
import java.util.Date

@Composable
fun StartupChooserDialog(
    chooser: StartupChooser,
) {
    val dateFormat = DateFormat.getDateInstance(DateFormat.SHORT)

    AlertDialog(
        onDismissRequest = chooser.onDismiss,
        title = {
            Text(
                text = stringResource(R.string.startup_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
            ) {
                // Primary Action: New Mindmap
                Button(
                    onClick = chooser.onNewMindmap,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.new_mindmap))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Secondary Actions: Browse & Help Demo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = chooser.onBrowse,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.open),
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }

                    OutlinedButton(
                        onClick = chooser.onOpenDemo,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.help),
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Recent Files Section
                Text(
                    text = stringResource(R.string.recent_files),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(modifier = Modifier.height(6.dp))

                if (chooser.recentFiles.isEmpty()) {
                    Text(
                        text = stringResource(R.string.no_recent_files),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                    ) {
                        items(chooser.recentFiles, key = { it.uriString }) { recent ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { chooser.onOpenRecent(recent) }
                                    .padding(vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recent.displayName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = dateFormat.format(Date(recent.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = chooser.onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        },
    )
}

@Preview(name = "Startup Chooser Light")
@Preview(name = "Startup Chooser Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PreviewStartupChooserDialog() {
    ContrastAwareReplyTheme {
        StartupChooserDialog(
            chooser = StartupChooser(
                recentFiles = listOf(
                    RecentFile(
                        uriString = "content://example/my_map.mm",
                        displayName = "Project Roadmap.mm",
                        timestamp = System.currentTimeMillis(),
                    ),
                    RecentFile(
                        uriString = "content://example/notes.mm",
                        displayName = "Meeting Notes.mm",
                        timestamp = System.currentTimeMillis() - 86400000L,
                    ),
                ),
                onNewMindmap = {},
                onOpenRecent = {},
                onBrowse = {},
                onOpenDemo = {},
                onDismiss = {},
            )
        )
    }
}
