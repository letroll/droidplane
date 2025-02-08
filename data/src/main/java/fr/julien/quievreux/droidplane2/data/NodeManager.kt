package fr.julien.quievreux.droidplane2.data

import android.net.Uri
import android.text.Html
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.*
import fr.julien.quievreux.droidplane2.data.model.NodeRelation
import fr.julien.quievreux.droidplane2.data.model.NodeTag
import fr.julien.quievreux.droidplane2.data.model.NodeTag.*
import fr.julien.quievreux.droidplane2.data.model.NodeType.ArrowLink
import fr.julien.quievreux.droidplane2.data.model.NodeType.Font
import fr.julien.quievreux.droidplane2.data.search.SearchManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import org.xmlpull.v1.XmlSerializer
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.abs
import kotlin.random.Random

class NodeManager(
    private val logger: Logger,
    private val nodeUtils: NodeUtils,
    private val xmlParseUtils: XmlParseUtils,
    val coroutineScope: CoroutineScope,
) {

    private val _allNodes = MutableStateFlow(emptyList<Node>())

    val allNodesId = _allNodes.map { nodes ->
        nodes.map { node ->
            node.numericId
        }
    }

    /**
     * A map that resolves node IDs to Node objects
     */
    private var mindmapIndexes: MindmapIndexes? = null

    var rootNode: Node? = null
        private set

    private var currentMindMapUri: Uri? = null

    private val searchManager = SearchManager(
        scope = coroutineScope,
        logger = logger,
        nodesSource = _allNodes,
        fetchText = { node -> getNodeText(node) }
    )

    fun getNodeByID(id: String?): Node? = mindmapIndexes?.nodesByIdIndex?.get(id)

    fun getNodeByNumericIndex(): Map<Int, Node>? = mindmapIndexes?.nodesByNumericIndex

    fun getNodeByIdIndex() = mindmapIndexes?.nodesByIdIndex

    inline fun <reified K, reified V> Map<K, V>?.orMutableMap(): MutableMap<K, V> = if (this == null) mutableMapOf() else toMutableMap()

    fun getNodeByNumericId(nodeId: Int): Node? = getNodeByID(getNodeID(nodeId))

    fun getNodeParent(childNodeId: Int): Node? = getNodeByNumericId(childNodeId)?.parentNode

    fun updatemMindmapIndexes(mindmapIndexes: MindmapIndexes) {
        this.mindmapIndexes = mindmapIndexes
    }

    fun updateNodeInMindMapIndexes(node: Node) {
        val nodesByIdIndex = getNodeByIdIndex().orMutableMap()
        val nodesByNumericIndex = getNodeByNumericIndex().orMutableMap()
        nodesByIdIndex[node.id] = node
        nodesByNumericIndex[node.numericId] = node

        updatemMindmapIndexes(
            MindmapIndexes(
                nodesByIdIndex = nodesByIdIndex,
                nodesByNumericIndex = nodesByNumericIndex
            )
        )
    }

    /**
     * Loads a mind map (*.mm) XML document into its internal DOM tree
     *
     * @param inputStream the inputStream to load
     */
    suspend fun loadMindMapFromInputStream(
        inputStream: InputStream,
        onError: (Exception) -> Unit,
        onParentNodeUpdate: (Node) -> Unit,
        onLoadFinished: (() -> Unit)? = null,
    ) {
        logger.e("loadMindMapFromInputStream")
        val xpp: XmlPullParser?
        try {
            // set up XML pull parsing
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            xpp = factory.newPullParser()
            xpp.setInput(inputStream, "UTF-8")
            xpp?.let {
                loadMindMapFromXml(
                    xpp = it,
                    onParentNodeUpdate = onParentNodeUpdate,
                    onReadFinish = onLoadFinished,
                    onError = onError,
                )
            }
        } catch (exeception: Exception) {
            onError(exeception)
        }
    }

    fun loadMindMapFromXml(
        xpp: XmlPullParser,
        onError: (Exception) -> Unit,
        onParentNodeUpdate: (Node) -> Unit,
        onReadFinish: (() -> Unit)? = null,
    ) {
        logger.e("loadMindMapFromXml")
        try {
            val nodes = mutableListOf<Node>()
            var eventType = xpp.eventType
            var hasStartDocument = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {
                        hasStartDocument = true
                        logger.e("Received XML Start Document")
                    }

                    XmlPullParser.START_TAG -> {
                        loadXmlTagNode(xpp, nodes, onParentNodeUpdate)
                    }

                    XmlPullParser.END_TAG -> {
                        if (hasStartDocument.not()) {
                            onError(Exception("Received END_DOCUMENT without START_DOCUMENT"))
                        }
                        if (xpp.name == "node") {
                            nodes.removeAt(nodes.size - 1)
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
            if (nodes.isNotEmpty()) {
                onError(Exception("Stack should be empty"))
                // TODO: we could try to be lenient here to allow opening partial documents
                //  (which sometimes happens when dropbox doesn't fully sync).
                //  Probably doesn't work anyways, as we already throw a runtime exception above if we receive garbage
            }

            onReadFinish?.invoke()
            processMindMap()
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    private fun loadXmlTagNode(xpp: XmlPullParser, nodes: MutableList<Node>, onParentNodeUpdate: (Node) -> Unit) {
        with(xmlParseUtils) {
            when {
                xpp.name == NODE.text -> parseNodeText(
                    nodes = nodes,
                    xpp = xpp,
                    addChildIntoParent = ::addChildIntoParent,
                    onParentNodeUpdate = { updatedRootNode ->
                        onParentNodeUpdate(updatedRootNode)
                        updateRootNode(updatedRootNode)
                    },
                )

                xpp.isRichContent() -> parseRichContent(xpp, nodes)

                xpp.name == Font.value -> parseFont(xpp, nodes)

                xpp.isIcon() -> parseIcon(xpp, nodes)

                xpp.name == ArrowLink.value -> parseArrowLink(xpp, nodes)

                else -> {
//                logger.w("Received unknown node " + xpp.name)
                }
            }
        }
    }

    private fun processMindMap() {
        // TODO: can we do this as we stream through the XML above?
        // load all nodes of root node into simplified Node, and index them by ID for faster lookup
        updatemMindmapIndexes(nodeUtils.loadAndIndexNodesByIds(rootNode))

        // Nodes can refer to other nodes with arrowlinks. We want to have the link on both ends of the link, so we can
        // now set the corresponding links
        nodeUtils.fillArrowLinks(getNodeByIdIndex())
    }

    fun addChildIntoParent(nodeRelation: NodeRelation) {
        //TODO change to immutable list
        nodeRelation.parent.addChildMindmapNode(nodeRelation.child)
        //            parentNode = parentNode.copy(
        //                childNodes = parentNode.childNodes + newMindmapNode
        //            )

        _allNodes.update {
            it + nodeRelation.child
        }
    }

    fun updateRootNode(newNode: Node) {
        rootNode = newNode
        _allNodes.update {
            listOf(newNode)
        }
    }

    fun getNodeText(node: Node): String? {
        getNodeByID(node.id)?.let { actualNode ->
            // if this is a cloned node, get the text from the original node
            if (actualNode.isClone()) {
                // TODO this now fails when loading, because the background indexing is not done yet - so we maybe should mark this as "pending", and put it into a queue, to be updated once the linked node is there
                val linkedNode = getNodeByID(actualNode.treeIdAttribute)
                if (linkedNode != null) {
                    return getNodeText(linkedNode)
                }
            }

            // if this is a rich text node, get the HTML content instead
            if (actualNode.text == null && actualNode.richTextContents.isNotEmpty()) {
                val richTextContent = actualNode.richTextContents.first()
                return Html.fromHtml(richTextContent).toString()
            }

            return actualNode.text
        } ?: run {
            return node.text
        }
    }

    private fun getParentFromStack(nodes: MutableList<Node>): Node? {
        var parentNode: Node? = null
        if (nodes.isNotEmpty()) {
            parentNode = nodes.last()
        }
        return parentNode
    }

    //TODO Try with clone
    // TODO use suspend
    /** Depth-first search in the core text of the nodes in this sub-tree.  */ // TODO: this doesn't work while viewModel is still loading
    suspend fun findFilledNode(
        parentNodeId: String,
    ): Node? {
        // Get all nodes
        val nodes = _allNodes.first()

        // Find the target node
        val targetNode = nodes.find { it.id == parentNodeId } ?: return null

        // Create a map of parent ID to children
        val childrenByParentId = nodes.groupBy { it.parentNode?.id }

        // Function to recursively rebuild a node with its children
        fun rebuildNode(node: Node): Node {
            val children = childrenByParentId[node.id] ?: emptyList()
            return node.copy(
                childNodes = children.map { rebuildNode(it) }.toMutableList(),
                parentNode = nodes.find { it.id == node.parentNode?.id }
            )
        }

        // Rebuild the target node with its children
        return rebuildNode(targetNode)
    }

    private fun depthFirstSearchRecursive(
        node: Node?,
        targetId: String,
    ): Node? {
        if (node == null) {
            return null
        }
        if (node.id == targetId) {
            return node
        }
        for (child in node.childNodes) {
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

    fun getSearchResult(): List<Node> = searchManager.getSearchResult().value

    fun getSearchResultFlow(): StateFlow<List<Node>> = searchManager.getSearchResult()

    fun getSearchResultCount() = searchManager.getSearchResult().value.size

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
        if (debug) logger.e("uri:$currentMindMapUri")
        val mindmapPath = currentMindMapUri?.path
        if (debug) logger.e("path $mindmapPath")
        val mindmapDirectoryPath = mindmapPath?.substring(0, mindmapPath.lastIndexOf("/"))
        if (debug) logger.e("directory path $mindmapDirectoryPath")
        return mindmapDirectoryPath
    }

    fun getMindmapFileName(debug: Boolean = false): String? {
        if (debug) logger.e("uri:$currentMindMapUri")
        val mindmapPath = currentMindMapUri?.path
        if (debug) logger.e("path $mindmapPath")
        val mindmapFileName = mindmapPath?.substring(mindmapPath.lastIndexOf("/") + 1, mindmapPath.length)
        if (debug) logger.e("filename $mindmapFileName")
        return mindmapFileName
    }

    fun setMapUri(data: Uri?) {
        currentMindMapUri = data
        logger.e("setMapUri uri:$currentMindMapUri")
    }

    suspend fun serializeMindmap(
        filePath: String,
        filename: String,
        onError: (Exception) -> Unit,
        onSaveFinished: ((File) -> Unit)? = null,
    ) {
        if (isInvalidFilePath(filePath)) onError.invoke(
            Exception("Invalid file path")
        )

        if (isInvalidFileName(filename)) onError.invoke(
            Exception("Invalid file name")
        )

        val fileSaveDestination = File(filePath, filename)
        try {
            withContext(Dispatchers.IO) {
                logger.e("Saving mindmap to $fileSaveDestination")
                logger.e("Current nodes: ${_allNodes.first().size}")

                // First write to a temporary file
                val tempFile = File(filePath, "temp_$filename")
                logger.e("Writing to temp file: $tempFile")

                try {
                    FileOutputStream(tempFile).use { outputStream ->
                        val factory = XmlPullParserFactory.newInstance()
                        val serializer = factory.newSerializer()

                        serializer.setOutput(outputStream, "UTF-8")
                        serializer.startDocument("UTF-8", true)
                        serializer.text(CHARIOT_RETURN)
                        serializer.startNodeTag(MAP)

                        // Get all nodes and create a map of parent ID to children
                        val nodes = _allNodes.first()
                        val childrenByParentId = nodes.groupBy { it.parentNode?.id }
                        logger.e("Children by parent: ${childrenByParentId.map { "${it.key}: ${it.value.size} children" }}")

                        // Find the root node (node with no parent)
                        val rootNodes = nodes.filter { it.parentNode == null }
                        logger.e("Found ${rootNodes.size} root nodes")

                        rootNodes.forEach { rootNode ->
                            logger.e("Serializing root node: ${rootNode.id}")
                            serializeNode(
                                serializer = serializer,
                                node = rootNode,
                                childrenByParentId = childrenByParentId,
                                onError = onError,
                            )
                        }

                        serializer.endNodeTag(MAP)
                        serializer.endDocument()
                    }

                    // Now read from temp file, remove CDATA tags, and write to final file
                    logger.e("Reading from temp file and writing to final file")
                    tempFile.useLines { lines ->
                        fileSaveDestination.bufferedWriter().use { writer ->
                            lines.forEach { line ->
                                val cleanedLine = line.replace("<![CDATA[", "").replace("]]>", "")
                                writer.write(cleanedLine)
                                writer.newLine()
                            }
                        }
                    }

                    // Delete the temporary file
                    if (tempFile.exists()) {
                        val deleted = tempFile.delete()
                        logger.e("Temp file deleted: $deleted")
                    }

                    logger.e("Save completed successfully")
                } catch (e: Exception) {
                    logger.e("Error during save: ${e.message}")
                    logger.e("Stack trace: ${e.stackTraceToString()}")
                    // Clean up temp file if it exists
                    if (tempFile.exists()) {
                        try {
                            tempFile.delete()
                        } catch (e: Exception) {
                            logger.e("Failed to delete temp file: ${e.message}")
                        }
                    }
                    throw e
                }
            }
        } catch (e: Exception) {
            logger.e("Save failed: ${e.message}")
            onError(e)
        }
        onSaveFinished?.invoke(fileSaveDestination)
    }

    fun isInvalidFilePath(filePath: String): Boolean = filePath.isBlank() || !filePath.contains("/") || !filePath.startsWith("/")

    fun isInvalidFileName(fileName: String): Boolean = fileName.isBlank() || !fileName.endsWith(".mm") || fileName == FILE_EXTENSION

    private fun getTabsForDepth(depth: Int) = TAB.repeat(depth)

    private fun serializeNode(
        serializer: XmlSerializer,
        node: Node,
        childrenByParentId: Map<String?, List<Node>>,
        depth: Int = 1,
        onError: (Exception) -> Unit,
    ) {
        try {
            serializer.text("$CHARIOT_RETURN${getTabsForDepth(depth)}")
            serializer.startNodeTag(NODE)

            // Write attributes in the correct order for Freeplane compatibility
            serializer.nodeAttribute(ID, node.id)
            node.creationDate?.let { date ->
                serializer.nodeAttribute(CREATED, date.toString())
            }
            node.modificationDate?.let { date ->
                serializer.nodeAttribute(MODIFIED, date.toString())
            }
            getNodeText(node)?.let { text ->
                serializer.nodeAttribute(TEXT, text)
            }
            node.position?.let {
                serializer.nodeAttribute(POSITION, it)
            }
            if (node.richTextContents.isNotEmpty()) {
                serializer.text("$CHARIOT_RETURN${getTabsForDepth(depth + 1)}")
                serializer.startNodeTag(RICH_CONTENT)
                serializer.nodeAttribute(TYPE, node.richContentType?.text.orEmpty())
                node.richTextContents.forEach { richTextContent ->
                    val cleanedText = richTextContent.replace('\u00A0', ' ').replace("&#160;", " ")
                    serializer.cdsect(cleanedText)
                }
                serializer.text(CHARIOT_RETURN)
                serializer.endNodeTag(RICH_CONTENT)
                serializer.text(CHARIOT_RETURN)
            }

            //TODO Add other attributes as needed (icon, link, format, etc.)
            node.link?.let { link -> serializer.nodeAttribute(LINK, link.toString()) }

            // Serialize arrow links
            if (node.arrowLinkDestinationIds.isNotEmpty()) {
                node.arrowLinkDestinationIds.forEach { arrowLinkId ->
                    serializer.text("$CHARIOT_RETURN${getTabsForDepth(depth + 1)}")
                    serializer.startNodeTag(ARROWLINK)
                    serializer.nodeAttribute(DESTINATION, arrowLinkId)
                    // Add other arrowLink attributes as needed
                    serializer.endNodeTag(ARROWLINK)
                }
            }

            // Serialize icons
            if (node.iconNames.isNotEmpty()) {
                node.iconNames.forEach { iconName ->
                    serializer.text("$CHARIOT_RETURN${getTabsForDepth(depth + 1)}")
                    serializer.startNodeTag(ICON)
                    serializer.nodeAttribute(BUILTIN, iconName)
                    serializer.endNodeTag(ICON)
                }
            }

            // Serialize font styles
            if (node.isItalic || node.isBold) {
                serializer.text("$CHARIOT_RETURN${getTabsForDepth(depth + 1)}")
                serializer.startNodeTag(FONT)
                if (node.isItalic) serializer.nodeAttribute(ITALIC, "true")
                if (node.isBold) serializer.nodeAttribute(BOLD, "true")
                serializer.endNodeTag(FONT)
            }

            // Serialize child nodes recursively using the childrenByParentId map
            val children = childrenByParentId[node.id] ?: emptyList()
            children.forEach { childNode ->
                serializeNode(
                    serializer = serializer,
                    node = childNode,
                    childrenByParentId = childrenByParentId,
                    onError = onError,
                    depth = depth + 1,
                )
            }

            serializer.endNodeTag(NODE)
        } catch (exception: Exception) {
            onError(exception)
        }
    }

    private fun XmlSerializer.startNodeTag(nodeTag: NodeTag) {
        startTag(null, nodeTag.text)
    }

    private fun XmlSerializer.endNodeTag(nodeTag: NodeTag) {
        endTag(null, nodeTag.text)
    }

    private fun XmlSerializer.nodeAttribute(
        nodeAttribute: NodeAttribute,
        value: String,
    ) {
        attribute(null, nodeAttribute.text, value)
    }

    /**
     * addNode : try to add a node and return it's id on success
     */
    suspend fun addNodeToMindmap(newValue: String, parentNode: Node? = null): Int? {
        return if (newValue.isBlank()) {
            null
        } else {
            generateNodeNumericID().let { nodeNumericId ->
                val time = System.currentTimeMillis()

                // First create the new node
                var newNode = Node(
                    id = getNodeID(nodeNumericId),
                    numericId = nodeNumericId,
                    text = newValue,
                    parentNode = parentNode,
                    creationDate = time,
                    modificationDate = time,
                )

                var updatedParentNode: Node? = null

                _allNodes.update { nodes ->
                    val updatedNodes = if (parentNode != null) {
                        // If we have a parent, update it with the new child
                        nodes.map { node ->
                            if (node.id == parentNode.id) {
                                // Create a deep copy of the parent with all its mutable lists
                                val updatedParent = node.copy(
                                    childNodes = ArrayList(node.childNodes),
                                    richTextContents = ArrayList(node.richTextContents),
                                    iconNames = ArrayList(node.iconNames),
                                    arrowLinkDestinationIds = ArrayList(node.arrowLinkDestinationIds),
                                    arrowLinkDestinationNodes = ArrayList(node.arrowLinkDestinationNodes),
                                    arrowLinkIncomingNodes = ArrayList(node.arrowLinkIncomingNodes)
                                )
                                // Add the new node to the fresh copy of childNodes
                                updatedParent.childNodes.add(newNode)
                                // Keep reference to updated parent for later use
                                updatedParentNode = updatedParent
                                // Update the parent reference in the new node to point to the updated parent
                                newNode = newNode.copy(parentNode = updatedParent)
                                updatedParent
                            } else node
                        }.toMutableList()
                    } else {
                        // If no parent, this is a root node
                        nodes.toMutableList()
                    }

                    // Add the new node
                    updatedNodes.add(newNode)
                    updatedNodes
                }

                // Update the indexes
                if (parentNode != null) {
                    // If we have a parent, update it first
                    updatedParentNode?.let { parent ->
                        updateNodeInMindMapIndexes(parent)
                        // If parent is root or is in root's path, update rootNode
                        if (parent.id == rootNode?.id) {
                            rootNode = parent
                        } else {
                            // Find the path from root to parent
                            var current = parent
                            while (current.parentNode != null) {
                                current.parentNode?.let { new ->
                                    current = new
                                }
                                if (current.id == rootNode?.id) {
                                    rootNode = current
                                    break
                                }
                            }
                        }
                    }
                } else {
                    // If no parent, this is a root node
                    if (rootNode == null) {
                        rootNode = newNode
                    }
                }

                // Update the new node in indexes
                updateNodeInMindMapIndexes(newNode)

                nodeNumericId
            }
        }
    }

    suspend fun generateNodeNumericID(): Int {
        val currentIds = allNodesId.first().toSet()
        if (currentIds.size >= Int.MAX_VALUE) {
            throw IllegalStateException("No more available IDs")
        }
        var newId = 0
        while (currentIds.contains(newId)) {
            newId = abs(Random.nextInt(UNDEFINED_NODE_ID))
        }
        return newId
    }

    fun getNodeID(nodeNumericId: Int): String = "$NODE_ID_PREFIX$nodeNumericId"

    companion object {
        const val UNDEFINED_NODE_ID: Int = 2000000000
        const val FILE_EXTENSION = ".mm"
        const val NODE_ID_PREFIX = "ID_"
        const val CHARIOT_RETURN = "\n"
        const val TAB = "\t"
    }
}