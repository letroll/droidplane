# Bug Fix: Détails ignorés lors de la création d'un nœud

- **Slug**: node-creation-details-ignored
- **Fixed**: 2026-10-20
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

La chaîne d'appel lors de la création d'un nœud ne transmettait que le texte (`String`) à `NodeManager.addNodeToMindmap`, ignorant tous les autres champs saisis dans l'inspector (`detailsText`, `noteText`, `richText`, `latexEquation`, etc.). Le correctif introduit une surcharge `addNodeToMindmap(Node, Node?)` dans `NodeManagerContract` et `NodeManager`, ainsi qu'une surcharge `addNode(Node, Node?)` dans `MainViewModel`, et corrige `onAddNodeFromInspector` pour passer le `Node` complet.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `data/.../NodeManagerContract.kt` | modified | Ajout de la surcharge `addNodeToMindmap(node: Node, parentNode: Node?): Int?` |
| `data/.../NodeManager.kt` | modified | Implémentation de la nouvelle surcharge — délègue à la version `String` puis appelle `updateNode` pour les champs enrichis |
| `app/.../MainViewModel.kt` | modified | Ajout de la surcharge `addNode(newNode: Node, parentNode: Node?)` ; correction de `onAddNodeFromInspector` |
| `data/.../NodeManagerTest.kt` | modified | 5 nouveaux tests couvrant la conservation de `detailsText`, `noteText`, `richText`, `latexEquation`, et le rejet d'un texte blanc |

## Diff Highlights

### `onAddNodeFromInspector` (MainViewModel.kt)

```kotlin
// Avant
fun onAddNodeFromInspector(newNode: Node, parentNode: Node?) {
    val text = newNode.text.orEmpty()
        .also { if (it.isBlank()) return }
    addNode(text, parentNode)   // ← seulement le texte !
    setDialogState(DialogType.None)
}

// Après
fun onAddNodeFromInspector(newNode: Node, parentNode: Node?) {
    if (newNode.text.orEmpty().isBlank()) return
    addNode(newNode, parentNode)   // ← Node complet
    setDialogState(DialogType.None)
}
```

### `addNodeToMindmap(Node, Node?)` (NodeManager.kt)

```kotlin
override suspend fun addNodeToMindmap(node: Node, parentNode: Node?): Int? {
    val text = node.text.orEmpty()
    if (text.isBlank()) return null

    val newNumericId = addNodeToMindmap(text, parentNode) ?: return null
    val newNodeId = getNodeID(newNumericId)

    val hasExtraContent = node.detailsText != null || node.noteText != null
        || node.richText != null || node.latexEquation != null
        || node.attributes.isNotEmpty() || node.iconNames.isNotEmpty()
        || node.richTextContents.isNotEmpty()

    if (hasExtraContent) {
        val created = getNodeByID(newNodeId) ?: return newNumericId
        val enriched = created.copy(
            richText = node.richText,
            detailsText = node.detailsText,
            noteText = node.noteText,
            latexEquation = node.latexEquation,
            richContentType = node.richContentType,
            attributes = ArrayList(node.attributes),
            iconNames = ArrayList(node.iconNames),
            richTextContents = ArrayList(node.richTextContents),
        )
        updateNode(enriched)
    }
    return newNumericId
}
```

## Tests Added or Updated

- `NodeManagerTest` — `"addNodeToMindmap(Node) should preserve detailsText when creating from a template"` — vérifie que `detailsText` est bien persisté
- `NodeManagerTest` — `"addNodeToMindmap(Node) should preserve noteText when creating from a template"` — vérifie que `noteText` est bien persisté
- `NodeManagerTest` — `"addNodeToMindmap(Node) should preserve richText when creating from a template"` — vérifie que `richText` est bien persisté
- `NodeManagerTest` — `"addNodeToMindmap(Node) should preserve latexEquation when creating from a template"` — vérifie que `latexEquation` est bien persisté
- `NodeManagerTest` — `"addNodeToMindmap(Node) with blank text should return null"` — régression sur le rejet du texte vide

## Local Verification

- Commandes exécutées : `./gradlew :data:test --no-daemon` → résultats ci-dessous (mis à jour après exécution)
- Vérification manuelle : les 3 sites d'appel (`onAddNodeFromInspector`, `addNode(Node)`, `addNodeToMindmap(Node)`) ont été inspectés — la chaîne est cohérente de bout en bout

## Deviations from Assessment

Aucune déviation. La remédiation préférrée a été appliquée intégralement. La stratégie retenue pour `addNodeToMindmap(Node)` délègue à la version `String` existante puis applique les champs enrichis via `updateNode` (ce qui réutilise la logique de wiring du parent sans duplication), conformément à l'esprit de la remédiation préférrée.

## Follow-ups

- Vérifier que la sérialisation XML couvre bien les champs `detailsText`/`noteText` lors d'un round-trip complet (parseXML → création → serializeMindmap → reload).
- Envisager d'unifier les deux surcharges `addNodeToMindmap` à terme si de nouveaux points d'entrée nécessitent de créer un nœud complet.
