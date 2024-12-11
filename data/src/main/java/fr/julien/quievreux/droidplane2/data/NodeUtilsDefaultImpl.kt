package fr.julien.quievreux.droidplane2.data

import android.net.Uri
import android.util.Pair
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.*
import fr.julien.quievreux.droidplane2.data.model.RichContent
import fr.julien.quievreux.droidplane2.data.model.RichContentType
import org.xmlpull.v1.XmlPullParser
import java.util.Stack

class NodeUtilsDefaultImpl : NodeUtils {

    override fun loadRichContent(xpp: XmlPullParser): Result<RichContent> {
        // as we are stream processing the XML, we need to consume the full XML until the
        // richcontent tag is closed (i.e. until we're back at the current parsing depth)
        // eagerly parse until richcontent node is closed
        val startingDepth = xpp.depth
        val richContentType = xpp.getNodeAttribute(TYPE)?.let { RichContentType.fromString(it) }?:RichContentType.NODE
        var richTextContent = ""

        var richContentSubParserEventType = xpp.next()

        do {
            // EVENT TYPES as reported by next()

            when (richContentSubParserEventType) {
                XmlPullParser.START_DOCUMENT -> return Result.failure(IllegalStateException("Received START_DOCUMENT but were already within the document"))

                XmlPullParser.END_DOCUMENT -> return Result.failure(IllegalStateException("Received END_DOCUMENT but expected to just parse a sub-document"))

                XmlPullParser.START_TAG -> {
                    var tagString = ""

                    val tagName = xpp.name
                    tagString += "<$tagName"

                    var i = 0
                    while (i < xpp.attributeCount) {
                        val attributeName = xpp.getAttributeName(i)
                        val attributeValue = xpp.getAttributeValue(i)

                        val attributeString = " $attributeName=\"$attributeValue\""
                        tagString += attributeString
                        i++
                    }

                    tagString += ">"

                    richTextContent += tagString
                }

                XmlPullParser.END_TAG -> {
                    val tagName = xpp.name
                    val tagString = "</$tagName>"
                    richTextContent += tagString
                }

                XmlPullParser.TEXT -> {
                    val text = xpp.text
                    richTextContent += text
                }

                else -> return Result.failure(IllegalStateException("Received unexpected event type $richContentSubParserEventType"))

            }

            richContentSubParserEventType = xpp.next()

            // stop parsing once we have come out far enough from the XML to be at the starting depth again
        } while (xpp.depth != startingDepth)
        return Result.success(RichContent(richContentType,richTextContent))
    }

    override fun fillArrowLinks(nodesById: Map<String, Node>?) {
        nodesById?.let {
            for (nodeId in nodesById.keys) {
                val mindmapNode = nodesById[nodeId]
                mindmapNode?.arrowLinkDestinationIds?.let {
                    for (linkDestinationId in it) {
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

    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    override fun loadAndIndexNodesByIds(root: Node?): MindmapIndexes {
        // TODO: check if this optimization was necessary - otherwise go back to old implementation

        // TODO: this causes us to load all viewModel nodes, defeating the lazy loading in.Node.getChildNodes

        val stack = Stack<Node?>()
        stack.push(root)

        // try first to just extract all IDs and the respective node, and
        // only insert into the hashmap once we know the size of the hashmap
        val idAndNode: MutableList<Pair<String, Node>> = mutableListOf()
        val numericIdAndNode: MutableList<Pair<Int, Node>> = mutableListOf()

        while (!stack.isEmpty()) {
            val node = stack.pop()

            idAndNode.add(Pair(node?.id, node))
            node?.let {
                numericIdAndNode.add(Pair(node.numericId, node))

                for (mindmapNode in node.childNodes) {
                    stack.push(mindmapNode)
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
                id.replace("\\D+".toRegex(), "").toInt()
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

        // get link
        val linkAttribute = xpp.getNodeAttribute(LINK)
        val link = if (linkAttribute.isNullOrEmpty()) {
            null
        } else {
            Uri.parse(linkAttribute)
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
        )
        Result.success(newNode)
    } catch (exception: Exception) {
        Result.failure(exception)
    }
}

fun XmlPullParser.getNodeAttribute(attribute: NodeAttribute): String? = this.getAttributeValue(null, attribute.text)