# Research: Support de l'état réduit (plié / déplié) des nœuds Freeplane (`016-node-folded-state`)

## 1. Analyse de l'existant dans Droidplane

### Parsing et Sérialisation XML (`:data`)
- **Constat** :
  - `Node` (`data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/Node.kt`) possède déjà la propriété `var isFolded: Boolean = false` prise en compte dans `equals` et `hashCode`.
  - `NodeUtilsDefaultImpl.parseNodeTag` (`data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeUtilsDefaultImpl.kt:176`) lit déjà `val isFolded = xpp.getNodeAttribute(FOLDED)?.equals("true", ignoreCase = true) ?: false`.
  - `NodeManager.serializeNode` (`data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt:509-511`) écrit déjà `if (node.isFolded) { serializer.nodeAttribute(FOLDED, "true") }`.
- **Lacunes identifiées dans `:data`** :
  1. Si un fichier `.mm` externe comporte `FOLDED="true"` sur le nœud racine (`parentNode == null`) ou sur un nœud feuille sans enfant (`childNodes.isEmpty()`), `isFolded` reste `true` à la lecture ou à l'écriture alors que Freeplane interdit le pliage de la racine et ignore le pliage sur les feuilles.
  2. Lors de l'ajout d'un enfant (`addNodeToMindmap`), de la suppression du dernier enfant (`deleteNode`), du retrait du dernier enfant lors d'un déplacement (`buildOldParentAfterRemoval`), ou de la restauration d'un sous-arbre (`restoreSubtree`), `updatedParent.isFolded` n'est pas systématiquement remis à `false`.
  3. Il manque une méthode dédiée `setNodeFolded(nodeId: String, isFolded: Boolean): Boolean` dans `NodeManagerContract` / `NodeManager` permettant de basculer proprement l'état `isFolded` d'un nœud dans l'arbre en mémoire (`rootNode`, `_allNodes`, `mindmapIndexes`).

### État d'interface et Synchronisation ViewModel (`:app`)
- **Constat** :
  - `MainUiState` expose `val collapsedNodeIds: Set<String> = emptySet()`.
  - `MindMapLayoutEngine` et `MindMapNodeCard` utilisent déjà `collapsedNodeIds` pour masquer les sous-arbres réduits et afficher l'indicateur `+` (réduit) ou `−` (déplié) sur tous les nœuds non-racines ayant des enfants.
- **Lacunes identifiées dans `:app`** :
  1. **Écrasement à l'ouverture (`MainViewModel.loadMindMap`)** : À la fin du chargement (`onLoadFinished` dans `MainViewModel.kt:145`), `collapsedNodeIds` est systématiquement réinitialisé à `emptySet()` au lieu d'être peuplé avec les identifiants des nœuds chargés ayant `isFolded == true` (et `parentNode != null && childNodes.isNotEmpty()`). C'est la cause directe du bug observé avec `feature_test.mm` où le nœud `test` (`FOLDED="true"`) s'affiche déplié au chargement.
  2. **Absence de persistance lors du pliage/dépliage manuel (`MainViewModel.toggleNodeCollapse`)** : `toggleNodeCollapse(node)` modifie uniquement `MainUiState.collapsedNodeIds` sans mettre à jour `node.isFolded` dans `NodeManager` ni activer `hasUnsavedChanges = true`. Par conséquent, toute action de pliage ou dépliage par l'utilisateur est perdue lors de la sauvegarde du fichier `.mm`.
  3. **Absence de dépliage lors de la recherche et des liens internes (`MainViewModel.downTo`)** : Lorsqu'une recherche (`showCurrentSearchResult`) ou un lien interne (`openInternalFragmentLink`) cible un nœud situé sous un ancêtre réduit (ex. rechercher `enfant 1` sous `test` réduit), `downTo` ne retire pas les ancêtres de `collapsedNodeIds` et ne met pas à jour `selectedNodeId`, laissant le nœud cible invisible sur le canevas 2D.

---

## 2. Décisions Techniques

