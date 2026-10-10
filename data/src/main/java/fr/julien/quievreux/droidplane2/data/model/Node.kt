package fr.julien.quievreux.droidplane2.data.model

import android.net.Uri

/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a MindMapNode if it has type ELEMENT,
 * and tag "node".
 */
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

    // Freeplane Rich Content Elements
    var richText: String? = null,
    var detailsText: String? = null,
    var noteText: String? = null,
    val richTextContents: MutableList<String> = mutableListOf(),
    var richContentType: RichContentType? = null,

    // Freeplane Attributes & Icons
    val attributes: MutableList<NodeAttributeEntry> = mutableListOf(),
    val iconNames: MutableList<String> = mutableListOf(),

    // Dates & State
    val creationDate: Long?,
    val modificationDate: Long?,
    var isFolded: Boolean = false,

    // Visual Presentation
    var color: String? = null,
    var backgroundColor: String? = null,
    var style: String? = null,
    var fontName: String? = null,
    var fontSize: Int? = null,
    var isBold: Boolean = false,
    var isItalic: Boolean = false,

    // Grouping & Connections
    var cloud: CloudProperties? = null,
    var edge: EdgeProperties? = null,
    val connectors: MutableList<ConnectorLink> = mutableListOf(),

    // Layout
    val position: String? = null,
    var hgap: Int? = null,
    var vgap: Int? = null,
    var vshift: Int? = null,

    // Extensions & Hooks
    var externalObject: ExternalObjectProperties? = null,
    var latexEquation: String? = null,
    val genericHooks: MutableList<GenericHookElement> = mutableListOf(),

    // Legacy arrow link destination compatibility
    val arrowLinkDestinationIds: MutableList<String> = mutableListOf(),
    val arrowLinkDestinationNodes: MutableList<Node> = mutableListOf(),
    val arrowLinkIncomingNodes: MutableList<Node> = mutableListOf(),
) {

    override fun toString(): String {
        return "Node(id=$id, numericId=$numericId, text=$text, parentNodeId=${parentNode?.id}, childCount=${childNodes.size})"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Node) return false

        if (id != other.id) return false
        if (text != other.text) return false
        if (detailsText != other.detailsText) return false
        if (noteText != other.noteText) return false
        if (color != other.color) return false
        if (backgroundColor != other.backgroundColor) return false
        if (style != other.style) return false
        if (cloud != other.cloud) return false
        if (edge != other.edge) return false
        if (attributes != other.attributes) return false
        if (iconNames != other.iconNames) return false
        if (modificationDate != other.modificationDate) return false

        if (link != other.link) return false
        if (connectors != other.connectors) return false
        if (arrowLinkDestinationIds != other.arrowLinkDestinationIds) return false

        // Compare child nodes by size and modification dates to avoid recursion
        if (childNodes.size != other.childNodes.size) return false
        for (i in childNodes.indices) {
            if (childNodes[i].id != other.childNodes[i].id ||
                childNodes[i].childNodes.size != other.childNodes[i].childNodes.size ||
                childNodes[i].modificationDate != other.childNodes[i].modificationDate) {
                return false
            }
        }

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (text?.hashCode() ?: 0)
        result = 31 * result + (detailsText?.hashCode() ?: 0)
        result = 31 * result + (noteText?.hashCode() ?: 0)
        result = 31 * result + attributes.hashCode()
        result = 31 * result + connectors.hashCode()
        result = 31 * result + arrowLinkDestinationIds.hashCode()
        return result
    }

    fun isClone() = treeIdAttribute != null && treeIdAttribute != ""

    fun addRichContent(
        richContentType: RichContentType,
        richTextContent: String,
    ) {
        this.richContentType = richContentType
        richTextContents.add(richTextContent)
        when (richContentType) {
            RichContentType.NODE -> richText = richTextContent
            RichContentType.DETAILS -> detailsText = richTextContent
            RichContentType.NOTE -> noteText = richTextContent
        }
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

    fun addAttribute(entry: NodeAttributeEntry) {
        attributes.add(entry)
    }

    fun addConnector(connector: ConnectorLink) {
        connectors.add(connector)
        addArrowLinkDestinationId(connector.destinationId)
    }

    fun addGenericHook(hook: GenericHookElement) {
        genericHooks.add(hook)
    }
}

// if the link has a "#ID123", it's an internal link within the document
fun Node.isInternalLink(): Boolean = link?.fragment != null && link.fragment?.startsWith("ID") == true
fun Node.isRoot(): Boolean = parentNode == null

fun Node.shortFamily(): String = "$text = ${childNodes.joinToString(separator = "|") { it.text.orEmpty() }}"
