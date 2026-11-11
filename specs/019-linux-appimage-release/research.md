# Phase 0: Technical Research & Architecture Decisions (Linux AppImage)

**Feature**: `019-linux-appimage-release`  
**Date**: 2026-11-11  

---

## 1. Méthode de génération d'une AppImage pour Compose Multiplatform

### Contexte & Problématique
Le projet Droidplane produit déjà un paquet Debian `.deb` pour Linux via la tâche Gradle `:app:packageDeb`. Cependant, de nombreuses distributions Linux (Arch Linux, Fedora, Manjaro, openSUSE, Alpine, Void, etc.) n'utilisent pas le gestionnaire de paquets APT/dpkg. L'AppImage est le standard "de facto" pour fournir un exécutable Linux universel ("one app = one file"), sans nécessiter d'installation ni de droits root.
Compose Multiplatform ne fournit pas directement de format `TargetFormat.AppImage` natif dans son DSL `nativeDistributions` (uniquement `.deb` et `.rpm`), mais il fournit la tâche standard `:app:createDistributable` qui assemble l'application complète dans `app/build/compose/binaries/main/app/Droidplane/` :
- `bin/Droidplane` (lanceur binaire natif généré par jpackage)
- `lib/` (l'ensemble des bibliothèques JAR et runtime Java optimisé embarqué)
- `lib/Droidplane.png` (icône applicative)

### Décision
Adopter une stratégie d'empaquetage en deux étapes :
1. **Étape Gradle** : Exécuter `./gradlew :app:createDistributable --no-daemon` pour produire le répertoire d'application autonome avec JRE embarqué.
2. **Étape Script d'empaquetage (`scripts/package-appimage.sh`)** :
   - Structurer un répertoire standard `AppDir` contenant :
     - `AppRun` (script de lancement pointant sur `bin/Droidplane`)
     - `droidplane.desktop` (fichier de métadonnées de bureau conforme FreeDesktop)
     - `droidplane.png` (icône officielle de l'application)
     - L'arborescence applicative (`bin/`, `lib/`)
   - Télécharger / invoquer `appimagetool` pour empaqueter `AppDir` en `Droidplane-<version>-x86_64.AppImage`.
   - Placer l'AppImage générée dans `app/build/compose/binaries/main/appimage/`.

### Justification & Rationale
- `appimagetool` est l'outil officiel maintenu par le projet AppImageKit.
- Le binaire produit embarque son propre JRE optimisé extrait par `createDistributable` ; il n'y a donc aucun prérequis Java sur la machine cible de l'utilisateur.
- La séparation via un script dédié `scripts/package-appimage.sh` permet une exécution aussi bien en local par le développeur que dans le runner CI/CD GitHub Actions sans alourdir `build.gradle.kts` avec des dépendances système externes.

### Alternatives considérées
- *Plugins Gradle tiers (comme `gradle-appimage-plugin`)* : Souvent obsolètes, mal maintenus avec Gradle 9.x / AGP 9.x, et ajoutent des dépendances fragiles non vérifiables au catalogue de versions. Un script shell propre exploitant les sorties natives de Compose Multiplatform respecte les principes de simplicité et YAGNI de la Constitution Droidplane.
- *Fournir uniquement un paquet `.rpm`* : Moins universel qu'une AppImage et n'élimine pas les frictions sur les distributions non-RPM (Arch, Slackware, etc.).

---

## 2. Exécution d'appimagetool en environnement CI/CD et conteneurs (FUSE-less)

### Contexte & Problématique
`appimagetool` est lui-même distribué sous forme d'AppImage. Par défaut, exécuter une AppImage nécessite `FUSE` (`/dev/fuse`). Dans les runners GitHub Actions ou les conteneurs Docker/sandboxes, le module FUSE n'est souvent pas disponible ou manque de privilèges `mount`.

### Décision
1. Utiliser le paramètre standard `--appimage-extract-and-run` ou la variable d'environnement `APPIMAGE_EXTRACT_AND_RUN=1` lors de l'exécution d'`appimagetool`.
2. Dans le script `scripts/package-appimage.sh`, vérifier si `appimagetool` est déjà installé localement (`/usr/local/bin/appimagetool` ou `which appimagetool`) ; s'il est absent (ex. runner GitHub Actions `ubuntu-latest`), le script le télécharge automatiquement depuis la release officielle AppImageKit dans un répertoire temporaire et le rend exécutable.

### Justification & Rationale
- Cette approche fonctionne de manière 100 % déterministe sur les runners hébergés GitHub Actions, dans les conteneurs Linux et sur les postes de développement sans nécessiter de configuration root ni d'installation de paquet supplémentaire.

---

## 3. Intégration dans le Workflow CI/CD GitHub Actions

### Contexte & Problématique
Le workflow `.github/workflows/android_release.yml` utilise un job matriciel `build-desktop` sur `ubuntu-latest` pour générer le paquet Debian via `:app:packageDeb`. L'AppImage doit être générée sur le même exécuteur Linux sans dupliquer inutilement un runner Linux complet.

### Décision
1. Mettre à jour l'étape Linux du job matriciel `build-desktop` (ou enrichir la commande) :
   - Sur l'exécuteur Linux : exécuter `:app:packageDeb :app:createDistributable`, puis invoquer `scripts/package-appimage.sh`.
   - Copier à la fois le `.deb` et le `.AppImage` dans `dist-desktop/`.
2. Mettre à jour le pattern de capture des artefacts du job `build-desktop` pour Linux :
   - `artifact_path: "app/build/compose/binaries/main/deb/*.deb app/build/compose/binaries/main/appimage/*.AppImage"`
3. Mettre à jour le job `publish-release` pour inclure `*.AppImage` dans la collection des actifs finaux attachés à la release GitHub.

### Justification & Rationale
- L'exécution de `packageDeb` et `createDistributable` sur le même runner Linux réutilise le cache de compilation Gradle (`compileKotlinDesktop`, `createRuntimeImage`), ce qui prend moins de 10 secondes supplémentaires.
- L'archive d'artefacts `release-desktop-linux` regroupe les deux livrables Linux (Debian et universel AppImage), permettant au job `publish-release` de les publier de manière synchrone.
