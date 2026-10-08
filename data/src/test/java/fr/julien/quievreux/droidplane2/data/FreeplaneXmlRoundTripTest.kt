package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.model.GenericHookElement
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets

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

            val root = nodeManager.rootNode
            root shouldNotBe null
            root!!.hgap shouldBe 25
            root.vgap shouldBe 15
            root.vshift shouldBe 8

            // Verify hooks captured
            root.genericHooks.size shouldBe 3
            root.genericHooks.any { it.name == "accessories/plugins/AutomaticLayout.properties" } shouldBe true
            root.genericHooks.any { it.name == "MapStyle" } shouldBe true
            val scriptHook = root.genericHooks.firstOrNull { it.name == "ScriptHook" }
            scriptHook shouldNotBe null
            scriptHook!!.attributes["SCRIPT_NAME"] shouldBe "auto_calc.groovy"

            // Edit child item via nodeManager.updateNode
            val child = nodeManager.getNodeByID("ID_CHILD")
            child shouldNotBe null
            val updatedChild = child!!.copy(
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

            savedFile shouldNotBe null
            val savedXml = savedFile!!.readText(StandardCharsets.UTF_8)

            // Verify desktop hooks and coordinates on root are retained
            savedXml shouldContain "HGAP=\"25\""
            savedXml shouldContain "VGAP=\"15\""
            savedXml shouldContain "VSHIFT=\"8\""
            savedXml shouldContain "NAME=\"accessories/plugins/AutomaticLayout.properties\""
            savedXml shouldContain "NAME=\"MapStyle\""
            savedXml shouldContain "NAME=\"ScriptHook\""
            savedXml shouldContain "SCRIPT_NAME=\"auto_calc.groovy\""

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
