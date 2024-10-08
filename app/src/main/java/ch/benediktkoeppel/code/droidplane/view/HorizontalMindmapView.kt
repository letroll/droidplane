package ch.benediktkoeppel.code.droidplane.view

import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.Context
import android.content.DialogInterface
import android.os.Handler
import android.text.Html
import android.util.Log
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView.BufferType.EDITABLE
import ch.benediktkoeppel.code.droidplane.MainActivity
import ch.benediktkoeppel.code.droidplane.MainApplication
import ch.benediktkoeppel.code.droidplane.R
import ch.benediktkoeppel.code.droidplane.helper.AndroidHelper.getActivity
import ch.benediktkoeppel.code.droidplane.MainViewModel
import ch.benediktkoeppel.code.droidplane.model.MindmapNode
import java.util.Collections
import kotlin.math.abs

class HorizontalMindmapView(private val mainActivity: MainActivity) : HorizontalScrollView(mainActivity), OnTouchListener, OnItemClickListener {
    /**
     * HorizontalScrollView can only have one view, so we need to add a LinearLayout underneath it, and then stuff
     * all NodeColumns into this linearLayout.
     */
    private val linearLayout: LinearLayout

    /**
     * nodeColumns holds the list of columns that are displayed in this HorizontalScrollView.
     */

    // TODO: why does the view need access to the mainActivity?

    // list where all columns are stored
    private val nodeColumns: MutableList<NodeColumn> = ArrayList()

    /**
     * Gesture detector
     */
    private val gestureDetector: GestureDetector

    /**
     * This translates ListViews to NodeColumns. We need this because the OnItemClicked Events come with a ListView
     * (i.e. the ListView which was clicked) as parent, but we need to find out which NodeColumn was clicked. This
     * would have been a simple cast if NodeColumn extended ListView, but we extend LinearLayout and wrap the ListView.
     */
    private val listViewToNodeColumn: MutableMap<ListView?, NodeColumn> = HashMap()

    var viewModel: MainViewModel? = null

    /**
     * The deepest selected viewModel node
     */
    var deepestSelectedMindmapNode: MindmapNode? = null

    // Search state
    private var lastSearchString: String? = null
    private var searchResultNodes = listOf<MindmapNode>()
    private var currentSearchResultIndex = 0

    /**
     * Setting up a HorizontalMindmapView. We initialize the nodeColumns, define the layout parameters for the
     * HorizontalScrollView and create the LinearLayout view inside the HorizontalScrollView.
     *
     * @param mainActivity the Application Context
     */
    init {
        // set the layout for the HorizontalScrollView itself
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)

        // create the layout parameters for a new LinearLayout
        val height = LayoutParams.MATCH_PARENT
        val width = LayoutParams.MATCH_PARENT
        val linearLayoutParams = ViewGroup.LayoutParams(width, height)

        // create a LinearLayout in this HorizontalScrollView. All NodeColumns will go into that LinearLayout.
        linearLayout = LinearLayout(mainActivity)
        linearLayout.layoutParams = linearLayoutParams
        this.addView(linearLayout)

        // add a new gesture controller
        val horizontalMindmapViewGestureDetector =
            HorizontalMindmapViewGestureDetector()
        gestureDetector = GestureDetector(context, horizontalMindmapViewGestureDetector)

        // register HorizontalMindmapView to receive all touch events on itself
        setOnTouchListener(this)

