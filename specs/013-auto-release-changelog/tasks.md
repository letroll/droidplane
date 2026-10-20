# Tasks: Auto Release Changelog & In-App Update Preview

**Input**: Design documents from `/specs/013-auto-release-changelog/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/ui-contracts.md](contracts/ui-contracts.md), [quickstart.md](quickstart.md)

**Tests**: Unit and integration tests are included per Droidplane Constitution Principle III (Test-Driven Verification). UI tests use Compose UI Test.

**Organization**: Tasks are grouped by user story (`US1`–`US3`) to enable independent implementation and verification of each increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`–`[US3]`)
- Exact file paths are included in every task description

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Verify development environment and baseline build health

- [ ] T001 Verify project build and test baseline across all modules via `./gradlew test --quiet`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure required before ANY user story can be implemented

**⚠️ CRITICAL**: Must complete before User Story phases

- [ ] T002 [P] Create `.cliff.toml` configuration for `git cliff` in repository root with commit groups, section order, and template matching data-model.md sections
- [ ] T003 [P] Create `.commitlintrc.json` extending `@commitlint/config-conventional` with rules from contracts/ui-contracts.md (type-enum, scope-case, subject-max-length, header-max-length)
- [ ] T004 Add `ReleaseNoteParser` interface and `ConventionalCommit`, `CommitType`, `ReleaseNote` data classes in `core/src/main/java/fr/julien/quievreux/droidplane2/core/util/ReleaseNoteParser.kt`
- [ ] T005 [P] Extend `GitHubRelease` data class in `app/src/main/java/fr/julien/quievreux/droidplane2/update/GitHubRelease.kt` with `body: String?` field for changelog
- [ ] T006 [P] Extend `UpdateCheckResult` sealed class in `app/src/main/java/fr/julien/quievreux/droidplane2/update/UpdateCheckResult.kt` with `releaseNotes: String?` in `UpdateAvailable`

**Checkpoint**: Foundation ready — parser interfaces, data models, and CI configs in place

---

## Phase 3: User Story 1 - Automated GitHub Release Notes (Priority: P1) 🎯 MVP

**Goal**: Automatically generate structured GitHub release notes from conventional commits on tag push

**Independent Test**: Push a version tag (e.g., `v1.0.99-test`), verify GitHub Actions workflow runs, creates release with categorized changelog (Features, Bug Fixes, etc.), and uploads APK

### Tests for User Story 1

- [ ] T007 [P] [US1] Unit tests for `ReleaseNoteParser.parseCommit` covering all `CommitType` values, scope parsing, subject extraction, and `OTHER` fallback in `core/src/test/java/fr/julien/quievreux/droidplane2/core/util/ReleaseNoteParserTest.kt`
- [ ] T008 [P] [US1] Unit tests for `ReleaseNoteParser.parseReleaseNotes` verifying section order (FEAT→FIX→PERF→REFACTOR→DOCS→TEST→CI→BUILD→CHORE→REVERT→OTHER), markdown generation, and empty range handling in `core/src/test/java/fr/julien/quievreux/droidplane2/core/util/ReleaseNoteParserTest.kt`
- [ ] T009 [P] [US1] Integration test for `ReleaseNoteParserImpl` against sample git history fixtures in `core/src/test/java/fr/julien/quievreux/droidplane2/core/util/ReleaseNoteParserIntegrationTest.kt`

### Implementation for User Story 1

- [ ] T010 [P] [US1] Implement `ReleaseNoteParserImpl` pure Kotlin parser in `core/src/main/java/fr/julien/quievreux/droidplane2/core/util/ReleaseNoteParserImpl.kt` with regex-based conventional commit parsing per data-model.md rules
- [ ] T011 [US1] Create GitHub Actions workflow `.github/workflows/release.yml` with `generate-release-notes` job: checkout (fetch-depth: 0), determine previous tag, install `git cliff` via binary download, run `git cliff`, create/update release via `gh release create`, upload APK from `app/build/outputs/apk/release/*.apk`
- [ ] T012 [P] [US1] Create GitHub Actions workflow `.github/workflows/commitlint.yml` with `commitlint` job on `pull_request` using `wagoid/commitlint-github-action@v6` to validate conventional commits

