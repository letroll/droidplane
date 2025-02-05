package fr.julien.quievreux.droidplane2

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.ACTION_EDIT
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.Intent.ACTION_VIEW
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.CreateNode
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.EditNodeDescription
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.None
import fr.julien.quievreux.droidplane2.core.PermissionUtils.checkStoragePermissions
import fr.julien.quievreux.droidplane2.core.PermissionUtils.requestForStoragePermissions
import fr.julien.quievreux.droidplane2.core.extensions.getOpenFileLauncher
import fr.julien.quievreux.droidplane2.core.extensions.getSaveFileLauncher
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.core.ui.component.AppFloatingActionButton
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.helper.FileRegister
import fr.julien.quievreux.droidplane2.model.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RelativeFile
import fr.julien.quievreux.droidplane2.model.ContentNodeType.RichText
import fr.julien.quievreux.droidplane2.ui.components.AppTopBar
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Backpress
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Help
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Open
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Save
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchNext
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchPrevious
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Top
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Up
import fr.julien.quievreux.droidplane2.core.ui.component.CustomDialog
import fr.julien.quievreux.droidplane2.ui.components.MindMap
import fr.julien.quievreux.droidplane2.ui.components.nodeList
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.ui.theme.primaryContainerLight
import fr.julien.quievreux.droidplane2.ui.theme.primaryLight
import fr.julien.quievreux.droidplane2.ui.view.RichTextViewActivity
import kotlinx.coroutines.CoroutineScope
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
class MainActivity : FragmentActivity(), FileRegister {

    private val viewModel: MainViewModel by viewModel()
    private val logger by inject<Logger>()
    private val useNewView = false

    private val clipboardManager by lazy {
        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

    private val openFileLauncher = getOpenFileLauncher()

    private val saveFileLauncher = getSaveFileLauncher(
        actionOnResultOk = { uri ->
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                viewModel.saveFile(outputStream)
            }
        }
    )

    @SuppressLint("RememberReturnType")
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.light(
                scrim = primaryLight.toArgb(),
                darkScrim = primaryContainerLight.toArgb()
            )
        )
        setContentView(R.layout.activity_main)
        setContent {
            val scope = rememberCoroutineScope()
            val snackbarHostState = remember { SnackbarHostState() }

            ContrastAwareReplyTheme {
                val state = viewModel.uiState.collectAsState()
                if (state.value.leaving) finish() //TODO confirm dialog
                val nodeFindList = viewModel.getSearchResultFlow().collectAsState()

                BackHandler(true) {
                    viewModel.upOrClose()
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

                when (val dialog = state.value.dialogUiState.dialogType) {
                    None -> {}
                    is EditNodeDescription -> {
                        CustomDialog(
                            titre = stringResource(R.string.edit),
                            value = dialog.oldValue,
                            onDismiss = {
                                viewModel.setDialogState(None)
                            }
                        ) { newValue ->
                            viewModel.updateNodeText(dialog.node, newValue)
                        }
                    }
                    CreateNode -> {
                        CustomDialog(
                            titre = stringResource(R.string.add_child),
                            value = "",
                            onDismiss = {
                                viewModel.setDialogState(None)
                            }
                        ) { newValue ->
                            viewModel.addNode(newValue)
                        }
                    }
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
                            hasSearchNavigateButton = Pair(state.value.searchUiState.currentSearchResultIndex > 0, state.value.searchUiState.currentSearchResultIndex < nodeFindList.value.size - 1),
                            onBarAction = { action ->
                                when (action) {
                                    Backpress -> viewModel.upOrClose()

                                    SearchNext -> viewModel.searchNext()

                                    SearchPrevious -> viewModel.searchPrevious()

                                    Up -> viewModel.up(false)

                                    Top -> viewModel.top()

                                    Open -> openFileLauncher.launch("*/*")

                                    Help -> showHelp()

                                    Save -> viewModel.launchSaveFile()
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
                                viewModel.setDialogState(CreateNode)
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
                        state.value.nodeCurrentlyDisplayed?.let { node ->
                            if (useNewView) {
                                MindMap(
                                    rootNode = node,
                                    fetchText = viewModel::getNodeText,
                                )
//                                MindMap2(
//                                    node = node,
//                                    modifier = Modifier
//                                        .fillMaxSize()
//                                        .background(MaterialTheme.colorScheme.surfaceContainer)
//                                        .padding(innerPadding),
//                                    fetchText = viewModel::getNodeText,
//                                    logger = logger,
//                                    viewModel = mindMapViewModel,
//                                )
//                                    { nodeClicked,mindMapView ->
//                                       createNode(nodeClicked)?.let {
//
//                            }
//                                    }
                            } else {
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.surfaceContainer)
                                        .padding(innerPadding),
                                ) {
                                    logger.e("list updated")
                                    val searchResultToShow = if (nodeFindList.value.isEmpty() || (state.value.searchUiState.currentSearchResultIndex in 0 until nodeFindList.value.size - 1)) {
                                        null
                                    } else {
                                        nodeFindList.value[state.value.searchUiState.currentSearchResultIndex]
                                    }
                                    nodeList(
                                        node = node,
                                        searchResultToShow = searchResultToShow,
                                        fetchText = viewModel::getNodeText,
                                        updateClipBoard = ::updateClipboard,
                                        onNodeClick = viewModel::onNodeClick,
                                        onNodeContextMenuClick = viewModel::onNodeContextMenuClick,
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        viewModel.apply {
            setFileRegister(this@MainActivity)
            if (isAnExternalMindMapEdit()) {
                setMapUri(intent.data)
            } else {
                setMapUri(Uri.parse("android.resource://$packageName/raw/example.mm"))
            }

            lifecycleScope.launch {
                logger.e("lifecycleScope")
                getDocumentInputStream(isAnExternalMindMapEdit())?.let { loadMindMap(it) }
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
                        startActivity(viewIntentNode.intent)
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

    private fun showHelp() {
        val helpIntent = Intent(this, MainActivity::class.java)
        helpIntent.putExtra(INTENT_START_HELP, true)
        startActivity(helpIntent)
    }

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

    private fun launchFileSavingIntent(filename: String) {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
//            type = "text/plain" // Set the MIME type
            type = "*/*" // Set the MIME type
            putExtra(Intent.EXTRA_TITLE, filename)
        }
        saveFileLauncher.launch(intent)
    }

    // Handle permission request result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            CREATE_FILE_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Permission granted, launch file saving intent
                    viewModel.getNameOfFileToSave()?.let { fileName ->
                        launchFileSavingIntent(fileName)
                    }

                } else {
                    // Permission denied, handle accordingly
                }
            }

            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }

    }

    override fun registerFile(file: File) {
        if (checkStoragePermissions(this)) {
            logger.e("Need write external storage permission")
            // Permission is not granted, request it
//            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(WRITE_EXTERNAL_STORAGE), CREATE_FILE_REQUEST_CODE)
            requestForStoragePermissions(
                this,
                logger,
            )
        } else {
            logger.e("Permission granted, proceed with file saving")
            // Permission is already granted, proceed with file saving
            launchFileSavingIntent(file.name)
        }
    }

    override fun getfilesDir(): String = filesDir.path

    companion object {
        const val INTENT_START_HELP: String = "fr.julien.quievreux.droidplane2.INTENT_START_HELP"
        const val CREATE_FILE_REQUEST_CODE = 1
    }
}
