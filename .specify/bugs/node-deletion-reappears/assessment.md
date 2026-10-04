# Bug Assessment: Deleted Node Reappears After Navigation

- **Slug**: node-deletion-reappears
- **Created**: 2026-10-04T19:36:12.237214
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> la suppression de node ne le fait pas disparaitre de l'affichage après validation dans la boite de dialogue. De plus lorsque l'on sauvegarde le fichier avec la suppression et le réouvre. On ne voit plus le node dans la liste des enfants du node parent. Mais si l'on affiche l'un des autres enfant puis revient au parent, le node normalement préalablement supprimé réapparait.

Translation: Node deletion doesn't make the node disappear from the display after confirmation in the dialog. Additionally, when saving the file with the deletion and reopening it, the node is no longer visible in the parent's children list. But if you display one of the other children then return to the parent, the supposedly previously deleted node reappears.

## Symptom

After confirming node deletion in the dialog:
1. The deleted node doesn't immediately disappear from the display
2. After saving and reopening the file, the deleted node is correctly absent from the parent's children list
3. However, navigating to another child of the same parent and then returning to the parent causes the supposedly deleted node to reappear in the children list

## Reproduction

1. Open a mindmap with a parent node that has multiple children
2. Open the context menu on one of the children
3. Select "Delete" and confirm in the dialog
4. Observe that the deleted node doesn't immediately disappear from the display
5. Save the file and reopen it → deleted node is correctly absent
6. Navigate to another child of the same parent
7. Navigate back to the parent (using Up/Top navigation)
8. The supposedly deleted node reappears in the children list

## Suspected Code Paths

- `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt:770` — `deleteNode()` method: creates new parent with filtered children but doesn't update children's `parentNode` references
- `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt:870` — `restoreSubtree()` method: same issue with parent references
- `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/Node.kt:10` — `Node` data class with `parentNode: Node?` field that isn't updated when parent is copied
- `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt:266` — `showNode()` method: updates UI state with node reference
- `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt:165` — `onNavigateUp()`: uses `currentNode?.parentNode` which may be stale

## Root Cause Hypothesis

**Confidence: high**

When `NodeManager.deleteNode()` deletes a node, it creates a new parent node with filtered children using `node.copy(childNodes = newChildren)`. Since `Node` is a data class, `copy()` performs a shallow copy - the children in the new list are the same object references. Their `parentNode` field still points to the OLD parent object.

When `onNavigateUp()` is called, it uses `currentNode?.parentNode` to get the parent. Since the children's `parentNode` references weren't updated when the parent was copied, they still point to the OLD parent object (which still contains the deleted node in its children list).

The file save/load works correctly because serialization only writes the tree structure from the root, and the deleted node is properly excluded from the serialized tree.

**Confidence: high**

## Proposed Remediation

**Preferred**: Update children's `parentNode` references when creating a new parent in `deleteNode()` and `restoreSubtree()`.

In `NodeManager.deleteNode()`, after creating the new parent with filtered children, iterate through the surviving children and update their `parentNode` to point to the new parent. Similarly, in `restoreSubtree()`, ensure restored children have correct `parentNode` references.

**Files likely to change**:
- `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` — `deleteNode()` and `restoreSubtree()` methods
- `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/Node.kt` — consider adding a helper method to rebuild parent-child relationships

**Tests to add or update**:
- Unit test for `deleteNode()` verifying children's `parentNode` is updated
- Unit test for `restoreSubtree()` verifying parent references
- Integration test: delete node, navigate to sibling, navigate back → deleted node should not reappear

## Risks & Considerations

- **Performance**: Updating parent references for all descendants could be costly for large subtrees. Consider lazy evaluation or only updating direct children.
- **Immutability**: The current approach relies on data class immutability. Updating parent references requires creating new child nodes, which increases object allocation.
- **Arrow links**: Arrow link references (`arrowLinkDestinationNodes`, `arrowLinkIncomingNodes`) also contain node references that may become stale.
- **Serialization**: File save/load works correctly because it rebuilds the tree from root.

## Open Questions

- [NEEDS CLARIFICATION: Should `parentNode` be a `WeakReference` to avoid memory leaks and stale references?]
- [NEEDS CLARIFICATION: Should we add a `rebuildParentReferences()` method to Node for consistency?]
