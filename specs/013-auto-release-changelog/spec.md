# Feature Specification: Auto Release Changelog & In-App Update Preview

**Feature Branch**: `013-auto-release-changelog`

**Created**: 2026-10-20

**Status**: Draft

**Input**: User description: "j'aimerai à présent que les releases sur github contienne un descriptif automatiquement tiré des commits depuis la dernière release et que la proposition de mise à jour dans l'app fournisse la capicité de lire directement ce changelog avant de récupéré la mise à jour"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Automated GitHub Release Notes (Priority: P1)

As a maintainer releasing a new version of Droidplane, I want GitHub release notes to be automatically generated from commits since the last release tag, so that I don't have to manually write changelogs and users get consistent, comprehensive release information.

**Why this priority**: Manual changelog maintenance is error-prone, time-consuming, and often forgotten. Automated generation from conventional commits ensures every release has accurate, complete notes without maintainer overhead.

**Independent Test**: Create a new release tag after several commits; verify GitHub release description contains formatted commit messages grouped by type (features, fixes, chore, etc.) since the previous tag.

**Acceptance Scenarios**:

1. **Given** a repository with commits since the last release tag following conventional commit format, **When** a new release is created via GitHub Actions, **Then** the release description is automatically populated with categorized commit messages.
2. **Given** a release is published on GitHub, **When** a user views the release page, **Then** they see a structured changelog with sections for Features, Bug Fixes, Maintenance, and Other changes.
3. **Given** no commits exist since the last release, **When** a release is created, **Then** the description indicates "No changes since previous release" or similar.

---

### User Story 2 - In-App Changelog Preview Before Update (Priority: P1)

As a user of Droidplane, when an app update is available, I want to read the full changelog directly in the update dialog before deciding to download and install, so that I can make an informed decision about whether to update now or later.

**Why this priority**: Users currently see only a version number or generic "update available" message. Showing the actual changelog respects user agency, reduces update anxiety, and increases update adoption by demonstrating value.

**Independent Test**: Trigger an update check when a newer version exists on GitHub; verify the update dialog displays the full release body (changelog) from GitHub with scrollable content, and the user can choose "Update Now" or "Later" after reading.

**Acceptance Scenarios**:

1. **Given** a newer app version exists on GitHub with release notes, **When** the app checks for updates and finds one, **Then** the update dialog displays the complete release body (changelog) from GitHub.
2. **Given** the update dialog shows the changelog, **When** the user scrolls through the changelog content, **Then** all release notes are readable without truncation.
3. **Given** the user reads the changelog, **When** they tap "Update Now", **Then** the download and install process begins; **When** they tap "Later", **Then** the dialog dismisses and the update can be triggered again later.
4. **Given** the GitHub release has no body/description, **When** the update dialog appears, **Then** it shows a fallback message like "No changelog provided for this release."

---

### User Story 3 - Conventional Commits Enforcement (Priority: P2)

As a contributor to Droidplane, I want commit messages to follow a conventional format so that automated release notes are meaningful and correctly categorized.

**Why this priority**: Automated release notes quality depends entirely on commit message quality. Enforcing conventional commits at the project level ensures the automation produces useful output.

**Independent Test**: Attempt to push a commit with non-conventional format; verify CI rejects it with guidance on correct format.

**Acceptance Scenarios**:

1. **Given** a contributor pushes a commit with format "feat: add new mindmap export", **When** CI runs, **Then** the commit passes validation.
2. **Given** a contributor pushes a commit with format "added new feature", **When** CI runs, **Then** the commit fails with a clear error message explaining the expected format.

---

### Edge Cases

- **First Release**: No previous tag exists — generate changelog from all commits or from repository initialization
- **Non-Conventional Commits**: Commits not following conventional format — include in "Other Changes" section or exclude with warning
- **Empty Release Body**: GitHub release created manually without description — show fallback in app
- **Network Failure**: Unable to fetch release notes from GitHub — show cached/local version info with retry option
- **Large Changelog**: Release notes exceed dialog height — enable scrolling with visual indicator
- **Malformed Release Data**: GitHub API returns unexpected format — gracefully degrade to version-only display

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The CI/CD pipeline MUST automatically generate GitHub release descriptions from commits since the last release tag using conventional commit parsing.
- **FR-002**: Generated release notes MUST categorize commits by type (feat, fix, chore, docs, refactor, perf, test, ci, build, revert) into corresponding sections.
- **FR-003**: The release generation MUST use the previous Git tag (semver) as the baseline for commit range.
- **FR-004**: The in-app update check MUST fetch the latest GitHub release metadata including the release body (changelog).
- **FR-005**: The update dialog MUST display the full release body (changelog) in a scrollable, readable format before the user confirms download.
- **FR-006**: The update dialog MUST provide two actions: "Update Now" (starts download/install) and "Later" (dismisses dialog).
- **FR-007**: If the GitHub release has no body/description, the update dialog MUST display a fallback message indicating no changelog is available.
- **FR-008**: The update check MUST continue to work for users who skip versions (e.g., v1.0.20 → v1.0.22) by showing the latest release's changelog.
- **FR-009**: Commit message validation SHOULD be enforced in CI to ensure conventional commit format compliance.
- **FR-010**: The system MUST handle GitHub API rate limits gracefully with appropriate caching or fallback.

### Key Entities

- **Release Note**: Auto-generated text content for a GitHub release, derived from commit history between tags, structured by commit type.
- **GitHub Release Metadata**: API response containing tag name, release name, body (changelog), published date, and asset URLs (APK).
- **Update Dialog UI**: In-app dialog presenting version info, changelog content, and action buttons.
- **Conventional Commit**: A commit message following the format `<type>(<scope>): <subject>` with optional body and footer.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of GitHub releases created via automation include structured changelog with categorized commits since previous tag.
- **SC-002**: Update dialog displays complete release body (changelog) from GitHub API with zero truncation for releases under 5000 characters.
- **SC-003**: Users can read full changelog and make update decision without leaving the app or opening browser.
- **SC-004**: CI validates conventional commit format on 100% of incoming PR commits, rejecting non-compliant messages.
- **SC-005**: Update check + changelog fetch completes within 3 seconds under normal network conditions.
- **SC-006**: Zero manual intervention required for release note generation after initial setup.

## Assumptions

- The project uses semantic versioning tags (e.g., `v1.0.22`) on GitHub for releases.
- GitHub Actions is the CI/CD platform for automated release creation.
- The existing update check mechanism (`UpdateCheckerService`) already fetches latest release from GitHub API.
- Conventional commit format: `type(scope): subject` where type ∈ {feat, fix, chore, docs, refactor, perf, test, ci, build, revert}.
- The app's current version is available via `BuildConfig.VERSION_NAME` or equivalent.
- Network connectivity is available when checking for updates (offline handling already exists).
- GitHub API rate limits (5000 req/hour) are sufficient for update checks.