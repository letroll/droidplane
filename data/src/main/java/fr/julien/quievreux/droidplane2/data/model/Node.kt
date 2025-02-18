package fr.julien.quievreux.droidplane2.data.model

import android.net.Uri

/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a MindMapNode if it has type ELEMENT,
 * and tag "node".
 */
//TODO make it stable
data class Node(
    val parentNode: Node?,
    /**
     * The ID of the node (ID attribute)
     */
    val id: String,
    val numericId: Int,

    val text: String?,
    /**
     * If the node has a LINK attribute, it will be stored in Uri link
     */
    val link: Uri? = null,
    /**
     * If the node clones another node, it doesn't have text or richtext, but a TREE_ID
     */
    val treeIdAttribute: String? = null,
    val childNodes: MutableList<Node> = mutableListOf(),
    val richTextContents: MutableList<String> = mutableListOf(),
    var richContentType: RichContentType?=null,
    val iconNames: MutableList<String> = mutableListOf(),
    val creationDate: Long?,
    val modificationDate: Long?,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,
    val position: String? = null,
    // TODO: this has nothing to do with the model
    var isSelected: Boolean = false,
    val arrowLinkDestinationIds: MutableList<String> = mutableListOf(),
    val arrowLinkDestinationNodes: MutableList<Node> = mutableListOf(),
    val arrowLinkIncomingNodes: MutableList<Node> = mutableListOf(),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false

        if (id != other.id) return false
        if (text != other.text) return false
        if (modificationDate != other.modificationDate) return false

        // Compare child nodes by size and modification dates to avoid recursion
        if (childNodes.size != other.childNodes.size) return false
        for (i in childNodes.indices) {
            if (childNodes[i].id != other.childNodes[i].id ||
                childNodes[i].modificationDate != other.childNodes[i].modificationDate) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + text.hashCode()
        return result
    }

    fun isClone() = treeIdAttribute != null && treeIdAttribute != ""

    fun addRichContent(
        richContentType: RichContentType,
        richTextContent: String,
    ) {
        this.richContentType = richContentType
        richTextContents.add(richTextContent)
    }

    val arrowLinks: List<Node>
        get() {
            val combinedArrowLists = mutableListOf<Node>()
            combinedArrowLists.addAll(arrowLinkDestinationNodes)
            combinedArrowLists.addAll(arrowLinkIncomingNodes)
            return combinedArrowLists
        }

    fun addChildMindmapNode(newNode: Node) {
        childNodes.add(newNode)
    }

    fun addIconName(iconName: String) {
        iconNames.add(iconName)
    }

    fun addArrowLinkDestinationId(destinationId: String) {
        arrowLinkDestinationIds.add(destinationId)
    }

    fun deselectAllChildNodes(): Node {
        childNodes.forEach {
            it.isSelected = false
        }
        return this
    }
}

// if the link has a "#ID123", it's an internal link within the document
fun Node.isInternalLink(): Boolean = link?.fragment != null && link.fragment?.startsWith("ID") == true
fun Node.isRoot(): Boolean = parentNode == null

fun Node.shortFamily(): String = "$text = ${childNodes.joinToString(separator = "|"){it.text.orEmpty()}}"
