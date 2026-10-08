package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.CloudProperties
import fr.julien.quievreux.droidplane2.data.model.EdgeProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttributeEntry
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.InputStream

class NodeManagerUpdateTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "updateNode should update node properties and indexes for child node" {
            val nodeManager = loadedNodeManager()
            val childNode = nodeManager.getNodeByID("ID_1002")
            childNode shouldNotBe null

            val updatedNode = childNode!!.copy(
                text = "Updated Child Title",
                detailsText = "Important child details",
                noteText = "Extended child note",
                attributes = mutableListOf(
                    NodeAttributeEntry("Status", "In Progress"),
                    NodeAttributeEntry("Priority", "High")
                ),
                cloud = CloudProperties(color = "#FFAA00", shape = "ROUND_RECT"),
                edge = EdgeProperties(color = "#0055AA", style = "bezier"),
                color = "#FF0000",
                backgroundColor = "#FFFFCC",
                style = "bubble",
            )

            val success = nodeManager.updateNode(updatedNode)
            success shouldBe true

            val retrieved = nodeManager.getNodeByID("ID_1002")
            retrieved shouldNotBe null
            retrieved!!.text shouldBe "Updated Child Title"
            retrieved.detailsText shouldBe "Important child details"
            retrieved.noteText shouldBe "Extended child note"
            retrieved.attributes.size shouldBe 2
            retrieved.attributes[0].name shouldBe "Status"
            retrieved.attributes[0].value shouldBe "In Progress"
            retrieved.cloud?.color shouldBe "#FFAA00"
            retrieved.edge?.style shouldBe "bezier"
            retrieved.color shouldBe "#FF0000"
            retrieved.backgroundColor shouldBe "#FFFFCC"
            retrieved.style shouldBe "bubble"

            // Verify parent's childNodes collection holds the updated node
            val parent = nodeManager.getNodeByID("ID_1001")
            parent shouldNotBe null
            val childInParent = parent!!.childNodes.firstOrNull { it.id == "ID_1002" }
            childInParent shouldNotBe null
            childInParent!!.text shouldBe "Updated Child Title"
            childInParent.detailsText shouldBe "Important child details"
        }

        "updateNode should update root node when root is modified" {
            val nodeManager = loadedNodeManager()
            val root = nodeManager.rootNode
            root shouldNotBe null

            val updatedRoot = root!!.copy(
                text = "Updated Root",
                detailsText = "Root details",
                noteText = "Root notes",
                cloud = CloudProperties(color = "#E0E0E0", shape = "ARC")
            )

            val success = nodeManager.updateNode(updatedRoot)
            success shouldBe true

            val currentRoot = nodeManager.rootNode
            currentRoot shouldNotBe null
            currentRoot!!.text shouldBe "Updated Root"
            currentRoot.detailsText shouldBe "Root details"
            currentRoot.noteText shouldBe "Root notes"
            currentRoot.cloud?.color shouldBe "#E0E0E0"
        }

        "updateNode should return false for nonexistent node" {
            val nodeManager = loadedNodeManager()
            val fakeNode = Node(
                parentNode = null,
                id = "NONEXISTENT_ID",
                numericId = 999999,
                text = "Fake",
                creationDate = 0L,
                modificationDate = 0L,
            )

            val success = nodeManager.updateNode(fakeNode)
            success shouldBe false
        }
    }

    private suspend fun loadedNodeManager(): NodeManager {
        val nodeManager = initNodeManager()
        val inputStream: InputStream? = java.lang.ClassLoader.getSystemResourceAsStream("test_map.mm")
        inputStream shouldNotBe null
        nodeManager.loadMindMapFromInputStream(
            inputStream = inputStream!!,
            onError = { throw it },
            onParentNodeUpdate = {},
            onLoadFinished = {},
        )
        return nodeManager
    }

    private fun initNodeManager(): NodeManager {
        val logger: Logger = mockk(relaxed = true)
        val nodeUtils: NodeUtils = NodeUtilsDefaultImpl()
        return NodeManager(
            logger = logger,
            nodeUtils = nodeUtils,
            xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger),
            coroutineScope = TestScope(),
        )
    }
}
