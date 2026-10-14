package fr.julien.quievreux.droidplane2.update

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class UpdateCheckerServiceTest {

    private val testDispatcher = StandardTestDispatcher()

    private val sampleReleaseJson = """
        {
          "tag_name": "v1.0.25",
          "name": "Release v1.0.25",
          "published_at": "2026-10-14T10:00:00Z",
          "html_url": "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
          "body": "Bug fixes and performance improvements",
          "draft": false,
          "prerelease": false,
          "assets": [
            {
              "name": "output-metadata.json",
              "browser_download_url": "https://github.com/letroll/droidplane/releases/download/v1.0.25/output-metadata.json",
              "size": 1024
            },
            {
              "name": "app-release.apk",
              "browser_download_url": "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
              "size": 15728640
            }
          ]
        }
    """.trimIndent()

    @Test
    fun `checkForUpdates returns UpdateAvailable when remote release is newer`() = runTest(testDispatcher) {
        val service = GitHubUpdateCheckerServiceImpl(
            dispatcher = testDispatcher,
            jsonFetcher = { sampleReleaseJson }
        )

        val result = service.checkForUpdates(currentVersion = "1.0.18")
        val available = assertIs<UpdateCheckResult.UpdateAvailable>(result)

        assertEquals("1.0.18", available.currentVersion)
        assertEquals("v1.0.25", available.release.tagName)
        assertEquals("Release v1.0.25", available.release.name)
        assertEquals("Bug fixes and performance improvements", available.release.body)
        assertEquals("https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk", available.release.apkDownloadUrl)
        assertEquals("app-release.apk", available.release.apkFileName)
        assertEquals(15728640L, available.release.apkSize)
    }

    @Test
    fun `checkForUpdates returns UpToDate when remote release matches current app version`() = runTest(testDispatcher) {
        val service = GitHubUpdateCheckerServiceImpl(
            dispatcher = testDispatcher,
            jsonFetcher = { sampleReleaseJson }
        )

        val result = service.checkForUpdates(currentVersion = "1.0.25")
        val upToDate = assertIs<UpdateCheckResult.UpToDate>(result)
        assertEquals("1.0.25", upToDate.currentVersion)
    }

    @Test
    fun `checkForUpdates returns UpToDate when remote release is older than current version`() = runTest(testDispatcher) {
        val service = GitHubUpdateCheckerServiceImpl(
            dispatcher = testDispatcher,
            jsonFetcher = { sampleReleaseJson }
        )

        val result = service.checkForUpdates(currentVersion = "1.0.30")
        assertIs<UpdateCheckResult.UpToDate>(result)
    }

    @Test
    fun `checkForUpdates parses release without apk asset correctly`() = runTest(testDispatcher) {
        val jsonNoApk = """
            {
              "tag_name": "v1.0.25",
              "name": "Release v1.0.25",
              "published_at": "2026-10-14T10:00:00Z",
              "html_url": "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
              "body": "No assets attached",
              "assets": []
            }
        """.trimIndent()

        val service = GitHubUpdateCheckerServiceImpl(
            dispatcher = testDispatcher,
            jsonFetcher = { jsonNoApk }
        )

        val result = service.checkForUpdates(currentVersion = "1.0.18")
        val available = assertIs<UpdateCheckResult.UpdateAvailable>(result)
        assertNull(available.release.apkDownloadUrl)
    }

    @Test
    fun `checkForUpdates returns Error on network failure`() = runTest(testDispatcher) {
        val service = GitHubUpdateCheckerServiceImpl(
            dispatcher = testDispatcher,
            jsonFetcher = { throw IOException("Connection reset by peer") }
        )

        val result = service.checkForUpdates(currentVersion = "1.0.18")
        val error = assertIs<UpdateCheckResult.Error>(result)
        assertNotNull(error.cause)
    }
}
