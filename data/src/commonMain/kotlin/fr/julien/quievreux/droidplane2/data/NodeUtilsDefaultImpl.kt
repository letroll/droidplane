package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.MindMapUri
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.*
import fr.julien.quievreux.droidplane2.data.model.RichContent
import fr.julien.quievreux.droidplane2.data.model.RichContentType
import org.xmlpull.v1.XmlPullParser

class NodeUtilsDefaultImpl : NodeUtils {

    override fun loadRichContent(xpp: XmlPullParser): Result<RichContent> {
        // as we are stream processing the XML, we need to consume the full XML until the
        // richcontent tag is closed (i.e. until we're back at the current parsing depth)
        // eagerly parse until richcontent node is closed
        val startingDepth = xpp.depth
        val richContentType = xpp.getNodeAttribute(TYPE)?.let { RichContentType.fromString(it) } ?: RichContentType.NODE
        val richTextBuilder = StringBuilder()

        var richContentSubParserEventType = xpp.next()

        do {
            // EVENT TYPES as reported by next()

            when (richContentSubParserEventType) {
                XmlPullParser.START_DOCUMENT -> return Result.failure(IllegalStateException("Received START_DOCUMENT but were already within the document"))

                XmlPullParser.END_DOCUMENT -> return Result.failure(IllegalStateException("Received END_DOCUMENT but expected to just parse a sub-document"))

                XmlPullParser.START_TAG -> {
                    val tagName = xpp.name
                    richTextBuilder.append('<').append(tagName)

                    for (i in 0 until xpp.attributeCount) {
                        val attributeName = xpp.getAttributeName(i)
                        val attributeValue = escapeXmlAttribute(xpp.getAttributeValue(i))
                        richTextBuilder.append(' ')
                            .append(attributeName)
                            .append("=\"")
                            .append(attributeValue)
                            .append('"')
                    }

                    richTextBuilder.append('>')
                }

                XmlPullParser.END_TAG -> {
                    val tagName = xpp.name
                    richTextBuilder.append("</").append(tagName).append('>')
                }

                XmlPullParser.TEXT -> {
                    richTextBuilder.append(escapeXmlText(xpp.text))
                }

                else -> return Result.failure(IllegalStateException("Received unexpected event type $richContentSubParserEventType"))

            }

            richContentSubParserEventType = xpp.next()

            // stop parsing once we have come out far enough from the XML to be at the starting depth again
        } while (xpp.depth != startingDepth)
        return Result.success(RichContent(richContentType, richTextBuilder.toString()))
    }

    private fun escapeXmlText(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        val sb = StringBuilder(text.length)
        for (ch in text) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    private fun escapeXmlAttribute(value: String?): String {
        if (value.isNullOrEmpty()) return ""
        val sb = StringBuilder(value.length)
        for (ch in value) {
            when (ch) {
                '&' -> sb.append("&amp;")
                '<' -> sb.append("&lt;")
                '>' -> sb.append("&gt;")
                '"' -> sb.append("&quot;")
                else -> sb.append(ch)
            }
        }
        return sb.toString()
    }

    override fun fillArrowLinks(nodesById: Map<String, Node>?) {
        nodesById?.let {
            for (nodeId in nodesById.keys) {
                nodesById[nodeId]?.let { mindmapNode ->
                    mindmapNode.arrowLinkDestinationIds?.let { nodeIds ->
                        for (linkDestinationId in nodeIds) {
                            val destinationNode = nodesById[linkDestinationId]
                            if (destinationNode != null) {
                                mindmapNode.arrowLinkDestinationNodes.add(destinationNode)
                                destinationNode.arrowLinkIncomingNodes.add(mindmapNode)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    override fun loadAndIndexNodesByIds(root: Node?): MindmapIndexes {
        val nodes = mutableListOf<Node?>()
        root?.let { nodes.add(it) }

        val idAndNode: MutableList<Pair<String, Node>> = mutableListOf()
        val numericIdAndNode: MutableList<Pair<Int, Node>> = mutableListOf()

        while (!nodes.isEmpty()) {
            val node = nodes.removeAt(nodes.size - 1)

            node?.let {
                idAndNode.add(it.id to it)
                numericIdAndNode.add(it.numericId to it)

                for (mindmapNode in it.childNodes) {
                    nodes.add(mindmapNode)
                }
            }
        }

        val newNodesById: MutableMap<String, Node> = HashMap(idAndNode.size)
        val newNodesByNumericId: MutableMap<Int, Node> = HashMap(numericIdAndNode.size)

        for (i in idAndNode) {
            newNodesById[i.first] = i.second
        }
        for (i in numericIdAndNode) {
            newNodesByNumericId[i.first] = i.second
        }

        return MindmapIndexes(newNodesById, newNodesByNumericId)
    }

    override fun parseNodeTag(xpp: XmlPullParser, parentNode: Node?): Result<Node> = try {
        val id = xpp.getNodeAttribute(ID).orEmpty()
        val numericId = try {
            if (id.isNotEmpty()) {
                id.replace("\\D+".toRegex(), "").toIntOrNull()?:-1
            } else {
                -1
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            -1
        }

        val text = xpp.getNodeAttribute(TEXT)

        val creationDate = xpp.getNodeAttribute(CREATED)?.takeIf { it.isNotEmpty() }?.toLong()
        val modificationDate = xpp.getNodeAttribute(MODIFIED)?.takeIf { it.isNotEmpty() }?.toLong()

        val position = xpp.getNodeAttribute(POSITION)?.takeIf { it.isNotEmpty() }
        val color = xpp.getNodeAttribute(COLOR)
        val backgroundColor = xpp.getNodeAttribute(BACKGROUND_COLOR)
        val style = xpp.getNodeAttribute(STYLE)
        val isFolded = xpp.getNodeAttribute(FOLDED)?.equals("true", ignoreCase = true) ?: false
        val hgap = xpp.getNodeAttribute(HGAP)?.toIntOrNull()
        val vgap = xpp.getNodeAttribute(VGAP)?.toIntOrNull()
        val vshift = xpp.getNodeAttribute(VSHIFT)?.toIntOrNull()

        // get link
        val linkAttribute = xpp.getNodeAttribute(LINK)
        val link = if (linkAttribute.isNullOrEmpty()) {
            null
        } else {
            MindMapUri.parse(linkAttribute)
        }

        //TODO look if cloned text is synchonized with original text, if not obtain text from original node
        // and stop calling original in method getNodeText
        // get tree ID (of cloned node)
        val treeIdAttribute = xpp.getNodeAttribute(TREE_ID)

        val newNode = Node(
            parentNode = parentNode,
            id = id,
            numericId = numericId,
            text = text,
            link = link,
            treeIdAttribute = treeIdAttribute,
            creationDate = creationDate,
            modificationDate = modificationDate,
            position = position,
            color = color,
            backgroundColor = backgroundColor,
            style = style,
            isFolded = isFolded,
            hgap = hgap,
            vgap = vgap,
            vshift = vshift,
        )
        Result.success(newNode)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

fun XmlPullParser.getNodeAttribute(attribute: NodeAttribute): String? = this.getAttributeValue(null, attribute.text)