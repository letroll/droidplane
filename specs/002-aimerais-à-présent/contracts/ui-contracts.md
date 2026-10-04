# UI & ViewModel Contract (`:app` Module) - Deletion Extension

## Purpose
Specifies the interaction contract between presentation components (Jetpack Compose), `MainViewModel`, and deletion operations.

## Extended ViewModel Interaction Contract

```kotlin
package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import kotlinx.coroutines.flow.StateFlow

interface MainViewModelDeletionContract {

    /** Observable immutable UI state for Compose screens. */
    val uiState: StateFlow<MainUiState>

    /** Initiates deletion flow for a node (shows confirmation if has children). */
    fun onDeleteNode(node: Node)

    /** Confirms deletion after user presses "Delete" in confirmation dialog. */
    fun onConfirmDelete()

    /** Cancels deletion after user presses "Cancel" in confirmation dialog. */
    fun onCancelDelete()

    /** Undoes the last deletion (Ctrl+Z or menu action). */
    fun onUndoDelete()

    /** Checks if undo is available. */
    val canUndoDelete: Boolean
}
```

## Extended DialogUiState

```kotlin
sealed interface DialogType {
    data object None : DialogType
    data class EditNodeDescription(val node: Node, val oldValue: String) : DialogType
    data class AddChildNode(val parentNode: Node) : DialogType
    data class DeleteConfirmation(
        val node: Node,
        val descendantCount: Int,
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit
    ) : DialogType
}
```

## Context Menu Extension

```kotlin
sealed class ContextMenuAction {
    // ... existing actions ...
    data class DeleteNode(val node: Node) : ContextMenuAction()
}
```

## Compose Component Integration

### NodeList.kt - Context Menu Items
```kotlin
// Non-root nodes show Delete option
if (node.parentNode != null) {
    contextMenuItems.add(
        ContextMenuDropDownItem(
            text = stringResource(R.string.delete),
            action = DeleteNode(node = node)
        )
    )
}
```

### DeleteConfirmationDialog.kt
```kotlin
@Composable
fun DeleteConfirmationDialog(
    node: Node,
    descendantCount: Int,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Delete Node?") },
        text = {
            Text("Delete "${node.title}" and $descendantCount descendants? This cannot be undone.")
        },
        confirmButton = {
            TextButton(onClick = { onConfirm() }) {
                Text("Delete", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = { onCancel() }) {
                Text("Cancel")
            }
        }
    )
}
```

## Navigation Behavior Contract

| Scenario | Expected Behavior |
|---|---|
| Delete leaf node | Parent's children list displayed, no node selected |
| Delete parent with children | Confirmation dialog → on confirm: parent's children list displayed |
| Delete root node | Not allowed (FR-003); no action |
| Undo after deletion | Restored node selected in parent's children list |