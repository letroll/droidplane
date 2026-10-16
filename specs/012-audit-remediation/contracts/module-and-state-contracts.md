# Module, Serialization & UI State Contracts (`012-audit-remediation`)

**Date**: 2026-10-16  
**Feature**: [spec.md](../spec.md)

## 1. Gradle Module & Test Fixtures Contract (`FR-001`, `FR-002`, `FR-016`)

### `:core` Module
- **Main Configuration (`src/main`)**:
  - Contains ONLY production utilities (`Logger`, `AppVersion`, `IntentHelper`, base Compose UI theme/components).
  - Exposes ZERO test libraries (`junit`, `kotest`, `mockk`, `espresso`) via `api` or `implementation`.
  - Uses centralized `platform(libs.androidx.compose.bom)` instead of hardcoded BOM coordinates.
- **Test Fixtures Configuration (`src/testFixtures`)**:
  - `android.testFixtures.enable = true`
  - Contains `fr.julien.quievreux.droidplane2.core.testutils.KStringSpec`.
  - Declares `testFixturesApi` for Kotest (`kotest.runner.junit5`, `kotest.assertions.core`, `kotest.property`, `kotest.extensions.koin`), `mockk`, and `junit`.

### `:data` Module
- **Main Configuration (`src/main`)**:
  - Contains domain models, `NodeManager`, `SearchManager`, `RecentFilesRepository`, `UpdateCheckerService`, and XML parsers.
  - Exposes ZERO fake/mock data classes in `src/main`.
- **Test Fixtures Configuration (`src/testFixtures`)**:
  - `android.testFixtures.enable = true`
  - Contains `fr.julien.quievreux.droidplane2.data.FakeDataSource`.
- **Test Configuration (`src/test`)**:
  - Consumes `testImplementation(testFixtures(project(":core")))` and `testImplementation(testFixtures(project(":data")))`.

### `:app` Module
- **Main Configuration (`src/main`)**:
  - `release` buildType sets `isMinifyEnabled = true` with rules in `app/proguard-rules.pro`.
  - Removes `implementation(libs.turbine)` from `main` (moved to `testImplementation(libs.turbine)`).
  - Removes unused `implementation(libs.mindmap)`.
- **Test & AndroidTest Configurations**:
  - `testImplementation(testFixtures(project(":core")))`
  - `testImplementation(testFixtures(project(":data")))`
  - `androidTestImplementation(testFixtures(project(":data")))`

---

## 2. XML Serialization & Rich-Content Contract (`FR-003`, `FR-004`, `FR-005`)

### `NodeManager.serializeMindmap(filePath, filename, onError, onSaveFinished)`
- **Preconditions**:
  - If `isInvalidFilePath(filePath)` is true -> invoke `onError(IllegalArgumentException("Invalid file path"))` and **return immediately** without touching disk or invoking `onSaveFinished`.
  - If `isInvalidFileName(filename)` is true -> invoke `onError(IllegalArgumentException("Invalid file name"))` and **return immediately** without touching disk or invoking `onSaveFinished`.
- **Execution**:
  - Serialize XML UTF-8 stream to `tempFile = File(filePath, "temp_$filename")` on `Dispatchers.IO`.
  - Atomically move/replace `tempFile` onto `fileSaveDestination = File(filePath, filename)` without stripping `<![CDATA[` or `]]>` strings.
  - Invoke `onSaveFinished?.invoke(fileSaveDestination)` ONLY if serialization and file replacement succeed without throwing.
  - On exception: clean up `tempFile`, invoke `onError(e)`, and do NOT invoke `onSaveFinished`.

### `NodeUtilsDefaultImpl.loadRichContent(parser)`
- Reconstructs `<richcontent>` inner HTML using a `StringBuilder`.
- Escapes XML entities in text nodes (`&` -> `&amp;`, `<` -> `&lt;`, `>` -> `&gt;`) and attribute values (`&`, `<`, `>`, `"` -> `&quot;`) so that subsequent serialization via `writeRawXmlToSerializer` parses valid XML without falling back to raw text.

---

## 3. ViewModel <-> Activity File & URI Contracts (`FR-006`, `FR-007`, `FR-008`, `FR-014`)

### Save File Contract
- `MainViewModel.launchSaveFile(filesDirPath: String)` (replaces `FileRegister.getFilesDirPath()` and `FileRegister.launchSaveFile(file)`).
- `MainViewModel.saveFile(outputStream: OutputStream, file: File)`:
  - Streams `file.inputStream().use { input -> outputStream.use { output -> input.copyTo(output) } }` instead of `outputStream.write(file.readText().toByteArray())`.
  - Invoked by `MainActivity` from `lifecycleScope.launch(Dispatchers.IO)`.

### Startup Recent Files Verification Contract
- `MainViewModel.refreshRecentFiles(fileChecker: StorageFileChecker)` executes inside `viewModelScope.launch(Dispatchers.IO)` so `ContentResolver` queries and stream checks never run on the main thread.

### Relative File URI Contract
- `app/src/main/AndroidManifest.xml` registers `androidx.core.content.FileProvider` with `android:authorities="${applicationId}.fileprovider"` and `@xml/file_paths`.
- When `MainActivity` handles `ContentNodeType.RelativeFile`, any `file://` URI in `viewIntentNode.intent.data` is transformed via `FileProvider.getUriForFile(this, "${packageName}.fileprovider", File(uri.path!!))` with `Intent.FLAG_GRANT_READ_URI_PERMISSION` before calling `startActivity`.
