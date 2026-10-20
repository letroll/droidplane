package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class NodeManagerReorderTest : StringSpec({

    val logger = mockk<Logger>(relaxed = true)
    val nodeUtils = NodeUtilsDefaultImpl()
    val xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger)
    val testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

    fun createTestTree(): NodeManager {
        val manager = NodeManager(logger, nodeUtils, xmlParseUtils, testScope)
        val root = manager.createNewMindmap("Root")
        return manager
    }

    "reordering child node downwards (insertAfter = true) places it after target and keeps subtree" {
        val manager = createTestTree()
        val root = manager.rootNode!!

        val idA = manager.addNodeToMindmap("Node A", root)!!
        val idB = manager.addNodeToMindmap("Node B", root)!!
        val idC = manager.addNodeToMindmap("Node C", root)!!

        val nodeA = manager.getNodeByNumericId(idA)!!
        val subA = manager.addNodeToMindmap("Sub A1", nodeA)!!

        val updatedRoot = manager.rootNode!!
        updatedRoot.childNodes.map { it.text } shouldContainExactly listOf("Node A", "Node B", "Node C")

        val result = manager.reorderChildNode(
            parentId = updatedRoot.id,
            draggedChildId = nodeA.id,
            targetNeighborId = manager.getNodeByNumericId(idB)!!.id,
            insertAfter = true
        )

        result shouldBe true
        val reorderedRoot = manager.rootNode!!
        reorderedRoot.childNodes.map { it.text } shouldContainExactly listOf("Node B", "Node A", "Node C")

        // Check subtree of A is preserved
        val reorderedA = manager.getNodeByNumericId(idA)!!
        reorderedA.childNodes.size shouldBe 1
        reorderedA.childNodes[0].text shouldBe "Sub A1"
    }

    "reordering child node upwards (insertAfter = false) places it before target" {
        val manager = createTestTree()
        val root = manager.rootNode!!

        val idA = manager.addNodeToMindmap("Node A", root)!!
        val idB = manager.addNodeToMindmap("Node B", root)!!
        val idC = manager.addNodeToMindmap("Node C", root)!!

        val nodeC = manager.getNodeByNumericId(idC)!!
        val nodeB = manager.getNodeByNumericId(idB)!!

        val result = manager.reorderChildNode(
            parentId = root.id,
            draggedChildId = nodeC.id,
            targetNeighborId = nodeB.id,
            insertAfter = false
        )

        result shouldBe true
        val reorderedRoot = manager.rootNode!!
        reorderedRoot.childNodes.map { it.text } shouldContainExactly listOf("Node A", "Node C", "Node B")
    }

    "dropping on same relative position is a no-op and returns false" {
        val manager = createTestTree()
        val root = manager.rootNode!!

        val idA = manager.addNodeToMindmap("Node A", root)!!
        val idB = manager.addNodeToMindmap("Node B", root)!!

        val nodeA = manager.getNodeByNumericId(idA)!!
        val nodeB = manager.getNodeByNumericId(idB)!!

        // A is already before B, so inserting before B is a no-op
        val resultSameBefore = manager.reorderChildNode(
            parentId = root.id,
            draggedChildId = nodeA.id,
            targetNeighborId = nodeB.id,
            insertAfter = false
        )
        resultSameBefore shouldBe false

        // B is already after A, so inserting after A is a no-op
        val resultSameAfter = manager.reorderChildNode(
            parentId = root.id,
            draggedChildId = nodeB.id,
            targetNeighborId = nodeA.id,
            insertAfter = true
        )
        resultSameAfter shouldBe false

        // Dropping on itself is a no-op
        val resultSelf = manager.reorderChildNode(
            parentId = root.id,
            draggedChildId = nodeA.id,
            targetNeighborId = nodeA.id,
            insertAfter = true
        )
        resultSelf shouldBe false
    }
})
