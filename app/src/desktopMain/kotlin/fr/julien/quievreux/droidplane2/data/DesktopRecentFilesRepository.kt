package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.data.model.MindMapUri
import fr.julien.quievreux.droidplane2.model.RecentFile
import java.io.File
import java.util.prefs.Preferences

class DesktopRecentFilesRepository(
    private val preferences: Preferences = Preferences.userNodeForPackage(DesktopRecentFilesRepository::class.java),
) : RecentFilesRepository {

    companion object {
        private const val PREF_KEY_RECENT_FILES = "recent_mindmap_files"
        private const val MAX_RECENTS = 8
        private const val DELIMITER = "\u001F"
    }

    override fun getRecentFiles(checker: (String) -> Boolean): List<RecentFile> {
        val raw = preferences.get(PREF_KEY_RECENT_FILES, "") ?: ""
        if (raw.isBlank()) return emptyList()

        val entries = raw.split("\n").mapNotNull { line ->
            val parts = line.split(DELIMITER)
            if (parts.size >= 3) {
                RecentFile(
                    uriString = parts[0],
                    displayName = parts[1],
                    timestamp = parts[2].toLongOrNull() ?: 0L,
                )
            } else {
                null
            }
        }

        return entries.filter { checker(it.uriString) }.sortedByDescending { it.timestamp }
    }

    override fun addRecentFile(uriString: String, displayName: String) {
        val current = getRecentFiles(checker = { true }).toMutableList()
        current.removeAll { it.uriString == uriString }
        current.add(0, RecentFile(uriString, displayName, System.currentTimeMillis()))
        val trimmed = current.take(MAX_RECENTS)
        saveEntries(trimmed)
    }

    override fun removeRecentFile(uriString: String) {
        val current = getRecentFiles(checker = { true }).toMutableList()
        current.removeAll { it.uriString == uriString }
        saveEntries(current)
    }

    override fun clear() {
        preferences.remove(PREF_KEY_RECENT_FILES)
    }

    private fun saveEntries(entries: List<RecentFile>) {
        val encoded = entries.joinToString("\n") {
            "${it.uriString}$DELIMITER${it.displayName}$DELIMITER${it.timestamp}"
        }
        preferences.put(PREF_KEY_RECENT_FILES, encoded)
    }
}

fun createDesktopFileChecker(): (String) -> Boolean = { uriString ->
    try {
        val uri = MindMapUri.parse(uriString)
        val path = uri.path ?: uriString
        File(path).exists()
    } catch (_: Exception) {
        false
    }
}
