# Quickstart & Verification Guide: Freeplane Node Icons

**Feature Branch**: `008-manage-node-icons`
**Created**: 2026-10-14
**Status**: Ready

## 1. Automated Verification (JVM Unit Tests)

Verify icon resolution, name normalization, catalog filtering, and unmapped icon handling:

```bash
./gradlew testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.model.NodeIconRegistryTest"
```

Verify XML parsing and round-trip serialization of `<icon>` elements:

```bash
./gradlew :data:test --tests "fr.julien.quievreux.droidplane2.data.NodeManagerTest"
```

Execute all unit tests across modules:

```bash
./gradlew test
```

---

## 2. Interactive Verification Scenarios

### Scenario 1: Visual Display of Icons in MindMap Canvas and NodeList
1. Launch Droidplane on an emulator or device:
   ```bash
   ./gradlew installDebug
   ```
2. Open a mindmap with nodes that contain icons (or use the built-in demo map).
3. **Canvas View**:
   - Verify that assigned icons (e.g. `button_ok`, `flag-red`, `priority-1`) appear horizontally aligned before the node label.
   - Verify that structural icons (link, rich text, note) are displayed distinctly without interfering with Freeplane icons.
4. **Node List View**:
   - Verify that for nodes with 1 to 3 icons, the icons are displayed inline next to the title.
   - Verify that for nodes with more than 3 icons (e.g. 5 icons), exactly 3 icons are shown inline plus an overflow badge `+2`.

---

### Scenario 2: Managing Icons via Node Properties Inspector
1. Select a node in the mindmap and open its context menu or tap the inspector action.
2. Navigate to the **Icons & Media** tab:
   - **Active Icons**:
     - Verify all currently assigned icons are displayed with graphical previews and names.
     - Tap **Remove** (`X`) on an icon: confirm it disappears immediately from active icons.
     - Tap **Move Left** / **Move Right** on an icon: confirm its position shifts in the list.
     - Tap **Clear All**: confirm all assigned icons are removed.
   - **Icon Catalog**:
     - Verify filter chips: tap `Status & Flags`, `Priorities`, `Emoticons`, etc., and verify the grid filters to matching icons.
     - In the search field, type `"flag"`: verify all colored flags appear in the grid.
     - Type `"ok"`: verify `button_ok` appears.
     - Tap an icon in the catalog: verify it is immediately appended to the active icons list.
3. Save or dismiss the inspector and observe the updated node in both Canvas and List views.

---

### Scenario 3: Unmapped / Custom Freeplane Icon Handling
1. Open a test mindmap containing an unrecognized icon tag (e.g., `<icon BUILTIN="unsupported_icon_xyz"/>`).
2. Verify that on the canvas and in the list, the icon is rendered as a clean textual badge: `[unsupported_icon_xyz]`.
3. In the Inspector, verify the unmapped icon appears in the active icons list with its badge, and can still be reordered or removed.
4. Save the file and inspect the resulting XML to confirm `<icon BUILTIN="unsupported_icon_xyz"/>` is preserved verbatim.
