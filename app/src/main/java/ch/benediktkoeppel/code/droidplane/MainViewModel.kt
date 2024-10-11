package ch.benediktkoeppel.code.droidplane

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.benediktkoeppel.code.droidplane.controller.NodeChange
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.AddedChild
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
        val loading: Boolean,
        val rootNode: MindmapNode? = null,
        val selectedNode: MindmapNode? = null,
//       val nodes:List<>
    ) {
        internal companion object {
            internal fun defaults() = MainUiState(
                loading = true
            )
        }
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
                                    Log.d(MainApplication.TAG, "Received unknown node " + xpp.getName());
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
            _uiState.update {
                it.copy(
                    rootNode = newMindmapNode,
                    selectedNode = newMindmapNode,
                )
            }
            rootNode = newMindmapNode
            onRootNodeLoaded(rootNode)
        } else {
            parentNode.addChildMindmapNode(newMindmapNode)
            _uiState.update {
                it.copy(
                    rootNode = parentNode
                )
            }
            if (parentNode.hasAddedChildMindmapNodeSubscribers()) {
                onNodeChange(
                    AddedChild(
                        parentNode = parentNode,
                        childNode = newMindmapNode,
                    )
                )
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
}

