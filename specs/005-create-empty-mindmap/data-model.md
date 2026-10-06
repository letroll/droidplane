# Data Model: Create Empty Mindmap with Demo Access

**Feature Branch**: `005-create-empty-mindmap`
**Feature Directory**: `specs/005-create-empty-mindmap`
**Status**: Completed

## Overview

This document specifies the data model additions, state representations, and transitions required for initializing empty mindmaps, accessing the demo document, and protecting unsaved changes.

---

## Dialog State Extensions (`MainUiState.kt`)

```kotlin
sealed class DialogType {
    data object None : DialogType()
    data class EditNodeDescription(...) : DialogType()
    data class AddChildNode(...) : DialogType()
    data class DeleteConfirmation(...) : DialogType()
    data class ExitConfirmation(...) : DialogType()

    /**
     * Confirmation dialog presented when an action (e.g. New Mindmap or Load Demo)
     * would discard unsaved modifications on the current mindmap.
     */
    data class DiscardConfirmation(
        val onConfirm: () -> Unit,
        val onCancel: () -> Unit,
    ) : DialogType()

    /**
     * Dialog presented upon app startup offering to create a new mindmap,
     * open an existing recent file, browse storage, or view the demo.
     */
    data class StartupChooser(
        val recentFiles: List<RecentFile>,
        val onNewMindmap: () -> Unit,
        val onOpenRecent: (RecentFile) -> Unit,
        val onBrowse: () -> Unit,
        val onOpenDemo: () -> Unit,
    ) : DialogType()
}

---

## Recent File Model (`RecentFile.kt`)

```kotlin
package fr.julien.quievreux.droidplane2.model

data class RecentFile(
    val uriString: String,
    val displayName: String,
    val timestamp: Long = System.currentTimeMillis(),
)
```

- **Validation Rules**:
  - `uriString` must be a valid Android URI string or file path.
  - `displayName` must not be blank.
  - Filtered dynamically before presentation: only files that currently exist and can be opened are included.
```

- **Validation Rules**:
  - `onConfirm` and `onCancel` callbacks must be non-null.
  - When confirmed, `hasUnsavedChanges` is reset and the pending action is executed.
  - When cancelled, the dialog is dismissed and the current document state remains unchanged.

---

## Domain Model (`NodeManagerContract.kt`)

```kotlin
interface NodeManagerContract {
    // ... existing members ...

    /**
     * Creates and initializes a new empty mindmap in memory consisting of a single root node.
     * Clears previous index structures, resets search streams, and updates [allNodes] StateFlow.
     *
     * @param rootTitle Text label for the central root node (defaults to "Central Idea").
     * @return The newly created root [Node].
     */
    fun createNewMindmap(rootTitle: String = "Central Idea"): Node
}
```

- **Invariants**:
  - `rootNode` is set to the newly created root `Node`.
  - `rootNode.parentNode` is strictly `null`.
  - `rootNode.childNodes` is initialized as an empty mutable list.
  - `_allNodes.value` contains exactly `listOf(rootNode)`.
  - `currentMindMapUri` is reset to `null`.

---

## State Transition Diagram

```text
[User selects "New Mindmap"]
            │
            ▼
   Has Unsaved Changes?
      ├── YES ──► Display DialogType.DiscardConfirmation
      │                 ├── Cancel ──► Dismiss dialog (no state change)
      │                 └── Confirm ──► Clear unsaved edits & proceed
      │
      └── NO  ──► NodeManager.createNewMindmap("Central Idea")
                        │
                        ▼
                  updateUiState:
                    - nodeCurrentlyDisplayed = newRoot
                    - selectedNodeId = newRoot.id
                    - collapsedNodeIds = emptySet()
                    - navigationStack = listOf(newRoot.id)
                    - title = "Central Idea"
                    - treeVersion = treeVersion + 1
                    - hasUnsavedChanges = false
                    - canUndoDelete = false
```
