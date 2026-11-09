package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import fr.julien.quievreux.droidplane2.model.NodeIconCategory
import fr.julien.quievreux.droidplane2.model.NodeIconDescriptor
import fr.julien.quievreux.droidplane2.model.NodeIconRegistry
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.res.stringResource
import fr.julien.quievreux.droidplane2.ui.components.NodeIconView
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

internal const val TEST_TAG_ICON_CATEGORY_PREFIX = "icon_category_section_"

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
    val categories = remember { NodeIconRegistry.categorizedEntries() }
    var expandedCategories by remember { mutableStateOf(setOf(NodeIconCategory.STATUS)) }

    val iconsByCategory = remember(searchQuery, categories) {
        categories.associateWith { category ->
            NodeIconRegistry.filterIcons(query = searchQuery, category = category)
        }
    }
    val totalFilteredCount = remember(iconsByCategory) {
        iconsByCategory.values.sumOf { it.size }
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

        Spacer(modifier = Modifier.height(10.dp))

        // Category Filter Chips
        // Icon Grid
        if (totalFilteredCount == 0) {
            Text(
                text = "No icons found matching your search.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val categoryIcons = iconsByCategory[category].orEmpty()
                    if (searchQuery.isBlank() || categoryIcons.isNotEmpty()) {
                        val isExpanded = if (searchQuery.isNotBlank()) {
                            true
                        } else {
                            expandedCategories.contains(category)
                        }
                        IconCategoryFoldableSubsection(
                            category = category,
                            icons = categoryIcons,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                expandedCategories = if (expandedCategories.contains(category)) {
                                    expandedCategories - category
                                } else {
                                    expandedCategories + category
                                }
                            },
                            onIconSelected = onIconSelected
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IconCategoryFoldableSubsection(
    category: NodeIconCategory,
    icons: List<NodeIconDescriptor>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onIconSelected: (iconId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("$TEST_TAG_ICON_CATEGORY_PREFIX${category.name}"),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggleExpand)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = stringResource(category.titleRes),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "(${icons.size})",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (isExpanded) {
                        Icons.Filled.KeyboardArrowDown
                    } else {
                        Icons.AutoMirrored.Filled.KeyboardArrowRight
                    },
                    contentDescription = stringResource(category.titleRes)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    icons.forEach { descriptor ->
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
}

@Preview
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

@Preview
@Composable
fun PreviewIconCategoryFoldableSubsection() {
    ContrastAwareReplyTheme {
        Surface {
            IconCategoryFoldableSubsection(
                category = NodeIconCategory.STATUS,
                icons = NodeIconRegistry.filterIcons(category = NodeIconCategory.STATUS).take(4),
                isExpanded = true,
                onToggleExpand = {},
                onIconSelected = {},
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
