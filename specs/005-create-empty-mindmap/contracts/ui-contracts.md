# UI Contracts: Create Empty Mindmap & Help Demo Access

- **Module**: `:app`
- **Classes**: `AppTopBarAction`, `AppTopBar`, `MainActivity`

## TopBar Actions Contract

```kotlin
package fr.julien.quievreux.droidplane2.ui.components

enum class AppTopBarAction {
    SearchNext,
    SearchPrevious,
    Up,
    Top,
    NewMindmap, // Added action
    Open,
    Help,
    Save,
    Undo,
    Backpress,
    ExitSearch,
    ToggleDisplayMode,
}
```

### Options Menu Item Order in `AppTopBar.kt`

```kotlin
DropdownMenu(...) {
    // 1. New Mindmap
    DropdownMenuItem(
        text = { Text(stringResource(R.string.new_mindmap)) },
        onClick = {
            showMenu = false
            onBarAction(AppTopBarAction.NewMindmap)
        }
    )

    // 2. Open
    DropdownMenuItem(
        text = { Text(stringResource(R.string.open)) },
        onClick = {
            showMenu = false
            onBarAction(AppTopBarAction.Open)
        }
    )

    // 3. Save
    DropdownMenuItem(
        text = { Text(stringResource(R.string.save)) },
        onClick = {
            showMenu = false
            onBarAction(AppTopBarAction.Save)
        }
    )

    // 4. Undo (if canUndoDelete is true)
    // 5. Toggle Display Mode
    // 6. Help & Demo
    DropdownMenuItem(
        text = { Text(stringResource(R.string.help_demo)) },
        onClick = {
            showMenu = false
            onBarAction(AppTopBarAction.Help)
        }
    )
}
```

---

## Discard Confirmation Dialog Rendering (`MainActivity.kt`)

```kotlin
is DialogType.DiscardConfirmation -> {
    DiscardConfirmationDialog(confirmation = dialog)
}
```

---

## Startup Chooser Dialog Rendering (`MainActivity.kt`)

```kotlin
is DialogType.StartupChooser -> {
    StartupChooserDialog(
        chooser = dialog,
    )
}
```