### Decision 1 : Initialisation de `collapsedNodeIds` depuis l'arbre chargé dans `MainViewModel.loadMindMap`
- **Decision** : À la fin du chargement XML (`onLoadFinished`), extraire l'ensemble des identifiants des nœuds non-racines possédant au moins un enfant et ayant `isFolded == true` (par parcours de `nodeManager.rootNode` ou filtrage de `nodeManager.allNodes.value`), et affecter cet ensemble à `MainUiState.collapsedNodeIds`.
- **Rationale** : Garantit que dès le premier rendu du canevas 2D après l'ouverture d'un fichier `.mm`, tous les nœuds marqués `FOLDED="true"` (y compris les sous-nœuds imbriqués à plusieurs niveaux) sont dans `collapsedNodeIds` et s'affichent repliés comme dans Freeplane.
- **Alternatives considered** :
  - *Lire directement `node.isFolded` dans `MindMapLayoutEngine` et supprimer `collapsedNodeIds` de `MainUiState`* : Rejeté car `collapsedNodeIds: Set<String>` dans `MainUiState` est une clé immuable utilisée par `remember(rootNode, treeVersion, collapsedNodeIds, density.density)` dans `MindMapCanvasScreen` pour déclencher efficacement le recalcul de mise en page Compose sans cloner tout l'arbre à chaque clic. Conserver `collapsedNodeIds` synchronisé avec `node.isFolded` combine réactivité UI immédiate et persistance dans `:data`.

### Decision 2 : Synchronisation bidirectionnelle via `NodeManager.setNodeFolded`
- **Decision** : Ajouter `suspend fun setNodeFolded(nodeId: String, isFolded: Boolean): Boolean` dans `NodeManagerContract` et `NodeManager`. Lors d'un appel à `MainViewModel.toggleNodeCollapse(node)`:
  1. Ignorer l'appel si `node.parentNode == null` ou `node.childNodes.isEmpty()`.
  2. Mettre à jour `collapsedNodeIds` dans `MainUiState` et activer `hasUnsavedChanges = true`.
  3. Appeler `nodeManager.setNodeFolded(node.id, newFoldedState)` pour mettre à jour `isFolded` sur le nœud dans `rootNode`, `_allNodes` et `mindmapIndexes`.
- **Rationale** : Respecte le Principe II de la Constitution (*Unidirectional Data Flow & State Consistency*) et garantit que `serializeMindmap` écrit toujours l'état exact visible à l'écran.
- **Alternatives considered** :
  - *Passer `collapsedNodeIds` en paramètre de `serializeMindmap`* : Rejeté car `:data` doit rester la source de vérité autonome du modèle Freeplane (Principe I & II de la Constitution).

### Decision 3 : Dépliage automatique des ancêtres dans `downTo` (Recherche & Liens internes) et cohérence lors des mutations d'arbre
- **Decision** :
  - Dans `MainViewModel.downTo(node, openLast)`, collecter tous les ancêtres du nœud cible (ainsi que le nœud lui-même si `openLast == true`), retirer leurs identifiants de `collapsedNodeIds`, mettre à jour leur `isFolded = false` via `nodeManager.setNodeFolded`, et sélectionner le nœud cible (`selectedNodeId = node.id`).
  - Dans `NodeManager` (`addNodeToMindmap`, `deleteNode`, `restoreSubtree`, `moveNode`), maintenir `isFolded = false` sur un parent qu'on déplie ou qui perd son dernier enfant, et refléter ce changement dans `MainUiState.collapsedNodeIds`.
- **Rationale** : Assure qu'un nœud trouvé par la recherche ou ciblé par un lien `#ID_` devient immédiatement visible et sélectionné sur le canevas 2D, et qu'aucun nœud sans enfant ne conserve un état réduit fantôme.
- **Alternatives considered** :
  - *Ne pas déplier les ancêtres lors de la recherche* : Rejeté car sur le canevas 2D (`MindMapCanvasScreen`), un nœud dont le parent est dans `collapsedNodeIds` n'est pas rendu à l'écran, rendant la navigation par recherche inopérante pour les branches réduites.
