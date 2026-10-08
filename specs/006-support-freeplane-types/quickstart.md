# Quickstart Validation Guide: Freeplane Document Types Support & Editing

**Feature Branch**: `006-support-freeplane-types`  
**Feature Directory**: `specs/006-support-freeplane-types`  
**Status**: Ready for Implementation  

## Overview

This guide outlines end-to-end verification workflows and automated test commands to validate support and editing for all Freeplane document elements in Droidplane.

---

## Prerequisites

- JDK 21 installed and configured (`JAVA_HOME`).
- Android SDK installed (Android 15 / API 35 SDK components).
- Clean working tree on branch `006-support-freeplane-types`.

---

## Automated Test Execution

Run the full automated test suite verifying XML parsing, serialization round-trip fidelity, and property updates:

```bash
# Run all unit tests in the project
./gradlew test

# Run isolated data module tests for XML parsing and NodeManager
./gradlew :data:test --tests "fr.julien.quievreux.droidplane2.data.NodeManagerTest"
./gradlew :data:test --tests "fr.julien.quievreux.droidplane2.data.FreeplaneXmlRoundTripTest"

# Run ViewModel and UI integration tests
./gradlew :app:test --tests "fr.julien.quievreux.droidplane2.MainViewModelTest"
```

Expected Outcome: All tests compile and pass with zero failures.

---

## Manual End-to-End Validation Scenarios

### Scenario 1: Comprehensive Node Property Editing via Inspector
1. **Launch App**: Open Droidplane on an Android device or emulator.
2. **Open Existing Map**: Load `test/Rich Text/richtext.mm` or create a new mind map.
3. **Trigger Inspector**: Long-press any node or tap its context menu (`...`) and select **Edit / Properties**.
4. **Edit Content (Tab 1)**:
   - Edit the node title.
   - Switch to **Details**: Add text "Priority tasks for Q4".
   - Switch to **Notes**: Add note text with bullet points using the hybrid editor.
   - Switch to **Formula**: Enter LaTeX `\alpha + \beta = \gamma`.
5. **Edit Attributes & Links (Tab 2)**:
   - Tap **Add Attribute**: Enter Name = "Owner", Value = "Development Team".
   - Tap **Add Attribute**: Enter Name = "Progress", Value = "75%".
   - Set Hyperlink to `https://www.freeplane.org`.
6. **Customize Styling & Cloud (Tab 3)**:
   - Select node shape: **Bubble**.
   - Change Background Color to `#E3F2FD` (soft blue).
   - Change Text Color to `#0D47A1` (dark blue).
   - Enable **Cloud Grouping**: Color `#FFF3E0` (soft orange), Shape `ROUND_RECT`.
7. **Configure Edge & Connectors (Tab 4)**:
   - Change parent branch edge style to `bezier` with width `2`.
   - Add a connector to another node with label "related".
8. **Save Changes**: Tap **Done / Save** in the Inspector.
9. **Verification**:
   - The node card updates immediately with the new background color, bubble shape, and cloud envelope.
   - Node list and canvas reflect the modified content and child indicators.

---

### Scenario 2: Round-Trip Save and Desktop Freeplane Interoperability
1. **Save Document**: Tap the **Save** action in the top bar.
2. **Inspect Saved XML (`.mm`)**:
   - Verify all tags are written in the correct canonical sequence:
     - `<edge .../>`, `<font .../>`, `<cloud .../>`, `<richcontent TYPE="DETAILS">`, `<richcontent TYPE="NOTE">`, `<attribute .../>`, `<arrowlink .../>`, `<hook .../>`.
   - Verify unedited desktop hooks (`MapStyle`, scripts) are preserved byte-for-byte.
3. **Reload in Desktop Freeplane**:
   - Open the saved `.mm` file in desktop Freeplane.
   - Confirm that notes, details, attributes, clouds, and colors display correctly without warnings.

---

### Scenario 3: Adaptive Layout Responsiveness
1. **Compact Phone View**: Open inspector in portrait mode on a phone screen (< 600dp width) -> Displays as `ModalBottomSheet`.
2. **Tablet / Landscape View**: Rotate device to landscape or run on a tablet (>= 600dp width) -> Inspector displays as a sliding side panel or multi-section inspector preserving map view context.
