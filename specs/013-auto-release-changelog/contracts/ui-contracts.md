# UI Contracts: Auto Release Changelog & In-App Update Preview

**Date**: 2026-10-20
**Feature**: [spec.md](spec.md)
**Data Model**: [data-model.md](data-model.md)

## Update Dialog Contract

### Component: `UpdateDialog` (or `UpdateAvailableDialog`)

**Location**: `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/UpdateDialog.kt`

**Responsibility**: Display update availability with full changelog, handle user actions.

### Props / Inputs

| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| `currentVersion` | `String` | Yes | App's current version name |
| `latestVersion` | `String` | Yes | Latest version from GitHub |
| `changelog` | `String?` | No | Release body (markdown) from GitHub |
| `onUpdateNow` | `() -> Unit` | Yes | Callback for "Update Now" action |
| `onLater` | `() -> Unit` | Yes | Callback for "Later" action |
| `onRetry` | `() -> Unit` | No | Callback for retry on error |
| `isLoading` | `Boolean` | Yes | Show loading state |
| `error` | `String?` | No | Error message if fetch failed |

### Behavior Specification

#### Loading State
- Show circular progress indicator centered
- Text: "Checking for updates..."
- Disable all interactions

#### Error State
- Show error icon + message
- Show "Retry" button if `onRetry` provided
- Show "Dismiss" button always

#### Loaded State (Update Available)
- **Title**: "Update Available: v{latestVersion}"
- **Subtitle**: "Current version: v{currentVersion}"
- **Changelog Section**:
  - Label: "What's New"
  - Content: Scrollable text (max height 60% screen height, min 200dp)
  - Render: Plain text (markdown not rendered, shown as-is) — *simpler, safer*
  - Fallback if empty/null: "No changelog provided for this release."
- **Actions** (horizontal, bottom):
  - "Later" — outlined button, calls `onLater`
  - "Update Now" — filled button, primary color, calls `onUpdateNow`

#### Loaded State (No Update)
- Dialog not shown (handled by caller)

### Visual Specs

- **Container**: Material 3 `AlertDialog` or custom `Dialog` with `Surface`
- **Width**: 90% screen width, max 560dp
- **Changelog Text**: `Text` with `style = MaterialTheme.typography.bodyMedium`, `Modifier.verticalScroll(rememberScrollState())`
- **Scroll Indicator**: Fade edge at bottom when content exceeds max height
- **Dark/Light**: Full theme support via `MaterialTheme.colorScheme`

### Accessibility

- Changelog content: `semantics { heading() }` for screen readers
- Buttons: `contentDescription` for actions
- Scrollable area: Announce scroll position changes

---

## GitHub Actions Workflow Contract

### Workflow: `release.yml`

**Location**: `.github/workflows/release.yml`

**Triggers**:
```yaml
on:
  push:
    tags:
      - 'v*.*.*'
```

### Jobs

#### `generate-release-notes`
- **Runs on**: `ubuntu-latest`
- **Steps**:
  1. Checkout with `fetch-depth: 0` (full history for git cliff)
  2. Determine previous tag: `git describe --tags --abbrev=0 HEAD^` (or empty for first release)
  3. Install `git cliff` (via `cargo install git-cliff` or download binary)
  4. Run `git cliff --tag <current> --previous-tag <prev> --output CHANGELOG.md`
  5. Read `CHANGELOG.md` content
  6. Create/update GitHub Release via `gh release create` or REST API
     - Tag: `${{ github.ref_name }}`
     - Title: "Release ${{ github.ref_name }}"
     - Body: `<changelog content>`
     - Draft: `false`
     - Prerelease: `false`
  7. Upload APK artifact (from `app/build/outputs/apk/release/*.apk`)

#### `validate-commits` (separate workflow or job)
- **Trigger**: `pull_request`
- **Steps**:
  1. Checkout with `fetch-depth: 0`
  2. Run `commitlint` on commit range `${{ github.event.pull_request.base.sha }}..${{ github.sha }}`
  3. Fail if any commit invalid

