package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.NodeAttribute.TYPE
import org.xmlpull.v1.XmlPullParser

fun XmlPullParser.isRichContent():Boolean = (name == "richcontent"
    && (getAttributeValue(null, TYPE.name) == "NODE"
    || getAttributeValue(null, TYPE.name) == "NOTE"
    || getAttributeValue(null, TYPE.name) == "DETAILS"
    ))