package fr.julien.quievreux.droidplane2.data.util

object HtmlTextExtractor {
    private val ignoredSectionRegex = Regex("<(head|style|script)[^>]*>[\\s\\S]*?</\\1>", RegexOption.IGNORE_CASE)
    private val blockTagRegex = Regex("</?(p|div|br|li|tr|h[1-6])[^>]*>", RegexOption.IGNORE_CASE)
    private val tagRegex = Regex("<[^>]+>")
    private val whitespaceRegex = Regex("[ \\t\\x0B\\f\\r]+")
    private val multiNewlineRegex = Regex("\\n{3,}")

    fun extractPlainText(html: String): String {
        if (html.isBlank()) return ""
        val withoutIgnored = ignoredSectionRegex.replace(html, "")
        val withNewlines = blockTagRegex.replace(withoutIgnored) { match ->
            val tag = match.value.lowercase()
            if (tag.startsWith("<br") || tag.startsWith("</p") || tag.startsWith("</div") || tag.startsWith("</li") || tag.startsWith("</tr") || tag.startsWith("</h")) {
                "\n"
            } else {
                " "
            }
        }
        val withoutTags = tagRegex.replace(withNewlines, "")
        val decoded = decodeHtmlEntities(withoutTags)
        return decoded
            .lines()
            .joinToString("\n") { whitespaceRegex.replace(it, " ").trim() }
            .replace(multiNewlineRegex, "\n\n")
            .trim()
    }

    private fun decodeHtmlEntities(input: String): String {
        return input
            .replace("&nbsp;", " ")
            .replace("&#160;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace(Regex("&#(\\d+);")) { matchResult ->
                val code = matchResult.groupValues[1].toIntOrNull()
                if (code != null) code.toChar().toString() else matchResult.value
            }
            .replace(Regex("&#x([0-9a-fA-F]+);")) { matchResult ->
                val code = matchResult.groupValues[1].toIntOrNull(16)
                if (code != null) code.toChar().toString() else matchResult.value
            }
    }
}
