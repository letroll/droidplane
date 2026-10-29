package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.NodeAttributeEntry
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

class NodeAttributeXmlTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "parse and serialize node key-value attributes" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_1" TEXT="Task Topic" CREATED="1000" MODIFIED="2000">
                        <attribute NAME="Priority" VALUE="High"/>
                        <attribute NAME="Status" VALUE="Done" TYPE="string"/>
                        <attribute NAME="Estimate" VALUE="5" TYPE="number"/>
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
            root!!.attributes.size shouldBe 3
            root.attributes[0].name shouldBe "Priority"
            root.attributes[0].value shouldBe "High"
            root.attributes[1].name shouldBe "Status"
            root.attributes[1].value shouldBe "Done"
            root.attributes[1].type shouldBe "string"
            root.attributes[2].name shouldBe "Estimate"
            root.attributes[2].value shouldBe "5"
            root.attributes[2].type shouldBe "number"

            // Now serialize to temp file and verify XML output
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_attributes_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            savedFile shouldNotBe null
            val savedXml = savedFile!!.readText(StandardCharsets.UTF_8)
            savedXml shouldContain "NAME=\"Priority\""
            savedXml shouldContain "VALUE=\"High\""
            savedXml shouldContain "NAME=\"Status\""
            savedXml shouldContain "VALUE=\"Done\""
            savedXml shouldContain "TYPE=\"string\""

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
