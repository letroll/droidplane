package ch.benediktkoeppel.code.droidplane

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.benediktkoeppel.code.droidplane.SelectedNodeType.Link
import ch.benediktkoeppel.code.droidplane.SelectedNodeType.None
import ch.benediktkoeppel.code.droidplane.SelectedNodeType.RichText
import ch.benediktkoeppel.code.droidplane.controller.NodeChange
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.NodeStyleChanged
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.RichContentChanged
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.SubscribeNodeRichContentChanged
import ch.benediktkoeppel.code.droidplane.helper.NodeUtils
import ch.benediktkoeppel.code.droidplane.helper.NodeUtils.fillArrowLinks
import ch.benediktkoeppel.code.droidplane.model.MindmapIndexes
import ch.benediktkoeppel.code.droidplane.model.MindmapNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Stack

/**
 * MainViewModel handles the loading and storing of a mind map document.
 */
class MainViewModel : ViewModel() {
    data class MainUiState(
        val title: String,
        val defaultTitle: String,
        val lastSearchString: String,
        val loading: Boolean,
        val leaving: Boolean,
        val canGoBack: Boolean,
        val rootNode: MindmapNode? = null,
        val selectedNode: MindmapNode? = null,
        val selectedNodeType: SelectedNodeType,
    ) {
        internal companion object {
            internal fun defaults() = MainUiState(
                loading = true,
                canGoBack = false,
                leaving = false,
                title = "",
                defaultTitle = "",
                lastSearchString = "",
                selectedNodeType = None,
            )
        }

        val hasSelectedNodeType: SelectedNodeType? = if (selectedNode != null && selectedNodeType != None) selectedNodeType else null
    }

    val modern = false

    private val _uiState: MutableStateFlow<MainUiState> = MutableStateFlow(MainUiState.defaults())
    val uiState: StateFlow<MainUiState> = _uiState

    var currentMindMapUri: Uri? = null
    var rootNode: MindmapNode? = null

    /**
     * A map that resolves node IDs to Node objects
     */
    var mindmapIndexes: MindmapIndexes? = null

    init {

    }

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    fun getNodeByID(id: String?): MindmapNode? = mindmapIndexes?.nodesByIdIndex?.get(id)

