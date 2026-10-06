# Contract: UI & ViewModel Interface

- **Module**: `:app`
- **Classes**: `MainViewModel`, `MainActivity`, `NodeList`

## ViewModel Contract

```kotlin
class MainViewModel(...) {
    /**
     * Adds a new child node with [newValue] under [parentNode].
     *
     * @param newValue Text content of the node. If blank, no-op.
     * @param parentNode Target parent node. Defaults to the currently displayed node.
     */
    fun addNode(
        newValue: String,
        parentNode: Node? = _uiState.value.nodeCurrentlyDisplayed
    )

    /**
     * Sets dialog state to display the child creation dialog.
     */
    fun setDialogState(dialogType: MainUiState.DialogType)
}
```

## UI Dialog Wiring Contract (`MainActivity.kt`)

```kotlin
when (val dialog = state.value.dialogUiState.dialogType) {
    is DialogType.AddChildNode -> {
        CustomDialog(
            titre = stringResource(R.string.add_child),
            value = "",
            onDismiss = {
                viewModel.setDialogState(DialogType.None)
            }
        ) { newValue ->
            viewModel.addNode(newValue, dialog.parentNode)
        }
    }
    // ...
}
```
