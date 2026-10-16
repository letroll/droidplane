# Tasks: Codebase Audit Remediation & Quality Hardening

**Input**: Design documents from `/specs/012-audit-remediation/`
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/module-and-state-contracts.md](contracts/module-and-state-contracts.md), [quickstart.md](quickstart.md)

**Tests**: Unit and regression tests are included per Droidplane Constitution Principle III (Test-Driven Verification).

**Organization**: Tasks are grouped by user story (`US1`–`US5`) to enable independent implementation and verification of each increment.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (`[US1]`–`[US5]`)
- Exact file paths are included in every task description

---

## Phase 1: Setup (Shared Build Catalog & Version Alignment)

**Purpose**: Clean up unused version catalog entries and align module BOM coordinates before modifying module dependencies.

- [X] T001 Remove 7 unused library definitions (`mindmap`, `commons-io`, `commons-lang3`, `androidx-constraintlayout`, `androidx-legacy-support-v4`, `androidx-preference-ktx`, `material`) and their unused version entries from `gradle/libs.versions.toml`
- [X] T002 [P] Replace hardcoded `"androidx.compose:compose-bom:2024.09.03"` with `platform(libs.androidx.compose.bom)` in `core/build.gradle.kts`

---

## Phase 2: Foundational (Test Fixtures Source Sets)

**Purpose**: Enable Android Gradle `testFixtures` in `:core` and `:data` and relocate shared test classes out of `src/main` so all user stories can build and test cleanly without shipping test utilities in production.

**⚠️ CRITICAL**: Must complete before User Story phases.

- [X] T003 Enable `testFixtures { enable = true }` inside the `android` block of `core/build.gradle.kts` and `data/build.gradle.kts`
- [X] T004 [P] Move `KStringSpec.kt` from `core/src/main/java/fr/julien/quievreux/droidplane2/core/testutils/KStringSpec.kt` to `core/src/testFixtures/java/fr/julien/quievreux/droidplane2/core/testutils/KStringSpec.kt`
- [X] T005 [P] Move `FakeDataSource.kt` from `data/src/main/java/fr/julien/quievreux/droidplane2/data/FakeDataSource.kt` to `data/src/testFixtures/java/fr/julien/quievreux/droidplane2/data/FakeDataSource.kt`

**Checkpoint**: Shared test fixtures (`KStringSpec` and `FakeDataSource`) are isolated in `src/testFixtures`.

---

## Phase 3: User Story 1 - Lightweight, Secure Production Application Package (Priority: P1) 🎯 MVP

**Goal**: Exclude all test frameworks, mocking agents, and fake data generators from the production release APK, and enable R8 code shrinking (`isMinifyEnabled = true`) with proper ProGuard rules.

**Independent Test**: Run `./gradlew testDebugUnitTest :app:assembleRelease` and verify that unit tests pass across all modules while `app-release-unsigned.apk` contains zero classes from `io.kotest`, `io.mockk`, `app.cash.turbine`, `KStringSpec`, or `FakeDataSource`.

### Implementation for User Story 1

- [X] T006 [US1] Change `api(libs.junit)`, `api(libs.kotest.*)`, `api(libs.mockk)`, `api(libs.androidx.junit)`, and `api(libs.androidx.espresso.core)` to `testFixturesApi(...)` / `testImplementation(...)` / `androidTestImplementation(...)` and delete the `excludes += " **/attach_hotspot_windows.dll"` packaging block in `core/build.gradle.kts`
- [X] T007 [US1] Add `testFixturesImplementation(project(":core"))` and `testImplementation(testFixtures(project(":core")))` in `data/build.gradle.kts`
- [X] T008 [US1] Change `implementation(libs.turbine)` to `testImplementation(libs.turbine)`, remove `implementation(libs.mindmap)`, add `testImplementation(testFixtures(project(":core")))`, `testImplementation(testFixtures(project(":data")))`, and `androidTestImplementation(testFixtures(project(":data")))`, and set `isMinifyEnabled = true` in `app/build.gradle.kts`
- [X] T009 [P] [US1] Create R8/ProGuard configuration keeping Koin modules, `org.xmlpull.v1.**`, and serialized domain models in `app/proguard-rules.pro`

