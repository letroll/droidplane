package fr.julien.quievreux.droidplane2.ui.inspector

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import fr.julien.quievreux.droidplane2.InspectorTab
import fr.julien.quievreux.droidplane2.R
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NodePropertiesInspector(
    node: Node,
    initialTab: InspectorTab = InspectorTab.CONTENT,
    onSave: (Node) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    onMoveCloudToParent: ((Node) -> Unit)? = null,
    contentTabContent: @Composable (Node, (Node) -> Unit) -> Unit = { n, onC -> ContentTab(node = n, onNodeChanged = onC) },
    attributesTabContent: @Composable (Node, (Node) -> Unit) -> Unit = { n, onC -> AttributesTab(node = n, onNodeChanged = onC) },
    stylingTabContent: @Composable (Node, (Node) -> Unit) -> Unit = { n, onC -> StylingTab(node = n, onNodeChanged = onC, onMoveCloudToParent = onMoveCloudToParent) },
    edgesConnectorsTabContent: @Composable (Node, (Node) -> Unit) -> Unit = { n, onC -> EdgesConnectorsTab(node = n, onNodeChanged = onC) },
    iconsMediaTabContent: @Composable (Node, (Node) -> Unit) -> Unit = { n, onC -> IconsMediaTab(node = n, onNodeChanged = onC) },
    scriptsTabContent: @Composable (Node, (Node) -> Unit) -> Unit = { n, onC -> ScriptsTab(node = n, onNodeChanged = onC) },
) {
    var currentNode by remember(node) { mutableStateOf(node) }
    var selectedTab by remember(initialTab) { mutableStateOf(initialTab) }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isExpandedScreen = maxWidth >= 600.dp

        if (isExpandedScreen) {
            Dialog(
                onDismissRequest = onDismiss,
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f)),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(520.dp)
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 6.dp
                    ) {
                        InspectorScaffold(
                            node = currentNode,
                            selectedTab = selectedTab,
                            onTabSelected = { selectedTab = it },
                            onNodeChanged = { currentNode = it },
                            onSave = { onSave(currentNode) },
                            onDismiss = onDismiss,
                            contentTabContent = contentTabContent,
                            attributesTabContent = attributesTabContent,
                            stylingTabContent = stylingTabContent,
                            edgesConnectorsTabContent = edgesConnectorsTabContent,
                            iconsMediaTabContent = iconsMediaTabContent,
                            scriptsTabContent = scriptsTabContent,
                        )
                    }
                }
            }
        } else {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                modifier = modifier
            ) {
                InspectorScaffold(
                    node = currentNode,
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it },
                    onNodeChanged = { currentNode = it },
                    onSave = { onSave(currentNode) },
                    onDismiss = onDismiss,
                    contentTabContent = contentTabContent,
                    attributesTabContent = attributesTabContent,
                    stylingTabContent = stylingTabContent,
                    edgesConnectorsTabContent = edgesConnectorsTabContent,
                    iconsMediaTabContent = iconsMediaTabContent,
                    scriptsTabContent = scriptsTabContent,
                    modifier = Modifier.fillMaxHeight(0.85f)
                )
            }
        }
    }
}

@Composable
fun InspectorScaffold(
    node: Node,
    selectedTab: InspectorTab,
    onTabSelected: (InspectorTab) -> Unit,
    onNodeChanged: (Node) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
    contentTabContent: @Composable (Node, (Node) -> Unit) -> Unit,
    attributesTabContent: @Composable (Node, (Node) -> Unit) -> Unit,
    stylingTabContent: @Composable (Node, (Node) -> Unit) -> Unit,
    edgesConnectorsTabContent: @Composable (Node, (Node) -> Unit) -> Unit,
    iconsMediaTabContent: @Composable (Node, (Node) -> Unit) -> Unit,
    scriptsTabContent: @Composable (Node, (Node) -> Unit) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.node_properties),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = node.text ?: node.id,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
                Button(onClick = onSave) {
                    Text(stringResource(R.string.save))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tabs
        val tabs = listOf(
            InspectorTab.CONTENT to stringResource(R.string.tab_content),
            InspectorTab.ATTRIBUTES_LINKS to stringResource(R.string.tab_attributes),
            InspectorTab.STYLING_CLOUD to stringResource(R.string.tab_styling),
            InspectorTab.EDGES_CONNECTORS to stringResource(R.string.tab_edges_connectors),
            InspectorTab.ICONS_MEDIA to stringResource(R.string.tab_icons_media),
            InspectorTab.SCRIPTS to stringResource(R.string.tab_scripts),
        )

        ScrollableTabRow(
            selectedTabIndex = tabs.indexOfFirst { it.first == selectedTab }.coerceAtLeast(0),
            edgePadding = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEach { (tab, title) ->
                Tab(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    text = { Text(text = title, fontSize = 13.sp) }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedTab) {
                InspectorTab.CONTENT -> contentTabContent(node, onNodeChanged)
                InspectorTab.ATTRIBUTES_LINKS -> attributesTabContent(node, onNodeChanged)
                InspectorTab.STYLING_CLOUD -> stylingTabContent(node, onNodeChanged)
                InspectorTab.EDGES_CONNECTORS -> edgesConnectorsTabContent(node, onNodeChanged)
                InspectorTab.ICONS_MEDIA -> iconsMediaTabContent(node, onNodeChanged)
                InspectorTab.SCRIPTS -> scriptsTabContent(node, onNodeChanged)
            }
        }
    }
}

@Composable
private fun DefaultTabPlaceholder(title: String, node: Node) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$title section for ${node.text ?: node.id}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(name = "Inspector Scaffold Preview")
@Composable
fun PreviewInspectorScaffold() {
    ContrastAwareReplyTheme {
        Surface {
            InspectorScaffold(
                node = Node(
                    parentNode = null,
                    id = "ID_123",
                    numericId = 123,
                    text = "Sample Mindmap Topic",
                    creationDate = 0L,
                    modificationDate = 0L
                ),
                selectedTab = InspectorTab.CONTENT,
                onTabSelected = {},
                onNodeChanged = {},
                onSave = {},
                onDismiss = {},
                contentTabContent = { n, _ -> DefaultTabPlaceholder("Content", n) },
                attributesTabContent = { n, _ -> DefaultTabPlaceholder("Attributes", n) },
                stylingTabContent = { n, _ -> DefaultTabPlaceholder("Styling", n) },
                edgesConnectorsTabContent = { n, _ -> DefaultTabPlaceholder("Connectors", n) },
                iconsMediaTabContent = { n, _ -> DefaultTabPlaceholder("Media", n) },
                scriptsTabContent = { n, _ -> DefaultTabPlaceholder("Scripts", n) },
            )
        }
    }
}
