package fr.julien.quievreux.droidplane2

import android.annotation.SuppressLint
import android.app.AlertDialog.Builder
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.ACTION_EDIT
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import fr.julien.quievreux.droidplane2.ContentNodeType.Classic
import fr.julien.quievreux.droidplane2.ContentNodeType.RelativeFile
import fr.julien.quievreux.droidplane2.ContentNodeType.RichText
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.Edit
import fr.julien.quievreux.droidplane2.MainUiState.DialogType.None
import fr.julien.quievreux.droidplane2.core.log.Logger
import fr.julien.quievreux.droidplane2.data.model.Node
import fr.julien.quievreux.droidplane2.ui.components.AppTopBar
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Backpress
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Help
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Open
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchNext
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchPrevious
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Top
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Up
import fr.julien.quievreux.droidplane2.ui.components.CustomDialog
import fr.julien.quievreux.droidplane2.ui.components.MindMap
import fr.julien.quievreux.droidplane2.ui.components.nodeList
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.ui.theme.primaryContainerLight
import fr.julien.quievreux.droidplane2.ui.theme.primaryLight
import fr.julien.quievreux.droidplane2.view.RichTextViewActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.io.FileNotFoundException
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
    private val useNewView = false

    private val clipboardManager by lazy {
        getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
    }

    private val launcher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val openFileIntent = Intent(this, MainActivity::class.java)
            openFileIntent.setData(uri)
            openFileIntent.setAction(ACTION_OPEN_DOCUMENT)
            startActivity(openFileIntent)
        }
    }

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

        viewModel.apply {
            if (isAnExternalMindMapEdit()) {
                viewModel.setMapUri(intent.data)
            }

            getDocumentInputStream(isAnExternalMindMapEdit())?.let { loadMindMap(it) }
        }
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
                    is Edit -> {
                        CustomDialog(
                            value = dialog.oldValue,
                            onDismiss = {
                                viewModel.setDialogState(None)
                            }
                        ) { newValue ->
                            viewModel.updateNodeText(dialog.node, newValue)
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

                                    Open -> openFile()

                                    Help -> showHelp()
                                }
                            },
                        )
                    },
                    //                    bottomBar = {},
                    snackbarHost = {
                        SnackbarHost(hostState = snackbarHostState)
                    },
                    //                    floatingActionButton = {},
                    //                    floatingActionButtonPosition =,
                    //                    containerColor =,
                    //                    contentColor =,
                    //                    contentWindowInsets =,
                    content = { innerPadding ->
                        state.value.rootNode?.let { node ->
                            if (useNewView) {
                                MindMap(
                                    rootNode = node,
                                    fetchText = viewModel::getNodeText,
                                )
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
                        snackbarHostState.showSnackbar(message)
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

    private fun openFile() {
        launcher.launch("*/*")
    }

    private fun getDocumentInputStream(isAnExternalMindMapEdit: Boolean): InputStream? {
        return if (isAnExternalMindMapEdit) {
            val uri = intent.data
            if (uri != null) {
                val cr = contentResolver
                try {
                    cr.openInputStream(uri)
                } catch (e: FileNotFoundException) {
                    abortWithPopup(R.string.filenotfound)
                    e.printStackTrace()
                    null
                }
            } else {
                abortWithPopup(R.string.novalidfile)
                null
            }
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

    /**
     * Shows a popup with an error message and then closes the application
     *
     * @param stringResourceId
     */
    private fun abortWithPopup(stringResourceId: Int) {
        val builder = Builder(this)
        builder.setMessage(stringResourceId)
        builder.setCancelable(true)
        builder.setPositiveButton(R.string.ok) { _, _ -> finish() }

        val alert = builder.create()
        alert.show()
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

    companion object {
        const val INTENT_START_HELP: String = "fr.julien.quievreux.droidplane2.INTENT_START_HELP"
    }
}
