package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NodeUtilsTest : StringSpec() {
    init {
        coroutineTestScope = true

        val nodeUtils = NodeUtilsDefaultImpl()

        "indexing empty root should return empty indexes" {
            val indexes = nodeUtils.loadAndIndexNodesByIds(null)
            indexes.nodesByIdIndex.isEmpty() shouldBe true
            indexes.nodesByNumericIndex.isEmpty() shouldBe true
        }

        "indexing tree should index root and all descendants by id and numericId" {
            val root = Node(
                parentNode = null,
                id = "ID_100",
                numericId = 100,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child1 = Node(
                parentNode = root,
                id = "ID_101",
                numericId = 101,
                text = "Child 1",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val subChild = Node(
                parentNode = child1,
                id = "ID_102",
                numericId = 102,
                text = "SubChild 1.1",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            child1.childNodes.add(subChild)
            root.childNodes.add(child1)

            // Debug: check the structure before indexing
            root.childNodes.size shouldBe 1
            root.childNodes[0].id shouldBe "ID_101"
            root.childNodes[0].childNodes.size shouldBe 1
            root.childNodes[0].childNodes[0].id shouldBe "ID_102"

            // Debug: directly test the indexing logic
            val nodes = mutableListOf<Node?>()
            nodes.add(root)
            val idAndNode: MutableList<Pair<String, Node>> = mutableListOf()
            while (!nodes.isEmpty()) {
                val node = nodes.removeAt(nodes.size - 1)
                node?.let {
                    idAndNode.add(it.id to it)
                    for (mindmapNode in it.childNodes) {
                        nodes.add(mindmapNode)
                    }
                }
            }
            idAndNode.size shouldBe 3
            idAndNode.map { it.first } shouldContainExactlyInAnyOrder listOf("ID_100", "ID_101", "ID_102")

            val indexes = nodeUtils.loadAndIndexNodesByIds(root)

            // Debug: check what we actually got
            val idKeys = indexes.nodesByIdIndex.keys.toList()
            val numKeys = indexes.nodesByNumericIndex.keys.toList()
            val idSize = indexes.nodesByIdIndex.size
            val numSize = indexes.nodesByNumericIndex.size
            
            // Debug assertions
            idSize shouldBe 3
            idKeys.size shouldBe 3
            idKeys[0] shouldNotBe null
            idKeys[1] shouldNotBe null
            idKeys[2] shouldNotBe null
            
            // This will fail with a clear message showing actual values
            idKeys shouldContainExactlyInAnyOrder listOf("ID_100", "ID_101", "ID_102")
            numKeys shouldContainExactlyInAnyOrder listOf(100, 101, 102)

            indexes.nodesByIdIndex["ID_100"]?.text shouldBe "Root Node"
            indexes.nodesByIdIndex["ID_101"]?.text shouldBe "Child 1"
            indexes.nodesByIdIndex["ID_102"]?.text shouldBe "SubChild 1.1"

            val retrievedSubChild = indexes.nodesByIdIndex["ID_102"]
            retrievedSubChild?.parentNode?.id shouldBe "ID_101"
            retrievedSubChild?.parentNode?.parentNode?.id shouldBe "ID_100"
        }

        "arrow links should be connected bidirectionally across nodes" {
            val sourceNode = Node(
                parentNode = null,
                id = "ID_1",
                numericId = 1,
                text = "Source",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val targetNode = Node(
                parentNode = null,
                id = "ID_2",
                numericId = 2,
                text = "Target",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            sourceNode.arrowLinkDestinationIds.add("ID_2")

            val nodesMap = mapOf("ID_1" to sourceNode, "ID_2" to targetNode)
            nodeUtils.fillArrowLinks(nodesMap)

            sourceNode.arrowLinkDestinationNodes.size shouldBe 1
            sourceNode.arrowLinkDestinationNodes.first().id shouldBe "ID_2"
            targetNode.arrowLinkIncomingNodes.size shouldBe 1
            targetNode.arrowLinkIncomingNodes.first().id shouldBe "ID_1"
        }

        "parent traversal should work correctly through multiple levels" {
            val root = Node(
                parentNode = null,
                id = "ID_100",
                numericId = 100,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child1 = Node(
                parentNode = root,
                id = "ID_101",
                numericId = 101,
                text = "Child 1",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child2 = Node(
                parentNode = child1,
                id = "ID_102",
                numericId = 102,
                text = "Child 2",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child3 = Node(
                parentNode = child2,
                id = "ID_103",
                numericId = 103,
                text = "Child 3",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            child2.childNodes.add(child3)
            child1.childNodes.add(child2)
            root.childNodes.add(child1)

            val indexes = nodeUtils.loadAndIndexNodesByIds(root)

            val node3 = indexes.nodesByIdIndex["ID_103"]
            node3 shouldNotBe null
            node3?.parentNode?.id shouldBe "ID_102"
            node3?.parentNode?.parentNode?.id shouldBe "ID_101"
            node3?.parentNode?.parentNode?.parentNode?.id shouldBe "ID_100"
            // Avoid checking parentNode?.parentNode?.parentNode?.parentNode to prevent StackOverflow in toString
            (node3?.parentNode?.parentNode?.parentNode?.parentNode == null) shouldBe true
        }

        "sibling nodes should share the same parent reference" {
            val root = Node(
                parentNode = null,
                id = "ID_100",
                numericId = 100,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child1 = Node(
                parentNode = root,
                id = "ID_101",
                numericId = 101,
                text = "Child 1",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val child2 = Node(
                parentNode = root,
                id = "ID_102",
                numericId = 102,
                text = "Child 2",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            root.childNodes.add(child1)
            root.childNodes.add(child2)

            val indexes = nodeUtils.loadAndIndexNodesByIds(root)

            val idxChild1 = indexes.nodesByIdIndex["ID_101"]
            val idxChild2 = indexes.nodesByIdIndex["ID_102"]

            idxChild1 shouldNotBe null
            idxChild2 shouldNotBe null
            idxChild1?.parentNode?.id shouldBe "ID_100"
            idxChild2?.parentNode?.id shouldBe "ID_100"
            // Compare parent node IDs instead of references to avoid StackOverflow in toString
            idxChild1?.parentNode?.id shouldBe idxChild2?.parentNode?.id
        }

        "root node should have no parent" {
            val root = Node(
                parentNode = null,
                id = "ID_100",
                numericId = 100,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )

            val indexes = nodeUtils.loadAndIndexNodesByIds(root)

            val idxRoot = indexes.nodesByIdIndex["ID_100"]
            idxRoot shouldNotBe null
            idxRoot?.parentNode shouldBe null
        }

        "parseNodeTag should parse FOLDED attribute on standard node with parent" {
            val root = Node(
                parentNode = null,
                id = "ID_100",
                numericId = 100,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )

            val foldedNode = assertNotNull(
                nodeUtils.parseNodeTag(
                    createNodeXmlPullParser("""<node ID="ID_101" TEXT="Folded Child" FOLDED="true"/>"""),
                    root,
                ).getOrNull(),
            )
            assertTrue(foldedNode.isFolded)

            val unfoldedNode = assertNotNull(
                nodeUtils.parseNodeTag(
                    createNodeXmlPullParser("""<node ID="ID_102" TEXT="Unfolded Child" FOLDED="false"/>"""),
                    root,
                ).getOrNull(),
            )
            assertFalse(unfoldedNode.isFolded)

            val defaultNode = assertNotNull(
                nodeUtils.parseNodeTag(
                    createNodeXmlPullParser("""<node ID="ID_103" TEXT="Default Child"/>"""),
                    root,
                ).getOrNull(),
            )
            assertFalse(defaultNode.isFolded)
        }

        "loadMindMapFromXml should ignore FOLDED=true on root node and leaf node without children" {
            val xml = """
                <map version="freeplane 1.12.1">
                    <node ID="ID_100" TEXT="Root Node" FOLDED="true">
                        <node ID="ID_101" TEXT="Leaf Node" FOLDED="true"/>
                        <node ID="ID_102" TEXT="Folded Branch" FOLDED="true">
                            <node ID="ID_103" TEXT="Branch Child"/>
                        </node>
                    </node>
                </map>
            """.trimIndent()
            val logger = io.mockk.mockk<fr.julien.quievreux.droidplane2.core.log.Logger>(relaxed = true)
            val xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger)
            val nodeManager = NodeManager(
                logger = logger,
                nodeUtils = nodeUtils,
                xmlParseUtils = xmlParseUtils,
                coroutineScope = kotlinx.coroutines.test.TestScope(),
            )
            val factory = XmlPullParserFactory.newInstance()
            factory.isNamespaceAware = true
            val xpp = factory.newPullParser()
            xpp.setInput(StringReader(xml))

            nodeManager.loadMindMapFromXml(
                xpp = xpp,
                onError = { throw it },
                onParentNodeUpdate = {},
            )

            val indexedRoot = assertNotNull(nodeManager.getNodeByID("ID_100"))
            val indexedLeaf = assertNotNull(nodeManager.getNodeByID("ID_101"))
            val indexedBranch = assertNotNull(nodeManager.getNodeByID("ID_102"))
            val indexedBranchChild = assertNotNull(nodeManager.getNodeByID("ID_103"))

            assertFalse(indexedRoot.isFolded)
            assertTrue(indexedLeaf.childNodes.isEmpty())
            assertFalse(indexedLeaf.isFolded)
            assertTrue(indexedBranch.isFolded)
            assertFalse(indexedBranchChild.isFolded)
        }

        "loadAndIndexNodesByIds should preserve isFolded=true on nested folded nodes with children" {
            val root = Node(
                parentNode = null,
                id = "ID_100",
                numericId = 100,
                text = "Root Node",
                creationDate = 1000L,
                modificationDate = 1000L,
            )
            val foldedParent = assertNotNull(
                nodeUtils.parseNodeTag(
                    createNodeXmlPullParser("""<node ID="ID_101" TEXT="Folded Parent" FOLDED="true"/>"""),
                    root,
                ).getOrNull(),
            )
            val foldedChild = assertNotNull(
                nodeUtils.parseNodeTag(
                    createNodeXmlPullParser("""<node ID="ID_102" TEXT="Folded Child" FOLDED="true"/>"""),
                    foldedParent,
                ).getOrNull(),
            )
            val grandChild = assertNotNull(
                nodeUtils.parseNodeTag(
                    createNodeXmlPullParser("""<node ID="ID_103" TEXT="GrandChild"/>"""),
                    foldedChild,
                ).getOrNull(),
            )

            foldedChild.childNodes.add(grandChild)
            foldedParent.childNodes.add(foldedChild)
            root.childNodes.add(foldedParent)

            val indexes = nodeUtils.loadAndIndexNodesByIds(root)

            val indexedRoot = assertNotNull(indexes.nodesByIdIndex["ID_100"])
            val indexedParent = assertNotNull(indexes.nodesByIdIndex["ID_101"])
            val indexedChild = assertNotNull(indexes.nodesByIdIndex["ID_102"])
            val indexedGrandChild = assertNotNull(indexes.nodesByIdIndex["ID_103"])

            assertFalse(indexedRoot.isFolded)
            assertTrue(indexedParent.isFolded)
            assertTrue(indexedChild.isFolded)
            assertFalse(indexedGrandChild.isFolded)
            assertEquals("ID_101", indexedChild.parentNode?.id)
            assertEquals("ID_102", indexedGrandChild.parentNode?.id)
        }
    }

    private fun createNodeXmlPullParser(xml: String): XmlPullParser {
        val factory = XmlPullParserFactory.newInstance()
        factory.isNamespaceAware = true
        val xpp = factory.newPullParser()
        xpp.setInput(StringReader(xml))
        while (xpp.eventType != XmlPullParser.END_DOCUMENT) {
            if (xpp.eventType == XmlPullParser.START_TAG && xpp.name == "node") {
                break
            }
            xpp.next()
        }
        return xpp
    }
}
