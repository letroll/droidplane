package fr.julien.quievreux.droidplane2.data

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.provider.OpenableColumns
import fr.julien.quievreux.droidplane2.model.RecentFile
import java.io.File

interface RecentFilesRepository {
    fun getRecentFiles(checker: (String) -> Boolean = { true }): List<RecentFile>
    fun addRecentFile(uriString: String, displayName: String)
    fun removeRecentFile(uriString: String)
    fun clear()
}

class RecentFilesRepositoryImpl(
    private val preferences: SharedPreferences,
) : RecentFilesRepository {

    companion object {
        private const val PREF_KEY_RECENT_FILES = "recent_mindmap_files"
        private const val MAX_RECENTS = 8
        private const val DELIMITER = "\u001F"
    }

    override fun getRecentFiles(checker: (String) -> Boolean): List<RecentFile> {
        val raw = preferences.getString(PREF_KEY_RECENT_FILES, "") ?: ""
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
        preferences.edit().remove(PREF_KEY_RECENT_FILES).apply()
    }

    private fun saveEntries(entries: List<RecentFile>) {
        val encoded = entries.joinToString("\n") {
            "${it.uriString}$DELIMITER${it.displayName}$DELIMITER${it.timestamp}"
        }
        preferences.edit().putString(PREF_KEY_RECENT_FILES, encoded).apply()
    }
}

fun getFileNameFromUri(context: Context, uri: Uri): String {
    if (uri.scheme == "content") {
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val name = cursor.getString(nameIndex)
                    if (!name.isNullOrBlank()) return name
                }
            }
        } catch (_: Exception) {}
    }
    return uri.lastPathSegment?.substringAfterLast('/')?.ifBlank { null } ?: "mindmap.mm"
}

fun createStorageFileChecker(context: Context): (String) -> Boolean = { uriString ->
    try {
        val uri = Uri.parse(uriString)
        when (uri.scheme) {
            "file" -> {
                uri.path?.let { File(it).exists() } ?: false
            }
            "content" -> {
                var streamAccessible = false
                try {
                    context.contentResolver.openInputStream(uri)?.use {
                        streamAccessible = true
                    }
                } catch (_: Exception) {}

                if (streamAccessible) {
                    true
                } else {
                    val hasPersistedPermission = context.contentResolver.persistedUriPermissions.any {
                        it.uri == uri && it.isReadPermission
                    }
                    if (hasPersistedPermission) {
                        try {
                            context.contentResolver.query(uri, null, null, null, null)?.use {
                                it.moveToFirst()
                            } ?: false
                        } catch (_: Exception) {
                            false
                        }
                    } else {
                        false
                    }
                }
            }
            else -> false
        }
    } catch (_: Exception) {
        false
    }
}
