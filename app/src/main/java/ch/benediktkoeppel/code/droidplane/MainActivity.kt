package ch.benediktkoeppel.code.droidplane

import android.app.AlertDialog.Builder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.Intent.ACTION_EDIT
import android.content.Intent.ACTION_OPEN_DOCUMENT
import android.content.Intent.ACTION_VIEW
import android.content.Intent.CATEGORY_OPENABLE
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.LinearLayout
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons.Filled
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.AddedChild
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.NodeStyleChanged
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.RichContentChanged
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.SubscribeNodeRichContentChanged
import ch.benediktkoeppel.code.droidplane.helper.NodeUtils
import ch.benediktkoeppel.code.droidplane.helper.NodeUtils.openRichText
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBar
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Backpress
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Help
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Open
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Search
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.SearchNext
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.SearchPrevious
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Top
import ch.benediktkoeppel.code.droidplane.ui.components.AppTopBarAction.Up
import ch.benediktkoeppel.code.droidplane.ui.theme.ContrastAwareReplyTheme
import ch.benediktkoeppel.code.droidplane.view.HorizontalMindmapView
import ch.benediktkoeppel.code.droidplane.view.MindmapNodeLayout
import kotlinx.coroutines.launch
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

    var horizontalMindmapView: HorizontalMindmapView? = null
        private set
    private var menu: Menu? = null

    private val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            horizontalMindmapView?.upOrClose()
        }
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        /*
        feature de la bar
        * affichage du titre plus icon et éventuellement du back
        * ouverture du champ de recherche
        * occurance suivante
        * occurance précédente
        * up
        * top
        * open
        * help

         */


        if (viewModel.modern) {
            if (viewModel.uiState.value.loading) {
                viewModel.apply {
                    if (isAnExternalMindMapEdit()) {
                        currentMindMapUri = intent.data
                    }

                    loadMindMap(
                        getDocumentInputStream(isAnExternalMindMapEdit()),
                        onRootNodeLoaded = { rootNode ->
//                            horizontalMindmapView?.apply {
//                                mainViewModel = viewModel
//                                // by default, the root node is the deepest node that is expanded
//                                deepestSelectedMindmapNode = rootNode
//                                onRootNodeLoaded()
//                            }
                        },
                        onNodeChange = { nodeChange ->
                            Log.e("toto", "nodeChange:$nodeChange")
                            when (nodeChange) {
                                is AddedChild -> {
//                                    nodeChange.parentNode.notifySubscribersAddedChildMindmapNode(nodeChange.childNode)
                                }

                                is RichContentChanged -> {
//                                    nodeChange.node.notifySubscribersNodeRichContentChanged()
                                }

                                is NodeStyleChanged -> {
//                                    nodeChange.node.notifySubscribersNodeStyleChanged()
                                }

                                is SubscribeNodeRichContentChanged -> {
//                                    nodeChange.node.subscribeNodeRichContentChanged(this@MainActivity)
                                }
                            }
                        }
                    )
                }
            }
            setContent {
                ContrastAwareReplyTheme {
                    val state = viewModel.uiState.collectAsState()
                    Scaffold(
                        modifier = Modifier,
                        topBar = {
                            AppTopBar(
                                text = state.value.selectedNode?.getNodeText(viewModel) ?: stringResource(R.string.app_name),
                                hasBackIcon = state.value.selectedNode != state.value.rootNode,
                                onBarAction = {

                                }
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
                                //                            state =,
                                //                            contentPadding =,
                                //                            reverseLayout = false,
                                //                            verticalArrangement =,
                                //                            horizontalAlignment =,
                                //                            flingBehavior =,
                                //                            userScrollEnabled = false
                            ) {
                                state.value.rootNode?.childMindmapNodes?.let { nodes ->
                                    items(nodes) { node ->
                                        node.getNodeText(viewModel)?.let { text ->
                                            Text(text = text)
                                        }
                                    }
                                }
                            }
                        }
                    )
                }
            }
        } else {
            // set up horizontal viewModel view first
            setUpHorizontalMindmapView()

            lifecycleScope.launch {
                viewModel.uiState.collect { newState ->
                }
            }

            // then populate view with viewModel
            // if we already have a loaded viewModel, use this; otherwise load from the intent
            if (!viewModel.uiState.value.loading) {
                horizontalMindmapView?.apply {
                    horizontalMindmapView?.mainViewModel = viewModel
                    deepestSelectedMindmapNode = viewModel.rootNode
                    onRootNodeLoaded()
                }
                viewModel.rootNode?.subscribeNodeRichContentChanged(this)
            } else {
                viewModel.apply {
                    if (isAnExternalMindMapEdit()) {
                        currentMindMapUri = intent.data
                    }

                    loadMindMap(
                        getDocumentInputStream(isAnExternalMindMapEdit()),
                        onRootNodeLoaded = { rootNode ->
                            horizontalMindmapView?.apply {
                                mainViewModel = viewModel
                                // by default, the root node is the deepest node that is expanded
                                deepestSelectedMindmapNode = rootNode
                                onRootNodeLoaded()
                            }
                        },
                        onNodeChange = { nodeChange ->
                            when (nodeChange) {
                                is AddedChild -> {
                                    nodeChange.parentNode.notifySubscribersAddedChildMindmapNode(nodeChange.childNode)
                                }

                                is RichContentChanged -> {
                                    nodeChange.node.notifySubscribersNodeRichContentChanged()
                                }

                                is NodeStyleChanged -> {
                                    nodeChange.node.notifySubscribersNodeStyleChanged()
                                }

                                is SubscribeNodeRichContentChanged -> {
                                    nodeChange.node.subscribeNodeRichContentChanged(this@MainActivity)
                                }
                            }
                        }
                    )
                }
            }
        }

        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // The document selected by the user won't be returned in the intent. Instead, a URI to that document
                // will be contained in the return intent provided to this method as a parameter. Pull that URI using
                // resultData.getData().
                result.data?.let {
                    val uri = it.data
                    Log.i(MainApplication.TAG, "Uri: " + uri.toString())

                    // create a new intent (with URI) to open this document
                    val openFileIntent = Intent(this, MainActivity::class.java)
                    openFileIntent.setData(uri)
                    openFileIntent.setAction(ACTION_OPEN_DOCUMENT)
                    startActivity(openFileIntent)
                }
            }
        }

        onBackPressedDispatcher.addCallback(this, callback)
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

    private fun setUpHorizontalMindmapView() {
        horizontalMindmapView = HorizontalMindmapView(
            mainActivity = this,
            onToolbarTitleUpdate = { newTitle ->
                title = newTitle
                //TODO set in uiState and use in app bar
            },
            enableBackPress = { enable ->
                actionBar?.setDisplayHomeAsUpEnabled(enable)
            }
        )

        findViewById<ComposeView>(R.id.compose_view)?.apply {
            // Dispose of the Composition when the view's LifecycleOwner
            // is destroyed
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                ContrastAwareReplyTheme {
                    val state = viewModel.uiState.collectAsState()
                    AppTopBar(
                        text = state.value.selectedNode?.getNodeText(viewModel) ?: stringResource(R.string.app_name),
                        hasBackIcon = false,
                        onBarAction = { action ->
                            when (action) {
                                Search -> {
                                    horizontalMindmapView?.startSearch()
                                }

                                Backpress -> {
                                    horizontalMindmapView?.upOrClose()
                                }

                                SearchNext -> {
                                    horizontalMindmapView?.searchNext()
                                }
                                SearchPrevious -> {
                                    horizontalMindmapView?.searchPrevious()
                                }
                                Up -> {
                                    horizontalMindmapView?.up()
                                }
                                Top -> {
                                    horizontalMindmapView?.top()
                                }
                                Open -> {
                                    performFileSearch()
                                }
                                Help -> {
                                 showHelp()
                                }
                            }
                        },
                    )
                }
            }
        }

        findViewById<LinearLayout>(R.id.layout_wrapper)?.apply {
            addView(horizontalMindmapView)
        }

        horizontalMindmapView?.enableHomeButtonIfEnoughColumns()

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        horizontalMindmapView?.setApplicationTitle()
    }

    private fun showHelp() {
        val helpIntent = Intent(this, MainActivity::class.java)
        helpIntent.putExtra(INTENT_START_HELP, true)
        startActivity(helpIntent)
    }

    /**
     * It looks like the onContextItemSelected has to be overwritten in a class extending Activity. It was not
     * possible to have this callback in the NodeColumn. As a result, we have to find out here again where the event
     * happened
     *
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    override fun onContextItemSelected(item: MenuItem): Boolean {
        val contextMenuInfo = item.menuInfo as AdapterContextMenuInfo?

        // contextMenuInfo.position is the position in the ListView where the context menu was loaded, i.e. the index
        // of the item in our mindmapNodeLayouts list

        // MindmapNodeLayout extends LinearView, so we can cast targetView back to MindmapNodeLayout
        val mindmapNodeLayout = contextMenuInfo?.targetView as MindmapNodeLayout
        Log.d(MainApplication.TAG, "mindmapNodeLayout.text = " + mindmapNodeLayout.mindmapNode?.getNodeText(viewModel))

        Log.d(MainApplication.TAG, "contextMenuInfo.position = " + contextMenuInfo.position)
        Log.d(MainApplication.TAG, "item.getTitle() = " + item.title)

        when (item.groupId) {
            MindmapNodeLayout.CONTEXT_MENU_NORMAL_GROUP_ID -> when (item.itemId) {
                R.id.contextcopy -> {
                    Log.d(MainApplication.TAG, "Copying text to clipboard")
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                    val clipData = ClipData.newPlainText("node", mindmapNodeLayout.mindmapNode?.getNodeText(viewModel))
                    clipboardManager.setPrimaryClip(clipData)
                }

                R.id.contextedittext -> {
                    //TODO edit feature
                    mindmapNodeLayout.mindmapNode?.getNodeText(viewModel)
                }

                R.id.contextopenlink -> {
                    Log.d(MainApplication.TAG, "Opening node link " + mindmapNodeLayout.mindmapNode?.link)
                    NodeUtils.openLink(
                        mindmapNode = mindmapNodeLayout.mindmapNode,
                        activity = this,
                        horizontalMindmapView = horizontalMindmapView,
                        viewModel = viewModel,
                    )
                }

                R.id.openrichtext -> {
                    Log.d(
                        MainApplication.TAG,
                        "Opening rich text of node " + mindmapNodeLayout.mindmapNode?.richTextContents
                    )
                    mindmapNodeLayout.mindmapNode?.let { node ->
                        openRichText(
                            mindmapNode = node,
                            activity = this
                        )
                    }
                }

                else -> {}
            }

            MindmapNodeLayout.CONTEXT_MENU_ARROWLINK_GROUP_ID -> {
                val nodeNumericId = item.itemId
                val nodeByNumericID = viewModel.getNodeByNumericID(nodeNumericId)
                horizontalMindmapView?.downTo(nodeByNumericID, true)
            }
        }

        return true
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

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    private fun performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.

        val intent = Intent(ACTION_OPEN_DOCUMENT)

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(CATEGORY_OPENABLE)
        intent.setType("*/*")

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    companion object {
        const val INTENT_START_HELP: String = "ch.benediktkoeppel.code.droidplane.INTENT_START_HELP"

        private const val READ_REQUEST_CODE = 42
    }
}
