# Implementation Plan: Migration vers Kotlin Multiplatform et Compose Multiplatform

**Branch**: `015-kmp-compose-migration` | **Date**: 2026-10-29 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/015-kmp-compose-migration/spec.md`

## Summary

Migrer l'intégralité de **Droidplane** (`:core`, `:data`, `:app`) vers **Kotlin Multiplatform (KMP)** et **Compose Multiplatform** afin de supporter **Android** et **Desktop (JVM : macOS, Windows, Linux)** à partir d'une base de code partagée (`commonMain`). La migration supprime complètement **Firebase Crashlytics et Analytics**, conserve la vérification de nouvelle version via **GitHub Releases (`UpdateCheckerService`)** sur toutes les plateformes, et garantit 100 % de compatibilité avec le format Freeplane `.mm` ainsi que zéro régression fonctionnelle sur Android.

## Technical Context

**Language/Version**: Kotlin 2.3.20 (JVM target 17 pour Android et Desktop JVM)  
**Primary Dependencies**:
- Kotlin Multiplatform (`org.jetbrains.kotlin.multiplatform`)
- Compose Multiplatform (`org.jetbrains.compose`, `org.jetbrains.kotlin.plugin.compose`, Material 3, `compose.components.resources`)
- Lifecycle ViewModel Compose Multiplatform (`org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose`)
- Koin (`io.insert-koin:koin-core`, `koin-android`)
- Kotlinx Coroutines (`kotlinx-coroutines-core`, `kotlinx-coroutines-swing`, `kotlinx-coroutines-test`)
- Kotlinx Serialization JSON (`kotlinx-serialization-json` pour l'API GitHub Releases)
- XmlPullParser & XPP3 (`xmlpull:xmlpull:1.1.3.1`, `xpp3:xpp3:1.1.4c`)

**Storage**: Fichiers locaux Freeplane `.mm` (lecture/écriture atomique via `InputStream`/`OutputStream`/`File`) ; préférences locales via `SharedPreferences` sur Android (`androidMain`) et `java.util.prefs.Preferences` sur Desktop (`desktopMain`).  
**Testing**: `kotlin.test` (`commonTest`, `androidUnitTest`, `desktopTest`), `kotlinx-coroutines-test`, `MockK`.  
**Target Platform**: Android (Min SDK 24, Target/Compile SDK 36) + Desktop JVM (macOS, Windows, Linux).  
**Project Type**: Mobile + Desktop Application (Kotlin Multiplatform & Compose Multiplatform).  
**Performance Goals**: Rendu fluide 60/120 fps sur le canevas 2D interactif ; parsing XML et I/O exécutés hors du thread principal (`Dispatchers.IO` / `Dispatchers.Default`).  
**Constraints**:
- Zéro dépendance à Firebase Crashlytics / Analytics (`google-services` et `firebase-crashlytics` retirés).
- Préservation stricte du format `.mm` Freeplane et de la compatibilité ascendante des préférences Android.
- Préservation des variables `appVersionCode` et `appVersionName` dans `app/build.gradle.kts` pour `scripts/bump-version.sh` et `VersionConfigurationTest.kt`.

**Scale/Scope**: 3 modules Gradle (`:core`, `:data`, `:app`), ~55 fichiers Kotlin, >90 % du code métier et UI partagé dans `commonMain`.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Modular Separation of Concerns**: **PASS** — Les trois modules `:core`, `:data`, `:app` et leurs règles de dépendance (`:data` dépend uniquement de `:core` sans dépendance UI ; `:app` orchestre la présentation) sont intégralement maintenus en KMP.
- **II. Unidirectional Data Flow & State Consistency**: **PASS** — `MainViewModel` et `MainUiState` dans `commonMain` exposent un `StateFlow<MainUiState>` unique alimenté par `NodeManager`.
- **III. Test-Driven Verification (NON-NEGOTIABLE)**: **PASS** — Tous les tests de domaine, de parsing `.mm`, de `NodeManager`, de `MainViewModel` et de `UpdateCheckerService` sont conservés et étendus avec `kotlin.test` sur Android et Desktop, et chaque Composable conserve ses `@Preview`.
- **IV. Freeplane Format Compatibility & Data Integrity**: **PASS** — Le parser et sérialiseur XML `.mm` ainsi que la sauvegarde atomique (`saveMindMapToFile`) restent intacts dans `data/src/commonMain`.
- **V. Performance, Large Map Responsiveness & Simplicity**: **PASS** — Opérations I/O sur `Dispatchers.IO`, pas d'ajout de librairie réseau ou XML superflue.
- **VI. SOLID Principles & Cyclomatic Complexity**: **PASS** — Extraction propre des contrats de plateforme (`Logger`, `RecentFilesRepository`, `MindMapUri`, `HtmlTextExtractor`) avec complexité cyclomatique < 20.

## Project Structure

### Documentation (this feature)

```text
specs/015-kmp-compose-migration/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── platform-contracts.md
└── tasks.md             # Phase 2 output (/speckit-tasks command)
```

### Source Code (repository root)

```text
core/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/fr/julien/quievreux/droidplane2/core/
    │   ├── di/CoreKoinModule.kt
    │   ├── log/Logger.kt
    │   ├── log/TextViewLogger.kt
    │   └── ui/UiTools.kt
    ├── androidMain/kotlin/fr/julien/quievreux/droidplane2/core/
    │   ├── extensions/ActivityExtensions.kt
    │   ├── log/AndLogger.kt
    │   └── preview/DayNightPreviews.kt
    ├── desktopMain/kotlin/fr/julien/quievreux/droidplane2/core/
    │   └── log/JvmLogger.kt
    └── commonTest/kotlin/fr/julien/quievreux/droidplane2/core/
        └── log/LoggerTest.kt

