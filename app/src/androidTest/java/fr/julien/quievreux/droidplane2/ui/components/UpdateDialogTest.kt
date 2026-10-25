package fr.julien.quievreux.droidplane2.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import fr.julien.quievreux.droidplane2.ui.dialog.UpdateAvailableDialog
import fr.julien.quievreux.droidplane2.ui.theme.ContrastAwareReplyTheme
import fr.julien.quievreux.droidplane2.update.GitHubRelease
import org.junit.Rule
import org.junit.Test

class UpdateDialogTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun normalChangelog(): String =
        "• Support for Freeplane node icons\n• In-app GitHub update checker\n• Bug fixes and UI improvements"

    private fun longChangelog(): String =
        (1..50).map { "• Feature $it: Added new functionality for testing scrolling behavior" }.joinToString("\n")

    @Test
    fun updateDialogShowsTitleAndVersionInfo() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = normalChangelog(),
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                        apkFileName = "app-release.apk"
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Update Available: v1.0.25").assertIsDisplayed()
        composeRule.onNodeWithText("Current: 1.0.18 • New: v1.0.25").assertIsDisplayed()
    }

    @Test
    fun updateDialogShowsChangelogWithNormalContent() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = normalChangelog(),
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                        apkFileName = "app-release.apk"
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("What's new:").assertIsDisplayed()
        composeRule.onNodeWithText("Support for Freeplane node icons").assertIsDisplayed()
        composeRule.onNodeWithText("In-app GitHub update checker").assertIsDisplayed()
        composeRule.onNodeWithText("Bug fixes and UI improvements").assertIsDisplayed()
    }

    @Test
    fun updateDialogShowsScrollableChangelogForLongContent() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = longChangelog(),
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                        apkFileName = "app-release.apk"
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("What's new:").assertIsDisplayed()
        composeRule.onNodeWithText("Feature 1: Added new functionality for testing scrolling behavior").assertIsDisplayed()
        composeRule.onNodeWithText("Feature 50: Added new functionality for testing scrolling behavior").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun updateDialogShowsFallbackForEmptyChangelog() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = "",
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                        apkFileName = "app-release.apk"
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("What's new:").assertIsDisplayed()
        composeRule.onNodeWithText("No changelog provided for this release.").assertIsDisplayed()
    }

    @Test
    fun updateDialogShowsDownloadAndInstallButtonWhenApkAvailable() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = normalChangelog(),
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                        apkFileName = "app-release.apk"
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Download & Install").assertIsDisplayed()
        composeRule.onNodeWithText("View on GitHub").assertIsDisplayed()
        composeRule.onNodeWithText("Later").assertIsDisplayed()
    }

    @Test
    fun updateDialogShowsViewOnGithubWhenNoApk() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = "Bug fixes only",
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = null,
                        apkFileName = null
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Download & Install").assertIsDisplayed()
        composeRule.onNodeWithText("View on GitHub").assertIsDisplayed()
        composeRule.onNodeWithText("Later").assertIsDisplayed()
    }

    @Test
    fun updateDialogDarkThemeRendersCorrectly() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = "• Dark mode support added\n• Bug fixes",
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                        apkFileName = "app-release.apk"
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Update Available: v1.0.25").assertIsDisplayed()
        composeRule.onNodeWithText("Dark mode support added").assertIsDisplayed()
        composeRule.onNodeWithText("Bug fixes").assertIsDisplayed()
        composeRule.onNodeWithText("Download & Install").assertIsDisplayed()
        composeRule.onNodeWithText("Later").assertIsDisplayed()
    }

    @Test
    fun updateDialogLightThemeRendersCorrectly() {
        composeRule.setContent {
            ContrastAwareReplyTheme {
                UpdateAvailableDialog(
                    currentVersion = "1.0.18",
                    release = GitHubRelease(
                        tagName = "v1.0.25",
                        name = "Release v1.0.25",
                        publishedAt = "2026-10-14",
                        body = normalChangelog(),
                        htmlUrl = "https://github.com/letroll/droidplane/releases/tag/v1.0.25",
                        apkDownloadUrl = "https://github.com/letroll/droidplane/releases/download/v1.0.25/app-release.apk",
                        apkFileName = "app-release.apk"
                    ),
                    onDownloadAndInstall = { _, _ -> },
                    onViewOnGitHub = {},
                    onDismiss = {}
                )
            }
        }

        composeRule.onNodeWithText("Update Available: v1.0.25").assertIsDisplayed()
        composeRule.onNodeWithText("What's new:").assertIsDisplayed()
        composeRule.onNodeWithText("Download & Install").assertIsDisplayed()
        composeRule.onNodeWithText("Later").assertIsDisplayed()
    }
}