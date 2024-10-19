package fr.julien.quievreux.droidplane2.helper

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.Intent.ACTION_VIEW
import android.net.Uri
import android.util.Log
import android.util.Pair
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import fr.julien.quievreux.droidplane2.MainApplication
import fr.julien.quievreux.droidplane2.MainViewModel
import fr.julien.quievreux.droidplane2.model.MindmapIndexes
import fr.julien.quievreux.droidplane2.model.MindmapNode
import fr.julien.quievreux.droidplane2.view.RichTextViewActivity
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.File
import java.io.IOException
import java.util.Stack

object NodeUtils {

    @Throws(IOException::class, XmlPullParserException::class)
    fun loadRichContentNodes(xpp: XmlPullParser): String {
        // as we are stream processing the XML, we need to consume the full XML until the
        // richcontent tag is closed (i.e. until we're back at the current parsing depth)
        // eagerly parse until richcontent node is closed
        val startingDepth = xpp.depth
        var richTextContent = ""

        var richContentSubParserEventType = xpp.next()

        do {
            // EVENT TYPES as reported by next()

            when (richContentSubParserEventType) {
                XmlPullParser.START_DOCUMENT -> throw IllegalStateException("Received START_DOCUMENT but were already within the document")

                XmlPullParser.END_DOCUMENT -> throw IllegalStateException("Received END_DOCUMENT but expected to just parse a sub-document")

                XmlPullParser.START_TAG -> {
                    var tagString = ""

                    val tagName = xpp.name
                    tagString += "<$tagName"

                    var i = 0
                    while (i < xpp.attributeCount) {
                        val attributeName = xpp.getAttributeName(i)
                        val attributeValue = xpp.getAttributeValue(i)

                        val attributeString = " $attributeName=\"$attributeValue\""
                        tagString += attributeString
                        i++
                    }

                    tagString += ">"

                    richTextContent += tagString
                }

                XmlPullParser.END_TAG -> {
                    val tagName = xpp.name
                    val tagString = "</$tagName>"
                    richTextContent += tagString
                }

                XmlPullParser.TEXT -> {
                    val text = xpp.text
                    richTextContent += text
                }

                else -> throw IllegalStateException("Received unexpected event type $richContentSubParserEventType")

            }

            richContentSubParserEventType = xpp.next()

            // stop parsing once we have come out far enough from the XML to be at the starting depth again
        } while (xpp.depth != startingDepth)
        return richTextContent
    }

    fun fillArrowLinks(nodesById: Map<String, MindmapNode>? ) {
        nodesById?.let {
            for (nodeId in nodesById.keys) {
                val mindmapNode = nodesById[nodeId]
                mindmapNode?.arrowLinkDestinationIds?.let {
                    for (linkDestinationId in it) {
                        val destinationNode = nodesById[linkDestinationId]
                        if (destinationNode != null) {
                            mindmapNode.arrowLinkDestinationNodes.add(destinationNode)
                            destinationNode.arrowLinkIncomingNodes.add(mindmapNode)
                        }
                    }
                }
            }
        }
    }

    /**
     * Index all nodes (and child nodes) by their ID, for fast lookup
     *
     * @param root
     */
    fun loadAndIndexNodesByIds(root: MindmapNode?): MindmapIndexes {
        // TODO: check if this optimization was necessary - otherwise go back to old implementation

        // TODO: this causes us to load all viewModel nodes, defeating the lazy loading in fr.julien.quievreux.droidplane2.model.MindmapNode.getChildNodes

        val stack = Stack<MindmapNode?>()
        stack.push(root)

        // try first to just extract all IDs and the respective node, and
        // only insert into the hashmap once we know the size of the hashmap
        val idAndNode: MutableList<Pair<String, MindmapNode>> = mutableListOf()
        val numericIdAndNode: MutableList<Pair<Int, MindmapNode>> = mutableListOf()

        while (!stack.isEmpty()) {
            val node = stack.pop()

            idAndNode.add(Pair(node?.id, node))
            node?.let {
                numericIdAndNode.add(Pair(node.numericId, node))

                for (mindmapNode in node.childMindmapNodes) {
                    stack.push(mindmapNode)
                }
            }

        }

        val newNodesById: MutableMap<String, MindmapNode> = HashMap(idAndNode.size)
        val newNodesByNumericId: MutableMap<Int, MindmapNode> = HashMap(numericIdAndNode.size)

        for (i in idAndNode) {
            newNodesById[i.first] = i.second
        }
        for (i in numericIdAndNode) {
            newNodesByNumericId[i.first] = i.second
        }

        return MindmapIndexes(newNodesById, newNodesByNumericId)
    }

