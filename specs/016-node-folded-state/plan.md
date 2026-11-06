# Implementation Plan: Support de l'état réduit (plié / déplié) des nœuds Freeplane

**Branch**: `016-node-folded-state` | **Date**: 2026-11-06 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/016-node-folded-state/spec.md`

## Summary

Assurer le support complet et bidirectionnel de l'état réduit (`FOLDED="true"` / `FOLDED="false"`) des nœuds de cartes mentales Freeplane :
1. **Au chargement (`loadMindMap`)** : Initialiser `MainUiState.collapsedNodeIds` à partir de tous les nœuds non-racines ayant `isFolded == true` et au moins un enfant (au lieu de le réinitialiser à `emptySet()`), afin que les branches repliées dans Freeplane (comme le nœud `test` masquant `enfant 1` et `enfant 2` dans `feature_test.mm`) s'affichent immédiatement à l'état réduit.
2. **Au pliage/dépliage interactif (`toggleNodeCollapse`) et à l'enregistrement (`serializeMindmap`)** : Synchroniser `MainUiState.collapsedNodeIds` avec `Node.isFolded` dans `NodeManager` via `setNodeFolded(nodeId, isFolded)` et activer `hasUnsavedChanges = true`, afin que tout pliage ou dépliage manuel soit sauvegardé dans le fichier `.mm`.
3. **Lors de l'édition et de la navigation (`addNode`, `deleteNode`, `restoreSubtree`, `moveNode`, `downTo`)** : Déplier automatiquement les ancêtres réduits lors d'une recherche ou d'un suivi de lien interne, déplier un parent réduit lorsqu'on lui ajoute/déplace/restaure un enfant, et nettoyer `isFolded` lorsqu'un nœud perd son dernier enfant.

## Technical Context

**Language/Version**: Kotlin Multiplatform (JVM 17/21, Android & Desktop JVM)

**Primary Dependencies**: Compose Multiplatform (Material 3), AndroidX Lifecycle ViewModel, Koin 4.0+, Kotest, MockK, Turbine, XmlPullParser (`kxml2` / `xpp3`)

**Storage**: Fichiers Freeplane XML (`.mm`) sur système de fichiers local (Desktop) ou Android SAF (`content://`)

**Testing**: `kotlin.test` + Kotest (`KStringSpec`), MockK, Turbine (`:data:testDebugUnitTest`, `:data:desktopTest`, `:app:testDebugUnitTest`, `:app:desktopTest`)

**Target Platform**: Android (Min SDK 26, Target SDK 35) & Desktop JVM (macOS, Windows, Linux)

**Project Type**: Application multiplateforme (Android & Desktop) structurée en 3 modules Gradle (`:core`, `:data`, `:app`)

**Performance Goals**: Rendu du canevas 2D à 60/120 fps ; initialisation de `collapsedNodeIds` en $O(N)$ lors du chargement sans parcours redondant sur le thread UI

**Constraints**: Compatibilité stricte avec le format XML Freeplane (`.mm`) ; préservation de tous les attributs et hooks existants ; complexité cyclomatique par méthode $\le 20$

**Scale/Scope**: Cartes mentales de quelques dizaines à plusieurs milliers de nœuds sur `:data` et `:app`

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

- **I. Modular Separation of Concerns**: **PASS** — La logique de modèle et de persistance XML (`isFolded`, `setNodeFolded`, nettoyage sur `addNodeToMindmap`/`deleteNode`/`restoreSubtree`/`moveNode`, sérialisation `FOLDED="true"`) reste exclusivement dans `:data`. La synchronisation de l'état d'affichage (`collapsedNodeIds`, `toggleNodeCollapse`, `downTo`) reste dans `:app` (`MainViewModel`).
- **II. Unidirectional Data Flow & State Consistency**: **PASS** — `NodeManager` demeure la source unique de vérité pour `Node.isFolded`, et `MainViewModel` expose `collapsedNodeIds` via `StateFlow<MainUiState>` tout en répercutant chaque transition vers `NodeManager`.
- **III. Test-Driven Verification (NON-NEGOTIABLE)**: **PASS** — Tests unitaires JVM prévus dans `:data` (`NodeManagerTest`, `MindmapParserTest`) et `:app` (`MainViewModelTest`), incluant un test de bout en bout basé sur la carte `feature_test.mm`.
- **IV. Freeplane Format Compatibility & Data Integrity**: **PASS** — Préservation exacte de `FOLDED="true"` sur les nœuds réduits non-racines avec enfants et omission sur les nœuds dépliés, la racine et les feuilles.
- **V. Performance, Large Map Responsiveness & Simplicity**: **PASS** — Calcul en un seul passage $O(N)$ de `collapsedNodeIds` et mise à jour ciblée des ancêtres sans dépendance externe.
- **VI. SOLID Principles & Cyclomatic Complexity**: **PASS** — Découpage en méthodes courtes à responsabilité unique (`setNodeFolded`, `collectInitialCollapsedNodeIds`, `unfoldAncestorsOf`), chacune avec une complexité cyclomatique bien inférieure à 20.

## Project Structure

### Documentation (this feature)

```text
specs/016-node-folded-state/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/           # Phase 1 output (/speckit-plan command)
│   └── folded-state-contract.md
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
data/
├── src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/
│   ├── model/Node.kt
│   ├── NodeUtilsDefaultImpl.kt
│   ├── NodeManagerContract.kt
│   └── NodeManager.kt
└── src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/
    ├── MindmapParserTest.kt
    └── NodeManagerTest.kt

app/
├── src/commonMain/kotlin/fr/julien/quievreux/droidplane2/
│   ├── MainUiState.kt
│   ├── MainViewModel.kt
│   └── ui/mindmap/
│       ├── MindMapCanvasScreen.kt
│       ├── MindMapLayoutEngine.kt
│       └── MindMapNodeCard.kt
└── src/commonTest/kotlin/fr/julien/quievreux/droidplane2/
    └── MainViewModelTest.kt
```

**Structure Decision**: Architecture multi-modules KMP existante (`:core`, `:data`, `:app`). Les modifications portent uniquement sur `:data` (`NodeUtilsDefaultImpl`, `NodeManagerContract`, `NodeManager`) et `:app` (`MainViewModel`), accompagnées de leurs suites de tests respectives dans `commonTest`.

## Complexity Tracking

> Aucune violation de la Constitution — table non applicable.
