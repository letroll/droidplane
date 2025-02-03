package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.Node
import org.xmlpull.v1.XmlPullParser
import java.util.Stack

interface XmlParseUtils {
    // extract the richcontent (HTML) of the node. This works both for nodes with a rich text content
    // (TYPE="NODE"), for "Notes" (TYPE="NOTE"), for "Details" (TYPE="DETAILS").

    // if this is an empty tag, we won't need to bother trying to read its content
    // we don't even need to read the <richcontent> node's attributes, as we would
    // only be interested in it's children
    fun parseRichContent(xpp: XmlPullParser, nodes: MutableList<Node>)

    fun parseFont(xpp: XmlPullParser, nodes: MutableList<Node>)

    fun parseArrowLink(xpp: XmlPullParser, nodes: MutableList<Node>)

    fun parseIcon(xpp: XmlPullParser, nodes: MutableList<Node>)

}