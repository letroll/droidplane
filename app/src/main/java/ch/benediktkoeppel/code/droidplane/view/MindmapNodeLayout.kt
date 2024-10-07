package ch.benediktkoeppel.code.droidplane.view

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.SpannableString
import android.text.style.StyleSpan
import android.util.Log
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.Gravity
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.AbsListView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import ch.benediktkoeppel.code.droidplane.MainActivity
import ch.benediktkoeppel.code.droidplane.MainApplication
import ch.benediktkoeppel.code.droidplane.R
import ch.benediktkoeppel.code.droidplane.model.MindmapNode
import java.io.File

/**
 * A MindmapNodeLayout is the UI (layout) part of a MindmapNode.
 */
class MindmapNodeLayout : LinearLayout {
    /**
     * The MindmapNode, to which this layout belongs
     */
    @JvmField val mindmapNode: MindmapNode?

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
    @Deprecated("")
    constructor(context: Context?) : super(context) {
        mindmapNode = null
        require(isInEditMode) {
            "The constructor public MindmapNode(Context context) may only be called by graphical layout " +
                "tools, i.e. when View#isInEditMode() is true. In production, use the constructor public " +
                "MindmapNode(Context context, Node node)."
        }
    }

    constructor(context: Context, mindmapNode: MindmapNode) : super(context) {
        this.mindmapNode = mindmapNode
        mindmapNode.subscribeNodeStyleChanged(this)

        // extract icons
        val resources = context.resources
        val packageName = context.packageName

        val iconNames = mindmapNode.iconNames
        iconResourceIds = ArrayList()
        for (iconName in iconNames) {
            val drawableName = getDrawableNameFromMindmapIcon(iconName, context)
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

    @SuppressLint("InlinedApi") fun refreshView() {
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
        val spannableString = SpannableString(mindmapNode?.getNodeText())
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
        gravity = Gravity.LEFT or Gravity.CENTER
    }

    /**
     * Mindmap icons have names such as 'button-ok', but resources have to have names with pattern [a-z0-9_.]. This
     * method translates the Mindmap icon names to Android resource names.
     *
     * @param iconName the icon name as it is specified in the XML
     * @return the name of the corresponding android resource icon
     */
    private fun getDrawableNameFromMindmapIcon(iconName: String, context: Context): String {
        val locale = context.resources.configuration.locale
        var name = "icon_" + iconName.lowercase(locale).replace("[^a-z0-9_.]".toRegex(), "_")
        name = name.replace("_$".toRegex(), "")

        Log.d(MainApplication.TAG, "converted icon name $iconName to $name")

        return name
    }

    /**
     * The NodeColumn forwards the CreateContextMenu event to the appropriate MindmapNode, which can then generate
     * the context menu as it likes. Note that the MindmapNode itself is not registered as the listener for such
     * events per se, because the NodeColumn first has to decide for which MindmapNode the event applies.
     *
     * @param menu
     * @param v
     * @param menuInfo
     */
    fun onCreateContextMenu(menu: ContextMenu, v: View?, menuInfo: ContextMenuInfo?) {
        // build the menu

        menu.setHeaderTitle(mindmapNode?.getNodeText())
        if ((iconResourceIds?.size ?: 0) > 0) {
            iconResourceIds?.first()?.let { menu.setHeaderIcon(it) }
        }

        // allow copying the node text
        menu.add(CONTEXT_MENU_NORMAL_GROUP_ID, R.id.contextcopy, 0, R.string.copynodetext)

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
                menu.add(CONTEXT_MENU_ARROWLINK_GROUP_ID, linkedNode.numericId, 0, linkedNode.getNodeText())
            }
        }
    }

    /**
     * Opens the link of this node (if any)
     */
    fun openLink(mainActivity: MainActivity) {
        // TODO: if link is internal, substring ID

        Log.d(MainApplication.TAG, "Opening link (to string): " + mindmapNode?.link.toString())
        Log.d(MainApplication.TAG, "Opening link (fragment, everything after '#'): " + mindmapNode?.link?.fragment)

        // if the link has a "#ID123", it's an internal link within the document
        if (mindmapNode?.link?.fragment != null && mindmapNode.link.fragment?.startsWith("ID") == true) {
            openInternalFragmentLink(mainActivity)
        } else {
            openIntentLink(mainActivity)
        }
    }

