package fr.julien.quievreux.droidplane2.data.model

data class MindMapUri(val value: String) {
    val scheme: String?
        get() = value.substringBefore(":", missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() && !it.startsWith("#") && !it.contains("/") }

    val fragment: String?
        get() = value.substringAfter("#", missingDelimiterValue = "")
            .takeIf { value.contains("#") }

    val lastPathSegment: String?
        get() = value.substringBefore("#")
            .substringBefore("?")
            .trimEnd('/')
            .substringAfterLast('/', missingDelimiterValue = "")
            .takeIf { it.isNotEmpty() }

    val path: String?
        get() {
            val withoutFrag = value.substringBefore("#").substringBefore("?")
            val s = scheme
            return if (s != null) {
                val afterScheme = withoutFrag.removePrefix("$s:")
                if (afterScheme.startsWith("//")) {
                    val afterHost = afterScheme.removePrefix("//")
                    if (s == "file") {
                        afterHost
                    } else {
                        val slashIndex = afterHost.indexOf('/')
                        if (slashIndex >= 0) afterHost.substring(slashIndex) else ""
                    }
                } else {
                    afterScheme
                }
            } else {
                withoutFrag
            }
        }

    fun isInternalLink(): Boolean = scheme == null && !fragment.isNullOrBlank()

    override fun toString(): String = value

    companion object {
        fun parse(uriString: String): MindMapUri = MindMapUri(uriString)
        fun fromFile(file: java.io.File): MindMapUri = MindMapUri(file.toURI().toString())
    }
}