    fun parseNodeTag(xpp: XmlPullParser, parentNode: MindmapNode?): MindmapNode {
        val id = xpp.getAttributeValue(null, "ID")
        val numericId = try {
            id.replace("\\D+".toRegex(), "").toInt()
        } catch (e: NumberFormatException) {
            id.hashCode()
        }

        val text = xpp.getAttributeValue(null, "TEXT")

        // get link
        val linkAttribute = xpp.getAttributeValue(null, "LINK")
        val link = if (linkAttribute != null && linkAttribute != "") {
            Uri.parse(linkAttribute)
        } else {
            null
        }

        // get tree ID (of cloned node)
        val treeIdAttribute = xpp.getAttributeValue(null, "TREE_ID")

        val newMindmapNode = MindmapNode(parentNode, id, numericId, text, link, treeIdAttribute)
        return newMindmapNode
    }


    fun openRichText(
        mindmapNode: MindmapNode,
        activity: FragmentActivity,
    ) {
        if(mindmapNode.richTextContents.isNotEmpty()){
            val richTextContent = mindmapNode.richTextContents.first()
            val intent = Intent(activity, RichTextViewActivity::class.java)
            intent.putExtra("richTextContent", richTextContent)
            activity.startActivity(intent)
        }
    }


    // if the link has a "#ID123", it's an internal link within the document
    private fun isInternalLink(node: MindmapNode?): Boolean =
        node?.link?.fragment != null && node.link.fragment?.startsWith("ID") == true

    /**
     * Opens the link of this node (if any)
     */
    fun openLink(
        mindmapNode: MindmapNode?,
        activity: FragmentActivity,
        viewModel: MainViewModel,
        onLinkBroken : (String?) -> Unit,
    ) {
        //TODO use something like timber
        // TODO: if link is internal, substring ID
        Log.d(MainApplication.TAG, """
Opening link (to string): ${mindmapNode?.link}
Opening link (fragment, everything after '#'): ${mindmapNode?.link?.fragment}
        """.trimIndent())

        if (isInternalLink(mindmapNode)) {
            openInternalFragmentLink(
                mindmapNode = mindmapNode,
                viewModel = viewModel,
                onLinkBroken = onLinkBroken,
            )
        } else {
            openIntentLink(
                mindmapNode = mindmapNode,
                mindmapDirectoryPath = viewModel.getMindmapDirectoryPath(),
                activity = activity
            )
        }
    }

    /**
     * Open this node's link as internal fragment
     */
    private fun openInternalFragmentLink(
        mindmapNode: MindmapNode?,
        viewModel: MainViewModel,
        onLinkBroken: (String?) -> Unit,
    ) {
        // internal link, so this.link is of the form "#ID_123234534" this.link.getFragment() should give everything
        // after the "#" it is null if there is no "#", which should be the case for all other links
        val fragment = mindmapNode?.link?.fragment
        val linkedInternal = viewModel.getNodeByID(fragment)

        if (linkedInternal != null) {
            Log.d(MainApplication.TAG, "Opening internal node, $linkedInternal, with ID: $fragment")

            // the internal linked node might be anywhere in the viewModel, i.e. on a completely separate branch than
            // we are on currently. We need to go to the Top, and then descend into the viewModel to reach the right
            // point
            viewModel.downTo(linkedInternal, true)
        } else {
            onLinkBroken(fragment)
        }
    }

    /**
     * Open this node's link as intent
     */
    private fun openIntentLink(
        mindmapNode: MindmapNode?,
        mindmapDirectoryPath: String?,
        activity: FragmentActivity,
    ) {
        // try opening the link normally with an intent
        try {
            val openUriIntent = Intent(ACTION_VIEW)
            openUriIntent.setData(mindmapNode?.link)
            activity.startActivity(openUriIntent)
            return
        } catch (e: ActivityNotFoundException) {
            Log.d(MainApplication.TAG, "ActivityNotFoundException when opening link as normal intent")
        }

        // try to open as relative file
        try {
            val fileName: String? = if (mindmapNode?.link?.path?.startsWith("/") == true) {
                // absolute filename
                mindmapNode.link.path
            } else {
                mindmapDirectoryPath + "/" + mindmapNode?.link?.path
            }
            fileName?.let {
                val file = File(fileName)
                if (!file.exists()) {
                    Toast.makeText(activity, "File $fileName does not exist.", Toast.LENGTH_SHORT).show()
                    Log.d(MainApplication.TAG, "File $fileName does not exist.")
                    return
                }
                if (!file.canRead()) {
                    Toast.makeText(activity, "Can not read file $fileName.", Toast.LENGTH_SHORT).show()
                    Log.d(MainApplication.TAG, "Can not read file $fileName.")
                    return
                }
                Log.d(MainApplication.TAG, "Opening file " + Uri.fromFile(file))
                // http://stackoverflow.com/a/3571239/1067124
                var extension = ""
                val i = fileName.lastIndexOf('.')
                val p = fileName.lastIndexOf('/')
                if (i > p) {
                    extension = fileName.substring(i + 1)
                }
                val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

                val intent = Intent()
                intent.setAction(ACTION_VIEW)
                intent.setDataAndType(Uri.fromFile(file), mime)
                activity.startActivity(intent)
            }
        } catch (e1: Exception) {
            Toast.makeText(activity, "No application found to open " + mindmapNode?.link, Toast.LENGTH_SHORT).show()
            e1.printStackTrace()
        }
    }
}