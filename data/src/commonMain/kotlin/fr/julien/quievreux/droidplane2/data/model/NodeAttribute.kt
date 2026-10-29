package fr.julien.quievreux.droidplane2.data.model

enum class NodeAttribute(val text: String) {
    BACKGROUND_COLOR("BACKGROUND_COLOR"),
    BOLD("BOLD"),
    BUILTIN("BUILTIN"),
    COLOR("COLOR"),
    CREATED("CREATED"),
    DESTINATION("DESTINATION"),
    EDGE_LIKE("EDGE_LIKE"),
    ENDARROW("ENDARROW"),
    ENDINCLINATION("ENDINCLINATION"),
    EQUATION("EQUATION"),
    FOLDED("FOLDED"),
    HGAP("HGAP"),
    ID("ID"),
    ITALIC("ITALIC"),
    LINK("LINK"),
    MIDDLE_LABEL("MIDDLE_LABEL"),
    MODIFIED("MODIFIED"),
    NAME("NAME"),
    POSITION("POSITION"),
    SHAPE("SHAPE"),
    SIZE("SIZE"),
    SOURCE_LABEL("SOURCE_LABEL"),
    STARTARROW("STARTARROW"),
    STARTINCLINATION("STARTINCLINATION"),
    STYLE("STYLE"),
    TARGET_LABEL("TARGET_LABEL"),
    TEXT("TEXT"),
    TREE_ID("TREE_ID"),
    TYPE("TYPE"),
    URI("URI"),
    VALUE("VALUE"),
    VGAP("VGAP"),
    VSHIFT("VSHIFT"),
    WIDTH("WIDTH");

    companion object {
        fun fromString(text: String): NodeAttribute? = entries.firstOrNull { it.text.equals(text, ignoreCase = true) }
    }
}
