# Phase 0 Research: Auto Release Changelog & In-App Update Preview

**Date**: 2026-10-20
**Feature**: [spec.md](spec.md)

## Research Tasks

### 1. GitHub Actions Release Note Generation

**Decision**: Use `git cliff` or `release-drafter` or custom script for conventional commit parsing in GitHub Actions.

**Rationale**: 
- `git cliff` is a Rust-based tool that generates changelogs from git history with conventional commit support. It's fast, configurable, and can run in GitHub Actions.
- `release-drafter` is a GitHub Action that maintains a draft release with auto-updated changelog. Good but less flexible for custom formatting.
- Custom script (Node.js/Python) gives full control but adds maintenance burden.

**Selected**: **`git cliff`** — mature, actively maintained, supports conventional commits out of the box, configurable via TOML, runs as Docker image or binary in GitHub Actions. No Node.js/Python runtime needed in CI.

**Alternatives considered**:
- `release-drafter` — simpler but less customizable output format
- `semantic-release` — full release automation (version bump, npm publish) — overkill for Android APK releases
- Custom Kotlin script — would require JVM in CI, slower

**Configuration**: 
- `.cliff.toml` in repo root defining commit groups, template, and output format
- GitHub Actions workflow triggers on tag push (`v*.*.*`), runs `git cliff` to generate notes, creates/updates GitHub release

---

### 2. Conventional Commit Format Enforcement

**Decision**: Use `commitlint` with `@commitlint/config-conventional` in GitHub Actions (via `actions/github-script` or dedicated action) to validate PR commit messages.

**Rationale**: 
- `commitlint` is the standard for conventional commit validation
- Runs fast in CI, provides clear error messages
- Can be run on PR commits via `pull_request` trigger

**Configuration**:
- `.commitlintrc.json` extending `@commitlint/config-conventional`
- GitHub Actions workflow on `pull_request` runs `commitlint` on commit range
- Fail PR if any commit doesn't match format

**Alternatives considered**:
- Husky pre-commit hooks — local only, not enforceable on CI
- Custom regex validation — brittle, incomplete

---

### 3. GitHub API Release Body Fetching

**Decision**: Enhance existing `GitHubUpdateCheckerServiceImpl` to fetch full release object including `body` field from GitHub REST API `/repos/{owner}/{repo}/releases/latest`.

**Rationale**: 
- Existing service already calls GitHub API for latest release
- GitHub REST API v3 returns `body` (markdown) and `body_html` fields
- No additional authentication needed for public repos (rate limit 5000/hr)
- For private repos, would need `GITHUB_TOKEN` — but Droidplane is public

**Implementation**:
- Extend `GitHubRelease` data class with `body: String?` field
- Parse `body` as markdown or plain text for display
- Handle null/empty body with fallback message

---

### 4. In-App Changelog Display

**Decision**: Update `UpdateDialog` (or create `UpdateAvailableDialog` if not exists) to show scrollable changelog text with "Update Now" / "Later" buttons.

**Rationale**: 
- Existing update dialog likely shows version info only
- Jetpack Compose `Text` in `Column` with `Modifier.verticalScroll(rememberScrollState())` for scrolling
- Material 3 `AlertDialog` or custom `Dialog` composable
- Max height constraint to prevent full-screen takeover

**UI Specs**:
- Dialog title: "Update Available: v{version}"
- Changelog section: scrollable, max height 60% screen height
- Buttons: "Later" (dismiss), "Update Now" (trigger download)
- Fallback: "No changelog provided for this release." when body is empty

---

### 5. GitHub API Rate Limit Handling

**Decision**: Implement simple in-memory cache with TTL (e.g., 1 hour) for release metadata in `UpdateCheckerService`. On rate limit (403 with `X-RateLimit-Remaining: 0`), serve cached data or show version-only dialog.

**Rationale**: 
- 5000 req/hour is generous for update checks
- Cache prevents repeated calls during app session
- Graceful degradation maintains UX

---

### 6. First Release / No Previous Tag Handling

**Decision**: For first release (no previous tag), `git cliff` can generate from all commits using `--unreleased` or repository init commit. Configure in `.cliff.toml`.

**Rationale**: 
- `git cliff` supports `unreleased = true` to include all commits since beginning
- Or set `first_release = "commit-hash"` in config

---

## Consolidated Decisions

| Area | Decision | Key Config |
|------|----------|------------|
| Release note generator | `git cliff` | `.cliff.toml`, GitHub Actions workflow |
| Commit validation | `commitlint` | `.commitlintrc.json`, PR workflow |
| Release body fetch | Extend `GitHubUpdateCheckerServiceImpl` | Add `body` to `GitHubRelease` |
| Changelog display | Enhanced `UpdateDialog` in Compose | Scrollable text, 60% max height |
| Rate limiting | In-memory TTL cache | 1-hour TTL, fallback to cache |
| First release | `git cliff --unreleased` | Config in `.cliff.toml` |

## Next Steps

Proceed to Phase 1: Design data models (`data-model.md`), contracts (`contracts/`), and quickstart guide (`quickstart.md`).