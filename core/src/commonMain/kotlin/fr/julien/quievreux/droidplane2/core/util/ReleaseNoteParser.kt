package fr.julien.quievreux.droidplane2.core.util

import java.util.regex.Pattern

/**
 * Interface for parsing git log into structured release notes.
 */
interface ReleaseNoteParser {

    /**
     * Parses commits between two tags into structured release note.
     * @param fromTag Previous release tag (null = all commits from beginning)
     * @param toTag Current release tag
     * @return ReleaseNote with categorized commits and markdown
     */
    fun parseReleaseNotes(
        fromTag: String?,
        toTag: String
    ): Result<ReleaseNote>

    /**
     * Parses a single commit message into ConventionalCommit.
     * @return Parsed commit or failure if not conventional
     */
    fun parseCommit(
        message: String,
        hash: String,
        author: String,
        date: Long
    ): Result<ConventionalCommit>
}

/**
 * Represents a single parsed commit following conventional commit format.
 *
 * @property type Category of change (feat, fix, chore, etc.)
 * @property scope Optional scope (e.g., "ui", "core", "data")
 * @property subject Brief description of change
 * @property body Optional detailed explanation
 * @property footer Optional breaking change or issue refs
 * @property hash Full commit SHA
 * @property shortHash Abbreviated commit SHA (7 chars)
 * @property author Commit author name
 * @property date Commit timestamp (epoch ms)
 */
data class ConventionalCommit(
    val type: CommitType,
    val scope: String?,
    val subject: String,
    val body: String?,
    val footer: String?,
    val hash: String,
    val shortHash: String,
    val author: String,
    val date: Long
) {
    /**
     * Returns a display-friendly message with scope if present.
     */
    fun displayMessage(): String {
        return if (scope != null && scope.isNotBlank()) {
            "$subject ($scope)"
        } else {
            subject
        }
    }
}

/**
 * Enum representing conventional commit types.
 * Order defines the display order in generated changelogs.
 */
enum class CommitType(val displayName: String, val emoji: String) {
    FEAT("Features", "✨"),
    FIX("Bug Fixes", "🐛"),
    PERF("Performance", "⚡"),
    REFACTOR("Refactoring", "♻️"),
    DOCS("Documentation", "📚"),
    TEST("Tests", "✅"),
    CI("CI/CD", "🔧"),
    BUILD("Build", "📦"),
    CHORE("Chores", "🧹"),
    REVERT("Reverts", "↩️"),
    OTHER("Other Changes", "📝");

    companion object {
        /**
         * Parses a commit type string into CommitType enum.
         * Unknown types default to OTHER.
         */
        fun parse(typeStr: String): CommitType {
            return values().firstOrNull { it.name.lowercase() == typeStr.lowercase() } ?: OTHER
        }
    }
}

/**
 * Auto-generated structured release note content for a GitHub release.
 *
 * @property version Semantic version (e.g., "1.0.23")
 * @property tagName Git tag name (e.g., "v1.0.23")
 * @property previousTag Previous release tag for range
 * @property generatedAt Generation timestamp (epoch ms)
 * @property sections Commits grouped by CommitType
 * @property markdown Full rendered markdown for GitHub release body
 * @property hasChanges True if any commits in range
 */
data class ReleaseNote(
    val version: String,
    val tagName: String,
    val previousTag: String?,
    val generatedAt: Long,
    val sections: Map<CommitType, List<ConventionalCommit>>,
    val markdown: String,
    val hasChanges: Boolean
) {

    /**
     * Default section order for consistent output.
     */
    companion object {
        val SECTION_ORDER = listOf(
            CommitType.FEAT,
            CommitType.FIX,
            CommitType.PERF,
            CommitType.REFACTOR,
            CommitType.DOCS,
            CommitType.TEST,
            CommitType.CI,
            CommitType.BUILD,
            CommitType.CHORE,
            CommitType.REVERT,
            CommitType.OTHER
        )
    }

    /**
     * Generates markdown from sections.
     */
    fun generateMarkdown(): String {
        val sb = StringBuilder()
        for (type in SECTION_ORDER) {
            val commits = sections[type] ?: emptyList()
            if (commits.isNotEmpty()) {
                sb.appendLine("### ${type.emoji} ${type.displayName}\n")
                for (commit in commits) {
                    sb.appendLine("- ${commit.displayMessage()} (${commit.shortHash})")
                }
                sb.appendLine()
            }
        }
        if (!hasChanges) {
            sb.appendLine("No changes since previous release.")
        }
        return sb.toString().trim()
    }
}

