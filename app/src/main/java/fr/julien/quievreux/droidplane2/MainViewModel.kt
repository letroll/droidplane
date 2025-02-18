package fr.julien.quievreux.droidplane2

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julien.quievreux.droidplane2.MainUiState.DialogType
import fr.julien.quievreux.droidplane2.MainUiState.DialogUiState
import fr.julien.quievreux.droidplane2.MainUiState.SearchUiState
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.isInternalLink
import fr.julien.quievreux.droidplane2.data.model.shortFamily
import fr.julien.quievreux.droidplane2.helper.FileRegister
import fr.julien.quievreux.droidplane2.model.ContentNodeType
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RelativeFile
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.CopyText
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.Edit
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.NodeLink
import fr.julien.quievreux.droidplane2.model.ViewIntentNode
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
) : ViewModel(), KoinScopeComponent {

    override val scope: Scope by lazy { createScope(this) }

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    private val nodeManager: NodeManager by inject {
        parametersOf(viewModelScope)
    }
    private var fileRegister: FileRegister? = null
    private var fileToSave: File? = null
    private var nodeBeforeFileSave: Node? = null

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
        try {
            viewModelScope.launch {
                nodeManager.loadMindMapFromInputStream(
                    inputStream = inputStream,
                    onLoadFinished = onLoadFinished,
                    onError = { exception ->
                        logger.e("Error loading mind map:$exception")
                        updateUiState {
                            it.copy(
                                error = exception.message ?: "exception without message"//exception.stackTraceToString()
                            )
                        }
                    },
                    onParentNodeUpdate = { parentNode ->
                        logger.e("loadMindMap onParentNodeUpdate")
                        updateNodeDisplayed(parentNode)
                    }
                )
            }
        } catch (exception: Exception) {
            logger.e("loadMindMap exc")
        }
        setMindmapIsLoading(false)
    }

    private fun updateNodeDisplayed(parentNode: Node) {
        val title = getNodeText(parentNode).orEmpty()
        updateUiState {
            it.copy(
                title = title,
                nodeCurrentlyDisplayed = parentNode,
            )
        }
    }

    fun getSearchResultFlow() = nodeManager.getSearchResultFlow()

    fun onNodeClick(
        node: Node,
    ) {
        viewModelScope.launch {
            when {
                node.childNodes.size > 0 -> {
                    showNode(node)
                }

                node.link != null -> {
                    if (node.isInternalLink()) {
                        openInternalFragmentLink(node = node)
                    } else {
                        openIntentLink(node = node)
                    }
                }

                node.richTextContents.isNotEmpty() -> {
                    updateUiState {
                        it.copy(
                            viewIntentNode = ViewIntentNode(
                                intent = Intent(),
                                node = node,
                            ),
                            contentNodeType = ContentNodeType.RichText
                        )
                    }
                }

                else -> {
                    setTitle(getNodeText(node))
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
    private suspend fun showNode(node: Node) {
        node.deselectAllChildNodes()
        updateUiState {
            it.copy(
                nodeCurrentlyDisplayed = node,
            )
        }

        enableHomeButtonIfNeeded(node)

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        setTitle(getNodeText(node))

        // mark node as selected
        node.isSelected = true //TODO needed?
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
     */
    fun upOrClose() {
        up(true)
    }

    /**
     * Navigates back up one level in the MainViewModel, if possible. If force is true, the application closes if we can't
     * go further up
     *
     * @param force
     */
    fun up(force: Boolean) {
        viewModelScope.launch {
            _uiState.value.nodeCurrentlyDisplayed?.id?.let { nodeId ->
                nodeManager.findFilledNode(nodeId)?.let { node ->
                    // Find the parent using findFilledNode to get the updated version
                    node.parentNode?.id?.let { parentId ->
                        val parent = nodeManager.findFilledNode(parentId)
                        parent?.isSelected = false
                        updateUiState {
                            it.copy(
                                nodeCurrentlyDisplayed = parent,
                            )
                        }

                        // enable the up navigation with the Home (app) button (top left corner)
                        enableHomeButtonIfNeeded(parent)

                        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
                        parent?.let {
                            setTitle(getNodeText(it))
                        }
                    }
                } ?: run {
                    if (force) {
                        leaveApp()
                    }
                }
            } ?: run {
                if (force) {
                    leaveApp()
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
        if (_uiState.value.searchUiState.currentSearchResultIndex < nodeManager.getResultCount() - 1) {
            updateSearchUiState {
                it.copy(
                    currentSearchResultIndex = it.currentSearchResultIndex + 1
                )
            }

            showCurrentSearchResult()
        }
    }

    /** Selects the previous search result node.  */
    fun searchPrevious() {
        if (_uiState.value.searchUiState.currentSearchResultIndex > 0) {
            updateSearchUiState {
                it.copy(
                    currentSearchResultIndex = it.currentSearchResultIndex - 1,
                )
            }

            showCurrentSearchResult()
        }
    }

    private fun showCurrentSearchResult() {
        viewModelScope.launch {
            logger.e(
                "toto", """
showCurrentSearchResult:${_uiState.value.searchUiState.currentSearchResultIndex}
nodeFindList:${nodeManager.getSearchResult().map { getNodeText(it) }.joinToString(separator = "|")}
            """.trimIndent()
            )
            if (isSearchResultIndexValid()) {
                downTo(getCurrentSearchResultItem(), false)
            }
            //TODO Shows/hides the next/prev buttons
            //TODO highlight result in column
        }
    }

    private fun getCurrentSearchResultItem() = nodeManager.getSearchResult()[_uiState.value.searchUiState.currentSearchResultIndex]

    private fun isSearchResultIndexValid() = _uiState.value.searchUiState.currentSearchResultIndex >= 0 && _uiState.value.searchUiState.currentSearchResultIndex < nodeManager.getSearchResultCount()

    /**
     * Navigate down the MainViewModel to the specified node, opening each of it's parent nodes along the way.
     * @param node
     */
    private suspend fun downTo(node: Node?, openLast: Boolean) {
        // first navigate back to the top (essentially closing all other nodes)
        top()

        // go upwards from the target node, and keep track of each node leading down to the target node
        val nodeHierarchy: MutableList<Node> = mutableListOf()
        var tmpNode = node
        while (tmpNode?.parentNode != null) {   // TODO: this gives a NPE when rotating the device
            nodeHierarchy.add(tmpNode)
            tmpNode = tmpNode.parentNode
        }

        // reverse the list, so that we start with the root node
        nodeHierarchy.reverse()

        // descent from the root node down to the target node
        for (mindmapNode in nodeHierarchy) {
            mindmapNode.isSelected = true
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
//        updateUiState {
//            it.copy(
//                currentSearchResultIndex = 0
//            )
//        }
        nodeManager.search(
            query = query,
            onResultFound = {
                showCurrentSearchResult()
            }
        )
    }

    fun onNodeContextMenuClick(contextMenuAction: ContextMenuAction) {
        when (contextMenuAction) {
            is Edit -> {
                nodeManager.getNodeByID(contextMenuAction.node.id)?.let { node ->
                    setDialogState(
                        DialogType.EditNodeDescription(
                            node = node,
                            oldValue = getNodeText(node).orEmpty(),
                        )
                    )
                }
            }

            is NodeLink -> {
                val node = nodeManager.getNodeByID(contextMenuAction.node.id)
                viewModelScope.launch {
                    downTo(node, true)
                }
            }

            is CopyText -> {/* already handled by activity */
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
        val openUriIntent = Intent(ACTION_VIEW)
        openUriIntent.setData(node.link)
        updateUiState {
            it.copy(
                viewIntentNode = ViewIntentNode(
                    intent = openUriIntent,
                    node = node,
                ),
                contentNodeType = Classic
            )
        }
    }

    fun getNodeText(node: Node) = nodeManager.getNodeText(node)

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
            logger.e("Opening file " + Uri.fromFile(file))
            // http://stackoverflow.com/a/3571239/1067124
            var extension = ""
            val i = fileName.lastIndexOf('.')
            val p = fileName.lastIndexOf('/')
            if (i > p) {
                extension = fileName.substring(i + 1)
            }
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

            val intent = Intent()
            intent.setAction(ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), mime)
            updateUiState {
                it.copy(
                    viewIntentNode = ViewIntentNode(
                        intent = intent,
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
        //TODO jqx check if some logic shouldn't be down in nodeManager
        viewModelScope.launch(Dispatchers.IO) {
            setMindmapIsLoading(true)
            val updatedNode = node.copy(
                modificationDate = Date().time,
                text = newValue,
            )

            updateNodeInMindMapIndexes(updatedNode)

            var parentNodeToShow: Node? = null

            //update in parent also, if not the root, because it's what we show which is the list if(updatedNode.isRoot().not()){
            updatedNode.parentNode?.let { parentNode ->
                val childIndex = parentNode.childNodes.indexOfFirst { it.id == updatedNode.id }
                if (childIndex != -1) {
                    val updatedChildren = parentNode.childNodes.toMutableList()
                    updatedChildren[childIndex] = updatedNode

                    val updatedParent = parentNode.copy(
                        childNodes = updatedChildren
                    )

                    parentNodeToShow = updatedParent

                    updateNodeInMindMapIndexes(updatedParent)
                }
            } ?: run {
                parentNodeToShow = updatedNode
            }

            parentNodeToShow?.let { newParentNode ->
                nodeManager.updateRootNode(newParentNode)
                updateNodeDisplayed(newParentNode)
            }
            setMindmapIsLoading(false)
        }
    }

    fun setMapUri(data: Uri?) = nodeManager.setMapUri(data)

    fun launchSaveFile() {
        nodeBeforeFileSave = _uiState.value.nodeCurrentlyDisplayed
        nodeBeforeFileSave?.let {
            top()
            nodeManager.getMindmapFileName()?.let { filename ->
                fileRegister?.let { register ->
                    viewModelScope.launch {
                        nodeManager.serializeMindmap(
                            filePath = register.getfilesDir(),
                            filename = filename,
                            onError = {
                                logger.e("error saving file:$it")
                            },
                            onSaveFinished = { file ->
                                fileToSave = file
                                fileRegister?.registerFile(file)
                            }
                        )
                    }
                }
            }
        }
    }

    fun setFileRegister(fileRegister: FileRegister) {
        this.fileRegister = fileRegister
    }

    fun saveFile(outputStream: OutputStream) {
        fileToSave?.let { file ->
            logger.e("Saving file ${file.name} ")//content:${file.readText()}")
            outputStream.write(file.readText().toByteArray())
        }
        nodeBeforeFileSave?.let {
            onNodeClick(it)
        }
    }

    fun getNameOfFileToSave(): String? = fileToSave?.name

    fun addNode(newValue: String) {
        viewModelScope.launch(Dispatchers.IO) {
            setMindmapIsLoading(true)
            val deferredNodeId: Deferred<Int?> = async { nodeManager.addNodeToMindmap(newValue, _uiState.value.nodeCurrentlyDisplayed) }
            deferredNodeId.await()?.let { newNodeId ->
                // Get the node ID from the numeric ID
                val nodeId = nodeManager.getNodeID(newNodeId)
                // Get the updated node with all its children
                val updatedNode = nodeManager.findFilledNode(nodeId)
                if (updatedNode != null) {
                    // If this is a child node, show its parent
                    val parentNode = updatedNode.parentNode
                    if (parentNode != null) {
                        logger.e("addNode done updating UI with parent: ${parentNode.shortFamily()}")
                        val updatedParent = nodeManager.findFilledNode(parentNode.id)
                        if (updatedParent != null) {
                            showNode(updatedParent)
                        }
                    } else {
                        // This is a root node
                        logger.e("addNode done updating UI with new node")
                        showNode(updatedNode)
                    }
                }
            }

            setMindmapIsLoading(false)
        }
    }
}

