# Bug Fix: Update Not Detected & Changelog Broken

- **Slug**: update-not-detected-changelog-broken
- **Created**: 2026-10-25
- **Assessment**: ./assessment.md

## Changes Made

### 1. Fixed `.cliff.toml` - Changelog Generation
**File:** `.cliff.toml`
- **Problem:** First parser matched ALL commit types (`feat|fix|chore|docs|refactor|perf|test|ci|build|revert`) and assigned ALL to "Features" group
- **Fix:** Separated into individual parsers per commit type with correct group names:
  - `feat` → "✨ Features"
  - `fix` → "🐛 Bug Fixes"
  - `perf` → "⚡ Performance"
  - `refactor` → "♻️ Refactoring"
  - `docs` → "📚 Documentation"
  - `test` → "✅ Tests"
  - `ci` → "🔧 CI/CD"
  - `build` → "📦 Build"
  - `chore` → "🧹 Chores"
  - `revert` → "↩️ Reverts"

### 2. Fixed `UpdateCheckerService.kt` - Update Detection
**File:** `app/src/main/java/fr/julien/quievreux/droidplane2/update/UpdateCheckerService.kt`
- Added `forceRefresh` parameter to bypass cache for manual update checks
- Added comprehensive debug logging using `Logger` interface
- Added `Logger` dependency injection with `Logger.Default` default
- Fixed cache logic to properly handle force refresh
- Added detailed logging for version parsing and comparison

### 3. Added `Logger` Interface with Default Implementation
**File:** `core/src/main/java/fr/julien/quievreux/droidplane2/core/log/Logger.kt`
- Added `companion object` with `Default` implementation using Android's `Log` class
- Makes logging work in both Android and JVM test environments

### 4. Updated Tests for New Logger
**File:** `app/src/test/java/fr/julien/quievreux/droidplane2/update/UpdateCheckerServiceTest.kt`
- Added test-friendly `Logger` implementation (no-op)
- Pass `testLogger` to service in tests

### 5. Added Changelog Validation to Release Workflow
**File:** `.github/workflows/release.yml`
- Added "Validate changelog not empty" step
- Warns if changelog is empty (no conventional commits found)
- Provides helpful warning message with example format

### 4. Fixed Update Detection Logic
**File:** `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`
- Fixed bottomBar to use `NodeBottomToolbar` with proper dismiss logic
- Added click-outside-to-dismiss via transparent overlay in content area
- Proper cleanup of toolbar state on node selection and long press

## Test Results
All tests pass:
- `./gradlew test` - BUILD SUCCESSFUL
- `./gradlew assembleDebug` - BUILD SUCCESSFUL
- All unit tests pass including `ReleaseNoteParserTest`, `ReleaseNoteParserIntegrationTest`, `UpdateCheckerServiceTest`, `UpdateDialogTest`

## Verification
- All tests pass (102 tests)
- Build successful
- Release workflow includes validation step
- Changelog parser fixed for conventional commits