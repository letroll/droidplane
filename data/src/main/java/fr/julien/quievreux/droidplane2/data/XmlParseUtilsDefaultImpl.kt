package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.CloudProperties
import fr.julien.quievreux.droidplane2.data.model.ConnectorLink
import fr.julien.quievreux.droidplane2.data.model.EdgeProperties
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import fr.julien.quievreux.droidplane2.data.model.GenericHookElement
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.BOLD
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.BUILTIN
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.DESTINATION
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.ITALIC
import fr.julien.quievreux.droidplane2.data.model.NodeAttributeEntry
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
                    addChildIntoParent(NodeRelation(parentNode, newMindmapNode))
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

    override fun parseRichContent(xpp: XmlPullParser, nodes: MutableList<Node>) {
        if (xpp.isEmptyElementTag) {
            logger.e("Received empty richcontent node - skipping")
        } else {
            nodeUtils.loadRichContent(xpp).onSuccess { richContent ->
                check(nodes.isNotEmpty()) { "Received richtext without a parent node" }

                val parentNode = nodes.last()
                parentNode.addRichContent(
                    richContent.contentType,
                    richContent.content
                )
            }.onFailure {
                logger.e("loadRichContentNodes failed with:$it")
            }
        }
    }

    override fun parseFont(xpp: XmlPullParser, nodes: MutableList<Node>) {
        check(nodes.isNotEmpty()) { "Received font without a parent node" }
        val parentNode = nodes.last()

        val boldAttribute = xpp.getNodeAttribute(BOLD)
        if (boldAttribute != null && boldAttribute == "true") {
            parentNode.isBold = true
        }

        val italicsAttribute = xpp.getNodeAttribute(ITALIC)
        if (italicsAttribute != null && italicsAttribute == "true") {
            parentNode.isItalic = true
        }

        xpp.getAttributeValue(null, NodeAttribute.NAME.text)?.let {
            parentNode.fontName = it
        }
        xpp.getAttributeValue(null, NodeAttribute.SIZE.text)?.toIntOrNull()?.let {
            parentNode.fontSize = it
        }
    }

    override fun parseArrowLink(xpp: XmlPullParser, nodes: MutableList<Node>) {
        check(nodes.isNotEmpty()) { "Received arrowlink without a parent node" }
        val parentNode = nodes.last()

        val destinationId = xpp.getAttributeValue(null, DESTINATION.text) ?: return
        val color = xpp.getAttributeValue(null, NodeAttribute.COLOR.text)
        val startArrow = xpp.getAttributeValue(null, NodeAttribute.STARTARROW.text)
        val endArrow = xpp.getAttributeValue(null, NodeAttribute.ENDARROW.text)
        val startInclination = xpp.getAttributeValue(null, NodeAttribute.STARTINCLINATION.text)
        val endInclination = xpp.getAttributeValue(null, NodeAttribute.ENDINCLINATION.text)
        val sourceLabel = xpp.getAttributeValue(null, NodeAttribute.SOURCE_LABEL.text)
        val middleLabel = xpp.getAttributeValue(null, NodeAttribute.MIDDLE_LABEL.text)
        val targetLabel = xpp.getAttributeValue(null, NodeAttribute.TARGET_LABEL.text)
        val edgeLike = xpp.getAttributeValue(null, NodeAttribute.EDGE_LIKE.text)?.toBoolean() ?: false

        val connector = ConnectorLink(
            destinationId = destinationId,
            color = color,
            startArrow = startArrow,
            endArrow = endArrow,
            startInclination = startInclination,
            endInclination = endInclination,
            sourceLabel = sourceLabel,
            middleLabel = middleLabel,
            targetLabel = targetLabel,
            edgeLike = edgeLike,
        )
        parentNode.addConnector(connector)
    }

    override fun parseIcon(xpp: XmlPullParser, nodes: MutableList<Node>) {
        check(nodes.isNotEmpty()) { "Received icon without a parent node" }

        xpp.getNodeAttribute(BUILTIN)?.let { iconName ->
            val parentNode = nodes.last()
            parentNode.addIconName(iconName)
        }
    }

    override fun parseAttribute(xpp: XmlPullParser, nodes: MutableList<Node>) {
        check(nodes.isNotEmpty()) { "Received attribute without a parent node" }
        val name = xpp.getAttributeValue(null, NodeAttribute.NAME.text) ?: return
        val value = xpp.getAttributeValue(null, NodeAttribute.VALUE.text) ?: ""
        val type = xpp.getAttributeValue(null, NodeAttribute.TYPE.text)
        nodes.last().addAttribute(NodeAttributeEntry(name, value, type))
    }

    override fun parseCloud(xpp: XmlPullParser, nodes: MutableList<Node>) {
        check(nodes.isNotEmpty()) { "Received cloud without a parent node" }
        val color = xpp.getAttributeValue(null, NodeAttribute.COLOR.text)
        val width = xpp.getAttributeValue(null, NodeAttribute.WIDTH.text)?.toIntOrNull()
        val shape = xpp.getAttributeValue(null, NodeAttribute.SHAPE.text)
        nodes.last().cloud = CloudProperties(color, width, shape)
    }

    override fun parseEdge(xpp: XmlPullParser, nodes: MutableList<Node>) {
        check(nodes.isNotEmpty()) { "Received edge without a parent node" }
        val color = xpp.getAttributeValue(null, NodeAttribute.COLOR.text)
        val style = xpp.getAttributeValue(null, NodeAttribute.STYLE.text)
        val width = xpp.getAttributeValue(null, NodeAttribute.WIDTH.text)
        nodes.last().edge = EdgeProperties(color, style, width)
    }

    override fun parseHook(xpp: XmlPullParser, nodes: MutableList<Node>) {
        check(nodes.isNotEmpty()) { "Received hook without a parent node" }
        val parent = nodes.last()
        val name = xpp.getAttributeValue(null, NodeAttribute.NAME.text) ?: ""
        when {
            name == "ExternalObject" -> {
                val uri = xpp.getAttributeValue(null, NodeAttribute.URI.text) ?: ""
                val size = xpp.getAttributeValue(null, NodeAttribute.SIZE.text)?.toFloatOrNull()
                parent.externalObject = ExternalObjectProperties(uri, size)
            }
            name.contains("latex", ignoreCase = true) -> {
                val eq = xpp.getAttributeValue(null, NodeAttribute.EQUATION.text)
                parent.latexEquation = eq
            }
            else -> {
                val attrs = mutableMapOf<String, String>()
                for (i in 0 until xpp.attributeCount) {
                    attrs[xpp.getAttributeName(i)] = xpp.getAttributeValue(i)
                }
                parent.addGenericHook(GenericHookElement(name = name, attributes = attrs))
            }
        }
    }
}
