package fr.julien.quievreux.droidplane2.data

import android.net.Uri
import android.text.Html
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.DeleteSnapshot
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
) : NodeManagerContract {

    private val _allNodes = MutableStateFlow(emptyList<Node>())
    override val allNodes: StateFlow<List<Node>> = _allNodes

    val allNodesId = _allNodes.map { nodes ->
        nodes.map { node ->
            node.numericId
        }
    }

    /**
     * A map that resolves node IDs to Node objects
     */
    private var mindmapIndexes: MindmapIndexes? = null

    override var rootNode: Node? = null
        private set

    private var currentMindMapUri: Uri? = null

    private val searchManager = SearchManager(
        scope = coroutineScope,
        logger = logger,
        nodesSource = _allNodes,
        fetchText = { node -> getNodeText(node) }
    )

    override fun getNodeByID(id: String?): Node? {
        val result = mindmapIndexes?.nodesByIdIndex?.get(id)
        println("DEBUG NodeManager.getNodeByID: id=$id, result=${result?.id}, parentNodeId=${result?.parentNode?.id}, childCount=${result?.childNodes?.size}, hashCode=${System.identityHashCode(result)}")
        return result
    }

    fun getNodeByNumericIndex(): Map<Int, Node>? = mindmapIndexes?.nodesByNumericIndex

    fun getNodeByIdIndex() = mindmapIndexes?.nodesByIdIndex

    inline fun <reified K, reified V> Map<K, V>?.orMutableMap(): MutableMap<K, V> = if (this == null) mutableMapOf() else toMutableMap()

    override fun getNodeByNumericId(nodeId: Int): Node? = getNodeByID(getNodeID(nodeId))

    override fun getNodeParent(childNodeId: Int): Node? = getNodeByNumericId(childNodeId)?.parentNode

    fun updatemMindmapIndexes(mindmapIndexes: MindmapIndexes) {
        println("DEBUG updatemMindmapIndexes: old hash=${this.mindmapIndexes?.hashCode()}, new hash=${mindmapIndexes.hashCode()}, new nodesById size=${mindmapIndexes.nodesByIdIndex.size}")
        this.mindmapIndexes = mindmapIndexes
        println("DEBUG updatemMindmapIndexes: new hash=${this.mindmapIndexes?.hashCode()}")
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
    override suspend fun loadMindMapFromInputStream(
        inputStream: InputStream,
        onError: (Exception) -> Unit,
        onParentNodeUpdate: (Node) -> Unit,
        onLoadFinished: (() -> Unit)?,
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
            val allParsedNodes = mutableListOf<Node>() // Collect all parsed nodes in order
            var eventType = xpp.eventType
            var hasStartDocument = false
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_DOCUMENT -> {
                        hasStartDocument = true
                        logger.e("Received XML Start Document")
                    }

                    XmlPullParser.START_TAG -> {
                        // Track nodes before parsing
                        val sizeBefore = _allNodes.value.size
                        loadXmlTagNode(xpp, nodes, onParentNodeUpdate)
                        // Capture newly added nodes
                        val sizeAfter = _allNodes.value.size
                        if (sizeAfter > sizeBefore) {
                            val newNodes = _allNodes.value.subList(sizeBefore, sizeAfter)
                            allParsedNodes.addAll(newNodes)
                        }
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
            }

            // After parsing, find the root node (first parsed node with no parent)
            if (rootNode == null && allParsedNodes.isNotEmpty()) {
                rootNode = allParsedNodes.firstOrNull { it.parentNode == null }
                // If not found, use the first node
                if (rootNode == null) {
                    rootNode = allParsedNodes.first()
                }
            }

            // Ensure _allNodes contains all parsed nodes in the correct order
            _allNodes.value = allParsedNodes

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
                    onParentNodeUpdate = { parsedNode ->
                        // For root node (no parent), add it to _allNodes
                        if (parsedNode.parentNode == null) {
                            _allNodes.update { it + parsedNode }
                        }
                        onParentNodeUpdate(parsedNode)
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

    override fun getNodeText(node: Node): String? {
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

    /**
     * Returns the full text content of a node for copying to clipboard.
     * Includes raw HTML for rich text nodes.
     */
    fun getNodeTextForCopy(node: Node): String? {
        getNodeByID(node.id)?.let { actualNode ->
            if (actualNode.isClone()) {
                val linkedNode = getNodeByID(actualNode.treeIdAttribute)
                if (linkedNode != null) {
                    return getNodeTextForCopy(linkedNode)
                }
            }

            // if this is a rich text node, return raw HTML
            if (actualNode.richTextContents.isNotEmpty()) {
                return actualNode.richTextContents.first()
            }

            return actualNode.text
        } ?: run {
            return node.text
        }
    }

    //TODO Try with clone
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

    override fun search(
        query: String,
        onResultFound: () -> Unit,
    ) {
        searchManager.search(query, onResultFound)
    }

    fun getResultCount() = searchManager.getResultCount()

    override fun getSearchResult(): List<Node> = searchManager.getSearchResult().value

    fun getSearchResultFlow(): StateFlow<List<Node>> = searchManager.getSearchResult()

    override fun getSearchResultCount(): Int = searchManager.getSearchResult().value.size

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

    override suspend fun serializeMindmap(
        filePath: String,
        filename: String,
        onError: (Exception) -> Unit,
        onSaveFinished: ((File) -> Unit)?,
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
    override suspend fun addNodeToMindmap(newValue: String, parentNode: Node?): Int? {
        if (newValue.isBlank()) {
            return null
        }
        val nodeNumericId = generateNodeNumericID()
        val time = System.currentTimeMillis()

        val newNode = Node(
            id = getNodeID(nodeNumericId),
            numericId = nodeNumericId,
            text = newValue,
            parentNode = parentNode,
            creationDate = time,
            modificationDate = time,
        )

        val updatedNodesMap = mutableMapOf<String, Node>()

        if (parentNode != null) {
            val currentParent = getNodeByID(parentNode.id) ?: parentNode
            val updatedParent = currentParent.copy(
                childNodes = ArrayList(currentParent.childNodes),
                richTextContents = ArrayList(currentParent.richTextContents),
                iconNames = ArrayList(currentParent.iconNames),
                arrowLinkDestinationIds = ArrayList(currentParent.arrowLinkDestinationIds),
                arrowLinkDestinationNodes = ArrayList(currentParent.arrowLinkDestinationNodes),
                arrowLinkIncomingNodes = ArrayList(currentParent.arrowLinkIncomingNodes),
                modificationDate = time,
            )
            val fixedNewNode = newNode.copy(parentNode = updatedParent)
            updatedParent.childNodes.add(fixedNewNode)

            updatedNodesMap[fixedNewNode.id] = fixedNewNode
            updatedNodesMap[updatedParent.id] = updatedParent

            propagateUpdatedParentToRoot(updatedParent, updatedNodesMap)

            _allNodes.update { nodes ->
                val existingUpdated = nodes.map { node ->
                    updatedNodesMap[node.id] ?: node
                }
                if (existingUpdated.none { it.id == fixedNewNode.id }) {
                    existingUpdated + fixedNewNode
                } else {
                    existingUpdated
                }
            }
        } else {
            if (rootNode == null) {
                rootNode = newNode
            }
            _allNodes.update { it + newNode }
        }

        val allNodes = _allNodes.value
        val newNodesById = allNodes.associateBy { it.id }
        val newNodesByNumeric = allNodes.associateBy { it.numericId }
        updatemMindmapIndexes(MindmapIndexes(newNodesById, newNodesByNumeric))

        return nodeNumericId
    }

    override suspend fun updateNodeText(nodeId: String, newText: String): Node? {
        val targetNode = getNodeByID(nodeId) ?: return null
        val time = System.currentTimeMillis()
        val updatedNode = targetNode.copy(
            text = newText,
            modificationDate = time,
        )

        var updatedParentNode: Node? = null
        _allNodes.update { nodes ->
            nodes.map { node ->
                when {
                    node.id == nodeId -> updatedNode

                    node.id == targetNode.parentNode?.id -> {
                        val childIndex = node.childNodes.indexOfFirst { it.id == nodeId }
                        if (childIndex != -1) {
                            val newChildren = ArrayList(node.childNodes)
                            newChildren[childIndex] = updatedNode
                            node.copy(childNodes = newChildren).also { updatedParentNode = it }
                        } else {
                            node
                        }
                    }

                    else -> node
                }
            }
        }

        updateNodeInMindMapIndexes(updatedNode)
        // Re-index the parent from the just-updated stream: the index still holds the
        // pre-edit parent copy, so re-reading it here would drop the new childNodes.
        updatedParentNode?.let { updatedParent ->
            updateNodeInMindMapIndexes(updatedParent)
            if (updatedParent.id == rootNode?.id) {
                rootNode = updatedParent
            }
        }

        if (targetNode.parentNode == null && updatedNode.id == rootNode?.id) {
            rootNode = updatedNode
        }

        return updatedNode
    }

    private fun updateDescendantParents(node: Node, updatedNodesMap: MutableMap<String, Node>): Node {
        node.childNodes.replaceAll { child ->
            val updatedChild = child.copy(parentNode = node)
            updatedNodesMap[updatedChild.id] = updatedChild
            updateDescendantParents(updatedChild, updatedNodesMap)
        }
        return node
    }

    private fun propagateUpdatedParentToRoot(
        updatedParent: Node,
        updatedNodesMap: MutableMap<String, Node>,
    ) {
        if (updatedParent.id == rootNode?.id) {
            rootNode = updatedParent
            return
        }

        var currentChild = updatedParent
        var currentParent = currentChild.parentNode?.id?.let { getNodeByID(it) }

        while (currentParent != null) {
            val newAncestorChildren = currentParent.childNodes.toMutableList()
            val childIdx = newAncestorChildren.indexOfFirst { it.id == currentChild.id }
            val updatedAncestor = currentParent.copy(
                childNodes = mutableListOf(),
                modificationDate = updatedParent.modificationDate ?: System.currentTimeMillis(),
            )

            val fixedChild = currentChild.copy(parentNode = updatedAncestor)
            updatedNodesMap[fixedChild.id] = fixedChild
            updateDescendantParents(fixedChild, updatedNodesMap)

            if (childIdx != -1) {
                newAncestorChildren[childIdx] = fixedChild
            }
            updatedAncestor.childNodes.addAll(newAncestorChildren)
            updatedAncestor.childNodes.replaceAll { c ->
                if (c.id == fixedChild.id) {
                    fixedChild
                } else {
                    val updatedC = c.copy(parentNode = updatedAncestor)
                    updatedNodesMap[updatedC.id] = updatedC
                    updateDescendantParents(updatedC, updatedNodesMap)
                }
            }
            updatedNodesMap[updatedAncestor.id] = updatedAncestor

            if (updatedAncestor.id == rootNode?.id) {
                rootNode = updatedAncestor
                break
            }
            currentChild = updatedAncestor
            currentParent = currentChild.parentNode?.id?.let { getNodeByID(it) }
        }
    }

    override suspend fun deleteNode(nodeId: String): Boolean {
        val targetNode = getNodeByID(nodeId) ?: return false

        // Prevent deletion of root node
        if (targetNode.parentNode == null) {
            logger.e("Attempted to delete root node, which is not allowed")
            return false
        }

        // Collect all node IDs to delete (target + all descendants)
        val idsToDelete = mutableSetOf<String>()
        fun collectDescendants(node: Node) {
            idsToDelete.add(node.id)
            for (child in node.childNodes) {
                collectDescendants(child)
            }
        }
        collectDescendants(targetNode)

        // Collect all numeric IDs to delete
        val numericIdsToDelete = idsToDelete.mapNotNull { id ->
            getNodeByID(id)?.numericId
        }.toSet()

        val directParent = getNodeByID(targetNode.parentNode?.id) ?: targetNode.parentNode!!
        val updatedNodesMap = mutableMapOf<String, Node>()

        // Filter out deleted nodes from parent's children
        val survivingChildren = directParent.childNodes.filter { it.id !in idsToDelete }
        val updatedParent = directParent.copy(childNodes = mutableListOf())
        val updatedChildren = survivingChildren.map { child ->
            val updatedChild = child.copy(parentNode = updatedParent)
            updatedNodesMap[updatedChild.id] = updatedChild
            updateDescendantParents(updatedChild, updatedNodesMap)
            updatedChild
        }.toMutableList()
        updatedParent.childNodes.addAll(updatedChildren)
        updatedNodesMap[updatedParent.id] = updatedParent

        // Propagate updated parent upward through ancestor nodes to rootNode
        propagateUpdatedParentToRoot(updatedParent, updatedNodesMap)

        // Update the nodes list: remove all deleted nodes, update modified nodes
        _allNodes.update { nodes ->
            nodes.filter { it.id !in idsToDelete }.map { node ->
                updatedNodesMap[node.id] ?: node
            }
        }

        // Rebuild indexes from the updated _allNodes to ensure consistency
        val allNodes = _allNodes.value
        val newNodesById = allNodes.associateBy { it.id }
        val newNodesByNumeric = allNodes.associateBy { it.numericId }
        updatemMindmapIndexes(MindmapIndexes(newNodesById, newNodesByNumeric))

        // Clean up external links for deleted nodes
        for (deletedId in idsToDelete) {
            val deletedNode = getNodeByID(deletedId)
            if (deletedNode?.link != null) {
                // Link is cleaned up with node removal
            }
        }

        val remainingNodes = _allNodes.value
        for (node in remainingNodes) {
            try {
                // Remove deleted node IDs from arrow link destination lists
                val updatedDestIds = node.arrowLinkDestinationIds.filter { it !in idsToDelete }.toMutableList()
                val updatedDestNodes = node.arrowLinkDestinationNodes.filter { it.id !in idsToDelete }.toMutableList()
                val updatedIncomingNodes = node.arrowLinkIncomingNodes.filter { it.id !in idsToDelete }.toMutableList()
                if (updatedDestIds != node.arrowLinkDestinationIds ||
                    updatedDestNodes != node.arrowLinkDestinationNodes ||
                    updatedIncomingNodes != node.arrowLinkIncomingNodes) {
                    val updatedNode = node.copy(
                        arrowLinkDestinationIds = updatedDestIds,
                        arrowLinkDestinationNodes = updatedDestNodes,
                        arrowLinkIncomingNodes = updatedIncomingNodes
                    )
                    updateNodeInMindMapIndexes(updatedNode)
                }
            } catch (e: Exception) {
                println("deleteNode: error cleaning arrow links for node ${node.id}: ${e.message}")
                e.printStackTrace()
            }
        }

        return true
    }

    override suspend fun createDeleteSnapshot(nodeId: String): DeleteSnapshot? {
        val targetNode = getNodeByID(nodeId) ?: return null
        
        // Prevent deletion of root node (handled at ViewModel level, but also enforce here)
        if (targetNode.parentNode == null) {
            logger.e("Attempted to create snapshot of root node, which is not allowed")
            return null
        }
        
        val parentNode = targetNode.parentNode!!
        val parentChildIndex = parentNode.childNodes.indexOfFirst { it.id == nodeId }
        if (parentChildIndex == -1) {
            logger.e("Node $nodeId not found in parent's children")
            return null
        }
        
        return DeleteSnapshot.create(targetNode, parentNode, parentChildIndex)
    }

    override suspend fun restoreSubtree(snapshot: DeleteSnapshot): Boolean {
        // Verify parent still exists
        val parentNode = getNodeByID(snapshot.parentNodeId) ?: return false
        
        // Verify the index is still valid
        if (snapshot.parentChildIndex < 0 || snapshot.parentChildIndex > parentNode.childNodes.size) {
            logger.e("Invalid parentChildIndex in snapshot: ${snapshot.parentChildIndex}")
            return false
        }
        
        // Rebuild the subtree with new node instances but preserving IDs and timestamps
        val restoredNodes = mutableMapOf<String, Node>()
        
        // First, rebuild all nodes in the subtree
        for (originalNode in snapshot.deletedSubtree) {
            // Create a copy with all original properties preserved
            val restoredNode = originalNode.copy()
            restoredNodes[originalNode.id] = restoredNode
        }
        
        // Rebuild parent-child relationships
        for (originalNode in snapshot.deletedSubtree) {
            val restoredNode = restoredNodes[originalNode.id]!!
            for (child in originalNode.childNodes) {
                val restoredChild = restoredNodes[child.id]
                if (restoredChild != null) {
                    // Update the child's parent reference
                    val updatedChild = restoredChild.copy(parentNode = restoredNode)
                    restoredNodes[child.id] = updatedChild
                }
            }
        }
        
        // Insert the restored subtree back into the parent's children at the original index
        val restoredRoot = restoredNodes[snapshot.deletedNodeId]!!
        val newChildren = parentNode.childNodes.toMutableList()
        newChildren.add(snapshot.parentChildIndex, restoredRoot)

        val updatedParent = parentNode.copy(childNodes = mutableListOf())
        val updatedRestoredRoot = restoredRoot.copy(parentNode = updatedParent)
        restoredNodes[snapshot.deletedNodeId] = updatedRestoredRoot

        val updatedParentChildren = newChildren.map { child ->
            if (child.id == snapshot.deletedNodeId) {
                updatedRestoredRoot
            } else {
                child.copy(parentNode = updatedParent)
            }
        }.toMutableList()
        updatedParent.childNodes.addAll(updatedParentChildren)

        val updatedNodesMap = mutableMapOf<String, Node>()
        updatedNodesMap[updatedParent.id] = updatedParent
        for (child in updatedParentChildren) {
            updatedNodesMap[child.id] = child
            updateDescendantParents(child, updatedNodesMap)
        }
        for ((k, v) in restoredNodes) {
            if (k != snapshot.deletedNodeId) {
                updatedNodesMap[k] = v
            }
        }

        propagateUpdatedParentToRoot(updatedParent, updatedNodesMap)

        // Update all nodes in the hierarchy
        _allNodes.update { nodes ->
            val existingUpdated = nodes.map { node ->
                updatedNodesMap[node.id] ?: node
            }
            val existingIds = existingUpdated.map { it.id }.toSet()
            val nodesToAdd = snapshot.deletedSubtree
                .filter { it.id !in existingIds }
                .map { updatedNodesMap[it.id] ?: restoredNodes[it.id] ?: it }
            existingUpdated + nodesToAdd
        }

        // Rebuild indexes from the updated _allNodes to ensure consistency
        val allNodes = _allNodes.value
        val newNodesById = allNodes.associateBy { it.id }
        val newNodesByNumeric = allNodes.associateBy { it.numericId }
        updatemMindmapIndexes(MindmapIndexes(newNodesById, newNodesByNumeric))

        return true
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