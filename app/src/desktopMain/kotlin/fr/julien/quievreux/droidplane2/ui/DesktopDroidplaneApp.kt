package fr.julien.quievreux.droidplane2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.SnackbarResult.Dismissed
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.julien.quievreux.droidplane2.InspectorTab
import fr.julien.quievreux.droidplane2.MainUiState
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.AddChildNode
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.DeleteConfirmation
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.DiscardConfirmation
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.ExitConfirmation
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.NodePropertiesInspector
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.None
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.OverwriteConfirmation
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.RecentFiles
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.StartupChooser
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.UpdateAvailable
import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.ui.component.AppFloatingActionButton
import fr.julien.quievreux.droidplane2.core.ui.component.CustomDialog
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.createDesktopFileChecker
import fr.julien.quievreux.droidplane2.data.model.MindMapUri
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RelativeFile
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RichText
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.OpenLink
import fr.julien.quievreux.droidplane2.model.ContextMenuAction.Properties
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.ADD_CHILD
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.DELETE
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.OPEN_LINK
import fr.julien.quievreux.droidplane2.model.NodeToolbarAction.PROPERTIES
import fr.julien.quievreux.droidplane2.res.AppRes as R
import fr.julien.quievreux.droidplane2.res.stringResource
import fr.julien.quievreux.droidplane2.ui.components.AppTopBar
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Backpress
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.CheckForUpdates
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.ExitSearch
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Help
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.NewMindmap
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Open
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Save
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchNext
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchPrevious
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Undo
import fr.julien.quievreux.droidplane2.ui.components.NodeBottomToolbar
import fr.julien.quievreux.droidplane2.ui.dialog.UpdateAvailableDialog
import fr.julien.quievreux.droidplane2.ui.inspector.NodePropertiesInspector
import fr.julien.quievreux.droidplane2.ui.mindmap.MindMapCanvasScreen
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.ui.view.DeleteConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.DiscardConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.ExitConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.OverwriteConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.RecentFilesDialog
import fr.julien.quievreux.droidplane2.ui.view.StartupChooserDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.awt.Desktop
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FilenameFilter
import java.net.URI

private const val DESKTOP_APP_VERSION = "1.0.31"

