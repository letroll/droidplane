package fr.julien.quievreux.droidplane2

import app.cash.turbine.test
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.DisplayMode
import fr.julien.quievreux.droidplane2.model.RecentFile
import fr.julien.quievreux.droidplane2.MainUiState.DialogUiState
import fr.julien.quievreux.droidplane2.MainUiState.SearchUiState
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import fr.julien.quievreux.droidplane2.data.FakeDataSource
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.model.Node
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
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
                    displayMode shouldBe DisplayMode.MIND_MAP
                    selectedNodeId shouldBe null
                    collapsedNodeIds shouldBe emptySet()
                    treeVersion shouldBe 0L
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

        "addNode targeting a child of currently displayed node should keep current node displayed and attach child to target" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // root has 1 child: "child"
            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // User triggers context menu on "child" to add a sub-child
            viewModel.addNode("grandchild 1", parentNode = child)

            eventually {
                // Currently displayed node should still be root
                viewModel.uiState.value.nodeCurrentlyDisplayed?.id shouldBe root.id
                // Root's child list should have the updated child containing 1 grandchild
                val displayedChild = viewModel.uiState.value.nodeCurrentlyDisplayed?.childNodes?.first { it.id == child.id }
                displayedChild shouldNotBe null
                displayedChild!!.childNodes.size shouldBe 1
                displayedChild.childNodes[0].text shouldBe "grandchild 1"
            }

            // Clicking the child navigates to it and displays the grandchild
            val updatedChild = nodeManager.getNodeByID(child.id)!!
            viewModel.onChildNodeClicked(updatedChild)

            viewModel.uiState.value.nodeCurrentlyDisplayed?.id shouldBe child.id
            viewModel.uiState.value.nodeCurrentlyDisplayed?.childNodes?.size shouldBe 1
            viewModel.uiState.value.nodeCurrentlyDisplayed?.childNodes?.get(0)?.text shouldBe "grandchild 1"
        }

        "FAB addition targets screen node while context menu addition targets listed item" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child1 = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // 1. Simulate FAB addition (targets current screen node: root)
            viewModel.addNode("child2 via FAB", parentNode = viewModel.uiState.value.nodeCurrentlyDisplayed)

            eventually {
                val displayed = viewModel.uiState.value.nodeCurrentlyDisplayed!!
                displayed.id shouldBe root.id
                displayed.childNodes.size shouldBe 2
                displayed.childNodes.any { it.text == "child2 via FAB" } shouldBe true
            }

            // 2. Simulate context menu addition on child1 (targets child1)
            viewModel.addNode("subchild of child1", parentNode = child1)

            eventually {
                val displayed = viewModel.uiState.value.nodeCurrentlyDisplayed!!
                // Still showing root
                displayed.id shouldBe root.id
                // Root child count unchanged
                displayed.childNodes.size shouldBe 2
                // child1 now has 1 child
                val updatedChild1 = displayed.childNodes.first { it.id == child1.id }
                updatedChild1.childNodes.size shouldBe 1
                updatedChild1.childNodes[0].text shouldBe "subchild of child1"
            }
        }

        "addNode with blank or whitespace text should reject addition and leave target node unchanged" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            viewModel.addNode("   ", parentNode = child)
            viewModel.addNode("", parentNode = child)

            // Child's childNodes must remain 0
            val childAfter = nodeManager.getNodeByID(child.id)!!
            childAfter.childNodes.size shouldBe 0
            val rootAfter = nodeManager.getNodeByID(root.id)!!
            rootAfter.childNodes.first { it.id == child.id }.childNodes.size shouldBe 0
        }

        "onNodeClick on target child immediately after adding subchild navigates to target and displays subchild" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            viewModel.addNode("grandchild 1", parentNode = child)

            // Simulate user tapping the list item
            viewModel.onNodeClick(child)

            eventually {
                viewModel.uiState.value.nodeCurrentlyDisplayed?.id shouldBe child.id
                val displayed = viewModel.uiState.value.nodeCurrentlyDisplayed!!
                displayed.childNodes.size shouldBe 1
                displayed.childNodes[0].text shouldBe "grandchild 1"
                viewModel.uiState.value.canGoBack shouldBe true
            }

            viewModel.onNavigateUp()

            eventually {
                viewModel.uiState.value.nodeCurrentlyDisplayed?.id shouldBe root.id
            }
        }

        "node deletion emits undo snackbar and sets canUndoDelete, and undoing restores the node" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // Delete child
            viewModel.onDeleteNode(child)
            viewModel.onConfirmDelete()

            eventually {
                viewModel.uiState.value.canUndoDelete shouldBe true
                viewModel.uiState.value.snackbarMessage shouldNotBe null
                viewModel.uiState.value.snackbarMessage?.message shouldContain child.text.orEmpty()
                viewModel.uiState.value.snackbarMessage?.actionLabel shouldBe R.string.undo
            }

            // Execute undo action from the snackbar
            viewModel.uiState.value.snackbarMessage?.onAction?.invoke()

            eventually {
                viewModel.uiState.value.canUndoDelete shouldBe false
                viewModel.uiState.value.snackbarMessage shouldBe null
                val rootAfter = nodeManager.getNodeByID(root.id)!!
                rootAfter.childNodes.any { it.id == child.id } shouldBe true
            }
        }

        "toggleDisplayMode toggles between MIND_MAP and LIST preserving state" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            val initialTitle = viewModel.uiState.value.title
            val initialNode = viewModel.uiState.value.nodeCurrentlyDisplayed

            viewModel.uiState.value.displayMode shouldBe DisplayMode.MIND_MAP

            viewModel.toggleDisplayMode()
            viewModel.uiState.value.displayMode shouldBe DisplayMode.LIST
            viewModel.uiState.value.title shouldBe initialTitle
            viewModel.uiState.value.nodeCurrentlyDisplayed shouldBe initialNode

            viewModel.toggleDisplayMode()
            viewModel.uiState.value.displayMode shouldBe DisplayMode.MIND_MAP
            viewModel.uiState.value.title shouldBe initialTitle
            viewModel.uiState.value.nodeCurrentlyDisplayed shouldBe initialNode
        }

        "selectNode and clearNodeSelection updates selectedNodeId in uiState" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            val root = viewModel.uiState.value.nodeCurrentlyDisplayed!!
            val child = root.childNodes.first()

            viewModel.selectNode(child)
            viewModel.uiState.value.selectedNodeId shouldBe child.id

            viewModel.clearNodeSelection()
            viewModel.uiState.value.selectedNodeId shouldBe null
        }

        "toggleNodeCollapse adds and removes node ID from collapsedNodeIds" {
            val viewModel = initMainViewModelWithLoadedMindmap()
            val root = viewModel.uiState.value.nodeCurrentlyDisplayed!!

            viewModel.uiState.value.collapsedNodeIds shouldBe emptySet()

            viewModel.toggleNodeCollapse(root)
            viewModel.uiState.value.collapsedNodeIds shouldBe setOf(root.id)

            viewModel.toggleNodeCollapse(root)
            viewModel.uiState.value.collapsedNodeIds shouldBe emptySet()
        }

        "switching from MIND_MAP to LIST focuses nodeCurrentlyDisplayed on selectedNodeId" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // In mindmap mode
            viewModel.setDisplayMode(DisplayMode.MIND_MAP)
            viewModel.selectNode(child)

            // Switch to LIST
            viewModel.toggleDisplayMode()
            viewModel.uiState.value.displayMode shouldBe DisplayMode.LIST
            val expectedDisplayed = if (child.childNodes.isNotEmpty()) child else (child.parentNode ?: child)
            viewModel.uiState.value.nodeCurrentlyDisplayed?.id shouldBe expectedDisplayed.id
        }

        "context menu node operations and node addition work while in MIND_MAP mode" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            viewModel.setDisplayMode(DisplayMode.MIND_MAP)
            viewModel.uiState.value.displayMode shouldBe DisplayMode.MIND_MAP

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // Trigger AddChildNode context menu
            viewModel.onNodeContextMenuClick(ContextMenuAction.AddChildNode(parentNode = child))
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.AddChildNode(parentNode = child)

            // Add node
            viewModel.addNode("mindmap child", parentNode = child)

            eventually {
                val updatedChild = nodeManager.getNodeByID(child.id)!!
                updatedChild.childNodes.size shouldBe 1
                updatedChild.childNodes[0].text shouldBe "mindmap child"
            }

            // Trigger Edit context menu
            viewModel.onNodeContextMenuClick(ContextMenuAction.Edit(node = child))
            val dialog = viewModel.uiState.value.dialogUiState.dialogType
            (dialog is MainUiState.DialogType.EditNodeDescription) shouldBe true
        }

        "deleting a node increments treeVersion and cleans selection and collapsed state" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            viewModel.selectNode(child)
            viewModel.toggleNodeCollapse(child)

            val initialVersion = viewModel.uiState.value.treeVersion

            viewModel.onDeleteNode(child)
            viewModel.onConfirmDelete()

            eventually {
                viewModel.uiState.value.treeVersion shouldBeGreaterThan initialVersion
                viewModel.uiState.value.selectedNodeId shouldBe root.id
                viewModel.uiState.value.collapsedNodeIds.contains(child.id) shouldBe false
                viewModel.uiState.value.canUndoDelete shouldBe true
            }
        }

        "restoring a deleted node increments treeVersion, auto-expands collapsed parent, and selects restored node" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // Delete child
            viewModel.onDeleteNode(child)
            viewModel.onConfirmDelete()

            eventually {
                viewModel.uiState.value.canUndoDelete shouldBe true
            }

            // User puts parent in collapsed mode (mode réduit) while child was deleted
            viewModel.toggleNodeCollapse(root)
            viewModel.uiState.value.collapsedNodeIds shouldBe setOf(root.id)

            val versionBeforeUndo = viewModel.uiState.value.treeVersion

            // Undo delete
            viewModel.onUndoDelete()

            eventually {
                // treeVersion incremented so mindmap canvas recalculates layout immediately
                viewModel.uiState.value.treeVersion shouldBeGreaterThan versionBeforeUndo
                // Parent must be automatically un-collapsed so restored node is visible
                viewModel.uiState.value.collapsedNodeIds.contains(root.id) shouldBe false
                // Restored node is selected
                viewModel.uiState.value.selectedNodeId shouldBe child.id
                viewModel.uiState.value.canUndoDelete shouldBe false
            }
        }

        "onNewMindmapRequested creates new mindmap immediately when no unsaved changes" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            viewModel.onNewMindmapRequested("Brainstorming")

            viewModel.uiState.value.title shouldBe "Brainstorming"
            viewModel.uiState.value.nodeCurrentlyDisplayed?.text shouldBe "Brainstorming"
            viewModel.uiState.value.selectedNodeId shouldBe "ID_1"
            viewModel.uiState.value.navigationStack shouldBe listOf("ID_1")
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            viewModel.hasUnsavedChangesState shouldBe false
        }

        "onNewMindmapRequested shows DiscardConfirmation when unsaved changes exist" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Make an unsaved change
            viewModel.addNode("Unsaved child", root)
            eventually {
                viewModel.hasUnsavedChangesState shouldBe true
            }

            viewModel.onNewMindmapRequested("Fresh Map")

            val dialog = viewModel.uiState.value.dialogUiState.dialogType
            (dialog is MainUiState.DialogType.DiscardConfirmation) shouldBe true
            val discardDialog = dialog as MainUiState.DialogType.DiscardConfirmation

            // Cancel
            discardDialog.onCancel()
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            viewModel.uiState.value.title shouldBe root.text

            // Re-request and Confirm
            viewModel.onNewMindmapRequested("Fresh Map")
            val discardDialog2 = viewModel.uiState.value.dialogUiState.dialogType as MainUiState.DialogType.DiscardConfirmation
            discardDialog2.onConfirm()

            viewModel.uiState.value.title shouldBe "Fresh Map"
            viewModel.uiState.value.nodeCurrentlyDisplayed?.text shouldBe "Fresh Map"
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            viewModel.hasUnsavedChangesState shouldBe false
        }

        "onHelpDemoRequested executes onConfirmLoad immediately when no unsaved changes" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            var loaded = false
            viewModel.onHelpDemoRequested {
                loaded = true
            }

            loaded shouldBe true
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
        }

        "onHelpDemoRequested shows DiscardConfirmation when unsaved changes exist" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Make an unsaved change
            viewModel.addNode("Unsaved child", root)
            eventually {
                viewModel.hasUnsavedChangesState shouldBe true
            }

            var loaded = false
            viewModel.onHelpDemoRequested {
                loaded = true
            }

            loaded shouldBe false
            val dialog = viewModel.uiState.value.dialogUiState.dialogType
            (dialog is MainUiState.DialogType.DiscardConfirmation) shouldBe true
            val discardDialog = dialog as MainUiState.DialogType.DiscardConfirmation

            // Cancel
            discardDialog.onCancel()
            loaded shouldBe false
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None

            // Re-request and Confirm
            viewModel.onHelpDemoRequested {
                loaded = true
            }
            val discardDialog2 = viewModel.uiState.value.dialogUiState.dialogType as MainUiState.DialogType.DiscardConfirmation
            discardDialog2.onConfirm()

            loaded shouldBe true
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            viewModel.hasUnsavedChangesState shouldBe false
        }

        "editing root node on an empty mindmap updates document title and enables unsaved changes" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            viewModel.createEmptyMindmap("Initial Idea")

            viewModel.uiState.value.title shouldBe "Initial Idea"
            val root = viewModel.uiState.value.nodeCurrentlyDisplayed!!

            viewModel.updateNodeText(root, "Renamed Project")

            eventually {
                viewModel.uiState.value.title shouldBe "Renamed Project"
                viewModel.uiState.value.nodeCurrentlyDisplayed?.text shouldBe "Renamed Project"
                viewModel.hasUnsavedChangesState shouldBe true
            }
        }

        "showStartupChooser presents dialog and dispatches choices correctly" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)
            val sampleRecents = listOf(
                RecentFile("content://test/map.mm", "map.mm", 1000L)
            )

            var openedRecent: RecentFile? = null
            var browsed = false
            var openedDemo = false

            viewModel.showStartupChooser(
                recentFiles = sampleRecents,
                onOpenRecent = { openedRecent = it },
                onBrowse = { browsed = true },
                onOpenDemo = { openedDemo = true },
            )

            val dialog = viewModel.uiState.value.dialogUiState.dialogType
            (dialog is MainUiState.DialogType.StartupChooser) shouldBe true
            val chooser = dialog as MainUiState.DialogType.StartupChooser
            chooser.recentFiles shouldBe sampleRecents

            // Test onNewMindmap
            chooser.onNewMindmap()
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            viewModel.uiState.value.selectedNodeId shouldBe "ID_1"

            // Test onOpenRecent
            viewModel.showStartupChooser(sampleRecents, { openedRecent = it }, { browsed = true }, { openedDemo = true })
            val chooser2 = viewModel.uiState.value.dialogUiState.dialogType as MainUiState.DialogType.StartupChooser
            chooser2.onOpenRecent(sampleRecents.first())
            openedRecent shouldBe sampleRecents.first()
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None

            // Test onBrowse
            viewModel.showStartupChooser(sampleRecents, { openedRecent = it }, { browsed = true }, { openedDemo = true })
            val chooser3 = viewModel.uiState.value.dialogUiState.dialogType as MainUiState.DialogType.StartupChooser
            chooser3.onBrowse()
            browsed shouldBe true
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None

            // Test onOpenDemo
            viewModel.showStartupChooser(sampleRecents, { openedRecent = it }, { browsed = true }, { openedDemo = true })
            val chooser4 = viewModel.uiState.value.dialogUiState.dialogType as MainUiState.DialogType.StartupChooser
            chooser4.onOpenDemo()
            openedDemo shouldBe true
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
        }

        "loading new mindmap after creating empty mindmap replaces root and preserves displayed map across view modes" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)

            // Step 1: Start with empty mindmap
            viewModel.createEmptyMindmap("Empty Initial")
            viewModel.uiState.value.title shouldBe "Empty Initial"
            val initialRoot = nodeManager.rootNode!!
            initialRoot.text shouldBe "Empty Initial"

            // Step 2: Now load a new mindmap with different root
            val secondRoot = Node(
                parentNode = null,
                id = "ID_DEMO_ROOT",
                numericId = 99,
                text = "Demo Mindmap Root",
                creationDate = 0L,
                modificationDate = 0L,
            )
            val demoChild = Node(
                parentNode = secondRoot,
                id = "ID_DEMO_CHILD",
                numericId = 100,
                text = "Demo Guide",
                creationDate = 0L,
                modificationDate = 0L,
            )
            secondRoot.childNodes.add(demoChild)

            // Simulate loading new mindmap
            nodeManager.updateRootNode(secondRoot)
            viewModel.loadMindMap(
                inputStream = "".byteInputStream(),
                onLoadFinished = {
                    nodeManager.updateRootNode(secondRoot)
                }
            )

            eventually {
                nodeManager.rootNode?.id shouldBe "ID_DEMO_ROOT"
                nodeManager.rootNode?.text shouldBe "Demo Mindmap Root"
            }

            // Step 3: Switch between LIST and MIND_MAP, verify the active mindmap remains the loaded one
            viewModel.setDisplayMode(DisplayMode.LIST)
            viewModel.uiState.value.displayMode shouldBe DisplayMode.LIST

            viewModel.setDisplayMode(DisplayMode.MIND_MAP)
            viewModel.uiState.value.displayMode shouldBe DisplayMode.MIND_MAP
            viewModel.getRootNode()?.id shouldBe "ID_DEMO_ROOT"
            viewModel.getRootNode()?.text shouldBe "Demo Mindmap Root"
        }

        "loadMindMap reads stream asynchronously without error and closes it in finally" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)

            var streamClosed = false
            val trackingStream = object : java.io.ByteArrayInputStream("<map><node TEXT=\"Root\"/></map>".toByteArray()) {
                override fun close() {
                    super.close()
                    streamClosed = true
                }
            }

            viewModel.loadMindMap(trackingStream)

            eventually {
                viewModel.uiState.value.error shouldBe ""
                viewModel.uiState.value.loading shouldBe false
                streamClosed shouldBe true
            }
        }

        "launchSaveFile derives valid filename from title for a newly created empty mindmap and invokes registerFile" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(logger = mockk(relaxed = true), injectedNodeManager = nodeManager)

            viewModel.createEmptyMindmap("My Fresh Brainstorm")
            viewModel.getSaveFilename() shouldBe "My_Fresh_Brainstorm.mm"

            var registeredFileName: String? = null
            val mockRegister = object : fr.julien.quievreux.droidplane2.helper.FileRegister {
                override fun registerFile(file: java.io.File) {
                    registeredFileName = file.name
                }
                override fun getfilesDir(): String = System.getProperty("java.io.tmpdir")
            }
            viewModel.setFileRegister(mockRegister)

            viewModel.launchSaveFile()

            eventually {
                registeredFileName shouldBe "My_Fresh_Brainstorm.mm"
            }
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