data/
├── build.gradle.kts
└── src/
    ├── commonMain/kotlin/fr/julien/quievreux/droidplane2/data/
    │   ├── NodeManager.kt
    │   ├── NodeUtils.kt
    │   ├── NodeUtilsDefaultImpl.kt
    │   ├── di/DataKoinModule.kt
    │   ├── model/ (MindMap.kt, Node.kt, MindMapUri.kt, NodeAttributes.kt, NodeStyle.kt, ...)
    │   └── util/HtmlTextExtractor.kt
    └── commonTest/
        ├── kotlin/fr/julien/quievreux/droidplane2/data/ (MindMapRoundTripTest.kt, NodeManagerTest.kt, ...)
        └── resources/ (example.mm, freeplane_1_11_test.mm, ...)

app/
├── build.gradle.kts
└── src/
    ├── commonMain/
    │   ├── composeResources/
    │   │   ├── drawable/ (icon_*.png)
    │   │   ├── files/example.mm
    │   │   ├── values/strings.xml
    │   │   └── values-fr/strings.xml
    │   └── kotlin/fr/julien/quievreux/droidplane2/
    │       ├── data/RecentFilesRepository.kt
    │       ├── data/update/ (AppVersion.kt, GitHubRelease.kt, UpdateCheckerService.kt)
    │       ├── di/AppKoinModule.kt
    │       ├── presentation/ (MainUiState.kt, MainViewModel.kt, SaveTargetResolver.kt)
    │       ├── ui/ (IconMapper.kt, components/, screens/, theme/, view/)
    │       └── util/ (DateUtils.kt, DensityUtils.kt, MimeTypeUtils.kt)
    ├── androidMain/
    │   ├── AndroidManifest.xml
    │   ├── res/ (mipmap-*, values/themes.xml, xml/file_paths.xml)
    │   └── kotlin/fr/julien/quievreux/droidplane2/
    │       ├── MainApplication.kt
    │       ├── data/AndroidRecentFilesRepository.kt
    │       ├── ui/theme/PlatformTheme.android.kt
    │       ├── ui/util/ApkDownloadHelper.kt
    │       └── ui/view/ (MainActivity.kt, RichTextViewActivity.kt)
    ├── desktopMain/
    │   └── kotlin/fr/julien/quievreux/droidplane2/
    │       ├── main.kt
    │       ├── data/DesktopRecentFilesRepository.kt
    │       ├── ui/theme/PlatformTheme.desktop.kt
    │       └── ui/view/DesktopDroidplaneApp.kt
    ├── commonTest/kotlin/fr/julien/quievreux/droidplane2/
    └── androidUnitTest/kotlin/fr/julien/quievreux/droidplane2/
```

**Structure Decision**: Maintien des 3 modules Gradle existants (`:core`, `:data`, `:app`) configurés en Kotlin Multiplatform (`androidTarget()` + `jvm("desktop")`) afin de respecter la Constitution (Principe I) et de préserver la chaîne de build/release existante.

## Complexity Tracking

Aucune violation de la Constitution détectée.
