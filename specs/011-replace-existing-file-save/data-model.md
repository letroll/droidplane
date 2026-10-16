# Data Model: Replace Existing File on Save (`011-replace-existing-file-save`)

**Date**: 2026-10-16  
**Branch**: `011-replace-existing-file-save`  
**Spec**: [spec.md](./spec.md)

## 1. Domain & UI State Models

### 1.1 Active Document Metadata (`NodeManager` in `:data` & `MainViewModel` in `:app`)

Tracks the identity of the currently open mindmap document so save operations can distinguish between a brand-new / demo mindmap and an existing user file.

| Property | Type | Module | Description |
|----------|------|--------|-------------|
| `currentMindMapUri` | `Uri?` | `:data` (`NodeManager`) | The SAF `content://` or `file://` URI of the currently loaded mindmap, `android.resource://...` for the bundled demo map, or `null` for a newly created mindmap. |
| `currentMindMapDisplayName` | `String?` | `:data` (`NodeManager`) | The resolved display filename (e.g., `"project.mm"`) obtained from `OpenableColumns.DISPLAY_NAME` or `RecentFilesRepository` when the file was opened or last saved. `null` when `currentMindMapUri` is `null` or points to the demo resource. |
| `isEditingExistingUserFile` | `Boolean` (computed) | `:app` / `:data` | `true` iff `currentMindMapUri != null && currentMindMapUri.scheme != "android.resource"`. |

**Lifecycle & State Transitions**:
- **App Launch (Demo Map loaded)**: `currentMindMapUri = android.resource://.../example.mm`, `currentMindMapDisplayName = null` → `isEditingExistingUserFile = false`.
- **New Mindmap (`createNewMindmap`)**: `currentMindMapUri = null`, `currentMindMapDisplayName = null` → `isEditingExistingUserFile = false`.
- **Open File (`openFileLauncher` / Recent File / Incoming Intent)**: `currentMindMapUri = <uri>`, `currentMindMapDisplayName = <resolved display name>` → `isEditingExistingUserFile = true`.
- **Save New File Completed (`WriteNewFile`)**: `currentMindMapUri = <pickedUri>`, `currentMindMapDisplayName = <pickedDisplayName>` → `isEditingExistingUserFile = true` (subsequent saves in the same session now target this file).
- **Overwrite Existing File Confirmed (`ConfirmOverwrite` / `DirectOverwrite`)**: `currentMindMapUri` and `currentMindMapDisplayName` remain `<originalUri>` and `<originalDisplayName>`.

---

### 1.2 Save Target Resolution (`SaveTargetResolver` & `SaveResolution` in `:app`)

Pure Kotlin model representing the outcome of comparing the active document with the file picker result.

```kotlin
package fr.julien.quievreux.droidplane2.helper

/**
 * Outcome of resolving the URI and display name returned by the system file picker
 * against the currently active mindmap document.
 */
sealed interface SaveResolution {
    /**
     * The user is saving a new mindmap, saving the demo mindmap, or explicitly changed
     * the filename in the system picker to create a separate file ("Save As" behavior).
     */
    data class WriteNewFile(
        val targetUriString: String,
        val displayName: String,
    ) : SaveResolution

    /**
     * The user explicitly selected the exact same underlying document URI in the system
     * picker (the OS picker already prompted for overwrite and returned originalUri).
     */
    data class DirectOverwrite(
        val targetUriString: String,
        val displayName: String,
    ) : SaveResolution

    /**
     * The user validated the pre-filled filename in the system picker while editing an
     * existing file, causing Android's DocumentsProvider to create a temporary duplicate
     * URI (often with an incremented "(n)" suffix). Requires user confirmation before
     * overwriting [originalUriString] and deleting [duplicatePickerUriString].
     */
    data class ConfirmOverwrite(
        val originalUriString: String,
        val originalDisplayName: String,
        val duplicatePickerUriString: String,
        val duplicateDisplayName: String,
    ) : SaveResolution
}
```

#### Filename Matching Rules (`SaveTargetResolver.isSameOrAutoIncrementedFileName`)
Given `originalFileName` (e.g., `"carte.mm"`) and `pickedFileName`:
1. **Exact Match**: `pickedFileName.equals(originalFileName, ignoreCase = false)` → `true`.
2. **Base + `(n)` + Extension Match**:
   - Split `originalFileName` into `baseName` (`"carte"`) and `extension` (`"mm"`, if `.` is present).
   - Pattern: `^${Regex.escape(baseName)}\s*\([1-9]\d*\)(?:\.${Regex.escape(extension)})?$`
   - Matches `"carte (1).mm"`, `"carte (2).mm"`, `"carte (10).mm"`, and `"carte (1)"`.
3. **Full Name + `(n)` Match** (produced by some Android `DocumentsProvider` implementations when MIME type is `application/octet-stream`):
   - Pattern: `^${Regex.escape(originalFileName)}\s*\([1-9]\d*\)$`
   - Matches `"carte.mm (1)"`, `"carte.mm (2)"`.
4. **Different Base Name**:
   - Example: `originalFileName = "carte.mm"`, `pickedFileName = "carte_v2.mm"` or `"autre (1).mm"` → `false` (resolved as `WriteNewFile`).

---

### 1.3 Dialog State (`MainUiState.DialogType.OverwriteConfirmation` in `:app`)

Extends the sealed interface `MainUiState.DialogType` in `MainUiState.kt`:

```kotlin
data class OverwriteConfirmation(
    val fileName: String,
    val onConfirm: () -> Unit,
    val onCancel: () -> Unit,
) : DialogType
```

| Field | Type | Validation / Purpose |
|-------|------|----------------------|
| `fileName` | `String` | Non-blank display name of the existing file to be replaced (e.g., `"carte.mm"`), displayed in the confirmation dialog body. |
| `onConfirm` | `() -> Unit` | Callback invoked when the user confirms replacing the existing file. Truncates and writes to `originalUri`, deletes `duplicatePickerUri`, updates recent files, clears `hasUnsavedChanges`, and dismisses the dialog. |
| `onCancel` | `() -> Unit` | Callback invoked when the user cancels or dismisses the dialog. Deletes the empty `duplicatePickerUri`, leaves `originalUri` intact, preserves `hasUnsavedChanges = true`, and dismisses the dialog. |
