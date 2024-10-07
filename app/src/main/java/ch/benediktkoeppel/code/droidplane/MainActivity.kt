package ch.benediktkoeppel.code.droidplane

import android.annotation.SuppressLint
import android.app.AlertDialog.Builder
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProviders
import ch.benediktkoeppel.code.droidplane.controller.AsyncMindmapLoaderTask
import ch.benediktkoeppel.code.droidplane.controller.OnRootNodeLoadedListener
import ch.benediktkoeppel.code.droidplane.model.Mindmap
import ch.benediktkoeppel.code.droidplane.model.MindmapNode
import ch.benediktkoeppel.code.droidplane.view.HorizontalMindmapView
import ch.benediktkoeppel.code.droidplane.view.MindmapNodeLayout

/**
 * The MainActivity can be started from the App Launcher, or with a File Open intent. If the MainApplication was
 * already running, the previously used document is re-used. Also, most of the information about the mind map and the
 * currently opened views is stored in the MainApplication. This enables the MainActivity to resume wherever it was
 * before it got restarted. A restart can happen when the screen is rotated, and we want to continue wherever we were
 * before the screen rotate.
 */
class MainActivity : FragmentActivity() {
    private var mindmap: Mindmap? = null

    /**
     * HorizontalMindmapView that contains all NodeColumns
     */
    var horizontalMindmapView: HorizontalMindmapView? = null
        private set
    private var menu: Menu? = null
    private var mindmapIsLoading = false

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // enable the Android home button
        enableHomeButton()

        // set up horizontal mindmap view first
        setUpHorizontalMindmapView()

        // get the Mindmap ViewModel
        mindmap = ViewModelProviders.of(this).get(Mindmap::class.java)