**Checkpoint**: User Story 1 functional — automated release notes generated on tag push with categorized commits

---

## Phase 4: User Story 2 - In-App Changelog Preview Before Update (Priority: P1) 🎯 MVP

**Goal**: Enhanced update dialog displays full changelog from GitHub release body with scrollable content and Update Now/Later actions

**Independent Test**: Trigger update check when newer version exists on GitHub; verify dialog shows complete release body (changelog) scrollable, with "Update Now" and "Later" buttons

### Tests for User Story 2

- [ ] T013 [P] [US2] Unit tests for `GitHubUpdateCheckerServiceImpl.checkForUpdate()` mapping GitHub API `body` field to `UpdateCheckResult.UpdateAvailable.releaseNotes` in `app/src/test/java/fr/julien/quievreux/droidplane2/update/GitHubUpdateCheckerServiceImplTest.kt`
- [ ] T014 [P] [US2] Unit tests for caching behavior (1-hour TTL, fallback on rate limit 403, network error) in `app/src/test/java/fr/julien/quievreux/droidplane2/update/GitHubUpdateCheckerServiceImplTest.kt`
- [ ] T015 [P] [US2] Compose UI tests for `UpdateDialog` in `app/src/androidTest/java/fr/julien/quievreux/droidplane2/ui/components/UpdateDialogTest.kt` covering: loading state, error state with retry, loaded state with changelog scrollable, empty changelog fallback, Update Now/Later button actions, dark/light theme
- [ ] T016 [P] [US2] Compose previews for `UpdateDialog` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/UpdateDialog.kt` with `@Preview` for normal/long/empty changelog, loading, error, dark/light themes

### Implementation for User Story 2

- [ ] T017 [US2] Update `GitHubUpdateCheckerServiceImpl` in `app/src/main/java/fr/julien/quievreux/droidplane2/update/GitHubUpdateCheckerServiceImpl.kt` to fetch `body` from GitHub API `/releases/latest`, map to `UpdateCheckResult.UpdateAvailable.releaseNotes`, implement in-memory cache with 1-hour TTL, handle 403/404/network errors with fallback
- [ ] T018 [US2] Create `UpdateDialog` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/UpdateDialog.kt` per ui-contracts.md: Material 3 dialog, scrollable changelog (max 60% screen height, min 200dp), fallback message for empty body, Update Now/Later buttons with callbacks, accessibility semantics
- [ ] T019 [US2] Wire `UpdateDialog` into `MainViewModel`/`MainActivity` update flow: observe `UpdateCheckResult`, show dialog on `UpdateAvailable`, pass changelog and download URL, handle Update Now (launch download intent) and Later (dismiss)

**Checkpoint**: User Stories 1 and 2 functional — automated release notes on GitHub + in-app changelog preview with actions

---

## Phase 5: User Story 3 - Conventional Commits Enforcement (Priority: P2)

**Goal**: Enforce conventional commit format in CI to ensure quality automated release notes

**Independent Test**: Open PR with non-conventional commit; verify CI fails with clear error message; push conventional commit; verify CI passes

### Tests for User Story 3

- [ ] T020 [P] [US3] Test `commitlint` configuration locally: valid commits pass, invalid commits fail with descriptive errors (missing type, empty subject, invalid scope case, subject too long)

### Implementation for User Story 3

- [ ] T021 [US3] Verify `.commitlintrc.json` from T003 is committed and `.github/workflows/commitlint.yml` from T012 runs on all PRs
- [ ] T022 [US3] Add commitlint validation to local development docs (README or CONTRIBUTING.md) with commit message format examples

**Checkpoint**: All user stories functional — CI enforces conventional commits, automated release notes generated, in-app changelog displayed

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Quality gates, full test verification, documentation validation

