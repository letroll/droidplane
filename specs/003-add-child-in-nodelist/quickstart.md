# Quickstart Guide: Add Child Node to a Node in NodeList

- **Feature**: Add Child Node to a Node in NodeList
- **Branch**: `003-add-child-in-nodelist`
- **Date**: 2026-10-06

## Overview

This guide details the validation steps to verify that users can add a child node directly to one of the listed child nodes in `NodeList` and that the tree hierarchy remains consistent.

## Prerequisites

- Android SDK installed (`$ANDROID_HOME` configured).
- JDK 21 installed and configured.
- Local repository built cleanly: `./gradlew compileDebugKotlin`.

## Validation Scenarios

### Scenario 1: Add Child Node to Listed Child via Context Menu (Automated)
Run the automated unit and integration tests:
```bash
./gradlew :data:testDebugUnitTest --tests "*addNode*"
./gradlew :app:testDebugUnitTest --tests "*addNode*"
```
**Expected Outcome**:
- `addNodeToMindmap` with non-root child node appends new child to that node and propagates ancestor updates to root.
- `MainViewModel.addNode(newValue, childNode)` leaves `nodeCurrentlyDisplayed` intact while updating the child's children list and visual indicators.

### Scenario 2: Full Regression Verification
Run all project unit tests across all modules:
```bash
./gradlew testDebugUnitTest
```
**Expected Outcome**:
- All 66+ test tasks succeed with 0 failures.

### Scenario 3: Manual On-Device / Emulator Flow
1. Open Droidplane with any multi-node mindmap (e.g. root with multiple children).
2. Long-press on one of the children in the list to open the context menu.
3. Select "Add child node" (`+` icon).
4. Enter text (e.g., "Sub-child 1") and click OK.
5. Notice that you remain on the current screen, and the selected child now displays the toggle indicator (`ToggleIcon`).
6. Click the child node to navigate into it.
7. Verify that "Sub-child 1" is listed as its child.
8. Click "Up" to return to the parent node.
