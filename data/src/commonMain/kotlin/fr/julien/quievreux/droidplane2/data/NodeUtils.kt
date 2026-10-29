package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.RichContent
import org.xmlpull.v1.XmlPullParser

interface NodeUtils {

    fun loadRichContent(xpp: XmlPullParser): Result<RichContent>

    fun fillArrowLinks(nodesById: Map<String, Node>? )

    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    fun loadAndIndexNodesByIds(root: Node?): MindmapIndexes

    fun parseNodeTag(xpp: XmlPullParser, parentNode: Node?): Result<Node>

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