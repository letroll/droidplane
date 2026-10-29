---

description: "Task list for feature 015-kmp-compose-migration: Migration vers Kotlin Multiplatform et Compose Multiplatform"
---

# Tasks: Migration vers Kotlin Multiplatform et Compose Multiplatform (`015-kmp-compose-migration`)

**Input**: Design documents from `/specs/015-kmp-compose-migration/`

**Prerequisites**: `plan.md` (required), `spec.md` (required for user stories), `research.md`, `data-model.md`, `contracts/platform-contracts.md`, `quickstart.md`

**Tests**: Inclus conformément au Principe III de la Constitution (Test-Driven Verification non négociable) et à la User Story 3.

**Organization**: Les tâches sont organisées par phase et par User Story (`[US1]`, `[US2]`, `[US3]`) afin de permettre une implémentation et une validation incrémentales.

## Format: `[ID] [P?] [Story?] Description`

- **[P]**: Peut être exécuté en parallèle (fichiers distincts, pas de dépendance bloquante)
- **[Story]**: User story associée (`[US1]`, `[US2]`, `[US3]`)
- Chemins de fichiers exacts inclus dans chaque description

---

## Phase 1: Setup (Shared Infrastructure & Removal of Firebase Crashlytics)

**Purpose**: Mettre à jour le catalogue de versions Gradle, les plugins racine et le workflow CI/CD pour activer Kotlin Multiplatform (`androidTarget` + `desktop` JVM) et Compose Multiplatform, et supprimer complètement Firebase Crashlytics / Analytics (FR-003, FR-006).

- [X] T001 Update version catalog to add `kotlin-multiplatform`, `jetbrains-compose`, KMP lifecycle ViewModel, `kotlinx-coroutines-swing`, and remove all Firebase Crashlytics / Google Services entries in `gradle/libs.versions.toml`
- [X] T002 Update root Gradle configuration to register `kotlin-multiplatform` and `jetbrains-compose` plugins and remove `google-services` and `firebase-crashlytics` plugins in `build.gradle.kts`
- [X] T003 [P] Remove the `Decode google-services.json` step and add Desktop KMP test verification (`:core:desktopTest :data:desktopTest :app:desktopTest`) in `.github/workflows/android_release.yml`

---

## Phase 2: Foundational (`:core` & `:data` Multiplatform Modules)

**Purpose**: Migrer les modules `:core` et `:data` vers Kotlin Multiplatform (`commonMain`, `androidMain`, `desktopMain`, `commonTest`) afin de fournir le socle commun bloquant toutes les User Stories (FR-001, FR-004, FR-007).

**⚠️ CRITICAL**: Aucune User Story de la couche `:app` ne peut être finalisée avant la fin de cette phase.

### Module `:core`

- [X] T004 Convert `:core` to Kotlin Multiplatform (`androidTarget()` + `jvm("desktop")`) with Compose Multiplatform dependencies in `core/build.gradle.kts`
- [X] T005 [P] Move `Logger.kt` (replacing `android.util.Log` in `Logger.Default` with console output and declaring `expect fun createPlatformLogger(): Logger`), `TextViewLogger.kt`, `CoreKoinModule.kt`, and `UiTools.kt` to `core/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/core/`
- [X] T006 [P] Move Android-specific implementations (`AndLogger.kt` with `actual fun createPlatformLogger()`, `ActivityExtensions.kt`, `DayNightPreviews.kt`) to `core/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/core/`
- [X] T007 [P] Create Desktop JVM logger implementation (`actual fun createPlatformLogger(): Logger = Logger.Default`) in `core/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/core/log/JvmLogger.kt`

### Module `:data`

- [X] T008 Convert `:data` to Kotlin Multiplatform (`androidTarget()` + `jvm("desktop")`) with `xmlpull`, `xpp3`, `koin-core`, and `coroutines-core` in `commonMain` in `data/build.gradle.kts`
- [X] T009 [P] Create multiplatform `MindMapUri` model (`raw: String`, `scheme: String?`, `path: String?`, `fragment: String?`, `lastPathSegment: String?`, `MindMapUri.parse(raw: String)` preserving exact string on `toString()`, and returning `fragment = "ID_..."` with `scheme = null` for internal `#ID_...` links) in `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/MindMapUri.kt`
- [X] T010 [P] Create pure-Kotlin `HtmlTextExtractor` utility (`extractPlainText(html: String): String` converting `<br>`, `</p>`, `</div>`, `</li>` to newlines, stripping `<head>`/`<style>`/tags, and decoding HTML/XML entities) in `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/util/HtmlTextExtractor.kt`
- [X] T011 Move all domain models (`MindMap.kt`, `Node.kt`, `NodeAttributes.kt`, `NodeStyle.kt`, `NodeEdge.kt`, `NodeCloud.kt`, `NodeFont.kt`, `ArrowLink.kt`), `DataKoinModule.kt`, `NodeUtils.kt`, `NodeUtilsDefaultImpl.kt`, and `NodeManager.kt` to `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/`, replacing `android.net.Uri` with `MindMapUri` and `android.text.Html.fromHtml` with `HtmlTextExtractor.extractPlainText`