**Checkpoint**: User Story 1 is complete — production APK is minified and free of test frameworks while all test suites compile and pass.

---

## Phase 4: User Story 2 - Corruption-Free Mind Map Saving and Rich Content Fidelity (Priority: P1)

**Goal**: Prevent silent corruption of literal `<![CDATA[` / `]]>` strings and unescaped `<`, `>`, `&`, `"` characters in rich-text notes, and abort immediately on invalid save paths/filenames.

**Independent Test**: Run `./gradlew :data:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.data.RichContentXmlTest"` to verify round-trip fidelity for literal CDATA sequences, special XML entities in `<richcontent>`, and immediate error return on invalid file paths/names.

### Tests for User Story 2

- [X] T010 [P] [US2] Add unit tests covering literal `<![CDATA[` / `]]>` preservation, `<richcontent>` special characters (`a < b & c > "d"`), and invalid `filePath`/`filename` early return without calling `onSaveFinished` in `data/src/test/java/fr/julien/quievreux/droidplane2/data/RichContentXmlTest.kt`

### Implementation for User Story 2

- [X] T011 [P] [US2] Refactor `loadRichContent` to use `StringBuilder` and escape `&`, `<`, `>`, and `"` in text content and attribute values in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeUtilsDefaultImpl.kt`
- [X] T012 [US2] Return immediately after `onError` when `isInvalidFilePath(filePath)` or `isInvalidFileName(filename)` is true, replace the line-by-line `.replace("<![CDATA[", "").replace("]]>", "")` loop with atomic replacement of `fileSaveDestination` from `tempFile`, and move `onSaveFinished?.invoke(fileSaveDestination)` inside the `try` block in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt`

**Checkpoint**: User Story 2 is complete — saving mind maps is atomic, single-pass, and preserves all literal CDATA and HTML entities.

---

## Phase 5: User Story 3 - Responsive UI and Leak-Free Device Rotation During File Operations (Priority: P2)

**Goal**: Eliminate the `FileRegister` `MainActivity` memory leak in `MainViewModel`, move all `ContentResolver` save and startup recent-file I/O to `Dispatchers.IO`, handle `leaving` inside `LaunchedEffect`, and share relative files via `FileProvider`.

**Independent Test**: Run `./gradlew :app:testDebugUnitTest` to verify `MainViewModel` save state emission without `FileRegister` and off-main-thread `refreshRecentFiles` execution.

### Tests for User Story 3

- [X] T013 [P] [US3] Update and add unit tests for `launchSaveFile(filesDirPath: String)` and `MainUiState.pendingSaveTempFile` emission in `app/src/test/java/fr/julien/quievreux/droidplane2/MainViewModelSaveFlowTest.kt`

### Implementation for User Story 3

- [X] T014 [P] [US3] Add `val pendingSaveTempFile: File? = null` to `MainUiState` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/state/MainUiState.kt`
- [X] T015 [US3] Remove `fileRegister` and `setFileRegister`, update `launchSaveFile(filesDirPath: String)` to emit `pendingSaveTempFile` in `MainUiState`, add `clearPendingSaveTempFile()`, stream `saveFile` using `file.inputStream().use { it.copyTo(outputStream) }`, and wrap `refreshRecentFiles` in `viewModelScope.launch(Dispatchers.IO)` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- [X] T016 [P] [US3] Delete obsolete `FileRegister` interface file `app/src/main/java/fr/julien/quievreux/droidplane2/model/FileRegister.kt`
- [X] T017 [P] [US3] Declare `androidx.core.content.FileProvider` with authority `${applicationId}.fileprovider` in `app/src/main/AndroidManifest.xml` and create `app/src/main/res/xml/file_paths.xml`
- [X] T018 [US3] Remove `FileRegister` implementation, observe `state.value.pendingSaveTempFile` in `LaunchedEffect`, run `writeMindMapToUri` inside `lifecycleScope.launch(Dispatchers.IO)`, wrap `state.value.leaving` handling in `LaunchedEffect(state.value.leaving)`, and convert `file://` URIs in `RelativeFile` intents via `FileProvider.getUriForFile` with `Intent.FLAG_GRANT_READ_URI_PERMISSION` in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: User Story 3 is complete — zero `MainActivity` reference leaks in `MainViewModel`, zero main-thread file I/O, and safe `FileProvider` URI sharing.

