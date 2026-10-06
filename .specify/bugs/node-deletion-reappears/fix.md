# Bug Fix: Update Parent References on Node Deletion and Restoration

- **Slug**: node-deletion-reappears
- **Fixed**: 2026-10-06T12:00:00Z
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Synchronized `parentNode` references and rebuilt ancestor hierarchies up to `rootNode` when nodes are deleted or restored in `NodeManager`. This guarantees that navigating upward from surviving siblings or descendants references the updated parent instead of a stale instance containing deleted nodes.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` | modified | Added `updateDescendantParents` and `propagateUpdatedParentToRoot` to synchronize parent references down surviving subtrees and up the ancestor chain to `rootNode` in `deleteNode` and `restoreSubtree`. Rebuilt `_allNodes` and `mindmapIndexes`. |
| `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt` | modified | Added tests for `restoreSubtree` parent reference synchronization and end-to-end delete-navigate-back hierarchy integrity. |
| `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` | modified | Added integration test verifying that deleting a node, navigating to a sibling, and navigating up does not cause the deleted node to reappear in `MainUiState`. |

## Diff Highlights (optional)

```kotlin
// In NodeManager.kt: propagate updated parent to surviving children and ancestors
val directParent = getNodeByID(targetNode.parentNode?.id) ?: targetNode.parentNode!!
val updatedNodesMap = mutableMapOf<String, Node>()

val survivingChildren = directParent.childNodes.filter { it.id !in idsToDelete }
val updatedParent = directParent.copy(childNodes = mutableListOf())
val updatedChildren = survivingChildren.map { child ->
    val updatedChild = child.copy(parentNode = updatedParent)
    updatedNodesMap[updatedChild.id] = updatedChild
    updateDescendantParents(updatedChild, updatedNodesMap)
    updatedChild
}.toMutableList()
updatedParent.childNodes.addAll(updatedChildren)
updatedNodesMap[updatedParent.id] = updatedParent

propagateUpdatedParentToRoot(updatedParent, updatedNodesMap)

_allNodes.update { nodes ->
    nodes.filter { it.id !in idsToDelete }.map { node ->
        updatedNodesMap[node.id] ?: node
    }
}
```

## Tests Added or Updated

- `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt`:
  - `deleteNode should update parentNode references of surviving children` — pins down that surviving siblings reference the new parent instance and not the old one.
  - `deleteNode should not affect parentNode of nodes not in deleted subtree` — pins down that `rootNode.childNodes` is updated when a direct child or descendant is deleted.
  - `restoreSubtree should restore deleted subtree and update parentNode references` — pins down that restoring a deleted subtree restores the node in index and attaches correct parentNode references to restored nodes and siblings.
  - `delete node then navigate to sibling then navigate back should not cause deleted node to reappear` — pins down that navigating from sibling to parent using `parentNode`, `getNodeParent`, and `rootNode` does not expose the deleted node.
- `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`:
  - `delete node and navigate to sibling and back should not restore deleted node in UI state` — pins down the end-to-end UI state behavior on delete confirmation and back navigation.

## Local Verification

- Commands run:
  - `./gradlew :data:testDebugUnitTest` → PASS (all 40 tests passed)
  - `./gradlew :app:testDebugUnitTest` → PASS (all 15 tests passed)
  - `./gradlew testDebugUnitTest --rerun-tasks` → PASS (all 66 tasks across all modules passed)
- Manual checks: verified `git diff` to ensure only targeted changes in `NodeManager.kt`, `NodeManagerTest.kt`, and `MainViewModelTest.kt` were made.

## Deviations from Assessment

None. The preferred remediation of updating children's `parentNode` references and maintaining tree consistency was implemented directly in `NodeManager.kt`.

## Follow-ups

- Run `/speckit.bug.test slug=node-deletion-reappears` to generate the verification report.
