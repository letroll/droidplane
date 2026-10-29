package fr.julien.quievreux.droidplane2.model

data class RecentFile(
    val uriString: String,
    val displayName: String,
    val timestamp: Long = System.currentTimeMillis(),
)
