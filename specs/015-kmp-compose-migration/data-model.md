# Phase 1 Data Model: Migration vers Kotlin Multiplatform & Compose Multiplatform (`015-kmp-compose-migration`)

**Date**: 2026-10-29  
**Feature Branch**: `015-kmp-compose-migration`

---

## 1. Entités du domaine (`:data` — `commonMain`)

### `MindMapUri` (`fr.julien.quievreux.droidplane2.data.model.MindMapUri`)
Remplace `android.net.Uri` dans `:data` (`commonMain`) afin de représenter les hyperliens de nœuds Freeplane (`LINK="..."`) sans dépendance au SDK Android.

- **Champs & Propriétés** :
  - `raw: String` : Chaîne URI brute telle que stockée dans l'attribut `LINK` du fichier `.mm`.
  - `scheme: String?` : Schéma de l'URI (ex. `http`, `https`, `mailto`, `file`), ou `null` pour un chemin relatif ou une ancre interne (`#ID_...`).
  - `path: String?` : Chemin décodé de l'URI.
  - `fragment: String?` : Fragment après `#` (utilisé notamment pour les liens internes entre nœuds `#ID_12345`).
  - `lastPathSegment: String?` : Dernier segment non vide du `path`.
- **Règles de validation** :
  - `MindMapUri.parse(raw: String)` DOIT préserver la chaîne exacte lors de `toString()` pour garantir une sérialisation `.mm` sans perte (FR-004).
  - Un lien interne de type `#ID_1718923` DOIT retourner `fragment = "ID_1718923"` et `scheme = null`.

---

### `HtmlTextExtractor` (`fr.julien.quievreux.droidplane2.data.util.HtmlTextExtractor`)
Remplace `android.text.Html.fromHtml` dans `NodeManager` (`commonMain`).

- **Opérations** :
  - `extractPlainText(html: String): String` : Convertit les balises de saut de ligne/paragraphe (`<br>`, `</p>`, `</div>`, `</li>`) en retours à la ligne, supprime les balises HTML (`<head>`, `<style>`, `<...>`) et décode les entités HTML/XML (`&amp;`, `&lt;`, `&gt;`, `&quot;`, `&apos;`, `&nbsp;`, entités numériques `&#...;` et `&#x...;`).

---

### `Node`, `MindMap`, `NodeAttributes`, `NodeStyle`, `NodeEdge`, `NodeCloud`, `NodeFont`, `ArrowLink`
Entités existantes dans `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/` :
- Conservées à 100 % dans `commonMain`.
- Seul le type de `Node.link` évolue de `android.net.Uri?` vers `MindMapUri?`.

---

## 2. Entités de présentation & Contrats de plateforme (`:app` — `commonMain`)

### `MainUiState.ViewIntentNode` (`fr.julien.quievreux.droidplane2.presentation.MainUiState`)
Représente une demande d'ouverture de lien externe ou de fichier lié à un nœud, sans `android.content.Intent`.

- **Champs** :
  - `uriString: String` : URI cible à ouvrir.
  - `mimeType: String?` : Type MIME déduit de l'extension lorsque le lien pointe vers un fichier local, ou `null` pour une URL web.
  - `node: Node` : Nœud source de l'action.

---

### `RecentFilesRepository` (`fr.julien.quievreux.droidplane2.data.RecentFilesRepository`)
Interface multiplateforme dans `commonMain` pour la persistance des fichiers récents, du dernier fichier ouvert et des préférences de thème.

- **Méthodes** :
  - `getRecentFiles(): List<String>`
  - `addRecentFile(uri: String)`
  - `clearRecentFiles()`
  - `saveLastOpenedUri(uri: String)`
  - `getLastOpenedUri(): String?`
  - `clearLastOpenedUri()`
  - `saveThemeMode(mode: ThemeMode)`
  - `getThemeMode(): ThemeMode`
  - `saveThemeContrast(contrast: ThemeContrast)`
  - `getThemeContrast(): ThemeContrast`
- **Implémentations** :
  - `AndroidRecentFilesRepository` (`androidMain`) : basée sur `SharedPreferences` (conserve les mêmes clés `"recent_files_prefs"`, `"recent_uris"`, `"last_opened_uri"`, `"theme_mode"`, `"theme_contrast"` pour garantir zéro perte de données lors de la mise à jour Android).
  - `DesktopRecentFilesRepository` (`desktopMain`) : basée sur `java.util.prefs.Preferences` pour le nœud `fr.julien.quievreux.droidplane2`.

---

### `UpdateCheckerService` & `GitHubRelease` (`fr.julien.quievreux.droidplane2.data.update`)
Déplacés dans `commonMain` pour partager la vérification de version GitHub entre Android et Desktop :
- `checkForUpdate(currentVersionName: String): Result<GitHubRelease?>`
- Sur Android (`androidMain`) : propose le téléchargement/installation de l'APK (`ApkDownloadHelper`) ou l'ouverture de la page GitHub.
- Sur Desktop (`desktopMain`) : ouvre `release.htmlUrl` dans le navigateur système.
