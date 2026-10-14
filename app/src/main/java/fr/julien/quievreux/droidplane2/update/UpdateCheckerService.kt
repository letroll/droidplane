package fr.julien.quievreux.droidplane2.update

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result emitted by update checking operations.
 */
sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val release: GitHubRelease
    ) : UpdateCheckResult

    data class UpToDate(
        val currentVersion: String
    ) : UpdateCheckResult

    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : UpdateCheckResult
}

/**
 * Service querying GitHub releases for in-app update availability.
 */
interface UpdateCheckerService {
    suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult
}

/**
 * Default implementation querying GitHub REST API v3 using native HttpURLConnection.
 */
class GitHubUpdateCheckerServiceImpl(
    private val repoOwner: String = DEFAULT_OWNER,
    private val repoName: String = DEFAULT_REPO,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val jsonFetcher: (suspend (url: String) -> String)? = null
) : UpdateCheckerService {

    companion object {
        const val DEFAULT_OWNER = "letroll"
        const val DEFAULT_REPO = "droidplane"
        const val API_TIMEOUT_MS = 5000
    }

    override suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult = withContext(dispatcher) {
        val currentAppVersion = AppVersion.parse(currentVersion)
        val endpointUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

        try {
            val jsonBody = if (jsonFetcher != null) {
                jsonFetcher.invoke(endpointUrl)
            } else {
                fetchHttp(endpointUrl)
            }

            val release = GitHubRelease.fromJsonString(jsonBody)
            val releaseVersion = AppVersion.parse(release.tagName)

            if (releaseVersion > currentAppVersion) {
                UpdateCheckResult.UpdateAvailable(currentVersion, release)
            } else {
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            UpdateCheckResult.Error(e.message ?: "Network error checking for updates", e)
        }
    }

    private fun fetchHttp(endpointUrl: String): String {
        val url = URL(endpointUrl)
        val connection = url.openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("User-Agent", "Droidplane-App")
            connection.connectTimeout = API_TIMEOUT_MS
            connection.readTimeout = API_TIMEOUT_MS

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                throw NoSuchElementException("No releases found on repository")
            }
            if (responseCode !in 200..299) {
                throw IllegalStateException("GitHub API returned HTTP $responseCode")
            }

            connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}
