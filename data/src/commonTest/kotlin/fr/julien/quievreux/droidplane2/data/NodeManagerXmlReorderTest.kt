package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import java.io.File

class NodeManagerXmlReorderTest : StringSpec({

    val logger = mockk<Logger>(relaxed = true)
    val nodeUtils = NodeUtilsDefaultImpl()
    val xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger)
    val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    "reordered child nodes persist sequence after XML serialization and deserialization" {
        val manager = NodeManager(logger, nodeUtils, xmlParseUtils, testScope)
        val root = manager.createNewMindmap("Root")

        val idA = manager.addNodeToMindmap("Alpha", root)!!
        val idB = manager.addNodeToMindmap("Beta", root)!!
        val idC = manager.addNodeToMindmap("Gamma", root)!!

        val nodeA = manager.getNodeByNumericId(idA)!!
        val nodeB = manager.getNodeByNumericId(idB)!!

        // Move Alpha after Beta -> [Beta, Alpha, Gamma]
        val success = manager.reorderChildNode(
            parentId = manager.rootNode!!.id,
            draggedChildId = nodeA.id,
            targetNeighborId = nodeB.id,
            insertAfter = true
        )
        success shouldBe true

        val tempFile = File.createTempFile("reorder_test", ".mm")
        try {
            var saveFinished = false
            manager.serializeMindmap(
                filePath = tempFile.parent!!,
                filename = tempFile.name,
                onError = { throw it },
                onSaveFinished = { saveFinished = true }
            )
            saveFinished shouldBe true

            // Reload mindmap in a fresh manager instance
            val reloadManager = NodeManager(logger, nodeUtils, xmlParseUtils, testScope)
            tempFile.inputStream().use { input ->
                reloadManager.loadMindMapFromInputStream(
                    inputStream = input,
                    onError = { throw it },
                    onParentNodeUpdate = {}
                )
            }

            val reloadedRoot = reloadManager.rootNode!!
            val reloadedChildren = reloadedRoot.childNodes.map { it.text }
            reloadedChildren shouldBe listOf("Beta", "Alpha", "Gamma")
        } finally {
            tempFile.delete()
        }
    }
})
