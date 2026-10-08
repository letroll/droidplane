package fr.julien.quievreux.droidplane2.ui.inspector

import fr.julien.quievreux.droidplane2.InspectorTab
import fr.julien.quievreux.droidplane2.MainUiState
import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.TestScope

class ContentTabViewModelTest : KStringSpec() {

    init {
        coroutineTestScope = true

        "opening and saving node properties inspector updates node content and state" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // 1. Open Inspector
            viewModel.onOpenNodeInspector(child, InspectorTab.CONTENT)
            val dialogState = viewModel.uiState.value.dialogUiState.dialogType
            (dialogState is MainUiState.DialogType.NodePropertiesInspector) shouldBe true
            val inspectorDialog = dialogState as MainUiState.DialogType.NodePropertiesInspector
            inspectorDialog.node.id shouldBe child.id
            inspectorDialog.initialTab shouldBe InspectorTab.CONTENT

            // 2. Save modified content
            val updatedChild = child.copy(
                text = "Updated Title from Inspector",
                detailsText = "Added Details",
                noteText = "Added Note",
                latexEquation = "\\pi r^2"
            )

            viewModel.onSaveNodeProperties(updatedChild)

            eventually {
                val stored = nodeManager.getNodeByID(child.id)
                stored shouldNotBe null
                stored!!.text shouldBe "Updated Title from Inspector"
                stored.detailsText shouldBe "Added Details"
                stored.noteText shouldBe "Added Note"
                stored.latexEquation shouldBe "\\pi r^2"
                viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            }
        }

        "dismissing inspector resets dialog state without saving" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            viewModel.onOpenNodeInspector(root)
            (viewModel.uiState.value.dialogUiState.dialogType is MainUiState.DialogType.NodePropertiesInspector) shouldBe true

            viewModel.onDismissNodeInspector()
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
        }

        "moving cloud to parent transfers cloud from child to parent node" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            val childWithCloud = child.copy(
                cloud = fr.julien.quievreux.droidplane2.data.model.CloudProperties(color = "#FFAA00", shape = "ROUND_RECT")
            )
            nodeManager.updateNode(childWithCloud)

            viewModel.onMoveCloudToParent(childWithCloud)

            eventually {
                val storedParent = nodeManager.getNodeByID(root.id)
                val storedChild = nodeManager.getNodeByID(child.id)
                storedParent shouldNotBe null
                storedChild shouldNotBe null
                storedParent!!.cloud shouldNotBe null
                storedParent.cloud?.color shouldBe "#FFAA00"
                storedChild!!.cloud shouldBe null
            }
        }
    }

    private suspend fun eventually(
        timeoutMs: Long = 3000,
        condition: suspend () -> Unit,
    ) {
        val start = System.currentTimeMillis()
        var lastError: Throwable? = null
        while (System.currentTimeMillis() - start < timeoutMs) {
            try {
                condition()
                return
            } catch (e: Throwable) {
                lastError = e
                delay(10)
            }
        }
        throw lastError ?: AssertionError("condition never became true")
    }

    private suspend fun realNodeManager(): NodeManager {
        val logger = mockk<Logger>(relaxed = true)
        val nodeUtils = NodeUtilsDefaultImpl()
        val nodeManager = NodeManager(
            logger = logger,
            nodeUtils = nodeUtils,
            xmlParseUtils = XmlParseUtilsDefaultImpl(nodeUtils, logger),
            coroutineScope = TestScope(),
        )
        val rootId = nodeManager.addNodeToMindmap("root node", null)
        rootId shouldNotBe null
        nodeManager.addNodeToMindmap("child node", nodeManager.getNodeByNumericId(rootId!!))
        return nodeManager
    }
}
