package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.ConnectorLink
import fr.julien.quievreux.droidplane2.data.model.EdgeProperties
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

class ConnectorsXmlTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "parse and serialize node edge styles and arrowlink connectors" {
            val xml = """
                <map version="freeplane 1.2.0">
                    <node ID="ID_1" TEXT="Source Topic" CREATED="1000" MODIFIED="2000">
                        <edge COLOR="#0033aa" STYLE="linear" WIDTH="3"/>
                        <arrowlink DESTINATION="ID_2" COLOR="#ff0000" STARTARROW="None" ENDARROW="Default" MIDDLE_LABEL="relates to" EDGE_LIKE="true"/>
                        <node ID="ID_2" TEXT="Target Topic" CREATED="1001" MODIFIED="2001"/>
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
            root!!.edge shouldNotBe null
            root.edge!!.color shouldBe "#0033aa"
            root.edge!!.style shouldBe "linear"
            root.edge!!.width shouldBe "3"

            root.connectors.size shouldBe 1
            val conn = root.connectors[0]
            conn.destinationId shouldBe "ID_2"
            conn.color shouldBe "#ff0000"
            conn.startArrow shouldBe "None"
            conn.endArrow shouldBe "Default"
            conn.middleLabel shouldBe "relates to"
            conn.edgeLike shouldBe true

            // Serialize and verify output
            val tempDir = System.getProperty("java.io.tmpdir")
            val fileName = "test_connectors_${System.currentTimeMillis()}.mm"
            var savedFile: File? = null

            nodeManager.serializeMindmap(
                filePath = "$tempDir/",
                filename = fileName,
                onError = { throw it },
                onSaveFinished = { file -> savedFile = file }
            )

            savedFile shouldNotBe null
            val savedXml = savedFile!!.readText(StandardCharsets.UTF_8)
            savedXml shouldContain "<edge COLOR=\"#0033aa\" STYLE=\"linear\" WIDTH=\"3\""
            savedXml shouldContain "<arrowlink DESTINATION=\"ID_2\""
            savedXml shouldContain "COLOR=\"#ff0000\""
            savedXml shouldContain "MIDDLE_LABEL=\"relates to\""
            savedXml shouldContain "EDGE_LIKE=\"true\""

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
