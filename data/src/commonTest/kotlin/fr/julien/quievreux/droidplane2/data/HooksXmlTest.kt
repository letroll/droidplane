package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.ExternalObjectProperties
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
