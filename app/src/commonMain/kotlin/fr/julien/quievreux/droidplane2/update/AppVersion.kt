package fr.julien.quievreux.droidplane2.update

/**
 * Normalized semantic application version for numerical comparisons.
 * Parses version strings like "1.0.18", "v1.0.25", "v1.0.25-beta".
 */
data class AppVersion(
    val parts: List<Int>,
    val raw: String
) : Comparable<AppVersion> {

    override fun compareTo(other: AppVersion): Int {
        val maxLen = maxOf(parts.size, other.parts.size)
        for (i in 0 until maxLen) {
            val p1 = parts.getOrElse(i) { 0 }
            val p2 = other.parts.getOrElse(i) { 0 }
            if (p1 != p2) return p1.compareTo(p2)
        }
        return 0
    }

    companion object {
        fun parse(versionStr: String): AppVersion {
            val cleaned = versionStr.trim()
                .removePrefix("v")
                .removePrefix("V")
                .substringBefore('-')
                .substringBefore('+')
            val parts = cleaned.split('.').mapNotNull { it.toIntOrNull() }
            return AppVersion(parts, versionStr)
        }
    }
}
