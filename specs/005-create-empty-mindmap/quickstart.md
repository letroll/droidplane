# Quickstart & Validation Guide: Create Empty Mindmap with Demo Access

**Feature Branch**: `005-create-empty-mindmap`
**Feature Directory**: `specs/005-create-empty-mindmap`
**Status**: Ready for Validation

## Prerequisites

- Android SDK installed (`minSdk = 26`, `targetSdk = 35`)
- Java 21 toolchain configured
- Device or emulator running Android 8.0+

## Validation Scenarios

### Scenario 1: Create a New Empty Mindmap (US1)

1. **Launch App**: Open Droidplane (opens in default Mind Map view).
2. **Select New Mindmap**:
   - Tap the overflow menu (⋮) in the top bar.
   - Tap **"New Mindmap"** ("Nouvelle carte").
3. **Verify Clean Workspace**:
   - Verify that the previous document is cleared.
   - A single central root node labeled **"Central Idea"** (or localized "Nouvelle carte") appears centered on canvas.
   - Top bar title displays "Central Idea".
   - Navigation stack is reset; undo button is inactive.
4. **Author Nodes on New Map**:
   - Tap FAB or select "Add child node" from the root node's context menu.
   - Enter "First Topic" and confirm.
   - Verify the child branch attaches and renders cleanly.

---

### Scenario 2: Unsaved Changes Guard when Creating New Mindmap (US1)

1. **Modify Current Mindmap**:
   - Add a child node or edit text on the active mindmap.
   - Note that the document has unsaved modifications.
2. **Trigger New Mindmap**:
   - Tap the overflow menu (⋮) and select "New Mindmap".
3. **Verify Confirmation Dialog**:
   - Confirmation dialog appears: *"Discard unsaved changes?"*.
   - Tap **Cancel**: verify dialog dismisses and edits are kept intact.
   - Tap "New Mindmap" again, tap **Discard/Proceed**: verify clean new mindmap appears.

---

### Scenario 3: Access Bundled Help & Demo Mindmap (US2)

1. **Trigger Help & Demo**:
   - From an empty or edited mindmap, open the overflow menu (⋮).
   - Tap **"Help & Demo"** ("Aide / Démo").
2. **Verify Demo Loads**:
   - The complete tutorial mindmap (`example.mm`) is loaded into memory.
   - All guide nodes, formatting, and branches appear.
   - Links and navigation within the demo work properly.

---

### Scenario 4: Customize Central Topic (US3)

1. **Create New Mindmap**: Select "New Mindmap".
2. **Edit Root Text**:
   - Long-press the central root node, select "Edit".
   - Type "Project Roadmap" and confirm.
3. **Verify Immediate Update**:
   - Central node text updates to "Project Roadmap".
   - Top bar title updates to "Project Roadmap".
   - Document is marked as having unsaved changes.

---

### Scenario 5: Startup Chooser on App Open (US4)

1. **Launch App Normally**:
   - Launch Droidplane from launcher without an external file intent.
2. **Verify Startup Chooser Appears**:
   - Startup chooser dialog is presented with actions:
     - "Create New Mindmap"
     - "Browse File..."
     - "Help & Demo"
     - "Recent Files" list (if files were previously opened/saved and still exist on disk).
3. **Open Existing Recent File**:
   - Tap a listed recent file.
   - Chooser dismisses, and the selected file opens in Mind Map view.
4. **Deleted File Exclusion**:
   - Delete a recent file externally.
   - Restart the app.
   - Verify the deleted file is automatically excluded from the recent list.