    /**
     * Open this node's link as internal fragment
     */
    private fun openInternalFragmentLink(mainActivity: MainActivity) {
        // internal link, so this.link is of the form "#ID_123234534" this.link.getFragment() should give everything
        // after the "#" it is null if there is no "#", which should be the case for all other links

        val fragment = mindmapNode?.link?.fragment

        val linkedInternal = mindmapNode?.mindmap?.getNodeByID(fragment)

        if (linkedInternal != null) {
            Log.d(MainApplication.TAG, "Opening internal node, $linkedInternal, with ID: $fragment")

            // the internal linked node might be anywhere in the mindmap, i.e. on a completely separate branch than
            // we are on currently. We need to go to the Top, and then descend into the mindmap to reach the right
            // point
            val mindmapView = mainActivity.horizontalMindmapView
            mindmapView?.downTo(mainActivity, linkedInternal, true)
        } else {
            Toast.makeText(
                context,
                "This internal link to ID $fragment seems to be broken.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Open this node's link as intent
     */
    private fun openIntentLink(mainActivity: MainActivity) {
        // try opening the link normally with an intent

        try {
            val openUriIntent = Intent(Intent.ACTION_VIEW)
            openUriIntent.setData(mindmapNode?.link)
            mainActivity.startActivity(openUriIntent)
            return
        } catch (e: ActivityNotFoundException) {
            Log.d(MainApplication.TAG, "ActivityNotFoundException when opening link as normal intent")
        }

        // try to open as relative file
        try {
            // get path of mindmap file
            val fileName: String?
            if (mindmapNode?.link?.path?.startsWith("/") == true) {
                // absolute filename
                fileName = mindmapNode.link.path
            } else {
                // link is relative to mindmap file

                val mindmapPath = mindmapNode?.mindmap?.uri?.path
                Log.d(MainApplication.TAG, "Mindmap path $mindmapPath")
                val mindmapDirectoryPath = mindmapPath?.substring(0, mindmapPath.lastIndexOf("/"))
                Log.d(MainApplication.TAG, "Mindmap directory path $mindmapDirectoryPath")
                fileName = mindmapDirectoryPath + "/" + mindmapNode?.link?.path
            }
            val file = File(fileName)
            if (!file.exists()) {
                Toast.makeText(context, "File $fileName does not exist.", Toast.LENGTH_SHORT).show()
                Log.d(MainApplication.TAG, "File $fileName does not exist.")
                return
            }
            if (!file.canRead()) {
                Toast.makeText(context, "Can not read file $fileName.", Toast.LENGTH_SHORT).show()
                Log.d(MainApplication.TAG, "Can not read file $fileName.")
                return
            }
            Log.d(MainApplication.TAG, "Opening file " + Uri.fromFile(file))
            // http://stackoverflow.com/a/3571239/1067124
            var extension = ""
            fileName?.let {
                val i = fileName.lastIndexOf('.')
                val p = fileName.lastIndexOf('/')
                if (i > p) {
                    extension = fileName.substring(i + 1)
                }
            }
            val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

            val intent = Intent()
            intent.setAction(Intent.ACTION_VIEW)
            intent.setDataAndType(Uri.fromFile(file), mime)
            mainActivity.startActivity(intent)
        } catch (e1: Exception) {
            Toast.makeText(context, "No application found to open " + mindmapNode?.link, Toast.LENGTH_SHORT).show()
            e1.printStackTrace()
        }
    }

    fun openRichText(mainActivity: MainActivity) {
        val richTextContent = mindmapNode?.richTextContents?.get(0)
        val intent = Intent(mainActivity, RichTextViewActivity::class.java)
        intent.putExtra("richTextContent", richTextContent)
        mainActivity.startActivity(intent)
    }

    fun notifyNodeStyleChanged() {
        this.refreshView()
    }

    companion object {
        const val CONTEXT_MENU_NORMAL_GROUP_ID: Int = 0
        const val CONTEXT_MENU_ARROWLINK_GROUP_ID: Int = 1
    }
}
