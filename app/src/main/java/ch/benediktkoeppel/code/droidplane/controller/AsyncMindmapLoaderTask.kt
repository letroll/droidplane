package ch.benediktkoeppel.code.droidplane.controller

import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.util.Log
import android.util.Pair
import ch.benediktkoeppel.code.droidplane.MainActivity
import ch.benediktkoeppel.code.droidplane.MainApplication
import ch.benediktkoeppel.code.droidplane.R
import ch.benediktkoeppel.code.droidplane.model.Mindmap
import ch.benediktkoeppel.code.droidplane.model.MindmapIndexes
import ch.benediktkoeppel.code.droidplane.model.MindmapNode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import org.xmlpull.v1.XmlPullParserFactory
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.util.Stack

class AsyncMindmapLoaderTask(
// TODO: why is MainActivty needed here?
    private val mainActivity: MainActivity,
    private val onRootNodeLoadedListener: OnRootNodeLoadedListener,
    private val mindmap: Mindmap,
    private val intent: Intent
) : AsyncTask<String?, Void?, Any?>() {
    private val action = intent.action

    override fun doInBackground(vararg p0: String?): Any? {
        // prepare loading of the Mindmap file

        var mm: InputStream? = null

        // determine whether we are started from the EDIT or VIEW intent, or whether we are started from the
        // launcher started from ACTION_EDIT/VIEW intent
        if ((Intent.ACTION_EDIT == action || Intent.ACTION_VIEW == action) ||
            Intent.ACTION_OPEN_DOCUMENT == action
        ) {
            Log.d(MainApplication.TAG, "started from ACTION_EDIT/VIEW intent")

            // get the URI to the target document (the Mindmap we are opening) and open the InputStream
            val uri = intent.data
            if (uri != null) {
                val cr = mainActivity.contentResolver
                try {
                    mm = cr.openInputStream(uri)
                } catch (e: FileNotFoundException) {
                    mainActivity.abortWithPopup(R.string.filenotfound)
                    e.printStackTrace()
                }
            } else {
                mainActivity.abortWithPopup(R.string.novalidfile)
            }

            // store the Uri. Next time the MainActivity is started, we'll
            // check whether the Uri has changed (-> load new document) or
            // remained the same (-> reuse previous document)
            mindmap.uri = uri
        } else {
            Log.d(MainApplication.TAG, "started from app launcher intent")

            // display the default Mindmap "example.mm", from the resources
            mm = mainActivity.applicationContext.resources.openRawResource(R.raw.example)
        }

        // load the mindmap
        Log.d(MainApplication.TAG, "InputStream fetched, now starting to load document")

        loadDocument(mm)

        return null
    }

    /**
     * Loads a mind map (*.mm) XML document into its internal DOM tree
     *
     * @param inputStream the inputStream to load
     */
    fun loadDocument(inputStream: InputStream?) {
        // show loading indicator

        mainActivity.setMindmapIsLoading(true)

        // start measuring the document load time
        val loadDocumentStartTime = System.currentTimeMillis()

        var rootNode: MindmapNode? = null
        val nodeStack = Stack<MindmapNode>()
        var numNodes = 0

        try {
            // set up XML pull parsing
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(inputStream, "UTF-8")

            // stream parse the XML
            var eventType = xpp.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_DOCUMENT) {
                    Log.d(MainApplication.TAG, "Received XML Start Document")
                } else if (eventType == XmlPullParser.START_TAG) {
                    if (xpp.name == "node") {
                        var parentNode: MindmapNode? = null
                        if (!nodeStack.empty()) {
                            parentNode = nodeStack.peek()
                        }

                        val newMindmapNode = parseNodeTag(xpp, parentNode)
                        nodeStack.push(newMindmapNode)
                        numNodes += 1

                        newMindmapNode.subscribeNodeRichContentChanged(mainActivity)

                        // if we don't have a parent node, then this is the root node
                        if (parentNode == null) {
                            rootNode = newMindmapNode
                            mindmap.rootNode = rootNode
                            onRootNodeLoadedListener.rootNodeLoaded(mindmap, rootNode)
                        } else {
                            parentNode.addChildMindmapNode(newMindmapNode)
                            if (parentNode.hasAddedChildMindmapNodeSubscribers()) {
                                val finalParentNode: MindmapNode = parentNode
                                mainActivity.runOnUiThread {
                                    finalParentNode.notifySubscribersAddedChildMindmapNode(newMindmapNode)
                                }
                            }
                        }
                    } else if (xpp.name == "richcontent"
                        && (xpp.getAttributeValue(null, "TYPE") == "NODE"
                            || xpp.getAttributeValue(null, "TYPE") == "NOTE"
                            || xpp.getAttributeValue(null, "TYPE") == "DETAILS"
                            )
                    ) {
                        // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
                        // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").

                        // if this is an empty tag, we won't need to bother trying to read its content
                        // we don't even need to read the <richcontent> node's attributes, as we would
                        // only be interested in it's children

                        if (xpp.isEmptyElementTag) {
                            Log.d(MainApplication.TAG, "Received empty richcontent node - skipping")
                        } else {
                            val richTextContent = loadRichContentNodes(xpp)

                            // if we have no parent node, something went seriously wrong - we can't have a richcontent that is not part of a mindmap node
                            check(!nodeStack.empty()) { "Received richtext without a parent node" }

                            val parentNode = nodeStack.peek()
                            parentNode.addRichTextContent(richTextContent)

                            // let view know that node content has changed
                            if (parentNode.hasNodeRichContentChangedSubscribers()) {
                                val finalParentNode = parentNode
                                mainActivity.runOnUiThread {
                                    finalParentNode.notifySubscribersNodeRichContentChanged()
                                }
                            }
                        }
                    } else if (xpp.name == "font") {
                        val boldAttribute = xpp.getAttributeValue(null, "BOLD")

                        // if we have no parent node, something went seriously wrong - we can't have a font node that is not part of a mindmap node
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
                            val finalParentNode = parentNode
                            mainActivity.runOnUiThread {
                                finalParentNode.notifySubscribersNodeStyleChanged()
                            }
                        }
                    } else if (xpp.name == "icon" && xpp.getAttributeValue(null, "BUILTIN") != null) {
                        val iconName = xpp.getAttributeValue(null, "BUILTIN")

                        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a mindmap node
                        check(!nodeStack.empty()) { "Received icon without a parent node" }

                        val parentNode = nodeStack.peek()
                        parentNode.addIconName(iconName)

                        // let view know that node content has changed
                        if (parentNode.hasNodeStyleChangedSubscribers()) {
                            val finalParentNode = parentNode
                            mainActivity.runOnUiThread {
                                finalParentNode.notifySubscribersNodeStyleChanged()
                            }
                        }
                    } else if (xpp.name == "arrowlink") {
                        val destinationId = xpp.getAttributeValue(null, "DESTINATION")

                        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a mindmap node
                        check(!nodeStack.empty()) { "Received arrowlink without a parent node" }

                        val parentNode = nodeStack.peek()
                        parentNode.addArrowLinkDestinationId(destinationId)
                    } else {
                        // Log.d(MainApplication.TAG, "Received unknown node " + xpp.getName());
                    }
                } else if (eventType == XmlPullParser.END_TAG) {
                    if (xpp.name == "node") {
                        val completedMindmapNode = nodeStack.pop()
                        completedMindmapNode.setLoaded(true)
                    }
                } else if (eventType == XmlPullParser.TEXT) {
                    // TODO: do we have TEXT nodes in the mindmap at all?
                } else {
                    throw IllegalStateException("Received unknown event $eventType")
                }
                eventType = xpp.next()
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

        // stack should now be empty
        if (!nodeStack.empty()) {
            throw RuntimeException("Stack should be empty")
            // TODO: we could try to be lenient here to allow opening partial documents (which sometimes happens when dropbox doesn't fully sync). Probably doesn't work anyways, as we already throw a runtime exception above if we receive garbage
        }

        // TODO: can we do this as we stream through the XML above?

        // load all nodes of root node into simplified MindmapNode, and index them by ID for faster lookup
        val mindmapIndexes = loadAndIndexNodesByIds(rootNode)
        mindmap.mindmapIndexes = mindmapIndexes

        // Nodes can refer to other nodes with arrowlinks. We want to have the link on both ends of the link, so we can
        // now set the corresponding links
        fillArrowLinks()

        val loadDocumentEndTime = System.currentTimeMillis()
        Log.d(MainApplication.TAG, "Document loaded")

        //long numNodes = document.getElementsByTagName("node").getLength();

        // now the full mindmap is loaded
        mindmap.isLoaded = true
        mainActivity.setMindmapIsLoading(false)
    }

    @Throws(IOException::class, XmlPullParserException::class) private fun loadRichContentNodes(xpp: XmlPullParser): String {
        // as we are stream processing the XML, we need to consume the full XML until the
        // richcontent tag is closed (i.e. until we're back at the current parsing depth)
        // eagerly parse until richcontent node is closed
        val startingDepth = xpp.depth
        var richTextContent = ""

        var richContentSubParserEventType = xpp.next()

        do {
            // EVENT TYPES as reported by next()

            when (richContentSubParserEventType) {
                XmlPullParser.START_DOCUMENT -> throw IllegalStateException("Received START_DOCUMENT but were already within the document")

                XmlPullParser.END_DOCUMENT -> throw IllegalStateException("Received END_DOCUMENT but expected to just parse a sub-document")

                XmlPullParser.START_TAG -> {
                    var tagString = ""

                    val tagName = xpp.name
                    tagString += "<$tagName"

                    var i = 0
                    while (i < xpp.attributeCount) {
                        val attributeName = xpp.getAttributeName(i)
                        val attributeValue = xpp.getAttributeValue(i)

                        val attributeString = " $attributeName=\"$attributeValue\""
                        tagString += attributeString
                        i++
                    }

                    tagString += ">"

                    richTextContent += tagString
                }

                XmlPullParser.END_TAG -> {
                    val tagName = xpp.name
                    val tagString = "</$tagName>"
                    richTextContent += tagString
                }

                XmlPullParser.TEXT -> {
                    val text = xpp.text
                    richTextContent += text
                }

                else -> throw IllegalStateException("Received unexpected event type $richContentSubParserEventType")

            }

            richContentSubParserEventType = xpp.next()

            // stop parsing once we have come out far enough from the XML to be at the starting depth again
        } while (xpp.depth != startingDepth)
        return richTextContent
    }

    private fun parseNodeTag(xpp: XmlPullParser, parentNode: MindmapNode?): MindmapNode {
        val id = xpp.getAttributeValue(null, "ID")
        var numericId = try {
            id.replace("\\D+".toRegex(), "").toInt()
        } catch (e: NumberFormatException) {
            id.hashCode()
        }

        val text = xpp.getAttributeValue(null, "TEXT")

        // get link
        val linkAttribute = xpp.getAttributeValue(null, "LINK")
        val link = if (linkAttribute != null && linkAttribute != "") {
            Uri.parse(linkAttribute)
        } else {
            null
        }

        // get tree ID (of cloned node)
        val treeIdAttribute = xpp.getAttributeValue(null, "TREE_ID")

        val newMindmapNode = MindmapNode(mindmap, parentNode, id, numericId, text, link, treeIdAttribute)
        return newMindmapNode
    }

    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    private fun loadAndIndexNodesByIds(root: MindmapNode?): MindmapIndexes {
        // TODO: check if this optimization was necessary - otherwise go back to old implementation

        // TODO: this causes us to load all mindmap nodes, defeating the lazy loading in ch.benediktkoeppel.code.droidplane.model.MindmapNode.getChildNodes

        val stack = Stack<MindmapNode?>()
        stack.push(root)

        // try first to just extract all IDs and the respective node, and
        // only insert into the hashmap once we know the size of the hashmap
        val idAndNode: MutableList<Pair<String, MindmapNode>> = ArrayList()
        val numericIdAndNode: MutableList<Pair<Int, MindmapNode>> = ArrayList()

        while (!stack.isEmpty()) {
            val node = stack.pop()

            idAndNode.add(Pair(node!!.id, node))
            numericIdAndNode.add(Pair(node.numericId, node))

            for (mindmapNode in node.childMindmapNodes) {
                stack.push(mindmapNode)
            }
        }

        val newNodesById: MutableMap<String, MindmapNode> = HashMap(idAndNode.size)
        val newNodesByNumericId: MutableMap<Int, MindmapNode> = HashMap(numericIdAndNode.size)

        for (i in idAndNode) {
            newNodesById[i.first] = i.second
        }
        for (i in numericIdAndNode) {
            newNodesByNumericId[i.first] = i.second
        }

        return MindmapIndexes(newNodesById, newNodesByNumericId)
    }

    private fun fillArrowLinks() {
        val nodesById = mindmap.mindmapIndexes!!.nodesByIdIndex

        for (nodeId in nodesById.keys) {
            val mindmapNode = nodesById[nodeId]
            for (linkDestinationId in mindmapNode!!.arrowLinkDestinationIds) {
                val destinationNode = nodesById[linkDestinationId]
                if (destinationNode != null) {
                    mindmapNode.arrowLinkDestinationNodes.add(destinationNode)
                    destinationNode.arrowLinkIncomingNodes.add(mindmapNode)
                }
            }
        }
    }

}
