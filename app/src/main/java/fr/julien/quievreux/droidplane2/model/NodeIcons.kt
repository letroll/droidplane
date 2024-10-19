package fr.julien.quievreux.droidplane2.model

import androidx.compose.ui.graphics.vector.ImageVector
import compose.icons.FontAwesomeIcons
import compose.icons.TablerIcons
import compose.icons.fontawesomeicons.Solid
import compose.icons.fontawesomeicons.solid.Edit
import compose.icons.fontawesomeicons.solid.Link
import compose.icons.fontawesomeicons.solid.Paperclip
import compose.icons.tablericons.Circle0
import compose.icons.tablericons.Circle1
import compose.icons.tablericons.Circle2
import compose.icons.tablericons.Circle3
import compose.icons.tablericons.Circle4
import compose.icons.tablericons.Circle5
import compose.icons.tablericons.Circle6
import compose.icons.tablericons.Circle7
import compose.icons.tablericons.Circle8
import compose.icons.tablericons.Circle9
import fr.julien.quievreux.droidplane2.R

enum class NodeIcons(
    val text: String,
    val resId: Int,
    val imageVector: ImageVector,
) {
    //TODO bind all icon
    //TODO remove all drawable
    Attach("attach", R.drawable.icon_attach, FontAwesomeIcons.Solid.Paperclip),
    Full0("full-0", R.drawable.icon_full_0, TablerIcons.Circle0),
    Full1("full-1", R.drawable.icon_full_1, TablerIcons.Circle1),
    Full2("full-2", R.drawable.icon_full_2, TablerIcons.Circle2),
    Full3("full-3", R.drawable.icon_full_3, TablerIcons.Circle3),
    Full4("full-4", R.drawable.icon_full_4, TablerIcons.Circle4),
    Full5("full-5", R.drawable.icon_full_5, TablerIcons.Circle5),
    Full6("full-6", R.drawable.icon_full_6, TablerIcons.Circle6),
    Full7("full-7", R.drawable.icon_full_7, TablerIcons.Circle7),
    Full8("full-8", R.drawable.icon_full_8, TablerIcons.Circle8),
    Full9("full-9", R.drawable.icon_full_9, TablerIcons.Circle9),
    RicheText("riche-text", R.drawable.richtext, FontAwesomeIcons.Solid.Edit),
    Link("link", R.drawable.link, FontAwesomeIcons.Solid.Link),
}

fun getNodeIconsResIdFromName(iconName:String):Int? = NodeIcons.entries.firstOrNull {
    it.text == iconName
}?.resId

fun getNodeFontIconsFromName(iconName: String): ImageVector? = NodeIcons.entries.firstOrNull {
    it.text == iconName
}?.imageVector
