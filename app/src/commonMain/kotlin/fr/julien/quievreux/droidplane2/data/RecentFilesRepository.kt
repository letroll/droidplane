package fr.julien.quievreux.droidplane2.data

import fr.julien.quievreux.droidplane2.model.RecentFile

interface RecentFilesRepository {
    fun getRecentFiles(checker: (String) -> Boolean = { true }): List<RecentFile>
    fun addRecentFile(uriString: String, displayName: String)
    fun removeRecentFile(uriString: String)
    fun clear()
}
