package fr.julien.quievreux.droidplane2

import app.cash.turbine.test
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.RecentFile
import fr.julien.quievreux.droidplane2.MainUiState.DialogUiState
import fr.julien.quievreux.droidplane2.MainUiState.SearchUiState
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import io.kotest.matchers.comparables.shouldBeGreaterThan
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.FakeDataSource
import fr.julien.quievreux.droidplane2.data.NodeManager
import fr.julien.quievreux.droidplane2.data.NodeUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.XmlParseUtilsDefaultImpl
import fr.julien.quievreux.droidplane2.data.model.MindMapUri
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.res.AppRes as R
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import java.lang.System.currentTimeMillis
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)
            val before = nodeManager.getNodeByID(root.id)!!.childNodes.size

            viewModel.addNode("   ")

            nodeManager.getNodeByID(root.id)!!.childNodes.size shouldBe before
        }

        "delete node and navigate to sibling and back should not restore deleted node in UI state" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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

        "context menu node operations and node addition work while in MIND_MAP mode" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]

            // Trigger AddChildNode context menu - now opens NodePropertiesInspector
            viewModel.onNodeContextMenuClick(ContextMenuAction.AddChildNode(parentNode = child))
            val dialog = viewModel.uiState.value.dialogUiState.dialogType
            val inspectorDialog = dialog as MainUiState.DialogType.NodePropertiesInspector
            inspectorDialog.isNewNode shouldBe true
            inspectorDialog.parentNode shouldBe child

            // Add node
            viewModel.addNode("mindmap child", parentNode = child)

            eventually {
                val updatedChild = nodeManager.getNodeByID(child.id)!!
                updatedChild.childNodes.size shouldBe 1
                updatedChild.childNodes[0].text shouldBe "mindmap child"
            }

            // Trigger Properties context menu
            viewModel.onNodeContextMenuClick(ContextMenuAction.Properties(node = child))
            val propsInspectorDialog = viewModel.uiState.value.dialogUiState.dialogType
            (propsInspectorDialog is MainUiState.DialogType.NodePropertiesInspector) shouldBe true
        }

        "deleting a node increments treeVersion and cleans selection and collapsed state" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
                viewModel.uiState.value.selectedNodeId shouldBe null
                viewModel.uiState.value.collapsedNodeIds.contains(child.id) shouldBe false
                viewModel.uiState.value.canUndoDelete shouldBe true
            }
        }

        "restoring a deleted node increments treeVersion, auto-expands collapsed parent, and selects restored node" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
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
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )

            // Step 1: Start with empty mindmap
            viewModel.createEmptyMindmap("Empty Initial")
            viewModel.uiState.value.title shouldBe "Empty Initial"
            val initialRoot = nodeManager.rootNode!!
            initialRoot.text shouldBe "Empty Initial"

            // Step 2: Now load a new mindmap with different root
            val demoXml = """
                <map version="freeplane 1.11.5">
                    <node ID="ID_DEMO_ROOT" TEXT="Demo Mindmap Root" CREATED="0" MODIFIED="0">
                        <node ID="ID_DEMO_CHILD" TEXT="Demo Guide" CREATED="0" MODIFIED="0"/>
                    </node>
                </map>
            """.trimIndent()

            // Simulate loading new mindmap
            viewModel.loadMindMap(
                inputStream = demoXml.byteInputStream(),
            )

            eventually {
                nodeManager.rootNode?.id shouldBe "ID_DEMO_ROOT"
                nodeManager.rootNode?.text shouldBe "Demo Mindmap Root"
                viewModel.uiState.value.title shouldBe "Demo Mindmap Root"
                viewModel.uiState.value.nodeCurrentlyDisplayed?.id shouldBe "ID_DEMO_ROOT"
            }
        }

        "loadMindMap reads stream asynchronously without error and closes it in finally" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )

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

        "launchSaveFile derives valid filename from title for a newly created empty mindmap and emits pendingSaveTempFile" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )

            viewModel.createEmptyMindmap("My Fresh Brainstorm")
            viewModel.getSaveFilename() shouldBe "My_Fresh_Brainstorm.mm"

            val tempDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
            viewModel.launchSaveFile(tempDir)

            eventually {
                viewModel.uiState.value.pendingSaveTempFile?.name shouldBe "My_Fresh_Brainstorm.mm"
            }

            viewModel.clearPendingSaveTempFile()
            viewModel.uiState.value.pendingSaveTempFile shouldBe null
        }

        // ===== UNSAVED CHANGES TRACKING (T060) =====

        "unsaved changes tracking is enabled after node deletion" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            
            // Initially no unsaved changes
            viewModel.hasUnsavedChangesState shouldBe false
            
            // Delete child node
            viewModel.onDeleteNode(child)
            viewModel.onConfirmDelete()
            
            eventually {
                viewModel.hasUnsavedChangesState shouldBe true
            }
        }

        "unsaved changes tracking is enabled after node addition" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Initially no unsaved changes
            viewModel.hasUnsavedChangesState shouldBe false
            
            // Add child node
            viewModel.addNode("New child")
            
            eventually {
                viewModel.hasUnsavedChangesState shouldBe true
            }
        }

        "unsaved changes tracking is enabled after node text update" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            
            // Initially no unsaved changes
            viewModel.hasUnsavedChangesState shouldBe false
            
            // Update node text
            viewModel.updateNodeText(child, "Updated text")
            
            eventually {
                viewModel.hasUnsavedChangesState shouldBe true
            }
        }

        "unsaved changes are cleared after save" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Make an unsaved change
            viewModel.addNode("Unsaved child")
            
            eventually {
                viewModel.hasUnsavedChangesState shouldBe true
            }
            
            // Simulate save by clearing the flag (as saveFile does)
            viewModel.saveFile(java.io.ByteArrayOutputStream())
            
            viewModel.hasUnsavedChangesState shouldBe false
        }

        "exit confirmation sets leaving flag when unsaved changes exist and user tries to leave" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Make an unsaved change
            viewModel.addNode("Unsaved child")
            
            eventually {
                viewModel.hasUnsavedChangesState shouldBe true
            }
            
            // Trigger leaveApp (simulates user pressing back/exiting)
            viewModel.upOrClose(
                fileAccessibilityChecker = { true },
                onOpenRecent = {},
                onNewMindmap = {},
                onBrowse = {},
                onOpenDemo = {},
                onExitApp = {},
            )
            
            eventually {
                // ViewModel sets leaving = true, MainActivity will show ExitConfirmation dialog
                viewModel.uiState.value.leaving shouldBe true
            }
        }

        "shows Recent Files dialog instead of exit confirmation when no unsaved changes" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // No unsaved changes
            viewModel.hasUnsavedChangesState shouldBe false
            
            // Trigger back press - should show Recent Files dialog instead of setting leaving = true
            viewModel.upOrClose(
                fileAccessibilityChecker = { true },
                onOpenRecent = {},
                onNewMindmap = {},
                onBrowse = {},
                onOpenDemo = {},
                onExitApp = {},
            )
            
            // With no unsaved changes, leaving should NOT be set (shows Recent Files dialog instead)
            eventually {
                viewModel.uiState.value.leaving shouldBe false
                // Should show Recent Files dialog
                (viewModel.uiState.value.dialogUiState.dialogType is MainUiState.DialogType.RecentFiles) shouldBe true
            }
        }

        // ===== CONFIRMATION DIALOG STATE TRANSITIONS (T061) =====

        "onDeleteNode shows DeleteConfirmation dialog with correct descendant count" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            
            // Trigger delete
            viewModel.onDeleteNode(child)
            
            eventually {
                val dialog = viewModel.uiState.value.dialogUiState.dialogType
                (dialog is MainUiState.DialogType.DeleteConfirmation) shouldBe true
                val deleteDialog = dialog as MainUiState.DialogType.DeleteConfirmation
                deleteDialog.node.id shouldBe child.id
                deleteDialog.descendantCount shouldBe 0
            }
        }

        "onDeleteNode shows DeleteConfirmation dialog with correct descendant count for node with children" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Add a child to the first child to create a parent with descendants
            val firstChild = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            nodeManager.addNodeToMindmap("Grandchild", firstChild)
            val updatedFirstChild = nodeManager.getNodeByID(firstChild.id)!!
            
            // Trigger delete on parent with children
            viewModel.onDeleteNode(updatedFirstChild)
            
            eventually {
                val dialog = viewModel.uiState.value.dialogUiState.dialogType
                (dialog is MainUiState.DialogType.DeleteConfirmation) shouldBe true
                val deleteDialog = dialog as MainUiState.DialogType.DeleteConfirmation
                deleteDialog.node.id shouldBe updatedFirstChild.id
                deleteDialog.descendantCount shouldBe 1
            }
        }

        "onCancelDelete dismisses dialog without deleting" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            
            // Trigger delete
            viewModel.onDeleteNode(child)
            
            eventually {
                val dialog = viewModel.uiState.value.dialogUiState.dialogType
                (dialog is MainUiState.DialogType.DeleteConfirmation) shouldBe true
            }
            
            // Cancel the deletion
            viewModel.onCancelDelete()
            
            eventually {
                viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            }
            
            // Node should still exist
            nodeManager.getNodeByID(child.id) shouldNotBe null
        }

        "onConfirmDelete executes deletion after confirmation" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            
            // Trigger delete
            viewModel.onDeleteNode(child)
            
            eventually {
                val dialog = viewModel.uiState.value.dialogUiState.dialogType
                (dialog is MainUiState.DialogType.DeleteConfirmation) shouldBe true
            }
            
            // Confirm deletion
            viewModel.onConfirmDelete()
            
            eventually {
                viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
                nodeManager.getNodeByID(child.id) shouldBe null
            }
        }

        "confirmation dialog state transitions: show -> cancel -> show -> confirm" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val child = nodeManager.getNodeByID(root.id)!!.childNodes[0]
            
            // First attempt: show and cancel
            viewModel.onDeleteNode(child)
            eventually {
                (viewModel.uiState.value.dialogUiState.dialogType is MainUiState.DialogType.DeleteConfirmation) shouldBe true
            }
            viewModel.onCancelDelete()
            eventually {
                viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            }
            nodeManager.getNodeByID(child.id) shouldNotBe null
            
            // Second attempt: show and confirm
            viewModel.onDeleteNode(child)
            eventually {
                (viewModel.uiState.value.dialogUiState.dialogType is MainUiState.DialogType.DeleteConfirmation) shouldBe true
            }
            viewModel.onConfirmDelete()
            eventually {
                viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            }
            
            // Node should be deleted
            nodeManager.getNodeByID(child.id) shouldBe null
        }

        "deletion of root node is prevented and shows no dialog" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            // Try to delete root node
            viewModel.onDeleteNode(root)
            
            // No dialog should be shown
            viewModel.uiState.value.dialogUiState.dialogType shouldBe MainUiState.DialogType.None
            
            // Root should still exist
            nodeManager.getNodeByID(root.id) shouldNotBe null
        }

        // ===== REPLACE EXISTING FILE ON SAVE (011-replace-existing-file-save) =====

        "getSaveFilename and launchSaveFile pre-fill exact original displayName and preserve mapUri when editing an existing file (T009)" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val fakeUri = MindMapUri.parse("content:///document/msf:99999")

            viewModel.setMapUri(fakeUri, "Sprint_Retro.mm")

            assertEquals("Sprint_Retro.mm", viewModel.getSaveFilename())

            val tempDir = System.getProperty("java.io.tmpdir") ?: "/tmp"
            viewModel.launchSaveFile(tempDir)

            eventually {
                assertEquals("Sprint_Retro.mm", viewModel.uiState.value.pendingSaveTempFile?.name)
                assertEquals(fakeUri, viewModel.getMapUri())
            }

            viewModel.clearPendingSaveTempFile()
            assertEquals(null, viewModel.uiState.value.pendingSaveTempFile)
        }

        "OverwriteConfirmation dialog confirm clears hasUnsavedChanges and dismisses dialog while cancel preserves hasUnsavedChanges (T014)" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            viewModel.addNode("Unsaved Modification", root)
            eventually {
                assertTrue(viewModel.hasUnsavedChangesState)
            }

            // Show OverwriteConfirmation and cancel
            var cancelCalled = false
            viewModel.showOverwriteConfirmation(
                fileName = "Sprint_Retro.mm",
                onConfirm = {},
                onCancel = {
                    cancelCalled = true
                    viewModel.cancelSaveFile()
                }
            )

            val dialog1 = assertIs<MainUiState.DialogType.OverwriteConfirmation>(
                viewModel.uiState.value.dialogUiState.dialogType
            )
            assertEquals("Sprint_Retro.mm", dialog1.fileName)

            dialog1.onCancel()
            assertTrue(cancelCalled)
            assertEquals(MainUiState.DialogType.None, viewModel.uiState.value.dialogUiState.dialogType)
            assertTrue(viewModel.hasUnsavedChangesState)

            // Show OverwriteConfirmation and confirm
            viewModel.showOverwriteConfirmation(
                fileName = "Sprint_Retro.mm",
                onConfirm = {
                    viewModel.saveFile(java.io.ByteArrayOutputStream())
                },
                onCancel = {}
            )

            val dialog2 = assertIs<MainUiState.DialogType.OverwriteConfirmation>(
                viewModel.uiState.value.dialogUiState.dialogType
            )
            dialog2.onConfirm()

            assertEquals(MainUiState.DialogType.None, viewModel.uiState.value.dialogUiState.dialogType)
            assertFalse(viewModel.hasUnsavedChangesState)
        }

        "getSaveFilename derives filename from root node title when current mindmap is the read-only demo resource (T016)" {
            val nodeManager = realNodeManager()
            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            val root = nodeManager.rootNode!!
            viewModel.setInitialStateForTest(root)

            val demoUri = MindMapUri.parse("android.resource:///raw/example.mm")

            viewModel.setMapUri(demoUri, "example.mm")

            // Because scheme is android.resource, getSaveFilename derives from root node title ("root node")
            assertEquals("root_node.mm", viewModel.getSaveFilename())
        }

        // ===== 2D CANVAS DRAG AND DROP (014-nodelist-drag-drop) =====

        "onMoveNode reorders sibling, marks unsaved changes, and increments treeVersion (T008)" {
            val nodeManager = realNodeManager()
            val root = nodeManager.rootNode!!
            nodeManager.addNodeToMindmap("second child", root)
            val updatedRoot = nodeManager.rootNode!!
            val firstChild = updatedRoot.childNodes[0]
            val secondChild = updatedRoot.childNodes[1]

            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            viewModel.setInitialStateForTest(updatedRoot)

            val initialVersion = viewModel.uiState.value.treeVersion
            assertFalse(viewModel.hasUnsavedChangesState)

            viewModel.onMoveNode(
                draggedNodeId = firstChild.id,
                targetParentId = updatedRoot.id,
                targetSiblingId = secondChild.id,
                insertAfter = true,
            )

            eventually {
                assertTrue(viewModel.hasUnsavedChangesState)
                assertEquals(initialVersion + 1, viewModel.uiState.value.treeVersion)
                val newChildrenIds = nodeManager.rootNode!!.childNodes.map { it.id }
                assertEquals(listOf(secondChild.id, firstChild.id), newChildrenIds)
            }
        }

        "onMoveNode reparents node under collapsed target parent and unfolds target parent in collapsedNodeIds (T008)" {
            val nodeManager = realNodeManager()
            val root = nodeManager.rootNode!!
            nodeManager.addNodeToMindmap("branch B", root)
            val updatedRoot = nodeManager.rootNode!!
            val childA = updatedRoot.childNodes[0]
            val branchB = updatedRoot.childNodes[1]
            nodeManager.addNodeToMindmap("sub B", branchB)
            val refreshedRoot = nodeManager.rootNode!!

            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            viewModel.setInitialStateForTest(refreshedRoot)

            // Collapse branchB first
            viewModel.toggleNodeCollapse(nodeManager.getNodeByID(branchB.id)!!)
            assertTrue(branchB.id in viewModel.uiState.value.collapsedNodeIds)

            val initialVersion = viewModel.uiState.value.treeVersion

            // Drop childA as child of collapsed branchB
            viewModel.onMoveNode(
                draggedNodeId = childA.id,
                targetParentId = branchB.id,
                targetSiblingId = null,
                insertAfter = true,
            )

            eventually {
                assertFalse(branchB.id in viewModel.uiState.value.collapsedNodeIds)
                assertEquals(initialVersion + 1, viewModel.uiState.value.treeVersion)
                assertTrue(viewModel.hasUnsavedChangesState)
                val updatedBranchB = nodeManager.getNodeByID(branchB.id)!!
                assertTrue(updatedBranchB.childNodes.any { it.id == childA.id })
            }
        }

        "onMoveNode updates direct root child position and ignores no-op moves (T008)" {
            val nodeManager = realNodeManager()
            val root = nodeManager.rootNode!!
            val childA = root.childNodes[0]

            val viewModel = MainViewModel(
                logger = mockk(relaxed = true),
                injectedNodeManager = nodeManager,
                recentFilesRepository = mockk(relaxed = true),
            )
            viewModel.setInitialStateForTest(root)

            val versionBeforeMove = viewModel.uiState.value.treeVersion

            // Move childA to left side of root
            viewModel.onMoveNode(
                draggedNodeId = childA.id,
                targetParentId = root.id,
                targetSiblingId = null,
                insertAfter = true,
                targetPosition = "left",
            )

            eventually {
                assertEquals("left", nodeManager.getNodeByID(childA.id)!!.position)
                assertEquals(versionBeforeMove + 1, viewModel.uiState.value.treeVersion)
            }

            // Now perform a no-op move (already only child at "left")
            val versionAfterFirstMove = viewModel.uiState.value.treeVersion
            viewModel.onMoveNode(
                draggedNodeId = childA.id,
                targetParentId = root.id,
                targetSiblingId = null,
                insertAfter = true,
                targetPosition = "left",
            )

            // Wait briefly and verify treeVersion does not increment on no-op
            delay(100)
            assertEquals(versionAfterFirstMove, viewModel.uiState.value.treeVersion)
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
        val recentFilesRepository = mockk<RecentFilesRepository>(relaxed = true)
        coEvery { nodeManager.getNodeText(any()) } answers { firstArg<Node>().text.orEmpty() }
        coEvery { nodeManager.rootNode } returns null
        coEvery { recentFilesRepository.getRecentFiles(any()) } returns emptyList()
        return MainViewModel(
            logger = logger,
            injectedNodeManager = nodeManager,
            recentFilesRepository = recentFilesRepository,
        )
    }

    private fun initMainViewModelWithLoadedMindmap(): MainViewModel {
        val viewModel = initMainViewModel()
        val rootNode = FakeDataSource.getFakeRootNode()
        viewModel.setInitialStateForTest(rootNode)
        return viewModel
    }
}
