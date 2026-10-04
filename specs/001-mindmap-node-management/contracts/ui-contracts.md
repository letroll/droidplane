# UI & ViewModel Contract (`:app` Module)

## Purpose
Specifies the interaction contract between presentation components (Jetpack Compose), `MainViewModel`, and application intents.

## ViewModel Interaction Contract

```kotlin
package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import kotlinx.coroutines.flow.StateFlow
import java.io.InputStream
import java.io.OutputStream

interface MainViewModelContract {

    /** Observable immutable UI state for Compose screens. */
    val uiState: StateFlow<MainUiState>

    /** Loads a mindmap from an input stream. */
    fun loadMindMap(inputStream: InputStream, onLoadFinished: (() -> Unit)? = null)

    /** User taps a child node to drill down into its subtree. */
    fun onChildNodeClicked(childNode: Node)

    /** User triggers "Up" action to navigate to parent. */
    fun onNavigateUp()

    /** User triggers "Top" action to return to root node. */
    fun onNavigateToTop()

    /** Initiates search mode with the given query string. */
    fun onSearchQueryChanged(query: String)

    /** Cycles to next search match. */
    fun onNextSearchMatch()

    /** Cycles to previous search match. */
    fun onPreviousSearchMatch()

    /** Exits search mode and clears match highlighting. */
    fun onExitSearchMode()

    /** Submits a newly created child node under currently displayed node. */
    fun onAddChildNode(text: String)

    /** Submits text modification for a specific node. */
    fun onUpdateNodeText(node: Node, newText: String)

    /** Handles context menu actions (Edit, Copy, Link). */
    fun onNodeContextMenuClick(action: ContextMenuAction)

    /** Saves current mindmap to output stream. */
    fun saveFile(outputStream: OutputStream)
}
```

## Compose Component Hierarchy

```
MindMapScreen (Scaffold)
 ├── AppTopBar
 │    ├── [Normal Mode]: AppIcon, Title, SearchButton, MenuButton (Up, Top, Open, Save, Help)
 │    └── [Search Mode]: BackButton, SearchTextField, PrevMatchButton, NextMatchButton, MenuButton
 └── NodeList (LazyColumn)
      └── Cell (Card)
           ├── IconBadges (FlowRow of built-in icons)
           ├── NodeText (Text styled with Bold / Italic / Color)
           ├── ChevronIndicator (Icon if childNodes.isNotEmpty())
           └── ContextMenuDropdown (Edit, Copy, Open Link)
```
