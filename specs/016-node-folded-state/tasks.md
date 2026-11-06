---

description: "Task list for feature 016-node-folded-state implementation"
---

# Tasks: Support de l'état réduit (plié / déplié) des nœuds Freeplane

**Input**: Design documents from `/specs/016-node-folded-state/`

**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/folded-state-contract.md

**Tests**: Inclus conformément au Principe III de la Constitution (*Test-Driven Verification (NON-NEGOTIABLE)*).

**Organization**: Les tâches sont regroupées par User Story (`US1`, `US2`, `US3`) afin de permettre une implémentation et une vérification indépendantes de chaque incrément.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Exécutable en parallèle (fichiers distincts, aucune dépendance sur une tâche incomplète)
- **[Story]**: User Story associée (`[US1]`, `[US2]`, `[US3]`)
- Chaque tâche indique le chemin exact du fichier concerné

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Préparation des fixtures XML de test basées sur la carte d'exemple Freeplane fournie (`feature_test.mm`).

- [X] T001 Copier/intégrer la fixture XML d'exemple (avec le nœud `test` ayant `FOLDED="true"` et ses enfants `enfant 1` et `enfant 2`) dans `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt` et `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Contrat et invariants de base du modèle de données `:data` requis par toutes les User Stories.

**⚠️ CRITICAL**: Aucune User Story ne peut commencer avant la fin de cette phase.

- [X] T002 Déclarer la signature `suspend fun setNodeFolded(nodeId: String, isFolded: Boolean): Boolean` dans `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt`
- [X] T003 Garantir les invariants de parsing de `Node.isFolded` ("Toujours `false` pour le nœud racine (`parentNode == null`)" et "Effectif uniquement si `childNodes.isNotEmpty()`") dans `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeUtilsDefaultImpl.kt` et `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt`

**Checkpoint**: Fondation `:data` prête — l'implémentation des User Stories peut démarrer.

---

## Phase 3: User Story 1 - Respect de l'état réduit à l'ouverture d'une carte mentale (Priority: P1) 🎯 MVP

**Goal**: Afficher immédiatement à l'état réduit (`+`, sous-nœuds masqués) tous les nœuds non-racines ayant `FOLDED="true"` et au moins un enfant lors de l'ouverture d'un fichier `.mm` (comme le nœud `test` masquant `enfant 1` et `enfant 2` dans `feature_test.mm`).

**Independent Test**: Charger `feature_test.mm` via `MainViewModel.loadMindMap` et vérifier que `uiState.value.collapsedNodeIds` contient `ID_1478696366` (`test`), ne contient ni la racine (`ID_1090958577`) ni les feuilles (`ID_1756066841`), et que `MindMapLayoutEngine.computeLayout` masque `enfant 1` et `enfant 2` jusqu'au dépliage de `test`.

### Tests for User Story 1 ⚠️

- [X] T004 [P] [US1] Ajouter les tests unitaires de parsing de `FOLDED="true"` / `FOLDED="false"` (nœud standard, nœud racine ignoré, nœud feuille sans enfant ignoré, sous-nœuds réduits imbriqués) dans `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/NodeUtilsTest.kt`
- [X] T005 [P] [US1] Ajouter le test unitaire vérifiant que `MainViewModel.loadMindMap` avec le XML de `feature_test.mm` initialise `collapsedNodeIds` avec `ID_1478696366` (`test`) et masque `enfant 1` et `enfant 2` dans `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

### Implementation for User Story 1

- [X] T006 [US1] Nettoyer `isFolded = false` sur les nœuds sans enfant ou racine lors de la finalisation de l'arbre dans `processMindMap()` dans `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt`
- [X] T007 [US1] Initialiser `MainUiState.collapsedNodeIds` ("Peuplé à la fin de `loadMindMap` avec tous les `node.id` de l'arbre où `node.parentNode != null && node.isFolded && node.childNodes.isNotEmpty()`") dans `MainViewModel.loadMindMap` dans `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt`

**Checkpoint**: User Story 1 est fonctionnelle et testable de manière autonome (MVP).

---

## Phase 4: User Story 2 - Persistance de l'état réduit ou déplié lors de l'enregistrement (Priority: P1)

**Goal**: Synchroniser chaque action de pliage/dépliage manuel (`+` / `−`) avec `Node.isFolded` dans `NodeManager`, marquer la carte comme ayant des modifications non enregistrées, et sérialiser fidèlement `FOLDED="true"` lors de la sauvegarde du fichier `.mm`.

**Independent Test**: Plier un nœud déplié et déplier le nœud `test` (`ID_1478696366`) via `MainViewModel.toggleNodeCollapse`, vérifier que `hasUnsavedChangesState` passe à `true`, sérialiser la carte via `NodeManager.serializeMindmap` et vérifier que le XML résultant reflète exactement le nouvel état `FOLDED="true"`.

### Tests for User Story 2 ⚠️

- [X] T008 [P] [US2] Ajouter les tests unitaires de `setNodeFolded` et de sérialisation aller-retour (`FOLDED="true"` ajouté au pliage, retiré au dépliage, omis sur racine et feuilles) dans `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt`
- [X] T009 [P] [US2] Ajouter les tests unitaires vérifiant que `MainViewModel.toggleNodeCollapse` synchronise `collapsedNodeIds`, met à jour `node.isFolded` dans `NodeManager` et active `hasUnsavedChangesState` dans `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

### Implementation for User Story 2

