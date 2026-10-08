package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.CloudProperties
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

class VisualPropertiesXmlTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "parse and serialize node colors, shape style, font properties, and clouds" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_1" TEXT="Styled Topic" COLOR="#003366" BACKGROUND_COLOR="#fff4cc" STYLE="bubble" FOLDED="true" HGAP="10" VGAP="5" VSHIFT="2">
                        <font NAME="Serif" SIZE="16" BOLD="true" ITALIC="true"/>
                        <cloud COLOR="#ffcc99" SHAPE="ROUND_RECT" WIDTH="2"/>
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
            root!!.color shouldBe "#003366"
            root.backgroundColor shouldBe "#fff4cc"
            root.style shouldBe "bubble"
            root.isFolded shouldBe true
            root.hgap shouldBe 10
            root.vgap shouldBe 5
            root.vshift shouldBe 2

            root.fontName shouldBe "Serif"
            root.fontSize shouldBe 16
            root.isBold shouldBe true
            root.isItalic shouldBe true

            root.cloud shouldNotBe null
            root.cloud!!.color shouldBe "#ffcc99"
            root.cloud!!.shape shouldBe "ROUND_RECT"
            root.cloud!!.width shouldBe 2

            // Serialize and verify XML output
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_visual_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            savedFile shouldNotBe null
            val savedXml = savedFile!!.readText(StandardCharsets.UTF_8)
            savedXml shouldContain "COLOR=\"#003366\""
            savedXml shouldContain "BACKGROUND_COLOR=\"#fff4cc\""
            savedXml shouldContain "STYLE=\"bubble\""
            savedXml shouldContain "FOLDED=\"true\""
            savedXml shouldContain "<cloud COLOR=\"#ffcc99\""
            savedXml shouldContain "SHAPE=\"ROUND_RECT\""
            savedXml shouldContain "<font"
            savedXml shouldContain "NAME=\"Serif\""
            savedXml shouldContain "SIZE=\"16\""

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
