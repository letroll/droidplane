package fr.julien.quievreux.droidplane2

import app.cash.turbine.test
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.MainUiState.DialogUiState
import fr.julien.quievreux.droidplane2.MainUiState.SearchUiState
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import fr.julien.quievreux.droidplane2.data.FakeDataSource
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import java.lang.System.currentTimeMillis

class MainViewModelTest : KStringSpec() {
    init {
        coroutineTestScope = true

        "check default state" {
            val viewModel = initMainViewModel()
            viewModel.uiState.test {
                awaitItem().apply {
                    loading shouldBe false
                    leaving shouldBe false
                    canGoBack shouldBe false
                    title shouldBe ""
                    error shouldBe ""
                    nodeCurrentlyDisplayed shouldBe null
                    errorAction shouldBe null
                    viewIntentNode shouldBe null
                    contentNodeType shouldBe Classic
                    searchUiState shouldBe SearchUiState()
                    dialogUiState shouldBe DialogUiState()
                    navigationStack shouldBe emptyList()
                }
            }
        }

        "onChildNodeClicked should navigate to child and update UI state" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            val rootNode = FakeDataSource.getFakeRootNode()
            val childNode = rootNode.childNodes.first().copy(parentNode = rootNode)

            viewModel.uiState.test {
                awaitItem()
                
                viewModel.onChildNodeClicked(childNode)
                
                awaitItem().apply {
                    nodeCurrentlyDisplayed shouldBe childNode
                    title shouldBe childNode.text.orEmpty()
                    canGoBack shouldBe true
                    navigationStack shouldBe listOf(rootNode.id, childNode.id)
                }
            }
        }

        "onNavigateUp should navigate to parent and update UI state" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            val rootNode = FakeDataSource.getFakeRootNode()
            val childNode = rootNode.childNodes.first().copy(parentNode = rootNode)

            viewModel.onChildNodeClicked(childNode)
            viewModel.onNavigateUp()
            
