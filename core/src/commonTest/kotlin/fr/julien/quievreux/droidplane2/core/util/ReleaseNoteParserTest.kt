package fr.julien.quievreux.droidplane2.core.util

import fr.julien.quievreux.droidplane2.core.testutils.KStringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class ReleaseNoteParserTest : KStringSpec() {

    private val parser = ReleaseNoteParserImpl()

    init {
        "parseCommit should parse feat commit with scope" {
            val result = parser.parseCommit(
                message = "feat(ui): add dark mode toggle",
                hash = "abcdef1234567890",
                author = "John Doe",
                date = 1700000000000
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.type shouldBe CommitType.FEAT
            commit.scope shouldBe "ui"
            commit.subject shouldBe "add dark mode toggle"
            commit.hash shouldBe "abcdef1234567890"
            commit.shortHash shouldBe "abcdef1"
            commit.author shouldBe "John Doe"
            commit.date shouldBe 1700000000000
            commit.body shouldBe null
            commit.footer shouldBe null
        }

        "parseCommit should parse fix commit without scope" {
            val result = parser.parseCommit(
                message = "fix: resolve crash on startup",
                hash = "1234567890abcdef",
                author = "Jane Smith",
                date = 1700000000001
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.type shouldBe CommitType.FIX
            commit.scope shouldBe null
            commit.subject shouldBe "resolve crash on startup"
        }

        "parseCommit should parse chore commit with scope" {
            val result = parser.parseCommit(
                message = "chore(deps): update kotlin to 2.1.0",
                hash = "fedcba0987654321",
                author = "Bot",
                date = 1700000000002
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.type shouldBe CommitType.CHORE
            commit.scope shouldBe "deps"
            commit.subject shouldBe "update kotlin to 2.1.0"
        }

        "parseCommit should parse all commit types" {
            val types = mapOf(
                "feat" to CommitType.FEAT,
                "fix" to CommitType.FIX,
                "chore" to CommitType.CHORE,
                "docs" to CommitType.DOCS,
                "refactor" to CommitType.REFACTOR,
                "perf" to CommitType.PERF,
                "test" to CommitType.TEST,
                "ci" to CommitType.CI,
                "build" to CommitType.BUILD,
                "revert" to CommitType.REVERT
            )

            types.forEach { (typeStr, expectedType) ->
                val result = parser.parseCommit(
                    message = "$typeStr: test commit",
                    hash = "hash",
                    author = "Author",
                    date = 0
                )
                result.isSuccess shouldBe true
                result.getOrThrow().type shouldBe expectedType
            }
        }

        "parseCommit should handle unknown type as OTHER" {
            val result = parser.parseCommit(
                message = "random: this is not conventional",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            result.getOrThrow().type shouldBe CommitType.OTHER
        }

        "parseCommit should handle non-conventional commit as OTHER" {
            val result = parser.parseCommit(
                message = "added new feature without prefix",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.type shouldBe CommitType.OTHER
            commit.scope shouldBe null
            commit.subject shouldBe "added new feature without prefix"
        }

        "parseCommit should normalize scope to lowercase and trim" {
            val result = parser.parseCommit(
                message = "feat(  UI  ): test scope normalization",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            result.getOrThrow().scope shouldBe "ui"
        }

        "parseCommit should truncate scope to 20 characters" {
            val longScope = "this-is-a-very-long-scope-name"
            val result = parser.parseCommit(
                message = "feat($longScope): test",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            result.getOrThrow().scope shouldBe longScope.take(20)
        }

        "parseCommit should truncate subject to 100 characters and trim trailing period" {
            val longSubject = "a".repeat(105) + "."
            val result = parser.parseCommit(
                message = "feat: $longSubject",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.subject.length shouldBe 100
            commit.subject.endsWith(".") shouldBe false
        }

        "parseCommit should extract body and footer from full message" {
            val fullMessage = """feat: add new feature

This is the body explaining the feature in detail.

Closes #123""".trimIndent()

            val result = parser.parseCommit(
                message = fullMessage,
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.body shouldNotBe null
            commit.body!!.shouldBe("This is the body explaining the feature in detail.")
            commit.footer shouldNotBe null
            commit.footer!!.shouldBe("Closes #123")
        }

        "parseCommit should handle commit without body or footer" {
            val result = parser.parseCommit(
                message = "feat: simple commit",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            val commit = result.getOrThrow()
            commit.body shouldBe null
            commit.footer shouldBe null
        }

        "parseCommit should generate correct shortHash from hash" {
            val result = parser.parseCommit(
                message = "feat: test",
                hash = "abcdef1234567890",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            result.getOrThrow().shortHash shouldBe "abcdef1"
        }

        "parseCommit displayMessage should include scope when present" {
            val result = parser.parseCommit(
                message = "feat(ui): test",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            result.getOrThrow().displayMessage() shouldBe "test (ui)"
        }

        "parseCommit displayMessage should not include scope when null" {
            val result = parser.parseCommit(
                message = "feat: test",
                hash = "hash",
                author = "Author",
                date = 0
            )

            result.isSuccess shouldBe true
            result.getOrThrow().displayMessage() shouldBe "test"
        }

        "CommitType.parse should handle case insensitive types" {
            CommitType.parse("FEAT") shouldBe CommitType.FEAT
            CommitType.parse("Fix") shouldBe CommitType.FIX
            CommitType.parse("chore") shouldBe CommitType.CHORE
        }

        "CommitType.parse should return OTHER for unknown type" {
            CommitType.parse("unknown") shouldBe CommitType.OTHER
        }

        "CommitType enum should have correct display names and emojis" {
            CommitType.FEAT.displayName shouldBe "Features"
            CommitType.FEAT.emoji shouldBe "✨"
            CommitType.FIX.displayName shouldBe "Bug Fixes"
            CommitType.FIX.emoji shouldBe "🐛"
            CommitType.OTHER.displayName shouldBe "Other Changes"
            CommitType.OTHER.emoji shouldBe "📝"
        }

        "ConventionalCommit should have correct shortHash from hash" {
            val commit = ConventionalCommit(
                type = CommitType.FEAT,
                scope = null,
                subject = "test",
                body = null,
                footer = null,
                hash = "abcdef1234567890",
                shortHash = "abcdef1",
                author = "Author",
                date = 0
            )
            commit.shortHash shouldBe "abcdef1"
        }
    }
}