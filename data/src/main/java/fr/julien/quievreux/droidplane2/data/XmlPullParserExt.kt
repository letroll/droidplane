package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.NodeAttribute
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.BUILTIN
import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.TYPE
import fr.julien.quievreux.droidplane2.data.model.NodeTag.RICH_CONTENT
import fr.julien.quievreux.droidplane2.data.model.NodeType
import fr.julien.quievreux.droidplane2.data.model.RichContentType.*
import org.xmlpull.v1.XmlPullParser

fun XmlPullParser.isRichContent(): Boolean = name == RICH_CONTENT.text && (
        getNodeAttribute(TYPE) == NODE.text ||
        getNodeAttribute(TYPE) == NOTE.text ||
        getNodeAttribute(TYPE) == DETAILS.text
    )

fun XmlPullParser.isIcon(): Boolean = name == NodeType.Icon.value && getNodeAttribute(BUILTIN) != null