package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.data.model.RichContentType
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

class RichContentXmlTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "parse and serialize node with richText, details, and note" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_1" TEXT="Plain Text Fallback" CREATED="1000" MODIFIED="2000">
                        <richcontent TYPE="NODE">
                            <html><head></head><body><p>Formatted <b>Node</b></p></body></html>
                        </richcontent>
                        <richcontent TYPE="DETAILS">
                            <html><head></head><body><p>Sub-item details</p></body></html>
                        </richcontent>
                        <richcontent TYPE="NOTE">
                            <html><head></head><body><p>Extended note body</p></body></html>
                        </richcontent>
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
            root!!.id shouldBe "ID_1"
            root.richText shouldNotBe null
            root.richText shouldContain "Formatted <b>Node</b>"
            root.detailsText shouldNotBe null
            root.detailsText shouldContain "Sub-item details"
            root.noteText shouldNotBe null
            root.noteText shouldContain "Extended note body"

            // Now serialize to temp file and verify XML output
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_rich_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            savedFile shouldNotBe null
            val savedXml = savedFile!!.readText(StandardCharsets.UTF_8)
            savedXml shouldContain "TYPE=\"NODE\""
            savedXml shouldContain "Formatted <b>Node</b>"
            savedXml shouldContain "TYPE=\"DETAILS\""
            savedXml shouldContain "Sub-item details"
            savedXml shouldContain "TYPE=\"NOTE\""
            savedXml shouldContain "Extended note body"

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