**Checkpoint**: Les modules `:core` et `:data` compilent pour `androidTarget` et `desktop` JVM avec zéro dépendance Android dans `data/src/commonMain`.

---

## Phase 3: User Story 1 - Expérience utilisateur Android préservée à 100 % sur base partagée (Priority: P1) 🎯 MVP

**Goal**: Migrer `:app` vers Kotlin Multiplatform et Compose Multiplatform en déplaçant toute l'interface utilisateur Compose, les ressources, `MainViewModel`, `MainUiState` et `UpdateCheckerService` dans `commonMain`, tout en conservant 100 % des fonctionnalités Android existantes (`MainActivity`, SAF, téléchargement APK, préférences `SharedPreferences`) sans régression (FR-002, FR-005, FR-006, FR-008, SC-001, SC-003).

**Independent Test**: Compiler et assembler l'application Android (`./gradlew :app:assembleDebug` et `./gradlew :app:testDebugUnitTest`), ouvrir un fichier `.mm` ou la carte d'exemple, éditer/déplacer/rechercher/sauvegarder des nœuds, changer le thème et vérifier les mises à jour GitHub.

### Implementation for User Story 1

- [X] T012 [US1] Convert `app/build.gradle.kts` to Kotlin Multiplatform (`androidTarget()` + `jvm("desktop")`) + `com.android.application` + `org.jetbrains.compose`, preserving `val appVersionCode` and `val appVersionName` and removing Firebase plugins/dependencies in `app/build.gradle.kts`
- [X] T013 [P] [US1] Migrate shared string resources (`values/strings.xml`, `values-fr/strings.xml`), Freeplane node icons (`drawable/icon_*.png`), and sample mindmap (`files/example.mm`) to Compose Multiplatform resources in `app/src/commonMain/composeResources/` while keeping Android manifest resources (`mipmap-*`, `xml/file_paths.xml`, `values/themes.xml`) in `app/src/androidMain/res/`
- [X] T014 [P] [US1] Extract `RecentFilesRepository` interface (`getRecentFiles`, `addRecentFile`, `clearRecentFiles`, `saveLastOpenedUri`, `getLastOpenedUri`, `clearLastOpenedUri`, `saveThemeMode`, `getThemeMode`, `saveThemeContrast`, `getThemeContrast`) in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/RecentFilesRepository.kt` and implement `AndroidRecentFilesRepository` backed by `SharedPreferences` (preserving keys `"recent_files_prefs"`, `"recent_uris"`, `"last_opened_uri"`, `"theme_mode"`, `"theme_contrast"`) in `app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/data/AndroidRecentFilesRepository.kt`
- [X] T015 [P] [US1] Move `AppVersion.kt`, `GitHubRelease.kt`, and `UpdateCheckerService.kt` to `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/update/` for cross-platform GitHub release version checking
- [X] T016 [P] [US1] Create multiplatform utilities (`DateUtils.kt`, `DensityUtils.kt`, and `MimeTypeUtils.kt` replacing `android.webkit.MimeTypeMap`) in `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/util/`
- [X] T017 [US1] Move `MainUiState.kt` (replacing `ViewIntentNode(val intent: Intent, val node: Node)` with platform-agnostic `ViewIntentNode(val uriString: String, val mimeType: String?, val node: Node)`), `SaveTargetResolver.kt`, and `MainViewModel.kt` (using KMP `ViewModel` and `MindMapUri`) to `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/presentation/`
- [X] T018 [P] [US1] Move theme files (`Color.kt`, `Shapes.kt`, `Type.kt`, `Theme.kt`) to `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/theme/` with `expect`/`actual` dynamic color scheme support in `app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/ui/theme/PlatformTheme.android.kt`
- [X] T019 [US1] Move all shared Compose UI components, dialogs, inspector sheets, 2D canvas screens, and `IconMapper.kt` to `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/ui/`, updating resource references to `composeResources` (`Res.string.*`, `Res.drawable.*`) and preserving `@Preview` annotations
- [X] T020 [US1] Move Android entry points and platform helpers (`MainApplication.kt`, `MainActivity.kt`, `RichTextViewActivity.kt`, `ApkDownloadHelper.kt`, `AndroidManifest.xml`) to `app/src/androidMain/` and wire `AppKoinModule.kt` (`commonMain`) + `AndroidAppKoinModule.kt` (`androidMain`)

**Checkpoint**: User Story 1 est complète — l'application Android compile et fonctionne à 100 % sur la base partagée KMP + Compose Multiplatform sans Firebase Crashlytics.

---

## Phase 4: User Story 2 - Exécution de l'application sur Desktop (JVM : macOS, Windows, Linux) (Priority: P2)

**Goal**: Fournir l'application Desktop JVM complète utilisant l'interface Compose Multiplatform partagée, la persistance des préférences/fichiers récents Desktop, les boîtes de dialogue natives d'ouverture/sauvegarde de fichiers `.mm` et l'ouverture des liens externes / releases GitHub dans le navigateur système (FR-002, FR-003, FR-004, FR-005, FR-006).

**Independent Test**: Compiler la cible Desktop (`./gradlew :app:compileKotlinDesktop`), vérifier l'ouverture de la carte d'exemple, l'ouverture/sauvegarde d'un fichier `.mm` local et la vérification de mise à jour GitHub.

### Implementation for User Story 2

- [X] T021 [P] [US2] Implement `DesktopRecentFilesRepository` using `java.util.prefs.Preferences` (node `fr.julien.quievreux.droidplane2`) in `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/data/DesktopRecentFilesRepository.kt`
- [X] T022 [P] [US2] Implement Desktop platform theme hook (`actual` color scheme provider) in `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/theme/PlatformTheme.desktop.kt`
- [X] T023 [US2] Implement `DesktopDroidplaneApp` composable orchestrating `MainViewModel`, native `.mm` file open/save dialogs (`java.awt.FileDialog`), sample mindmap loading (`Res.readBytes("files/example.mm")`), system clipboard, external link browser launching (`Desktop.getDesktop().browse`), and GitHub release update dialog in `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/view/DesktopDroidplaneApp.kt` (including `@Preview`)
- [X] T024 [US2] Create Desktop application entry point (`main()`) initializing Koin (`coreKoinModule`, `dataKoinModule`, `appKoinModule`, `desktopAppModule`) and Compose Desktop `Window` in `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/main.kt`

**Checkpoint**: User Stories 1 ET 2 fonctionnent — Droidplane s'exécute sur Android et sur Desktop JVM à partir du même code partagé.

---

## Phase 5: User Story 3 - Validation automatisée multiplateforme de la logique et des modèles (Priority: P3)

**Goal**: Migrer et enrichir la suite de tests automatisés dans `commonTest`, `androidUnitTest` et `desktopTest` avec `kotlin.test` pour valider le parsing/écriture `.mm`, `MindMapUri`, `HtmlTextExtractor`, `NodeManager`, `MainViewModel`, `UpdateCheckerService` et `VersionConfigurationTest` sur toutes les plateformes cibles (SC-002, SC-004).

**Independent Test**: Exécuter `./gradlew :core:testDebugUnitTest :core:desktopTest :data:testDebugUnitTest :data:desktopTest :app:testDebugUnitTest :app:desktopTest` avec 0 échec.

### Implementation for User Story 3

- [X] T025 [P] [US3] Move `:core` unit tests to `core/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/core/log/LoggerTest.kt` using `kotlin.test`
- [X] T026 [P] [US3] Move all `:data` unit tests (`MindMapRoundTripTest.kt`, `NodeManagerTest.kt`, `NodeStyleAttributesTest.kt`, `RichTextEditingTest.kt`, `TestParser.kt`) and `.mm` test fixtures (`example.mm`, `freeplane_1_11_test.mm`, `test.mm`, `Test-2026-04-06.mm`) to `data/src/commonTest/` using `kotlin.test`
- [X] T027 [P] [US3] Add unit tests for `MindMapUri` (schemes, paths, fragments, `#ID_...` internal links, lossless `toString()`) and `HtmlTextExtractor` (HTML tags, line breaks, XML/HTML entities) in `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/model/MindMapUriTest.kt` and `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/util/HtmlTextExtractorTest.kt`
- [X] T028 [US3] Migrate `:app` unit tests (`MainViewModelTest.kt`, `MainViewModelSearchTest.kt`, `MainViewModelRichTextTest.kt`, `UpdateCheckerServiceTest.kt`, `AppVersionTest.kt`, `SaveTargetResolverTest.kt`, `VersionConfigurationTest.kt`, `MindMapLayoutEngineTest.kt`, `MindMapDragDropTest.kt`, `NodeStyleResolverTest.kt`, `ClipboardHelperTest.kt`) to KMP test source sets (`commonTest` / `androidUnitTest`) using `kotlin.test` and updated `MindMapUri` / `ViewIntentNode` signatures

