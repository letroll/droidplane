# Phase 0 Research: Codebase Audit Remediation & Quality Hardening (`012-audit-remediation`)

**Date**: 2026-10-16  
**Feature**: [spec.md](spec.md)

## Research Decisions

### 1. Isolating Shared Test Utilities (`KStringSpec` and `FakeDataSource`) from Production APK

- **Decision**: Move `KStringSpec.kt` from `core/src/main` to `core/src/testFixtures/java/...` (enabling `testFixtures.enable = true` in `core/build.gradle.kts`) and move `FakeDataSource.kt` from `data/src/main` to `data/src/testFixtures/java/...` (enabling `testFixtures.enable = true` in `data/build.gradle.kts`). Replace all `api(libs.kotest.*)`, `api(libs.mockk)`, `api(libs.junit)`, `api(libs.androidx.junit)`, and `api(libs.androidx.espresso.core)` in `core/build.gradle.kts` with `testFixturesApi(...)` / `testImplementation(...)` / `androidTestImplementation(...)`, and change `implementation(libs.turbine)` in `app/build.gradle.kts` to `testImplementation(libs.turbine)`. Remove the `excludes += " **/attach_hotspot_windows.dll"` packaging workaround from `core/build.gradle.kts`.
- **Rationale**:
  - `KStringSpec` is inherited by unit tests across `:data` and `:app`.
  - `FakeDataSource` is referenced by unit tests in `:data` and `:app`, as well as Android instrumentation tests in `:app` (`NodeListTest.kt`, `DeleteConfirmationDialogTest.kt`).
  - Android Gradle Plugin's built-in `testFixtures` feature allows `:data` and `:app` to depend on `testImplementation(testFixtures(project(":core")))` and `testImplementation(testFixtures(project(":data")))` / `androidTestImplementation(testFixtures(project(":data")))` without duplicating code and while guaranteeing zero test dependencies or fake classes enter the release APK.
- **Alternatives considered**:
  - *Duplicating `KStringSpec` and `FakeDataSource` into each module's `src/test` and `src/androidTest`*: Rejected because it violates DRY and creates maintenance drift across 3 source sets.
  - *Creating a separate `:testing` Gradle module*: Rejected as heavier than AGP's native `testFixtures` capability and unnecessary per Constitution Principle V (Simplicity/YAGNI).

---

### 2. Enabling R8 Minification Safely (`isMinifyEnabled = true`)

- **Decision**: Enable `isMinifyEnabled = true` in `app/build.gradle.kts` (`buildTypes.release`) and create `app/proguard-rules.pro` with explicit keep rules for Koin dependency injection, `org.xmlpull.v1.**` (XmlPullParser), and Kotlin Serialization / reflection-sensitive models if needed.
- **Rationale**:
  - `app/build.gradle.kts` includes `androidx.compose.material:material-icons-extended`, which adds thousands of unused vector icon methods unless R8 tree-shaking is enabled.
  - Currently `app/proguard-rules.pro` is referenced in `app/build.gradle.kts:52` but does not exist on disk. Creating `app/proguard-rules.pro` and verifying `./gradlew assembleRelease` ensures R8 strips unused icons and library code without breaking runtime DI or XML parsing.
- **Alternatives considered**:
  - *Replacing `material-icons-extended` manually while leaving `isMinifyEnabled = false`*: Rejected because R8 shrinking optimizes the entire app across Compose, Koin, and Kotlin stdlib.

---

### 3. Atomic Mind Map Serialization Without Global CDATA Stripping

- **Decision**:
  1. In `NodeManager.serializeMindmap`, add immediate `return` statements after `onError.invoke(...)` when `isInvalidFilePath(filePath)` or `isInvalidFileName(filename)` is true, and move `onSaveFinished?.invoke(fileSaveDestination)` inside the `try` block so it is never called when an exception occurs.
  2. Write the XML output to `tempFile` (`File(filePath, "temp_$filename")`) and atomically move/replace `tempFile` onto `fileSaveDestination` (`Files.move(tempFile.toPath(), fileSaveDestination.toPath(), StandardCopyOption.REPLACE_EXISTING)` with fallback to `tempFile.renameTo(fileSaveDestination)`), completely removing the second line-by-line pass that called `.replace("<![CDATA[", "").replace("]]>", "")`.
