package fr.julien.quievreux.droidplane2

import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.julien.quievreux.droidplane2.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.ContentNodeType.RelativeFile
import fr.julien.quievreux.droidplane2.helper.NodeUtils
import fr.julien.quievreux.droidplane2.helper.NodeUtils.fillArrowLinks
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.CopyText
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.Edit
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.NodeLink
import fr.julien.quievreux.droidplane2.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.model.MindmapNode
import fr.julien.quievreux.droidplane2.model.NodeAttribute
import fr.julien.quievreux.droidplane2.model.isInternalLink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.File
import java.io.InputStream
import java.util.Stack

data class ViewIntentNode(val intent: Intent, val node: MindmapNode)

/**
 * MainViewModel handles the loading and storing of a mind map document.
 */
class MainViewModel : ViewModel() {
    data class MainUiState(
        val title: String = "",
        val defaultTitle: String = "",
        val lastSearchString: String = "",
        val currentSearchResultIndex: Int = 0,
        val loading: Boolean = true,
        val leaving: Boolean = false,
        val canGoBack: Boolean = false,
        val rootNode: MindmapNode? = null,
        val selectedNode: MindmapNode? = null,
        val error: String = "",
        val viewIntentNode: ViewIntentNode? = null,
        val contentNodeType: ContentNodeType = ContentNodeType.None
    )

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState

    //first state whether the search is happening or not
    private val _isSearching = MutableStateFlow(false)

    //second state the text typed by the user
    private val _searchText = MutableStateFlow("")
    private val searchText = _searchText.asStateFlow()

