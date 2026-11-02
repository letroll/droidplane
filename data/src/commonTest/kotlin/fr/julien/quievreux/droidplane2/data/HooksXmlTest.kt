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

            val root = nodeManager.rootNode
            root shouldNotBe null
            root!!.externalObject shouldNotBe null
            root.externalObject!!.uri shouldBe "file:/path/to/diagram.png"
            root.externalObject!!.size shouldBe 1.5f

            root.latexEquation shouldBe "\\frac{a}{b} = c"

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

            savedFile shouldNotBe null
            val savedXml = savedFile!!.readText(StandardCharsets.UTF_8)
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

            val root = nodeManager.rootNode!!
            root.genericHooks.size shouldBe 2
            root.genericHooks.forEach { hook ->
                hook.attributes.containsKey("NAME") shouldBe false
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

            savedFile shouldNotBe null
            val savedXml = savedFile!!.readText(StandardCharsets.UTF_8)

            val hookTags = Regex("<hook\\b[^>]*>").findAll(savedXml).map { it.value }.toList()
            hookTags.size shouldBe 3
            hookTags.forEach { tag ->
                Regex("\\bNAME=", RegexOption.IGNORE_CASE).findAll(tag).count() shouldBe 1
            }
            savedXml shouldContain "<hook NAME=\"MapStyle\" zoom=\"1.2\""
            savedXml shouldContain "<hook NAME=\"AutomaticEdgeColor\" COUNTER=\"4\" RULE=\"ON_BRANCH_CREATION\""
            savedXml shouldContain "<hook NAME=\"CustomHook\" EXTRA=\"val\""

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
