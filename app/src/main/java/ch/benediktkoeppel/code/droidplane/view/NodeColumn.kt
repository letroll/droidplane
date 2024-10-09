package ch.benediktkoeppel.code.droidplane.view

import android.content.Context
import android.graphics.Point
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.View
import android.view.View.OnCreateContextMenuListener
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.AdapterView.AdapterContextMenuInfo
import android.widget.AdapterView.OnItemClickListener
import android.widget.LinearLayout
import android.widget.ListView
import ch.benediktkoeppel.code.droidplane.MainApplication
import ch.benediktkoeppel.code.droidplane.R
import ch.benediktkoeppel.code.droidplane.model.MindmapNode

/**
 * A column of MindmapNodes, i.e. one level in the mind map. It extends LinearLayout, and then embeds a ListView.
 * This is because we want to have a fine border around the ListView and we can only achieve this by having it
 * wrapped in a LinearLayout with a padding.
 */
class NodeColumn : LinearLayout, OnCreateContextMenuListener {
    /**
     * Returns the parent node of this column.
     *
     * @return the parent node of this colunn
     */
    /**
     * The parent node (i.e. the node that is parent to everything we display in this column)
     */
    val parentNode: MindmapNode?
    private val context: Context

    /**
     * The list of all MindmapNodeLayouts which we display in this column
     */
    //TODO replace arraylist by mutablelist
    private var mindmapNodeLayouts: ArrayList<MindmapNodeLayout>? = null

    /**
     * The adapter for this column
     */
    private var adapter: MindmapNodeAdapter? = null

    /**
     * Returns the ListView of this NodeColumn
     * @return the ListView of this NodeColumn
     */
    /**
     * The actual ListView that we'll display
     */
    var listView: ListView? = null
        private set

    /**
     * This constructor is only used to make graphical GUI layout tools happy. If used in running code, it will always
     * throw a IllegalArgumentException.
     *
     * @param context
     */
    @Deprecated("")
    constructor(context: Context) : super(context) {
        parentNode = null
        this.context = context
        require(isInEditMode) {
            "The constructor public NodeColumn(Context context) may only be called by graphical layout tools," +
                " i.e. when View#isInEditMode() is true. In production, use the constructor public NodeColumn" +
                "(Context context, Node parent)."
        }
    }

    /**
     * Creates a new NodeColumn for a parent node. This NodeColumn is a LinearLayout, which contains a ListView,
     * which displays all child nodes of the parent node.
     *
     * @param context
     * @param parent
     */
    constructor(context: Context, parent: MindmapNode) : super(context) {
        this.context = context

        this.parentNode = parent

        parent.subscribe(this)

        // create list items for each child node
        mindmapNodeLayouts = ArrayList()
        val mindmapNodes: List<MindmapNode> = parent.childMindmapNodes
        for (mindmapNode in mindmapNodes) {
            mindmapNodeLayouts?.add(MindmapNodeLayout(context, mindmapNode))
        }

        // define the layout of this LinearView
        val linearViewHeight = LayoutParams.MATCH_PARENT
        val linearViewWidth = getOptimalColumnWidth(context)
        val linearViewLayout = LayoutParams(linearViewWidth, linearViewHeight)
        layoutParams = linearViewLayout
        setPadding(0, 0, 1, 0)
        setBackgroundColor(context.resources.getColor(android.R.color.darker_gray))

        // create a ListView
        listView = ListView(context)

        // define the layout of the listView
        // should be as high as the parent (i.e. full screen height)
        val listViewHeight = LayoutParams.MATCH_PARENT
        val listViewWidth = LayoutParams.MATCH_PARENT
        val listViewLayout = ViewGroup.LayoutParams(listViewWidth, listViewHeight)
        listView?.layoutParams = listViewLayout
        listView?.setBackgroundColor(context.resources.getColor(android.R.color.background_light))

        // create adapter (i.e. data provider) for the column
        adapter = MindmapNodeAdapter(getContext(), R.layout.mindmap_node_list_item, mindmapNodeLayouts)

        // add the content adapter
        listView?.adapter = adapter

        // call NodeColumn's onCreateContextMenu when a context menu for one of the listView items should be generated
        listView?.setOnCreateContextMenuListener(this)

        // add the listView to the linearView
        this.addView(listView)
    }

