package fr.julien.quievreux.droidplane2.data

import android.net.Uri
import android.util.Pair
import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.MindmapNode
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Stack

class NodeUtilsDefaultImpl : NodeUtils {

    @Throws(IOException::class, XmlPullParserException::class)
    override fun loadRichContentNodes(xpp: XmlPullParser): String {
        // as we are stream processing the XML, we need to consume the full XML until the
        // richcontent tag is closed (i.e. until we're back at the current parsing depth)
        // eagerly parse until richcontent node is closed
        val startingDepth = xpp.depth
        var richTextContent = ""

        var richContentSubParserEventType = xpp.next()

        do {
            // EVENT TYPES as reported by next()

            when (richContentSubParserEventType) {
                XmlPullParser.START_DOCUMENT -> throw IllegalStateException("Received START_DOCUMENT but were already within the document")

                XmlPullParser.END_DOCUMENT -> throw IllegalStateException("Received END_DOCUMENT but expected to just parse a sub-document")

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

                else -> throw IllegalStateException("Received unexpected event type $richContentSubParserEventType")

            }

            richContentSubParserEventType = xpp.next()

            // stop parsing once we have come out far enough from the XML to be at the starting depth again
        } while (xpp.depth != startingDepth)
        return richTextContent
    }

    override fun fillArrowLinks(nodesById: Map<String, MindmapNode>?) {
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
    override fun loadAndIndexNodesByIds(root: MindmapNode?): MindmapIndexes {
        // TODO: check if this optimization was necessary - otherwise go back to old implementation

        // TODO: this causes us to load all viewModel nodes, defeating the lazy loading in.MindmapNode.getChildNodes

        val stack = Stack<MindmapNode?>()
        stack.push(root)

        // try first to just extract all IDs and the respective node, and
        // only insert into the hashmap once we know the size of the hashmap
        val idAndNode: MutableList<Pair<String, MindmapNode>> = mutableListOf()
        val numericIdAndNode: MutableList<Pair<Int, MindmapNode>> = mutableListOf()

        while (!stack.isEmpty()) {
            val node = stack.pop()

            idAndNode.add(Pair(node?.id, node))
            node?.let {
                numericIdAndNode.add(Pair(node.numericId, node))

                for (mindmapNode in node.childMindmapNodes) {
                    stack.push(mindmapNode)
                }
            }

        }

        val newNodesById: MutableMap<String, MindmapNode> = HashMap(idAndNode.size)
        val newNodesByNumericId: MutableMap<Int, MindmapNode> = HashMap(numericIdAndNode.size)

        for (i in idAndNode) {
            newNodesById[i.first] = i.second
        }
        for (i in numericIdAndNode) {
            newNodesByNumericId[i.first] = i.second
        }

        return MindmapIndexes(newNodesById, newNodesByNumericId)
    }

    override fun parseNodeTag(xpp: XmlPullParser, parentNode: MindmapNode?): MindmapNode {
        val id = xpp.getNodeAttribute(NodeAttribute.ID).orEmpty()
        val numericId = try {
            if (id.isNotEmpty()) {
                id.replace("\\D+".toRegex(), "").toInt()
            } else {
                -1
            }
        } catch (e: NumberFormatException) {
            id.hashCode()
        }

        val text = xpp.getNodeAttribute(NodeAttribute.TEXT)

        val creationDate = xpp.getNodeAttribute(NodeAttribute.CREATED)?.toLong()
        val modificationDate = xpp.getNodeAttribute(NodeAttribute.MODIFIED)?.toLong()

        // get link
        val linkAttribute = xpp.getNodeAttribute(NodeAttribute.LINK)
        val link = if (linkAttribute != null && linkAttribute != "") {
            Uri.parse(linkAttribute)
        } else {
            null
        }

        //TODO look if cloned text is synchonized with original text, if not obtain text from original node
        // and stop calling original in method getNodeText
        // get tree ID (of cloned node)
        val treeIdAttribute = xpp.getNodeAttribute(NodeAttribute.TREE_ID)

        val newMindmapNode = MindmapNode(
            parentNode = parentNode,
            id = id,
            numericId = numericId,
            text = text,
            link = link,
            treeIdAttribute = treeIdAttribute,
            creationDate = creationDate,
            modificationDate = modificationDate,
        )
        return newMindmapNode
    }
}

fun XmlPullParser.getNodeAttribute(attribute: NodeAttribute): String? = this.getAttributeValue(null, attribute.text)