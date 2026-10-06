# Contract: ViewModel & NodeManager Interface

- **Modules**: `:data`, `:app`
- **Classes**: `NodeManagerContract`, `NodeManager`, `MainViewModel`

## NodeManager Contract Additions (`:data`)

```kotlin
package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.Node

interface NodeManagerContract {
    /**
     * Initializes a fresh empty mindmap in memory with a single root node.
     * Clears previous index structures, resets search, and emits the new root to [allNodes].
     */
    fun createNewMindmap(rootTitle: String = "Central Idea"): Node
}
```

---

## MainViewModel Interface Additions (`:app`)

```kotlin
package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.data.model.Node

class MainViewModel(...) {

    /**
     * Handles the user's request to create a new mindmap.
     * Prompts with DiscardConfirmation if unsaved edits exist;
     * otherwise executes [createEmptyMindmap] immediately.
     */
    fun onNewMindmapRequested()

    /**
     * Directly creates a new empty mindmap in memory, resetting navigation and dirty state.
     */
    fun createEmptyMindmap(rootTitle: String = "Central Idea")

    /**
     * Handles the user's request to load the bundled Help & Demo mindmap.
     * Prompts with DiscardConfirmation if unsaved edits exist;
     * otherwise executes [onConfirmLoad].
     */
    fun onHelpDemoRequested(onConfirmLoad: () -> Unit)

    /**
     * Displays the startup chooser with accessible recent files.
     */
    fun showStartupChooser(
        recentFiles: List<RecentFile>,
        onOpenRecent: (RecentFile) -> Unit,
        onBrowse: () -> Unit,
        onOpenDemo: () -> Unit,
    )
}
```
