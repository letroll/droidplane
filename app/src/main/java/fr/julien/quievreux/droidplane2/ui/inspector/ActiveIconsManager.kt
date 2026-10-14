package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.ui.components.NodeIconView
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

/**
 * Management component for inspecting, reordering, and removing assigned icons on a node.
 */
@Composable
fun ActiveIconsManager(
    iconNames: List<String>,
    onRemoveIcon: (index: Int) -> Unit,
    onMoveIcon: (fromIndex: Int, toIndex: Int) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val titleText = if (iconNames.isEmpty()) {
                stringResource(R.string.assigned_icons)
            } else {
                "${stringResource(R.string.assigned_icons)} (${iconNames.size})"
            }
            Text(
                text = titleText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (iconNames.isNotEmpty()) {
                TextButton(onClick = onClearAll) {
                    Text(
                        text = stringResource(R.string.clear_all_icons),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (iconNames.isEmpty()) {
            Text(
                text = stringResource(R.string.no_assigned_icons),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                iconNames.forEachIndexed { index, iconName ->
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NodeIconView(
                                iconName = iconName,
                                size = 22.dp
                            )

                            Text(
                                text = iconName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            )

                            // Move Left / Earlier
                            if (iconNames.size > 1) {
                                IconButton(
                                    onClick = { onMoveIcon(index, index - 1) },
                                    enabled = index > 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = stringResource(R.string.move_icon_left),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                // Move Right / Later
                                IconButton(
                                    onClick = { onMoveIcon(index, index + 1) },
                                    enabled = index < iconNames.size - 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                        contentDescription = stringResource(R.string.move_icon_right),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // Remove
                            IconButton(
                                onClick = { onRemoveIcon(index) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.remove_icon, iconName),
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(name = "ActiveIconsManager Preview", showBackground = true)
@Composable
fun PreviewActiveIconsManager() {
    ContrastAwareReplyTheme {
        Surface {
            ActiveIconsManager(
                iconNames = listOf("button_ok", "flag-red", "unmapped_custom_icon"),
                onRemoveIcon = {},
                onMoveIcon = { _, _ -> },
                onClearAll = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
