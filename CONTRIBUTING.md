# Contributing to Droidplane

Thank you for your interest in contributing to Droidplane! This document provides guidelines for contributing to the project.

## Commit Message Convention

We follow the [Conventional Commits](https://www.conventionalcommits.org/) specification. All commit messages must adhere to this format:

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types

| Type | Description |
|------|-------------|
| `feat` | A new feature |
| `fix` | A bug fix |
| `chore` | Maintenance, no functional change |
| `docs` | Documentation only changes |
| `refactor` | Code restructuring without behavior change |
| `perf` | Performance improvements |
| `test` | Adding or modifying tests |
| `ci` | CI/CD configuration changes |
| `build` | Build system or dependency changes |
| `revert` | Reverts a previous commit |

### Scope (Optional)

The scope should be lowercase and describe the affected module or component:

- `ui` - User interface components
- `core` - Core utilities and shared code
- `data` - Data layer (parsing, models, repository)
- `update` - Update checker functionality
- `mindmap` - Mind map rendering and layout
- `deps` - Dependency updates

### Subject

- Use imperative mood ("add" not "added" or "adds")
- No trailing period
- Maximum 100 characters
- Lowercase first letter

### Body (Optional)

- Provide context for the change
- Explain *why* not *what*
- Wrap at 72 characters

### Footer (Optional)

- Reference issues: `Closes #123`
- Breaking changes: `BREAKING CHANGE: <description>`

### Examples

```bash
# Good commits
feat(ui): add dark mode toggle
fix(core): resolve crash on app startup
docs: update README with setup instructions
refactor(data): simplify NodeManager parsing logic
perf(mindmap): optimize layout calculation for large maps
test(update): add unit tests for caching behavior
chore(deps): update Kotlin to 2.1.0
ci: add commitlint validation to PR workflow

# With body and footer
feat(ui): add dark mode toggle

Adds a new toggle in settings to switch between light and dark themes.
Uses system theme by default with manual override.

Closes #42

# Breaking change
fix(api): change update check response format

BREAKING CHANGE: UpdateCheckResult.UpdateAvailable now includes releaseNotes field.
```

## Validation

Commit messages are validated automatically on pull requests using [commitlint](https://commitlint.js.org/). The configuration is in `.commitlintrc.json`.

To validate locally:

```bash
# Install commitlint
npm install -g @commitlint/cli @commitlint/config-conventional

# Validate a commit message
echo "feat(ui): add dark mode" | commitlint

# Validate last commit
commitlint --from HEAD~1 --to HEAD
```

## Pull Request Process

1. Ensure your commit messages follow the convention
2. Run tests locally: `./gradlew test`
3. Ensure lint passes: `./gradlew lintDebug`
4. Update documentation if needed
5. Submit PR with clear description

## Code Style

- Follow Kotlin coding conventions
- Use `ktlint` for formatting
- Keep cyclomatic complexity ≤ 20 (enforced by Detekt)
- Write tests for new functionality

## Release Process

Releases are automated via GitHub Actions on push to `master`:

1. Merge or push changes to `master`
2. GitHub Actions workflow (`.github/workflows/android_release.yml`):
   - Determines the previous release tag (`v*.*.*`)
   - Auto-increments the app version and commits with `[skip ci]`
   - Generates the changelog from commits since the previous tag using `git-cliff` (`.cliff.toml`)
   - Builds the signed release APK and publishes the GitHub Release with the generated changelog

See `.github/workflows/android_release.yml` for details.