        // fix the widths of all columns
        resizeAllColumns(context)
    }

    // TODO: comment missing
    fun onRootNodeLoaded() {
        // expand the selected node chain

        downTo(context, this.deepestSelectedMindmapNode, true)

        // and then scroll to the right
        scrollToRight()
    }

    /**
     * Add a new NodeColumn to the HorizontalMindmapView
     *
     * @param nodeColumn the NodeColumn to add to the HorizontalMindmapView
     */
    private fun addColumn(nodeColumn: NodeColumn) {
        // add the column to the layout

        nodeColumns.add(nodeColumn)

        // assert that the nodeColumns make a proper hierarchy, i.e. nodeColumn i's parent is nodeColumn i-1
        var previousParent: MindmapNode? = null
        for (column in nodeColumns) {
            val thisParent = column.parentNode
            check(thisParent?.parentNode == previousParent) { "Node column $nodeColumn has a parent that doesn't match with the left column" }
            previousParent = thisParent
        }


        linearLayout.addView(nodeColumn, linearLayout.childCount)
        Log.d(MainApplication.TAG, "linearLayout now has " + linearLayout.childCount + " items")

        // register as onItemClickListener and onItemLongClickListener. This HorizontalMindmapView has to register
        // itself as onItemClickListener, it's not enough if the nodeColumn would handle this onItemClick events itself.
        // This is because we might have to remove columns (and add new columns) depending on where the user clicks,
        // which is the responsibility of this HorizontalMindmapView.
        nodeColumn.setOnItemClickListener(this)
    }

    /**
     * GUI Helper to scroll the HorizontalMindmapView all the way to the right. Should be called after adding a
     * NodeColumn.
     *
     * @return true if the key event is consumed by this method, false otherwise
     */
    private fun scrollToRight() {
        // a runnable that knows "this"

        class HorizontalMindmapViewRunnable(var horizontalMindmapView: HorizontalMindmapView) : Runnable {
            override fun run() {
                horizontalMindmapView.fullScroll(FOCUS_RIGHT)
            }
        }

        Handler().postDelayed(HorizontalMindmapViewRunnable(this), 100L)
    }

    /**
     * Removes all columns from this HorizontalMindmapView
     */
    private fun removeAllColumns() {
        // unselect all nodes

        for (nodeColumn in nodeColumns) {
            nodeColumn.deselectAllNodes()
        }

        // then remove all columns
        nodeColumns.clear()
        linearLayout.removeAllViews()
    }

    /**
     * Adjusts the width of all columns in the HorizontalMindmapView
     */
    private fun resizeAllColumns(context: Context) {
        for (nodeColumn in nodeColumns) {
            nodeColumn.resizeColumnWidth(context)
        }
    }

    //TODO remove kotlin bang bang
    /**
     * Removes the rightmost column and returns true. If there was no column to remove, returns false. It never
     * removes the last column, i.e. it never removes the root node of the mind map.
     *
     * @return True if a column was removed, false if no column was removed.
     */
    private fun removeRightmostColumn(): Boolean {
        // only remove a column if we have at least 2 columns. If there is only one column, it will not be removed.

        if (nodeColumns.size >= 2) {
            // the column to remove

            val rightmostColumn = nodeColumns[nodeColumns.size - 1]

            // remove it from the linear layout
            linearLayout.removeView(rightmostColumn)

            // remove it from the nodeColumns list
            nodeColumns.removeAt(nodeColumns.size - 1)

            // then deselect all nodes on the now newly rightmost column and let the column redraw
            nodeColumns[nodeColumns.size - 1].deselectAllNodes()

            // a column was removed, so we return true
            return true
        } else {
            return false
        }
    }

    private val numberOfColumns: Int
        /**
         * Returns the number of columns in the HorizontalMindmapView.
         *
         * @return
         */
        get() = nodeColumns.size

    private val titleOfRightmostParent: String
        /**
         * Returns the title of the parent node of the rightmost column. This is the same as the node name of the
         * selected node from the 2nd-rightmost column. So this is the last node that the user has clicked. If the
         * rightmost column has no parent, an empty string is returned.
         *
         * @return Title of the right most parent node or an empty string.
         */
        get() {
            if (!nodeColumns.isEmpty()) {
                val parent = nodeColumns[nodeColumns.size - 1].parentNode
                val text = parent?.getNodeText()
                if (text != null && !text.isEmpty()) {
                    return text
                } else if (parent?.richTextContents != null && !parent.richTextContents.isEmpty()) {
                    val richTextContent = parent.richTextContents[0]
                    return Html.fromHtml(richTextContent).toString()
                } else {
                    return ""
                }
            } else {
                Log.d(MainApplication.TAG, "getTitleOfRightmostParent returned \"\" because nodeColumns is empty")
                return ""
            }
        }

    /**
     * Remove all columns at the right of the specified column.
     *
     * @param nodeColumn
     */
    private fun removeAllColumnsRightOf(nodeColumn: NodeColumn?) {
        // we go from right to left, from the end of nodeColumns back to one
        // element after nodeColumn
        //
        // nodeColumns = [ col1, col2, col3, col4, col5 ];
        // removeAllColumnsRightOf(col2) will do:
        //     nodeColumns.size()-1 => 4
        //     nodeColumns.lastIndexOf(col2)+1 => 2
        //
        // for i in (4, 3, 2): remove rightmost column
        //     i = 4: remove col5
        //     i = 3: remove col4
        //     i = 2: remove col3
        //
        // so at the end, we have
        // nodeColumns = [ col1, col2 ];

        for (i in nodeColumns.size - 1 downTo nodeColumns.lastIndexOf(nodeColumn) + 1) {
            // remove this column

            removeRightmostColumn()
        }
    }

    /**
     * Navigates to the top of the MainViewModel
     */
    fun top() {
        // remove all ListView layouts in linearLayout parent_list_view

        removeAllColumns()

        // go down into the root node
        viewModel?.rootNode?.let {
            down(context, it)
        }
    }

    /**
     * Navigates back up one level in the MainViewModel, if possible (otherwise does nothing)
     */
    fun up() {
        up(false)
    }

    /**
     * Navigates back up one level in the MainViewModel. If we already display the root node, the application will finish
     */
    fun upOrClose() {
        up(true)
    }

    /**
     * Navigates back up one level in the MainViewModel, if possible. If force is true, the application closes if we can't
     * go further up
     *
     * @param force
     */
    private fun up(force: Boolean) {
        val wasColumnRemoved = removeRightmostColumn()

        // close the application if no column was removed, and the force switch was on
        if (!wasColumnRemoved && force) {
            getActivity(context, Activity::class.java)?.finish()
        }

        // enable the up navigation with the Home (app) button (top left corner)
        enableHomeButtonIfEnoughColumns(context)

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        setApplicationTitle(context)
    }

    /**
     * Open up Node node, and display all its child nodes. This should only be called if the node's parent is
     * currently already expanded. If not (e.g. when following a deep link), use downTo
     *
     * @param node
     */
    private fun down(context: Context, node: MindmapNode) {
        // add a new column for this node and add it to the HorizontalMindmapView

        var nodeColumn: NodeColumn
        synchronized(node) {
            if (node.parentNode != null) {
                synchronized(node.parentNode) {
                    nodeColumn = NodeColumn(getContext(), node)
                    addColumn(nodeColumn)
                }
            } else {
                nodeColumn = NodeColumn(getContext(), node)
                addColumn(nodeColumn)
            }
        }

        // keep track of which list view belongs to which node column. This is necessary because onItemClick will get a
        // ListView (the one that was clicked), and we need to know which NodeColumn this is.
        val nodeColumnListView = nodeColumn.listView
        listViewToNodeColumn[nodeColumnListView] = nodeColumn

        // then scroll all the way to the right
        scrollToRight()

        // enable the up navigation with the Home (app) button (top left corner)
        enableHomeButtonIfEnoughColumns(context)

        // get the title of the parent of the rightmost column (i.e. the selected node in the 2nd-rightmost column)
        setApplicationTitle(context)

        // mark node as selected
        node.isSelected = true

        // keep track in the mind map which node is currently selected
        this.deepestSelectedMindmapNode = node
    }

    /**
     * Navigate down the MainViewModel to the specified node, opening each of it's parent nodes along the way.
     * @param context
     * @param node
     */
    fun downTo(context: Context, node: MindmapNode?, openLast: Boolean) {
        // first navigate back to the top (essentially closing all other nodes)

        top()

        // go upwards from the target node, and keep track of each node leading down to the target node
        val nodeHierarchy: MutableList<MindmapNode> = ArrayList()
        var tmpNode = node
        while (tmpNode?.parentNode != null) {   // TODO: this gives a NPE when rotating the device
            nodeHierarchy.add(tmpNode)
            tmpNode = tmpNode.parentNode
        }

        // reverse the list, so that we start with the root node
        Collections.reverse(nodeHierarchy)

        // descent from the root node down to the target node
        for (mindmapNode in nodeHierarchy) {
            mindmapNode.isSelected = true
            scrollTo(mindmapNode)
            if ((mindmapNode != node || openLast) && mindmapNode.numChildMindmapNodes > 0) {
                down(context, mindmapNode)
            }
        }
    }

    private fun scrollTo(node: MindmapNode) {
        if (nodeColumns.isEmpty()) {
            return
        }
        val lastCol = nodeColumns[nodeColumns.size - 1]
        lastCol.scrollTo(node)
    }

    /**
     * Sets the application title to the name of the parent node of the rightmost column, which is the most recently
     * clicked node.
     */
    fun setApplicationTitle(context: Context?) {
        // TODO: this needs to update when richtext content is loaded

        // get the title of the parent of the rightmost column (i.e. the
        // selected node in the 2nd-rightmost column)
        // set the application title to this nodeTitle. If the nodeTitle is
        // empty, we set the default Application title

        val nodeTitle = titleOfRightmostParent
        Log.d(MainApplication.TAG, "nodeTitle = $nodeTitle")
        if (nodeTitle == null || nodeTitle == "") {
            Log.d(
                MainApplication.TAG, "Setting application title to default string: " +
                    resources.getString(R.string.app_name)
            )
            getActivity(context, Activity::class.java)?.setTitle(R.string.app_name)
        } else {
            Log.d(MainApplication.TAG, "Setting application title to node name: $nodeTitle")
            getActivity(context, Activity::class.java)?.title = nodeTitle
            // TODO: java.lang.NullPointerException: Attempt to invoke virtual method 'void android.app.Activity.setTitle(java.lang.CharSequence)' on a null object reference
        }
    }

    /**
     * Enables the Home button in the application if we have enough columns, i.e. if "Up" will remove a column.
     */
    // TODO: the view should not do this
    fun enableHomeButtonIfEnoughColumns(context: Context?) {
        // if we only have one column (i.e. this is the root node), then we
        // disable the home button
        val numberOfColumns = numberOfColumns
        if (numberOfColumns >= 2) {
            getActivity(context, MainActivity::class.java)?.enableHomeButton()
        } else {
            getActivity(context, MainActivity::class.java)?.disableHomeButton()
        }
    }

    /**
     *
     * Handler when one of the ListItem's item is clicked Find the node which was clicked, and redraw the screen with
     * this node as new parent if the clicked node has no child, then we stop here
     *
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View,
     * int, long)
     */
    override fun onItemClick(parent: AdapterView<*>?, view: View, position: Int, id: Long) {
        // the clicked column parent is the ListView in which the user clicked. Because NodeColumn does not extend
        // ListView (it only wraps a ListView), we have to find out which NodeColumn it was. We can do so because
        // NodeColumn.getNodeColumnFromListView uses a static HashMap to do the translation.

        val clickedNodeColumn = listViewToNodeColumn[parent]

        // remove all columns right of the column which was clicked
        removeAllColumnsRightOf(clickedNodeColumn)

        // then get the clicked node
        val clickedNode = clickedNodeColumn?.getNodeAtPosition(position)

        // if the clicked node has child nodes, we set it to selected and drill down
        clickedNode?.let {
            if ((clickedNode.mindmapNode?.numChildMindmapNodes ?: 0) > 0) {
                // give it a special color

                clickedNodeColumn.setItemColor(position)

                // and drill down
                clickedNode.mindmapNode?.let {
                    down(mainActivity, it)
                }
            } else if (clickedNode.mindmapNode?.link != null) {
                clickedNode.openLink(mainActivity)
            } else if (clickedNode.mindmapNode?.richTextContents?.isNotEmpty() == true) {
                clickedNode.openRichText(mainActivity)
            } else {
                setApplicationTitle(context)
            }
        }
    }

    /**
     *
     *
     * Will be called whenever the HorizontalScrollView is touched. We have to capture the move left and right events
     * here, and snap to the appropriate column borders.
     *
     * @see android.view.View.OnTouchListener#onTouch(android.view.View, android.view.MotionEvent)
     */
    override fun onTouch(view: View, event: MotionEvent): Boolean {
        // first, we let the gestureDetector examine the event. It will process the event if it was a gesture, i.e.
        // if it was fast enough to trigger a Fling. If it handled the event, we don't process it further. This
        // gesture can be triggered if the user moves the finger fast enough. He does not necessarily have to move so
        // far that the next column is mostly visible.

        if (gestureDetector.onTouchEvent(event)) {
            Log.d(MainApplication.TAG, "Touch event was processed by HorizontalMindmapView (gesture)")
            return true
        } else if (event.action == MotionEvent.ACTION_UP || event.action == MotionEvent.ACTION_CANCEL) {
            // now we need to find out where the HorizontalMindmapView is horizontally scrolled

            val scrollX = scrollX
            Log.d(MainApplication.TAG, "HorizontalMindmapView is scrolled horizontally to $scrollX")

            // get the leftmost column that is still (partially) visible
            val leftmostVisibleColumn = getLeftmostVisibleColumn()

            // get the number of visible pixels of this column
            val numVisiblePixelsOnColumn = getVisiblePixelOfLeftmostColumn()

            // if we couldn't find a column, we could not process this event. I'm not sure how this might ever happen
            if (leftmostVisibleColumn == null) {
                Log.e(MainApplication.TAG, "No leftmost visible column was detected. Not sure how this could happen!")
                return false
            }

            // and then determine if the leftmost visible column shows more than 50% of its full width if it shows
            // more than 50%, then we scroll to the left, so that we can see it fully
            if (numVisiblePixelsOnColumn < leftmostVisibleColumn.width / 2) {
                Log.d(MainApplication.TAG, "Scrolling to the left, so that we can see the column fully")
                smoothScrollTo(scrollX + numVisiblePixelsOnColumn, 0)
            } else {
                Log.d(MainApplication.TAG, "Scrolling to the right, so that the column is not visible anymore")
                smoothScrollTo(scrollX + numVisiblePixelsOnColumn - leftmostVisibleColumn.width, 0)
            }

            // we have processed this event
            Log.d(MainApplication.TAG, "Touch event was processed by HorizontalMindmapView (no gesture)")
            return true
        } else {
            Log.d(MainApplication.TAG, "Touch event was not processed by HorizontalMindmapView")
            return false
        }
    }

    /**
     * Get the column at the left edge of the screen.
     *
     * @return NodeColumn
     */
    private fun getLeftmostVisibleColumn(): NodeColumn?{
            // how much we are horizontally scrolled
            val scrollX = scrollX

            // how many columns fit into less than scrollX space? as soon as sumColumnWdiths > scrollX, we have just
            // added the first visible column at the left.
            var sumColumnWidths = 0
            var leftmostVisibleColumn: NodeColumn? = null
            for (i in nodeColumns.indices) {
                sumColumnWidths += nodeColumns[i].width

                // if the sum of all columns so far exceeds scrollX, the current NodeColumn is (at least a little bit)
                // visible
                if (sumColumnWidths >= scrollX) {
                    leftmostVisibleColumn = nodeColumns[i]
                    break
                }
            }

            return leftmostVisibleColumn
        }

    /**
     * Get the number of pixels that are visible on the leftmost column.
     *
     * @return
     */
    fun getVisiblePixelOfLeftmostColumn(): Int{
            // how much we are horizontally scrolled
            val scrollX = scrollX

            // how many columns fit into less than scrollX space? as soon as
            // sumColumnWdiths > scrollX, we have just added the first visible
            // column at the left.
            var sumColumnWidths = 0
            var numVisiblePixelsOnColumn = 0
            for (i in nodeColumns.indices) {
                sumColumnWidths += nodeColumns[i].width

                // if the sum of all columns so far exceeds scrollX, the current NodeColumn is (at least a little bit)
                // visible
                if (sumColumnWidths >= scrollX) {
                    // how many pixels are visible of this column?
                    numVisiblePixelsOnColumn = sumColumnWidths - scrollX
                    break
                }
            }

            return numVisiblePixelsOnColumn
        }

    /** Shows a dialog to input the search string and fires the search.  */
    fun startSearch() {
        val alert = Builder(context)
        alert.setTitle("Search")

        val input = EditText(context)
        input.setText(lastSearchString, EDITABLE)
        input.hint = "Search"

        alert.setView(input)
        alert.setPositiveButton("Search") { dialog: DialogInterface?, which: Int -> search(input.text.toString()) }
        alert.create().show()
    }

    /** Performs the search, stores the result, and selects the first matching node.  */
    private fun search(searchString: String) {
        lastSearchString = searchString
        val searchRoot = nodeColumns[nodeColumns.size - 1].parentNode
        searchResultNodes = searchRoot?.search(searchString)?: emptyList()
        currentSearchResultIndex = 0
        showCurrentSearchResult()
    }

    /** Selects the current search result node.  */
    private fun showCurrentSearchResult() {
        if (currentSearchResultIndex >= 0 && currentSearchResultIndex < searchResultNodes.size) {
            downTo(context, searchResultNodes[currentSearchResultIndex], false)
        }
        // Shows/hides the next/prev buttons
        // FIXME findViewById doesn't work, looks like you need to call invalidateOptionsMenu()
        // and then hide/show the items in onCreateOptionsMenu(Menu) using menu.findItem().setVisible()
//        findViewById(R.id.search_prev).setVisibility(currentSearchResultIndex > 0 ? VISIBLE : GONE);
//        findViewById(R.id.search_next).setVisibility(currentSearchResultIndex < searchResultNodes.size() - 1 ? VISIBLE : GONE);
    }

    /** Selects the next search result node.  */
    fun searchNext() {
        if (currentSearchResultIndex < searchResultNodes.size - 1) {
            currentSearchResultIndex++
            showCurrentSearchResult()
        }
    }

    /** Selects the previous search result node.  */
    fun searchPrevious() {
        if (currentSearchResultIndex > 0) {
            currentSearchResultIndex--
            showCurrentSearchResult()
        }
    }

    fun notifyNodeContentChanged(context: Context?) {
        setApplicationTitle(context)
    }

    /**
     * The HorizontalMindmapViewGestureDetector should detect the onFling event. However, it never receives the
     * onDown event, so when it gets the onFling the event1 is empty, and we can't detect the fling properly.
     */
    private inner class HorizontalMindmapViewGestureDetector : SimpleOnGestureListener() {
        override fun onDown(e: MotionEvent): Boolean {
            return true
        }

        /**
         * onFling is called whenever a Fling (a fast swipe) event is detected. However, for some reason, our onDown
         * method is never called, and the onFling method never gets a valid event1 (it's always null). So instead of
         * relying on event1 and event2 (and determine the distance the finger moved), we only consider the velocity
         * of the fling. This is not as accurate as it could be, but it works.
         *
         * @see android.view.GestureDetector.SimpleOnGestureListener#onFling(android .view.MotionEvent, android.view
         * .MotionEvent, float, float)
         */
        override fun onFling(event1: MotionEvent?, event2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
            try {
                // how much we are horizontally scrolled

                val scrollX = scrollX
                Log.d(MainApplication.TAG, "Velocity = $velocityX")

                // get the leftmost column that is still (partially) visible
                val leftmostVisibleColumn: NodeColumn? = getLeftmostVisibleColumn()

                // get the number of visible pixels of this columkkkn
                val numVisiblePixelsOnColumn: Int = getVisiblePixelOfLeftmostColumn()

                // if we have moved at least the SWIPE_MIN_DISTANCE to the right and at faster than
                // SWIPE_THRESHOLD_VELOCITY
                if (velocityX < 0 && abs(velocityX.toDouble()) > SWIPE_THRESHOLD_VELOCITY) {
                    // scroll to the target column

                    smoothScrollTo(scrollX + numVisiblePixelsOnColumn, 0)

                    Log.d(MainApplication.TAG, "processed the Fling to Right gesture")
                    return true
                } else if (velocityX > 0 && abs(velocityX.toDouble()) > SWIPE_THRESHOLD_VELOCITY) {
                    // scroll to the target column
                    // scrolls in the wrong direction

                    smoothScrollTo(scrollX + numVisiblePixelsOnColumn - (leftmostVisibleColumn?.width?:0), 0)

                    Log.d(MainApplication.TAG, "processed the Fling to Left gesture")
                    return true
                } else {
                    Log.d(MainApplication.TAG, "Fling was no real fling")
                    return false
                }
            } catch (e: Exception) {
                Log.d(MainApplication.TAG, "A whole lot of stuff could have gone wrong here")
                e.printStackTrace()
                return false
            }
        }
    }

    companion object {
        /**
         * Constants to determine the minimum swipe distance and speed
         */
        private const val SWIPE_THRESHOLD_VELOCITY = 300
    }
}
