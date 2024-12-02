package fr.julien.quievreux.droidplane2.data.model

import android.net.Uri
import android.text.Html
import fr.julien.quievreux.droidplane2.data.NodeManager

/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a MindMapNode if it has type ELEMENT,
 * and tag "node".
 */
//TODO make it stable
data class MindmapNode(
    val parentNode: MindmapNode?,
    /**
     * The ID of the node (ID attribute)
     */
    val id: String,
    val numericId: Int,

    val text: String?,
    /**
     * If the node has a LINK attribute, it will be stored in Uri link
     */
    val link: Uri?,
    /**
     * If the node clones another node, it doesn't have text or richtext, but a TREE_ID
     */
    private val treeIdAttribute: String?,
    val childMindmapNodes: MutableList<MindmapNode> = mutableListOf(),
    val richTextContents: MutableList<String> = mutableListOf(),
    val iconNames: MutableList<String> = mutableListOf(),
    val creationDate: Long?,
    val modificationDate: Long?,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    // TODO: this has nothing to do with the model
    var isSelected: Boolean = false,
    val arrowLinkDestinationIds: MutableList<String> = mutableListOf(),
    val arrowLinkDestinationNodes: MutableList<MindmapNode> = mutableListOf(),
    val arrowLinkIncomingNodes: MutableList<MindmapNode> = mutableListOf(),
) {

    // TODO: this should probably live in a view controller, not here
    fun getNodeText(nodeManager: NodeManager): String? {
        // if this is a cloned node, get the text from the original node
        if (isClone()) {
            // TODO this now fails when loading, because the background indexing is not done yet - so we maybe should mark this as "pending", and put it into a queue, to be updated once the linked node is there
            val linkedNode = nodeManager.getNodeByID(treeIdAttribute)
            if (linkedNode != null) {
                return linkedNode.getNodeText(nodeManager)
            }
        }

        // if this is a rich text node, get the HTML content instead
        if (this.text == null && richTextContents.isNotEmpty()) {
            val richTextContent = richTextContents.first()
            return Html.fromHtml(richTextContent).toString()
        }

        return text
    }

    private fun isClone() = treeIdAttribute != null && treeIdAttribute != ""

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

    fun addChildMindmapNode(newMindmapNode: MindmapNode) {
        childMindmapNodes.add(newMindmapNode)
    }

    fun addIconName(iconName: String) {
        iconNames.add(iconName)
    }

    fun addArrowLinkDestinationId(destinationId: String) {
        arrowLinkDestinationIds.add(destinationId)
    }

    fun deselectAllChildNodes(): MindmapNode {
        childMindmapNodes.forEach {
            it.isSelected = false
        }
        return this
    }
}

// if the link has a "#ID123", it's an internal link within the document
fun MindmapNode.isInternalLink(): Boolean = link?.fragment != null && link.fragment?.startsWith("ID") == true
fun MindmapNode.isRoot(): Boolean = parentNode == null
