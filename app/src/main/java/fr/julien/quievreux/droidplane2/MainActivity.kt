package fr.julien.quievreux.droidplane2

import android.app.AlertDialog.Builder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.ACTION_EDIT
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import fr.julien.quievreux.droidplane2.SelectedNodeType.Link
import fr.julien.quievreux.droidplane2.SelectedNodeType.None
import fr.julien.quievreux.droidplane2.SelectedNodeType.RichText
import fr.julien.quievreux.droidplane2.helper.NodeUtils
import fr.julien.quievreux.droidplane2.helper.NodeUtils.openRichText
import fr.julien.quievreux.droidplane2.ui.components.AppTopBar
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Backpress
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Help
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Open
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchNext
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.SearchPrevious
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Top
import fr.julien.quievreux.droidplane2.ui.components.AppTopBarAction.Up
import fr.julien.quievreux.droidplane2.ui.components.nodeList
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
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

    val viewModel: MainViewModel by viewModel()

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

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (viewModel.uiState.value.loading) {
            viewModel.apply {
                if (isAnExternalMindMapEdit()) {
                    currentMindMapUri = intent.data
                }

                loadMindMap(getDocumentInputStream(isAnExternalMindMapEdit()))
            }
        }
        setContent {
            ContrastAwareReplyTheme {
                val state = viewModel.uiState.collectAsState()
                if (state.value.leaving) finish() //TODO confirm dialog
                val nodeFindList = viewModel.nodeFindList.collectAsState()

                state.value.hasSelectedNodeType?.let { selectedNodeType ->
                    state.value.selectedNode?.let { selectedNode ->
                        when (selectedNodeType) {
                            None -> { /*Not Used */
                            }

                            RichText -> {
                                openRichText(
                                    mindmapNode = selectedNode,
                                    activity = this@MainActivity
                                )
                            }

                            Link -> {
                                NodeUtils.openLink(
                                    mindmapNode = selectedNode,
                                    activity = this@MainActivity,
                                    viewModel = viewModel,
                                    onLinkBroken = { fragment ->
                                        Toast.makeText(
                                            this@MainActivity,
                                            "This internal link to ID $fragment seems to be broken.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }

                Scaffold(
                    modifier = Modifier,
                    topBar = {
                        AppTopBar(
                            text = state.value.selectedNode?.getNodeText(viewModel) ?: stringResource(R.string.app_name),
                            hasBackIcon = state.value.canGoBack,
                            onQuery = { query ->
                                viewModel.search(query)
                            },
                            hasSearchNavigateButton = Pair(state.value.currentSearchResultIndex > 0, state.value.currentSearchResultIndex < nodeFindList.value.size - 1),
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
                    //                    snackbarHost = {},
                    //                    floatingActionButton = {},
                    //                    floatingActionButtonPosition =,
                    //                    containerColor =,
                    //                    contentColor =,
                    //                    contentWindowInsets =,
                    content = { innerPadding ->

                        LazyColumn(
                            modifier = Modifier.padding(innerPadding),
                        ) {
                            state.value.rootNode?.childMindmapNodes?.let { nodes ->
                                Log.e("toto", "list updated")
                                val searchResultToShow = if (nodeFindList.value.isEmpty() || (state.value.currentSearchResultIndex in 0 until nodeFindList.value.size - 1)) {
                                    null
                                } else {
                                    nodeFindList.value[state.value.currentSearchResultIndex]
                                }
                                nodeList(
                                    nodes = nodes,
                                    searchResultToShow = searchResultToShow,
                                    fetchText = { node ->
                                        node.getNodeText(viewModel)
                                    },
                                    updateClipBoard = { text ->
                                        val clipData = ClipData.newPlainText("node", text)
                                        updateClipboard(clipData)
                                    },
                                    onNodeClick = viewModel::onNodeClick,
                                    onNodeContextMenuClick = viewModel::onNodeContextMenuClick,
                                )
                            }
                        }

                    }
                )
            }
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

    private fun updateClipboard(clipData: ClipData) {
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

    companion object {
        const val INTENT_START_HELP: String = "fr.julien.quievreux.droidplane2.INTENT_START_HELP"
    }
}