**Checkpoint**: 100 % des tests unitaires passent sur Android et Desktop JVM.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation finale multiplateforme, nettoyage des anciens répertoires sources et vérification de bout en bout.

- [X] T029 Remove obsolete empty legacy source directories (`core/src/main`, `core/src/test`, `data/src/main`, `data/src/test`, `app/src/main`, `app/src/test`) after verifying all files are migrated to KMP source sets
- [X] T030 Run full multiplatform verification suite (`./gradlew :core:testDebugUnitTest :core:desktopTest :data:testDebugUnitTest :data:desktopTest :app:testDebugUnitTest :app:desktopTest :app:assembleDebug :app:compileKotlinDesktop`) per `specs/015-kmp-compose-migration/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1 : T001–T003)**: Aucune dépendance — démarre immédiatement.
- **Foundational (Phase 2 : T004–T011)**: Dépend de la Phase 1 — **BLOQUE** les Phases 3, 4 et 5.
- **User Story 1 (Phase 3 : T012–T020)**: Dépend de la Phase 2.
- **User Story 2 (Phase 4 : T021–T024)**: Dépend de la Phase 2 et du partage UI de la Phase 3 (`commonMain`).
- **User Story 3 (Phase 5 : T025–T028)**: T025–T027 peuvent démarrer dès la fin de la Phase 2 ; T028 dépend de la Phase 3.
- **Polish (Phase 6 : T029–T030)**: Dépend des Phases 3, 4 et 5.

### Parallel Opportunities

- **Phase 2**: T005, T006, T007 peuvent être exécutés en parallèle ; T009 et T010 peuvent être exécutés en parallèle.
- **Phase 3 (US1)**: T013, T014, T015, T016, T018 peuvent être réalisés en parallèle une fois T012 configuré.
- **Phase 4 (US2)**: T021 et T022 peuvent être réalisés en parallèle.
- **Phase 5 (US3)**: T025, T026, T027 peuvent être réalisés en parallèle.

---

## Parallel Example: Foundational & User Story 1

```bash
# Phase 2 (:data multiplatform helpers in parallel):
Task T009: "Create multiplatform MindMapUri model in data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/MindMapUri.kt"
Task T010: "Create pure-Kotlin HtmlTextExtractor utility in data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/util/HtmlTextExtractor.kt"

