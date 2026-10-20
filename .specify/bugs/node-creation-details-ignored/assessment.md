# Bug Assessment: Détails ignorés lors de la création d'un nœud

- **Slug**: node-creation-details-ignored
- **Created**: 2026-10-20
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: medium

## Report (verbatim ou résumé)

> Lors de la création d'un node, l'ajout de texte dans la partie détails n'est pas prise en compte.
> Elle l'est par contre après sur une édition.

## Symptom

Lorsqu'un utilisateur crée un nouveau nœud via l'inspector (menu contextuel → "Ajouter un enfant"), les champs saisis dans l'onglet **Contenu** (`detailsText`, `noteText`, `richText`, `latexEquation`) sont ignorés à la sauvegarde. Ces mêmes champs sont correctement pris en compte lors d'une **édition ultérieure** du nœud via le même inspector.

## Reproduction

1. Ouvrir une mindmap.
2. Effectuer un appui long sur un nœud → menu contextuel → "Ajouter un enfant".
3. Dans l'inspector qui s'ouvre (`isNewNode = true`), remplir :
   - Le titre (champ Texte)
   - Le champ **Détails**
   - Optionnellement : Notes, formule LaTeX
4. Appuyer sur "Ajouter".
5. **Observation** : le nœud est créé avec seulement le titre. Les détails, notes et autres champs sont perdus.
6. Rouvrir l'inspector via les propriétés du nœud fraîchement créé → saisir les mêmes détails → "Sauvegarder".
7. **Observation** : les détails sont cette fois correctement enregistrés.

## Suspected Code Paths

- [`MainViewModel.kt:909-913`](file:///home/letroll/AndroidStudioProjects/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt#L909-L913) — `onAddNodeFromInspector` extrait uniquement `newNode.text` avant d'appeler `addNode(text, parentNode)`, laissant tomber tous les autres champs du `newNode`.
- [`MainViewModel.kt:1404-1441`](file:///home/letroll/AndroidStudioProjects/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt#L1404-L1441) — `addNode(newValue: String, ...)` n'accepte qu'une `String` de texte ; il n'existe aucun chemin pour passer `detailsText`, `noteText`, `richText`, `latexEquation`.
- [`NodeManager.kt:736-796`](file:///home/letroll/AndroidStudioProjects/droidplane/data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt#L736-L796) — `addNodeToMindmap(newValue: String, parentNode: Node?)` crée le `Node` avec uniquement `text = newValue`, tous les autres champs restent à `null`/vide.
- [`NodeManagerContract.kt:64`](file:///home/letroll/AndroidStudioProjects/droidplane/data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt#L64) — Signature du contrat : `suspend fun addNodeToMindmap(newValue: String, parentNode: Node?): Int?` — ne permet pas de passer un nœud complet.
- [`MainViewModel.kt:972-990`](file:///home/letroll/AndroidStudioProjects/droidplane/app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt#L972-L990) — Déclencheur : `AddChildNode` crée un `Node` temporaire vide et ouvre l'inspector avec `isNewNode = true`.

## Root Cause Hypothesis

**Confiance : haute.**

La chaîne d'appel lors de la création est asymétrique par rapport à l'édition :

- **Édition** : `onSaveNodeProperties(updatedNode: Node)` reçoit le `Node` complet avec tous ses champs et appelle `nodeManager.updateNode(updatedNode)`.
- **Création** : `onAddNodeFromInspector(newNode: Node, parentNode: Node?)` ne transmet que `newNode.text` à `addNode(text, parentNode)`, qui appelle `nodeManager.addNodeToMindmap(newValue: String, ...)` lequel reconstruit un `Node` vierge avec uniquement le texte.

Le `Node` entièrement renseigné par l'utilisateur dans l'inspector (`currentNode` local au composable, correctement propagé via `onNodeChanged`) est ignoré dès la sortie du composable.

## Proposed Remediation

### Préféré : Faire passer le `Node` complet lors de la création

**Étape 1 — Enrichir la signature de `addNodeToMindmap`** dans `NodeManagerContract` et `NodeManager` pour accepter un `Node` complet (ou optionnellement les champs supplémentaires) :

```kotlin
// NodeManagerContract.kt
suspend fun addNodeToMindmap(node: Node, parentNode: Node? = null): Int?
```

**Étape 2 — Adapter `NodeManager.addNodeToMindmap`** pour copier `detailsText`, `noteText`, `richText`, `latexEquation`, `attributes`, `iconNames`, etc. depuis le `Node` reçu au lieu d'en créer un vierge.

**Étape 3 — Adapter `MainViewModel.addNode`** pour accepter un `Node` complet (ou garder la surcharge `String` pour la création rapide depuis la barre de titre) :

```kotlin
fun addNode(newNode: Node, parentNode: Node? = _uiState.value.nodeCurrentlyDisplayed) { … }
```

**Étape 4 — Corriger `onAddNodeFromInspector`** pour passer le `Node` complet :

```kotlin
fun onAddNodeFromInspector(newNode: Node, parentNode: Node?) {
    if (newNode.text.orEmpty().isBlank()) return
    addNode(newNode, parentNode)   // ← passer le Node entier
    setDialogState(DialogType.None)
}
```

### Alternative : Post-création `updateNode`

Après `addNodeToMindmap(text, parentNode)`, récupérer le nœud créé et appeler `nodeManager.updateNode(createdNode.copy(detailsText = newNode.detailsText, ...))`. C'est plus simple à court terme mais induit deux writes et une logique redondante.

**Fichiers susceptibles de changer** :
- `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManagerContract.kt`
- `data/src/main/java/fr/julien/quievreux/droidplane2/data/NodeManager.kt`
- `app/src/main/java/fr/julien/quievreux/droidplane2/MainViewModel.kt`
- `data/src/test/java/fr/julien/quievreux/droidplane2/data/NodeManagerTest.kt` (ou équivalent)

**Tests à ajouter ou mettre à jour** :
- Test unitaire : `addNodeToMindmap` avec un `Node` complet conserve `detailsText`, `noteText`, `richText`, `latexEquation`.
- Test unitaire : `onAddNodeFromInspector` propage tous les champs du `Node` passé en paramètre.
- Test de régression : après création via inspector + champs remplis, la lecture du nœud retourne les valeurs saisies.

## Risks & Considerations

- **Changement de signature publique** : `addNodeToMindmap` est référencée dans `NodeManagerContract` (interface), `NodeManager`, `MainViewModel` et les tests — à adapter partout.
- **Compatibilité de la surcharge rapide** : `onAddChildNode(text: String)` (ligne 865) et l'ajout depuis la barre restent compatibles si on maintient une surcharge `addNode(String, Node?)` en plus.
- **Sérialisation XML** : `detailsText` et `noteText` sont déjà sérialisés dans `NodeManager.serializeNode` (lignes 569-573) — aucune modification XML nécessaire.
- **Pas de risque de migration de données** : les nœuds existants ne sont pas affectés.

## Open Questions

- Doit-on aussi propager `richText` au moment de la création (non seulement au `updateNode`) ? La même lacune peut exister pour ce champ si l'utilisateur bascule en mode texte riche lors de la création.
- Y a-t-il d'autres points d'entrée de création de nœud (ex : import, drag & drop) qui appellent directement `addNodeToMindmap(String, ...)` et seraient à harmoniser ?