/**
 * Pure Kotlin implementation of ReleaseNoteParser using regex-based conventional commit parsing.
 * This implementation is cross-platform and testable without external dependencies.
 */
class ReleaseNoteParserImpl : ReleaseNoteParser {

    private val commitPattern = Pattern.compile(
        """^(?<type>feat|fix|chore|docs|refactor|perf|test|ci|build|revert)(?:\((?<scope>[^)]+)\))?: (?<subject>.+)$""",
        Pattern.MULTILINE
    )

    private val bodyFooterPattern = Pattern.compile(
        """(?s)^.+\n\n(?<body>.+?)(?:\n\n(?<footer>.+))?$"""
    )

    override fun parseReleaseNotes(
        fromTag: String?,
        toTag: String
    ): Result<ReleaseNote> {
        // This implementation would typically shell out to `git log` or use a git library
        // For now, return empty result as this requires git operations
        return Result.failure(UnsupportedOperationException("parseReleaseNotes requires git operations - use git cliff in CI"))
    }

    override fun parseCommit(
        message: String,
        hash: String,
        author: String,
        date: Long
    ): Result<ConventionalCommit> {
        val trimmedMessage = message.trim()
        val firstLine = trimmedMessage.lineSequence().firstOrNull()?.trim() ?: ""

        // Try to match conventional commit format on the first line
        val matcher = commitPattern.matcher(firstLine)
        if (!matcher.matches()) {
            // Non-conventional commit - parse as OTHER
            return Result.success(ConventionalCommit(
                type = CommitType.OTHER,
                scope = null,
                subject = trimmedMessage.take(100),
                body = null,
                footer = null,
                hash = hash,
                shortHash = hash.take(7),
                author = author,
                date = date
            ))
        }

        val typeStr = matcher.group("type") ?: ""
        val scope = matcher.group("scope")
        val subject = matcher.group("subject") ?: ""

        // Parse body and footer from full message if present
        var body: String? = null
        var footer: String? = null
        val paragraphs = trimmedMessage.split(Regex("""\n\s*\n""")).drop(1)
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (paragraphs.size == 1) {
            val paragraph = paragraphs[0]
            if (isFooterParagraph(paragraph)) {
                footer = paragraph
            } else {
                body = paragraph
            }
        } else if (paragraphs.size >= 2) {
            body = paragraphs.dropLast(1).joinToString("\n\n")
            footer = paragraphs.last()
        }

        val commitType = CommitType.parse(typeStr)
        val normalizedScope = scope?.lowercase()?.trim()?.takeIf { it.isNotBlank() }?.take(20)

        return Result.success(ConventionalCommit(
            type = commitType,
            scope = normalizedScope,
            subject = subject.trim().take(100).trimEnd('.'),
            body = body,
            footer = footer,
            hash = hash,
            shortHash = hash.take(7),
            author = author,
            date = date
        ))
    }

    private fun isFooterParagraph(paragraph: String): Boolean {
        return paragraph.startsWith("BREAKING CHANGE:", ignoreCase = true) ||
            paragraph.startsWith("BREAKING-CHANGE:", ignoreCase = true) ||
            paragraph.startsWith("Closes #", ignoreCase = true) ||
            paragraph.startsWith("Fixes #", ignoreCase = true) ||
            paragraph.startsWith("Refs #", ignoreCase = true)
    }
}