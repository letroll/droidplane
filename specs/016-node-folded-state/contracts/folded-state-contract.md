# Contract: Gestion de l'état réduit (`FOLDED`) des nœuds (`016-node-folded-state`)

## 1. `NodeManagerContract` (`:data`)

### Nouvelle opération : `setNodeFolded`
```kotlin
/**
 * Updates the folded state of the non-root node identified by [nodeId].
 *
 * - Returns `false` if [nodeId] does not exist, is the root node (`parentNode == null`),
 *   or has no child nodes (`childNodes.isEmpty()`) when [isFolded] is `true`.
 * - Propagates the updated `isFolded` property to `rootNode`, `allNodes`, and internal indexes.
 * - Returns `true` when the node state was updated (or confirmed).
 */
suspend fun setNodeFolded(nodeId: String, isFolded: Boolean): Boolean
```

### Garanties sur les opérations existantes de `NodeManager`
- **`parseNodeTag` (`NodeUtilsDefaultImpl`)** :
  - `isFolded = if (parentNode == null) false else xpp.getNodeAttribute(FOLDED)?.equals("true", ignoreCase = true) ?: false`.
- **`loadMindMapFromXml` (`NodeManager`)** :
  - Après la construction de l'arbre complet (`processMindMap`), tout nœud qui n'a aucun enfant (`childNodes.isEmpty()`) ou qui est la racine (`parentNode == null`) a `isFolded = false`.
- **`serializeNode` (`NodeManager`)** :
  - Écrit l'attribut `FOLDED="true"` uniquement si `node.isFolded && node.parentNode != null && children.isNotEmpty()`.
  - N'écrit jamais `FOLDED="true"` sur un nœud déplié (`isFolded == false`), sur le nœud racine, ni sur un nœud sans enfant.
- **`addNodeToMindmap` / `restoreSubtree` / `moveNode` (`NodeManager`)** :
  - Tout nœud parent qui reçoit un nouvel enfant ou un enfant restauré/déplacé voit son `isFolded` forcé à `false`.
- **`deleteNode` / `moveNode` (`NodeManager`)** :
  - Tout nœud parent qui perd son dernier enfant (`childNodes.isEmpty()`) voit son `isFolded` forcé à `false`.

---

## 2. `MainViewModel` (`:app`)

- **`loadMindMap(inputStream, onLoadFinished)`** :
  - Lors de `onLoadFinished`, initialise `MainUiState.collapsedNodeIds` avec l'ensemble des `node.id` de l'arbre chargé vérifiant `node.parentNode != null && node.isFolded && node.childNodes.isNotEmpty()`.
- **`toggleNodeCollapse(node: Node)`** :
  - Ignore l'appel si `node.parentNode == null` ou `node.childNodes.isEmpty()`.
  - Bascule `node.id` dans `MainUiState.collapsedNodeIds`.
  - Appelle `nodeManager.setNodeFolded(node.id, isNowCollapsed)` et active `hasUnsavedChanges = true`.
- **`downTo(node: Node?, openLast: Boolean)` (Recherche & Liens internes)** :
  - Pour tout ancêtre de `node` (ainsi que `node` lui-même si `openLast == true`), retire son `id` de `MainUiState.collapsedNodeIds` et appelle `nodeManager.setNodeFolded(ancestorId, false)` s'il était réduit.
  - Met à jour `selectedNodeId = node.id` afin que le nœud cible soit visible et sélectionné sur le canevas 2D.
