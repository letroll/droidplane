package fr.julien.quievreux.droidplane2.model

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FreeplaneIconXmlTest {

    private fun initNodeManager(): NodeManager {
        val nodeUtils = NodeUtilsDefaultImpl()
        val logger: Logger = mockk(relaxed = true)
        return NodeManager(
            logger = logger,
            nodeUtils = nodeUtils,
            xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger),
            coroutineScope = TestScope()
        )
    }

    @Test
    fun `node icons round trip through XML serialization and parsing with 100 percent fidelity`() = runTest {
        val nodeManager = initNodeManager()
        val root = nodeManager.createNewMindmap("Icon Root")
        val childId = nodeManager.addNodeToMindmap("Node with multiple icons", root)
        assertNotNull(childId)

        val childNode = nodeManager.getNodeByNumericId(childId)
        assertNotNull(childNode)
        childNode.iconNames.addAll(listOf("button_ok", "flag-red", "full-1", "custom_unmapped_icon_123"))

        val outDir = createTempDirectory(prefix = "droidplane-icons")
        var savedFile: File? = null

        nodeManager.serializeMindmap(
            filePath = outDir.toAbsolutePath().toString(),
            filename = "icons_round_trip.mm",
            onError = { throw it },
            onSaveFinished = { savedFile = it }
        )
        assertNotNull(savedFile)

        val xmlContent = savedFile!!.readText()
        assertTrue(xmlContent.contains("""<icon BUILTIN="button_ok""""))
        assertTrue(xmlContent.contains("""<icon BUILTIN="flag-red""""))
        assertTrue(xmlContent.contains("""<icon BUILTIN="full-1""""))
        assertTrue(xmlContent.contains("""<icon BUILTIN="custom_unmapped_icon_123""""))

        // Reload into a fresh NodeManager
        val reloadedManager = initNodeManager()
        reloadedManager.loadMindMapFromInputStream(
            inputStream = savedFile!!.inputStream(),
            onError = { throw it },
            onParentNodeUpdate = {},
            onLoadFinished = {}
        )

        val reloadedNodes = reloadedManager.allNodes.first()
        val reloadedChild = reloadedNodes.firstOrNull { it.text == "Node with multiple icons" }
        assertNotNull(reloadedChild)
        assertEquals(
            listOf("button_ok", "flag-red", "full-1", "custom_unmapped_icon_123"),
            reloadedChild.iconNames
        )
    }
}