    fun getNodeByNumericID(numericId: Int?): MindmapNode? = mindmapIndexes?.nodesByNumericIndex?.get(numericId)

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
        onRootNodeLoaded: (rootNode: MindmapNode?) -> Unit,
        onNodeChange: (nodeChange: NodeChange) -> Unit,
        //TODO remove lambda and use state
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
                                    parseNode(nodeStack, xpp, onNodeChange, onRootNodeLoaded)
                                }

                                isRichContent(xpp) -> {
                                    parseRichContent(xpp, nodeStack, onNodeChange)
                                }

                                xpp.name == "font" -> {
                                    parseFont(xpp, nodeStack, onNodeChange)
                                }

                                xpp.name == "icon" && xpp.getAttributeValue(null, "BUILTIN") != null -> {
                                    parseIcon(xpp, nodeStack, onNodeChange)
                                }

                                xpp.name == "arrowlink" -> {
                                    parseArrowLink(xpp, nodeStack)
                                }

                                else -> {
                                    Log.d(MainApplication.TAG, "Received unknown node " + xpp.name);
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
        onNodeChange: (nodeChange: NodeChange) -> Unit,
        onRootNodeLoaded: (rootNode: MindmapNode?) -> Unit,
    ) {
        var parentNode: MindmapNode? = null
        if (!nodeStack.empty()) {
            parentNode = nodeStack.peek()
        }

        val newMindmapNode = NodeUtils.parseNodeTag(xpp, parentNode)
        nodeStack.push(newMindmapNode)

        onNodeChange(SubscribeNodeRichContentChanged(newMindmapNode))

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
            onRootNodeLoaded(rootNode)
        } else {
            parentNode.addChildMindmapNode(newMindmapNode)
            if (parentNode.hasAddedChildMindmapNodeSubscribers()) { //si le node est a ajouter
                Log.e("toto", "add node:(${newMindmapNode.id})${newMindmapNode.getNodeText(this)}")
//                Log.e("toto", "add node:${newMindmapNode}")
                _uiState.update {
                    it.copy(
                        rootNode = parentNode
                    )
                }

                parentNode.notifySubscribersAddedChildMindmapNode(newMindmapNode)
            }
        }
    }

    private fun isRichContent(xpp: XmlPullParser) = (xpp.name == "richcontent"
        && (xpp.getAttributeValue(null, "TYPE") == "NODE"
        || xpp.getAttributeValue(null, "TYPE") == "NOTE"
        || xpp.getAttributeValue(null, "TYPE") == "DETAILS"
        ))

    // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
    // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").

    // if this is an empty tag, we won't need to bother trying to read its content
    // we don't even need to read the <richcontent> node's attributes, as we would
    // only be interested in it's children
    private fun parseRichContent(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>, onNodeChange: (nodeChange: NodeChange) -> Unit) {
        if (xpp.isEmptyElementTag) {
            Log.d(MainApplication.TAG, "Received empty richcontent node - skipping")
        } else {
            val richTextContent = NodeUtils.loadRichContentNodes(xpp)

            // if we have no parent node, something went seriously wrong - we can't have a richcontent that is not part of a viewModel node
            check(!nodeStack.empty()) { "Received richtext without a parent node" }

            val parentNode = nodeStack.peek()
            parentNode.addRichTextContent(richTextContent)

            // let view know that node content has changed
            if (parentNode.hasNodeRichContentChangedSubscribers()) {
                onNodeChange(RichContentChanged(parentNode))
            }
        }
    }

    private fun parseIcon(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>, onNodeChange: (nodeChange: NodeChange) -> Unit) {
        val iconName = xpp.getAttributeValue(null, "BUILTIN")

        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received icon without a parent node" }

        val parentNode = nodeStack.peek()
        parentNode.addIconName(iconName)

        // let view know that node content has changed
        if (parentNode.hasNodeStyleChangedSubscribers()) {
            onNodeChange(NodeStyleChanged(parentNode))
        }
    }

    private fun parseFont(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>, onNodeChange: (nodeChange: NodeChange) -> Unit) {
        val boldAttribute = xpp.getAttributeValue(null, "BOLD")

        // if we have no parent node, something went seriously wrong - we can't have a font node that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received richtext without a parent node" }
        val parentNode = nodeStack.peek()

        if (boldAttribute != null && boldAttribute == "true") {
            parentNode.isBold = true
        }

        val italicsAttribute = xpp.getAttributeValue(null, "ITALIC")
        if (italicsAttribute != null && italicsAttribute == "true") {
            parentNode.isItalic = true
        }

        // let view know that node content has changed
        if (parentNode.hasNodeStyleChangedSubscribers()) {
            onNodeChange(NodeStyleChanged(parentNode))
        }
    }

    private fun parseArrowLink(xpp: XmlPullParser, nodeStack: Stack<MindmapNode>) {
        val destinationId = xpp.getAttributeValue(null, "DESTINATION")

        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received arrowlink without a parent node" }

        val parentNode = nodeStack.peek()
        parentNode.addArrowLinkDestinationId(destinationId)
    }

    fun getMindmapDirectoryPath(): String? {
        // link is relative to viewModel file
        val mindmapPath = currentMindMapUri?.path
        Log.d(MainApplication.TAG, "MainViewModel path $mindmapPath")
        val mindmapDirectoryPath = mindmapPath?.substring(0, mindmapPath.lastIndexOf("/"))
        Log.d(MainApplication.TAG, "MainViewModel directory path $mindmapDirectoryPath")
        return mindmapDirectoryPath
    }

    fun onNodeClick(node: MindmapNode) {
        when {
            node.childMindmapNodes.size > 0 -> {
//                Log.e(
//                    "toto", """
//                parent:${node.parentNode}
//                children:
//                    ${node.childMindmapNodes.joinToString(separator = "\n", prefix = "      ")}
//                """.trimIndent()
//                )
                Log.e(
                    "toto", """
-----------------------------------
parent:${node.parentNode?.id}   
children:${node.id}   
${node.childMindmapNodes.joinToString(separator = "\n", transform = { "(${it.id})${it.getNodeText(this)}"})}
                """.trimIndent()
                )
                showNode(node)
            }

            node.link != null -> {
                _uiState.update {
                    it.copy(
                        selectedNode = node,
                        selectedNodeType = Link,
                    )
                }
            }

            node.richTextContents.isNotEmpty() -> {
                _uiState.update {
                    it.copy(
                        selectedNode = node,
                        selectedNodeType = RichText,
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
        _uiState.update {
            it.copy(
                selectedNode = node,
                rootNode = node
            )
        }
//        val nodeColumn: NodeColumn = NodeColumn(context, node, vm)
//        addColumn(nodeColumn)
//        // keep track of which list view belongs to which node column. This is necessary because onItemClick will get a
//        // ListView (the one that was clicked), and we need to know which NodeColumn this is.
//        val nodeColumnListView = nodeColumn.listView
//        listViewToNodeColumn[nodeColumnListView] = nodeColumn

        // then scroll all the way to the right
//        scrollToRight()

        enableHomeButtonIfNeeded(node)

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        setTitle(node.getNodeText(this))

        // mark node as selected
        node.isSelected = true
    }

    private fun enableHomeButtonIfNeeded(node: MindmapNode?) {
        _uiState.update {
            it.copy(
                canGoBack = node?.parentNode != null
            )
        }
    }

    fun setTitle(title: String?) {
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
                _uiState.update {
                    it.copy(
                        rootNode = node.parentNode,
                        selectedNode = node.parentNode?.parentNode,
                    )
                }

                // enable the up navigation with the Home (app) button (top left corner)
                enableHomeButtonIfNeeded(node.parentNode)

                // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
                setTitle(node.parentNode?.getNodeText(this))
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
        return depthFirstSearchRecursive(rootNode,parentNodeId)
    }

    private fun depthFirstSearchRecursive(
        node: MindmapNode?,
        targetId: String,
    ): MindmapNode? {
        if (node == null){
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

    fun searchNext() {
        TODO("Not yet implemented")
    }

    fun searchPrevious() {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

//    fun search(
//        searchString: String,
//        viewModel: MainViewModel,
//    ): List<MindmapNode> {
//        val res = ArrayList<MindmapNode>()
//        if (getNodeText(viewModel)?.uppercase(Locale.getDefault())?.contains(searchString.uppercase(Locale.getDefault())) == true) { // TODO: npe here when text is null, because text is a rich text
//            res.add(this)
//        }
//        for (child in childMindmapNodes) {
//            res.addAll(
//                child.search(
//                    searchString,
//                    viewModel
//                )
//            )
//        }
//        return res
//    }
}