        // then populate view with mindmap
        // if we already have a loaded mindmap, use this; otherwise load from the intent
        if (mindmap?.isLoaded == true) {
            horizontalMindmapView?.mindmap = mindmap
            horizontalMindmapView?.deepestSelectedMindmapNode = mindmap?.rootNode
            horizontalMindmapView?.onRootNodeLoaded()
            mindmap?.rootNode?.subscribeNodeRichContentChanged(this)
        } else {
            val onRootNodeLoadedListener: OnRootNodeLoadedListener = object : OnRootNodeLoadedListener {
                override fun rootNodeLoaded(mindmap: Mindmap?, rootNode: MindmapNode?) {
                    // now set up the view
                    val finalRootNode = rootNode
                    runOnUiThread {
                        horizontalMindmapView?.mindmap = mindmap
                        // by default, the root node is the deepest node that is expanded
                        horizontalMindmapView?.deepestSelectedMindmapNode = finalRootNode
                        horizontalMindmapView?.onRootNodeLoaded()
                    }
                }
            }

            // load the file asynchronously
            mindmap?.let {
                AsyncMindmapLoaderTask(
                    this,
                    onRootNodeLoadedListener,
                    it,
                    intent
                ).execute()
            }
        }
    }

    private fun setUpHorizontalMindmapView() {
        // create a new HorizontalMindmapView

        horizontalMindmapView = HorizontalMindmapView(this)

        (findViewById<View>(R.id.layout_wrapper) as LinearLayout).addView(horizontalMindmapView)

        // enable the up navigation with the Home (app) button (top left corner)
        horizontalMindmapView?.enableHomeButtonIfEnoughColumns(this)

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        horizontalMindmapView?.setApplicationTitle(this)
    }

    /**
     * Enables the home button if the Android version allows it
     */
    @SuppressLint("NewApi") fun enableHomeButton() {
        // menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
        val bar = actionBar
        bar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Disables the home button if the Android version allows it
     */
    @SuppressLint("NewApi") fun disableHomeButton() {
        // menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
        val bar = actionBar
        bar?.setDisplayHomeAsUpEnabled(false)
    }

    /* (non-Javadoc)
     * Creates the options menu
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater = menuInflater
        inflater.inflate(R.menu.main, menu)
        this.menu = menu
        updateLoadingIndicatorOnUiThread()
        return true
    }

    /* (non-Javadoc)
     * Handler for the back button, Navigate one level up, and stay at the root node
     * @see android.app.Activity#onBackPressed()
     */
    override fun onBackPressed() {
        horizontalMindmapView?.upOrClose()
    }

    /*
     * (non-Javadoc)
     *
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

    /*
     * (non-Javadoc)
     *
     * It looks like the onContextItemSelected has to be overwritten in a class extending Activity. It was not
     * possible to have this callback in the NodeColumn. As a result, we have to find out here again where the event
     * happened
     *
     * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
     */
    @Suppress("deprecation") @SuppressLint("NewApi") override fun onContextItemSelected(item: MenuItem): Boolean {
        val contextMenuInfo = item.menuInfo as AdapterContextMenuInfo?

        // contextMenuInfo.position is the position in the ListView where the context menu was loaded, i.e. the index
        // of the item in our mindmapNodeLayouts list

        // MindmapNodeLayout extends LinearView, so we can cast targetView back to MindmapNodeLayout
        val mindmapNodeLayout = contextMenuInfo?.targetView as MindmapNodeLayout
        Log.d(MainApplication.TAG, "mindmapNodeLayout.text = " + mindmapNodeLayout.mindmapNode?.getNodeText())

        Log.d(MainApplication.TAG, "contextMenuInfo.position = " + contextMenuInfo.position)
        Log.d(MainApplication.TAG, "item.getTitle() = " + item.title)

        when (item.groupId) {
            MindmapNodeLayout.CONTEXT_MENU_NORMAL_GROUP_ID -> when (item.itemId) {
                R.id.contextcopy -> {
                    Log.d(MainApplication.TAG, "Copying text to clipboard")
                    val clipboardManager = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager

                    val clipData = ClipData.newPlainText("node", mindmapNodeLayout.mindmapNode?.getNodeText())
                    clipboardManager.setPrimaryClip(clipData)
                }

                R.id.contextopenlink -> {
                    Log.d(MainApplication.TAG, "Opening node link " + mindmapNodeLayout.mindmapNode?.link)
                    mindmapNodeLayout.openLink(this)
                }

                R.id.openrichtext -> {
                    Log.d(
                        MainApplication.TAG,
                        "Opening rich text of node " + mindmapNodeLayout.mindmapNode?.richTextContents
                    )
                    mindmapNodeLayout.openRichText(this)
                }

                else -> {}
            }

            MindmapNodeLayout.CONTEXT_MENU_ARROWLINK_GROUP_ID -> {
                val nodeNumericId = item.itemId
                val nodeByNumericID = mindmap?.getNodeByNumericID(nodeNumericId)
                horizontalMindmapView?.downTo(this, nodeByNumericID, true)
            }
        }

        return true
    }

    /**
     * Shows a popup with an error message and then closes the application
     *
     * @param stringResourceId
     */
    fun abortWithPopup(stringResourceId: Int) {
        val builder = Builder(this)
        builder.setMessage(stringResourceId)
        builder.setCancelable(true)
        builder.setPositiveButton(R.string.ok) { dialog, which -> finish() }

        val alert = builder.create()
        alert.show()
    }

    /**
     * Fires an intent to spin up the "file chooser" UI and select an image.
     */
    fun performFileSearch() {
        // ACTION_OPEN_DOCUMENT is the intent to choose a file via the system's file
        // browser.

        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)

        // Filter to only show results that can be "opened", such as a
        // file (as opposed to a list of contacts or timezones)
        intent.addCategory(Intent.CATEGORY_OPENABLE)
        intent.setType("*/*")

        startActivityForResult(intent, READ_REQUEST_CODE)
    }

    public override fun onActivityResult(
        requestCode: Int, resultCode: Int,
        resultData: Intent?
    ) {
        // The ACTION_OPEN_DOCUMENT intent was sent with the request code READ_REQUEST_CODE. If the request code seen
        // here doesn't match, it's the response to some other intent, and the code below shouldn't run at all.

        super.onActivityResult(requestCode, resultCode, resultData)
        if (requestCode == READ_REQUEST_CODE && resultCode == RESULT_OK) {
            // The document selected by the user won't be returned in the intent. Instead, a URI to that document
            // will be contained in the return intent provided to this method as a parameter. Pull that URI using
            // resultData.getData().
            if (resultData != null) {
                val uri = resultData.data
                Log.i(MainApplication.TAG, "Uri: " + uri.toString())

                // create a new intent (with URI) to open this document
                val openFileIntent = Intent(this, MainActivity::class.java)
                openFileIntent.setData(uri)
                openFileIntent.setAction(Intent.ACTION_OPEN_DOCUMENT)
                startActivity(openFileIntent)
            }
        }
    }

    fun setMindmapIsLoading(mindmapIsLoading: Boolean) {
        this.mindmapIsLoading = mindmapIsLoading

        // update the loading indicator in the menu
        updateLoadingIndicatorOnUiThread()
    }

    private fun updateLoadingIndicatorOnUiThread() {
        if (menu != null && menu?.findItem(R.id.mindmap_loading) != null) {
            val mindmapLoadingIndicator = menu?.findItem(R.id.mindmap_loading)

            runOnUiThread { mindmapLoadingIndicator?.setVisible(mindmapIsLoading) }
        }
    }

    fun notifyNodeRichContentChanged() {
        horizontalMindmapView?.notifyNodeContentChanged(this)
    }

    companion object {
        const val INTENT_START_HELP: String = "ch.benediktkoeppel.code.droidplane.INTENT_START_HELP"

        private const val READ_REQUEST_CODE = 42
    }
}
