package fr.julien.quievreux.droidplane2

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_EDIT
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
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
import fr.julien.quievreux.droidplane2.core.extensions.buildCreateDocumentIntent
import fr.julien.quievreux.droidplane2.core.extensions.deleteSafDocument
import fr.julien.quievreux.droidplane2.core.extensions.getSaveFileLauncher
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.ui.component.AppFloatingActionButton
import fr.julien.quievreux.droidplane2.core.ui.component.CustomDialog
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.RecentFilesRepositoryImpl
import fr.julien.quievreux.droidplane2.data.createStorageFileChecker
import fr.julien.quievreux.droidplane2.data.getFileNameFromUri
import fr.julien.quievreux.droidplane2.data.model.MindMapUri
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.helper.SaveResolution
import fr.julien.quievreux.droidplane2.helper.SaveTargetResolver
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
import fr.julien.quievreux.droidplane2.res.AppRes
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
import fr.julien.quievreux.droidplane2.ui.theme.primaryContainerLight
import fr.julien.quievreux.droidplane2.ui.theme.primaryLight
import fr.julien.quievreux.droidplane2.ui.view.DeleteConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.DiscardConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.ExitConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.OverwriteConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.RecentFilesDialog
import fr.julien.quievreux.droidplane2.ui.view.RichTextViewActivity
import fr.julien.quievreux.droidplane2.ui.view.StartupChooserDialog
import fr.julien.quievreux.droidplane2.update.ApkDownloadHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.File
import java.io.InputStream

/**
 * The MainActivity can be started from the App Launcher, or with a File Open intent. If the MainApplication was
 * already running, the previously used document is re-used. Also, most of the information about the mind map and the
 * currently opened views is stored in the MainApplication. This enables the MainActivity to resume wherever it was
 * before it got restarted. A restart can happen when the screen is rotated, and we want to continue wherever we were
 * before the screen rotate.
 */
class MainActivity : FragmentActivity() {

    private val viewModel: MainViewModel by viewModel()
    private val logger by inject<Logger>()
    private val recentFilesRepository: RecentFilesRepository by lazy {
        RecentFilesRepositoryImpl(getSharedPreferences("droidplane_recent_files", MODE_PRIVATE))
    }

    private var showBottomToolbar by mutableStateOf(false)
    private var selectedNodeForToolbar: String? = null

    private val openFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { u ->
            try {
                contentResolver.takePersistableUriPermission(
                    u,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
                try {
                    contentResolver.takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Exception) {
                }
            }
            openSelectedUri(u)
        }
    }

