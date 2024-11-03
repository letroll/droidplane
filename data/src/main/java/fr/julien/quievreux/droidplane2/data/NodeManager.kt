package fr.julien.quievreux.droidplane2.data

import kotlin.random.Random

class NodeManager {
    private val random = Random(System.currentTimeMillis())

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