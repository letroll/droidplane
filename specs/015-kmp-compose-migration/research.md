# Phase 0 Research: Migration vers Kotlin Multiplatform & Compose Multiplatform (`015-kmp-compose-migration`)

**Date**: 2026-10-29  
**Feature Branch**: `015-kmp-compose-migration`

---

## 1. Architecture Multi-Modules KMP (`:core`, `:data`, `:app`) & Préservation du Versionnage

### Decision
Convertir les trois modules existants (`:core`, `:data`, `:app`) au plugin `org.jetbrains.kotlin.multiplatform` (`kotlin("multiplatform")`) avec les cibles `androidTarget()` et `jvm("desktop")`. Le module `:app` conserve le plugin `com.android.application` ainsi que les variables `val appVersionCode = ...` et `val appVersionName = "1.0.$appVersionCode"`.

### Rationale
- Respecte le **Principe I de la Constitution** (séparation stricte `:core`, `:data`, `:app`).
- Préserve l'intégralité du pipeline CI/CD (`.github/workflows/android_release.yml`), du script `scripts/bump-version.sh` et du test de non-régression `VersionConfigurationTest.kt` sans déplacer `app/build.gradle.kts`.
- Permet d'exécuter l'application sur Android (`:app:assembleDebug` / `:app:assembleRelease`) et sur Desktop JVM (`:app:run`, `:app:packageDmg`, `:app:packageMsi`, `:app:packageDeb`) depuis le même module `:app`.

### Alternatives considered
- **Créer un 4e module `:composeApp` ou `:shared`** : Rejeté car cela briserait la structure à 3 modules inscrite dans la Constitution (Principe I) et casserait `scripts/bump-version.sh` ainsi que `VersionConfigurationTest.kt`.

---

## 2. Parsing XML Freeplane (`.mm`) et I/O dans `:data` (`commonMain`)

### Decision
Conserver `org.xmlpull.v1.XmlPullParser` / `XmlPullParserFactory` (via `libs.xmlpull` et `libs.xpp3`) ainsi que `java.io.InputStream`, `java.io.OutputStream` et `java.io.File` dans `data/src/commonMain/kotlin`, et remplacer les deux seules dépendances Android (`android.net.Uri` et `android.text.Html`) par des implémentations pures Kotlin dans `commonMain` (`MindMapUri` et `HtmlTextExtractor`).

### Rationale
- Les deux cibles retenues (`androidTarget()` et `jvm("desktop")`) s'exécutent sur la JVM et partagent nativement les API `java.io.*` et `xmlpull`/`xpp3` (qui sont de pures bibliothèques Java sans dépendance au runtime Android).
- Remplacer `android.net.Uri` par `MindMapUri` dans `:data` élimine le besoin de mocks ou de Robolectric lors de l'exécution des tests unitaires sur les nœuds contenant des liens (`node.link`).
- Remplacer `android.text.Html.fromHtml(richContent)` par `HtmlTextExtractor.extractPlainText(html)` garantit un rendu identique des notes et nœuds HTML riches sur Android et Desktop tout en étant 100 % testable dans `commonTest`.

### Alternatives considered
- **Réécrire tout le parser XML avec `xmlutil` (pdvrieze)** : Rejeté (principe YAGNI de la Constitution) car `xmlpull` + `xpp3` fonctionne déjà sur Android et Desktop JVM avec 100 % de compatibilité Freeplane prouvée par les tests existants.

---

## 3. Partage de l'UI avec Compose Multiplatform & Ressources (`composeResources`)

