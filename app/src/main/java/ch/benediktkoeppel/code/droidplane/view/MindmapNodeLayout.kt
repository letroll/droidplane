package ch.benediktkoeppel.code.droidplane.view

import android.content.Context
import android.graphics.Typeface
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.ContextMenu
import android.view.Gravity
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import ch.benediktkoeppel.code.droidplane.MainApplication
import ch.benediktkoeppel.code.droidplane.MainViewModel
import ch.benediktkoeppel.code.droidplane.R
import ch.benediktkoeppel.code.droidplane.model.MindmapNode

/**
 * A MindmapNodeLayout is the UI (layout) part of a MindmapNode.
 */
class MindmapNodeLayout : LinearLayout {
    /**
     * The MindmapNode, to which this layout belongs
     */
    val mindmapNode: MindmapNode?

    var viewModel: MainViewModel?=null
    /**
     * The Android resource IDs of the icon
     */
    private var iconResourceIds: MutableList<Int>? = null

    /**
     * This constructor is only used to make graphical GUI layout tools happy. If used in running code, it will always
     * throw a IllegalArgumentException.
     *
     * @param context
     */
    constructor(context: Context?) : super(context) {
        mindmapNode = null
        require(isInEditMode) {
            "The constructor public MindmapNode(Context context) may only be called by graphical layout " +
                "tools, i.e. when View#isInEditMode() is true. In production, use the constructor public " +
                "MindmapNode(Context context, Node node)."
        }
    }

    constructor(
        context: Context,
        mindmapNode: MindmapNode,
        viewModel: MainViewModel,
    ) : super(context) {
        this.mindmapNode = mindmapNode
        this.viewModel = viewModel
        mindmapNode.subscribeNodeStyleChanged(this)

        // extract icons
        val resources = context.resources
        val packageName = context.packageName

        val iconNames = mindmapNode.iconNames
        iconResourceIds = mutableListOf()
        for (iconName in iconNames) {
            val drawableName = getDrawableNameFromMindmapIcon(iconName)
            iconResourceIds?.add(resources.getIdentifier("@drawable/$drawableName", "id", packageName))
        }

        // set link icon if node has a link. The link icon will be the first icon shown
        if (mindmapNode.link != null) {
            iconResourceIds?.add(0, resources.getIdentifier("@drawable/link", "id", packageName))
        }

        // set the rich text icon if it has
        if (mindmapNode.richTextContents.isNotEmpty()) {
            iconResourceIds?.add(0, resources.getIdentifier("@drawable/richtext", "id", packageName))
        }
    }

    //TODO find why the first load donc show icon
    fun refreshView() {
        // inflate the layout if we haven't done so yet
        inflate(context, R.layout.mindmap_node_list_item, this)

        // the mindmap_node_list_item consists of a ImageView (icon), a TextView (node text), and another TextView
        // ("+" button)
        val icon0View = findViewById<ImageView>(R.id.icon0)
        val icon1View = findViewById<ImageView>(R.id.icon1)

        if ((iconResourceIds?.size ?: 0) > 0) {
            iconResourceIds?.first()?.let { icon0View.setImageResource(it) }
        } else {
            // don't waste space, there are no icons

            icon0View.visibility = GONE
            icon1View.visibility = GONE
        }

        // second icon
        if ((iconResourceIds?.size ?: 0) > 1) {
            iconResourceIds?.get(1)?.let { icon1View.setImageResource(it) }
        } else {
            // no second icon, don't waste space
            icon1View.visibility = GONE
        }

        val textView = findViewById<TextView>(R.id.label)
        textView.setTextColor(context.resources.getColor(android.R.color.primary_text_light))
        val spannableString = SpannableString(
            viewModel?.let {
                mindmapNode?.getNodeText(
                    viewModel = it
                )
            }
        )
        if (mindmapNode?.isBold == true) {
            spannableString.setSpan(StyleSpan(Typeface.BOLD), 0, spannableString.length, 0)
        }
        if (mindmapNode?.isItalic == true) {
            spannableString.setSpan(StyleSpan(Typeface.ITALIC), 0, spannableString.length, 0)
        }
        textView.text = spannableString

        val expandable = findViewById<ImageView>(R.id.expandable)
        if (mindmapNode?.isExpandable == true) {
            if (mindmapNode.isSelected) {
                expandable.setImageResource(R.drawable.minus_alt)
            } else {
                expandable.setImageResource(R.drawable.plus_alt)
            }
        }

        // if the node is selected, give it a special background
        if (mindmapNode?.isSelected == true) {
            // menu bar: if we are at least at API 11, the Home button is kind of a back button in the app
            val backgroundColor = context.resources.getColor(android.R.color.holo_blue_bright)

            setBackgroundColor(backgroundColor)
        } else {
            setBackgroundColor(0)
        }

        // set the layout parameter
        // TODO: this should not be necessary. The problem is that the inflate
        // (in the constructor) loads the XML as child of this LinearView, so
        // the MindmapNode-LinearView wraps the root LinearView from the
        // mindmap_node_list_item XML file.
        layoutParams = AbsListView.LayoutParams(
            AbsListView.LayoutParams.MATCH_PARENT,
            AbsListView.LayoutParams.WRAP_CONTENT
        )
        gravity = Gravity.START or Gravity.CENTER
    }

    /**
     * MainViewModel icons have names such as 'button-ok', but resources have to have names with pattern [a-z0-9_.]. This
     * method translates the MainViewModel icon names to Android resource names.
     *
     * @param iconName the icon name as it is specified in the XML
     * @return the name of the corresponding android resource icon
     */
    private fun getDrawableNameFromMindmapIcon(iconName: String): String {
        val locale = resources.configuration.locale
        var name = "icon_" + iconName.lowercase(locale).replace("[^a-z0-9_.]".toRegex(), "_")
        name = name.replace("_$".toRegex(), "")
        Log.e(MainApplication.TAG, "converted icon name $iconName to $name")

        return name
    }

    /**
     * The NodeColumn forwards the CreateContextMenu event to the appropriate MindmapNode, which can then generate
     * the context menu as it likes. Note that the MindmapNode itself is not registered as the listener for such
     * events per se, because the NodeColumn first has to decide for which MindmapNode the event applies.
     *
     * @param menu
     */
    public override fun onCreateContextMenu(menu: ContextMenu) {
        // build the menu

        menu.setHeaderTitle(viewModel?.let { mindmapNode?.getNodeText(it) })
        if ((iconResourceIds?.size ?: 0) > 0) {
            iconResourceIds?.first()?.let { menu.setHeaderIcon(it) }
        }

        // allow copying the node text
        menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.contextcopy, 0, R.string.copynodetext)

        menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.contextedittext, 0, R.string.editnodetext)

        // add menu to open link, if the node has a hyperlink
        if (mindmapNode?.link != null) {
            menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.contextopenlink, 0, R.string.openlink)
        }

        // add menu to show rich text, if the node has
        if (mindmapNode?.richTextContents != null && mindmapNode.richTextContents.isNotEmpty()) {
            menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.openrichtext, 0, R.string.openrichtext)
        }

        // add menu for each arrow link
        mindmapNode?.arrowLinks?.let { arrowLinks ->
            for (linkedNode in arrowLinks) {
                menu.add(CONTEXT_MENU_ARROWLINK_GROUP_ID, linkedNode.numericId, 0, viewModel?.let { linkedNode.getNodeText(it) })
            }
        }
    }

    companion object {
        const val CONTEXT_MENU_NORMAL_GROUP_ID: Int = 0
        const val CONTEXT_MENU_ARROWLINK_GROUP_ID: Int = 1
    }
}
