# Platform Contracts: `:core`, `:data`, `:app` (`015-kmp-compose-migration`)

**Date**: 2026-10-29

---

## 1. `:core` Platform Contracts (`fr.julien.quievreux.droidplane2.core.log`)

```kotlin
// commonMain
expect fun createPlatformLogger(): Logger
```

- **Android (`androidMain`)** : Retourne `AndLogger()` utilisant `android.util.Log`.
- **Desktop (`desktopMain`)** : Retourne `Logger.Default` écrivant sur la sortie standard / erreur JVM.

---

## 2. `:data` Domain Contracts (`fr.julien.quievreux.droidplane2.data`)

- `NodeManager` (`commonMain`) :
  - `suspend fun loadMindMapFromStream(inputStream: InputStream): Result<MindMap>`
  - `fun writeMindMapToStream(outputStream: OutputStream): Result<Unit>`
  - `fun saveMindMapToFile(targetFile: File): Result<Unit>` (écriture atomique via fichier `.tmp`)
- `MindMapUri` (`commonMain`) :
  - `MindMapUri.parse(uriString: String): MindMapUri`
  - Propriétés : `scheme: String?`, `path: String?`, `fragment: String?`, `lastPathSegment: String?`

---

## 3. `:app` Presentation & Platform Contracts (`fr.julien.quievreux.droidplane2`)

### Stockage des préférences et fichiers récents (`RecentFilesRepository`)
- Défini comme interface dans `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/RecentFilesRepository.kt`.
- Injecté via Koin dans `MainViewModel` (`commonMain`).
- Fourni par le module Koin de chaque plateforme (`androidAppModule` dans `androidMain`, `desktopAppModule` dans `desktopMain`).

### Thème Compose Multiplatform (`Theme.kt`)
- `DroidplaneTheme(themeMode: ThemeMode, themeContrast: ThemeContrast, dynamicColor: Boolean, content: @Composable () -> Unit)` dans `commonMain`.
- Sur Android (`androidMain`) : supporte les couleurs dynamiques Material You (Android 12+) lorsque `dynamicColor = true` et `themeContrast == ThemeContrast.Normal`.
- Sur Desktop (`desktopMain`) : utilise les palettes Material 3 statiques (`LightColorScheme`, `DarkColorScheme`, palettes Medium/High Contrast).

### Ressources partagées (`composeResources`)
- Chaînes localisées (`values/strings.xml`, `values-fr/strings.xml`), icônes de nœuds (`drawable/icon_*.png`) et carte mentale d'exemple (`files/example.mm`) accessibles depuis `commonMain` sur Android et Desktop.