    fun notifyNewMindmapNode(mindmapNode: MindmapNode) {
        mindmapNodeLayouts?.add(MindmapNodeLayout(context, mindmapNode))
        adapter?.notifyDataSetChanged()
    }

    // TODO we need a new notifier, if the node itself has updated (if text was updated, or icon was updated)
    /**
     * Sets the width of this column to columnWidth
     *
     * @param columnWidth width of the column
     */
    private fun setWidth(columnWidth: Int) {
        Log.d(MainApplication.TAG, "Setting column width to $columnWidth")

        val listViewParam = this.layoutParams
        listViewParam.width = columnWidth
        this.layoutParams = listViewParam
    }

    /**
     * Deselects all nodes of this column
     */
    fun deselectAllNodes() {
        // deselect all nodes
        mindmapNodeLayouts?.let {
            for (mindmapNodeLayout in it) {
            val mindmapNode = mindmapNodeLayout.mindmapNode
            mindmapNode?.isSelected = false
        }

            // then notify about the GUI change
            adapter?.notifyDataSetChanged()
        }
    }

    /**
     * Resizes the column to its optimal column width
     */
    fun resizeColumnWidth(context: Context) {
        setWidth(getOptimalColumnWidth(context))
    }

    /**
     * Fetches the MindmapNodeLayout at the given position
     *
     * @param position the position from which the MindmapNodeLayout should be returned
     * @return MindmapNodeLayout
     */
    fun getNodeAtPosition(position: Int): MindmapNodeLayout? {
        return mindmapNodeLayouts?.get(position)
    }

    private fun getPositionOf(node: MindmapNode): Int {
        mindmapNodeLayouts?.let {
            for (i in it.indices) {
                if (it[i].mindmapNode == node) {
                    return i
                }
            }
        }
        return 0
    }

    fun scrollTo(node: MindmapNode) {
        post { listView?.smoothScrollToPosition(getPositionOf(node)) }
    }

    /**
     * Sets the color on the node at the specified position
     *
     * @param position
     */
    fun setItemColor(position: Int) {
        // deselect all nodes
        mindmapNodeLayouts?.let {
            for (i in it.indices) {
                it[i].isSelected = false
            }

            // then select node at position
            it[position].isSelected = true

            // then notify about the GUI change
            adapter?.notifyDataSetChanged()
        }
    }

    /**
     * Simply a wrapper for ListView's setOnItemClickListener. Technically, the NodeColumn (which is a LinearView)
     * does not generate OnItemClick Events, but it's child view (the ListView) does. But it's simpler if the outside
     * world does not have to care about that detail, so we implement setOnItemClickListener and just forward the
     * listener to the actual ListView.
     *
     * @param listener
     */
    fun setOnItemClickListener(listener: OnItemClickListener?) {
        listView?.onItemClickListener = listener
    }

    /**
     *
     * This is called when a context menu for one of the list items is generated.
     *
     * @see android.view.View.OnCreateContextMenuListener#onCreateContextMenu(android.view.ContextMenu, android.view
     * .View, android.view.ContextMenu.ContextMenuInfo)
     */
    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo) {
        super.onCreateContextMenu(menu)

        // get the menu information
        val contextMenuInfo = menuInfo as AdapterContextMenuInfo

        // get the clicked node
        mindmapNodeLayouts?.let {
            val clickedNode = it[contextMenuInfo.position]

            // forward the event to the clicked node
            clickedNode.onCreateContextMenu(menu)
        }
    }

    companion object {
        /**
         * Calculates the column width which this column should have
         *
         * @return
         */
        private fun getOptimalColumnWidth(context: Context): Int {
            // and R.integer.horizontally_visible_panes defines how many columns should be visible side by side
            // so we need 1/(horizontally_visible_panes) * displayWidth as column width

            val resources = context.resources
            val horizontallyVisiblePanes = resources.getInteger(R.integer.horizontally_visible_panes)
            val display = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            val displayWidth: Int

            // get the Display width
            val displaySize = Point()
            display.getSize(displaySize)
            displayWidth = displaySize.x
            val columnWidth = displayWidth / horizontallyVisiblePanes

            Log.d(MainApplication.TAG, "Calculated column width = $columnWidth")

            return columnWidth
        }
    }
}


