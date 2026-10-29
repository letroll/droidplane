package fr.julien.quievreux.droidplane2

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julien.quievreux.droidplane2.MainUiState.DialogType
import fr.julien.quievreux.droidplane2.MainUiState.DialogUiState
import fr.julien.quievreux.droidplane2.MainUiState.SearchUiState
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.model.DeleteSnapshot
import fr.julien.quievreux.droidplane2.data.model.MindMapUri
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.isInternalLink
import fr.julien.quievreux.droidplane2.data.model.shortFamily
import fr.julien.quievreux.droidplane2.helper.MimeTypeUtils
import fr.julien.quievreux.droidplane2.model.ContentNodeType
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RelativeFile
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.CopyText
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.NodeLink
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.AddChildNode
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.OpenLink
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.model.RecentFile
import fr.julien.quievreux.droidplane2.model.ViewIntentNode
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.update.GitHubRelease
import fr.julien.quievreux.droidplane2.update.GitHubUpdateCheckerServiceImpl
import fr.julien.quievreux.droidplane2.update.UpdateCheckResult
import fr.julien.quievreux.droidplane2.update.UpdateCheckerService
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinScopeComponent
import org.koin.core.component.createScope
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.Date

/**
 * MainViewModel handles the loading and storing of a mind map document.
 */