@Composable
fun DesktopDroidplaneApp(
    viewModel: MainViewModel,
    recentFilesRepository: RecentFilesRepository,
    logger: Logger,
    onExitApplication: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showBottomToolbar by remember { mutableStateOf(false) }
    var selectedNodeForToolbar by remember { mutableStateOf<String?>(null) }

    fun openFileFromDisk(file: File) {
        if (!file.exists()) return
        val uriString = "file://${file.absolutePath}"
        recentFilesRepository.addRecentFile(uriString, file.name)
        viewModel.setMapUri(MindMapUri.parse(uriString), file.name)
        scope.launch(Dispatchers.IO) {
            try {
                FileInputStream(file).use { stream ->
                    viewModel.loadMindMap(stream)
                }
            } catch (e: Exception) {
                logger.e("Error opening file on Desktop: $e")
            }
        }
    }

    fun pickAndOpenFile() {
        val dialog = FileDialog(null as Frame?, R.getString(R.string.open), FileDialog.LOAD)
        dialog.filenameFilter = FilenameFilter { _, name -> name.endsWith(".mm", ignoreCase = true) }
        dialog.isVisible = true
        val dir = dialog.directory
        val fileName = dialog.file
        if (dir != null && fileName != null) {
            openFileFromDisk(File(dir, fileName))
        }
    }

    fun pickAndSaveFile(defaultFileName: String) {
        val dialog = FileDialog(null as Frame?, R.getString(R.string.save), FileDialog.SAVE)
        dialog.file = defaultFileName
        dialog.isVisible = true
        val dir = dialog.directory
        val fileName = dialog.file
        if (dir != null && fileName != null) {
            val targetName = if (fileName.endsWith(".mm", ignoreCase = true)) fileName else "$fileName.mm"
            val targetFile = File(dir, targetName)
            scope.launch(Dispatchers.IO) {
                try {
                    FileOutputStream(targetFile).use { outputStream ->
                        viewModel.saveFile(outputStream)
                    }
                    val uriString = "file://${targetFile.absolutePath}"
                    viewModel.setMapUri(MindMapUri.parse(uriString), targetFile.name)
                    recentFilesRepository.addRecentFile(uriString, targetFile.name)
                } catch (e: Exception) {
                    logger.e("Failed to write mindmap to ${targetFile.absolutePath}: $e")
                }
            }
        } else {
            viewModel.cancelSaveFile()
        }
    }

    fun openDemoMap() {
        viewModel.setMapUri(MindMapUri.parse("classpath://raw/example.mm"), "example.mm")
        scope.launch(Dispatchers.IO) {
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream("raw/example.mm")
            if (stream != null) {
                viewModel.loadMindMap(stream)
            } else {
                logger.e("Demo resource raw/example.mm not found on classpath")
            }
        }
    }

    fun openUrlInBrowser(url: String) {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(URI(url))
            }
        } catch (e: Exception) {
            logger.e("Failed to open URL in browser ($url): $e")
        }
    }

    fun triggerUpdateCheck(isManual: Boolean) {
        viewModel.checkForUpdates(
            currentVersion = DESKTOP_APP_VERSION,
            isManual = isManual,
            onDownloadAndInstall = { apkUrl, _ ->
                openUrlInBrowser(apkUrl)
            },
            onViewOnGitHub = { url ->
                openUrlInBrowser(url)
            },
            upToDateMessage = R.getString(R.string.update_up_to_date, DESKTOP_APP_VERSION),
            errorMessage = R.getString(R.string.update_error)
        )
    }

    LaunchedEffect(Unit) {
        if (viewModel.getRootNode() == null) {
            viewModel.createEmptyMindmap(R.getString(R.string.default_root_node_title))
            viewModel.showStartupChooser(
                fileAccessibilityChecker = createDesktopFileChecker(),
                onOpenRecent = { recent ->
                    val path = MindMapUri.parse(recent.uriString).path ?: recent.uriString
                    openFileFromDisk(File(path))
                },
                onBrowse = { pickAndOpenFile() },
                onOpenDemo = { openDemoMap() }
            )
            triggerUpdateCheck(isManual = false)
        }
    }

    ContrastAwareReplyTheme {
        val state = viewModel.uiState.collectAsState()
        val nodeFindList = viewModel.getSearchResultFlow().collectAsState()

        LaunchedEffect(state.value.leaving) {
            if (state.value.leaving) {
                if (viewModel.hasUnsavedChangesState && state.value.dialogUiState.dialogType is None) {
                    viewModel.resetLeaving()
                    viewModel.setDialogState(
                        ExitConfirmation(
                            onConfirm = {
                                viewModel.resetLeaving()
                                onExitApplication()
                            },
                            onCancel = {
                                viewModel.resetLeaving()
                                viewModel.setDialogState(None)
                            }
                        )
                    )
                } else if (!viewModel.hasUnsavedChangesState) {
                    viewModel.resetLeaving()
                    onExitApplication()
                }
            }
        }

        LaunchedEffect(state.value.pendingSaveTempFile) {
            state.value.pendingSaveTempFile?.let { file ->
                viewModel.clearPendingSaveTempFile()
                pickAndSaveFile(file.name)
            }
        }

        LaunchedEffect(state.value.error) {
            val message = state.value.error
            if (message.isNotEmpty()) {
                state.value.errorAction?.let { errorAction ->
                    scope.launch {
                        val result = snackbarHostState.showSnackbar(
                            message = message,
                            actionLabel = R.getString(errorAction.actionLabel),
                            duration = Indefinite
                        )
                        if (result == ActionPerformed) {
                            errorAction.action()
                        }
                    }
                } ?: scope.launch {
                    snackbarHostState.showSnackbar(message = message, duration = Indefinite)
                }
            }
        }

        LaunchedEffect(state.value.viewIntentNode) {
            state.value.viewIntentNode?.let { viewIntentNode ->
                when (state.value.contentNodeType) {
                    RichText -> {
                        // Inspect node content in NodePropertiesInspector on Desktop
                        viewModel.onNodeContextMenuClick(Properties(viewIntentNode.node))
                    }
                    RelativeFile, Classic -> {
                        val targetUri = viewIntentNode.uriString
                        if (targetUri != null) {
                            val parsed = MindMapUri.parse(targetUri)
                            if (parsed.scheme == "file" && parsed.path != null) {
                                try {
                                    Desktop.getDesktop().open(File(parsed.path!!))
                                } catch (_: Exception) {
                                    openUrlInBrowser(targetUri)
                                }
                            } else {
                                openUrlInBrowser(targetUri)
                            }
                        }
                    }
                }
            }
        }

        LaunchedEffect(state.value.snackbarMessage?.eventId) {
            state.value.snackbarMessage?.let { snackbarMsg ->
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = snackbarMsg.message,
                        actionLabel = snackbarMsg.actionLabel?.let { R.getString(it) },
                        duration = SnackbarDuration.Short,
                    )
                    if (result == ActionPerformed) {
                        snackbarMsg.onAction?.invoke()
                    }
                    viewModel.clearSnackbarMessage()
                }
            }
        }

        DesktopDialogs(state = state, viewModel = viewModel)

        Scaffold(
            modifier = Modifier,
            topBar = {
                AppTopBar(
                    text = state.value.title.ifEmpty { stringResource(R.string.app_name) },
                    hasBackIcon = state.value.canGoBack,
                    onQuery = { query -> viewModel.search(query) },
                    hasSearchNavigateButton = Pair(
                        state.value.searchUiState.currentResultIndex > 0,
                        state.value.searchUiState.currentResultIndex < nodeFindList.value.size - 1
                    ),
                    searchUiState = state.value.searchUiState,
                    onExitSearch = { viewModel.onExitSearchMode() },
                    canUndoDelete = state.value.canUndoDelete,
                    onBarAction = { action ->
                        when (action) {
                            Backpress -> viewModel.upOrClose(
                                fileAccessibilityChecker = createDesktopFileChecker(),
                                onOpenRecent = { recent ->
                                    val path = MindMapUri.parse(recent.uriString).path ?: recent.uriString
                                    openFileFromDisk(File(path))
                                },
                                onNewMindmap = {
                                    viewModel.onNewMindmapRequested(R.getString(R.string.default_root_node_title))
                                },
                                onBrowse = { pickAndOpenFile() },
                                onOpenDemo = { viewModel.onHelpDemoRequested { openDemoMap() } },
                                onExitApp = onExitApplication
                            )
                            SearchNext -> viewModel.searchNext()
                            SearchPrevious -> viewModel.searchPrevious()
                            NewMindmap -> viewModel.onNewMindmapRequested(R.getString(R.string.default_root_node_title))
                            Open -> pickAndOpenFile()
                            Help -> viewModel.onHelpDemoRequested { openDemoMap() }
                            Save -> {
                                val tempDir = System.getProperty("java.io.tmpdir") ?: "."
                                viewModel.launchSaveFile(tempDir)
                            }
                            Undo -> viewModel.onUndoDelete()
                            ExitSearch -> viewModel.onExitSearchMode()
                            AppTopBarAction.RecentFiles -> viewModel.showRecentFilesDialog(
                                fileAccessibilityChecker = createDesktopFileChecker(),
                                onOpenRecent = { recent ->
                                    val path = MindMapUri.parse(recent.uriString).path ?: recent.uriString
                                    openFileFromDisk(File(path))
                                },
                                onNewMindmap = {
                                    viewModel.onNewMindmapRequested(R.getString(R.string.default_root_node_title))
                                },
                                onBrowse = { pickAndOpenFile() },
                                onOpenDemo = { viewModel.onHelpDemoRequested { openDemoMap() } },
                                onExitApp = onExitApplication
                            )
                            CheckForUpdates -> triggerUpdateCheck(isManual = true)
                        }
                    },
                )
            },
            bottomBar = {
                if (showBottomToolbar && selectedNodeForToolbar != null) {
                    val node = viewModel.getNodeByID(selectedNodeForToolbar!!)
                    if (node != null) {
                        NodeBottomToolbar(
                            node = node,
                            onDismiss = {
                                showBottomToolbar = false
                                selectedNodeForToolbar = null
                            },
                            onAction = { action ->
                                when (action) {
                                    PROPERTIES -> viewModel.onNodeContextMenuClick(Properties(node = node))
                                    OPEN_LINK -> viewModel.onNodeContextMenuClick(OpenLink(node = node))
                                    ADD_CHILD -> viewModel.onNodeContextMenuClick(ContextMenuAction.AddChildNode(parentNode = node))
                                    DELETE -> viewModel.onDeleteNode(node)
                                }
                            }
                        )
                    }
                }
            },
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState)
            },
            floatingActionButton = {
                AppFloatingActionButton(
                    onClick = {
                        val parentNode = state.value.selectedNodeId?.let { selectedNodeId ->
                            viewModel.getNodeByID(selectedNodeId)
                                ?: viewModel.getRootNode()
                                ?: state.value.nodeCurrentlyDisplayed
                        } ?: state.value.nodeCurrentlyDisplayed

                        parentNode?.let { parent ->
                            val timestamp = System.currentTimeMillis()
                            val newNode = Node(
                                parentNode = parent,
                                id = "temp_$timestamp",
                                numericId = 0,
                                text = "",
                                creationDate = timestamp,
                                modificationDate = timestamp
                            )
                            viewModel.setDialogState(
                                NodePropertiesInspector(
                                    node = newNode,
                                    initialTab = InspectorTab.CONTENT,
                                    parentNode = parent,
                                    isNewNode = true,
                                )
                            )
                        }
                    },
                    iconContentDsc = "Add",
                    icon = Filled.Add
                )
            },
            content = { innerPadding ->
                val rootNode = viewModel.getRootNode() ?: state.value.nodeCurrentlyDisplayed
                rootNode?.let { node ->
                    MindMapCanvasScreen(
                        rootNode = node,
                        selectedNodeId = state.value.selectedNodeId,
                        collapsedNodeIds = state.value.collapsedNodeIds,
                        treeVersion = state.value.treeVersion,
                        onNodeSelect = { selected ->
                            viewModel.selectNode(selected)
                            showBottomToolbar = false
                            selectedNodeForToolbar = null
                        },
                        onNodeToggleCollapse = viewModel::toggleNodeCollapse,
                        onNodeDoubleClick = { clickedNode ->
                            viewModel.onNodeContextMenuClick(Properties(clickedNode))
                        },
                        onNodeLongPress = { pressedNode ->
                            selectedNodeForToolbar = pressedNode.id
                            showBottomToolbar = true
                        },
                        onMoveNode = viewModel::onMoveNode,
                        fetchText = viewModel::getNodeText,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }
            }
        )
    }
}

