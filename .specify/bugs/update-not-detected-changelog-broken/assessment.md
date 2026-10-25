# Bug Assessment: Update Not Detected & Changelog Broken

- **Slug**: update-not-detected-changelog-broken
- **Created**: 2026-10-25
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

User reports two issues:
1. **Update not detected**: User has version 1.0.26 installed, version 1.0.27 is available on GitHub but the app does not offer the update.
2. **Changelog broken**: The changelog on GitHub releases shows the fallback text "APK de release signé généré automatiquement suite au push sur master." instead of proper changelog generated from conventional commits.

**User clarification**: v1.0.27 is a full release (not draft/prerelease). The commits did NOT follow conventional commit format. The GitHub workflows ran without errors.

## Symptom

1. **Update not detected**: User running version 1.0.26 does not receive update notification when version 1.0.27 is available on GitHub. The update check mechanism fails to detect the newer version.
2. **Changelog broken**: GitHub releases display the fallback message "APK de release signé généré automatiquement suite au push sur master." instead of a proper changelog generated from conventional commits using `git cliff`.

## Reproduction

1. **Update not detected**:
   1. Install app version 1.0.26
   2. Ensure version 1.0.27 exists as a GitHub release (tag `v1.0.27`)
   3. Trigger update check in app (menu → Check for Updates)
   3. Observe: No update dialog appears despite newer version being available

2. **Changelog broken**:
   1. Create a GitHub release with tag `v*.*.*` (e.g., `v1.0.27`)
   1. Observe the GitHub release page
   1. Observe: Release body shows "APK de release signé généré automatiquement suite au push sur master." instead of structured changelog

[NEEDS CLARIFICATION: Exact version string format used in app - does it include "v" prefix? Need to confirm version string format in BuildConfig or version config.]

## Suspected Code Paths

- `app/src/main/java/fr/julien/quievreux/droidplane2/update/AppVersion.kt:23-31` — Version parsing logic. Removes "v" prefix, splits by ".", converts to Int list. Version 1.0.26 → [1,0,26], 1.0.27 → [1,0,27]. Comparison should work (27 > 26).
- `app/src/main/java/fr/julien/quievreux/droidplane2/update/UpdateCheckerService.kt:66-104` — Version comparison logic. Uses `AppVersion.parse()` and compares `releaseVersion > currentAppVersion`. 
- `app/src/main/java/fr/julien/quievreux/droidplane2/update/GitHubRelease.kt:19-23` — Parses `body` field from GitHub API. If GitHub release body is empty or contains fallback text, `body` field will contain fallback text.
- `.github/workflows/release.yml:38-59` — Generates changelog using `git cliff`. Uses `git cliff --tag "$CURRENT_TAG" --previous-tag "$PREV_TAG"` to generate changelog between tags.
- `.cliff.toml:30-41` — Commit parsers for `git cliff`. Uses regex patterns to match conventional commits and group them.

## Root Cause Hypothesis

**Confidence: high**

Two separate issues:

1. **Update not detected**: The version comparison logic in `AppVersion` and `UpdateCheckerService` should correctly detect 1.0.27 > 1.0.26 ([1,0,27] > [1,0,26]). Since v1.0.27 is confirmed as a full release (not draft/prerelease), the GitHub API `/releases/latest` should return it. Possible causes:
   - The `UpdateCheckerService` cache may be serving stale data (1-hour TTL)
   - The `AppVersion.parse()` may be normalizing the version incorrectly (e.g., handling "v" prefix differently)
   - The `releaseVersion > currentAppVersion` comparison may have an edge case
   - Network failure with stale cache fallback returning older data

2. **Changelog broken (CONFIRMED)**: The fallback text "APK de release signé généré automatiquement suite au push sur master" is GitHub's default release body when `git cliff` generates empty output. This happens because:
   - **CONFIRMED**: Commits did NOT follow conventional commit format
   - The `.cliff.toml` commit_parsers only match conventional commit format: `type(scope): subject`
   - Since commits don't follow this format, `git cliff` finds no matching commits and generates empty output
   - GitHub then uses its default release body text
   - The workflow runs without errors because `git cliff` succeeds (just produces empty output)

## Proposed Remediation

**Preferred**: 

1. **Fix update detection**: 
   - Add debug logging to `UpdateCheckerService.checkForUpdates()` to trace version parsing and comparison
   - Verify `AppVersion.parse()` handles both "1.0.26" and "v1.0.26" formats identically
   - Consider bypassing cache for manual update checks (force fresh fetch)
   - Add more detailed error logging for network failures

2. **Fix changelog generation (PRIORITY)**:
   - **Immediate**: Fix `.cliff.toml` commit_parsers - each commit type needs its own parser with correct group name:
     - Current: First parser catches `feat|fix|chore|docs|refactor|perf|test|ci|build|revert` all as "Features"
     - Fix: Separate parser per type with correct group name (feat→Features, fix→Bug Fixes, etc.)
   - **Long-term**: Enforce conventional commits via commitlint (already configured) and update CONTRIBUTING.md with examples
   - Run `git cliff` locally to verify changelog generation before releasing

**Files likely to change**:
- `.cliff.toml` — **URGENT**: Fix commit parsers (separate parser per type with correct group names)
- `.github/workflows/release.yml` — Add validation step to warn if changelog is empty
- `app/src/main/java/fr/julien/quievreux/droidplane2/update/UpdateCheckerService.kt` — Add debug logging for version comparison
- `app/src/main/java/fr/julien/quievreux/droidplane2/update/GitHubRelease.kt` — Handle empty body case

**Tests to add or update**:
- Integration test for `UpdateCheckerService` with mocked GitHub API returning various release scenarios
- Test `git cliff` configuration with sample commits matching actual repository commit format
- Add test for version comparison edge cases (v prefix, missing minor/patch, etc.)

## Risks & Considerations

- **API breakage**: Changing `.cliff.toml` commit parsers will affect changelog generation for all future releases
- **Performance**: Adding debug logging to update check may slightly increase latency
- **GitHub API rate limits**: Debugging may consume API calls; use test tokens or mock servers
- **Backward compatibility**: Version parsing must handle existing version formats (with/without "v" prefix)

## Open Questions

- [NEEDS CLARIFICATION: What exact version string does the app report as current version? Does it include "v" prefix?]
- ~~[NEEDS CLARIFICATION: Is the v1.0.27 release on GitHub a draft, prerelease, or full release?]~~ **RESOLVED: Full release**
- ~~[NEEDS CLARIFICATION: What is the actual commit message format used in the repository? Do they follow conventional commits?]~~ **RESOLVED: Commits do NOT follow conventional commits**
- ~~[NEEDS CLARIFICATION: Does the GitHub Actions workflow run successfully? Check workflow run logs for errors.]~~ **RESOLVED: Workflows ran without errors**
- [NEEDS CLARIFICATION: What exact version string does the app report as current version? Does it include "v" prefix?]
- [NEEDS CLARIFICATION: Check UpdateCheckerService cache behavior - is stale cache being served?]
- [NEEDS CLARIFICATION: Does the AppVersion.parse() handle "1.0.26" vs "v1.0.26" identically?]