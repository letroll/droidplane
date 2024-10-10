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
import ch.benediktkoeppel.code.droidplane.controller.OnRootNodeLoadedListener
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
   data class State(val loading:Boolean){
       internal companion object{
           internal fun defaults() = State(
               loading = true
           )
       }
   }
    private val _state: MutableStateFlow<State> = MutableStateFlow(State.defaults())
    val state: StateFlow<State> = _state

    var currentMindMapUri: Uri? = null
    var rootNode: MindmapNode? = null

    /**
     * A map that resolves node IDs to Node objects
     */
    var mindmapIndexes: MindmapIndexes? = null
    var isLoaded: Boolean = false

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    fun getNodeByID(id: String?): MindmapNode? = mindmapIndexes?.nodesByIdIndex?.get(id)

    fun getNodeByNumericID(numericId: Int?): MindmapNode? = mindmapIndexes?.nodesByNumericIndex?.get(numericId)


    fun setMindmapIsLoading(mindmapIsLoading: Boolean) {
        _state.update {
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
        mm: InputStream? = null,
        onRootNodeLoadedListener: OnRootNodeLoadedListener,
        viewModel: MainViewModel,
        onNodeChange:(nodeChange: NodeChange) -> Unit,
        onLoadFinish:()->Unit,
        //TOPO remove lambda and use state
    ) {
        viewModelScope.launch {
           _state.update {
               it.copy(
                   loading = true
               )
           }

            var rootNode: MindmapNode? = null
            val nodeStack = Stack<MindmapNode>()
            var numNodes = 0

            try {
                // set up XML pull parsing
                val factory = XmlPullParserFactory.newInstance()
                factory.isNamespaceAware = true
                val xpp = factory.newPullParser()
                xpp.setInput(mm, "UTF-8")

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

                            val newMindmapNode = NodeUtils.parseNodeTag(viewModel,xpp, parentNode)
                            nodeStack.push(newMindmapNode)
                            numNodes += 1

                            onNodeChange(SubscribeNodeRichContentChanged(newMindmapNode))

                            // if we don't have a parent node, then this is the root node
                            if (parentNode == null) {
                                rootNode = newMindmapNode
                                viewModel.rootNode = rootNode
                                onRootNodeLoadedListener.rootNodeLoaded(viewModel, rootNode)
                            } else {
                                parentNode.addChildMindmapNode(newMindmapNode)
                                if (parentNode.hasAddedChildMindmapNodeSubscribers()) {
                                    onNodeChange(
                                        AddedChild(
                                            parentNode = parentNode,
                                            childNode = newMindmapNode,
                                        )
                                    )
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
                        } else if (xpp.name == "font") {
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
                        } else if (xpp.name == "icon" && xpp.getAttributeValue(null, "BUILTIN") != null) {
                            val iconName = xpp.getAttributeValue(null, "BUILTIN")

                            // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
                            check(!nodeStack.empty()) { "Received icon without a parent node" }

                            val parentNode = nodeStack.peek()
                            parentNode.addIconName(iconName)

                            // let view know that node content has changed
                            if (parentNode.hasNodeStyleChangedSubscribers()) {
                                onNodeChange(NodeStyleChanged(parentNode))
                            }
                        } else if (xpp.name == "arrowlink") {
                            val destinationId = xpp.getAttributeValue(null, "DESTINATION")

                            // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
                            check(!nodeStack.empty()) { "Received arrowlink without a parent node" }

                            val parentNode = nodeStack.peek()
                            parentNode.addArrowLinkDestinationId(destinationId)
                        } else {
                            // Log.d(MainApplication.TAG, "Received unknown node " + xpp.getName());
                        }
                    } else if (eventType == XmlPullParser.END_TAG) {
                        if (xpp.name == "node") {
                            val completedMindmapNode = nodeStack.pop()
                            completedMindmapNode.loaded = true
                        }
                    } else if (eventType == XmlPullParser.TEXT) {
                        // do we have TEXT nodes in the viewModel at all?
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
            val mindmapIndexes = NodeUtils.loadAndIndexNodesByIds(rootNode)
            viewModel.mindmapIndexes = mindmapIndexes

            // Nodes can refer to other nodes with arrowlinks. We want to have the link on both ends of the link, so we can
            // now set the corresponding links
            fillArrowLinks(viewModel.mindmapIndexes?.nodesByIdIndex)

//        val loadDocumentEndTime = System.currentTimeMillis()
            Log.d(MainApplication.TAG, "Document loaded")

            //long numNodes = document.getElementsByTagName("node").getLength();

            // now the full viewModel is loaded
            onLoadFinish()
        }
    }
}

