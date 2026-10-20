# Data Model: Auto Release Changelog & In-App Update Preview

**Date**: 2026-10-20
**Feature**: [spec.md](spec.md)
**Research**: [research.md](research.md)

## Entities

### 1. ConventionalCommit

Represents a single parsed commit following conventional commit format.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `type` | `CommitType` | Yes | Category of change (feat, fix, chore, etc.) |
| `scope` | `String?` | No | Optional scope (e.g., "ui", "core", "data") |
| `subject` | `String` | Yes | Brief description of change |
| `body` | `String?` | No | Optional detailed explanation |
| `footer` | `String?` | No | Optional breaking change or issue refs |
| `hash` | `String` | Yes | Full commit SHA |
| `shortHash` | `String` | Yes | Abbreviated commit SHA (7 chars) |
| `author` | `String` | Yes | Commit author name |
| `date` | `Long` | Yes | Commit timestamp (epoch ms) |

**CommitType Enum**:
```kotlin
enum class CommitType {
    FEAT,       // New feature
    FIX,        // Bug fix
    CHORE,      // Maintenance, no functional change
    DOCS,       // Documentation only
    REFACTOR,   // Code restructuring
    PERF,       // Performance improvement
    TEST,       // Test additions/changes
    CI,         // CI/CD configuration
    BUILD,      // Build system changes
    REVERT,     // Reverts previous commit
    OTHER       // Non-conventional or unrecognized
}
```

---

### 2. ReleaseNote

Auto-generated structured release note content for a GitHub release.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `version` | `String` | Yes | Semantic version (e.g., "1.0.23") |
| `tagName` | `String` | Yes | Git tag name (e.g., "v1.0.23") |
| `previousTag` | `String?` | No | Previous release tag for range |
| `generatedAt` | `Long` | Yes | Generation timestamp (epoch ms) |
| `sections` | `Map<CommitType, List<ConventionalCommit>>` | Yes | Commits grouped by type |
| `markdown` | `String` | Yes | Full rendered markdown for GitHub release body |
| `hasChanges` | `Boolean` | Yes | True if any commits in range |

**Sections Order** (for consistent output):
1. FEAT → "### ✨ Features"
2. FIX → "### 🐛 Bug Fixes"
3. PERF → "### ⚡ Performance"
4. REFACTOR → "### ♻️ Refactoring"
5. DOCS → "### 📚 Documentation"
6. TEST → "### ✅ Tests"
7. CI → "### 🔧 CI/CD"
8. BUILD → "### 📦 Build"
9. CHORE → "### 🧹 Chores"
10. REVERT → "### ↩️ Reverts"
11. OTHER → "### 📝 Other Changes"

---

### 3. GitHubRelease (Extended)

Existing data class extended with release body (changelog).

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `tagName` | `String` | Yes | Release tag (e.g., "v1.0.23") |
| `name` | `String` | Yes | Release title |
| `body` | `String?` | No | **NEW** — Changelog markdown from GitHub |
| `publishedAt` | `String` | Yes | ISO 8601 publish timestamp |
| `htmlUrl` | `String` | Yes | Release page URL |
| `assets` | `List<ReleaseAsset>` | Yes | APK/download assets |
| `prerelease` | `Boolean` | Yes | Pre-release flag |
| `draft` | `Boolean` | Yes | Draft flag |

**ReleaseAsset** (unchanged):
| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `name` | `String` | Yes | Asset filename |
| `browserDownloadUrl` | `String` | Yes | Direct download URL |
| `size` | `Long` | Yes | File size in bytes |
| `contentType` | `String` | Yes | MIME type |

---

### 4. UpdateDialogState

UI state for the enhanced update dialog with changelog.

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| `showDialog` | `Boolean` | Yes | Whether dialog is visible |
| `currentVersion` | `String` | Yes | App's current version |
| `latestVersion` | `String` | Yes | Latest version from GitHub |
| `changelog` | `String?` | No | Release body (markdown) from GitHub |
| `downloadUrl` | `String?` | No | APK download URL |
| `isLoading` | `Boolean` | Yes | Fetching release data |
| `error` | `String?` | No | Error message if fetch failed |

---

### 5. ReleaseNoteParser (Pure Function)

Interface for parsing git log into structured release notes.

```kotlin
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
    fun parseCommit(message: String, hash: String, author: String, date: Long): Result<ConventionalCommit>
}
```

**Implementation**: `ReleaseNoteParserImpl` in `:core` using `git cliff` CLI or pure Kotlin parsing.

---

### 6. UpdateCheckResult (Extended)

Extended with changelog data.

```kotlin
sealed class UpdateCheckResult {
    data class UpdateAvailable(
        val currentVersion: String,
        val latestVersion: String,
        val downloadUrl: String,
        val releaseNotes: String?,        // NEW: GitHub release body
        val releaseTag: String,
        val publishedAt: String
    ) : UpdateCheckResult()
    
    data class NoUpdateAvailable(val currentVersion: String) : UpdateCheckResult()
    data class Error(val message: String) : UpdateCheckResult()
}
```

---

## State Transitions

### UpdateDialog Lifecycle

```
Hidden → Loading (fetch release) → Loaded (show dialog with changelog)
                ↓ Error                         ↓ User Action
            Error State                   Update Now / Later
                ↓ Retry                         ↓
            Loading ──────────────────────→ Hidden
```

### Release Generation (CI)

```
Tag Push (v*.*.*) 
    → Checkout repo with full history
    → Determine previous tag (git describe --tags --abbrev=0 HEAD^)
    → Run git cliff --from <prev> --to <current>
    → Create/Update GitHub Release with generated body
    → Upload APK artifact
```

---

## Validation Rules

1. **CommitType parsing**: Unknown types → `OTHER`
2. **Scope normalization**: Lowercase, trim, max 20 chars
3. **Subject**: Trim, max 100 chars, no trailing period
4. **ReleaseNote.markdown**: Must be valid GitHub-flavored markdown
5. **GitHubRelease.body**: Null/empty → fallback in UI
6. **UpdateCheckResult.releaseNotes**: Mirrors GitHubRelease.body