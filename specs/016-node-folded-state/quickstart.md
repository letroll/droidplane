# Quickstart & Validation Guide: Support de l'état réduit (`FOLDED`) des nœuds (`016-node-folded-state`)

## Prérequis
- JDK 17+ configuré pour Gradle
- Dépôt Droidplane sur la branche `016-node-folded-state`

## 1. Exécution des tests automatisés

### Tests unitaires du module `:data` (Parsing, Sérialisation et `NodeManager`)
```bash
./gradlew :data:testDebugUnitTest :data:desktopTest
```
**Résultat attendu** :
- Lecture d'une carte `.mm` contenant `<node TEXT="test" FOLDED="true">...</node>` (comme `feature_test.mm`) → `node.isFolded == true` uniquement sur les nœuds non-racines ayant des enfants.
- `setNodeFolded(nodeId, true/false)` met à jour `rootNode`, `allNodes` et les index, et la sérialisation XML (`serializeMindmap`) écrit ou omet `FOLDED="true"` en conséquence.
- L'ajout, le déplacement, la restauration ou la suppression du dernier enfant maintient `isFolded` cohérent.

### Tests unitaires du module `:app` (`MainViewModelTest` & Layout)
```bash
./gradlew :app:testDebugUnitTest :app:desktopTest
```
**Résultat attendu** :
- `loadMindMap` avec la structure de `feature_test.mm` initialise `uiState.value.collapsedNodeIds` avec l'ID du nœud `test` (`ID_1478696366`), et `MindMapLayoutEngine.computeLayout` ne génère pas les nœuds enfants `enfant 1` et `enfant 2` tant que `test` est réduit.
- `toggleNodeCollapse(testNode)` déplie `test` dans `collapsedNodeIds`, met `testNode.isFolded` à `false` dans `NodeManager`, et active `hasUnsavedChangesState == true`.
- La recherche de `enfant 1` lorsque `test` est réduit déplie automatiquement `test` (`collapsedNodeIds` ne contient plus `ID_1478696366`) et sélectionne `enfant 1`.

## 2. Vérification manuelle (Android / Desktop)
1. Ouvrir le fichier `feature_test.mm` dans Droidplane.
2. Vérifier que le nœud `test` affiche le badge `+` et que ses enfants `enfant 1` et `enfant 2` sont masqués.
3. Cliquer sur `+` sur le nœud `test` : vérifier que `enfant 1` et `enfant 2` apparaissent et que le badge passe à `−`.
4. Sauvegarder la carte mentale et la rouvrir (dans Droidplane ou Freeplane Desktop) : vérifier que l'état plié/déplié est fidèlement conservé.
