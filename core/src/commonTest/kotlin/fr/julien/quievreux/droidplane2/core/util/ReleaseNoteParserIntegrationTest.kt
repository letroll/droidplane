package fr.julien.quievreux.droidplane2.core.util

import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Integration tests for ReleaseNoteParserImpl using sample git history fixtures.
 * These tests verify the parser works correctly with realistic commit messages
 * that simulate a real git history between tags.
 */
class ReleaseNoteParserIntegrationTest : KStringSpec() {

    private val parser = ReleaseNoteParserImpl()

    init {
        "parseReleaseNotes integration: should parse multiple commits of different types between tags" {
            // Simulated commits from a realistic git history between v1.0.22 and v1.0.23
            val commits = listOf(
                "feat(ui): add dark mode toggle" to CommitType.FEAT,
                "fix(core): resolve crash on startup" to CommitType.FIX,
                "perf(mindmap): optimize layout calculation for large maps" to CommitType.PERF,
                "refactor(data): simplify NodeManager parsing logic" to CommitType.REFACTOR,
                "docs: update README with setup instructions" to CommitType.DOCS,
                "test(update): add unit tests for caching behavior" to CommitType.TEST,
                "ci: add commitlint validation to PR workflow" to CommitType.CI,
                "build: update gradle wrapper to 8.7" to CommitType.BUILD,
                "chore(deps): update kotlin to 2.1.0" to CommitType.CHORE,
                "revert: revert commit abc123" to CommitType.REVERT,
                "added feature without prefix" to CommitType.OTHER
            )

            val parsedCommits = commits.map { (msg, expectedType) ->
                val result = parser.parseCommit(
                    message = msg,
                    hash = "abcdef1234567890",
                    author = "Test Author",
                    date = 1700000000000
                )
                result.isSuccess shouldBe true
                result.getOrThrow().type shouldBe expectedType
                result.getOrThrow()
            }

            // Verify section order in ReleaseNote
            val sections = parsedCommits.groupBy { it.type }
            val releaseNote = ReleaseNote(
                version = "1.0.23",
                tagName = "v1.0.23",
                previousTag = "v1.0.22",
                generatedAt = System.currentTimeMillis(),
                sections = sections,
                markdown = "",
                hasChanges = true
            )

            val markdown = releaseNote.generateMarkdown()

            // Verify all sections appear in correct order
            val expectedOrder = listOf(
                "### ✨ Features",
                "### 🐛 Bug Fixes",
                "### ⚡ Performance",
                "### ♻️ Refactoring",
                "### 📚 Documentation",
                "### ✅ Tests",
                "### 🔧 CI/CD",
                "### 📦 Build",
                "### 🧹 Chores",
                "### ↩️ Reverts",
                "### 📝 Other Changes"
            )

            var lastIndex = -1
            for (section in expectedOrder) {
                val index = markdown.indexOf(section)
                if (index != -1) {
                    (index > lastIndex) shouldBe true
                    lastIndex = index
                }
            }

            // Verify each commit appears in its section
            markdown.shouldNotBe("")
            markdown.contains("add dark mode toggle") shouldBe true
            markdown.contains("resolve crash on startup") shouldBe true
            markdown.contains("optimize layout calculation") shouldBe true
            markdown.contains("simplify NodeManager parsing") shouldBe true
            markdown.contains("update README with setup") shouldBe true
            markdown.contains("add unit tests for caching") shouldBe true
            markdown.contains("add commitlint validation") shouldBe true
            markdown.contains("update gradle wrapper") shouldBe true
            markdown.contains("update kotlin to 2.1.0") shouldBe true
            markdown.contains("revert commit abc123") shouldBe true
            markdown.contains("added feature without prefix") shouldBe true
        }

        "parseReleaseNotes integration: should handle empty commit range (first release)" {
            val emptySections = mapOf<CommitType, List<ConventionalCommit>>()
            val releaseNote = ReleaseNote(
                version = "1.0.0",
                tagName = "v1.0.0",
                previousTag = null,
                generatedAt = System.currentTimeMillis(),
                sections = emptySections,
                markdown = "",
                hasChanges = false
            )

            val markdown = releaseNote.generateMarkdown()
            markdown shouldBe "No changes since previous release."
        }

        "parseReleaseNotes integration: should handle commits with body and footer" {
            val fullMessage = """feat(api): add new REST endpoint

This commit adds a new REST endpoint for fetching user preferences.
The endpoint supports query parameters for filtering and pagination.

Closes #456
BREAKING CHANGE: API response format changed"""

            val result = parser.parseCommit(
                message = fullMessage,
                hash = "hash123",
                author = "Dev",
                date = 1700000000000
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.type shouldBe CommitType.FEAT
            commit.scope shouldBe "api"
            commit.body shouldNotBe null
            commit.body!!.contains("REST endpoint") shouldBe true
            commit.footer shouldNotBe null
            commit.footer!!.contains("Closes #456") shouldBe true
            commit.footer!!.contains("BREAKING CHANGE") shouldBe true
        }

        "parseReleaseNotes integration: should handle scope normalization edge cases" {
            val testCases = listOf(
                "feat(  UI  ): test" to "ui",
                "fix(CORE): test" to "core",
                "perf(Data-Model): test" to "data-model",
                "docs(  ): test" to null, // empty scope should become null
                "refactor(): test" to null // empty scope should become null
            )

            testCases.forEach { (message, expectedScope) ->
                val result = parser.parseCommit(
                    message = message,
                    hash = "hash",
                    author = "Author",
                    date = 0
                )
                result.isSuccess shouldBe true
                result.getOrThrow().scope shouldBe expectedScope
            }
        }

        "parseReleaseNotes integration: should handle subject length limits" {
            val longSubject = "a".repeat(150)
            val result = parser.parseCommit(
                message = "feat: $longSubject",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            result.getOrThrow().subject.length shouldBe 100
        }

        "parseReleaseNotes integration: should handle commits with special characters in subject" {
            val messages = listOf(
                "feat: add support for emoji 🎉 in titles",
                "fix: handle null pointer exception in parser",
                "docs: update README.md with #header and [link](url)",
                "chore: update deps (kotlin, compose, hilt)"
            )

            messages.forEach { msg ->
                val result = parser.parseCommit(
                    message = msg,
                    hash = "hash",
                    author = "Author",
                    date = 0
                )
                result.isSuccess shouldBe true
                result.getOrThrow().subject shouldNotBe ""
            }
        }

        "parseReleaseNotes integration: should correctly parse merge commits as OTHER" {
            val mergeCommits = listOf(
                "Merge branch 'feature/dark-mode' into main",
                "Merge pull request #123 from user/feature",
                "Merge remote-tracking branch 'origin/develop'"
            )

            mergeCommits.forEach { msg ->
                val result = parser.parseCommit(
                    message = msg,
                    hash = "hash",
                    author = "Author",
                    date = 0
                )
                result.isSuccess shouldBe true
                result.getOrThrow().type shouldBe CommitType.OTHER
            }
        }

        "parseReleaseNotes integration: should handle breaking change footer" {
            val messages = listOf(
                "feat(api): change response format\n\nBREAKING CHANGE: response now includes pagination",
                "fix: critical security fix\n\nBREAKING CHANGE: removed deprecated endpoint"
            )

            messages.forEach { msg ->
                val result = parser.parseCommit(
                    message = msg,
                    hash = "hash",
                    author = "Author",
                    date = 0
                )
                result.isSuccess shouldBe true
                val commit = result.getOrThrow()
                commit.footer shouldNotBe null
                commit.footer!!.contains("BREAKING CHANGE") shouldBe true
            }
        }

        "parseReleaseNotes integration: should generate correct markdown with commit hashes" {
            val commits = listOf(
                "feat(ui): add button" to "abcdef1234567890",
                "fix: resolve issue" to "1234567890abcdef"
            )

            val parsedCommits = commits.map { (msg, hash) ->
                parser.parseCommit(message = msg, hash = hash, author = "Author", date = 0).getOrThrow()
            }

            val sections = parsedCommits.groupBy { it.type }
            val releaseNote = ReleaseNote(
                version = "1.0.1",
                tagName = "v1.0.1",
                previousTag = "v1.0.0",
                generatedAt = System.currentTimeMillis(),
                sections = sections,
                markdown = "",
                hasChanges = true
            )

            val markdown = releaseNote.generateMarkdown()
            markdown.contains("(abcdef1)") shouldBe true
            markdown.contains("(1234567)") shouldBe true
        }
    }
}