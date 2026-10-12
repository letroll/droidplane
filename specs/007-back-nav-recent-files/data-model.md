# Data Model: Back Navigation to Recent Files

## Entities

### RecentFile (existing - from :data module)
Represents a recently accessed mindmap file.
- uriString: String - Content URI or file URI
- displayName: String - User-friendly filename
- timestamp: Long - Milliseconds since epoch

### DialogType.RecentFiles (new - in MainUiState.kt)
Sealed class variant for the Recent Files dialog state.
- recentFiles: List<RecentFile> - Filtered accessible recent files
- onOpenRecent: (RecentFile) -> Unit - Callback when user selects a file
- onNewMindmap: () -> Unit - Callback for New Mindmap button
- onBrowse: () -> Unit - Callback for Browse button
- onOpenDemo: () -> Unit - Callback for Help & Demo button
- onExitApp: () -> Unit - Callback for Exit App button (closes app)
- onDismiss: () -> Unit - Callback for cancel/dismiss

## Relationships
- MainViewModel owns RecentFilesRepository instance
- MainUiState.DialogUiState holds current DialogType
- RecentFilesDialog reads recentFiles from DialogType.RecentFiles
- RecentFilesRepository persists to SharedPreferences

## Validation Rules
- Recent files list filtered by createStorageFileChecker (accessibility)
- Max 8 recent files (RecentFilesRepository.MAX_RECENTS)
- Dialog only shown when hasUnsavedChangesState == false
- Exit App action finishes Activity (irreversible)