---

## Phase 6: User Story 4 - Consistent Deep-Tree Editing, Fast Large-Map Loading, and Smooth Selection (Priority: P2)

**Goal**: Ensure `Node.equals`/`hashCode` includes all styling/hook properties, propagate `parentNode` updates to all descendant depths, make XML node list loading $O(N)$, memoize 2D layout subtree heights, prevent layout recalculation on node selection, and clean up `SearchManager` logging.

**Independent Test**: Run `./gradlew :data:testDebugUnitTest :app:testDebugUnitTest` to verify deep parent propagation, `Node.equals` completeness, $O(N)$ XML loading, and `MindMapLayoutEngine` layout tests.

### Tests for User Story 4

- [X] T019 [P] [US4] Add unit tests verifying `Node.equals`/`hashCode` for `fontSize`, `fontName`, `isBold`, `isItalic`, `isFolded`, `latexEquation`, and `externalObject`, deep 4-level `parentNode` propagation after ancestor update, and single `_allNodes` emission on XML load in `data/src/test/java/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt`

### Implementation for User Story 4

- [X] T020 [P] [US4] Update `equals` and `hashCode` to include `parentNode?.id`, `isFolded`, `fontSize`, `fontName`, `isBold`, `isItalic`, `latexEquation`, and `externalObject` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/model/Node.kt`
- [X] T021 [US4] Call `updateDescendantParents(fixedChild)` inside `propagateUpdatedParentToRoot` and remove `_allNodes.update { it + nodeRelation.child }` from `addChildIntoParent` (populating `allParsedNodes` directly and setting `_allNodes.value = allParsedNodes` once) in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt`
- [X] T022 [P] [US4] Remove `logger.e("toto")` and log `filteredNodes.size` via `logger.d` instead of stale `nodeFindList.value?.size` in `data/src/main/java/fr/julien/quievreux/droidplane2/data/SearchManager.kt`
- [X] T023 [P] [US4] Memoize `computeSubtreeHeight` and `hasCloudInSubtree` using per-pass `MutableMap<String, Float>` and `MutableMap<String, Boolean>` caches in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngine.kt`
- [X] T024 [US4] Remove `selectedNodeId` from the `remember(...)` keys of `MindMapLayoutEngine.computeLayout` and pass `isSelected = nodeLayout.node.id == selectedNodeId` directly to `MindMapNodeCard` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapCanvasScreen.kt`

**Checkpoint**: User Story 4 is complete — deep tree edits keep valid `parentNode` references, XML parsing is $O(N)$, and 2D node selection no longer recomputes tree layout.

---

## Phase 7: User Story 5 - Clean, Maintainable Codebase Free of Obsolete Legacy Artifacts (Priority: P3)

**Goal**: Delete all dead legacy ViewModels, duplicate UI models, unused XML layouts, dead helper methods, duplicate imports, and hardcoded version fallbacks.

**Independent Test**: Run `./gradlew testDebugUnitTest lintDebug` to verify the project compiles and passes lint with all dead files and methods removed.

### Implementation for User Story 5