- [ ] T023 [P] Add Compose `@Preview` functions and dark/light theme validation for `UpdateDialog` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/UpdateDialog.kt`
- [ ] T024 Run full regression test suite across all modules via `./gradlew test --rerun-tasks`
- [ ] T025 Verify complete application build and APK assembly via `./gradlew assembleDebug`
- [ ] T026 Execute quickstart validation scenarios from `specs/013-auto-release-changelog/quickstart.md`
- [ ] T027 [P] Update `README.md` or `CONTRIBUTING.md` with conventional commit format guide and release process documentation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — **BLOCKS** all user stories
- **User Story 1 (Phase 3)**: Depends on Phase 2. Core MVP for release automation
- **User Story 2 (Phase 4)**: Depends on Phase 2 & Phase 3 (needs release notes generated for changelog to exist)
- **User Story 3 (Phase 5)**: Depends on Phase 2 (commitlint config) — can run in parallel with US1/US2
- **Polish (Phase 6)**: Depends on Phases 3–5 completion

### User Story Dependencies

- **US1 (P1)**: Foundational prerequisites only
- **US2 (P1)**: Depends on US1 (requires GitHub release with body to display)
- **US3 (P2)**: Depends on Phase 2 (commitlint config) — independent of US1/US2 execution

---

## Parallel Opportunities

- **Phase 2**: T002 (`.cliff.toml`), T003 (`.commitlintrc.json`), T005 (GitHubRelease extension), T006 (UpdateCheckResult extension) can run in parallel
- **Phase 3 (US1)**: T007, T008, T009 (tests) can run in parallel before T010, T011, T012 (implementation)
- **Phase 4 (US2)**: T013, T014 (service tests), T015 (UI tests), T016 (previews) can run in parallel before T017, T018, T019 (implementation)
- **Phase 5 (US3)**: T020 (test) can run in parallel with T021, T022 (implementation)
- **Phase 6**: T023 (previews), T027 (docs) can run in parallel with test suite runs

---

## Parallel Example: User Story 1

```bash
# Launch independent test tasks concurrently:
Task T007: "Unit tests for ReleaseNoteParser.parseCommit in core/src/test/.../ReleaseNoteParserTest.kt"
Task T008: "Unit tests for ReleaseNoteParser.parseReleaseNotes in core/src/test/.../ReleaseNoteParserTest.kt"
Task T009: "Integration test for ReleaseNoteParserImpl in core/src/test/.../ReleaseNoteParserIntegrationTest.kt"
```

---

## Parallel Example: User Story 2

```bash
# Launch independent test tasks concurrently:
Task T013: "Unit tests for GitHubUpdateCheckerServiceImpl in app/src/test/.../GitHubUpdateCheckerServiceImplTest.kt"
Task T014: "Unit tests for caching in app/src/test/.../GitHubUpdateCheckerServiceImplTest.kt"
Task T015: "Compose UI tests for UpdateDialog in app/src/androidTest/.../UpdateDialogTest.kt"
Task T016: "Compose previews for UpdateDialog in app/src/main/.../UpdateDialog.kt"
```

---

## Implementation Strategy

### MVP First (User Stories 1 & 2 - P1)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (CRITICAL - blocks all stories)
3. Complete Phase 3: User Story 1 (GitHub Actions release workflow)
4. Complete Phase 4: User Story 2 (Enhanced update dialog with changelog)
5. **STOP and VALIDATE**: Run full test suite, verify release workflow on test tag, verify update dialog shows changelog

### Incremental Delivery

1. Deliver P1 (US1 + US2): Automated GitHub releases + in-app changelog preview
2. Deliver P2 (US3): Commitlint enforcement for commit quality
3. Deliver Polish: Full regression test, documentation, quickstart validation

### Parallel Team Strategy

With multiple developers:

1. Team completes Setup + Foundational together
2. Once Foundational is done:
   - Developer A: User Story 1 (CI/CD workflow + parser)
   - Developer B: User Story 2 (UpdateCheckerService + UpdateDialog)
   - Developer C: User Story 3 (commitlint config + docs)
3. Stories complete and integrate independently

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Verify tests fail before implementing (TDD per Constitution Principle III)
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Avoid: vague tasks, same file conflicts, cross-story dependencies that break independence
- All file paths use Android multi-module structure (`:core`, `:app`) per plan.md