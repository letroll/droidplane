package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute
import org.xmlpull.v1.XmlPullParser
import java.util.Stack

class XmlParseUtilsDefaultImpl(
    val nodeUtils: NodeUtils,
    val logger: Logger,
) : XmlParseUtils {

    // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
    // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").

    // if this is an empty tag, we won't need to bother trying to read its content
    // we don't even need to read the <richcontent> node's attributes, as we would
    // only be interested in it's children
    override fun parseRichContent(xpp: XmlPullParser, nodeStack: Stack<Node>) {
        if (xpp.isEmptyElementTag) {
            logger.e( "Received empty richcontent node - skipping")
        } else {
            nodeUtils.loadRichContent(xpp).onSuccess { richContent ->
                // if we have no parent node, something went seriously wrong - we can't have a richcontent that is not part of a viewModel node
                check(!nodeStack.empty()) { "Received richtext without a parent node" }

                val parentNode = nodeStack.peek()
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

    override fun parseFont(xpp: XmlPullParser, nodeStack: Stack<Node>) {
        val boldAttribute = xpp.getAttributeValue(null, NodeAttribute.BOLD.name)

        // if we have no parent node, something went seriously wrong - we can't have a font node that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received richtext without a parent node" }
        val parentNode = nodeStack.peek()

        if (boldAttribute != null && boldAttribute == "true") {
            parentNode.isBold = true
        }

        val italicsAttribute = xpp.getAttributeValue(null, NodeAttribute.ITALIC.name)
        if (italicsAttribute != null && italicsAttribute == "true") {
            parentNode.isItalic = true
        }
    }

    override fun parseArrowLink(xpp: XmlPullParser, nodeStack: Stack<Node>) {
        val destinationId = xpp.getAttributeValue(null, NodeAttribute.DESTINATION.name)

        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received arrowlink without a parent node" }

        val parentNode = nodeStack.peek()
        parentNode.addArrowLinkDestinationId(destinationId)
    }
    override fun parseIcon(xpp: XmlPullParser, nodeStack: Stack<Node>) {
        val iconName = xpp.getAttributeValue(null, NodeAttribute.BUILTIN.name)

        // if we have no parent node, something went seriously wrong - we can't have icons that is not part of a viewModel node
        check(!nodeStack.empty()) { "Received icon without a parent node" }

        val parentNode = nodeStack.peek()
        parentNode.addIconName(iconName)
    }

}