- **Rationale**:
  - Inspection of `NodeManager.writeRawXmlToSerializer` (lines 767–796) confirms that `<richcontent>` HTML is parsed via `XmlPullParser` and emitted as proper XML start/end tags rather than wrapped in a CDATA section.
  - The line-by-line `.replace("<![CDATA[", "").replace("]]>", "")` was an obsolete leftover that corrupted any user node text or code snippet containing literal `<![CDATA[` or `]]>` and doubled disk I/O.
  - Keeping `tempFile` and replacing `fileSaveDestination` on completion satisfies Constitution Principle IV ("File saving MUST be atomic via safe replacement or staging").
- **Alternatives considered**:
  - *Writing directly to `fileSaveDestination` without `tempFile`*: Rejected because an interrupted write would corrupt an existing local file, violating Constitution Principle IV.

---

### 4. Rich Content HTML Entity Preservation (`NodeUtilsDefaultImpl.loadRichContent`)

- **Decision**: Refactor `NodeUtilsDefaultImpl.loadRichContent` to use a `StringBuilder` instead of repeated `+=` string concatenation, and escape XML/HTML special characters (`&`, `<`, `>`, `"`) in `XmlPullParser.TEXT` and attribute values when reconstructing the inner HTML string.
- **Rationale**:
  - `XmlPullParser` resolves entities (`&lt;` -> `<`, `&amp;` -> `&`) when reading `parser.text` and `parser.getAttributeValue(i)`.
  - Previously, `loadRichContent` appended raw `parser.text` directly into the HTML string. When `writeRawXmlToSerializer` later re-parsed that HTML string on save, unescaped `<` or `&` caused `XmlPullParserException`, falling back to `serializer.text(xmlString)` and destroying the HTML structure.
- **Alternatives considered**:
  - *Using `android.text.TextUtils.htmlEncode`*: Rejected because `:data` runs fast JVM unit tests on JUnit Platform without Robolectric (`android.text.TextUtils` throws `RuntimeException: Stub!` in unit tests). A pure Kotlin XML entity escaping helper in `:data` works identically in JVM unit tests and Android runtime.

---

### 5. Eliminating `FileRegister` Activity Leak & Off-Main-Thread Document Saves

- **Decision**:
  1. Remove `private var fileRegister: FileRegister? = null` and `setFileRegister` from `MainViewModel`, and delete `FileRegister.kt`.
  2. Add a one-shot `pendingSaveTempFile: File? = null` field in `MainUiState` (and `clearPendingSaveTempFile()` in `MainViewModel`) and pass `filesDirPath: String` as a parameter to `MainViewModel.launchSaveFile(filesDirPath: String)`.
  3. When `MainUiState.pendingSaveTempFile` is non-null, handle it inside a `LaunchedEffect(state.value.pendingSaveTempFile)` in `MainActivity`: if overwriting an existing target URI, launch a coroutine on `Dispatchers.IO` (`lifecycleScope.launch(Dispatchers.IO)`) that opens the `ContentResolver` output stream and streams the file via `tempFile.inputStream().use { it.copyTo(outputStream) }`; otherwise launch `saveFileLauncher.launch(filename)`.
