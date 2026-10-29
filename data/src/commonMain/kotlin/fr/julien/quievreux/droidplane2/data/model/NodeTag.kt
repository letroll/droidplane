package fr.julien.quievreux.droidplane2.data.model

enum class NodeTag(val text: String) {
    ARROWLINK("arrowlink"),
    ATTRIBUTE("attribute"),
    ATTRIBUTE_NAME("attribute_name"),
    ATTRIBUTE_REGISTRY("attribute_registry"),
    ATTRIBUTE_VALUE("attribute_value"),
    CLOUD("cloud"),
    EDGE("edge"),
    FONT("font"),
    HOOK("hook"),
    ICON("icon"),
    MAP("map"),
    NODE("node"),
    PARAMETERS("Parameters"),
    RICH_CONTENT("richcontent");

    companion object {
        fun fromString(text: String): NodeTag? = entries.firstOrNull { it.text.equals(text, ignoreCase = true) }
    }
}
