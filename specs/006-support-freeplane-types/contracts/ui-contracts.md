# UI Contract: Adaptive Node Properties Inspector

**Feature Branch**: `006-support-freeplane-types`  
**Feature Directory**: `specs/006-support-freeplane-types/contracts`  
**Status**: Completed  

## Overview

This contract governs presentation layer components in `:app`, including the adaptive `NodePropertiesInspector`, UI state models in `MainUiState`, and intent handlers in `MainViewModel`.

---

## 1. UI State Additions (`MainUiState`)

```kotlin
sealed class DialogType {
    data object None : DialogType()

    // Existing Dialogs
    data class EditNodeDescription(val node: Node, val oldValue: String) : DialogType()
    data class AddChildNode(val parentNode: Node) : DialogType()
    data class DeleteConfirmation(...) : DialogType()

    // Comprehensive Node Properties Inspector
    data class NodePropertiesInspector(
        val node: Node,
        val initialTab: InspectorTab = InspectorTab.CONTENT,
    ) : DialogType()
}

enum class InspectorTab {
    CONTENT,          // Title (Plain/Rich), Details, Notes, LaTeX
    ATTRIBUTES_LINKS, // Key-value Attributes table, Hyperlink / internal link
    STYLING_CLOUD,    // Colors (text, background), shape, font, cloud grouping
    EDGES_CONNECTORS, // Branch edge, arrowlink connectors
    ICONS_MEDIA,      // Icons selector, external object (image)
    SCRIPTS,          // Embedded script hooks & parameters (read/edit text)
}
```

---

## 2. ViewModel Intent Handlers (`MainViewModel`)

```kotlin
// Open comprehensive inspector for a given node
fun onOpenNodeInspector(node: Node, initialTab: InspectorTab = InspectorTab.CONTENT) {
    nodeManager.getNodeByID(node.id)?.let { freshNode ->
        setDialogState(
            DialogType.NodePropertiesInspector(
                node = freshNode,
                initialTab = initialTab,
            )
        )
    }
}

// Commit updated node properties
fun onSaveNodeProperties(updatedNode: Node) {
    viewModelScope.launch {
        nodeManager.updateNode(updatedNode)
        setDialogState(DialogType.None)
    }
}

// Dismiss inspector without saving
fun onDismissNodeInspector() {
    setDialogState(DialogType.None)
}
```

---

## 3. Adaptive Layout Rules

| Screen Configuration | Width Size Class | Inspector Component | Presentation Style |
| :--- | :--- | :--- | :--- |
| **Phone (Portrait)** | `Compact` (< 600dp) | `NodeInspectorBottomSheet` | Material 3 `ModalBottomSheet` anchored to bottom, swipeable, categorized tab bar. |
| **Tablet / Foldable / Landscape** | `Medium` or `Expanded` (>= 600dp) | `NodeInspectorPanel` | Sliding Side Sheet or multi-section side panel next to the canvas/list view. |

---

## 4. Context Menu Integration

In both `NodeList.kt` (List View) and `MindMapNodeCard.kt` (Canvas View), the context menu provides:
1. `Edit` -> Opens `NodePropertiesInspector` (Tab: `CONTENT`).
2. `Properties` / `Style & Details` -> Opens `NodePropertiesInspector`.
3. Quick actions (`Add Child`, `Copy Text`, `Open Link`, `Delete`) remain available for rapid workflow.
