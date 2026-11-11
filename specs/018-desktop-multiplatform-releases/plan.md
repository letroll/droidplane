# Implementation Plan: Génération simultanée des distributions desktop (Windows, Linux, macOS) lors de la publication d'une release

**Branch**: `018-desktop-multiplatform-releases` | **Date**: 2026-11-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `specs/018-desktop-multiplatform-releases/spec.md`

## Summary

Étendre le pipeline de publication automatique (CI/CD GitHub Actions) et la configuration de build Compose Multiplatform pour générer et publier simultanément les distributions desktop natives (Windows `.msi`, Linux `.deb`, macOS `.dmg`) aux côtés de l'APK Android lors de chaque release. L'approche technique s'articule autour de deux volets : (1) la déclaration du bloc `nativeDistributions` dans `app/build.gradle.kts` tirant parti de `jpackage` via le plugin Compose Multiplatform, et (2) la restructuration du workflow GitHub Actions en une orchestration modulaire (préparation de release et auto-incrémentation, build Android, matrice multi-OS pour le packaging desktop sur `ubuntu-latest`, `windows-latest` et `macos-latest`, et job de publication atomique sur GitHub Releases).

## Technical Context

**Language/Version**: Kotlin 2.2.10 (toolchain Java 21, target JVM 21), Android Gradle Plugin 9.4.1, JetBrains Compose Multiplatform 1.7.3

**Primary Dependencies**: Compose Multiplatform Desktop DSL (`org.jetbrains.compose`), Gradle Wrapper 9.6, GitHub Actions CI/CD

**Storage**: N/A (processus de compilation et distribution binaire)

**Testing**: JUnit 5 Platform, Kotest 5.9.0, MockK 1.13.13, Turbine 1.2.0 (exécution des suites `:core:desktopTest`, `:data:desktopTest`, `:app:desktopTest` et `testDebugUnitTest` avant publication)

**Target Platform**: Android Min SDK 26 (Android 8.0) — Target SDK 35, Desktop JVM (Linux x64, Windows x64, macOS x64/arm64)

**Project Type**: Application Kotlin Multiplatform / Compose Multiplatform & pipeline CI/CD automatisé

**Performance Goals**: Publication de l'ensemble des 4 livrables (Android + 3 Desktop) sous la même release en une seule exécution sans intervention manuelle ; durée de build matriciel optimisée grâce à la mise en cache Gradle

**Constraints**: `jpackage` nécessite l'exécution sur l'OS cible respectif (runners GitHub Actions `windows-latest`, `macos-latest`, `ubuntu-latest`) ; atomicité de la release (tous les actifs attachés à la même version avec le même changelog)

**Scale/Scope**: Configuration de build dans `:app` (`app/build.gradle.kts`), script de bump existant (`scripts/bump-version.sh`), et workflow CI/CD dans `.github/workflows/android_release.yml`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principe | Pre-Design Check | Post-Design Check | Notes |
|---|---|---|---|
| **I. Modular Separation of Concerns** (`:core`, `:data`, `:app`) | ✅ PASS | ✅ PASS | Le packaging desktop et la cible applicative se situent exclusivement dans `:app` (`app/build.gradle.kts`) et dans le workflow `.github/workflows/`. Les modules `:core` et `:data` restent strictement agnostiques. |
| **II. Unidirectional Data Flow & State Consistency** | ✅ PASS | ✅ PASS | Aucun impact sur les flux d'état ou l'arbre de nœuds (`NodeManager` / `MainViewModel`). |
| **III. Test-Driven Verification (NON-NEGOTIABLE)** | ✅ PASS | ✅ PASS | Les tests automatisés multiplateformes (`testDebugUnitTest`, `:core:desktopTest`, `:data:desktopTest`, `:app:desktopTest`) sont exécutés comme prérequis obligatoire avant packaging et release. |
| **IV. Freeplane Format Compatibility & Data Integrity** | ✅ PASS | ✅ PASS | Aucun changement dans la sérialisation ou la lecture des fichiers `.mm`. |
| **V. Performance, Large Map Responsiveness & Simplicity** | ✅ PASS | ✅ PASS | Utilisation exclusive des mécanismes standards de Compose Multiplatform (`nativeDistributions` basé sur `jpackage`) et des runners officiels GitHub Actions sans surcouche ni dépendance superflue (YAGNI). |
| **VI. SOLID Principles & Cyclomatic Complexity (≤20)** | ✅ PASS | ✅ PASS | Découpage du pipeline CI/CD en jobs spécialisés à responsabilité unique (`prepare-release`, `build-android`, `build-desktop`, `publish-release`). |

## Project Structure

### Documentation (this feature)

```text
specs/018-desktop-multiplatform-releases/
├── plan.md              # Ce fichier (output de la phase de planification)
├── research.md          # Phase 0: décisions d'architecture (Compose DSL & GitHub Actions Matrix)
├── data-model.md        # Phase 1: modèle de données d'une Release et schéma d'actifs
├── quickstart.md        # Phase 1: guide de validation exécutable
├── contracts/           # Phase 1: contrats d'interface (Gradle tasks & Workflow schema)
│   └── release-pipeline-contract.md
├── checklists/          # Validation qualité
│   └── requirements.md
└── tasks.md             # Phase 2 (généré ultérieurement par /speckit.tasks)
```

### Source Code (repository root)

```text
.github/
└── workflows/
    └── android_release.yml      # Pipeline CI/CD restructuré avec matrice multi-OS

app/
└── build.gradle.kts             # Configuration du bloc nativeDistributions (Deb, Msi, Dmg)

scripts/
└── bump-version.sh              # Script d'incrémentation de version (partagé Android + Desktop)
```

**Structure Decision**: La configuration de distribution desktop est nativement intégrée au module applicatif `:app` dans son fichier existant `build.gradle.kts` via le bloc `compose.desktop.application.nativeDistributions`. L'automatisation s'appuie sur le workflow existant dans `.github/workflows/android_release.yml` étendu avec une matrice d'environnements `ubuntu-latest`, `windows-latest` et `macos-latest`.

## Complexity Tracking

> Aucune violation des principes de la Constitution. Tous les critères passent sans dérogation.

| Violation | Why Needed | Simpler Alternative Rejected Because |
|---|---|---|
| *Aucune* | N/A | N/A |
