# Research & Technical Decisions: Create Empty Mindmap with Demo Access

**Feature Branch**: `005-create-empty-mindmap`
**Feature Directory**: `specs/005-create-empty-mindmap`
**Status**: Completed

## Overview

This document analyzes the architectural and design decisions for enabling users to create a clean, empty mindmap directly within Droidplane, while preserving continuous access to the bundled tutorial/demo mindmap (`example.mm`).

---

## Decision 1: Empty Mindmap In-Memory Initialization Strategy

### Context
Droidplane previously initialized its state upon application launch by loading the bundled `example.mm` raw resource. There was no mechanism to create an empty document without editing an existing file or manually deleting tutorial nodes.

### Evaluation
- **Option A: Re-parsing a minimal XML template from assets/raw**:
  - Requires maintaining a static `empty.mm` XML asset with `<map><node text="Central Idea"/></map>`.
  - Incurs XML parser overhead for a trivial 1-node tree.
- **Option B: Programmatic root node creation in `NodeManager`**:
  - Constructs a fresh root `Node(id = "ID_...", text = rootTitle, parentNode = null, childNodes = mutableListOf())`.
  - Re-indexes the node table and resets `_allNodes` StateFlow immediately.
  - Zero I/O overhead, instantaneous execution (<5ms), robust and fully testable in pure JVM unit tests without Android XML dependencies.

### Decision
**Option B: Programmatic initialization via `NodeManager.createNewMindmap(title: String)`**. This directly initializes an in-memory document with a single root node, clears residual indexes, and updates `_allNodes` reactively.

---

## Decision 2: Preserving Access to the Help & Demo Mindmap

### Context
The user specified that the demo/help file must remain accessible: *"tout en préservant la possibilité d'accéder au fichier d'aide servant aussi de démo"*.

### Decision
1. Enhance the top bar menu action `Help` (labeled "Help & Demo" / "Aide / Démo").
2. When triggered:
   - If unsaved modifications exist on the active document, display a discard confirmation dialog.
   - If confirmed or if no unsaved changes exist, invoke `loadMindMap(resources.openRawResource(R.raw.example))` and reset the map URI to `android.resource://$packageName/raw/example.mm`.
3. Ensures users can switch freely between working on their personal mindmaps and referencing the tutorial/demo.

---

## Decision 3: Unsaved Changes Protection (Guard Dialog)

### Context
When a user has made edits to the current document and chooses "New Mindmap" or "Help & Demo", replacing the document in memory without confirmation would cause silent data loss.

### Decision
1. Add `DiscardConfirmation` to `MainUiState.DialogType`:
   ```kotlin
   data class DiscardConfirmation(
       val titleRes: Int,
       val messageRes: Int,
       val onConfirm: () -> Unit,
       val onCancel: () -> Unit,
   ) : DialogType()
   ```
2. When the user requests `NewMindmap` or `Help`:
   - If `hasUnsavedChangesState` is `true`: set dialog state to `DiscardConfirmation` with callbacks to proceed (clearing dirty state and executing the action) or cancel (keeping current mindmap).
   - If `hasUnsavedChangesState` is `false`: execute immediately.

### Rationale
- Satisfies Droidplane Constitution Principle IV (Data Integrity).
- Familiar and standard UX pattern for document-based applications.

---

## Decision 4: TopBar Menu Structure & Action Mapping

### Context
The options menu in `AppTopBar.kt` contains `Open`, `Save`, `Undo`, `ToggleDisplayMode`, and `Help`.

### Decision
Add a top-level menu item `New` / `New Mindmap` (`R.string.new_mindmap`) at the top of the dropdown menu:
- `New Mindmap`
- `Open`
- `Save`
- `Undo`
- `Switch to List view / Mind Map view`
- `Help & Demo`

Add action `AppTopBarAction.NewMindmap` to `AppTopBarAction` enum and route it in `MainActivity.kt` to `viewModel.onNewMindmapRequested()`.
