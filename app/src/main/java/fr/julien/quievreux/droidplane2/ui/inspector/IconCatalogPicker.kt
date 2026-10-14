package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.model.NodeIconCategory
import fr.julien.quievreux.droidplane2.model.NodeIconRegistry
import fr.julien.quievreux.droidplane2.ui.components.NodeIconView
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

/**
 * Hybrid visual icon catalog browser featuring real-time text searching
 * and category filter chips to quickly select and append Freeplane icons to a node.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconCatalogPicker(
    onIconSelected: (iconId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(NodeIconCategory.ALL) }

    val filteredIcons = remember(searchQuery, selectedCategory) {
        NodeIconRegistry.filterIcons(query = searchQuery, category = selectedCategory)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.icon_catalog),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.search_icons_placeholder)) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = null
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Clear search"
                        )
                    }
                }
            },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Category Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            NodeIconCategory.entries.forEach { category ->
                FilterChip(
                    selected = selectedCategory == category,
                    onClick = { selectedCategory = category },
                    label = { Text(stringResource(category.titleRes)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Icon Grid
        if (filteredIcons.isEmpty()) {
            Text(
                text = "No icons found matching your search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                filteredIcons.forEach { descriptor ->
                    AssistChip(
                        onClick = { onIconSelected(descriptor.id) },
                        leadingIcon = {
                            NodeIconView(
                                visual = descriptor.visual,
                                contentDescription = descriptor.label,
                                size = 18.dp
                            )
                        },
                        label = {
                            Text(
                                text = descriptor.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                }
            }
        }
    }
}

@Preview(name = "IconCatalogPicker Preview", showBackground = true)
@Composable
fun PreviewIconCatalogPicker() {
    ContrastAwareReplyTheme {
        Surface {
            IconCatalogPicker(
                onIconSelected = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