    private val _allNodes = MutableStateFlow(emptyList<MindmapNode>())
    val nodeFindList = searchText
        .combine(_allNodes) { text, nodes ->//combine searchText with _nodeFindList
            if (text.isBlank()) { //return the entery list of nodes if not is typed
                nodes
            } else {
                nodes.filter { node ->// filter and return a list of nodes based on the text the user typed
                    node.getNodeText(this)?.contains(text.trim(), ignoreCase = true) == true
                }//.reversed()
            }
        }.stateIn( //basically convert the Flow returned from combine operator to StateFlow
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),//it will allow the StateFlow survive 5 seconds before it been canceled
            initialValue = _allNodes.value
        )

    var currentMindMapUri: Uri? = null
    var rootNode: MindmapNode? = null

    /**
     * A map that resolves node IDs to Node objects
     */
    private var mindmapIndexes: MindmapIndexes? = null

    init {

    }

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    fun getNodeByID(id: String?): MindmapNode? = mindmapIndexes?.nodesByIdIndex?.get(id)

    private fun getNodeByNumericID(numericId: Int?): MindmapNode? = mindmapIndexes?.nodesByNumericIndex?.get(numericId)

    private fun setMindmapIsLoading(mindmapIsLoading: Boolean) {
        _uiState.update {
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
        inputStream: InputStream? = null,
    ) {
        viewModelScope.launch {
            setMindmapIsLoading(true)

            val nodeStack = Stack<MindmapNode>()

            try {
                // set up XML pull parsing
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()
                xpp.setInput(inputStream, "UTF-8")

                // stream parse the XML
                var eventType = xpp.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    when (eventType) {
                        XmlPullParser.START_DOCUMENT -> {
                            Log.d(MainApplication.TAG, "Received XML Start Document")
                        }

                        XmlPullParser.START_TAG -> {
                            when {
                                xpp.name == "node" -> {
                                    parseNode(nodeStack, xpp)
                                }

                                isRichContent(xpp) -> {
                                    parseRichContent(xpp, nodeStack)
                                }

                                xpp.name == "font" -> {
                                    parseFont(xpp, nodeStack)
                                }

                                xpp.name == "icon" && xpp.getAttributeValue(null, "BUILTIN") != null -> {
                                    parseIcon(xpp, nodeStack)
                                }

                                xpp.name == "arrowlink" -> {
                                    parseArrowLink(xpp, nodeStack)
                                }

                                else -> {
                                    Log.d(MainApplication.TAG, "Received unknown node " + xpp.name)
                                }
                            }
                        }

                        XmlPullParser.END_TAG -> {
                            if (xpp.name == "node") {
                                val completedMindmapNode = nodeStack.pop()
                                completedMindmapNode.loaded = true
                            }
                        }

                        XmlPullParser.TEXT -> {
                            // do we have TEXT nodes in the viewModel at all?
                        }

                        else -> {
                            throw IllegalStateException("Received unknown event $eventType")
                        }
                    }

                    eventType = xpp.next()
                }
            } catch (e: Exception) {
                throw RuntimeException(e)
            }

            // stack should now be empty
            if (!nodeStack.empty()) {
                throw RuntimeException("Stack should be empty")
                // TODO: we could try to be lenient here to allow opening partial documents
                //  (which sometimes happens when dropbox doesn't fully sync).
                //  Probably doesn't work anyways, as we already throw a runtime exception above if we receive garbage
            }

            // TODO: can we do this as we stream through the XML above?
            // load all nodes of root node into simplified MindmapNode, and index them by ID for faster lookup
            mindmapIndexes = NodeUtils.loadAndIndexNodesByIds(rootNode)

            // Nodes can refer to other nodes with arrowlinks. We want to have the link on both ends of the link, so we can
            // now set the corresponding links
            fillArrowLinks(mindmapIndexes?.nodesByIdIndex)

            Log.d(MainApplication.TAG, "Document loaded")

            setMindmapIsLoading(false)
        }
    }

    private fun parseNode(
        nodeStack: Stack<MindmapNode>,
        xpp: XmlPullParser,
    ) {
        var parentNode: MindmapNode? = null
        if (!nodeStack.empty()) {
            parentNode = nodeStack.peek()
        }

        val newMindmapNode = NodeUtils.parseNodeTag(xpp, parentNode)
        nodeStack.push(newMindmapNode)

        // if we don't have a parent node, then this is the root node
        if (parentNode == null) {
            val title = newMindmapNode.getNodeText(this).orEmpty()
            _uiState.update {
                it.copy(
                    title = title,
                    defaultTitle = title,
                    rootNode = newMindmapNode,
                    selectedNode = newMindmapNode,
                )
            }
            rootNode = newMindmapNode
            _allNodes.update {
                listOf(newMindmapNode)
            }
        } else {
            parentNode.addChildMindmapNode(newMindmapNode)

            _allNodes.update {
                it + newMindmapNode
            }
        }
    }

    private fun isRichContent(xpp: XmlPullParser) = (xpp.name == "richcontent"
        && (xpp.getAttributeValue(null, NodeAttribute.TYPE.name) == "NODE"
        || xpp.getAttributeValue(null, NodeAttribute.TYPE.name) == "NOTE"
        || xpp.getAttributeValue(null, NodeAttribute.TYPE.name) == "DETAILS"
        ))

    // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
    // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").

    // if this is an empty tag, we won't need to bother trying to read its content
    // we don't even need to read the <richcontent> node's attributes, as we would
    // only be interested in it's children
    private fun parseRichContent(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>) {
        if (xpp.isEmptyElementTag) {
            Log.d(MainApplication.TAG, "Received empty richcontent node - skipping")
        } else {
            val richTextContent = NodeUtils.loadRichContentNodes(xpp)

            // if we have no parent node, something went seriously wrong - we can't have a richcontent that is not part of a viewModel node
            check(!nodeStack.empty()) { "Received richtext without a parent node" }

            val parentNode = nodeStack.peek()
            parentNode.addRichTextContent(richTextContent)
        }
    }

    private fun parseIcon(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>) {
        val iconName = xpp.getAttributeValue(null, NodeAttribute.BUILTIN.name)

        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received icon without a parent node" }

        val parentNode = nodeStack.peek()
        parentNode.addIconName(iconName)
    }

    private fun parseFont(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>) {
        val boldAttribute = xpp.getAttributeValue(null, NodeAttribute.BOLD.name)

        // if we have no parent node, something went seriously wrong - we can't have a font node that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received richtext without a parent node" }
        val parentNode = nodeStack.peek()

        if (boldAttribute != null && boldAttribute == "true") {
            parentNode.isBold = true
        }

        val italicsAttribute = xpp.getAttributeValue(null, NodeAttribute.ITALIC.name)
        if (italicsAttribute != null && italicsAttribute == "true") {
            parentNode.isItalic = true
        }
    }

    private fun parseArrowLink(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>) {
        val destinationId = xpp.getAttributeValue(null, NodeAttribute.DESTINATION.name)

        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received arrowlink without a parent node" }

        val parentNode = nodeStack.peek()
        parentNode.addArrowLinkDestinationId(destinationId)
    }

    private fun getMindmapDirectoryPath(): String? {
        // link is relative to viewModel file
        val mindmapPath = currentMindMapUri?.path
        Log.d(MainApplication.TAG, "MainViewModel path $mindmapPath")
        val mindmapDirectoryPath = mindmapPath?.substring(0, mindmapPath.lastIndexOf("/"))
        Log.d(MainApplication.TAG, "MainViewModel directory path $mindmapDirectoryPath")
        return mindmapDirectoryPath
    }

    fun onNodeClick(
        node: MindmapNode,
    ) {
        when {
            node.childMindmapNodes.size > 0 -> {
                Log.e(
                    "toto", """
-----------------------------------
parent:${node.parentNode?.id}   
children:${node.id}   
${node.childMindmapNodes.joinToString(separator = "\n", transform = { "(${it.id})${it.getNodeText(this)}" })}
                """.trimIndent()
                )
                showNode(node)
            }

            node.link != null -> {
                if (node.isInternalLink()) {
                    openInternalFragmentLink(mindmapNode = node)
                } else {
                    openIntentLink(mindmapNode = node)
                }
            }

            node.richTextContents.isNotEmpty() -> {
                _uiState.update {
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
                setTitle(node.getNodeText(this))
            }
        }
    }

    /**
     * Open up Node node, and display all its child nodes. This should only be called if the node's parent is
     * currently already expanded. If not (e.g. when following a deep link), use downTo
     *
     * @param node
     */
    private fun showNode(node: MindmapNode) {
        node.deselectAllChildNodes()
        _uiState.update {
            it.copy(
                selectedNode = node,//TODO needed?
                rootNode = node,
            )
        }

        enableHomeButtonIfNeeded(node)

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        setTitle(node.getNodeText(this))

        // mark node as selected
        node.isSelected = true //TODO needed?
    }

    private fun enableHomeButtonIfNeeded(node: MindmapNode?) {
        _uiState.update {
            it.copy(
                canGoBack = node?.parentNode != null
            )
        }
    }

    private fun setTitle(title: String?) {
        _uiState.update {
            it.copy(
                title = title ?: it.defaultTitle
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
        _uiState.value.selectedNode?.id?.let { nodeId ->
            findFilledNode(nodeId)?.let { node ->
                val parent = node.parentNode
                parent?.isSelected = false
                _uiState.update {
                    it.copy(
                        rootNode = parent,
                        selectedNode = parent,
                    )
                }

                // enable the up navigation with the Home (app) button (top left corner)
                enableHomeButtonIfNeeded(parent)

                // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
                setTitle(parent?.getNodeText(this))

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

    private fun leaveApp() {
        _uiState.update {
            it.copy(
                leaving = true
            )
        }
    }

    //TODO Try with clone
    // TODO use suspend
    /** Depth-first search in the core text of the nodes in this sub-tree.  */ // TODO: this doesn't work while viewModel is still loading
    private fun findFilledNode(
        parentNodeId: String,
    ): MindmapNode? {
//        viewModelScope.launch {
//
//        }
        return depthFirstSearchRecursive(rootNode, parentNodeId)
    }

    private fun depthFirstSearchRecursive(
        node: MindmapNode?,
        targetId: String,
    ): MindmapNode? {
        if (node == null) {
            return null
        }
        if (node.id == targetId) {
            return node
        }
        for (child in node.childMindmapNodes) {
            val result = depthFirstSearchRecursive(child, targetId)
            if (result != null) {
                return result
            }
        }
        return null
    }

    /** Selects the next search result node.  */
    fun searchNext() {
        if (_uiState.value.currentSearchResultIndex < nodeFindList.value.size - 1) {
            _uiState.update {
                it.copy(
                    currentSearchResultIndex = it.currentSearchResultIndex + 1
                )
            }
            showCurrentSearchResult()
        }
    }

    /** Selects the previous search result node.  */
    fun searchPrevious() {
        if (_uiState.value.currentSearchResultIndex > 0) {
            _uiState.update {
                it.copy(
                    currentSearchResultIndex = it.currentSearchResultIndex - 1
                )
            }
            showCurrentSearchResult()
        }
    }

    private fun showCurrentSearchResult() {
        Log.e(
            "toto", """
showCurrentSearchResult:${_uiState.value.currentSearchResultIndex}
nodeFindList:${nodeFindList.value.map { it.getNodeText(this) }.joinToString(separator = "|")}
        """.trimIndent()
        )
        if (_uiState.value.currentSearchResultIndex >= 0 && _uiState.value.currentSearchResultIndex < nodeFindList.value.size) {
            downTo(nodeFindList.value[_uiState.value.currentSearchResultIndex], false)
        }
        //TODO Shows/hides the next/prev buttons
        //TODO highlight result in column
    }

    /**
     * Navigate down the MainViewModel to the specified node, opening each of it's parent nodes along the way.
     * @param node
     */
    private fun downTo(node: MindmapNode?, openLast: Boolean) {
        // first navigate back to the top (essentially closing all other nodes)
        top()

        // go upwards from the target node, and keep track of each node leading down to the target node
        val nodeHierarchy: MutableList<MindmapNode> = mutableListOf()
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
            scrollTo(mindmapNode)
            if ((mindmapNode != node || openLast) && mindmapNode.childMindmapNodes.size > 0) {
                onNodeClick(mindmapNode)
            }
        }
    }

    private fun scrollTo(node: MindmapNode) {
        //TODO for column with a lot of elements
//        if (nodeColumns.isEmpty()) {
//            return
//        }
//        val lastCol = nodeColumns[nodeColumns.size - 1]
//        lastCol.scrollTo(node)
    }

    fun top() {
        _uiState.update {
            it.copy(
                rootNode = rootNode,
                selectedNode = null,
            )
        }
    }

    fun search(query: String) {
//        _uiState.update {
//            it.copy(
//                currentSearchResultIndex = 0
//            )
//        }
        _searchText.update {
            query
        }
        _isSearching.update {
            query.isNotEmpty()
        }
        Log.e(
            "toto", """
query:$query 
isSearching:${_isSearching.value} 
find:${nodeFindList.value.joinToString(separator = "|", transform = { it.getNodeText(this@MainViewModel).orEmpty() })} 
        """.trimIndent()
        )

        if (nodeFindList.value.size == 1) {
            showCurrentSearchResult()
        }
    }

    fun onNodeContextMenuClick(contextMenuAction: ContextMenuAction) {
        when (contextMenuAction) {
            Edit -> {

            }

            is NodeLink -> {
                val nodeByNumericID = getNodeByNumericID(contextMenuAction.node.numericId)
                downTo(nodeByNumericID, true)
            }

            is CopyText -> {/* already handled by activity */
            }
        }
    }

    /**
     * Open this node's link as internal fragment
     */
    private fun openInternalFragmentLink(mindmapNode: MindmapNode?) {
        // internal link, so this.link is of the form "#ID_123234534" this.link.getFragment() should give everything
        // after the "#" it is null if there is no "#", which should be the case for all other links
        val fragment = mindmapNode?.link?.fragment
        val linkedInternal = getNodeByID(fragment)

        if (linkedInternal != null) {
            Log.d(MainApplication.TAG, "Opening internal node, $linkedInternal, with ID: $fragment")

            // the internal linked node might be anywhere in the viewModel, i.e. on a completely separate branch than
            // we are on currently. We need to go to the Top, and then descend into the viewModel to reach the right
            // point
            downTo(linkedInternal, true)
        } else {
            _uiState.update {
                it.copy(
                    error = "This internal link to ID $fragment seems to be broken.",
                )
            }
        }
    }

    /**
     * Open this node's link as intent
     */
    private fun openIntentLink(
        mindmapNode: MindmapNode,
    ) {
        val openUriIntent = Intent(ACTION_VIEW)
        openUriIntent.setData(mindmapNode.link)
        _uiState.update {
            it.copy(
                viewIntentNode = ViewIntentNode(
                    intent = openUriIntent,
                    node = mindmapNode,
                ),
               contentNodeType = Classic
            )
        }
    }

    fun openRelativeFile(mindmapNode: MindmapNode) {
        val fileName: String? = if (mindmapNode.link?.path?.startsWith("/") == true) {
            // absolute filename
            mindmapNode.link.path
        } else {
            getMindmapDirectoryPath() + "/" + mindmapNode.link?.path
        }
        fileName?.let {
            val file = File(fileName)
            if (!file.exists()) {
                Log.e(MainApplication.TAG, "File $fileName does not exist.")
                return
            }
            if (!file.canRead()) {
                Log.e(MainApplication.TAG, "Can not read file $fileName.")
                return
            }
            Log.d(MainApplication.TAG, "Opening file " + Uri.fromFile(file))
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
            _uiState.update {
                it.copy(
                    viewIntentNode = ViewIntentNode(
                        intent = intent,
                        node = mindmapNode
                    ),
                    contentNodeType = RelativeFile
                )
            }
        }
    }
}

