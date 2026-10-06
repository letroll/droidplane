# Phase 0 Research: Add Child Node to a Node in NodeList

- **Feature**: Add Child Node to a Node in NodeList
- **Branch**: `003-add-child-in-nodelist`
- **Date**: 2026-10-06

## Research Topics & Decisions

### 1. Dialog State and Parent Targeting
- **Context**: `MainActivity.kt` renders `CustomDialog` when `DialogType.AddChildNode(parentNode)` is active, but currently delegates to `viewModel.addNode(newValue)` without passing `parentNode`.
- **Decision**: Update `addNode` in `MainViewModel` to accept an explicit target `parentNode: Node?` (defaulting to `_uiState.value.nodeCurrentlyDisplayed`), and pass `dialog.parentNode` in `MainActivity.kt`.
- **Rationale**: Reuses the existing `DialogType.AddChildNode` payload (`parentNode: Node`) without creating redundant dialog models. Both the Floating Action Button and list item context menus feed into the same dialog flow cleanly.
- **Alternatives Considered**: Creating a distinct `AddChildToListItem` dialog type; rejected as unnecessary duplication since `DialogType.AddChildNode(val parentNode: Node)` already stores the exact target node.

### 2. Upward Ancestor Propagation on Node Addition
- **Context**: When a child is added to a node that is not the current screen parent (e.g. adding a child to a list item), the list item's parent (`nodeCurrentlyDisplayed`) and all ancestors up to `rootNode` must have their `childNodes` updated to contain the updated list item.
- **Decision**: Reuse `propagateUpdatedParentToRoot` in `NodeManager.addNodeToMindmap` when a child is added.
- **Rationale**: Guarantees bidirectional consistency (`childNodes` and `parentNode`) across the entire tree, preventing stale references when navigating up or saving.
- **Alternatives Considered**: Only updating `parentNode` in isolation; rejected because parent views and `rootNode` would retain stale references, causing desynchronization upon back navigation.

### 3. Screen View Update Behavior
- **Context**: When a child is added to a listed node, should the view navigate into the listed node, or stay on the current screen?
- **Decision**: The view stays on the current screen parent (`nodeCurrentlyDisplayed`). `MainViewModel` refreshes `nodeCurrentlyDisplayed` from `nodeManager.getNodeByID(currentNode.id)`, reflecting the updated child collection (e.g. child count and expand toggle icon).
- **Rationale**: In desktop Freeplane and mobile mindmap apps, adding a sub-node from a list view should not disrupt the user's browsing position unless they explicitly choose to navigate into it.
- **Alternatives Considered**: Automatically navigating down to the target node; rejected as disorienting when users are rapidly structuring multiple sibling categories.

### 4. Input Validation and Hygiene
- **Context**: Users might submit empty strings or whitespace.
- **Decision**: `NodeManager.addNodeToMindmap` already guards `if (newValue.isBlank()) return null`. In `MainViewModel`, validate `if (newValue.isNotBlank())` before launching mutation to avoid unnecessary UI loading states.
- **Rationale**: Minimal, defensive check preventing unnecessary dispatch and tree recomputation.

## Verification Strategy
- **Unit Tests (`:data`)**:
  - `addNodeToMindmap` when adding to a non-root child: verify target child's `childNodes` includes the new node, target child's parent includes the updated target child, and `rootNode` is updated.
- **Integration Tests (`:app`)**:
  - `MainViewModelTest`: verify adding a child to a listed node via `addNode(text, childNode)` preserves `nodeCurrentlyDisplayed`, updates child node's children list, and subsequent navigation into child node displays the new grandchild.
