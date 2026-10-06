# UI Contracts: Mind Map View & View Mode Switching

- **Module**: `:app`
- **Classes**: `AppTopBar`, `MindMapCanvasScreen`, `MindMapNodeCard`, `MainActivity`

## TopBar Actions & Menu Contract

```kotlin
package fr.julien.quievreux.droidplane2.ui.components

enum class AppTopBarAction {
    SearchNext,
    SearchPrevious,
    Up,
    Top,
    Open,
    Help,
    Save,
    Undo,
    Backpress,
    ExitSearch,
    ToggleDisplayMode, // Added action
}
```

### Menu Item Rendering in `AppTopBar.kt`

```kotlin
DropdownMenuItem(
    onClick = {
        showMenu = false
        onBarAction(AppTopBarAction.ToggleDisplayMode)
    },
    text = {
        val label = if (displayMode == DisplayMode.LIST) {
            stringResource(R.string.switch_to_mindmap_view)
        } else {
            stringResource(R.string.switch_to_list_view)
        }
        Text(label)
    },
)
```

---

## MindMap Canvas Composable Contract

```kotlin
package fr.julien.quievreux.droidplane2.ui.mindmap

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContextMenuAction

/**
 * Renders the interactive 2D spatial mindmap canvas.
 */
@Composable
fun MindMapCanvasScreen(
    rootNode: Node,
    selectedNodeId: String?,
    collapsedNodeIds: Set<String>,
    onNodeSelect: (Node) -> Unit,
    onNodeToggleCollapse: (Node) -> Unit,
    onNodeContextMenuClick: (ContextMenuAction) -> Unit,
    modifier: Modifier = Modifier,
)
```

### Responsibilities:
1. **Layout Computation**: Invokes `MindMapLayoutEngine.computeLayout(rootNode, collapsedNodeIds, selectedNodeId)` to get node coordinates and branch connectors.
2. **Gesture Detection**: Applies `Modifier.pointerInput` with `detectTransformGestures` for continuous 2D pan and zoom gestures.
3. **Branch Rendering**: Draws smooth cubic Bezier connectors linking parent node departure edges to child arrival edges.
4. **Node Card Rendering & Dynamic Auto-Resizing**: Displays interactive node cards that dynamically auto-resize their dimensions based on text length, multiline wrapping (within min/max width bounds), font formatting, fold toggles, and selection outlines.
5. **Preview Support**: Includes `@Preview` composables for dark and light Material 3 themes.

---

## Screen View Switching Contract (`MainActivity.kt`)

```kotlin
content = { innerPadding ->
    when (state.value.displayMode) {
        DisplayMode.LIST -> {
            state.value.nodeCurrentlyDisplayed?.let { node ->
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .padding(innerPadding),
                ) {
                    // Classic hierarchical list rendering
                    nodeList(...)
                }
            }
        }
        DisplayMode.MIND_MAP -> {
            val root = viewModel.getRootNode() ?: state.value.nodeCurrentlyDisplayed
            root?.let { node ->
                MindMapCanvasScreen(
                    rootNode = node,
                    selectedNodeId = state.value.selectedNodeId,
                    collapsedNodeIds = state.value.collapsedNodeIds,
                    onNodeSelect = viewModel::selectNode,
                    onNodeToggleCollapse = viewModel::toggleNodeCollapse,
                    onNodeContextMenuClick = viewModel::onNodeContextMenuClick,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                )
            }
        }
    }
}
```
