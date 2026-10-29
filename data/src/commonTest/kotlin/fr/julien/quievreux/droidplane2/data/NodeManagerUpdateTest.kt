package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.CloudProperties
import fr.julien.quievreux.droidplane2.data.model.EdgeProperties
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.NodeAttributeEntry
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import java.io.ByteArrayInputStream
import java.io.InputStream
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

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

        "Node.equals and hashCode should detect changes in styling, folding, hooks, and parentNode.id" {
            val parentA = Node(
                parentNode = null,
                id = "ID_1",
                numericId = 1,
                text = "Parent A",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val parentB = Node(
                parentNode = null,
                id = "ID_2",
                numericId = 2,
                text = "Parent B",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val baseNode = Node(
                parentNode = parentA,
                id = "ID_10",
                numericId = 10,
                text = "Node",
                creationDate = 0L,
                modificationDate = 0L,
                isFolded = false,
                fontSize = 12,
                fontName = "SansSerif",
                isBold = false,
                isItalic = false,
                latexEquation = null,
                externalObject = null,
            )

            assertEquals(baseNode, baseNode.copy())
            assertEquals(baseNode.hashCode(), baseNode.copy().hashCode())

            assertNotEquals(baseNode, baseNode.copy(isFolded = true))
            assertNotEquals(baseNode, baseNode.copy(fontSize = 18))
            assertNotEquals(baseNode, baseNode.copy(fontName = "Monospaced"))
            assertNotEquals(baseNode, baseNode.copy(isBold = true))
            assertNotEquals(baseNode, baseNode.copy(isItalic = true))
            assertNotEquals(baseNode, baseNode.copy(latexEquation = "E = mc^2"))
            assertNotEquals(
                baseNode,
                baseNode.copy(externalObject = ExternalObjectProperties(uri = "image.png"))
            )
            assertNotEquals(baseNode, baseNode.copy(parentNode = parentB))
        }

        "updateNode on ancestor in 4-level tree should keep parentNode references consistent for all deep descendants" {
            val deepXml = """
                <map version="freeplane 1.9.13">
                <node TEXT="Root" ID="ID_1">
                    <node TEXT="Level 1" ID="ID_2">
                        <node TEXT="Level 2" ID="ID_3">
                            <node TEXT="Level 3" ID="ID_4"/>
                        </node>
                    </node>
                </node>
                </map>
            """.trimIndent()
            val nodeManager = initNodeManager()
            nodeManager.loadMindMapFromInputStream(
                inputStream = ByteArrayInputStream(deepXml.toByteArray(Charsets.UTF_8)),
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )

            val level1 = nodeManager.getNodeByID("ID_2")
            assertNotNull(level1)
            val updatedSuccess = nodeManager.updateNode(level1.copy(text = "Level 1 Edited"))
            assertTrue(updatedSuccess)

            val rootAfter = nodeManager.getNodeByID("ID_1")
            val level1After = nodeManager.getNodeByID("ID_2")
            val level2After = nodeManager.getNodeByID("ID_3")
            val level3After = nodeManager.getNodeByID("ID_4")

            assertNotNull(rootAfter)
            assertNotNull(level1After)
            assertNotNull(level2After)
            assertNotNull(level3After)

            assertEquals("Level 1 Edited", level1After.text)
            assertSame(rootAfter, level1After.parentNode)
            assertSame(level1After, level2After.parentNode)
            assertSame(level2After, level3After.parentNode)
            assertEquals("Level 1 Edited", level3After.parentNode?.parentNode?.text)
        }

        "loadMindMapFromInputStream should populate allNodes in a single batch emission" {
            val deepXml = """
                <map version="freeplane 1.9.13">
                <node TEXT="Root" ID="ID_1">
                    <node TEXT="Child 1" ID="ID_2">
                        <node TEXT="Grandchild 1" ID="ID_3"/>
                    </node>
                    <node TEXT="Child 2" ID="ID_4"/>
                </node>
                </map>
            """.trimIndent()
            val nodeManager = initNodeManager()
            val emissions = mutableListOf<List<Node>>()
            val collectJob = launch(UnconfinedTestDispatcher()) {
                nodeManager.allNodes.toList(emissions)
            }

            nodeManager.loadMindMapFromInputStream(
                inputStream = ByteArrayInputStream(deepXml.toByteArray(Charsets.UTF_8)),
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )
            collectJob.cancel()

            // Initial empty list + 1 batch emission with all 4 nodes (no intermediate 1, 2, 3 node lists)
            val nonEmptyEmissions = emissions.filter { it.isNotEmpty() }
            assertEquals(1, nonEmptyEmissions.size)
            assertEquals(4, nonEmptyEmissions.first().size)
        }
    }

    private suspend fun loadedNodeManager(): NodeManager {
        val nodeManager = initNodeManager()
        val inputStream: InputStream? = javaClass.classLoader?.getResourceAsStream("test_map.mm")
            ?: java.io.File("src/commonTest/resources/test_map.mm").takeIf { it.exists() }?.inputStream()
            ?: java.io.File("data/src/commonTest/resources/test_map.mm").takeIf { it.exists() }?.inputStream()
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
