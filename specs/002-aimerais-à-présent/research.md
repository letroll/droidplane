# Phase 0 Research: Delete Mindmap Nodes

## 1. Deletion Architecture & State Management

### Decision
Extend existing `NodeManager.deleteNode(nodeId: String): Boolean` in `:data` module as the single source of truth for deletion. `MainViewModel` in `:app` will call this method and handle UI state transitions (navigation, undo stack, confirmation dialog).

### Rationale
- **Constitutional Alignment**: Principle I (Modular Separation) - deletion logic belongs in `:data`. Principle II (Unidirectional Flow) - `MainViewModel` observes state via `StateFlow`.
- **Reusability**: Centralizing in `NodeManager` allows JVM unit tests without Android framework.
- **Existing Implementation**: `deleteNode` already exists and handles recursive removal from hierarchy and indexes.

### Alternatives Considered
- **Direct ViewModel mutation**: Rejected - violates Principle II, causes desynchronization.
- **New DeleteManager class**: Rejected - YAGNI (Principle V); `NodeManager` already handles CRUD.

---

## 2. Confirmation Dialog & UX Flow

### Decision
Implement confirmation dialog as a Compose dialog (using `:core` `CustomDialog`) triggered when user selects "Delete" from context menu for a node with children. Dialog shows node title, descendant count, and irreversible warning. Buttons: "Delete" (destructive) and "Cancel". Cancellation dismisses with no action.

### Rationale
- **User Safety**: Explicit confirmation with descendant count prevents accidental bulk deletion.
- **Consistency**: Matches existing dialog patterns in app (`EditDescriptionDialog`).
- **Accessibility**: Proper semantics for screen readers.

### Alternatives Considered
- **Toast/snackbar undo only**: Rejected - less discoverable, no pre-action confirmation.
- **Native Android AlertDialog**: Rejected - not Compose-native, harder to test.

---

## 3. Undo Implementation

### Decision
Implement undo via state snapshots in `MainViewModel`. Before deletion, capture the deleted node subtree and parent state. Provide `undoDelete()` callable via Ctrl+Z (via `KeyEvent` handler) and menu action. Undo restores subtree to `NodeManager` and updates UI state. Undo stack limited to last deletion in current session.

### Rationale
- **Principle II**: Undo is a state mutation, must go through `NodeManager`.
- **Principle III**: Undo logic testable via JVM unit tests.
- **User Expectation**: Standard Ctrl+Z pattern for destructive actions.

### Alternatives Considered
- **Memento pattern with full document snapshots**: Rejected - memory overhead for large mindmaps.
- **Persistent undo log to disk**: Rejected - over-engineering for session-only requirement.

---

## 4. Link & Arrow Link Cleanup

### Decision
During deletion, `NodeManager.deleteNode()` will:
1. Remove the deleted node's `link` (external URI).
2. Remove deleted node's ID from all other nodes' `arrowLinkDestinationIds` and `arrowLinkIncomingNodes`.
3. Clear `arrowLinkDestinationNodes` and `arrowLinkIncomingNodes` collections.
This prevents dangling references while maintaining document validity for Freeplane.

### Rationale
- **Principle IV**: Data integrity - no orphaned references in saved `.mm` files.
- **Consistency**: Freeplane doesn't support dangling links; cleanup ensures round-trip compatibility.

### Alternatives Considered
- **Leave dangling, handle at UI/load time**: Rejected - violates Principle IV, causes load errors in desktop Freeplane.
- **Only clean internal arrow links, keep external links**: Rejected - external links on deleted node are meaningless.

---

## 5. Post-Deletion Navigation

### Decision
After deletion, `MainViewModel` calls `showNode(parentNode)` where `parentNode` is the deleted node's parent. The UI displays the parent's children list with no specific node selected (selection cleared). User can then navigate or use undo.

### Rationale
- **Consistency**: Matches "Up" navigation behavior; parent context is preserved.
- **Simplicity**: No complex sibling-selection logic needed.
- **Undo-friendly**: Parent stays visible, making undo intuitive.

### Alternatives Considered
- **Select next/previous sibling**: Rejected - fragile if order changes; confusing if many siblings.
- **Return to root**: Rejected - loses context, more navigation steps.

---

## 5. Test Strategy

### Decision
- **JVM Unit Tests** (`:data`): `NodeManagerTest` - recursive deletion, index consistency, link cleanup, undo snapshot restore.
- **ViewModel Tests** (`:app`): `MainViewModelTest` - confirmation dialog state, deletion flow, undo, navigation.
- **UI Tests** (`:app`): Compose tests for context menu, dialog, accessibility.

### Rationale
- **Principle III**: All business logic tested on JVM; UI logic verified with Compose tests.
- **Fast Feedback**: JVM tests run in seconds; no emulator needed for core logic.

### Alternatives Considered
- **Espresso/UI Automator**: Rejected - slower, flakier; Compose UI tests are sufficient for this scope.