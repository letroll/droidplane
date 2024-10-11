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
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.LinearLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.AddedChild
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.NodeStyleChanged
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.RichContentChanged
import ch.benediktkoeppel.code.droidplane.controller.NodeChange.SubscribeNodeRichContentChanged
import ch.benediktkoeppel.code.droidplane.controller.OnRootNodeLoadedListener
import ch.benediktkoeppel.code.droidplane.helper.NodeUtils
import ch.benediktkoeppel.code.droidplane.helper.NodeUtils.openRichText
import ch.benediktkoeppel.code.droidplane.model.MindmapNode
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

/*        setContent {
            ContrastAwareReplyTheme {
                val state = viewModel.uiState.collectAsState()
                Scaffold(
                    modifier = Modifier,
                    topBar = {
                        AppTopBar(
                            text = stringResource(R.string.app_name),
                            hasBackIcon = false,
                            isFullScreen = false,
//                            modifier =
                        ) { }
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
//                            items(state.value.loading){
//                                Text("test")
//                            }
                        }
                    }
                )
            }
        }*/

        // set up horizontal viewModel view first
        setUpHorizontalMindmapView()

        lifecycleScope.launch {
            viewModel.uiState.collect { newState ->
                menu?.findItem(R.id.mindmap_loading)?.setVisible(newState.loading)
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
            val onRootNodeLoadedListener: OnRootNodeLoadedListener = object : OnRootNodeLoadedListener {
                override fun rootNodeLoaded(viewModel: MainViewModel, rootNode: MindmapNode?) {
                    // now set up the view
                    val finalRootNode = rootNode
                    runOnUiThread {
                        horizontalMindmapView?.apply {
                            mainViewModel = viewModel
                            // by default, the root node is the deepest node that is expanded
                            deepestSelectedMindmapNode = finalRootNode
                            onRootNodeLoaded()
                        }
                    }
                }
            }

            viewModel.apply {
                if (isAnExternalMindMapEdit()) {
                    currentMindMapUri = intent.data
                }

                loadMindMap(
                    getDocumentInputStream(isAnExternalMindMapEdit()),
                    onRootNodeLoadedListener,
                    this,
                    onNodeChange = { nodeChange ->
                        when (nodeChange) {
                            is AddedChild -> {
                                runOnUiThread {
                                    nodeChange.parentNode.notifySubscribersAddedChildMindmapNode(nodeChange.childNode)
                                }
                            }

                            is RichContentChanged -> {
                                runOnUiThread {
                                    nodeChange.node.notifySubscribersNodeRichContentChanged()
                                }
                            }

                            is NodeStyleChanged -> {
                                runOnUiThread {
                                    nodeChange.node.notifySubscribersNodeStyleChanged()
                                }
                            }

                            is SubscribeNodeRichContentChanged -> {
                                nodeChange.node.subscribeNodeRichContentChanged(this@MainActivity)
                            }
                        }
                    }
                )
            }
        }

        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        (findViewById<View>(R.id.layout_wrapper) as LinearLayout?)?.addView(horizontalMindmapView)

        horizontalMindmapView?.enableHomeButtonIfEnoughColumns()

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        horizontalMindmapView?.setApplicationTitle()
    }

    /**
     * Creates the options menu
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        this.menu = menu
        return true
    }

    /**
     * Handler for the back button, Navigate one level up, and stay at the root node
     * @see android.app.Activity#onBackPressed()
     */
    override fun onBackPressed() {
        super.onBackPressed()
        horizontalMindmapView?.upOrClose()
    }

    /**
     * Handler of all menu events Home button: navigate one level up, and exit the application if the home button is
     * pressed at the root node Menu Up: navigate one level up, and stay at the root node
     *
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.up -> horizontalMindmapView?.up()
            R.id.top -> horizontalMindmapView?.top()
            R.id.help -> {
                // create a new intent (without URI)
                val helpIntent = Intent(this, MainActivity::class.java)
                helpIntent.putExtra(INTENT_START_HELP, true)
                startActivity(helpIntent)
            }

            R.id.open -> performFileSearch()
            android.R.id.home -> horizontalMindmapView?.up()
            R.id.search -> horizontalMindmapView?.startSearch()
            R.id.search_next -> horizontalMindmapView?.searchNext()
            R.id.search_prev -> horizontalMindmapView?.searchPrevious()
        }

        return true
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
                        mainActivity = this
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
