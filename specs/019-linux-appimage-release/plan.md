# Implementation Plan: Génération et inclusion du format universel AppImage pour Linux lors des releases

**Branch**: `019-linux-appimage-release` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/019-linux-appimage-release/spec.md`

## Summary

Compléter les options de distribution desktop en ajoutant le format universel **AppImage pour Linux (x86_64)** lors de chaque publication de release. La solution technique repose sur deux éléments : (1) un script d'empaquetage automatisé et déterministe `scripts/package-appimage.sh` qui assemble un répertoire standard `AppDir` (contenant `AppRun`, les métadonnées FreeDesktop `.desktop`, l'icône applicative et l'application autonome produite par `:app:createDistributable`) puis invoque `appimagetool` (avec support FUSE-less via `--appimage-extract-and-run`), et (2) l'intégration de cette étape dans le job matriciel Linux du pipeline GitHub Actions (`.github/workflows/android_release.yml`) pour téléverser et publier l'AppImage en même temps que le paquet Debian (`.deb`), l'installateur Windows (`.msi`), l'image macOS (`.dmg`) et l'APK Android.

## Technical Context

**Language/Version**: Kotlin 2.2.10 (toolchain Java 21, target JVM 21), Gradle 9.6, AGP 9.4.1, Compose Multiplatform 1.7.3, Bash 5+

**Primary Dependencies**: Compose Desktop task `:app:createDistributable`, `appimagetool` (AppImageKit continuous / v13)

**Storage**: N/A (empaquetage de binaire distribuable)

**Testing**: Validation locale de la construction de l'AppDir et de l'extraction squashfs ; validation syntaxique et d'intégration du workflow GitHub Actions

**Target Platform**: Linux x86_64 (compatible avec toutes les distributions Linux majeures : Ubuntu, Debian, Arch, Fedora, Manjaro, openSUSE, etc.)

**Project Type**: Application Kotlin Multiplatform & pipeline de livraison continue (CI/CD)

**Performance Goals**: Construction de l'AppImage en moins de 30 secondes à partir de l'arborescence `createDistributable` ; exécution directe par l'utilisateur sans installation en moins de 5 secondes

**Constraints**: Exécution possible sans privilèges root et sans pilote FUSE en environnement CI/CD (via `--appimage-extract-and-run`) ; atomicité de la release (numéro de version `appVersionName` partagé)

**Scale/Scope**: Script d'empaquetage `scripts/package-appimage.sh`, workflow CI/CD `.github/workflows/android_release.yml`, et documentation `README.md`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principe | Pre-Design Check | Post-Design Check | Notes |
|---|---|---|---|
| **I. Modular Separation of Concerns** (`:core`, `:data`, `:app`) | ✅ PASS | ✅ PASS | Aucun impact sur `:core` ou `:data`. L'empaquetage exploite la sortie standard `:app:createDistributable`. |
| **II. Unidirectional Data Flow & State Consistency** | ✅ PASS | ✅ PASS | Aucun impact sur les flux d'état de l'application ou `NodeManager`. |
| **III. Test-Driven Verification (NON-NEGOTIABLE)** | ✅ PASS | ✅ PASS | L'AppImage encapsule les mêmes binaires testés préalablement par la suite unitaire KMP du pipeline CI/CD. |
| **IV. Freeplane Format Compatibility & Data Integrity** | ✅ PASS | ✅ PASS | Aucun impact sur la lecture ou l'écriture du format Freeplane `.mm`. |
| **V. Performance, Large Map Responsiveness & Simplicity** | ✅ PASS | ✅ PASS | Utilisation directe de `createDistributable` et du script minimal `appimagetool` sans plugin externe lourd (YAGNI). |
| **VI. SOLID Principles & Cyclomatic Complexity (≤20)** | ✅ PASS | ✅ PASS | Script shell modulaire à responsabilité unique (`scripts/package-appimage.sh`). |

## Project Structure

### Documentation (this feature)

```text
specs/019-linux-appimage-release/
├── plan.md              # Ce fichier (plan technique d'implémentation)
├── research.md          # Phase 0: décisions d'architecture (AppImage structure & FUSE-less CI)
├── data-model.md        # Phase 1: schéma de données de l'AppImage et de l'AppDir
├── quickstart.md        # Phase 1: scénarios de validation exécutables
├── contracts/           # Phase 1: contrats d'interface (script CLI et GitHub Actions)
│   └── appimage-packaging-contract.md
├── checklists/          # Contrôle qualité
│   └── requirements.md
└── tasks.md             # Phase 2 (généré par /speckit.tasks)
```

### Source Code (repository root)

```text
.github/
└── workflows/
    └── android_release.yml      # Mise à jour de l'étape Linux et de la publication GitHub Release

scripts/
├── bump-version.sh              # Script existant de version
└── package-appimage.sh          # Nouveau script de génération AppImage Linux

README.md                        # Documentation de téléchargement et d'exécution AppImage
```

**Structure Decision**: Création d'un script dédié `scripts/package-appimage.sh` qui encapsule la logique d'assemblage d'`AppDir` et l'appel à `appimagetool`. Mise à jour du runner Linux dans `.github/workflows/android_release.yml` et documentation dans `README.md`.

## Complexity Tracking

> Aucune violation des principes de la Constitution.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| *Aucune* | N/A | N/A |
