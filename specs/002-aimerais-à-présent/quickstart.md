# Quickstart & End-to-End Validation Guide

**Feature**: [002-delete-nodes](spec.md)  
**Date**: 2026-10-04  

## Prerequisites
- Android Studio Ladybug or later / JDK 21
- Android SDK 35 build tools installed
- Emulator or physical device running Android 8.0+ (API 26+)
- Sample mindmap fixture (`test_map.mm`) with nested nodes

## Quick Build & Test Commands

Run the test suite across all modules:
```bash
./gradlew test
```

Run tests specifically for the data layer (deletion, link cleanup, undo):
```bash
./gradlew :data:test
```

Run tests for the presentation layer (ViewModel, dialogs):
```bash
./gradlew :app:test
```

---

## Runnable Validation Scenarios

### Scenario 1: Basic Node Deletion (US1 - P1)
**Objective**: Verify a node and its descendants are deleted and UI updates correctly.
1. Launch app with sample mindmap (`test_map.mm`).
2. Navigate to a parent node with at least 2 child nodes.
3. Open context menu on one child node (leaf or with children).
4. Select **Delete**.
5. **Expected**: Node disappears from list; parent's children list updates immediately.
6. Navigate away and back to parent → deleted node not present.
7. **Save** the mindmap, reopen → deletion persisted.

### Scenario 2: Delete Node with Children + Confirmation (US2 - P2)
**Objective**: Verify confirmation dialog shows for nodes with children.
1. Navigate to a node that has children.
2. Open context menu, select **Delete**.
3. **Expected**: Confirmation dialog appears with:
   - Title: "Delete Node?"
   - Message: "Delete '[Node Title]' and X descendants? This cannot be undone."
   - Buttons: "Delete" (red/error color), "Cancel"
4. Press **Cancel**.
   - **Expected**: Dialog dismisses; node remains unchanged.
5. Press **Delete**.
   - **Expected**: Dialog dismisses; node and all descendants removed from hierarchy.
6. Verify parent's children list no longer shows deleted node.

### Scenario 3: Undo Deletion (US1 + FR-006)
**Objective**: Verify deleted node can be restored via undo.
1. Delete a node with children (confirm deletion).
2. Press **Ctrl+Z** (or use undo menu action if available).
3. **Expected**: Node and all descendants restored to original position in hierarchy.
4. Verify node appears in parent's children list at original index.
5. Verify node content, links, and timestamps preserved.

### Scenario 4: Link Cleanup on Deletion (FR-008)
**Objective**: Verify links/arrow links are cleaned up.
1. Create a node with an external link (URL) and arrow link to another node.
2. Delete the node.
3. **Expected**: No dangling references remain.
4. Save and reopen mindmap → no errors loading; other nodes' arrow links no longer reference deleted node.

### Scenario 5: Root Node Protection (FR-003)
**Objective**: Verify root node cannot be deleted.
1. Navigate to root node.
2. Open context menu.
3. **Expected**: "Delete" option not present (or disabled).
4. If somehow triggered, deletion returns false; root remains.

### Scenario 6: Post-Deletion Navigation (FR-007)
**Objective**: Verify UI shows parent's children list with no selection.
1. Delete a child node.
2. **Expected**: View shows parent's children list.
3. No specific node is highlighted/selected.
4. User can immediately navigate to another child or use undo.

### Scenario 7: Accessibility (US3 - P3)
**Objective**: Verify delete option is accessible.
1. Enable TalkBack / VoiceOver.
2. Navigate to a non-root node.
3. Open context menu.
4. Navigate to "Delete" option.
   - **Expected**: Screen reader announces "Delete node [Title]".
5. Activate with Enter/Space.
   - **Expected**: Confirmation dialog opens with focus on "Cancel" button.

---

## Expected Test Results

| Scenario | Test Type | Pass Criteria |
|---|---|---|
| 1. Basic Deletion | Manual / UI Test | Node removed, UI syncs, save persists |
| 2. Confirmation Dialog | Manual / UI Test | Dialog shows correct count, Cancel/Delete work |
| 3. Undo | Manual / Unit Test | Subtree restored exactly |
| 4. Link Cleanup | Unit Test / Manual | No dangling refs, Freeplane loads clean |
| 5. Root Protection | Unit Test | Root cannot be deleted |
| 6. Navigation | Manual | Parent list shown, no selection |
| 7. Accessibility | Manual | TalkBack announces correctly |

---

## Troubleshooting

- **Dialog not appearing**: Check `DialogUiState.DeleteConfirmation` is set in `MainViewModel.onDeleteNode()`.
- **Undo not working**: Verify `DeleteSnapshot` captured before `NodeManager.deleteNode()`.
- **Link cleanup fails**: Check `NodeManager.deleteNode()` removes from `arrowLinkDestinationIds` and incoming lists.
- **Build errors**: Ensure `FontAwesomeIcons.Solid.Link` (or valid icon) used in `NodeList.kt` for delete action.