class MainViewModel(
    val logger: Logger,
    private val injectedNodeManager: NodeManager? = null,
    private val recentFilesRepository: RecentFilesRepository,
    private val updateCheckerService: UpdateCheckerService = GitHubUpdateCheckerServiceImpl(),
) : ViewModel(), KoinScopeComponent {

    override val scope: Scope by lazy { createScope(this) }

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    /**
     * Test helper to set initial UI state for testing
     */
    fun setInitialStateForTest(rootNode: Node) {
        _uiState.value = _uiState.value.copy(
            title = rootNode.text.orEmpty(),
            nodeCurrentlyDisplayed = rootNode,
            canGoBack = false,
            navigationStack = listOf(rootNode.id),
        )
    }

    private val nodeManager: NodeManager
        get() = injectedNodeManager ?: scope.get<NodeManager> { parametersOf(viewModelScope) }
    private var fileToSave: File? = null
    private var nodeBeforeFileSave: Node? = null
    // Undo stack for node deletion
    private val undoStack: MutableList<DeleteSnapshot> = mutableListOf()
    // Unsaved changes tracking
    private var hasUnsavedChanges: Boolean = false
    val hasUnsavedChangesState: Boolean
        get() = hasUnsavedChanges
    val canUndoDelete: Boolean
        get() = undoStack.isNotEmpty()
    val isLeaving: Boolean
        get() = _uiState.value.leaving

    // Toolbar state for bottom toolbar
    private val _showBottomToolbar = mutableStateOf(false)
    private val _selectedNodeForToolbar = mutableStateOf<String?>(null)

    val showBottomToolbar: Boolean
        get() = _showBottomToolbar.value
    val selectedNodeForToolbar: String?
        get() = _selectedNodeForToolbar.value

    fun showBottomToolbar(nodeId: String) {
        _selectedNodeForToolbar.value = nodeId
        _showBottomToolbar.value = true
    }

    fun dismissBottomToolbar() {
        _showBottomToolbar.value = false
        _selectedNodeForToolbar.value = null
    }

    override fun onCleared() {
        super.onCleared()
        scope.close()
    }

    private fun setMindmapIsLoading(mindmapIsLoading: Boolean) {
        updateUiState {
            it.copy(
                loading = mindmapIsLoading
            )
        }
    }

    /**
     * Loads a mind map (*.mm) XML document into its internal DOM tree
     *
     * @param inputStream the inputStream to load
     */
    fun loadMindMap(
        inputStream: InputStream,
        onLoadFinished: (() -> Unit)? = null,
    ) {
        setMindmapIsLoading(true)
        hasUnsavedChanges = false
        undoStack.clear()
        viewModelScope.launch(Dispatchers.IO) {
            try {
                nodeManager.loadMindMapFromInputStream(
                    inputStream = inputStream,
                    onLoadFinished = {
                        setMindmapIsLoading(false)
                        updateUiState { currentState ->
                            currentState.copy(
                                treeVersion = currentState.treeVersion + 1,
                                collapsedNodeIds = emptySet(),
                                canUndoDelete = false,
                                snackbarMessage = null,
                            )
                        }
                        onLoadFinished?.invoke()
                    },
                    onError = { exception ->
                        logger.e("Error loading mind map:$exception")
                        setMindmapIsLoading(false)
                        updateUiState {
                            it.copy(
                                error = exception.message ?: "exception without message"
                            )
                        }
                    },
                    onParentNodeUpdate = { parentNode ->
                        logger.e("loadMindMap onParentNodeUpdate")
                        updateNodeDisplayed(parentNode)
                    }
                )
            } catch (exception: Exception) {
                logger.e("loadMindMap exc:$exception")
                setMindmapIsLoading(false)
            } finally {
                try {
                    inputStream.close()
                } catch (_: Exception) {}
            }
        }
    }

    private fun updateNodeDisplayed(parentNode: Node) {
        val title = getNodeText(parentNode).orEmpty()
        val isRoot = parentNode.parentNode == null
        val initialStack = if (isRoot) listOf(parentNode.id) else emptyList<String>()
        updateUiState {
            it.copy(
                title = title,
                nodeCurrentlyDisplayed = parentNode,
                selectedNodeId = parentNode.id,
                canGoBack = !isRoot,
                navigationStack = initialStack,
                treeVersion = it.treeVersion + 1,
            )
        }
    }

    /**
     * Navigate to a child node, pushing it onto the navigation stack
     */
    suspend fun onChildNodeClicked(childNode: Node) {
        updateUiState { currentState ->
            val newStack = currentState.navigationStack + childNode.id
            currentState.copy(
                nodeCurrentlyDisplayed = childNode,
                title = getNodeText(childNode).orEmpty(),
                canGoBack = true,
                navigationStack = newStack,
            )
        }
    }

    /**
     * Navigate up to parent node, popping from navigation stack
     */
    suspend fun onNavigateUp() {
        updateUiState { currentState ->
            val currentNode = currentState.nodeCurrentlyDisplayed
            val parentNode = currentNode?.parentNode
            
            if (parentNode != null) {
                val isParentRoot = parentNode.parentNode == null
                val newStack = if (isParentRoot) {
                    listOf(parentNode.id)
                } else {
                    currentState.navigationStack.dropLast(1)
                }
                currentState.copy(
                    nodeCurrentlyDisplayed = parentNode,
                    title = getNodeText(parentNode).orEmpty(),
                    canGoBack = !isParentRoot,
                    navigationStack = newStack,
                )
            } else {
                currentState
            }
        }
    }

    /**
     * Navigate to root node, clearing navigation stack
     */
    suspend fun onNavigateToTop() {
        updateUiState { currentState ->
            val rootNode = nodeManager.rootNode
            if (rootNode != null) {
                currentState.copy(
                    nodeCurrentlyDisplayed = rootNode,
                    title = getNodeText(rootNode).orEmpty(),
                    canGoBack = false,
                    navigationStack = listOf(rootNode.id),
                )
            } else {
                currentState
            }
        }
    }

    /**
     * Non-suspend wrapper for onNavigateUp for use from UI components
     */
    fun navigateUp() {
        viewModelScope.launch { onNavigateUp() }
    }

    /**
     * Non-suspend wrapper for onNavigateToTop for use from UI components
     */
    fun navigateToTop() {
        viewModelScope.launch { onNavigateToTop() }
    }

    /**
     * Returns the root node of the active mindmap document, if loaded.
     */
    fun getRootNode(): Node? = nodeManager.rootNode

    /**
     * Finds a node by its string ID.
     */
    fun getNodeByID(id: String): Node? = nodeManager.getNodeByID(id)

    /**
     * Handles user request to create a new empty mindmap.
     * Shows DiscardConfirmation dialog if unsaved modifications exist.
     */
    fun onNewMindmapRequested(defaultTitle: String = "Central Idea") {
        if (hasUnsavedChangesState) {
            setDialogState(
                MainUiState.DialogType.DiscardConfirmation(
                    onConfirm = {
                        setDialogState(MainUiState.DialogType.None)
                        createEmptyMindmap(defaultTitle)
                    },
                    onCancel = {
                        setDialogState(MainUiState.DialogType.None)
                    }
                )
            )
        } else {
            createEmptyMindmap(defaultTitle)
        }
    }

    /**
     * Handles user request to load the bundled Help & Demo mindmap.
     * Shows DiscardConfirmation dialog if unsaved modifications exist.
     */
    fun onHelpDemoRequested(onConfirmLoad: () -> Unit) {
        if (hasUnsavedChangesState) {
            setDialogState(
                MainUiState.DialogType.DiscardConfirmation(
                    onConfirm = {
                        setDialogState(MainUiState.DialogType.None)
                        hasUnsavedChanges = false
                        undoStack.clear()
                        onConfirmLoad()
                    },
                    onCancel = {
                        setDialogState(MainUiState.DialogType.None)
                    }
                )
            )
        } else {
            hasUnsavedChanges = false
            undoStack.clear()
            onConfirmLoad()
        }
    }

    /**
     * Loads accessible recent files on [Dispatchers.IO] and displays the startup chooser dialog.
     */
    fun showStartupChooser(
        fileAccessibilityChecker: (String) -> Boolean,
        onOpenRecent: (RecentFile) -> Unit,
        onBrowse: () -> Unit,
        onOpenDemo: () -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val recentFiles = recentFilesRepository.getRecentFiles(fileAccessibilityChecker)
            viewModelScope.launch(Dispatchers.Main) {
                showStartupChooser(
                    recentFiles = recentFiles,
                    onOpenRecent = onOpenRecent,
                    onBrowse = onBrowse,
                    onOpenDemo = onOpenDemo,
                )
            }
        }
    }

    /**
     * Displays the startup chooser dialog allowing the user to create a new mindmap,
     * select from existing recent files, browse storage, or open the demo.
     */
    fun showStartupChooser(
        recentFiles: List<RecentFile>,
        onOpenRecent: (RecentFile) -> Unit,
        onBrowse: () -> Unit,
        onOpenDemo: () -> Unit,
    ) {
        setDialogState(
            MainUiState.DialogType.StartupChooser(
                recentFiles = recentFiles,
                onNewMindmap = {
                    setDialogState(MainUiState.DialogType.None)
                    createEmptyMindmap()
                },
                onOpenRecent = { recent ->
                    setDialogState(MainUiState.DialogType.None)
                    onOpenRecent(recent)
                },
                onBrowse = {
                    setDialogState(MainUiState.DialogType.None)
                    onBrowse()
                },
                onOpenDemo = {
                    setDialogState(MainUiState.DialogType.None)
                    onOpenDemo()
                },
                onDismiss = {
                    setDialogState(MainUiState.DialogType.None)
                }
            )
        )
    }

    /**
     * Displays the recent files dialog allowing the user to open recent files,
     * create a new mindmap, browse storage, open the demo, or exit the app.
     */
    fun showRecentFilesDialog(
        fileAccessibilityChecker: (String) -> Boolean,
        onOpenRecent: (RecentFile) -> Unit,
        onNewMindmap: () -> Unit,
        onBrowse: () -> Unit,
        onOpenDemo: () -> Unit,
        onExitApp: () -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val recentFiles = recentFilesRepository.getRecentFiles(fileAccessibilityChecker)
            viewModelScope.launch(Dispatchers.Main) {
                setDialogState(
                    MainUiState.DialogType.RecentFiles(
                        recentFiles = recentFiles,
                        onOpenRecent = { recent ->
                            setDialogState(MainUiState.DialogType.None)
                            onOpenRecent(recent)
                        },
                        onNewMindmap = {
                            setDialogState(MainUiState.DialogType.None)
                            onNewMindmap()
                        },
                        onBrowse = {
                            setDialogState(MainUiState.DialogType.None)
                            onBrowse()
                        },
                        onOpenDemo = {
                            setDialogState(MainUiState.DialogType.None)
                            onOpenDemo()
                        },
                        onExitApp = {
                            setDialogState(MainUiState.DialogType.None)
                            onExitApp()
                        },
                        onDismiss = {
                            setDialogState(MainUiState.DialogType.None)
                        }
                    )
                )
            }
        }
    }

    /**
     * Creates a new empty mindmap in memory with a single root node.
     */
    fun createEmptyMindmap(rootTitle: String = "Central Idea") {
        val newRoot = nodeManager.createNewMindmap(rootTitle)
        hasUnsavedChanges = false
        undoStack.clear()
        updateUiState { currentState ->
            currentState.copy(
                nodeCurrentlyDisplayed = newRoot,
                selectedNodeId = newRoot.id,
                collapsedNodeIds = emptySet(),
                navigationStack = listOf(newRoot.id),
                title = rootTitle,
                treeVersion = currentState.treeVersion + 1,
                canGoBack = false,
                canUndoDelete = false,
                snackbarMessage = null,
                searchUiState = MainUiState.SearchUiState(),
            )
        }
    }

    /**
     * Sets the currently selected node in Mind Map view.
     */
    fun selectNode(node: Node) {
        updateUiState {
            it.copy(selectedNodeId = node.id)
        }
    }

    /**
     * Clears any active node selection.
     */
    fun clearNodeSelection() {
        updateUiState {
            it.copy(selectedNodeId = null)
        }
    }

    /**
     * Toggles the collapsed/expanded branch state for [node].
     */
    fun toggleNodeCollapse(node: Node) {
        updateUiState { currentState ->
            val updatedCollapsed = if (currentState.collapsedNodeIds.contains(node.id)) {
                currentState.collapsedNodeIds - node.id
            } else {
                currentState.collapsedNodeIds + node.id
            }
            currentState.copy(collapsedNodeIds = updatedCollapsed)
        }
    }

    fun getSearchResultFlow() = nodeManager.getSearchResultFlow()

    fun onNodeClick(
        node: Node,
    ) {
        viewModelScope.launch {
            val targetNode = nodeManager.getNodeByID(node.id) ?: node
            when {
                targetNode.childNodes.isNotEmpty() -> {
                    showNode(targetNode, updateStack = true)
                }

                targetNode.link != null -> {
                    if (targetNode.isInternalLink()) {
                        openInternalFragmentLink(node = targetNode)
                    } else {
                        openIntentLink(node = targetNode)
                    }
                }

                targetNode.richTextContents.isNotEmpty() -> {
                    updateUiState {
                        it.copy(
                            viewIntentNode = ViewIntentNode(
                                node = targetNode,
                            ),
                            contentNodeType = ContentNodeType.RichText
                        )
                    }
                }

                else -> {
                    setTitle(getNodeText(targetNode))
                }
            }
        }
    }

    /**
     * Open up Node node, and display all its child nodes. This should only be called if the node's parent is
     * currently already expanded. If not (e.g. when following a deep link), use downTo
     *
     * @param node
     */
    private fun showNode(node: Node, updateStack: Boolean = false) {
        val titleText = getNodeText(node).orEmpty()
        val canGoBack = node.parentNode != null
        updateUiState { currentState ->
            val newStack = if (updateStack) {
                if (node.parentNode == null) listOf(node.id) else currentState.navigationStack + node.id
            } else {
                currentState.navigationStack
            }
            currentState.copy(
                nodeCurrentlyDisplayed = node,
                selectedNodeId = node.id,
                title = titleText,
                canGoBack = canGoBack,
                navigationStack = newStack,
            )
        }
    }

    private fun enableHomeButtonIfNeeded(node: Node?) {
        updateUiState {
            it.copy(
                canGoBack = node?.parentNode != null
            )
        }
    }

    private fun setTitle(title: String?) {
        updateUiState {
            it.copy(
                title = title.orEmpty()
            )
        }
    }

    /**
     * Navigates back up one level in the MainViewModel. If we already display the root node, the application will finish
     * or show the Recent Files dialog if there are no unsaved changes.
     * This version is called from the Activity with full callbacks.
     */
    fun upOrClose(
        fileAccessibilityChecker: (String) -> Boolean,
        onOpenRecent: (RecentFile) -> Unit,
        onNewMindmap: () -> Unit,
        onBrowse: () -> Unit,
        onOpenDemo: () -> Unit,
        onExitApp: () -> Unit,
    ) {
        up(
            force = true,
            fileAccessibilityChecker = fileAccessibilityChecker,
            onOpenRecent = onOpenRecent,
            onNewMindmap = onNewMindmap,
            onBrowse = onBrowse,
            onOpenDemo = onOpenDemo,
            onExitApp = onExitApp,
        )
    }

    /**
     * Navigates back up one level in the MainViewModel, if possible. If force is true and we can't
     * go further up, shows the Recent Files dialog if there are no unsaved changes, otherwise shows
     * the Exit Confirmation dialog.
     *
     * @param force Whether to force exit when at root
     * @param fileAccessibilityChecker Optional checker for recent file accessibility. Required when force=true at root.
     * @param onOpenRecent Callback to open a recent file
     * @param onNewMindmap Callback to create a new mindmap
     * @param onBrowse Callback to browse for files
     * @param onOpenDemo Callback to open the demo mindmap
     * @param onExitApp Callback to exit the app
     */
    fun up(
        force: Boolean,
        fileAccessibilityChecker: ((String) -> Boolean)? = null,
        onOpenRecent: (RecentFile) -> Unit = {},
        onNewMindmap: () -> Unit = {},
        onBrowse: () -> Unit = {},
        onOpenDemo: () -> Unit = {},
        onExitApp: () -> Unit = {},
    ) {
        viewModelScope.launch {
            _uiState.value.nodeCurrentlyDisplayed?.id?.let { nodeId ->
                nodeManager.getNodeByID(nodeId)?.let { node ->
                    // Use NodeManager's index to get parent directly
                    nodeManager.getNodeParent(node.numericId)?.let { parent ->
                        showNode(parent)
                    }
                } ?: run {
                    if (force) {
                        if (hasUnsavedChangesState) {
                            leaveApp()
                        } else {
                            fileAccessibilityChecker?.let { checker ->
                                showRecentFilesDialog(
                                    fileAccessibilityChecker = checker,
                                    onOpenRecent = onOpenRecent,
                                    onNewMindmap = onNewMindmap,
                                    onBrowse = onBrowse,
                                    onOpenDemo = onOpenDemo,
                                    onExitApp = onExitApp,
                                )
                            } ?: leaveApp()
                        }
                    }
                }
            } ?: run {
                if (force) {
                    if (hasUnsavedChangesState) {
                        leaveApp()
                    } else {
                        fileAccessibilityChecker?.let { checker ->
                            showRecentFilesDialog(
                                fileAccessibilityChecker = checker,
                                onOpenRecent = onOpenRecent,
                                onNewMindmap = onNewMindmap,
                                onBrowse = onBrowse,
                                onOpenDemo = onOpenDemo,
                                onExitApp = onExitApp,
                            )
                        } ?: leaveApp()
                    }
                }
            }
        }
    }

    private fun leaveApp() {
        updateUiState {
            it.copy(
                leaving = true
            )
        }
    }

    fun resetLeaving() {
        updateUiState {
            it.copy(leaving = false)
        }
    }

    private fun updateUiState(newUiState: (MainUiState) -> MainUiState) {
        _uiState.update {
            newUiState(it)
        }
    }

    private fun updateSearchUiState(newSearchUiState: (SearchUiState) -> SearchUiState) {
        updateUiState {
            it.copy(
                searchUiState = newSearchUiState(it.searchUiState),
            )
        }
    }

    private fun updateDialogState(newDialogUiState: (DialogUiState) -> DialogUiState) {
        updateUiState {
            it.copy(
                dialogUiState = newDialogUiState(it.dialogUiState),
            )
        }
    }

    /** Selects the next search result node.  */
    fun searchNext() {
        if (_uiState.value.searchUiState.currentResultIndex < nodeManager.getResultCount() - 1) {
            updateSearchUiState {
                it.copy(
                    currentResultIndex = it.currentResultIndex + 1
                )
            }

            showCurrentSearchResult()
        }
    }

    /** Selects the previous search result node.  */
    fun searchPrevious() {
        if (_uiState.value.searchUiState.currentResultIndex > 0) {
            updateSearchUiState {
                it.copy(
                    currentResultIndex = it.currentResultIndex - 1,
                )
            }

            showCurrentSearchResult()
        }
    }

    /** Exits search mode and clears match highlighting. */
    fun onExitSearchMode() {
        updateSearchUiState {
            it.copy(
                isSearchActive = false,
                searchQuery = "",
                currentResultIndex = 0,
                totalResults = 0,
            )
        }
    }

    private fun showCurrentSearchResult() {
        viewModelScope.launch {
            val resultCount = nodeManager.getSearchResultCount()
            logger.e(
                "toto", """
showCurrentSearchResult:${_uiState.value.searchUiState.currentResultIndex}
nodeFindList:${nodeManager.getSearchResult().map { getNodeText(it) }.joinToString(separator = "|")}
            """.trimIndent()
            )
            if (isSearchResultIndexValid()) {
                downTo(getCurrentSearchResultItem(), false)
            }
            // Update total results count
            updateSearchUiState {
                it.copy(totalResults = resultCount)
            }
            //TODO Shows/hides the next/prev buttons
            //TODO highlight result in column
        }
    }

    private fun getCurrentSearchResultItem() = nodeManager.getSearchResult()[_uiState.value.searchUiState.currentResultIndex]

    private fun isSearchResultIndexValid() = _uiState.value.searchUiState.currentResultIndex >= 0 && _uiState.value.searchUiState.currentResultIndex < nodeManager.getSearchResultCount()

    /**
     * Navigate down the MainViewModel to the specified node, opening each of it's parent nodes along the way.
     * @param node
     */
    private suspend fun downTo(node: Node?, openLast: Boolean) {
        // first navigate back to the top (essentially closing all other nodes)
        top()

        if (node == null) return

        // go upwards from the target node, and keep track of each node leading down to the target node
        val nodeHierarchy: MutableList<Node> = mutableListOf(node)
        var tmpNode = node.parentNode
        while (tmpNode != null) {
            nodeHierarchy.add(tmpNode)
            tmpNode = tmpNode.parentNode
        }

        // reverse the list, so that we start with the root node
        nodeHierarchy.reverse()

        // descent from the root node down to the target node
        for (mindmapNode in nodeHierarchy) {
//            scrollTo(mindmapNode)
            if ((mindmapNode != node || openLast) && mindmapNode.childNodes.size > 0) {
                showNode(mindmapNode)
            }
        }
    }