    private val saveFileLauncher = getSaveFileLauncher(
        actionOnResultOk = { uri ->
            try {
                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
            } catch (_: Exception) {
            }

            val pickerDisplayName = getFileNameFromUri(this@MainActivity, uri)
            val originalUri = viewModel.getMapUri()
            val resolution = SaveTargetResolver.resolve(
                originalUriString = originalUri?.toString(),
                originalDisplayName = viewModel.getMindmapFileName(),
                pickedUriString = uri.toString(),
                pickedDisplayName = pickerDisplayName,
            )

            when (resolution) {
                is SaveResolution.WriteNewFile -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        writeMindMapToUri(resolution.targetUriString.toUri(), resolution.displayName)
                    }
                }

                is SaveResolution.DirectOverwrite -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        writeMindMapToUri(resolution.targetUriString.toUri(), resolution.displayName)
                    }
                }

                is SaveResolution.ConfirmOverwrite -> {
                    val targetOriginalUri = originalUri?.toString()?.toUri() ?: resolution.originalUriString.toUri()
                    val duplicateUri = resolution.duplicatePickerUriString.toUri()
                    viewModel.showOverwriteConfirmation(
                        fileName = resolution.originalDisplayName,
                        onConfirm = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                val wroteOriginal = writeMindMapToUri(
                                    targetOriginalUri,
                                    resolution.originalDisplayName
                                )
                                if (wroteOriginal) {
                                    deleteSafDocument(this@MainActivity, duplicateUri)
                                } else {
                                    writeMindMapToUri(duplicateUri, resolution.duplicateDisplayName)
                                }
                            }
                        },
                        onCancel = {
                            lifecycleScope.launch(Dispatchers.IO) {
                                deleteSafDocument(this@MainActivity, duplicateUri)
                            }
                            viewModel.cancelSaveFile()
                        }
                    )
                }
            }
        }
    )

    private fun writeMindMapToUri(targetUri: Uri, displayName: String): Boolean {
        return try {
            val stream = try {
                contentResolver.openOutputStream(targetUri, "wt")
            } catch (_: Exception) {
                contentResolver.openOutputStream(targetUri)
            }
            if (stream != null) {
                stream.use { outputStream ->
                    viewModel.saveFile(outputStream)
                }
                viewModel.setMapUri(MindMapUri.parse(targetUri.toString()), displayName)
                recentFilesRepository.addRecentFile(targetUri.toString(), displayName)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            logger.e("Failed to write mindmap to $targetUri: $e")
            false
        }
    }

    private fun openSelectedUri(uri: Uri) {
        val fileName = getFileNameFromUri(this, uri)
        recentFilesRepository.addRecentFile(uri.toString(), fileName)
        viewModel.setMapUri(MindMapUri.parse(uri.toString()), fileName)
        lifecycleScope.launch {
            try {
                val stream = contentResolver.openInputStream(uri)
                if (stream != null) {
                    viewModel.loadMindMap(stream)
                } else {
                    logger.e("Failed to open stream for $uri")
                }
            } catch (e: Exception) {
                logger.e("Error opening file: $e")
            }
        }
    }

    private fun triggerUpdateCheck(isManual: Boolean) {
        val packageInfo = try {
            packageManager.getPackageInfo(packageName, 0)
        } catch (_: Exception) {
            null
        }
        val currentVersion = packageInfo?.versionName.orEmpty()

        viewModel.checkForUpdates(
            currentVersion = currentVersion,
            isManual = isManual,
            onDownloadAndInstall = { apkUrl, fileName ->
                try {
                    ApkDownloadHelper.enqueueDownload(
                        context = this@MainActivity,
                        apkUrl = apkUrl,
                        fileName = fileName,
                        title = getString(R.string.app_name)
                    )
                    viewModel.showSnackbarMessage(getString(R.string.update_downloading_enqueued))
                } catch (_: Exception) {
                    val browserIntent = Intent(Intent.ACTION_VIEW, apkUrl.toUri())
                    startActivity(browserIntent)
                }
            },
            onViewOnGitHub = { url ->
                val browserIntent = Intent(Intent.ACTION_VIEW, url.toUri())
                startActivity(browserIntent)
            },
            upToDateMessage = getString(R.string.update_up_to_date, currentVersion),
            errorMessage = getString(R.string.update_error)
        )
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            triggerUpdateCheck(isManual = false)
        }
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                scrim = primaryLight.toArgb(),
                darkScrim = primaryContainerLight.toArgb()
            )
        )
        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            ContrastAwareReplyTheme {
                val state = viewModel.uiState.collectAsState()
                val nodeFindList = viewModel.getSearchResultFlow().collectAsState()

                LaunchedEffect(state.value.leaving) {
                    onLeaving(state)
                }

                LaunchedEffect(state.value.pendingSaveTempFile) {
                    state.value.pendingSaveTempFile?.let { file ->
                        viewModel.clearPendingSaveTempFile()
                        launchFileSavingIntent(file.name, viewModel.getMapUri()?.toString()?.toUri())
                    }
                }

                BackHandler(true) {
                    onBackPress()
                }

                LaunchedEffect(state.value.error) {
                    onError(state, scope, snackbarHostState)
                }

                LaunchedEffect(state.value.viewIntentNode) {
                    onViewIntent(
                        state = state,
                        scope = scope,
                        snackbarHostState = snackbarHostState,
                    )
                }

                LaunchedEffect(state.value.snackbarMessage?.eventId) {
                    onSnackBarEvent(state, scope, snackbarHostState)
                }

                OnDialogEvent(state)

                Scaffold(
                    modifier = Modifier,
                    topBar = {
                        AppTopBar(
                            text = state.value.title.ifEmpty { stringResource(R.string.app_name) },
                            hasBackIcon = state.value.canGoBack,
                            onQuery = { query ->
                                viewModel.search(query)
                            },
                            hasSearchNavigateButton = Pair(
                                state.value.searchUiState.currentResultIndex > 0,
                                state.value.searchUiState.currentResultIndex < nodeFindList.value.size - 1
                            ),
                            searchUiState = state.value.searchUiState,
                            onExitSearch = { viewModel.onExitSearchMode() },
                            canUndoDelete = state.value.canUndoDelete,
                            onBarAction = { action ->
                                onBarAction(action)
                            },
                        )
                    },
                    bottomBar = {
                        BottomBar()
                    },
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    floatingActionButton = {
                        FloatingActionButton(state)
                    },
                    content = { innerPadding ->
                        val rootNode = viewModel.getRootNode() ?: state.value.nodeCurrentlyDisplayed
                        rootNode?.let { node ->
                            MindMapCanvasScreen(
                                rootNode = node,
                                selectedNodeId = state.value.selectedNodeId,
                                collapsedNodeIds = state.value.collapsedNodeIds,
                                treeVersion = state.value.treeVersion,
                                onNodeSelect = { node ->
                                    viewModel.selectNode(node)
                                    showBottomToolbar = false
                                    selectedNodeForToolbar = null
                                },
                                onNodeToggleCollapse = viewModel::toggleNodeCollapse,
                                onNodeDoubleClick = { clickedNode ->
                                    viewModel.onNodeContextMenuClick(ContextMenuAction.Properties(clickedNode))
                                },
                                onNodeLongPress = { node ->
                                    selectedNodeForToolbar = node.id
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

        viewModel.apply {
            if (savedInstanceState == null && getRootNode() == null) {
                if (isAnExternalMindMapEdit()) {
                    openMindMapFromIntent()
                } else {
                    openStartupChooser()
                }
            }
        }
    }

    private fun onSnackBarEvent(state: State<MainUiState>, scope: CoroutineScope, snackbarHostState: SnackbarHostState) {
        state.value.snackbarMessage?.let { snackbarMsg ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = snackbarMsg.message,
                    actionLabel = snackbarMsg.actionLabel?.let { AppRes.getString(it) },
                    duration = SnackbarDuration.Short,
                )
                when (result) {
                    ActionPerformed -> {
                        snackbarMsg.onAction?.invoke()
                    }

                    Dismissed -> {}
                }
                viewModel.clearSnackbarMessage()
            }
        }
    }

    private fun onLeaving(state: State<MainUiState>) {
        if (state.value.leaving) {
            if (viewModel.hasUnsavedChangesState && state.value.dialogUiState.dialogType is None) {
                viewModel.resetLeaving()
                viewModel.setDialogState(
                    ExitConfirmation(
                        onConfirm = {
                            viewModel.resetLeaving()
                            finish()
                        },
                        onCancel = {
                            viewModel.resetLeaving()
                            viewModel.setDialogState(None)
                        }
                    )
                )
            } else if (!viewModel.hasUnsavedChangesState) {
                viewModel.resetLeaving()
                finish()
            }
        }
    }

    private fun onBarAction(action: AppTopBarAction) {
        when (action) {
            Backpress -> viewModel.upOrClose(
                fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
                onOpenRecent = { recent ->
                    openSelectedUri(recent.uriString.toUri())
                },
                onNewMindmap = {
                    viewModel.onNewMindmapRequested(getString(R.string.default_root_node_title))
                },
                onBrowse = {
                    openFileLauncher.launch(arrayOf("*/*"))
                },
                onOpenDemo = {
                    viewModel.onHelpDemoRequested {
                        viewModel.setMapUri(MindMapUri.parse("android.resource://$packageName/raw/example.mm"))
                        lifecycleScope.launch {
                            resources.openRawResource(R.raw.example).let { viewModel.loadMindMap(it) }
                        }
                    }
                },
                onExitApp = {
                    finish()
                }
            )

            SearchNext -> viewModel.searchNext()

            SearchPrevious -> viewModel.searchPrevious()

            NewMindmap -> {
                viewModel.onNewMindmapRequested(getString(R.string.default_root_node_title))
            }

            Open -> openFileLauncher.launch(arrayOf("*/*"))

            Help -> {
                viewModel.onHelpDemoRequested {
                    viewModel.setMapUri(MindMapUri.parse("android.resource://$packageName/raw/example.mm"))
                    lifecycleScope.launch {
                        resources.openRawResource(R.raw.example).let { viewModel.loadMindMap(it) }
                    }
                }
            }

            Save -> viewModel.launchSaveFile(filesDir.path)

            Undo -> viewModel.onUndoDelete()

            ExitSearch -> viewModel.onExitSearchMode()

            AppTopBarAction.RecentFiles -> viewModel.showRecentFilesDialog(
                fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
                onOpenRecent = { recent ->
                    openSelectedUri(recent.uriString.toUri())
                },
                onNewMindmap = {
                    viewModel.onNewMindmapRequested(getString(R.string.default_root_node_title))
                },
                onBrowse = {
                    openFileLauncher.launch(arrayOf("*/*"))
                },
                onOpenDemo = {
                    viewModel.onHelpDemoRequested {
                        viewModel.setMapUri(MindMapUri.parse("android.resource://$packageName/raw/example.mm"))
                        lifecycleScope.launch {
                            resources.openRawResource(R.raw.example).let { viewModel.loadMindMap(it) }
                        }
                    }
                },
                onExitApp = {
                    finish()
                }
            )

            CheckForUpdates -> triggerUpdateCheck(isManual = true)
        }
    }

    private fun onBackPress() {
        viewModel.upOrClose(
            fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
            onOpenRecent = { recent ->
                openSelectedUri(recent.uriString.toUri())
            },
            onNewMindmap = {
                viewModel.onNewMindmapRequested(getString(R.string.default_root_node_title))
            },
            onBrowse = {
                openFileLauncher.launch(arrayOf("*/*"))
            },
            onOpenDemo = {
                viewModel.onHelpDemoRequested {
                    viewModel.setMapUri(MindMapUri.parse("android.resource://$packageName/raw/example.mm"))
                    lifecycleScope.launch {
                        resources.openRawResource(R.raw.example).let { viewModel.loadMindMap(it) }
                    }
                }
            },
            onExitApp = {
                finish()
            }
        )
    }

    @Composable
    private fun BottomBar() {
        if (showBottomToolbar && selectedNodeForToolbar != null) {
            val nodeId = selectedNodeForToolbar!!
            val node = viewModel.getNodeByID(nodeId)
            if (node != null) {
                NodeBottomToolbar(
                    node = node,
                    onDismiss = { showBottomToolbar = false; selectedNodeForToolbar = null },
                    onAction = { action ->
                        when (action) {
                            PROPERTIES -> {
                                viewModel.onNodeContextMenuClick(
                                    Properties(node = node)
                                )
                            }

                            OPEN_LINK -> {
                                viewModel.onNodeContextMenuClick(
                                    OpenLink(node = node)
                                )
                            }

                            ADD_CHILD -> {
                                viewModel.onNodeContextMenuClick(
                                    ContextMenuAction.AddChildNode(parentNode = node)
                                )
                            }

                            DELETE -> {
                                viewModel.onDeleteNode(node)
                            }
                        }
                    }
                )
            }
        }
    }

    @Composable
    private fun FloatingActionButton(state: State<MainUiState>) {
        AppFloatingActionButton(
            onClick = {
                getTargetParent(state)?.let { parentNode ->
                    val timestamp = System.currentTimeMillis()
                    val newNode = Node(
                        parentNode = parentNode,
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
                            parentNode = parentNode,
                            isNewNode = true,
                        )
                    )
                }
            },
            iconContentDsc = "Add",
            icon = Filled.Add
        )
    }

    private fun getTargetParent(state: State<MainUiState>): Node? = state.value.selectedNodeId?.let { selectedNodeId ->
        viewModel.getNodeByID(selectedNodeId)
            ?: viewModel.getRootNode()
            ?: state.value.nodeCurrentlyDisplayed
    } ?: run {
        state.value.nodeCurrentlyDisplayed
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isAnExternalMindMapEdit()) {
            val uri = intent.data
            val fileName = uri?.let { getFileNameFromUri(this@MainActivity, it) }
            viewModel.setMapUri(uri?.let { MindMapUri.parse(it.toString()) }, fileName)
            if (uri != null && fileName != null) {
                recentFilesRepository.addRecentFile(uri.toString(), fileName)
            }
            lifecycleScope.launch {
                getDocumentInputStream()?.let { viewModel.loadMindMap(it) }
            }
        }
    }

    @Composable
    private fun OnDialogEvent(state: State<MainUiState>) {
        when (val dialog = state.value.dialogUiState.dialogType) {
            None -> {}
            is AddChildNode -> {
                CustomDialog(
                    titre = stringResource(R.string.add_child),
                    value = "",
                    onDismiss = {
                        viewModel.setDialogState(None)
                    }
                ) { newValue ->
                    viewModel.addNode(newValue, dialog.parentNode)
                }
            }

            is DeleteConfirmation -> {
                DeleteConfirmationDialog(confirmation = dialog)
            }

            is ExitConfirmation -> {
                ExitConfirmationDialog(
                    confirmation = dialog,
                )
            }

            is DiscardConfirmation -> {
                DiscardConfirmationDialog(
                    confirmation = dialog,
                )
            }

            is OverwriteConfirmation -> {
                OverwriteConfirmationDialog(
                    confirmation = dialog,
                )
            }

            is StartupChooser -> {
                StartupChooserDialog(
                    chooser = dialog,
                )
            }

            is RecentFiles -> {
                RecentFilesDialog(
                    recentFilesDialog = dialog,
                )
            }

            is NodePropertiesInspector -> {
                NodePropertiesInspector(
                    node = dialog.node,
                    initialTab = dialog.initialTab,
                    parentNode = dialog.parentNode,
                    isNewNode = dialog.isNewNode,
                    onSave = { updatedNode ->
                        viewModel.onSaveNodeProperties(updatedNode)
                    },
                    onAddNode = { newNode, parentNode ->
                        viewModel.onAddNodeFromInspector(newNode, parentNode)
                    },
                    onDismiss = {
                        viewModel.onDismissNodeInspector()
                    },
                    onMoveCloudToParent = { targetNode ->
                        viewModel.onMoveCloudToParent(targetNode)
                    }
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

            is None -> {}
        }
    }

    private fun MainViewModel.openStartupChooser() {
        createEmptyMindmap(getString(R.string.default_root_node_title))

        showStartupChooser(
            fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
            onOpenRecent = { recent ->
                openSelectedUri(recent.uriString.toUri())
            },
            onBrowse = {
                openFileLauncher.launch(arrayOf("*/*"))
            },
            onOpenDemo = {
                setMapUri(MindMapUri.parse("android.resource://$packageName/raw/example.mm"))
                lifecycleScope.launch {
                    resources.openRawResource(R.raw.example).let { loadMindMap(it) }
                }
            }
        )
    }

    private fun MainViewModel.openMindMapFromIntent() {
        val uri = intent.data
        val fileName = uri?.let { getFileNameFromUri(this@MainActivity, it) }
        setMapUri(uri?.let { MindMapUri.parse(it.toString()) }, fileName)
        if (uri != null && fileName != null) {
            recentFilesRepository.addRecentFile(uri.toString(), fileName)
        }
        lifecycleScope.launch {
            getDocumentInputStream()?.let { loadMindMap(it) }
        }
    }

    private fun onViewIntent(
        state: State<MainUiState>,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
    ) {
        state.value.viewIntentNode?.let { viewIntentNode ->
            when (state.value.contentNodeType) {
                RichText -> {
                    openRichText(
                        node = viewIntentNode.node,
                        activity = this@MainActivity
                    )
                }

                RelativeFile -> {
                    try {
                        val dataUri = viewIntentNode.uriString?.toUri()
                        val launchIntent = if (dataUri?.scheme == "file" && dataUri.path != null) {
                            val contentUri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "$packageName.fileprovider",
                                File(dataUri.path!!)
                            )
                            Intent(ACTION_VIEW).apply {
                                setDataAndType(contentUri, viewIntentNode.mimeType)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        } else {
                            Intent(ACTION_VIEW).apply {
                                if (dataUri != null) {
                                    setDataAndType(dataUri, viewIntentNode.mimeType)
                                }
                            }
                        }
                        startActivity(launchIntent)
                    } catch (e1: Exception) {
                        showError(
                            message = "No application found to open ${viewIntentNode.node.link}",
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                        )
                        e1.printStackTrace()
                    }
                }

                Classic -> {
                    try {
                        val dataUri = viewIntentNode.uriString?.toUri()
                        startActivity(Intent(ACTION_VIEW, dataUri))
                    } catch (e: ActivityNotFoundException) {
                        showError(
                            message = "ActivityNotFoundException when opening link as normal intent",
                            scope = scope,
                            snackbarHostState = snackbarHostState,
                        )
                        viewModel.openRelativeFile(viewIntentNode.node)
                    }
                }
            }
        }
    }

    private fun onError(
        state: State<MainUiState>,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
    ) {
        state.value.error.let { message ->
            if (message.isNotEmpty()) {
                state.value.errorAction?.let { errorAction ->
                    scope.launch {
                        val result = snackbarHostState
                            .showSnackbar(
                                message = message,
                                actionLabel = AppRes.getString(errorAction.actionLabel),
                                duration = Indefinite
                            )
                        when (result) {
                            ActionPerformed -> {
                                errorAction.action()
                            }

                            Dismissed -> {
                            }
                        }
                    }
                } ?: run {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = message,
                            duration = Indefinite
                        )
                    }
                }
            }
        }
    }

    private fun showError(
        message: String,
        scope: CoroutineScope,
        snackbarHostState: SnackbarHostState,
    ) {
        scope.launch {
            snackbarHostState.showSnackbar(message)
        }
    }

    private fun getDocumentInputStream(): InputStream? = intent.data?.let { contentResolver.openInputStream(it) }

    private fun isAnExternalMindMapEdit(): Boolean =
        ACTION_EDIT == intent.action || ACTION_VIEW == intent.action || ACTION_OPEN_DOCUMENT == intent.action

    private fun openRichText(
        node: Node,
        activity: FragmentActivity,
    ) {
        if (node.richTextContents.isNotEmpty()) {
            val richTextContent = node.richTextContents.first()
            val intent = Intent(activity, RichTextViewActivity::class.java)
            intent.putExtra("richTextContent", richTextContent)
            activity.startActivity(intent)
        }
    }

    private fun launchFileSavingIntent(filename: String, initialUri: Uri? = null) {
        val intent = buildCreateDocumentIntent(filename, initialUri)
        saveFileLauncher.launch(intent)
    }

    override fun onKeyDown(keyCode: Int, event: android.view.KeyEvent): Boolean {
        if (keyCode == android.view.KeyEvent.KEYCODE_Z && event.isCtrlPressed) {
            if (viewModel.canUndoDelete) {
                viewModel.onUndoDelete()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
}
