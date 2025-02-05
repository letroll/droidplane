package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.TYPE
import fr.julien.quievreux.droidplane2.data.model.NodeType
import fr.julien.quievreux.droidplane2.data.model.RichContentType.*
import org.xmlpull.v1.XmlPullParser

fun XmlPullParser.isRichContent(): Boolean = name == "richcontent" && (
        getAttributeValue(null, TYPE.name) == NODE.text ||
        getAttributeValue(null, TYPE.name) == NOTE.text ||
        getAttributeValue(null, TYPE.name) == DETAILS.text
    )

fun XmlPullParser.isIcon(): Boolean = name == NodeType.Icon.value && getAttributeValue(null, "BUILTIN") != null