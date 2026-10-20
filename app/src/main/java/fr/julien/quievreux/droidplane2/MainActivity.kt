package fr.julien.quievreux.droidplane2

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration.Indefinite
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.SnackbarResult.Dismissed
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import fr.julien.quievreux.droidplane2.MainUiState.DialogType
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.AddChildNode
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.None
import fr.julien.quievreux.droidplane2.ui.view.DeleteConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.DiscardConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.ExitConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.OverwriteConfirmationDialog
import fr.julien.quievreux.droidplane2.ui.view.RecentFilesDialog
import fr.julien.quievreux.droidplane2.ui.view.StartupChooserDialog
import fr.julien.quievreux.droidplane2.data.RecentFilesRepository
import fr.julien.quievreux.droidplane2.data.RecentFilesRepositoryImpl
import fr.julien.quievreux.droidplane2.data.createStorageFileChecker
import fr.julien.quievreux.droidplane2.data.getFileNameFromUri
import fr.julien.quievreux.droidplane2.core.extensions.buildCreateDocumentIntent
import fr.julien.quievreux.droidplane2.core.extensions.deleteSafDocument
import fr.julien.quievreux.droidplane2.core.extensions.getSaveFileLauncher
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.ui.component.AppFloatingActionButton
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.helper.SaveResolution
import fr.julien.quievreux.droidplane2.helper.SaveTargetResolver
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RelativeFile
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RichText
import fr.julien.quievreux.droidplane2.model.ContextMenuAction
import fr.julien.quievreux.droidplane2.ui.components.AppTopBar
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Backpress
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.ExitSearch
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Help
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.NewMindmap
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Open
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.RecentFiles
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.CheckForUpdates
import fr.julien.quievreux.droidplane2.ui.dialog.UpdateAvailableDialog
import fr.julien.quievreux.droidplane2.update.ApkDownloadHelper
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Save
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchNext
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchPrevious
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Undo
import fr.julien.quievreux.droidplane2.core.ui.component.CustomDialog
import fr.julien.quievreux.droidplane2.ui.inspector.NodePropertiesInspector
import fr.julien.quievreux.droidplane2.ui.mindmap.MindMapCanvasScreen
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.ui.theme.primaryContainerLight
import fr.julien.quievreux.droidplane2.ui.theme.primaryLight
import fr.julien.quievreux.droidplane2.ui.view.RichTextViewActivity
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

    private val clipboardManager by lazy {
        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

    private val recentFilesRepository: RecentFilesRepository by lazy {
        RecentFilesRepositoryImpl(getSharedPreferences("droidplane_recent_files", MODE_PRIVATE))
    }

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
                } catch (_: Exception) {}
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
            } catch (_: Exception) {}

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
                        writeMindMapToUri(Uri.parse(resolution.targetUriString), resolution.displayName)
                    }
                }
                is SaveResolution.DirectOverwrite -> {
                    lifecycleScope.launch(Dispatchers.IO) {
                        writeMindMapToUri(Uri.parse(resolution.targetUriString), resolution.displayName)
                    }
                }
                is SaveResolution.ConfirmOverwrite -> {
                    val targetOriginalUri = originalUri ?: Uri.parse(resolution.originalUriString)
                    val duplicateUri = Uri.parse(resolution.duplicatePickerUriString)
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
                viewModel.setMapUri(targetUri, displayName)
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
        viewModel.setMapUri(uri, fileName)
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
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl))
                    startActivity(browserIntent)
                }
            },
            onViewOnGitHub = { url ->
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
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
                    if (state.value.leaving) {
                        if (viewModel.hasUnsavedChangesState && state.value.dialogUiState.dialogType is DialogType.None) {
                            viewModel.resetLeaving()
                            viewModel.setDialogState(
                                DialogType.ExitConfirmation(
                                    onConfirm = {
                                        viewModel.resetLeaving()
                                        finish()
                                    },
                                    onCancel = {
                                        viewModel.resetLeaving()
                                        viewModel.setDialogState(DialogType.None)
                                    }
                                )
                            )
                        } else if (!viewModel.hasUnsavedChangesState) {
                            viewModel.resetLeaving()
                            finish()
                        }
                    }
                }

                LaunchedEffect(state.value.pendingSaveTempFile) {
                    state.value.pendingSaveTempFile?.let { file ->
                        viewModel.clearPendingSaveTempFile()
                        launchFileSavingIntent(file.name, viewModel.getMapUri())
                    }
                }

                BackHandler(true) {
                    viewModel.upOrClose(
                        fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
                        onOpenRecent = { recent ->
                            openSelectedUri(Uri.parse(recent.uriString))
                        },
                        onNewMindmap = {
                            viewModel.onNewMindmapRequested(getString(R.string.default_root_node_title))
                        },
                        onBrowse = {
                            openFileLauncher.launch(arrayOf("*/*"))
                        },
                        onOpenDemo = {
                            viewModel.onHelpDemoRequested {
                                viewModel.setMapUri(Uri.parse("android.resource://$packageName/raw/example.mm"))
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
                    state.value.snackbarMessage?.let { snackbarMsg ->
                        scope.launch {
                            val result = snackbarHostState.showSnackbar(
                                message = snackbarMsg.message,
                                actionLabel = snackbarMsg.actionLabel?.let { getString(it) },
                                duration = androidx.compose.material3.SnackbarDuration.Short,
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
                    is DialogType.DeleteConfirmation -> {
                        DeleteConfirmationDialog(confirmation = dialog)
                    }
                    is DialogType.ExitConfirmation -> {
                        ExitConfirmationDialog(
                            confirmation = dialog,
                        )
                    }
                    is DialogType.DiscardConfirmation -> {
                        DiscardConfirmationDialog(
                            confirmation = dialog,
                        )
                    }
                    is DialogType.OverwriteConfirmation -> {
                        OverwriteConfirmationDialog(
                            confirmation = dialog,
                        )
                    }
                    is DialogType.StartupChooser -> {
                        StartupChooserDialog(
                            chooser = dialog,
                        )
                    }
                    is DialogType.RecentFiles -> {
                        RecentFilesDialog(
                            recentFilesDialog = dialog,
                        )
                    }
                    is DialogType.NodePropertiesInspector -> {
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
                    is DialogType.UpdateAvailable -> {
                        UpdateAvailableDialog(
                            currentVersion = dialog.currentVersion,
                            release = dialog.release,
                            onDownloadAndInstall = dialog.onDownloadAndInstall,
                            onViewOnGitHub = dialog.onViewOnGitHub,
                            onDismiss = dialog.onDismiss
                        )
                    }
                    is DialogType.None -> {}
                }

                Scaffold(
                    modifier = Modifier,
                    topBar = {
                        AppTopBar(
                            text = state.value.title.ifEmpty { stringResource(R.string.app_name) },
                            hasBackIcon = state.value.canGoBack,
                            onQuery = { query ->
                                viewModel.search(query)
                            },
                            hasSearchNavigateButton = Pair(state.value.searchUiState.currentResultIndex > 0, state.value.searchUiState.currentResultIndex < nodeFindList.value.size - 1),
                            searchUiState = state.value.searchUiState,
                            onExitSearch = { viewModel.onExitSearchMode() },
                            canUndoDelete = state.value.canUndoDelete,
                            onBarAction = { action ->
                                when (action) {
                                    Backpress -> viewModel.upOrClose(
                                        fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
                                        onOpenRecent = { recent ->
                                            openSelectedUri(Uri.parse(recent.uriString))
                                        },
                                        onNewMindmap = {
                                            viewModel.onNewMindmapRequested(getString(R.string.default_root_node_title))
                                        },
                                        onBrowse = {
                                            openFileLauncher.launch(arrayOf("*/*"))
                                        },
                                        onOpenDemo = {
                                            viewModel.onHelpDemoRequested {
                                                viewModel.setMapUri(Uri.parse("android.resource://$packageName/raw/example.mm"))
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
                                            viewModel.setMapUri(Uri.parse("android.resource://$packageName/raw/example.mm"))
                                            lifecycleScope.launch {
                                                resources.openRawResource(R.raw.example).let { viewModel.loadMindMap(it) }
                                            }
                                        }
                                    }

                                    Save -> viewModel.launchSaveFile(filesDir.path)

                                    Undo -> viewModel.onUndoDelete()

                                    ExitSearch -> viewModel.onExitSearchMode()

                                    RecentFiles -> viewModel.showRecentFilesDialog(
                                        fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
                                        onOpenRecent = { recent ->
                                            openSelectedUri(Uri.parse(recent.uriString))
                                        },
                                        onNewMindmap = {
                                            viewModel.onNewMindmapRequested(getString(R.string.default_root_node_title))
                                        },
                                        onBrowse = {
                                            openFileLauncher.launch(arrayOf("*/*"))
                                        },
                                        onOpenDemo = {
                                            viewModel.onHelpDemoRequested {
                                                viewModel.setMapUri(Uri.parse("android.resource://$packageName/raw/example.mm"))
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
                            },
                        )
                    },
                    //                    bottomBar = {},
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    floatingActionButton = {
                        AppFloatingActionButton(
                            onClick = {
                                val targetParent = if (state.value.selectedNodeId != null) {
                                    viewModel.getNodeByID(state.value.selectedNodeId!!)
                                        ?: viewModel.getRootNode()
                                        ?: state.value.nodeCurrentlyDisplayed
                                } else {
                                    state.value.nodeCurrentlyDisplayed
                                }
                                targetParent?.let { parentNode ->
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
                                        DialogType.NodePropertiesInspector(
                                            node = newNode,
                                            initialTab = InspectorTab.CONTENT,
                                            parentNode = parentNode,
                                            isNewNode = true,
                                        )
                                    )
                                }
                            },
                            iconContentDsc = "Add",
                            icon = Icons.Filled.Add
                        )
                    },
                    //                    floatingActionButtonPosition =,
                    //                    containerColor =,
                    //                    contentColor =,
                    //                    contentWindowInsets =,
                    content = { innerPadding ->
                        val rootNode = viewModel.getRootNode() ?: state.value.nodeCurrentlyDisplayed
                        rootNode?.let { node ->
                            MindMapCanvasScreen(
                                rootNode = node,
                                selectedNodeId = state.value.selectedNodeId,
                                collapsedNodeIds = state.value.collapsedNodeIds,
                                treeVersion = state.value.treeVersion,
                                onNodeSelect = viewModel::selectNode,
                                onNodeToggleCollapse = viewModel::toggleNodeCollapse,
                                onNodeDoubleClick = { clickedNode ->
                                    viewModel.onNodeContextMenuClick(ContextMenuAction.Properties(clickedNode))
                                },
                                onNodeContextMenuClick = viewModel::onNodeContextMenuClick,
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
                    val uri = intent.data
                    val fileName = uri?.let { getFileNameFromUri(this@MainActivity, it) }
                    setMapUri(uri, fileName)
                    if (uri != null && fileName != null) {
                        recentFilesRepository.addRecentFile(uri.toString(), fileName)
                    }
                    lifecycleScope.launch {
                        getDocumentInputStream(true)?.let { loadMindMap(it) }
                    }
                } else {
                    // Initialize an empty mindmap by default
                    createEmptyMindmap(getString(R.string.default_root_node_title))

                    // Query accessible recent files off the main thread and show Startup Chooser Dialog
                    showStartupChooser(
                        fileAccessibilityChecker = createStorageFileChecker(this@MainActivity),
                        onOpenRecent = { recent ->
                            openSelectedUri(Uri.parse(recent.uriString))
                        },
                        onBrowse = {
                            openFileLauncher.launch(arrayOf("*/*"))
                        },
                        onOpenDemo = {
                            setMapUri(Uri.parse("android.resource://$packageName/raw/example.mm"))
                            lifecycleScope.launch {
                                resources.openRawResource(R.raw.example).let { loadMindMap(it) }
                            }
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (isAnExternalMindMapEdit()) {
            val uri = intent.data
            val fileName = uri?.let { getFileNameFromUri(this@MainActivity, it) }
            viewModel.setMapUri(uri, fileName)
            if (uri != null && fileName != null) {
                recentFilesRepository.addRecentFile(uri.toString(), fileName)
            }
            lifecycleScope.launch {
                getDocumentInputStream(true)?.let { viewModel.loadMindMap(it) }
            }
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
                        val originalIntent = viewIntentNode.intent
                        val dataUri = originalIntent.data
                        val launchIntent = if (dataUri?.scheme == "file" && dataUri.path != null) {
                            val contentUri = FileProvider.getUriForFile(
                                this@MainActivity,
                                "$packageName.fileprovider",
                                File(dataUri.path!!)
                            )
                            Intent(originalIntent).apply {
                                setDataAndType(contentUri, originalIntent.type)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                        } else {
                            originalIntent
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
                        startActivity(viewIntentNode.intent)
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
                                actionLabel = getString(errorAction.actionLabel),
                                // Defaults to SnackbarDuration.Short
                                duration = Indefinite
                            )
                        when (result) {
                            ActionPerformed -> {
                                /* Handle snackbar action performed */
                                errorAction.action()
                            }

                            Dismissed -> {
                                /* Handle snackbar dismissed */
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

    private fun getDocumentInputStream(isAnExternalMindMapEdit: Boolean): InputStream? {
        return if (isAnExternalMindMapEdit) {
            intent.data?.let { contentResolver.openInputStream(it) }
        } else {
            resources.openRawResource(R.raw.example)
        }
    }

    // determine whether we are started from the EDIT or VIEW intent, or whether we are started from the
// launcher started from ACTION_EDIT/VIEW intent
    private fun isAnExternalMindMapEdit(): Boolean = ACTION_EDIT == intent.action || ACTION_VIEW == intent.action || ACTION_OPEN_DOCUMENT == intent.action

    private fun updateClipboard(text: String) {
        val clipData = ClipData.newPlainText("node", text)
        clipboardManager.setPrimaryClip(clipData)
    }

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