@Composable
private fun DesktopDialogs(
    state: State<MainUiState>,
    viewModel: MainViewModel,
) {
    when (val dialog = state.value.dialogUiState.dialogType) {
        None -> {}
        is AddChildNode -> {
            CustomDialog(
                titre = stringResource(R.string.add_child),
                value = "",
                onDismiss = { viewModel.setDialogState(None) }
            ) { newValue ->
                viewModel.addNode(newValue, dialog.parentNode)
            }
        }
        is DeleteConfirmation -> DeleteConfirmationDialog(confirmation = dialog)
        is ExitConfirmation -> ExitConfirmationDialog(confirmation = dialog)
        is DiscardConfirmation -> DiscardConfirmationDialog(confirmation = dialog)
        is OverwriteConfirmation -> OverwriteConfirmationDialog(confirmation = dialog)
        is StartupChooser -> StartupChooserDialog(chooser = dialog)
        is RecentFiles -> RecentFilesDialog(recentFilesDialog = dialog)
        is NodePropertiesInspector -> {
            NodePropertiesInspector(
                node = dialog.node,
                initialTab = dialog.initialTab,
                parentNode = dialog.parentNode,
                isNewNode = dialog.isNewNode,
                onSave = { updatedNode -> viewModel.onSaveNodeProperties(updatedNode) },
                onAddNode = { newNode, parentNode -> viewModel.onAddNodeFromInspector(newNode, parentNode) },
                onDismiss = { viewModel.onDismissNodeInspector() },
                onMoveCloudToParent = { targetNode -> viewModel.onMoveCloudToParent(targetNode) }
            )
        }
        is UpdateAvailable -> {
            UpdateAvailableDialog(
                currentVersion = dialog.currentVersion,
                release = dialog.release,
                onDownloadAndInstall = dialog.onDownloadAndInstall,
                onViewOnGitHub = dialog.onViewOnGitHub,
                onDismiss = dialog.onDismiss
            )
        }
    }
}

@Preview
@Composable
fun PreviewDesktopDroidplaneApp() {
    ContrastAwareReplyTheme {
        Surface {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(text = stringResource(R.string.app_name))
            }
        }
    }
}