### Inputs/Outputs

| Job | Input | Output |
|-----|-------|--------|
| `generate-release-notes` | Tag name, previous tag | GitHub Release created, CHANGELOG.md artifact |
| `validate-commits` | PR commit range | Pass/Fail |

---

## UpdateCheckerService Contract

### Interface: `UpdateCheckerService`

**Location**: `app/src/main/java/fr/julien/quievreux/droidplane2/update/UpdateCheckerService.kt`

### Methods

```kotlin
interface UpdateCheckerService {
    /**
     * Checks for app updates on GitHub.
     * @return UpdateCheckResult with latest release info including changelog
     */
    suspend fun checkForUpdate(): UpdateCheckResult
}
```

### Implementation: `GitHubUpdateCheckerServiceImpl`

**GitHub API Endpoint**: `GET https://api.github.com/repos/{owner}/{repo}/releases/latest`

**Request Headers**:
- `Accept: application/vnd.github.v3+json`
- `User-Agent: Droidplane-UpdateChecker`

**Response Mapping**:
| GitHub API Field | UpdateCheckResult Field |
|------------------|------------------------|
| `tag_name` | `latestVersion` (strip 'v' prefix) |
| `name` | `releaseTag` |
| `body` | `releaseNotes` (changelog) |
| `published_at` | `publishedAt` |
| `assets[].browser_download_url` (first `.apk`) | `downloadUrl` |
| `html_url` | (for "View on GitHub" link) |

**Error Handling**:
- 404 → `Error("No releases found")`
- 403 rate limit → `Error("Rate limited, try later")` + serve cached
- Network error → `Error("Network error")`
- No APK asset → `Error("No download available")`

**Caching**:
- In-memory cache with 1-hour TTL keyed by "latest-release"
- On rate limit or network error, return cached `UpdateAvailable` if exists

---

## ReleaseNoteParser Contract

### Interface: `ReleaseNoteParser`

**Location**: `core/src/main/java/fr/julien/quievreux/droidplane2/core/util/ReleaseNoteParser.kt`

### Methods

```kotlin
interface ReleaseNoteParser {
    fun parseReleaseNotes(fromTag: String?, toTag: String): Result<ReleaseNote>
    fun parseCommit(message: String, hash: String, author: String, date: Long): Result<ConventionalCommit>
}
```

### Implementation: `ReleaseNoteParserImpl`

**Option A**: Shell out to `git cliff` binary (bundled or installed)
- Pros: Full git cliff features, battle-tested
- Cons: Binary dependency, platform-specific

**Option B**: Pure Kotlin conventional commit parser
- Pros: No external deps, cross-platform, testable
- Cons: Re-implements parsing logic

**Decision**: **Option B** — Pure Kotlin parser for `:core` module portability. Use regex-based parsing matching conventional commit spec. `git cliff` reserved for CI where binary is available.

**Parsing Rules**:
```
<type>(<scope>): <subject>
[blank line]
<body>
[blank line]
<footer>
```

Types: `feat`, `fix`, `chore`, `docs`, `refactor`, `perf`, `test`, `ci`, `build`, `revert`
Scope: optional, alphanumeric + hyphen
Subject: required, no trailing period

---

## Commitlint Config Contract

### File: `.commitlintrc.json`

```json
{
  "extends": ["@commitlint/config-conventional"],
  "rules": {
    "type-enum": [2, "always", ["feat", "fix", "chore", "docs", "refactor", "perf", "test", "ci", "build", "revert"]],
    "scope-case": [2, "always", "lower-case"],
    "subject-empty": [2, "never"],
    "subject-max-length": [2, "always", 100],
    "header-max-length": [2, "always", 120]
  }
}
```

### GitHub Actions Integration

```yaml
# .github/workflows/commitlint.yml
name: Commitlint
on: [pull_request]
jobs:
  lint:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
        with: { fetch-depth: 0 }
      - uses: wagoid/commitlint-github-action@v6
```