- [X] T010 [US2] Implémenter `setNodeFolded(nodeId: String, isFolded: Boolean): Boolean` et sécuriser `serializeNode` ("Sérialisé en XML sous la forme `FOLDED=\"true\"` uniquement si `isFolded == true && parentNode != null && childNodes.isNotEmpty()`") dans `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt`
- [X] T011 [US2] Mettre à jour `MainViewModel.toggleNodeCollapse(node: Node)` pour ignorer la racine/les feuilles, basculer `collapsedNodeIds`, appeler `nodeManager.setNodeFolded` et activer `hasUnsavedChanges = true` dans `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt`

**Checkpoint**: User Stories 1 et 2 fonctionnent de bout en bout (lecture, bascule interactive et sauvegarde `.mm`).

---

## Phase 5: User Story 3 - Dépliage automatique cohérent lors de l'édition, de la recherche et de la navigation (Priority: P2)

**Goal**: Déplier automatiquement (dans `collapsedNodeIds` et `Node.isFolded`) les branches parentes lorsqu'un nœud masqué est ciblé par une recherche textuelle, un lien interne (`#ID_`), un ajout d'enfant, un glisser-déposer ou une restauration (Undo), et désactiver `isFolded` lorsqu'un nœud perd son dernier enfant.

**Independent Test**: Charger `feature_test.mm` (avec `test` réduit), rechercher `"enfant 1"` (ou suivre un lien interne `#ID_1128035855`), et vérifier que `test` (`ID_1478696366`) est automatiquement retiré de `collapsedNodeIds`, que `test.isFolded` passe à `false`, et que `enfant 1` devient visible et sélectionné.

### Tests for User Story 3 ⚠️

- [X] T012 [P] [US3] Ajouter les tests unitaires vérifiant que `addNodeToMindmap`, `restoreSubtree`, `deleteNode` (dernier enfant) et `moveNode` (dernier enfant retiré de l'ancien parent) mettent `isFolded = false` dans `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt`
- [X] T013 [P] [US3] Ajouter les tests unitaires vérifiant que la recherche textuelle (`search`) et le suivi d'un lien interne (`openInternalFragmentLink`) vers un nœud situé sous un parent réduit déplient automatiquement tous ses ancêtres dans `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt`

### Implementation for User Story 3

- [X] T014 [US3] Mettre à jour `addNodeToMindmap`, `deleteNode`, `restoreSubtree` et `buildOldParentAfterRemoval` pour forcer `isFolded = false` sur le parent déplié ou vidé de ses enfants dans `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt`
- [X] T015 [US3] Mettre à jour `downTo(node: Node?, openLast: Boolean)` et `onMoveNode` dans `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt` pour déplier automatiquement les ancêtres réduits (dans `collapsedNodeIds` et via `nodeManager.setNodeFolded`), nettoyer l'ancien parent vidé lors d'un déplacement, et sélectionner le nœud cible

**Checkpoint**: Les 3 User Stories sont complètes et testées indépendamment.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validation globale multiplateforme (Android & Desktop) selon `quickstart.md`.

- [X] T016 Exécuter la suite complète de tests unitaires `:data` et `:app` (`./gradlew :data:testDebugUnitTest :data:desktopTest :app:testDebugUnitTest :app:desktopTest`) et valider les scénarios de `specs/016-node-folded-state/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Aucune dépendance — démarrage immédiat (`T001`).
- **Foundational (Phase 2)**: Dépend de Setup (`T002`, `T003`) — bloque toutes les User Stories.
- **User Story 1 (Phase 3 - P1 MVP)**: Dépend de la Phase 2 (`T004`–`T007`).
- **User Story 2 (Phase 4 - P1)**: Dépend de la Phase 2 (`T008`–`T011`).
- **User Story 3 (Phase 5 - P2)**: Dépend des Phases 3 et 4 (`T012`–`T015`).
- **Polish (Phase 6)**: Dépend de la complétion des Phases 3, 4 et 5 (`T016`).

### Parallel Opportunities

- `T004` (`NodeUtilsTest.kt`) et `T005` (`MainViewModelTest.kt`) peuvent être écrits en parallèle.
- `T008` (`FreeplaneXmlRoundTripTest.kt`) et `T009` (`MainViewModelTest.kt`) peuvent être écrits en parallèle.
- `T012` (`NodeManagerTest.kt`) et `T013` (`MainViewModelTest.kt`) peuvent être écrits en parallèle.

---

## Parallel Example: User Story 1

```bash
# Écriture parallèle des tests de la User Story 1 sur :data et :app :
Task T004: "Ajouter les tests unitaires de parsing de FOLDED dans data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/NodeUtilsTest.kt"
Task T005: "Ajouter le test unitaire de MainViewModel.loadMindMap avec feature_test.mm dans app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Compléter Phase 1 (Setup) et Phase 2 (Foundational).
2. Compléter Phase 3 (User Story 1 : chargement initial de l'état réduit `FOLDED="true"`).
3. **STOP & VALIDATE** : Vérifier que `feature_test.mm` s'ouvre avec le nœud `test` réduit (`+`) et ses enfants `enfant 1` et `enfant 2` masqués.

### Incremental Delivery

1. Setup + Foundational → Socle `:data` prêt.
2. User Story 1 (P1) → Respect de `FOLDED="true"` au chargement.
3. User Story 2 (P1) → Persistance du pliage/dépliage manuel à l'enregistrement.
4. User Story 3 (P2) → Dépliage automatique lors de la recherche, des liens internes et de l'édition.
5. Polish → Validation multiplateforme complète Android & Desktop.