### Decision
Utiliser le plugin `org.jetbrains.compose` (Compose Multiplatform 1.7.3+ compatible Kotlin 2.3) dans `:core` et `:app`. Migrer les ressources partagées (`strings.xml`, `values-fr/strings.xml`, `drawable/icon_*.png`, `files/example.mm`) dans `app/src/commonMain/composeResources` tout en conservant dans `app/src/androidMain/res` les ressources strictement requises par le `AndroidManifest.xml` (icônes de lanceur `@mipmap/ic_launcher`, `@xml/file_paths`, thèmes XML d'activité).

### Rationale
- `org.jetbrains.compose.resources` génère une classe `Res` typée accessible depuis `commonMain` sur Android et Desktop (`stringResource(Res.string.xxx)`, `painterResource(Res.drawable.xxx)`, `Res.readBytes("files/example.mm")`).
- Permet de partager plus de 90 % du code UI (`MindMapCanvasScreen`, `MindMapNodeCard`, `MindMapLayoutEngine`, `NodeInspectorSheet`, dialogues, thèmes) dans `app/src/commonMain/kotlin`.

### Alternatives considered
- **Dupliquer les chaînes et icônes entre Android et Desktop** : Rejeté car source de désynchronisation et contraire à FR-002.

---

## 4. Gestion du ViewModel, de l'État (`MainUiState`) et des Actions Plateformes

### Decision
- Utiliser `org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose` dans `commonMain` pour `MainViewModel`.
- Remplacer `MainUiState.ViewIntentNode(val intent: Intent, val node: Node)` par `MainUiState.ViewIntentNode(val uriString: String, val mimeType: String?, val node: Node)` (ou `ExternalLinkRequest`) afin de supprimer tout import `android.content.Intent` et `android.webkit.MimeTypeMap` de `commonMain`.
- Extraire `RecentFilesRepository` en interface dans `commonMain`, avec `AndroidRecentFilesRepository` (`SharedPreferences`) dans `androidMain` et `DesktopRecentFilesRepository` (`java.util.prefs.Preferences`) dans `desktopMain`.

### Rationale
- Maintient le flux de données unidirectionnel (**Principe II de la Constitution**) dans `commonMain`.
- Préserve les préférences et fichiers récents des utilisateurs Android existants lors d'une mise à jour (aucun changement des clés `SharedPreferences`).
- Permet de tester `MainViewModel` dans `commonTest` / `desktopTest` sans dépendance Android.

### Alternatives considered
- **Utiliser `expect`/`actual` sur `Intent`** : Rejeté car `Intent` est un concept purement Android ; exposer un modèle de domaine/présentation (`uriString` + `mimeType`) est plus propre et respecte SOLID (Principe VI).

---

## 5. Suppression de Firebase Crashlytics/Analytics & Partage de `UpdateCheckerService`

### Decision
- Retirer les plugins `com.google.gms.google-services` et `com.google.firebase.crashlytics`, les dépendances `firebase-bom`, `firebase-crashlytics`, `firebase-analytics`, ainsi que l'étape `Decode google-services.json` dans `.github/workflows/android_release.yml`.
- Conserver `UpdateCheckerService`, `GitHubRelease` et `AppVersion` dans `app/src/commonMain/kotlin`.
- Sur Android : conserver `ApkDownloadHelper` dans `androidMain` pour télécharger et installer l'APK ou ouvrir la release GitHub.
- Sur Desktop : ouvrir l'URL de la release GitHub (`release.htmlUrl`) dans le navigateur par défaut du système via `java.awt.Desktop.getDesktop().browse(URI(url))`.

### Rationale
- Répond exactement à la clarification Q2 de la spécification (`FR-006`).
- `UpdateCheckerService` utilise déjà `java.net.HttpURLConnection` et `kotlinx.serialization.json.Json`, qui fonctionnent nativement dans `commonMain` pour `androidTarget()` et `jvm("desktop")` sans ajouter de dépendance lourde.

### Alternatives considered
- **Ajouter Ktor Client uniquement pour l'appel GitHub Releases** : Rejeté (Principe V — Simplicité et YAGNI) car `HttpURLConnection` + `Dispatchers.IO` fonctionne nativement sur Android et Desktop JVM et passe déjà tous les tests unitaires existants.
