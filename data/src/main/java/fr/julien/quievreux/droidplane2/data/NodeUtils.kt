package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.MindmapNode
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

interface NodeUtils {

    @Throws(IOException::class, XmlPullParserException::class)
    fun loadRichContentNodes(xpp: XmlPullParser): String

    fun fillArrowLinks(nodesById: Map<String, MindmapNode>? )

    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    fun loadAndIndexNodesByIds(root: MindmapNode?): MindmapIndexes

    fun parseNodeTag(xpp: XmlPullParser, parentNode: MindmapNode?): MindmapNode

/*    fun generateNodeID(proposedID: String?): String {
        if (proposedID != null && "" != proposedID && getNodeForID(proposedID) == null) {
            return proposedID
        }
        var returnValue: String
        do {
            val prefix = "ID_"
            *//*
			 * The prefix is to enable the id to be an ID in the sense of
			 * XML/DTD.
			 *//*
            returnValue = prefix + ran.nextInt(UNDEFINED_NODE_ID).toString()
        } while (nodes.containsKey(returnValue))
        return returnValue
    }*/
}