- [X] T025 [P] [US5] Delete obsolete files `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/MindMapViewModel.kt`, `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/MindMapUiState.kt`, `app/src/main/java/fr/julien/quievreux/droidplane2/ui/view/EditDescriptionViewModel.kt`, `app/src/main/java/fr/julien/quievreux/droidplane2/ui/model/Node.kt`, `app/src/main/java/fr/julien/quievreux/droidplane2/ui/model/NodePath.kt`, `core/src/main/java/fr/julien/quievreux/droidplane2/core/PermissionUtils.kt`, `app/src/main/res/layout/activity_main.xml`, and `app/src/main/res/layout/dialog_edit_description.xml`
- [X] T026 [P] [US5] Remove unused methods `findFilledNode`, `depthFirstSearchRecursive`, and commented-out legacy code blocks in `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt` and `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- [X] T027 [US5] Remove unused `onRequestPermissionsResult` / `CREATE_FILE_REQUEST_CODE`, duplicate imports (`AlertDialog`, `File`), `@SuppressLint("RemeddmberReturnType")` typo, and hardcoded `"1.0.18"` version fallback in `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt`

**Checkpoint**: User Story 5 is complete — all obsolete artifacts and dead code are removed.

---

## Phase 8: Polish & Cross-Cutting Verification

**Purpose**: End-to-end validation across all modules and build variants.

- [X] T028 Run full validation suite (`./gradlew testDebugUnitTest :app:assembleRelease lintDebug` and DEX string verification) per `specs/012-audit-remediation/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Phase 1 (Setup)**: No dependencies — can start immediately.
- **Phase 2 (Foundational)**: Depends on Phase 1 — **BLOCKS** all user stories because moving `KStringSpec` and `FakeDataSource` to `src/testFixtures` is required before modifying module dependencies and running tests.
- **Phase 3 (`US1` - P1)**: Depends on Phase 2 — configures `testFixtures` consumers and `release` minification.
- **Phase 4 (`US2` - P1)**: Depends on Phase 3 (for test runner configuration) — can run in parallel with `US3`, `US4`, and `US5`.
- **Phase 5 (`US3` - P2)**: Depends on Phase 3 — can run in parallel with `US2` and `US4` (touches `:app` `MainActivity`/`MainViewModel`).
- **Phase 6 (`US4` - P2)**: Depends on Phase 3 — `:data` tasks (`T019`–`T022`) and `:app` layout tasks (`T023`–`T024`) can run in parallel with `US3`.
- **Phase 7 (`US5` - P3)**: Can run after `US2`–`US4` (or `T025` in parallel at any time since those files are unreferenced).
- **Phase 8 (Polish)**: Depends on Phases 3–7 completion.

### Parallel Opportunities

- **Phase 2**: `T004` (`KStringSpec.kt` move) and `T005` (`FakeDataSource.kt` move) can run in parallel.
- **Phase 4 (`US2`)**: `T010` (tests in `RichContentXmlTest.kt`) and `T011` (`NodeUtilsDefaultImpl.kt`) can run in parallel before `T012` (`NodeManager.kt`).
- **Phase 5 (`US3`)**: `T013`, `T014`, `T016`, and `T017` touch completely separate files and can run in parallel before `T015` and `T018`.
- **Phase 6 (`US4`)**: `T019`, `T020`, `T022`, and `T023` touch separate files and can run in parallel.
- **Phase 7 (`US5`)**: `T025` and `T026` can run in parallel.

---

## Parallel Example: User Story 4

```bash
# Run independent tasks across :data and :app concurrently:
Task T020: "Update equals and hashCode in data/src/main/java/fr/julien/quievreux/droidplane2/data/model/Node.kt"
Task T022: "Clean up SearchManager logging in data/src/main/java/fr/julien/quievreux/droidplane2/data/SearchManager.kt"
Task T023: "Memoize computeSubtreeHeight and hasCloudInSubtree in app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapLayoutEngine.kt"
```

---

## Implementation Strategy

### MVP First (User Stories 1 & 2 - P1)

1. Complete Phase 1 (Setup) + Phase 2 (Foundational `testFixtures` move)
2. Complete Phase 3 (`US1`: Production APK hygiene & R8 minification)
3. Complete Phase 4 (`US2`: Corruption-free XML saving & rich-content escaping)
4. **STOP and VALIDATE**: Run `./gradlew testDebugUnitTest :app:assembleRelease`

### Incremental Delivery

1. Deliver P1 (`US1` + `US2`): Clean release APK + corruption-free `.mm` saving.
2. Deliver P2 (`US3` + `US4`): Leak-free rotation, background I/O, deep-tree consistency, and fast 2D layout.
3. Deliver P3 (`US5` + Polish): Dead code removal and final lint/release verification.
