package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.NodeUtils.fillArrowLinks
import fr.julien.quievreux.droidplane2.data.XmlParseUtils.parseArrowLink
import fr.julien.quievreux.droidplane2.data.XmlParseUtils.parseFont
import fr.julien.quievreux.droidplane2.data.XmlParseUtils.parseIcon
import fr.julien.quievreux.droidplane2.data.XmlParseUtils.parseRichContent
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.MindmapNode
import fr.julien.quievreux.droidplane2.data.model.NodeType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.InputStream
import java.util.Stack
import kotlin.random.Random

class NodeManager(
    private val logger: Logger?=null,
    coroutineScope: CoroutineScope,
) {
    private val random = Random(System.currentTimeMillis())

    /**
     * A map that resolves node IDs to Node objects
     */
    private var mindmapIndexes: MindmapIndexes? = null

    private var rootNode: MindmapNode? = null

    private val _allNodes = MutableStateFlow(emptyList<MindmapNode>())

    //first state whether the search is happening or not
    private val _isSearching = MutableStateFlow(false)

    //second state the text typed by the user
    private val _searchText = MutableStateFlow("")
    private val searchText = _searchText.asStateFlow()

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
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5000),//it will allow the StateFlow survive 5 seconds before it been canceled
            initialValue = _allNodes.value
        )

    fun getRootNode(): MindmapNode? = rootNode

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    fun getNodeByID(id: String?): MindmapNode? = mindmapIndexes?.nodesByIdIndex?.get(id)

    fun getNodeByNumericID(numericId: Int?): MindmapNode? = mindmapIndexes?.nodesByNumericIndex?.get(numericId)

    fun getNodeByNumericIndex(): Map<Int, MindmapNode>? {
        return mindmapIndexes?.nodesByNumericIndex
    }

    fun getNodeByIdIndex(): Map<String, MindmapNode>? {
        return mindmapIndexes?.nodesByIdIndex
    }

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
        onParentNode: (MindmapNode) -> Unit,
        onLoadFinished: () -> Unit,
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
                onParentNode = onParentNode,
                onReadFinished = onLoadFinished,
                onError = onError,
            )
        }
    }

    suspend fun loadMindMap(
        xpp: XmlPullParser,
        onError: (Exception) -> Unit ,
        onParentNode: (MindmapNode) -> Unit,
        onReadFinished: () -> Unit,
    ) {
        val nodeStack = Stack<MindmapNode>()

        // stream parse the XML
        var eventType = xpp.eventType
        var hasStartDocument = false
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_DOCUMENT -> {
                    hasStartDocument = true
                    logger?.e("Received XML Start Document")
                }

                XmlPullParser.START_TAG -> {
                    when {
                        xpp.name == "node" -> {
                            parseNode(
                                nodeStack = nodeStack,
                                xpp = xpp,
                                onParentNode = onParentNode,
                            )
                        }

                        xpp.isRichContent() -> {
                            parseRichContent(xpp, nodeStack)
                        }

                        xpp.name == NodeType.Font.value -> {
                            parseFont(xpp, nodeStack)
                        }

                        xpp.name == NodeType.Icon.value && xpp.getAttributeValue(null, "BUILTIN") != null -> {
                            parseIcon(xpp, nodeStack)
                        }

                        xpp.name == NodeType.ArrowLink.value -> {
                            parseArrowLink(xpp, nodeStack)
                        }

                        else -> {
                            logger?.d( "Received unknown node " + xpp.name)
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

        onReadFinished()
        processMindMap()
    }

    private fun processMindMap() {
        // TODO: can we do this as we stream through the XML above?
        // load all nodes of root node into simplified MindmapNode, and index them by ID for faster lookup
        updatemMindmapIndexes(NodeUtils.loadAndIndexNodesByIds(rootNode))

        // Nodes can refer to other nodes with arrowlinks. We want to have the link on both ends of the link, so we can
        // now set the corresponding links
        fillArrowLinks(getNodeByIdIndex())
    }

    private fun parseNode(
        nodeStack: Stack<MindmapNode>,
        xpp: XmlPullParser,
        onParentNode: (MindmapNode) -> Unit,
    ) {
        var parentNode: MindmapNode? = null
        if (!nodeStack.empty()) {
            parentNode = nodeStack.peek()
        }

        val newMindmapNode = NodeUtils.parseNodeTag(xpp, parentNode)
        nodeStack.push(newMindmapNode)

        // if we don't have a parent node, then this is the root node
        if (parentNode == null) {
            onParentNode(newMindmapNode)
            rootNode = newMindmapNode
            _allNodes.update {
                listOf(newMindmapNode)
            }
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
        _searchText.update {
            query
        }
        _isSearching.update {
            query.isNotEmpty()
        }
        logger?.e(
            "toto", """
query:$query 
isSearching:${_isSearching.value} 
find:${nodeFindList.value.joinToString(separator = "|", transform = { it.getNodeText(this).orEmpty() })} 
        """.trimIndent()
        )

        if (nodeFindList.value.size == 1) {
            onResultFound()
        }
    }

    fun getResultCount() = nodeFindList.value.size

    fun getSearchResult() = nodeFindList

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

}