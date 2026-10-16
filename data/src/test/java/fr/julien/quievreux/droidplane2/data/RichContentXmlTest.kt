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

        "preserve literal CDATA sequences and XML special characters in richcontent across round-trip" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_1" TEXT="Code snippet: &lt;![CDATA[raw]]&gt;" CREATED="1000" MODIFIED="2000">
                        <richcontent TYPE="NOTE">
                            <html><head></head><body><p><a href="https://example.com?a=1&amp;b=2" title="quote &quot;here&quot;">a &lt; b &amp; c &gt; d and &lt;![CDATA[test]]&gt;</a></p></body></html>
                        </richcontent>
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
            root.text shouldBe "Code snippet: <![CDATA[raw]]>"
            root.noteText shouldContain "a &lt; b &amp; c &gt; d and &lt;![CDATA[test]]&gt;"
            root.noteText shouldContain "href=\"https://example.com?a=1&amp;b=2\""
            root.noteText shouldContain "title=\"quote &quot;here&quot;\""

            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_cdata_entities_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            savedFile shouldNotBe null
            val reloadedManager = initNodeManager()
            reloadedManager.loadMindMapFromInputStream(
                inputStream = savedFile!!.inputStream(),
                onError = { throw it },
                onParentNodeUpdate = {},
                onLoadFinished = {},
            )

            val reloadedRoot = reloadedManager.rootNode!!
            reloadedRoot.text shouldBe "Code snippet: <![CDATA[raw]]>"
            reloadedRoot.noteText shouldNotBe null
            reloadedRoot.noteText shouldContain "a &lt; b &amp; c &gt; d and &lt;![CDATA[test]]&gt;"
            reloadedRoot.noteText shouldContain "href=\"https://example.com?a=1&amp;b=2\""
            reloadedRoot.noteText shouldContain "title=\"quote &quot;here&quot;\""

            savedFile?.delete()
        }

        "serializeMindmap returns early without calling onSaveFinished when filePath or filename is invalid" {
            val nodeManager = initNodeManager()
            var errorCalled = false
            var saveFinishedCalled = false

            nodeManager.serializeMindmap(
                filePath = "",
                filename = "valid.mm",
                onError = { errorCalled = true },
                onSaveFinished = { saveFinishedCalled = true }
            )
            errorCalled shouldBe true
            saveFinishedCalled shouldBe false

            errorCalled = false
            nodeManager.serializeMindmap(
                filePath = "/tmp/",
                filename = "invalid_name",
                onError = { errorCalled = true },
                onSaveFinished = { saveFinishedCalled = true }
            )
            errorCalled shouldBe true
            saveFinishedCalled shouldBe false
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
