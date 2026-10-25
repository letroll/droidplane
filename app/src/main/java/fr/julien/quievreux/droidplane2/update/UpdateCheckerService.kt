package fr.julien.quievreux.droidplane2.update

import fr.julien.quievreux.droidplane2.core.log.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Result emitted by update checking operations.
 */
sealed interface UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val release: GitHubRelease
    ) : UpdateCheckResult {
        /** Convenience property returning the release body (changelog) from GitHub. */
        val releaseNotes: String? get() = release.body
    }

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
    suspend fun checkForUpdates(currentVersion: String, forceRefresh: Boolean): UpdateCheckResult
}

/**
 * Default implementation querying GitHub REST API v3 using native HttpURLConnection.
 */
class GitHubUpdateCheckerServiceImpl(
    private val repoOwner: String = DEFAULT_OWNER,
    private val repoName: String = DEFAULT_REPO,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val jsonFetcher: (suspend (url: String) -> String)? = null,
    private val logger: Logger = Logger.Default
) : UpdateCheckerService {

    companion object {
        const val DEFAULT_OWNER = "letroll"
        const val DEFAULT_REPO = "droidplane"
        const val API_TIMEOUT_MS = 5000
        /** Cache TTL: 1 hour in milliseconds */
        const val CACHE_TTL_MS = 60 * 60 * 1000L
    }

    // In-memory cache for release data
    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val release: GitHubRelease,
        val timestamp: Long
    )

    override suspend fun checkForUpdates(currentVersion: String): UpdateCheckResult = withContext(dispatcher) {
        checkForUpdatesInternal(currentVersion, forceRefresh = false)
    }

    override suspend fun checkForUpdates(currentVersion: String, forceRefresh: Boolean): UpdateCheckResult = withContext(dispatcher) {
        checkForUpdatesInternal(currentVersion, forceRefresh)
    }

    private suspend fun checkForUpdatesInternal(currentVersion: String, forceRefresh: Boolean): UpdateCheckResult = withContext(dispatcher) {
        val currentAppVersion = AppVersion.parse(currentVersion)
        val endpointUrl = "https://api.github.com/repos/$repoOwner/$repoName/releases/latest"

        logger.d("Checking for updates: current=$currentVersion (parsed=${currentAppVersion.parts}), forceRefresh=$forceRefresh")

        // Try to get cached release data first (unless force refresh)
        val cachedEntry = cache["latest-release"]
        val hasValidCache = if (!forceRefresh) {
            cachedEntry?.let {
                System.currentTimeMillis() - it.timestamp < CACHE_TTL_MS
            } ?: false
        } else false

        // If cache is valid and not forcing refresh, return cached data immediately
        if (hasValidCache) {
            val release = cachedEntry!!.release
            val releaseVersion = AppVersion.parse(release.tagName)
            logger.d("Using cached release: tag=${release.tagName}, version=$releaseVersion")
            return@withContext if (releaseVersion > currentAppVersion) {
                logger.d("Update available: $releaseVersion > $currentAppVersion")
                UpdateCheckResult.UpdateAvailable(currentVersion, release)
            } else {
                logger.d("Up to date: $releaseVersion <= $currentAppVersion")
                UpdateCheckResult.UpToDate(currentVersion)
            }
        }

        // Cache expired, not present, or force refresh - fetch fresh data
        logger.d("Fetching fresh release data from GitHub API")
        return@withContext try {
            val jsonBody = if (jsonFetcher != null) {
                jsonFetcher.invoke(endpointUrl)
            } else {
                fetchHttp(endpointUrl)
            }

            val release = GitHubRelease.fromJsonString(jsonBody)
            // Update cache with fresh data
            cache["latest-release"] = CacheEntry(release, System.currentTimeMillis())

            val releaseVersion = AppVersion.parse(release.tagName)
            logger.d("Fetched release: tag=${release.tagName}, parsedVersion=$releaseVersion, current=$currentAppVersion")

            if (releaseVersion > currentAppVersion) {
                logger.d("Update available: $releaseVersion > $currentAppVersion")
                UpdateCheckResult.UpdateAvailable(currentVersion, release)
            } else {
                logger.d("Up to date: $releaseVersion <= $currentAppVersion")
                UpdateCheckResult.UpToDate(currentVersion)
            }
        } catch (e: Exception) {
            // On error, try to serve stale cache if available (even if expired)
            val staleEntry = cache["latest-release"]
            val hasStaleCache = staleEntry != null

            logger.w("UpdateChecker", "Error checking for updates: ${e.message}, hasStaleCache=$hasStaleCache")

            if (hasStaleCache) {
                val staleRelease = staleEntry!!.release
                val staleVersion = AppVersion.parse(staleRelease.tagName)
                logger.d("Using stale cache: tag=${staleRelease.tagName}, version=$staleVersion")
                if (staleVersion > currentAppVersion) {
                    UpdateCheckResult.UpdateAvailable(currentVersion, staleRelease)
                } else {
                    UpdateCheckResult.UpToDate(currentVersion)
                }
            } else {
                UpdateCheckResult.Error(e.message ?: "Network error checking for updates", e)
            }
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

