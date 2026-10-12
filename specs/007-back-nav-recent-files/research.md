# Research: Back Navigation to Recent Files

## Summary
No NEEDS CLARIFICATION markers in specification. All technical decisions derived from existing codebase patterns.

## Decisions

### Dialog Implementation: Reuse StartupChooserDialog
**Decision**: Extend/reuse existing `StartupChooserDialog` component
**Rationale**: 
- Already has recent files list with LazyColumn
- Already has New Mindmap, Browse, Help & Demo buttons
- Uses RecentFilesRepository and createStorageFileChecker
- Only needs to add "Exit App" button and remove "Cancel" button (replace with dismiss)
- Located at `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/StartupChooserDialog.kt`

### State Management: Add DialogType.RecentFiles
**Decision**: Add new variant to `DialogType` sealed class in `MainUiState.kt`
**Rationale**: 
- Follows existing pattern for all dialogs (ExitConfirmation, DiscardConfirmation, StartupChooser, etc.)
- ViewModel controls dialog state via `setDialogState()`
- MainActivity renders dialog via when-expression on `dialogUiState.dialogType`

### Back Handling: Modify MainViewModel.upOrClose() / up(force: Boolean)
**Decision**: Check `hasUnsavedChangesState` in `up(force: Boolean)` and route to appropriate dialog
**Rationale**:
- Current logic in `up(force: Boolean)` calls `leaveApp()` when at root with force=true
- `leaveApp()` sets `leaving = true` which triggers ExitConfirmation in MainActivity
- Need to intercept before `leaveApp()` when no unsaved changes
- New logic: if force && !hasUnsavedChangesState → show RecentFiles dialog instead

### Toolbar Menu: Add AppTopBarAction.RecentFiles
**Decision**: Add new enum value and handle in AppTopBar dropdown menu
**Rationale**:
- Existing pattern: enum `AppTopBarAction` with cases for each menu item
- MainActivity handles actions in `onBarAction` when-expression
- Add to dropdown menu in `AppTopBar.kt` (always visible, not conditional like Up/Top)

### Exit App Action: Call Activity.finish()
**Decision**: Use same pattern as ExitConfirmationDialog.onConfirm
**Rationale**: 
- ExitConfirmationDialog calls `viewModel.resetLeaving(); finish()`
- RecentFilesDialog.onExitApp should call `finish()` directly on Activity
- MainActivity has access to `finish()` as FragmentActivity

## Alternatives Considered

### Option: New Dialog Component from Scratch
**Rejected**: Duplicates StartupChooserDialog logic; violates DRY and Constitution Principle V (simplicity)

### Option: Modify StartupChooserDialog In-Place
**Rejected**: StartupChooserDialog used at app startup; different callbacks needed (onDismiss vs onExitApp). Better to create RecentFilesDialog that composes StartupChooserDialog's content or extracts shared components.

### Option: Navigation Rail for Large Screens
**Rejected**: Spec says "toolbar menus or equivalent large screen" - current codebase uses toolbar overflow menu on all sizes. No navigation rail exists.

## Constitution Compliance Check
All decisions comply with:
- Principle I: Changes in :app only, reads from :data RecentFilesRepository
- Principle II: StateFlow for dialog state, ViewModel as single source of truth
- Principle III: Unit testable ViewModel logic, Compose testable dialog
- Principle IV: No XML/format changes
- Principle V: Reuses existing components, virtualized LazyColumn
- Principle VI: Small focused methods, complexity ≤ 20