- **Rationale**:
  - Completely decouples `MainViewModel` from `MainActivity` lifecycle, fixing the configuration-change memory leak (Must Fix #3).
  - Moves `ContentResolver` stream opening and file copying off the main UI thread and replaces full-file `file.readText().toByteArray()` RAM allocation with a buffered byte stream copy (Must Fix #4).
- **Alternatives considered**:
  - *Clearing `fileRegister = null` in `MainActivity.onDestroy()`*: Rejected because it still violates unidirectional data flow (Constitution Principle II) and drops saves that complete while the Activity is rotating.

---

### 6. Linear-Time XML Parsing & Deep Tree `parentNode` Consistency

- **Decision**:
  1. Include `fontSize`, `fontName`, `isBold`, `isItalic`, `isFolded`, `latexEquation`, and `externalObject` in `Node.equals` and `Node.hashCode`.
  2. Call `updateDescendantParents(fixedChild)` inside `NodeManager.propagateUpdatedParentToRoot` so grandchildren and deeper nodes always point to the latest `parentNode` instance.
  3. Remove `_allNodes.update { it + nodeRelation.child }` from `NodeManager.addChildIntoParent` during XML parsing; instead, collect parsed nodes into `allParsedNodes` via `onParentNodeUpdate` (or a tree traversal helper) and publish `_allNodes.value = allParsedNodes` once at the end of `loadMindMapFromXml`.
- **Rationale**:
  - Eliminates $O(N^2)$ list copying and `SearchManager` combine triggers on every `<node>` tag during file load.
  - Ensures Compose recomposes when font/style properties change and prevents upward navigation (`node.parentNode`) from jumping to a stale branch after editing an ancestor.
- **Alternatives considered**:
  - *Converting `Node` to a deeply immutable value tree with ID-only parent lookups*: Deferred as a larger architectural rewrite; fixing `equals`/`hashCode` and calling the already-existing `updateDescendantParents` resolves the bug immediately within the current architecture.

---

### 7. 2D Canvas Layout Memoization & Selection Decoupling

- **Decision**:
  1. In `MindMapCanvasScreen.kt`, remove `selectedNodeId` from the `remember(rootNode, treeVersion, collapsedNodeIds, density.density)` keys for `MindMapLayoutEngine.computeLayout`, and pass `isSelected = (nodeLayout.node.id == selectedNodeId)` directly to `MindMapNodeCard`.
  2. In `MindMapLayoutEngine.kt`, memoize `computeSubtreeHeight` and `hasCloudInSubtree` in a per-pass `MutableMap<String, Float>` / `MutableMap<String, Boolean>` (or single bottom-up pass) so subtree heights and cloud flags are computed at most once per node per layout pass. Keep `selectedNodeId: String? = null` as an optional parameter on `computeLayout` for backward compatibility with existing unit tests (`MindMapLayoutEngineTest`).
- **Rationale**:
  - Tapping a node on the 2D canvas will only recompose the affected `MindMapNodeCard` selection border instead of recalculating the entire 2D tree layout.
  - Memoizing subtree height eliminates redundant $O(N \cdot \text{depth})$ recursion and `Html.fromHtml` calls on deep trees.
- **Alternatives considered**:
  - *Removing `selectedNodeId` from `MindMapLayoutEngine.computeLayout` signature*: Rejected because `MindMapLayoutEngineTest` directly tests `computeLayout(..., selectedNodeId = "child")`; keeping the parameter with default `null` preserves test compatibility while removing it from the `remember` trigger in `MindMapCanvasScreen`.

---

### 8. Secure `FileProvider` Sharing for Relative File Links

- **Decision**: Declare `androidx.core.content.FileProvider` in `app/src/main/AndroidManifest.xml` with authority `${applicationId}.fileprovider` and `app/src/main/res/xml/file_paths.xml` covering internal and external storage paths. In `MainActivity`, when launching `viewIntentNode.intent` for `RelativeFile`, convert any `file://` URI into a `content://` URI via `FileProvider.getUriForFile(...)` and set `Intent.FLAG_GRANT_READ_URI_PERMISSION`.
- **Rationale**:
  - Android 7.0+ (`minSdk = 26`) throws `FileUriExposedException` whenever a `file://` URI is passed via an external `Intent`.
  - Performing the `FileProvider.getUriForFile` conversion in `MainActivity` keeps `MainViewModel` free of Android `Context` dependencies.
- **Alternatives considered**:
  - *Injecting `Application` context into `MainViewModel`*: Rejected to keep `MainViewModel` pure and easily unit-testable on the JVM.