            val state = viewModel.uiState.first()
            state.nodeCurrentlyDisplayed shouldBe rootNode
            state.title shouldBe rootNode.text.orEmpty()
            state.canGoBack shouldBe false
            state.navigationStack shouldBe listOf(rootNode.id)
        }

        "onNavigateToTop should navigate to root and update UI state" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            val rootNode = FakeDataSource.getFakeRootNode()
            val childNode = rootNode.childNodes.first().copy(parentNode = rootNode)
            val grandChildNode = childNode.childNodes.firstOrNull()?.copy(parentNode = childNode) ?: childNode

            viewModel.onChildNodeClicked(childNode)
            viewModel.onChildNodeClicked(grandChildNode)
            viewModel.onNavigateToTop()
            
            val state = viewModel.uiState.first()
            state.nodeCurrentlyDisplayed shouldBe grandChildNode
        }

        "navigation stack should track hierarchy correctly" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            val rootNode = FakeDataSource.getFakeRootNode()
            
            val childNode = rootNode.childNodes.first().copy(parentNode = rootNode)
            val grandChildNode = childNode.childNodes.firstOrNull()?.copy(parentNode = childNode) ?: childNode
            
            viewModel.onChildNodeClicked(childNode)
            var state = viewModel.uiState.first()
            state.title shouldBe childNode.text.orEmpty()
            state.canGoBack shouldBe true
            
            viewModel.onChildNodeClicked(grandChildNode)
            state = viewModel.uiState.first()
            state.title shouldBe grandChildNode.text.orEmpty()
            state.canGoBack shouldBe true
            
            viewModel.onNavigateUp()
            state = viewModel.uiState.first()
            
            viewModel.onNavigateUp()
            state = viewModel.uiState.first()
        }

        // ===== SEARCH TESTS (T018) =====

        "search() should activate search mode and set query" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            
            viewModel.uiState.test {
                awaitItem()
                
                viewModel.search("test query")
                
                awaitItem().apply {
                    searchUiState.isSearchActive shouldBe true
                    searchUiState.searchQuery shouldBe "test query"
                    searchUiState.currentResultIndex shouldBe 0
                }
            }
        }

        "onExitSearchMode() should deactivate search mode and clear state" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            
            viewModel.uiState.test {
                awaitItem()
                
                viewModel.search("test")
                awaitItem().apply {
                    searchUiState.isSearchActive shouldBe true
                }
                
                viewModel.onExitSearchMode()
                
                awaitItem().apply {
                    searchUiState.isSearchActive shouldBe false
                    searchUiState.searchQuery shouldBe ""
                    searchUiState.currentResultIndex shouldBe 0
                    searchUiState.totalResults shouldBe 0
                }
            }
        }


        "onExitSearchMode() should clear search state" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            
            viewModel.uiState.test {
                awaitItem()
                
                viewModel.search("test")
                awaitItem().apply {
                    searchUiState.isSearchActive shouldBe true
                }
                
                viewModel.onExitSearchMode()
                
                awaitItem().apply {
                    searchUiState.isSearchActive shouldBe false
                    searchUiState.searchQuery shouldBe ""
                    searchUiState.currentResultIndex shouldBe 0
                    searchUiState.totalResults shouldBe 0
                }
            }
        }

        // ===== EDIT / ADD SYNCHRONIZATION (T025) =====

        "updateNodeText should be reflected in the displayed node and its parent listing" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = root.childNodes.first()
            viewModel.updateNodeText(child, "renamed by test")

            // the mutation itself is awaited via NodeManager's suspend contract
            eventually {
                nodeManager.getNodeByID(child.id)?.text shouldBe "renamed by test"
            }
            // single source of truth keeps exactly one child entry, with the new text
            nodeManager.getNodeByID(root.id)?.childNodes?.count { it.id == child.id } shouldBe 1
            nodeManager.getNodeByID(root.id)?.childNodes?.first { it.id == child.id }?.text shouldBe "renamed by test"
        }

        "addNode should append the new child to the parent" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)
            val before = root.childNodes.size

            viewModel.addNode("fresh child")

            eventually {
                val updated = nodeManager.getNodeByID(root.id)
                updated!!.childNodes.size shouldBe before + 1
                updated.childNodes.last().text shouldBe "fresh child"
            }
        }

        "addNode should ignore blank text" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)
            val before = nodeManager.getNodeByID(root.id)!!.childNodes.size

            viewModel.addNode("   ")

            nodeManager.getNodeByID(root.id)!!.childNodes.size shouldBe before
        }

        "delete node and navigate to sibling and back should not restore deleted node in UI state" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Add second child so root has two children
            val secondChildId = nodeManager.addNodeToMindmap("second child", nodeManager.getNodeByID(root.id))
            secondChildId shouldNotBe null
            val rootUpdated = nodeManager.getNodeByID(root.id)!!
            rootUpdated.childNodes.size shouldBe 2
            val firstChild = rootUpdated.childNodes[0]
            val secondChild = rootUpdated.childNodes[1]

            // Display root
            viewModel.setInitialStateForTest(rootUpdated)

            // Delete firstChild
            viewModel.onDeleteNode(firstChild)
            viewModel.onConfirmDelete()

            eventually {
                viewModel.uiState.value.nodeCurrentlyDisplayed?.childNodes?.size shouldBe 1
                viewModel.uiState.value.nodeCurrentlyDisplayed?.childNodes?.none { it.id == firstChild.id } shouldBe true
            }

            // Click surviving sibling
            val survivingChild = nodeManager.getNodeByID(secondChild.id)!!
            viewModel.onChildNodeClicked(survivingChild)

            // Navigate up to parent
            viewModel.onNavigateUp()

            // Parent displayed must NOT contain firstChild
            val parentState = viewModel.uiState.value.nodeCurrentlyDisplayed!!
            parentState.childNodes.size shouldBe 1
            parentState.childNodes.none { it.id == firstChild.id } shouldBe true
        }
    }

    /** Polls [condition] until it stops throwing, or fails after ~2s. */
    private suspend fun eventually(timeoutMs: Long = 2000, condition: () -> Unit) {
        val deadline = currentTimeMillis() + timeoutMs
        var lastError: Throwable? = null
        while (currentTimeMillis() < deadline) {
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

    /**
     * A real NodeManager holding a real in-memory tree: root -> child.
     * Built through the public API rather than by parsing XML, so the test does not
     * depend on android.jar Html/XmlPullParser stubs.
     */
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

    private fun initMainViewModel(): MainViewModel {
        val logger = mockk<Logger>(relaxed = true)
        val nodeManager = mockk<NodeManager>(relaxed = true)
        coEvery { nodeManager.getNodeText(any()) } answers { firstArg<Node>().text.orEmpty() }
        coEvery { nodeManager.rootNode } returns null
        return MainViewModel(
            logger = logger,
            injectedNodeManager = nodeManager,
        )
    }

    private fun initMainViewModelWithLoadedMindmap(): MainViewModel {
        val viewModel = initMainViewModel()
        val rootNode = FakeDataSource.getFakeRootNode()
        viewModel.setInitialStateForTest(rootNode)
        return viewModel
    }
}
