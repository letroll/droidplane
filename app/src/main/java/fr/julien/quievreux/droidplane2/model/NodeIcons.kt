package fr.julien.quievreux.droidplane2.model

import fr.julien.quievreux.droidplane2.R

enum class NodeIcons(
    val text: String,
    val resId: Int,
) {
    Full0("full-0", R.drawable.icon_full_0),
    Full1("full-1", R.drawable.icon_full_1),
    Full2("full-2", R.drawable.icon_full_2),
    Full3("full-3", R.drawable.icon_full_3),
    Full4("full-4", R.drawable.icon_full_4),
    Full5("full-5", R.drawable.icon_full_5),
    Full6("full-6", R.drawable.icon_full_6),
    Full7("full-7", R.drawable.icon_full_7),
    Full8("full-8", R.drawable.icon_full_8),
    Full9("full-9", R.drawable.icon_full_9),
}

fun getNodeIconsResIdFromName(text:String):Int? = NodeIcons.entries.firstOrNull {
    it.text == text
}?.resId