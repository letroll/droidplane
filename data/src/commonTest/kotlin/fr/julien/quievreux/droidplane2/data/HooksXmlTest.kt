package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
import fr.julien.quievreux.droidplane2.data.model.GenericHookElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HooksXmlTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "parse and serialize external object and LaTeX formula hooks" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_1" TEXT="Scientific Topic" CREATED="1000" MODIFIED="2000">
                        <hook NAME="ExternalObject" URI="file:/path/to/diagram.png" SIZE="1.5"/>
                        <hook NAME="plugins/latex/LatexNodeHook.properties" EQUATION="\frac{a}{b} = c"/>
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
            val externalObject = assertNotNull(root.externalObject)
            assertEquals("file:/path/to/diagram.png", externalObject.uri)
            assertEquals(1.5f, externalObject.size)

            assertEquals("\\frac{a}{b} = c", root.latexEquation)

            // Serialize and verify output
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_hooks_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            val verifiedSavedFile = assertNotNull(savedFile)
            val savedXml = verifiedSavedFile.readText(StandardCharsets.UTF_8)
            savedXml shouldContain "<hook NAME=\"ExternalObject\" URI=\"file:/path/to/diagram.png\" SIZE=\"1.5\""
            savedXml shouldContain "<hook NAME=\"plugins/latex/LatexNodeHook.properties\" EQUATION=\"\\frac{a}{b} = c\""

            // Clean up
            savedFile?.delete()
        }

        "generic hooks should not emit duplicate NAME attributes across multiple round-trips or when attributes map contains NAME" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_1" TEXT="Root" CREATED="1000" MODIFIED="2000">
                        <hook NAME="MapStyle" zoom="1.2"/>
                        <hook NAME="AutomaticEdgeColor" COUNTER="4" RULE="ON_BRANCH_CREATION"/>
                    </node>
                </map>
            """.trimIndent()

            val nodeManager = initNodeManager()
            nodeManager.loadMindMapFromInputStream(
                inputStream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)),
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )

            val root = assertNotNull(nodeManager.rootNode)
            assertEquals(2, root.genericHooks.size)
            root.genericHooks.forEach { hook ->
                assertFalse(hook.attributes.containsKey("NAME"))
            }

            // Also add a GenericHookElement constructed with "NAME" inside attributes to verify serializer guard
            root.addGenericHook(
                GenericHookElement(
                    name = "CustomHook",
                    attributes = mapOf("NAME" to "StaleName", "EXTRA" to "val")
                )
            )

            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_generic_hooks_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            val verifiedSavedFile = assertNotNull(savedFile)
            val savedXml = verifiedSavedFile.readText(StandardCharsets.UTF_8)

            val hookTags = Regex("<hook\\b[^>]*>").findAll(savedXml).map { it.value }.toList()
            assertEquals(3, hookTags.size)
            hookTags.forEach { tag ->
                assertEquals(1, Regex("\\bNAME=", RegexOption.IGNORE_CASE).findAll(tag).count())
            }
            savedXml shouldContain "<hook NAME=\"MapStyle\" zoom=\"1.2\""
            savedXml shouldContain "<hook NAME=\"AutomaticEdgeColor\" COUNTER=\"4\" RULE=\"ON_BRANCH_CREATION\""
            savedXml shouldContain "<hook NAME=\"CustomHook\" EXTRA=\"val\""

            savedFile?.delete()
        }

        "MapStyle hook with nested stylenodes does not leak edge, font, cloud, or arrowlink onto parent node and round-trips map version and hook XML" {
            val xml = """
                <map version="freeplane 1.12.15">
                    <node ID="ID_1385587321" CREATED="1361978276662" MODIFIED="1794042503907" TEXT="DroidPlane 2">
                        <icon BUILTIN="mindmap"/>
                        <hook NAME="MapStyle" background="#2e3440ff">
                            <properties show_icon_for_attributes="true" fit_to_viewport="false"/>
                            <map_styles>
                                <stylenode LOCALIZED_TEXT="styles.root_node" STYLE="oval">
                                    <stylenode LOCALIZED_TEXT="styles.predefined" POSITION="bottom_or_right" STYLE="bubble">
                                        <stylenode LOCALIZED_TEXT="default" ID="ID_1499691433" COLOR="#000000" STYLE="fork">
                                            <arrowlink SHAPE="CUBIC_CURVE" COLOR="#000000" DESTINATION="ID_1499691433" STARTARROW="NONE" ENDARROW="DEFAULT"/>
                                        </stylenode>
                                        <stylenode LOCALIZED_TEXT="defaultstyle.floating">
                                            <edge STYLE="hide_edge"/>
                                            <cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT"/>
                                        </stylenode>
                                        <stylenode LOCALIZED_TEXT="styles.subsubtopic">
                                            <font NAME="Liberation Sans" SIZE="10" BOLD="true"/>
                                        </stylenode>
                                    </stylenode>
                                </stylenode>
                            </map_styles>
                        </hook>
                        <node ID="ID_200" TEXT="Child Topic" CREATED="1000" MODIFIED="2000"/>
                    </node>
                </map>
            """.trimIndent()

            val nodeManager = initNodeManager()
            nodeManager.loadMindMapFromInputStream(
                inputStream = ByteArrayInputStream(xml.toByteArray(StandardCharsets.UTF_8)),
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )

            val root = assertNotNull(nodeManager.rootNode)
            assertEquals("DroidPlane 2", root.text)
            assertEquals(listOf("mindmap"), root.iconNames)
            assertNull(root.edge)
            assertNull(root.cloud)
            assertNull(root.fontName)
            assertNull(root.fontSize)
            assertFalse(root.isBold)
            assertTrue(root.connectors.isEmpty())
            assertTrue(root.arrowLinkDestinationIds.isEmpty())

            assertEquals(1, root.genericHooks.size)
            val mapStyleHook = root.genericHooks.first()
            assertEquals("MapStyle", mapStyleHook.name)
            assertEquals("#2e3440ff", mapStyleHook.attributes["background"])
            val hookRawXml = assertNotNull(mapStyleHook.rawXml)
            assertTrue(hookRawXml.contains("<map_styles>"))
            assertTrue(hookRawXml.contains("ID_1499691433"))

            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_mapstyle_isolation_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            val verifiedSavedFile = assertNotNull(savedFile)
            val savedXml = verifiedSavedFile.readText(StandardCharsets.UTF_8)
            assertTrue(savedXml.startsWith("<map version=\"freeplane 1.12.15\">"))
            assertFalse(savedXml.contains("<?xml"))
            assertTrue(savedXml.contains("<map_styles>"))
            assertTrue(savedXml.contains("<stylenode LOCALIZED_TEXT=\"default\" ID=\"ID_1499691433\""))

            // Reload the saved XML and verify root still has no leaked stylenode properties
            val reloadedManager = initNodeManager()
            reloadedManager.loadMindMapFromInputStream(
                inputStream = ByteArrayInputStream(savedXml.toByteArray(StandardCharsets.UTF_8)),
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )
            val reloadedRoot = assertNotNull(reloadedManager.rootNode)
            assertNull(reloadedRoot.edge)
            assertNull(reloadedRoot.cloud)
            assertNull(reloadedRoot.fontName)
            assertFalse(reloadedRoot.isBold)
            assertTrue(reloadedRoot.connectors.isEmpty())

            savedFile?.delete()
        }
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
