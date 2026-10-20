# Quickstart & Validation Guide: Auto Release Changelog & In-App Update Preview

**Date**: 2026-10-20
**Feature**: [spec.md](spec.md)
**Contracts**: [contracts/ui-contracts.md](contracts/ui-contracts.md)
**Data Model**: [data-model.md](data-model.md)

## Prerequisites

- JDK 21 configured (`java -version`)
- Android SDK with API 35 installed
- GitHub CLI (`gh`) for local release testing
- `git cliff` binary for local changelog generation (`cargo install git-cliff` or download from releases)
- Working directory: repository root

---

## Validation Scenarios

### 1. Verify Conventional Commit Parsing (Unit Tests)

Test the `ReleaseNoteParser` implementation in `:core` module.

```bash
./gradlew :core:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.core.util.ReleaseNoteParserTest"
```

**Expected Outcomes**:
- Valid conventional commits parse correctly into `ConventionalCommit` with proper type, scope, subject
- Non-conventional commits return `Result.failure` or parse as `OTHER`
- `parseReleaseNotes` groups commits by type in correct section order
- Generated markdown matches GitHub-flavored markdown spec

---

### 2. Verify GitHub Actions Release Workflow (Local Simulation)

Simulate the release workflow locally before pushing tags.

```bash
# 1. Ensure you have commits since last tag following conventional format
git log --oneline --since="$(git describe --tags --abbrev=0 HEAD^)"..HEAD

# 2. Generate changelog locally with git cliff
git cliff --tag v1.0.23 --previous-tag v1.0.22 --output CHANGELOG.md

# 3. Inspect generated CHANGELOG.md
cat CHANGELOG.md
```

**Expected Outcomes**:
- Sections appear in correct order: Features, Bug Fixes, Performance, Refactoring, Documentation, Tests, CI/CD, Build, Chores, Reverts, Other
- Each commit appears under correct section with scope and subject
- No duplicate or missing commits
- Markdown renders correctly on GitHub

---

### 3. Verify Commitlint Enforcement (PR Validation)

Test commit message validation locally.

```bash
# Install commitlint locally
npm install -g @commitlint/cli @commitlint/config-conventional

# Test valid commit
echo "feat(ui): add dark mode toggle" | commitlint

# Test invalid commit
echo "added dark mode" | commitlint  # Should fail
```

**Expected Outcomes**:
- Valid conventional commits pass
- Invalid format fails with clear error message listing expected format
- Scope is lowercase, subject non-empty, max 100 chars

---

### 4. Verify Update Dialog Changelog Display (Compose Preview + UI Test)

Test the enhanced update dialog with various changelog scenarios.

```bash
# Run Compose preview for UpdateDialog
./gradlew :app:compileDebugKotlin  # Verify compiles

# Run UI tests for update dialog
./gradlew :app:connectedAndroidTest --tests "fr.julien.quievreux.droidplane2.ui.components.UpdateDialogTest"
```

**Preview Scenarios** (in `UpdateDialogPreview.kt`):
1. **Normal changelog** — ~2000 chars, multiple sections
2. **Long changelog** — ~5000 chars, tests scrolling
3. **Empty changelog** — fallback message displayed
4. **Loading state** — progress indicator shown
5. **Error state** — error message + retry button
6. **Dark/Light themes** — both render correctly

**UI Test Assertions**:
- Dialog title shows "Update Available: v{version}"
- Changelog text is fully scrollable without truncation
- "Later" button dismisses dialog
- "Update Now" button triggers download intent
- Fallback message shown when changelog empty

---

### 5. Verify UpdateCheckerService Integration (Integration Test)

Test the enhanced `GitHubUpdateCheckerServiceImpl` with real GitHub API.

```bash
# Run integration test (requires network)
./gradlew :app:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.update.GitHubUpdateCheckerServiceImplTest"
```

**Test Cases**:
1. **Success** — Latest release fetched, `body` mapped to `releaseNotes`, APK URL extracted
2. **Empty body** — `releaseNotes` is null/empty, fallback handled
3. **Rate limit (403)** — Cached result returned if available
4. **Network error** — `Error` result with appropriate message
5. **No APK asset** — `Error` result
6. **Version skip** — v1.0.20 → v1.0.22 shows v1.0.22 changelog

**MockWebServer** test for deterministic results:
```kotlin
// In test: mock GitHub API response with body, assets, etc.
```

---

### 6. Verify End-to-End Release Flow (Manual)

Perform a test release to validate full automation.

```bash
# 1. Create test commits with conventional messages
git commit --allow-empty -m "feat(test): add test feature for release"
git commit --allow-empty -m "fix(test): fix test bug in parser"

# 2. Create and push test tag
git tag v1.0.99-test
git push origin v1.0.99-test

# 3. Observe GitHub Actions workflow run
# 4. Verify release created at: https://github.com/<owner>/droidplane/releases/tag/v1.0.99-test
# 5. Verify release body contains structured changelog
# 5. Verify APK artifact uploaded
```

**Expected Outcomes**:
- Workflow triggers on tag push
- `git cliff` runs successfully
- GitHub Release created with correct tag, title, body
- Changelog sections populated correctly
- APK attached as asset
- Clean up: delete test tag/release after verification

---

### 7. Verify In-App Update Flow (Manual Device Test)

Test the complete user-facing update flow.

```bash
# 1. Build and install current version on device
./gradlew :app:installDebug

# 2. Trigger update check (menu → Check for Updates)
# 3. Verify dialog appears with:
#    - Correct version numbers
#    - Full changelog scrollable
#    - "Later" dismisses dialog
#    - "Update Now" launches download intent
```

**Test with different scenarios**:
- Normal update available
- No update available (dialog not shown)
- Network offline (cached/error handling)

---

## Acceptance Criteria Checklist

| Scenario | SC Reference | Verified |
|----------|--------------|----------|
| Automated release notes on tag push | SC-001, SC-006 | ☐ |
| Structured sections (feat, fix, etc.) | SC-001 | ☐ |
| Update dialog shows full changelog | SC-002, SC-003 | ☐ |
| Changelog scrollable, no truncation | SC-002 | ☐ |
| "Update Now" / "Later" actions work | SC-003 | ☐ |
| Fallback for empty changelog | FR-007 | ☐ |
| Commitlint rejects invalid commits | SC-004 | ☐ |
| Update check + fetch < 3 seconds | SC-005 | ☐ |
| Version skip works (v1.0.20 → v1.0.22) | FR-008 | ☐ |

---

## Troubleshooting

| Issue | Resolution |
|-------|------------|
| `git cliff` not found | Install via `cargo install git-cliff` or download binary |
| Commitlint fails on valid commits | Check `.commitlintrc.json` matches conventional spec |
| GitHub API 403 rate limit | Wait 1 hour or use authenticated `GITHUB_TOKEN` |
| Release workflow doesn't trigger | Verify tag format `v*.*.*` and workflow file in `.github/workflows/` |
| Changelog not scrollable | Check `Modifier.verticalScroll` and max height constraint |
| APK not uploaded | Verify `app/build/outputs/apk/release/` path in workflow |

---

## Cleanup After Validation

```bash
# Delete test tag locally and remotely
git tag -d v1.0.99-test
git push origin :refs/tags/v1.0.99-test

# Delete test release on GitHub (via UI or CLI)
gh release delete v1.0.99-test --yes
```