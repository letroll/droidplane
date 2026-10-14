# Bug Assessment: Screen Rotation Data Loss

- **Slug**: screen-rotation-data-loss
- **Created**: 2026-10-14
- **Source**: pasted text ("lors d'une rotation d'écran, les données de la mindmap sont perdu")
- **Verdict**: valid
- **Severity**: critical

## Report (verbatim or summarized)

> "lors d'une rotation d'écran, les données de la mindmap sont perdu"
> (During a screen rotation, the mindmap data is lost).

## Symptom

When the user rotates their device (triggering an Android configuration change and activity recreation), the currently open and edited mindmap is completely wiped. The app resets to an empty root node ("Central Idea") and re-opens the Startup Chooser dialog, discarding all newly created or edited nodes and unsaved modifications.

## Reproduction

1. Launch Droidplane on an Android device or emulator in portrait mode.
2. Select "Create Empty Mindmap" or open an existing mindmap.
3. Add child nodes, edit node texts, or assign icons to nodes.
4. Rotate the device to landscape mode (or trigger orientation change via emulator/device rotation).
5. **Observed Behavior**: The modified mindmap disappears; the app displays an empty mindmap with "Central Idea" and presents the Startup Chooser dialog. All user additions and edits are lost.
6. **Expected Behavior**: The activity resumes with the exact same mindmap, displayed node, selection, navigation stack, and unsaved edits intact.

## Suspected Code Paths

- `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt:563-610` — In `onCreate(savedInstanceState: Bundle?)`, the initialization logic unconditionally executes on every activity creation:
  ```kotlin
  viewModel.apply {
      setFileRegister(this@MainActivity)
      if (isAnExternalMindMapEdit()) {
          ...
          lifecycleScope.launch {
              getDocumentInputStream(true)?.let { loadMindMap(it) }
          }
      } else {
          // Initialize an empty mindmap by default
          createEmptyMindmap(getString(R.string.default_root_node_title))
          val recentFiles = recentFilesRepository.getRecentFiles(...)
          showStartupChooser(...)
      }
  }
  ```
- `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt:200` — `savedInstanceState` is passed into `onCreate`, but never checked before running startup initialization.
- `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt:396-418` — `createEmptyMindmap()` creates a brand new root node, resets `hasUnsavedChanges = false`, clears the undo stack, and resets the `_uiState`.

## Root Cause Hypothesis

**Confidence: High (100%)**

Android destroys and recreates the `MainActivity` instance during screen rotation. While the `MainViewModel` (and its internal `NodeManager` instance) correctly survives configuration changes across activity recreation, `MainActivity.onCreate()` unconditionally calls `createEmptyMindmap()` and `showStartupChooser()` (or re-executes `loadMindMap()` for external intents) without checking whether `savedInstanceState == null` or whether `viewModel.getRootNode() != null`. Consequently, every screen rotation overwrites the surviving in-memory mindmap with a blank map and pops up the startup chooser.

## Proposed Remediation

**Preferred**:
In `MainActivity.kt`:
1. Guard the initialization block in `onCreate()` by checking `savedInstanceState == null` and ensuring the `viewModel` does not already hold an active mindmap (`viewModel.getRootNode() == null`).
2. Only execute `createEmptyMindmap()` and `showStartupChooser()` (or intent loading) on a genuine cold start (`savedInstanceState == null && viewModel.getRootNode() == null`).
3. Ensure `setFileRegister(this@MainActivity)` continues to be called on every `onCreate()` so that the surviving ViewModel maintains an active reference to the current activity's `FileRegister`.
4. Similarly, ensure the startup `triggerUpdateCheck(isManual = false)` only executes on cold start (`savedInstanceState == null`).

**Alternatives**:
- *Handle configuration changes manually via `android:configChanges="orientation|screenSize"` in `AndroidManifest.xml`*: Not recommended as it bypasses standard Android lifecycle handling and can cause resource configuration bugs (e.g. tablet vs. phone layouts).

**Files likely to change**:
- `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`
- `app/src/test/java/fr/julien/quievreux/droidplane2/MainActivityRotationTest.kt` (or ViewModel lifecycle test)

**Tests to add or update**:
- Test verifying that when an existing mindmap is loaded in `MainViewModel`, calling activity recreation logic or initialization with `savedInstanceState != null` does not overwrite or wipe `nodeManager.rootNode` and `uiState.value.nodeCurrentlyDisplayed`.

## Risks & Considerations

- **Low risk**: The change strictly prevents destructive overwriting of surviving ViewModel state during configuration changes.
- **Backwards compatibility**: Existing fresh launches (`savedInstanceState == null`) will behave identically.

## Open Questions

None.
