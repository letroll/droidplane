package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.GenericHookElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FreeplaneXmlRoundTripTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "load complex map with desktop hooks, edit a node, and verify complete round-trip preservation" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_ROOT" TEXT="Root Concept" CREATED="1000" MODIFIED="2000" HGAP="25" VGAP="15" VSHIFT="8">
                        <hook NAME="accessories/plugins/AutomaticLayout.properties"/>
                        <hook NAME="MapStyle">
                            <map_styles>
                                <map_style_node name="Standard"/>
                            </map_styles>
                        </hook>
                        <hook NAME="ScriptHook" SCRIPT_NAME="auto_calc.groovy">
                            println("Running desktop script");
                        </hook>
                        <node ID="ID_CHILD" TEXT="Child Item" CREATED="1001" MODIFIED="2001">
                            <attribute NAME="Status" VALUE="Draft"/>
                            <cloud COLOR="#FFEECC" SHAPE="ARC"/>
                        </node>
                    </node>
                </map>
            """.trimIndent()

            val nodeManager = initNodeManager()
            val inputStream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
            nodeManager.loadMindMapFromInputStream(
                inputStream = inputStream,
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )

            val root = assertNotNull(nodeManager.rootNode)
            root shouldNotBe null
            root.hgap shouldBe 25
            root.vgap shouldBe 15
            root.vshift shouldBe 8

            // Verify hooks captured
            root.genericHooks.size shouldBe 3
            root.genericHooks.any { it.name == "accessories/plugins/AutomaticLayout.properties" } shouldBe true
            root.genericHooks.any { it.name == "MapStyle" } shouldBe true
            val scriptHook = assertNotNull(root.genericHooks.firstOrNull { it.name == "ScriptHook" })
            scriptHook shouldNotBe null
            scriptHook.attributes["SCRIPT_NAME"] shouldBe "auto_calc.groovy"
            root.genericHooks.forEach { hook ->
                hook.attributes.containsKey("NAME") shouldBe false
            }

            // Edit child item via nodeManager.updateNode
            val child = assertNotNull(nodeManager.getNodeByID("ID_CHILD"))
            child shouldNotBe null
            val updatedChild = child.copy(
                text = "Edited Child Item",
                noteText = "Added extended notes"
            )
            val updated = nodeManager.updateNode(updatedChild)
            updated shouldBe true

            // Save and verify that desktop hooks on root are preserved
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_roundtrip_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            val nonNullSavedFile = assertNotNull(savedFile)
            nonNullSavedFile shouldNotBe null
            val savedXml = nonNullSavedFile.readText(StandardCharsets.UTF_8)

            // Verify desktop hooks and coordinates on root are retained
            savedXml shouldContain "HGAP=\"25\""
            savedXml shouldContain "VGAP=\"15\""
            savedXml shouldContain "VSHIFT=\"8\""
            savedXml shouldContain "NAME=\"accessories/plugins/AutomaticLayout.properties\""
            savedXml shouldContain "NAME=\"MapStyle\""
            savedXml shouldContain "NAME=\"ScriptHook\""
            savedXml shouldContain "SCRIPT_NAME=\"auto_calc.groovy\""

            // Verify no hook element has a duplicated NAME attribute
            val hookTagRegex = Regex("<hook\\b[^>]*>")
            val hookTags = hookTagRegex.findAll(savedXml).map { it.value }.toList()
            hookTags.size shouldBe 3
            hookTags.forEach { tag ->
                Regex("\\bNAME=").findAll(tag).count() shouldBe 1
            }

            // Verify edited child updates are present
            savedXml shouldContain "Edited Child Item"
            savedXml shouldContain "TYPE=\"NOTE\""
            savedXml shouldContain "Added extended notes"
            savedXml shouldContain "NAME=\"Status\""
            savedXml shouldContain "VALUE=\"Draft\""
            savedXml shouldContain "COLOR=\"#FFEECC\""

            // Clean up
            savedFile?.delete()
        }

        "load FEATURE_TEST_MM_XML marks ID_1478696366 folded, root and leaves unfolded, and preserves FOLDED on XML re-serialization" {
            val nodeManager = loadXmlIntoNodeManager(FEATURE_TEST_MM_XML)

            val root = assertNotNull(nodeManager.rootNode)
            val testNode = assertNotNull(nodeManager.getNodeByID("ID_1478696366"))
            val child1 = assertNotNull(nodeManager.getNodeByID("ID_1128035855"))
            val child2 = assertNotNull(nodeManager.getNodeByID("ID_22398760"))
            val leafSibling = assertNotNull(nodeManager.getNodeByID("ID_1756066841"))

            assertFalse(root.isFolded)
            assertTrue(testNode.isFolded)
            assertFalse(child1.isFolded)
            assertFalse(child2.isFolded)
            assertFalse(leafSibling.isFolded)

            val savedXml = serializeToXml(nodeManager)
            val testNodeTag = assertNotNull(extractNodeStartTag(savedXml, "ID_1478696366"))
            val rootTag = assertNotNull(extractNodeStartTag(savedXml, root.id))
            val child1Tag = assertNotNull(extractNodeStartTag(savedXml, "ID_1128035855"))
            val child2Tag = assertNotNull(extractNodeStartTag(savedXml, "ID_22398760"))

            assertTrue(testNodeTag.contains("FOLDED=\"true\""))
            assertFalse(rootTag.contains("FOLDED="))
            assertFalse(child1Tag.contains("FOLDED="))
            assertFalse(child2Tag.contains("FOLDED="))
            assertEquals(1, Regex("FOLDED=\"true\"").findAll(savedXml).count())
        }

        "setNodeFolded unfolds ID_1478696366 and removes FOLDED attribute on serialization, then refolds and writes FOLDED attribute back" {
            val nodeManager = loadXmlIntoNodeManager(FEATURE_TEST_MM_XML)

            val unfoldedResult = nodeManager.setNodeFolded("ID_1478696366", false)
            assertTrue(unfoldedResult)
            val unfoldedNode = assertNotNull(nodeManager.getNodeByID("ID_1478696366"))
            assertFalse(unfoldedNode.isFolded)

            val unfoldedXml = serializeToXml(nodeManager)
            val unfoldedTestTag = assertNotNull(extractNodeStartTag(unfoldedXml, "ID_1478696366"))
            assertFalse(unfoldedTestTag.contains("FOLDED=\"true\""))
            assertFalse(unfoldedXml.contains("FOLDED=\"true\""))

            val refoldedResult = nodeManager.setNodeFolded("ID_1478696366", true)
            assertTrue(refoldedResult)
            val refoldedNode = assertNotNull(nodeManager.getNodeByID("ID_1478696366"))
            assertTrue(refoldedNode.isFolded)

            val refoldedXml = serializeToXml(nodeManager)
            val refoldedTestTag = assertNotNull(extractNodeStartTag(refoldedXml, "ID_1478696366"))
            assertTrue(refoldedTestTag.contains("FOLDED=\"true\""))
            assertEquals(1, Regex("FOLDED=\"true\"").findAll(refoldedXml).count())
        }

        "setNodeFolded refuses to fold root or leaf nodes, returns false, and XML serialization never emits FOLDED on root or leaves" {
            val nodeManager = loadXmlIntoNodeManager(FEATURE_TEST_MM_XML)
            val root = assertNotNull(nodeManager.rootNode)

            assertFalse(nodeManager.setNodeFolded(root.id, true))
            assertFalse(nodeManager.setNodeFolded("ID_1128035855", true))
            assertFalse(nodeManager.setNodeFolded("ID_22398760", true))
            assertFalse(nodeManager.setNodeFolded("ID_1756066841", true))
            assertFalse(nodeManager.setNodeFolded("ID_1970981533", true))

            assertFalse(assertNotNull(nodeManager.rootNode).isFolded)
            assertFalse(assertNotNull(nodeManager.getNodeByID("ID_1128035855")).isFolded)
            assertFalse(assertNotNull(nodeManager.getNodeByID("ID_22398760")).isFolded)
            assertFalse(assertNotNull(nodeManager.getNodeByID("ID_1756066841")).isFolded)

            // Unfold ID_1478696366 as well to ensure zero FOLDED attributes remain across root and leaf nodes
            assertTrue(nodeManager.setNodeFolded("ID_1478696366", false))

            val savedXml = serializeToXml(nodeManager)
            val rootTag = assertNotNull(extractNodeStartTag(savedXml, root.id))
            val child1Tag = assertNotNull(extractNodeStartTag(savedXml, "ID_1128035855"))
            val child2Tag = assertNotNull(extractNodeStartTag(savedXml, "ID_22398760"))
            val leafSiblingTag = assertNotNull(extractNodeStartTag(savedXml, "ID_1756066841"))

            assertFalse(rootTag.contains("FOLDED=\"true\""))
            assertFalse(child1Tag.contains("FOLDED=\"true\""))
            assertFalse(child2Tag.contains("FOLDED=\"true\""))
            assertFalse(leafSiblingTag.contains("FOLDED=\"true\""))
            assertFalse(savedXml.contains("FOLDED=\"true\""))
        }
    }

    private suspend fun loadXmlIntoNodeManager(xml: String): NodeManager {
        val nodeManager = initNodeManager()
        val inputStream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8))
        nodeManager.loadMindMapFromInputStream(
            inputStream = inputStream,
            onError = { throw it },
            onParentNodeUpdate = {},
            onLoadFinished = {},
        )
        return nodeManager
    }

    private suspend fun serializeToXml(nodeManager: NodeManager): String {
        val tempDir = System.getProperty("java.io.tmpdir")
        val fileName = "test_folded_roundtrip_${System.nanoTime()}.mm"
        var savedFile: File? = null
        nodeManager.serializeMindmap(
            filePath = "$tempDir/",
            filename = fileName,
            onError = { throw it },
            onSaveFinished = { file -> savedFile = file },
        )
        val file = assertNotNull(savedFile)
        return try {
            file.readText(StandardCharsets.UTF_8)
        } finally {
            file.delete()
        }
    }

    private fun extractNodeStartTag(xml: String, nodeId: String): String? =
        Regex("<node\\b[^>]*\\bID=\"$nodeId\"[^>]*>").find(xml)?.value

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

    companion object {
        internal val FEATURE_TEST_MM_XML = """
            <map version="freeplane 1.12.15">
                <bookmarks/>
                <node LOCALIZED_STYLE_REF="AutomaticLayout.level.root" FOLDED="false" ID="ID_1090958577" CREATED="1409300609620" MODIFIED="1792230718501">
                    <richcontent TYPE="NODE">
                        <html><head></head><body><p style="text-align: right">feature rich texte</p></body></html>
                    </richcontent>
                    <hook NAME="accessories/plugins/AutomaticLayout.properties" VALUE="ALL"/>
                    <font BOLD="true"/>
                    <node TEXT="test" FOLDED="true" POSITION="bottom_or_right" ID="ID_1478696366" CREATED="1792230719592" MODIFIED="1792230750542" TEXT_ALIGN="RIGHT">
                        <node TEXT="enfant 1" ID="ID_1128035855" CREATED="1793965383229" MODIFIED="1793965421741"/>
                        <node TEXT="enfant 2" ID="ID_22398760" CREATED="1793965422991" MODIFIED="1793965425479"/>
                    </node>
                    <node TEXT="à partir de quand le texte passe à la ligne suivante ?&#xa;est géré" POSITION="bottom_or_right" ID="ID_1756066841" CREATED="1792230757655" MODIFIED="1792230867476" TEXT_ALIGN="RIGHT"/>
                </node>
            </map>
        """.trimIndent()
    }
}
