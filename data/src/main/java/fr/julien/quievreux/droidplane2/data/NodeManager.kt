package fr.julien.quievreux.droidplane2.data

import android.net.Uri
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.MindmapNode
import fr.julien.quievreux.droidplane2.data.model.NodeType.ArrowLink
import fr.julien.quievreux.droidplane2.data.model.NodeType.Font
import fr.julien.quievreux.droidplane2.data.model.NodeType.Icon
import fr.julien.quievreux.droidplane2.data.search.SearchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Stack

class NodeManager(
    private val logger: Logger,
    private val nodeUtils: NodeUtils,
    private val xmlParseUtils: XmlParseUtils,
    val coroutineScope: CoroutineScope,
) {
//    private val random = Random(System.currentTimeMillis())

    private var currentMindMapUri: Uri? = null

    private val _allNodes = MutableStateFlow(emptyList<MindmapNode>())

    private val searchManager = SearchManager(
        scope = coroutineScope,
        logger = logger,
        nodesSource = _allNodes,
        fetchText = { node -> node.getNodeText(this) }
    )
    /**
     * A map that resolves node IDs to Node objects
     */
    private var mindmapIndexes: MindmapIndexes? = null

    var rootNode: MindmapNode? = null
    private set

    fun getNodeByID(id: String?) = mindmapIndexes?.nodesByIdIndex?.get(id)

    fun getNodeByNumericIndex() = mindmapIndexes?.nodesByNumericIndex

    fun getNodeByIdIndex() = mindmapIndexes?.nodesByIdIndex

    fun updatemMindmapIndexes(mindmapIndexes: MindmapIndexes) {
        this.mindmapIndexes = mindmapIndexes
    }

    /**
     * Loads a mind map (*.mm) XML document into its internal DOM tree
     *
     * @param inputStream the inputStream to load
     */
    suspend fun loadMindMap(
        inputStream: InputStream,
        onError: (Exception) -> Unit ,
        onParentNodeUpdate: (MindmapNode) -> Unit,
        onLoadFinished: (() -> Unit)? = null,
    ) {
        val xpp :XmlPullParser?
        try {
            // set up XML pull parsing
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            xpp = factory.newPullParser()
            xpp.setInput(inputStream, "UTF-8")

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        xpp?.let {
            loadMindMap(
                xpp = it,
                onParentNodeUpdate = onParentNodeUpdate,
                onReadFinish = onLoadFinished,
                onError = onError,
            )
        }
    }

    suspend fun loadMindMap(
        xpp: XmlPullParser,
        onError: (Exception) -> Unit ,
        onParentNodeUpdate: (MindmapNode) -> Unit,
        onReadFinish: (() -> Unit)? = null,
    ) {
        val nodeStack = Stack<MindmapNode>()

        // stream parse the XML
        var eventType = xpp.eventType
        var hasStartDocument = false
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_DOCUMENT -> {
                    hasStartDocument = true
                    logger.e("Received XML Start Document")
                }

                XmlPullParser.START_TAG -> {
                    when {
                        xpp.name == "node" -> {
                            parseNode(
                                nodeStack = nodeStack,
                                xpp = xpp,
                                onParentNodeUpdate = onParentNodeUpdate,
                            )
                        }

                        xpp.isRichContent() -> {
                            xmlParseUtils.parseRichContent(xpp, nodeStack)
                        }

                        xpp.name == Font.value -> {
                            xmlParseUtils.parseFont(xpp, nodeStack)
                        }

                        xpp.name == Icon.value && xpp.getAttributeValue(null, "BUILTIN") != null -> {
                            xmlParseUtils.parseIcon(xpp, nodeStack)
                        }

                        xpp.name == ArrowLink.value -> {
                            xmlParseUtils.parseArrowLink(xpp, nodeStack)
                        }

                        else -> {
                            logger.d( "Received unknown node " + xpp.name)
                        }
                    }
                }

                XmlPullParser.END_TAG -> {
                    if(hasStartDocument.not()){
                        onError(RuntimeException("Received END_DOCUMENT without START_DOCUMENT"))
                    }
                    if (xpp.name == "node") {
                        nodeStack.pop()
                    }
                }

                XmlPullParser.TEXT -> {
                    // do we have TEXT nodes in the viewModel at all?
                }

                else -> {
                    onError(IllegalStateException("Received unknown event $eventType"))
                }
            }

            eventType = xpp.next()
        }

        // stack should now be empty
        if (!nodeStack.empty()) {
            onError(RuntimeException("Stack should be empty"))
            // TODO: we could try to be lenient here to allow opening partial documents
            //  (which sometimes happens when dropbox doesn't fully sync).
            //  Probably doesn't work anyways, as we already throw a runtime exception above if we receive garbage
        }

        onReadFinish?.invoke()
        processMindMap()
    }

    private fun processMindMap() {
        // TODO: can we do this as we stream through the XML above?
        // load all nodes of root node into simplified MindmapNode, and index them by ID for faster lookup
        updatemMindmapIndexes(nodeUtils.loadAndIndexNodesByIds(rootNode))

        // Nodes can refer to other nodes with arrowlinks. We want to have the link on both ends of the link, so we can
        // now set the corresponding links
        nodeUtils.fillArrowLinks(getNodeByIdIndex())
    }

    public fun parseNode(
        nodeStack: Stack<MindmapNode>,
        xpp: XmlPullParser,
        onParentNodeUpdate: (MindmapNode) -> Unit,
    ) {
        val parentNode: MindmapNode? = getParentFromStack(nodeStack)

        val newMindmapNode = nodeUtils.parseNodeTag(xpp, parentNode)

        nodeStack.push(newMindmapNode)

        // if we don't have a parent node, then this is the root node
        if (parentNode == null) {
            onParentNodeUpdate(newMindmapNode)
            updateNodeInstances(newMindmapNode)
        } else {
            //TODO change to immutable list
            parentNode.addChildMindmapNode(newMindmapNode)
//            parentNode = parentNode.copy(
//                childMindmapNodes = parentNode.childMindmapNodes + newMindmapNode
//            )

            _allNodes.update {
                it + newMindmapNode
            }
        }
    }

    fun updateNodeInstances(newMindmapNode: MindmapNode) {
        rootNode = newMindmapNode
        _allNodes.update {
            listOf(newMindmapNode)
        }
    }

    private fun getParentFromStack(nodeStack: Stack<MindmapNode>): MindmapNode? {
        var parentNode: MindmapNode? = null
        if (!nodeStack.empty()) {
            parentNode = nodeStack.peek()
        }
        return parentNode
    }

    //TODO Try with clone
    // TODO use suspend
    /** Depth-first search in the core text of the nodes in this sub-tree.  */ // TODO: this doesn't work while viewModel is still loading
    fun findFilledNode(
        parentNodeId: String,
    ): MindmapNode? = depthFirstSearchRecursive(rootNode, parentNodeId)

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

    fun search(
        query: String,
        onResultFound: () -> Unit,
    ) {
        searchManager.search(query, onResultFound)
    }

    fun getResultCount() = searchManager.getResultCount()

    fun getSearchResult(): List<MindmapNode> = searchManager.getSearchResult().value

    fun getSearchResultFlow(): StateFlow<List<MindmapNode>> = searchManager.getSearchResult()

    fun getSearchResultCount() = searchManager.getSearchResult().value.size

    companion object {
        const val UNDEFINED_NODE_ID: Int = 2000000000
    }

//    fun generateNodeID(): String {
//        var returnValue: String
//        do {
//            val prefix = "ID_"
//            /*
//			 * The prefix is to enable the id to be an ID in the sense of
//			 * XML/DTD.
//			 */
//            returnValue = prefix + random.nextInt(UNDEFINED_NODE_ID).toString()
//        } while (nodes.containsKey(returnValue))
//        return returnValue
//    }

    fun getMindmapDirectoryPath(debug: Boolean = false): String? {
        val mindmapPath = currentMindMapUri?.path
        if(debug)logger.d( "MainViewModel path $mindmapPath")
        val mindmapDirectoryPath = mindmapPath?.substring(0, mindmapPath.lastIndexOf("/"))
        if(debug)logger.d("MainViewModel directory path $mindmapDirectoryPath")
        return mindmapDirectoryPath
    }

    fun setMapUri(data: Uri?) {
        currentMindMapUri = data
    }

}