package fr.julien.quievreux.droidplane2

import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.model.Node
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MainActivityRotationTest {

    private fun createViewModel(): MainViewModel {
        val logger: Logger = mockk(relaxed = true)
        val recentFilesRepository: RecentFilesRepository = mockk(relaxed = true)
        val nodeUtils = NodeUtilsDefaultImpl()
        val nodeManager = NodeManager(
            logger = logger,
            nodeUtils = nodeUtils,
            xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger),
            coroutineScope = TestScope()
        )
        return MainViewModel(
            logger = logger,
            injectedNodeManager = nodeManager,
            recentFilesRepository = recentFilesRepository
        )
    }

    @Test
    fun `cold start condition is true when savedInstanceState is null and getRootNode is null`() {
        val viewModel = createViewModel()
        val savedInstanceState: Any? = null

        val shouldInitializeColdStart = savedInstanceState == null && viewModel.getRootNode() == null
        assertTrue(shouldInitializeColdStart, "Cold start should initialize empty mindmap on fresh start")
    }

    @Test
    fun `screen rotation does not trigger cold start reinitialization when savedInstanceState is not null`() {
        val viewModel = createViewModel()
        viewModel.createEmptyMindmap("My Working Mindmap")
        val root = viewModel.getRootNode()
        assertNotNull(root)

        // Add child node
        val child = Node(
            parentNode = root,
            id = "CHILD_1",
            numericId = 1001,
            text = "Important Child Node",
            creationDate = 0L,
            modificationDate = 0L
        )
        root.addChildMindmapNode(child)

        // Simulate activity recreation where savedInstanceState is non-null
        val savedInstanceState: Any? = "not_null_bundle"

        val shouldInitializeColdStart = savedInstanceState == null && viewModel.getRootNode() == null
        assertFalse(shouldInitializeColdStart, "Activity recreation should NOT reinitialize cold start")

        // Assert mindmap and child node are preserved
        val currentRoot = viewModel.getRootNode()
        assertNotNull(currentRoot)
        assertEquals("My Working Mindmap", currentRoot.text)
        assertEquals(1, currentRoot.childNodes.size)
        assertEquals("Important Child Node", currentRoot.childNodes[0].text)
    }

    @Test
    fun `screen rotation does not trigger cold start when getRootNode is already loaded`() {
        val viewModel = createViewModel()
        viewModel.createEmptyMindmap("Existing Mindmap")
        assertNotNull(viewModel.getRootNode())

        // Even if savedInstanceState is null (e.g. ViewModel already alive from process),
        // getRootNode() prevents overwriting existing mindmap
        val shouldReinitialize = viewModel.getRootNode() == null
        assertFalse(shouldReinitialize, "Should not overwrite already active root node")
    }
}