//    private fun scrollTo(node: Node) {
    //TODO for column with a lot of elements
//        if (nodeColumns.isEmpty()) {
//            return
//        }
//        val lastCol = nodeColumns[nodeColumns.size - 1]
//        lastCol.scrollTo(node)
//    }

    fun top() {
        updateUiState {
            it.copy(
                nodeCurrentlyDisplayed = nodeManager.rootNode,
            )
        }
    }

    fun search(query: String) {
        updateSearchUiState {
            it.copy(
                isSearchActive = true,
                searchQuery = query,
                currentResultIndex = 0,
                totalResults = 0,
            )
        }
        nodeManager.search(
            query = query,
            onResultFound = {
                showCurrentSearchResult()
            }
        )
    }



    fun onSearchQueryChanged(query: String) {
        search(query)
    }

    fun onNextSearchMatch() {
        searchNext()
    }

    fun onPreviousSearchMatch() {
        searchPrevious()
    }

    fun onAddChildNode(text: String) {
        addNode(text)
    }

    fun onUpdateNodeText(node: Node, newText: String) {
        updateNodeText(node, newText)
    }

    fun onOpenNodeInspector(node: Node, initialTab: InspectorTab = InspectorTab.CONTENT) {
        val freshNode = nodeManager.getNodeByID(node.id) ?: node
        setDialogState(
            DialogType.NodePropertiesInspector(
                node = freshNode,
                initialTab = initialTab,
            )
        )
    }

    fun onSaveNodeProperties(updatedNode: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            setMindmapIsLoading(true)
            val success = nodeManager.updateNode(updatedNode)
            if (success) {
                hasUnsavedChanges = true
                val currentScreenNode = _uiState.value.nodeCurrentlyDisplayed
                if (currentScreenNode != null) {
                    val refreshedNode = nodeManager.getNodeByID(currentScreenNode.id)
                    if (refreshedNode != null) {
                        showNode(refreshedNode)
                    }
                }
                updateUiState { currentState ->
                    currentState.copy(
                        treeVersion = currentState.treeVersion + 1,
                        dialogUiState = DialogUiState(dialogType = DialogType.None),
                    )
                }
            } else {
                setDialogState(DialogType.None)
            }
            setMindmapIsLoading(false)
        }
    }

    fun onAddNodeFromInspector(newNode: Node, parentNode: Node?) {
        if (newNode.text.orEmpty().isBlank()) return
        addNode(newNode, parentNode)
        setDialogState(DialogType.None)
    }

    fun onDismissNodeInspector() {
        setDialogState(DialogType.None)
    }

    fun onMoveCloudToParent(node: Node) {
        val parentId = node.parentNode?.id ?: return
        val cloud = node.cloud ?: return
        viewModelScope.launch(Dispatchers.IO) {
            setMindmapIsLoading(true)
            val currentChild = nodeManager.getNodeByID(node.id) ?: node
            nodeManager.updateNode(currentChild.copy(cloud = null))

            val currentParent = nodeManager.getNodeByID(parentId)
            if (currentParent != null) {
                val updatedParent = currentParent.copy(cloud = cloud)
                nodeManager.updateNode(updatedParent)
                hasUnsavedChanges = true
                val currentScreenNode = _uiState.value.nodeCurrentlyDisplayed
                if (currentScreenNode != null) {
                    val refreshedNode = nodeManager.getNodeByID(currentScreenNode.id)
                    if (refreshedNode != null) {
                        showNode(refreshedNode)
                    }
                }
                updateUiState { currentState ->
                    currentState.copy(
                        treeVersion = currentState.treeVersion + 1,
                        dialogUiState = DialogUiState(
                            dialogType = DialogType.NodePropertiesInspector(
                                node = updatedParent,
                                initialTab = InspectorTab.STYLING_CLOUD,
                            )
                        )
                    )
                }
            }
            setMindmapIsLoading(false)
        }
    }

    fun onNodeContextMenuClick(contextMenuAction: ContextMenuAction) {
        when (contextMenuAction) {
            is ContextMenuAction.Properties -> {
                onOpenNodeInspector(contextMenuAction.node)
            }

            is NodeLink -> {
                val node = nodeManager.getNodeByID(contextMenuAction.node.id)
                viewModelScope.launch {
                    downTo(node, true)
                }
            }

            is CopyText -> {/* already handled by activity */
            }

            is AddChildNode -> {
                nodeManager.getNodeByID(contextMenuAction.parentNode.id)?.let { parentNode ->
                    val newNode = Node(
                        parentNode = parentNode,
                        id = "temp_${System.currentTimeMillis()}",
                        numericId = 0,
                        text = "",
                        creationDate = System.currentTimeMillis(),
                        modificationDate = System.currentTimeMillis()
                    )
                    setDialogState(
                        DialogType.NodePropertiesInspector(
                            node = newNode,
                            initialTab = InspectorTab.CONTENT,
                            parentNode = parentNode,
                            isNewNode = true,
                        )
                    )
                }
            }

            is OpenLink -> {
                val node = contextMenuAction.node
                if (node.isInternalLink()) {
                    openInternalFragmentLink(node)
                } else {
                    openIntentLink(node)
                }
            }

            is ContextMenuAction.DeleteNode -> {
                onDeleteNode(contextMenuAction.node)
            }
        }
    }

    /**
     * Open this node's link as internal fragment
     */
    private fun openInternalFragmentLink(node: Node?) {
        viewModelScope.launch {
            // internal link, so this.link is of the form "#ID_123234534" this.link.getFragment() should give everything
            // after the "#" it is null if there is no "#", which should be the case for all other links
            val fragment = node?.link?.fragment
            val linkedInternal = nodeManager.getNodeByID(fragment)

            if (linkedInternal != null) {
                logger.e("Opening internal node, $linkedInternal, with ID: $fragment")

                // the internal linked node might be anywhere in the viewModel, i.e. on a completely separate branch than
                // we are on currently. We need to go to the Top, and then descend into the viewModel to reach the right
                // point
                downTo(linkedInternal, true)
            } else {
                updateUiState {
                    it.copy(
                        error = "This internal link to ID $fragment seems to be broken.",
                    )
                }
            }
        }
    }

    /**
     * Open this node's link as intent
     */
    private fun openIntentLink(
        node: Node,
    ) {
        updateUiState {
            it.copy(
                viewIntentNode = ViewIntentNode(
                    uriString = node.link?.toString(),
                    node = node,
                ),
                contentNodeType = Classic
            )
        }
    }

    fun getNodeText(node: Node) = nodeManager.getNodeText(node)

    fun getNodeTextForCopy(node: Node) = nodeManager.getNodeTextForCopy(node)

    fun openRelativeFile(node: Node) {
        val fileName: String? = if (node.link?.path?.startsWith("/") == true) {
            // absolute filename
            node.link?.path
        } else {
            nodeManager.getMindmapDirectoryPath() + "/" + node.link?.path
        }
        fileName?.let {
            val file = File(fileName)
            if (!file.exists()) {
                logger.e("File $fileName does not exist.")
                return
            }
            if (!file.canRead()) {
                logger.e("Can not read file $fileName.")
                return
            }
            val fileUri = MindMapUri.fromFile(file)
            logger.e("Opening file $fileUri")
            var extension = ""
            val i = fileName.lastIndexOf('.')
            val p = fileName.lastIndexOf('/')
            if (i > p) {
                extension = fileName.substring(i + 1)
            }
            val mime = MimeTypeUtils.getMimeTypeFromExtension(extension)

            updateUiState {
                it.copy(
                    viewIntentNode = ViewIntentNode(
                        uriString = fileUri.toString(),
                        mimeType = mime,
                        node = node
                    ),
                    contentNodeType = RelativeFile
                )
            }
        }
    }

    fun setDialogState(dialogType: DialogType) {
        updateDialogState { it.copy(dialogType = dialogType) }
    }

    private fun updateNodeInMindMapIndexes(node: Node) {
        nodeManager.updateNodeInMindMapIndexes(node)
    }

    fun updateNodeText(
        node: Node,
        newValue: String,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            setMindmapIsLoading(true)
            val updatedNode = nodeManager.updateNodeText(node.id, newValue)
            hasUnsavedChanges = true
            setMindmapIsLoading(false)
            updatedNode?.let { node ->
                // Reload the displayed node from NodeManager to ensure synchronization
                val reloadedNode = nodeManager.getNodeByID(node.id)
                reloadedNode?.let { updated ->
                    updateNodeDisplayed(updated)
                }
                updateUiState { currentState ->
                    val updatedTitle = if (node.parentNode == null) {
                        newValue
                    } else {
                        currentState.title
                    }
                    currentState.copy(
                        title = updatedTitle,
                        treeVersion = currentState.treeVersion + 1,
                    )
                }
            }
        }
    }

    fun setMapUri(data: MindMapUri?, displayName: String? = null) = nodeManager.setMapUri(data, displayName)

    fun getMapUri(): MindMapUri? = nodeManager.getMapUri()

    fun getMindmapFileName(): String? = nodeManager.getMindmapFileName()

    fun getSaveFilename(): String {
        val currentUri = nodeManager.getMapUri()
        val isDemoResource = currentUri?.scheme == "android.resource"
        if (!isDemoResource) {
            val currentName = nodeManager.getMindmapFileName()
            if (!currentName.isNullOrBlank()) {
                return if (currentName.endsWith(".mm", ignoreCase = true)) {
                    currentName
                } else {
                    "$currentName.mm"
                }
            }
        }
        val title = _uiState.value.title.trim().ifBlank {
            nodeManager.rootNode?.let { getNodeText(it) }?.trim() ?: "mindmap"
        }
        val sanitized = title.replace(Regex("[^a-zA-Z0-9._-]"), "_").trim('_').ifBlank { "mindmap" }
        return if (sanitized.endsWith(".mm", ignoreCase = true)) sanitized else "$sanitized.mm"
    }

    fun launchSaveFile(filesDirPath: String) {
        nodeBeforeFileSave = _uiState.value.nodeCurrentlyDisplayed
        nodeBeforeFileSave?.let {
            top()
            val filename = getSaveFilename()
            viewModelScope.launch {
                nodeManager.serializeMindmap(
                    filePath = filesDirPath,
                    filename = filename,
                    onError = {
                        logger.e("error saving file:$it")
                    },
                    onSaveFinished = { file ->
                        fileToSave = file
                        updateUiState { state ->
                            state.copy(pendingSaveTempFile = file)
                        }
                    }
                )
            }
        }
    }

    fun clearPendingSaveTempFile() {
        updateUiState { state ->
            state.copy(pendingSaveTempFile = null)
        }
    }

    fun saveFile(outputStream: OutputStream) {
        fileToSave?.let { file ->
            logger.e("Saving file ${file.name} ")
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        hasUnsavedChanges = false
        setDialogState(DialogType.None)
        nodeBeforeFileSave?.let {
            onNodeClick(it)
        }
    }

    fun showOverwriteConfirmation(
        fileName: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit,
    ) {
        setDialogState(
            DialogType.OverwriteConfirmation(
                fileName = fileName,
                onConfirm = onConfirm,
                onCancel = onCancel,
            )
        )
    }

    fun cancelSaveFile() {
        setDialogState(DialogType.None)
        nodeBeforeFileSave?.let {
            onNodeClick(it)
        }
    }


    fun onDeleteNode(node: Node) {
        if (node.parentNode == null) {
            return
        }
        // Calculate descendant count
        val descendantCount = countDescendants(node)
        
        // Show confirmation dialog
        setDialogState(
            DialogType.DeleteConfirmation(
                node = node,
                descendantCount = descendantCount,
                onConfirm = { confirmDelete(node) },
                onCancel = { setDialogState(DialogType.None) }
            )
        )
    }

    private fun countDescendants(node: Node): Int {
        var count = 0
        fun countChildren(n: Node) {
            count += n.childNodes.size
            n.childNodes.forEach { countChildren(it) }
        }
        countChildren(node)
        return count
    }

    fun onConfirmDelete() {
        val dialogType = _uiState.value.dialogUiState.dialogType
        if (dialogType is DialogType.DeleteConfirmation) {
            confirmDelete(dialogType.node)
        }
    }

    private fun confirmDelete(node: Node) {
        viewModelScope.launch(Dispatchers.IO) {
            setMindmapIsLoading(true)
            // Create snapshot for undo before deleting
            val snapshot = nodeManager.createDeleteSnapshot(node.id)
            snapshot?.let { undoStack.add(0, it) }
            nodeManager.deleteNode(node.id)
            hasUnsavedChanges = true
            val parent = node.parentNode
            val updatedParent = parent?.id?.let { nodeManager.getNodeByID(it) } ?: parent
            if (updatedParent != null) {
                // Show parent's children list without selecting any node (FR-007)
                val titleText = getNodeText(updatedParent).orEmpty()
                val canGoBack = updatedParent.parentNode != null
                updateUiState { currentState ->
                    currentState.copy(
                        nodeCurrentlyDisplayed = updatedParent,
                        title = titleText,
                        canGoBack = canGoBack,
                        // Keep navigationStack as-is, don't push parent onto it
                    )
                }
            }
            val nodeTitle = node.text ?: "Node"
            updateUiState { currentState ->
                val newSelectedId = if (currentState.selectedNodeId == node.id) {
                    null  // FR-007: show parent's children list with no selection
                } else {
                    currentState.selectedNodeId
                }
                val newCollapsedIds = currentState.collapsedNodeIds.toMutableSet().apply {
                    remove(node.id)
                    if (updatedParent != null && updatedParent.childNodes.isEmpty()) {
                        remove(updatedParent.id)
                    }
                }
                currentState.copy(
                    treeVersion = currentState.treeVersion + 1,
                    selectedNodeId = newSelectedId,
                    collapsedNodeIds = newCollapsedIds,
                    canUndoDelete = undoStack.isNotEmpty(),
                    leaving = false,  // Reset leaving flag on deletion
                    snackbarMessage = MainUiState.SnackbarMessage(
                        message = "\"$nodeTitle\" deleted",
                        actionLabel = R.string.undo,
                        onAction = { onUndoDelete() }
                    )
                )
            }
            setMindmapIsLoading(false)
            setDialogState(DialogType.None)
        }
    }

    fun onUndoDelete() {
        viewModelScope.launch(Dispatchers.IO) {
            setMindmapIsLoading(true)
            var restoredParentId: String? = null
            var restoredNode: Node? = null
            undoStack.firstOrNull()?.let { snapshot ->
                restoredParentId = snapshot.parentNodeId
                val success = nodeManager.restoreSubtree(snapshot)
                if (success) {
                    undoStack.removeAt(0)
                    // Navigate to restored node
                    restoredNode = snapshot.rootDeletedNode
                    restoredNode?.let { showNode(it) }
                }
            }
            updateUiState { currentState ->
                val newCollapsed = if (restoredParentId != null) {
                    currentState.collapsedNodeIds - restoredParentId!!
                } else {
                    currentState.collapsedNodeIds
                }
                currentState.copy(
                    treeVersion = currentState.treeVersion + 1,
                    collapsedNodeIds = newCollapsed,
                    selectedNodeId = restoredNode?.id ?: currentState.selectedNodeId,
                    canUndoDelete = undoStack.isNotEmpty(),
                    leaving = false,  // Reset leaving flag on undo
                    snackbarMessage = null
                )
            }
            setMindmapIsLoading(false)
        }
    }

    fun clearSnackbarMessage() {
        updateUiState {
            it.copy(snackbarMessage = null)
        }
    }

    fun showSnackbarMessage(message: String) {
        updateUiState {
            it.copy(snackbarMessage = MainUiState.SnackbarMessage(message = message))
        }
    }

    fun checkForUpdates(
        currentVersion: String,
        isManual: Boolean = false,
        onDownloadAndInstall: (apkUrl: String, fileName: String) -> Unit,
        onViewOnGitHub: (url: String) -> Unit,
        upToDateMessage: String? = null,
        errorMessage: String? = null
    ) {
        viewModelScope.launch {
            val result = updateCheckerService.checkForUpdates(currentVersion)
            when (result) {
                is UpdateCheckResult.UpdateAvailable -> {
                    setDialogState(
                        DialogType.UpdateAvailable(
                            currentVersion = result.currentVersion,
                            release = result.release,
                            onDownloadAndInstall = onDownloadAndInstall,
                            onViewOnGitHub = onViewOnGitHub,
                            onDismiss = { setDialogState(DialogType.None) }
                        )
                    )
                }
                is UpdateCheckResult.UpToDate -> {
                    if (isManual && upToDateMessage != null) {
                        showSnackbarMessage(upToDateMessage)
                    }
                }
                is UpdateCheckResult.Error -> {
                    if (isManual && errorMessage != null) {
                        showSnackbarMessage(errorMessage)
                    }
                }
            }
        }
    }

    fun onCancelDelete() {
        setDialogState(DialogType.None)
    }

    /**
     * Creates a new child node using [newNode] as a template, preserving all content fields
     * (detailsText, noteText, richText, latexEquation, attributes, iconNames, etc.).
     */
    fun addNode(newNode: Node, parentNode: Node? = _uiState.value.nodeCurrentlyDisplayed) {
        if (newNode.text.orEmpty().isBlank()) return
        viewModelScope.launch(Dispatchers.IO) {
            val currentScreenNode = _uiState.value.nodeCurrentlyDisplayed
            val targetParent = parentNode ?: currentScreenNode
            val newNodeId = nodeManager.addNodeToMindmap(newNode, targetParent)
            newNodeId?.let {
                if (currentScreenNode != null) {
                    val refreshedScreenNode = nodeManager.getNodeByID(currentScreenNode.id)
                    if (refreshedScreenNode != null) {
                        showNode(refreshedScreenNode)
                    }
                } else if (targetParent != null) {
                    val updatedParent = nodeManager.getNodeByID(targetParent.id)
                    if (updatedParent != null) {
                        showNode(updatedParent)
                    }
                }
                val createdNodeId = nodeManager.getNodeID(newNodeId)
                updateUiState { currentState ->
                    val newCollapsed = if (targetParent != null) {
                        currentState.collapsedNodeIds - targetParent.id
                    } else {
                        currentState.collapsedNodeIds
                    }
                    currentState.copy(
                        treeVersion = currentState.treeVersion + 1,
                        collapsedNodeIds = newCollapsed,
                        selectedNodeId = createdNodeId ?: currentState.selectedNodeId,
                        leaving = false,
                    )
                }
            }
            hasUnsavedChanges = true
        }
    }

    fun addNode(newValue: String, parentNode: Node? = _uiState.value.nodeCurrentlyDisplayed) {
        if (newValue.isBlank()) {
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val currentScreenNode = _uiState.value.nodeCurrentlyDisplayed
            val targetParent = parentNode ?: currentScreenNode
            val newNodeId = nodeManager.addNodeToMindmap(newValue, targetParent)
            newNodeId?.let {
                if (currentScreenNode != null) {
                    val refreshedScreenNode = nodeManager.getNodeByID(currentScreenNode.id)
                    if (refreshedScreenNode != null) {
                        showNode(refreshedScreenNode)
                    }
                } else if (targetParent != null) {
                    val updatedParent = nodeManager.getNodeByID(targetParent.id)
                    if (updatedParent != null) {
                        showNode(updatedParent)
                    }
                }
                val createdNodeId = nodeManager.getNodeID(newNodeId)
                updateUiState { currentState ->
                    val newCollapsed = if (targetParent != null) {
                        currentState.collapsedNodeIds - targetParent.id
                    } else {
                        currentState.collapsedNodeIds
                    }
                    currentState.copy(
                        treeVersion = currentState.treeVersion + 1,
                        collapsedNodeIds = newCollapsed,
                        selectedNodeId = createdNodeId ?: currentState.selectedNodeId,
                        leaving = false,  // Reset leaving flag on node creation
                    )
                }
            }
            hasUnsavedChanges = true
        }
    }

    /**
     * Moves a node (and its entire subtree) to a new parent and/or sibling position.
     */
    fun onMoveNode(
        draggedNodeId: String,
        targetParentId: String,
        targetSiblingId: String? = null,
        insertAfter: Boolean = true,
        targetPosition: String? = null,
    ) {
        viewModelScope.launch {
            val success = nodeManager.moveNode(
                draggedNodeId = draggedNodeId,
                targetParentId = targetParentId,
                targetSiblingId = targetSiblingId,
                insertAfter = insertAfter,
                targetPosition = targetPosition,
            )
            if (success) {
                hasUnsavedChanges = true
                val displayedId = _uiState.value.nodeCurrentlyDisplayed?.id
                if (displayedId != null) {
                    val updatedDisplayed = nodeManager.getNodeByID(displayedId)
                    if (updatedDisplayed != null) {
                        showNode(updatedDisplayed)
                    }
                }
                updateUiState { currentState ->
                    currentState.copy(
                        treeVersion = currentState.treeVersion + 1,
                        collapsedNodeIds = currentState.collapsedNodeIds - targetParentId,
                        leaving = false,
                    )
                }
            }
        }
    }
}
