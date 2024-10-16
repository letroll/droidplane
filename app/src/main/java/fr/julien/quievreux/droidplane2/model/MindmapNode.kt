package fr.julien.quievreux.droidplane2.model

import android.net.Uri
import android.text.Html
import fr.julien.quievreux.droidplane2.MainActivity
import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.view.MindmapNodeLayout
import fr.julien.quievreux.droidplane2.view.NodeColumn
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a MindMapNode if it has type ELEMENT,
 * and tag "node".
 */
//TODO make it stable
data class MindmapNode(
    /**
     * The Parent MindmapNode
     */
    val parentNode: MindmapNode?,
    /**
     * The ID of the node (ID attribute)
     */
    val id: String,
    /**
     * The numeric representation of this ID
     */
    val numericId: Int,

    private val text: String?,
    /**
     * If the node has a LINK attribute, it will be stored in Uri link
     */
    val link: Uri?,
    /**
     * If the node clones another node, it doesn't have text or richtext, but a TREE_ID
     */
    private val treeIdAttribute: String?,
) {
    /**
     * The Rich Text content of the node (if any)
     */
    val richTextContents: MutableList<String> = mutableListOf()

    /**
     * Bold style
     */
    var isBold: Boolean = false

    /**
     * Italic style
     */
    var isItalic: Boolean = false

    /**
     * The names of the icon
     */
    val iconNames: MutableList<String> = mutableListOf()

    /**
     * Returns whether this node is selected
     */
    /**
     * Selects or deselects this node
     *
     * @param selected
     */
    /**
     * Whether the node is selected or not, will be set after it was clicked by the user
     */
    // TODO: this has nothing to do with the model
    var isSelected: Boolean = false

    /**
     * The list of child MindmapNodes. We support lazy loading.
     */
    var childMindmapNodes: MutableList<MindmapNode> = mutableListOf()

    /**
     * List of outgoing arrow links
     */
    val arrowLinkDestinationIds: MutableList<String> = mutableListOf()

    /**
     * List of outgoing arrow MindmapNodes
     */
    val arrowLinkDestinationNodes: MutableList<MindmapNode> = mutableListOf()

    /**
     * List of incoming arrow MindmapNodes
     */
    val arrowLinkIncomingNodes: MutableList<MindmapNode> = mutableListOf()
    private var subscribedNodeColumn: WeakReference<NodeColumn>? = null
    private var subscribedMainActivity: WeakReference<MainActivity>? = null
    private var subscribedNodeLayout: WeakReference<MindmapNodeLayout>? = null
    var loaded = false

    // TODO: this should probably live in a view controller, not here
    fun getNodeText(viewModel: MainViewModel): String? {
        // if this is a cloned node, get the text from the original node

        if (treeIdAttribute != null && treeIdAttribute != "") {
            // TODO this now fails when loading, because the background indexing is not done yet - so we maybe should mark this as "pending", and put it into a queue, to be updated once the linked node is there
            val linkedNode = viewModel.getNodeByID(treeIdAttribute)
            if (linkedNode != null) {
                return linkedNode.getNodeText(viewModel)
            }
        }

        // if this is a rich text node, get the HTML content instead
        if (this.text == null && richTextContents.isNotEmpty()) {
            val richTextContent = richTextContents[0]
            return Html.fromHtml(richTextContent).toString()
        }

        return text
    }

    val isExpandable: Boolean = childMindmapNodes.isNotEmpty()

    fun addRichTextContent(richTextContent: String) {
        richTextContents.add(richTextContent)
    }

    val arrowLinks: List<MindmapNode>
        get() {
            val combinedArrowLists = ArrayList<MindmapNode>()
            combinedArrowLists.addAll(arrowLinkDestinationNodes)
            combinedArrowLists.addAll(arrowLinkIncomingNodes)
            return combinedArrowLists
        }

    fun subscribe(nodeColumn: NodeColumn) {
        this.subscribedNodeColumn = WeakReference(nodeColumn)
    }

    fun addChildMindmapNode(newMindmapNode: MindmapNode) {
        childMindmapNodes.add(newMindmapNode)
    }

    fun hasAddedChildMindmapNodeSubscribers(): Boolean = this.subscribedNodeColumn != null

    fun notifySubscribersAddedChildMindmapNode(mindmapNode: MindmapNode) {
        if (this.subscribedNodeColumn != null) {
            subscribedNodeColumn?.get()?.notifyNewMindmapNode(mindmapNode)
        }
    }

    fun hasNodeRichContentChangedSubscribers(): Boolean = this.subscribedMainActivity != null

    fun notifySubscribersNodeRichContentChanged() {
        if (this.subscribedMainActivity != null) {
            subscribedMainActivity?.get()?.horizontalMindmapView?.setApplicationTitle()
        }
    }

    // TODO: ugly that MainActivity is needed here. Would be better to introduce an listener interface (same for node column above)
    fun subscribeNodeRichContentChanged(mainActivity: MainActivity) {
        this.subscribedMainActivity = WeakReference(mainActivity)
    }

    fun hasNodeStyleChangedSubscribers(): Boolean = this.subscribedNodeLayout != null

    fun subscribeNodeStyleChanged(nodeLayout: MindmapNodeLayout) {
        this.subscribedNodeLayout = WeakReference(nodeLayout)
    }

    fun notifySubscribersNodeStyleChanged() {
        if (this.subscribedNodeLayout != null) {
            subscribedNodeLayout?.get()?.refreshView()
        }
    }

    fun addIconName(iconName: String) {
        iconNames.add(iconName)
    }

    fun addArrowLinkDestinationId(destinationId: String) {
        arrowLinkDestinationIds.add(destinationId)
    }

    /** Depth-first search in the core text of the nodes in this sub-tree.  */ // TODO: this doesn't work while viewModel is still loading
    fun search(
        searchString: String,
        viewModel: MainViewModel,
    ): List<MindmapNode> {
        val res = ArrayList<MindmapNode>()
        if (getNodeText(viewModel)?.uppercase(Locale.getDefault())?.contains(searchString.uppercase(Locale.getDefault())) == true) { // TODO: npe here when text is null, because text is a rich text
            res.add(this)
        }
        for (child in childMindmapNodes) {
            res.addAll(
                child.search(
                    searchString,
                    viewModel
                )
            )
        }
        return res
    }

    fun deselectAllChildNodes(): MindmapNode {
        childMindmapNodes.forEach {
            it.isSelected = false
        }
        return this
    }
}