# Phase 3 (User Story 1 shared modules in parallel):
Task T014: "Extract RecentFilesRepository interface in app/src/commonMain/... and AndroidRecentFilesRepository in app/src/androidMain/..."
Task T015: "Move AppVersion.kt, GitHubRelease.kt, and UpdateCheckerService.kt to app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/update/"
Task T016: "Create multiplatform utilities (DateUtils.kt, DensityUtils.kt, MimeTypeUtils.kt) in app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/util/"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Compléter la **Phase 1 (Setup)** et la **Phase 2 (Foundational `:core` & `:data`)**.
2. Compléter la **Phase 3 (User Story 1)** pour faire tourner l'application Android sur la base partagée KMP + Compose Multiplatform sans Firebase Crashlytics.
3. **STOP & VALIDATE** : Vérifier que l'APK Android compile et que toutes les fonctionnalités existantes marchent à l'identique.

### Incremental Delivery

1. **Setup + Foundational** → `:core` et `:data` en KMP pur.
2. **User Story 1 (P1)** → `:app` en Compose Multiplatform + Android fonctionnel à 100 % (MVP).
3. **User Story 2 (P2)** → Application Desktop JVM fonctionnelle avec fenêtrage, fichiers `.mm` et mise à jour GitHub.
4. **User Story 3 (P3)** → Suite complète de tests unitaires multiplateformes (`androidUnitTest` + `desktopTest`).
