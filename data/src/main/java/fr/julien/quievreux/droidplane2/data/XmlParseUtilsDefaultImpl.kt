package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.BOLD
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.BUILTIN
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.DESTINATION
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.ITALIC
import fr.julien.quievreux.droidplane2.data.model.NodeRelation
import org.xmlpull.v1.XmlPullParser

class XmlParseUtilsDefaultImpl(
    val nodeUtils: NodeUtils,
    val logger: Logger,
) : XmlParseUtils {

    override fun parseNodeText(
        nodes: MutableList<Node>,
        xpp: XmlPullParser,
        addChildIntoParent: (NodeRelation) -> Unit,
        onParentNodeUpdate: (Node) -> Unit,
    ) {
        val parentNode: Node? = getParentFromStack(nodes)

        nodeUtils.parseNodeTag(xpp, parentNode)
            .onSuccess { newMindmapNode ->
                nodes.add(newMindmapNode)

                // if we don't have a parent node, then this is the root node
                if (parentNode == null) {
                    onParentNodeUpdate(newMindmapNode)
                } else {
                    addChildIntoParent(NodeRelation(parentNode,newMindmapNode))
                }
            }.onFailure {
                logger.e("Failed to parse node:$it")
            }
    }

    private fun getParentFromStack(nodes: MutableList<Node>): Node? {
        var parentNode: Node? = null
        if (nodes.isNotEmpty()) {
            parentNode = nodes.last()
        }
        return parentNode
    }


    // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
    // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").

    // if this is an empty tag, we won't need to bother trying to read its content
    // we don't even need to read the <richcontent> node's attributes, as we would
    // only be interested in it's children
    override fun parseRichContent(xpp: XmlPullParser, nodes: MutableList<Node>) {
        if (xpp.isEmptyElementTag) {
            logger.e("Received empty richcontent node - skipping")
        } else {
            nodeUtils.loadRichContent(xpp).onSuccess { richContent ->
                // if we have no parent node, something went seriously wrong - we can't have a richcontent that is not part node
                check(nodes.isNotEmpty()) { "Received richtext without a parent node" }

                val parentNode = nodes.last()
                parentNode.addRichContent(
                    richContent.contentType,
                    richContent.content
                )
            }
                .onFailure {
                    logger.e("loadRichContentNodes failed with:$it")
                }
        }
    }

    override fun parseFont(xpp: XmlPullParser, nodes: MutableList<Node>) {

        // if we have no parent node, something went seriously wrong - we can't have a font node that is not part node
        check(nodes.isNotEmpty()) { "Received richtext without a parent node" }
        val parentNode = nodes.last()

        val boldAttribute = xpp.getNodeAttribute(BOLD)
        if (boldAttribute != null && boldAttribute == "true") {
            parentNode.isBold = true
        }

        val italicsAttribute = xpp.getNodeAttribute(ITALIC)
        if (italicsAttribute != null && italicsAttribute == "true") {
            parentNode.isItalic = true
        }
    }

    override fun parseArrowLink(xpp: XmlPullParser, nodes: MutableList<Node>) {
        // if we have no parent node, something went seriously wrong - we can't have icons that is not part node
        check(nodes.isNotEmpty()) { "Received arrowlink without a parent node" }

        xpp.getNodeAttribute(DESTINATION)?.let { destinationId ->
            val parentNode = nodes.last()
            parentNode.addArrowLinkDestinationId(destinationId)
        }
    }

    override fun parseIcon(xpp: XmlPullParser, nodes: MutableList<Node>) {
        // if we have no parent node, something went seriously wrong - we can't have icons that is not part node
        check(nodes.isNotEmpty()) { "Received icon without a parent node" }

        xpp.getNodeAttribute(BUILTIN)?.let { iconName ->
            val parentNode = nodes.last()
            parentNode.addIconName(iconName)
        }
    }

}