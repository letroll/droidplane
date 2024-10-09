package ch.benediktkoeppel.code.droidplane.helper

import android.net.Uri
import android.util.Pair
import ch.benediktkoeppel.code.droidplane.model.Mindmap
import ch.benediktkoeppel.code.droidplane.model.MindmapIndexes
import ch.benediktkoeppel.code.droidplane.model.MindmapNode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.util.Stack

object NodeUtils {

    @Throws(IOException::class, XmlPullParserException::class)
    fun loadRichContentNodes(xpp: XmlPullParser): String {
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

    fun fillArrowLinks(nodesById: Map<String, MindmapNode>? ) {
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
    fun loadAndIndexNodesByIds(root: MindmapNode?): MindmapIndexes {
        // TODO: check if this optimization was necessary - otherwise go back to old implementation

        // TODO: this causes us to load all mindmap nodes, defeating the lazy loading in ch.benediktkoeppel.code.droidplane.model.MindmapNode.getChildNodes

        val stack = Stack<MindmapNode?>()
        stack.push(root)

        // try first to just extract all IDs and the respective node, and
        // only insert into the hashmap once we know the size of the hashmap
        val idAndNode: MutableList<Pair<String, MindmapNode>> = ArrayList()
        val numericIdAndNode: MutableList<Pair<Int, MindmapNode>> = ArrayList()

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

    fun parseNodeTag(mindmap: Mindmap, xpp: XmlPullParser, parentNode: MindmapNode?): MindmapNode {
        val id = xpp.getAttributeValue(null, "ID")
        val numericId = try {
            id.replace("\\D+".toRegex(), "").toInt()
        } catch (e: NumberFormatException) {
            id.hashCode()
        }

        val text = xpp.getAttributeValue(null, "TEXT")

        // get link
        val linkAttribute = xpp.getAttributeValue(null, "LINK")
        val link = if (linkAttribute != null && linkAttribute != "") {
            Uri.parse(linkAttribute)
        } else {
            null
        }

        // get tree ID (of cloned node)
        val treeIdAttribute = xpp.getAttributeValue(null, "TREE_ID")

        val newMindmapNode = MindmapNode(mindmap, parentNode, id, numericId, text, link, treeIdAttribute)
        return newMindmapNode
    }
}