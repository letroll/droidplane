# Phase 0 Research: Replace Existing File on Save (`011-replace-existing-file-save`)

**Date**: 2026-10-16  
**Branch**: `011-replace-existing-file-save`  
**Spec**: [spec.md](./spec.md)

## 1. Root Cause Analysis of `(n)` Duplicate Creation

### Context
Currently, when a user opens an existing `.mm` file (e.g., `carte.mm`) and saves modifications via **Menu → Save** or the unsaved-changes confirmation flow (**Save** before Exit/New/Open), `MainViewModel.launchSaveFile()` serializes the mindmap into a local cache file (`cacheDir/carte.mm`) and calls `MainActivity.registerFile(file)`. `MainActivity` then launches `Intent(Intent.ACTION_CREATE_DOCUMENT)` with `Intent.EXTRA_TITLE = "carte.mm"`.

### Why `(n)` Duplicates Happen in Android SAF (`DocumentsUI`)
1. **`ACTION_CREATE_DOCUMENT` creates a brand-new document by default**: When the user clicks the **Save** button in the system file picker without explicitly tapping an existing file row in the directory list, Android's `ExternalStorageProvider` / `DownloadsStorageProvider` invokes `DocumentsProvider.createDocument(parentDocumentId, mimeType, displayName)`. Because a file named `carte.mm` already exists in that directory, Android's `FileUtils.buildUniqueFileWithExtension()` automatically appends an incremental suffix: `carte (1).mm`, `carte (2).mm` (or `carte.mm (1)` when MIME type `application/octet-stream` does not map to `.mm` in Android's `MimeTypeMap`).
2. **Missing Write Permission on Open**: In `MainActivity.openFileLauncher`, `contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)` only takes **read** permission. Without `FLAG_GRANT_WRITE_URI_PERMISSION`, the app cannot write back directly to the original document `Uri`.
3. **Unresolved Display Name from SAF `Uri`**: `NodeManager.getMindmapFileName()` uses `currentMindMapUri?.path?.substringAfterLast("/")`. For SAF `content://` URIs, `uri.path` often returns `primary:Download/carte.mm` or a numeric document ID (e.g., `1234` from `DownloadsStorageProvider`), rather than the actual display name (`OpenableColumns.DISPLAY_NAME`).
4. **Missing Active URI Update After Save**: `MainActivity.saveFileLauncher` never calls `viewModel.setMapUri(uri)` after saving a new file, causing subsequent saves in the same session to lose track of the newly saved document URI.

---

## 2. Technical Decisions

### Decision 1: Overwrite Detection & Target Resolution Strategy

- **Decision**: Introduce a deterministic `SaveTargetResolver` utility in `:app` (unit-testable in pure Kotlin without Android framework dependencies) that compares the currently edited document's `originalUri` and `originalDisplayName` against the `pickedUri` and `pickedDisplayName` returned by the system file picker (`ACTION_CREATE_DOCUMENT`):
  1. **Not editing an existing user file** (`originalUri == null` or `originalUri.scheme == "android.resource"`):
     - Resolution: `SaveResolution.WriteNewFile(targetUri = pickedUri, displayName = pickedDisplayName)`
  2. **Same URI returned** (`pickedUri == originalUri` — happens when the user explicitly taps the existing file in the system picker and confirms the OS-level prompt):
     - Resolution: `SaveResolution.DirectOverwrite(targetUri = originalUri, displayName = originalDisplayName)` (avoid double-prompting if the OS picker already confirmed and returned the exact same URI, or confirm if configured; note: if the user simply clicked "Save" in `ACTION_CREATE_DOCUMENT`, the OS creates a new `pickedUri != originalUri` with `(n)`).
  3. **Different URI returned, but `pickedDisplayName` matches `originalDisplayName` or its Android `(n)` auto-incremented variant** (`<base> (<n>).<ext>` or `<originalDisplayName> (<n>)` where `n >= 1`):
     - Resolution: `SaveResolution.ConfirmOverwrite(originalUri = originalUri, originalDisplayName = originalDisplayName, duplicatePickerUri = pickedUri, duplicateDisplayName = pickedDisplayName)`
  4. **Different URI returned, and `pickedDisplayName` is a genuinely different filename chosen by the user** (e.g., `carte_v2.mm`):
     - Resolution: `SaveResolution.WriteNewFile(targetUri = pickedUri, displayName = pickedDisplayName)`

- **Rationale**:
  - Directly addresses the user's requirement: *"à la validation dans le filepicker système le fichier existant soit remplacé avec éventuellement le flow de confirmation. Actuellement un nouveau fichier est créé avec le nom du fichier d'origine et le postfix (n) où n est un incrément"*.
  - Keeps the system file picker validation flow intact while intercepting Android's automatic `(n)` duplicate creation.
  - Pure Kotlin regex/string matching for `<base> (<n>).<ext>` and `<originalName> (<n>)` makes all edge cases 100% unit-testable with `kotlin.test`.

- **Alternatives Considered**:
  - *Alternative A: Bypass the file picker completely on Save when `originalUri != null`*: Rejected because the user specifically requested that validation in the system file picker replaces the existing file with a confirmation flow rather than silently skipping the file picker.
  - *Alternative B: Use `Intent.ACTION_OPEN_DOCUMENT` for saving*: Rejected because `ACTION_OPEN_DOCUMENT` does not allow typing a filename or saving a new file, and changes the picker UI from a Save dialog to an Open dialog.

---

### Decision 2: Handling the Empty `(n)` Document Created by `ACTION_CREATE_DOCUMENT`

- **Decision**:
  - When `ACTION_CREATE_DOCUMENT` returns `duplicatePickerUri` (which Android's `DocumentsProvider` created as a 0-byte placeholder file with the `(n)` suffix before invoking our `ActivityResultCallback`), the app **does not write** to `duplicatePickerUri` yet.
  - Instead, the app shows `OverwriteConfirmationDialog` (`DialogType.OverwriteConfirmation`).
  - **If the user confirms overwrite**:
    1. Write the serialized mindmap content to `originalUri` using `contentResolver.openOutputStream(originalUri, "wt")` (write + truncate so no trailing bytes remain if the new XML is shorter).
    2. Delete `duplicatePickerUri` via `DocumentsContract.deleteDocument(contentResolver, duplicatePickerUri)` (with `contentResolver.delete(duplicatePickerUri, null, null)` fallback).
    3. Keep `originalUri` and `originalDisplayName` as the active document, update `RecentFilesRepository.addRecentFile(originalUri.toString(), originalDisplayName)`, clear `hasUnsavedChanges = false`, and execute any pending post-save action (e.g., Exit, New Mindmap, Open File).
    4. *Fallback*: If writing to `originalUri` throws `SecurityException` or `FileNotFoundException` (e.g., external file was deleted outside the app or provider revoked write grant), fall back to writing to `duplicatePickerUri`, updating the active URI to `duplicatePickerUri`, and informing the user.
  - **If the user cancels overwrite**:
    1. Delete the empty 0-byte `duplicatePickerUri` via `DocumentsContract.deleteDocument(contentResolver, duplicatePickerUri)` so zero `(n)` files remain on disk.
    2. Dismiss the dialog, keep `originalUri` untouched, and keep `hasUnsavedChanges = true` (cancelling any pending post-save navigation).

- **Rationale**:
  - Android SAF creates the empty `(n)` file inside `DocumentsProvider.createDocument` as soon as the user taps "Save" in `DocumentsUI`. Deleting `duplicatePickerUri` via `DocumentsContract.deleteDocument()` is the standard, officially supported SAF mechanism (grants `FLAG_SUPPORTS_DELETE` on newly created documents) to remove that placeholder on both Confirm (after writing to `originalUri`) and Cancel.
  - Using mode `"wt"` when opening `OutputStream` on `originalUri` ensures the existing file is truncated before writing, preventing corrupted trailing XML tags when the updated mindmap is smaller than the previous version.

- **Alternatives Considered**:
  - *Leaving the 0-byte `(n)` file on disk when overwriting `originalUri`*: Rejected because it violates SC-001 ("Zero duplicate files with `(1)`, `(2)`, or `(n)` suffixes are created") and leaves orphan empty files in the user's storage.

---

### Decision 3: Persistable Read + Write URI Permissions & Initial URI Hint

- **Decision**:
  - Update `MainActivity.openFileLauncher` (and `OpenDocument` contract configuration in `ActivityExtensions.kt`) to request and persist both read and write permissions:
    ```kotlin
    val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    try {
        contentResolver.takePersistableUriPermission(uri, takeFlags)
    } catch (e: SecurityException) {
        // Fallback to read-only if provider only granted read permission
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    ```
  - Pass `DocumentsContract.EXTRA_INITIAL_URI` set to `currentMindMapUri` when launching `Intent.ACTION_CREATE_DOCUMENT` in `launchFileSavingIntent`, so the system file picker opens directly in the directory of the currently edited file on Android 8.0+ (API 26+).

- **Rationale**:
  - `Intent.ACTION_OPEN_DOCUMENT` grants both read and write URI permissions by default when the provider supports writing, **provided** the app calls `takePersistableUriPermission` with `FLAG_GRANT_WRITE_URI_PERMISSION` (and adds `FLAG_GRANT_WRITE_URI_PERMISSION` to the `OpenDocument` intent flags). This also allows files reopened from `RecentFilesDialog` to retain write access across app restarts.
  - Setting `DocumentsContract.EXTRA_INITIAL_URI` ensures the system picker defaults to the same folder as the open file.

---

### Decision 4: Accurate Display Name Tracking in `NodeManager` & `MainViewModel`

- **Decision**:
  - Store `currentMindMapDisplayName: String?` alongside `currentMindMapUri: Uri?` in `NodeManager` (via `setMapUri(uri: Uri?, displayName: String? = null)`), or resolve the display name when opening/saving a file in `MainActivity` using `getFileNameFromUri(context, uri)` (`OpenableColumns.DISPLAY_NAME`) and pass it to `viewModel.setMapUri(uri, displayName)`.
  - Update `NodeManager.getMindmapFileName()` to prefer `currentMindMapDisplayName` when non-blank, falling back to `currentMindMapUri?.path?.substringAfterLast("/")?.substringAfterLast(":")` only when `currentMindMapDisplayName` is null.
  - Reset `currentMindMapDisplayName = null` in `NodeManager.createNewMindmap()` and when loading the bundled demo mindmap (`android.resource://...`).

- **Rationale**:
  - Fixes a latent bug where SAF URIs such as `content://com.android.providers.downloads.documents/document/raw%3A%2Fstorage%2Femulated%2F0%2FDownload%2Fcarte.mm` or `content://com.android.providers.downloads.documents/document/msf%3A100001234` returned `"msf:100001234"` instead of `"carte.mm"` from `getMindmapFileName()`.
  - Guarantees that `getSaveFilename()` pre-fills the exact original filename (`carte.mm`) in `Intent.EXTRA_TITLE` and provides the exact original display name to `SaveTargetResolver`.
