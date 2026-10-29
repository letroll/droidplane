# Quickstart & Validation Guide: Migration KMP & Compose Multiplatform (`015-kmp-compose-migration`)

**Date**: 2026-10-29  
**Feature Branch**: `015-kmp-compose-migration`

---

## Prérequis

- **JDK 17+** (ou JDK 21) configuré dans le `PATH` / `JAVA_HOME`.
- **Android SDK** (API 36) pour la compilation et l'exécution de la cible Android.
- Aucun fichier `google-services.json` n'est requis (Firebase Crashlytics et Analytics sont supprimés).

---

## 1. Exécuter les tests unitaires multiplateformes (Android + Desktop JVM)

Valide les modèles de domaine, le parsing/sérialisation XML `.mm`, `NodeManager`, `MindMapUri`, `HtmlTextExtractor`, `MainViewModel`, `UpdateCheckerService` et `VersionConfigurationTest` :

```bash
./gradlew :core:testDebugUnitTest :core:desktopTest \
          :data:testDebugUnitTest :data:desktopTest \
          :app:testDebugUnitTest :app:desktopTest
```

**Résultat attendu** : `BUILD SUCCESSFUL` avec 0 test en échec sur les deux cibles (`androidUnitTest` et `desktopTest`).

---

## 2. Compiler et vérifier l'application Android

```bash
./gradlew :app:assembleDebug
```

**Résultat attendu** :
- Génération de l'APK `app/build/outputs/apk/debug/app-debug.apk` sans dépendance à Firebase Crashlytics / Google Services.
- Ouverture de la carte d'exemple, ouverture/édition/sauvegarde d'un fichier `.mm`, glisser-déposer 2D, changement de thème et vérification de version GitHub fonctionnels.

---

## 3. Compiler et lancer l'application Desktop (macOS / Windows / Linux)

Pour vérifier la compilation de la cible Desktop JVM :

```bash
./gradlew :app:compileKotlinDesktop
```

Pour lancer l'application Desktop en mode fenêtré :

```bash
./gradlew :app:run
```

**Résultat attendu** :
- Une fenêtre Desktop **Droidplane** s'ouvre avec la carte mentale d'exemple chargée sur le canevas 2D Compose Multiplatform.
- Les menus permettent d'ouvrir un fichier `.mm` via le sélecteur de fichiers natif, d'éditer les nœuds, de sauvegarder atomiquement sur le disque et de vérifier les mises à jour GitHub.
