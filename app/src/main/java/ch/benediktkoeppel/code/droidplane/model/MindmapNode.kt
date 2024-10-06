package ch.benediktkoeppel.code.droidplane.model

import android.net.Uri
import android.text.Html
import ch.benediktkoeppel.code.droidplane.MainActivity
import ch.benediktkoeppel.code.droidplane.view.MindmapNodeLayout
import ch.benediktkoeppel.code.droidplane.view.NodeColumn
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * A MindMapNode is a special type of DOM Node. A DOM Node can be converted to a MindMapNode if it has type ELEMENT,
 * and tag "node".
 */
//@Builder
class MindmapNode(
    /**
     * The mindmap, in which this node is
     */
    val mindmap: Mindmap,
    /**
     * The Parent MindmapNode
     */
    @JvmField val parentNode: MindmapNode?,
    /**
     * The ID of the node (ID attribute)
     */
    val id: String,
    /**
     * The numeric representation of this ID
     */
    val numericId: Int,
    /**
     * The Text of the node (TEXT attribute).
     */
    private val text: String?,
    /**
     * If the node has a LINK attribute, it will be stored in Uri link
     */
    @JvmField val link: Uri?,
    /**
     * If the node clones another node, it doesn't have text or richtext, but a TREE_ID
     */
    private val treeIdAttribute: String?,
) {
    /**
     * The Rich Text content of the node (if any)
     */
    val richTextContents: MutableList<String>

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
    val iconNames: MutableList<String>

    /**
     * The XML DOM node from which this MindMapNode is derived
     */
    // TODO: MindmapNode should not need this node
    //private final Node node;
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
    var childMindmapNodes: MutableList<MindmapNode>

    /**
     * List of outgoing arrow links
     */
    val arrowLinkDestinationIds: MutableList<String>

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

    init {
        this.childMindmapNodes = mutableListOf()
        this.richTextContents = mutableListOf()
        iconNames = mutableListOf()
        arrowLinkDestinationIds = mutableListOf()
        //node = null;
    }

    /**
     * Creates a new MindMapNode from Node. The node needs to be of type ELEMENT and have tag "node". Throws a
     * [ClassCastException] if the Node can not be converted to a MindmapNode.
     *
     * @param node
     */
    //    public MindmapNode(Node node, MindmapNode parentNode, Mindmap mindmap) {
    //
    //        this.mindmap = mindmap;
    //
    //        // store the parentNode
    //        this.parentNode = parentNode;
    //
    //        // convert the XML Node to a XML Element
    //        Element tmpElement;
    //        if (isMindmapNode(node)) {
    //            tmpElement = (Element)node;
    //        } else {
    //            throw new ClassCastException("Can not convert Node to MindmapNode");
    //        }
    //
    //        // store the Node
    //        this.node = node;
    //
    //        // extract the ID of the node
    //        id = tmpElement.getAttribute("ID");
    //
    //        try {
    //            numericId = Integer.parseInt(id.replaceAll("\\D+", ""));
    //        } catch (NumberFormatException e) {
    //            numericId = id.hashCode();
    //        }
    //
    //
    //        // extract the string (TEXT attribute) of the nodes
    //        String text = tmpElement.getAttribute("TEXT");
    //
    //        // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
    //        // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").
    //        String richTextContent = null;
    //        // find 'richcontent TYPE="NODE"' subnode, which will contain the rich text content
    //        NodeList richtextNodeList = tmpElement.getChildNodes();
    //        for (int i = 0; i < richtextNodeList.getLength(); i++) {
    //            Node n = richtextNodeList.item(i);
    //            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("richcontent")) {
    //                Element richcontentElement = (Element)n;
    //                String typeAttribute = richcontentElement.getAttribute("TYPE");
    //                if (typeAttribute.equals("NODE") || typeAttribute.equals("NOTE") || typeAttribute.equals("DETAILS")) {
    //
    //                    // extract the whole rich text (XML), to show in a WebView activity
    //                    try {
    //                        Transformer transformer = TransformerFactory.newInstance().newTransformer();
    //                        ByteArrayOutputStream boas = new ByteArrayOutputStream();
    //                        transformer.transform(new DOMSource(richtextNodeList.item(0)), new StreamResult(boas));
    //                        richTextContent = boas.toString();
    //                    } catch (TransformerException e) {
    //                        e.printStackTrace();
    //                    }
    //
    //                    // if the node has no text itself, then convert the rich text content to a text
    //                    if (text == null || text.equals("")) {
    //                        // convert the content (text only) into a string, to show in the normal list view
    //                        text = Html.fromHtml(richcontentElement.getTextContent()).toString();
    //                    }
    //                }
    //            }
    //        }
    //        this.richTextContent = richTextContent;
    //        this.text = text;
    //
    //
    //        // extract styles
    //        NodeList styleNodeList = tmpElement.getChildNodes();
    //        boolean isBold = false;
    //        boolean isItalic = false;
    //        for (int i = 0; i < styleNodeList.getLength(); i++) {
    //            Node n = styleNodeList.item(i);
    //            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("font")) {
    //                Element fontElement = (Element)n;
    //                if (fontElement.hasAttribute("BOLD") && fontElement.getAttribute("BOLD").equals("true")) {
    //                    Log.d(MainApplication.TAG, "Found bold node");
    //                    isBold = true;
    //                }
    //                if (fontElement.hasAttribute("ITALIC") && fontElement.getAttribute("ITALIC").equals("true")) {
    //                    isItalic = true;
    //                }
    //            }
    //        }
    //        this.isBold = isBold;
    //        this.isItalic = isItalic;
    //
    //        // extract icons
    //        iconNames = getIcons();
    //
    //        // find out if it has sub nodes
    //        // TODO: this should just go into a getter
    //        isExpandable = (getNumChildMindmapNodes() > 0);
    //
    //        // extract link
    //        String linkAttribute = tmpElement.getAttribute("LINK");
    //        if (!linkAttribute.equals("")) {
    //            link = Uri.parse(linkAttribute);
    //        } else {
    //            link = null;
    //        }
    //
    //        // get cloned node's info
    //        treeIdAttribute = tmpElement.getAttribute("TREE_ID");
    //
    //        // get arrow link destinations
    //        arrowLinkDestinationIds = new ArrayList<>();
    //        arrowLinkDestinationNodes = new ArrayList<>();
    //        arrowLinkIncomingNodes = new ArrayList<>();
    //        NodeList arrowlinkList = tmpElement.getChildNodes();
    //        for (int i = 0; i< arrowlinkList.getLength(); i++) {
    //            Node n = arrowlinkList.item(i);
    //            if (n.getNodeType() == Node.ELEMENT_NODE && n.getNodeName().equals("arrowlink")) {
    //                Element arrowlinkElement = (Element)n;
    //                String destinationId = arrowlinkElement.getAttribute("DESTINATION");
    //                arrowLinkDestinationIds.add(destinationId);
    //            }
    //        }
    //
    //    }

    // TODO: this should probably live in a view controller, not here
    fun getNodeText(): String? {
        // if this is a cloned node, get the text from the original node

        if (treeIdAttribute != null && treeIdAttribute != "") {
            // TODO this now fails when loading, because the background indexing is not done yet - so we maybe should mark this as "pending", and put it into a queue, to be updated once the linked node is there
            val linkedNode = mindmap.getNodeByID(treeIdAttribute)
            if (linkedNode != null) {
                return linkedNode.getNodeText()
            }
        }

        // if this is a rich text node, get the HTML content instead
        if (this.text == null && richTextContents.isNotEmpty()) {
            val richTextContent = richTextContents[0]
            return Html.fromHtml(richTextContent).toString()
        }

        return text
    }

    val isExpandable: Boolean
        get() = !childMindmapNodes.isEmpty()

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


    val numChildMindmapNodes: Int
        get() = childMindmapNodes.size

    fun subscribe(nodeColumn: NodeColumn) {
        this.subscribedNodeColumn = WeakReference(nodeColumn)
    }

    fun addChildMindmapNode(newMindmapNode: MindmapNode) {
        childMindmapNodes.add(newMindmapNode)
    }

    fun hasAddedChildMindmapNodeSubscribers(): Boolean {
        return this.subscribedNodeColumn != null
    }

    fun notifySubscribersAddedChildMindmapNode(mindmapNode: MindmapNode?) {
        if (this.subscribedNodeColumn != null) {
            subscribedNodeColumn!!.get()!!.notifyNewMindmapNode(mindmapNode)
        }
    }

    fun hasNodeRichContentChangedSubscribers(): Boolean {
        return this.subscribedMainActivity != null
    }

    fun notifySubscribersNodeRichContentChanged() {
        if (this.subscribedMainActivity != null) {
            subscribedMainActivity!!.get()!!.notifyNodeRichContentChanged()
        }
    }

    // TODO: ugly that MainActivity is needed here. Would be better to introduce an listener interface (same for node column above)
    fun subscribeNodeRichContentChanged(mainActivity: MainActivity) {
        this.subscribedMainActivity = WeakReference(mainActivity)
    }

    fun hasNodeStyleChangedSubscribers(): Boolean {
        return this.subscribedNodeLayout != null
    }

    fun subscribeNodeStyleChanged(nodeLayout: MindmapNodeLayout) {
        this.subscribedNodeLayout = WeakReference(nodeLayout)
    }

    fun notifySubscribersNodeStyleChanged() {
        if (this.subscribedNodeLayout != null) {
            subscribedNodeLayout!!.get()!!.notifyNodeStyleChanged()
        }
    }

    fun addIconName(iconName: String) {
        iconNames.add(iconName)
    }

    fun addArrowLinkDestinationId(destinationId: String) {
        arrowLinkDestinationIds.add(destinationId)
    }

    /** Depth-first search in the core text of the nodes in this sub-tree.  */ // TODO: this doesn't work while mindmap is still loading
    fun search(searchString: String): List<MindmapNode> {
        val res = ArrayList<MindmapNode>()
        if (getNodeText()!!.uppercase(Locale.getDefault()).contains(searchString.uppercase(Locale.getDefault()))) { // TODO: npe here when text is null, because text is a rich text
            res.add(this)
        }
        for (child in childMindmapNodes) {
            res.addAll(child.search(searchString))
        }
        return res
    }
}
