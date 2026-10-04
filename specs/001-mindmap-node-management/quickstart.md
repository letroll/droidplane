# Quickstart & End-to-End Validation Guide

**Feature**: [001-mindmap-node-management](spec.md)  
**Date**: 2026-10-03  

## Prerequisites
- Android Studio Ladybug or later / JDK 21
- Android SDK 35 build tools installed
- Emulator or physical device running Android 8.0+ (API 26+)

## Quick Build & Test Commands

Run the test suite across all modules:
```bash
./gradlew test
```

Run tests specifically for the data layer (parsers, NodeManager, indexing):
```bash
./gradlew :data:test
```

Run tests for the presentation layer (ViewModels, UI state):
```bash
./gradlew :app:test
```

---

## Runnable Validation Scenarios

### Scenario 1: Mindmap Exploration & Hierarchy Navigation (P1)
**Objective**: Verify seamless navigation down through child subtrees and back up to root.
1. Launch the application with sample mindmap fixture (`test_map.mm`).
2. Verify top bar shows root node title: `"Droidplane Root"`.
3. Verify child nodes are rendered as formatted cards with icons and styled typography (bold/italic).
4. Tap child node `"Architecture"`.
   - **Expected**: Top bar updates to `"Architecture"`, and the list displays only direct descendants of `"Architecture"`.
5. Tap sub-child node `":core Module"`.
   - **Expected**: Top bar updates to `":core Module"`. If it has no children, an informative leaf node message is displayed.
6. Open menu and tap **Up**.
   - **Expected**: View ascends back to `"Architecture"`.
7. Open menu and tap **Top**.
   - **Expected**: View returns immediately to `"Droidplane Root"`.

### Scenario 2: Search Traversal Across Hierarchy (P2)
**Objective**: Verify search across multiple hierarchy levels without manual navigation.
1. Tap search icon in top bar.
2. Enter search query: `"XML"`.
   - **Expected**: Match count shows `1/3` (or total matching nodes). Active view jumps to first matching node.
3. Tap **Next Match** button.
   - **Expected**: Active view updates to the second matching node; match count shows `2/3`.
4. Tap **Search Back Arrow**.
   - **Expected**: Search toolbar closes, search highlights clear, and normal navigation mode resumes.

### Scenario 3: Child Node Creation (P3)
**Objective**: Verify adding a new child node updates the tree.
1. In the active node view, tap **Add Node** action.
2. Enter `"New Test Node"` and confirm.
   - **Expected**: A new card titled `"New Test Node"` appears in the current children listing.
   - **Expected**: Creation timestamp is set to current time.

### Scenario 4: Node Text Editing & Synchronized View Update (P3)
**Objective**: Verify that editing a node synchronizes across both its detail view and parent listing.
1. Long-press or click context menu on `"New Test Node"`.
2. Select **Edit**.
3. Change text to `"Updated Node Title"` and submit.
   - **Expected**: The node immediately displays `"Updated Node Title"`.
   - **Expected**: Navigate Up to parent, then back down; the node remains `"Updated Node Title"`.
   - **Expected**: Modification date is updated.

### Scenario 5: Freeplane Format Save & Verification (P3)
**Objective**: Verify non-destructive round-trip saving.
1. From top menu, select **Save**.
2. Inspect saved `.mm` file:
   - **Expected**: Newly added/edited nodes are serialized with correct XML attributes (`TEXT`, `CREATED`, `MODIFIED`).
   - **Expected**: All unchanged nodes, rich content, fonts, and icons remain identical to the original file.
