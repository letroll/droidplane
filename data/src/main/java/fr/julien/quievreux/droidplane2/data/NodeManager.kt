package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.data.model.MindmapNode
import kotlin.random.Random

class NodeManager {
    private val random = Random(System.currentTimeMillis())

    /**
     * A map that resolves node IDs to Node objects
     */
    private var mindmapIndexes: MindmapIndexes? = null

    /**
     * Returns the node for a given Node ID
     *
     * @param id
     * @return
     */
    fun getNodeByID(id: String?): MindmapNode? = mindmapIndexes?.nodesByIdIndex?.get(id)

    fun getNodeByNumericID(numericId: Int?): MindmapNode? = mindmapIndexes?.nodesByNumericIndex?.get(numericId)

    fun getNodeByNumericIndex(): Map<Int, MindmapNode>? {
        return mindmapIndexes?.nodesByNumericIndex
    }

    fun getNodeByIdIndex(): Map<String, MindmapNode>? {
        return mindmapIndexes?.nodesByIdIndex
    }

    fun updatemMindmapIndexes(mindmapIndexes: MindmapIndexes) {
        this.mindmapIndexes = mindmapIndexes
    }

    companion object{
        const val UNDEFINED_NODE_ID: Int = 2000000000
    }

//    fun generateNodeID(): String {
//        var returnValue: String
//        do {
//            val prefix = "ID_"
//            /*
//			 * The prefix is to enable the id to be an ID in the sense of
//			 * XML/DTD.
//			 */
//            returnValue = prefix + random.nextInt(UNDEFINED_NODE_ID).toString()
//        } while (nodes.containsKey(returnValue))
//        return returnValue
//    }

}