# Quickstart & Validation Guide: Replace Existing File on Save (`011-replace-existing-file-save`)

**Date**: 2026-10-16  
**Branch**: `011-replace-existing-file-save`  
**Spec**: [spec.md](./spec.md)

## 1. Running Automated Unit Tests

Run all unit tests across `:core`, `:data`, and `:app` (including `SaveTargetResolverTest`, `MainViewModelTest`, and `NodeManagerTest`):

```bash
./gradlew test
```

Run only the unit tests relevant to this feature:

```bash
./gradlew :app:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.helper.SaveTargetResolverTest" \
  --tests "fr.julien.quievreux.droidplane2.MainViewModelTest" \
  :data:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.data.NodeManagerTest"
```

---

## 2. Manual Verification Scenarios

### Scenario 1: Replace Existing File on Save (Confirm Overwrite)
1. Launch DroidPlane and open an existing `.mm` file (e.g., `test_map.mm`) via **Open** or **Recent Files**.
2. Modify any node text or add a new child node.
3. Open the top app bar menu and tap **Save**.
4. Observe the Android system file picker opens with `test_map.mm` pre-filled.
5. Tap **Save** in the system file picker.
6. Verify the **Replace existing file?** confirmation dialog appears naming `test_map.mm`.
7. Tap **Replace**.
8. **Expected Result**:
   - `test_map.mm` is updated in place with the new changes.
   - No `test_map (1).mm` file exists in the folder.
   - **Recent Files** shows a single entry for `test_map.mm` at the top.

### Scenario 2: Cancel Overwrite Confirmation
1. With `test_map.mm` still open, make another modification to a node.
2. Tap **Menu → Save**, then tap **Save** in the system file picker.
3. When the **Replace existing file?** confirmation dialog appears, tap **Cancel** (or press Back).
4. **Expected Result**:
   - The dialog closes and you return to the editor with your unsaved changes still in memory.
   - `test_map.mm` on disk still has its previous content.
   - No empty `test_map (1).mm` file remains in the folder.

### Scenario 3: Save Under a New Name ("Save As") or Save New Mindmap
1. While editing `test_map.mm`, tap **Menu → Save**.
2. In the system file picker, change the filename to `test_map_v2.mm` and tap **Save**.
3. **Expected Result**:
   - `test_map_v2.mm` is created and written without an overwrite confirmation prompt (unless `test_map_v2.mm` already existed).
   - `test_map.mm` remains untouched.
   - Subsequent saves in the same session now default to `test_map_v2.mm`.
