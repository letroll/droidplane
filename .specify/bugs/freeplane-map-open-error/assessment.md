# Bug Assessment: Freeplane signale une erreur à l'ouverture d'une carte `.mm` enregistrée par DroidPlane2 (`DroidPlane2map.mm`)

- **Slug**: freeplane-map-open-error
- **Created**: 2026-11-08
- **Source**: pasted text + local file `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm`
- **Verdict**: valid
- **Severity**: critical

## Report (verbatim or summarized)

> L'ouverture de ma carte intitulée DroidPlane2map.mm provoque l'indication par le logiciel FreePlane de la présence d'une erreur dans celle-ci. Aide moi à comprendre pourquoi et à corriger cela?
>
> chemin du fichier : `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm`
> dossier des logs freeplane : `/Users/julienquievreux/.freeplane/1.12.x/logs` (`freeplane_version = 1.13.4; freeplane_xml_version = freeplane 1.12.15`)

Extrait des 10 premières lignes de `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm` :

```xml
<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<map>
	<node ID="ID_1385587321" CREATED="1361978276662" MODIFIED="1794042503907" TEXT="DroidPlane 2">
		<edge STYLE="hide_edge" />
		<font BOLD="true" NAME="Liberation Sans" SIZE="10" />
		<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT" />
		<icon BUILTIN="mindmap" />
		<arrowlink DESTINATION="ID_1499691433" COLOR="#000000" STARTARROW="NONE" ENDARROW="DEFAULT" />
		<hook NAME="MapStyle" background="#2e3440ff" />
```

## Symptom

Lorsqu'une carte mentale `.mm` créée par Freeplane (ou créée/modifiée puis enregistrée par DroidPlane2, telle que `DroidPlane2map.mm`) est ouverte dans Freeplane Desktop (v1.12.x / v1.13.4), Freeplane affiche un message d'erreur / avertissement à l'ouverture de la carte et le nœud racine se retrouve pollué par des propriétés parasites (nuage gris `#f0f0f0`, police taille 10 `Liberation Sans`, bordure cachée `hide_edge`, et lien fléché cassé vers un identifiant inexistant `ID_1499691433`).

## Reproduction

1. Ouvrir dans DroidPlane2 une carte mentale `.mm` issue de Freeplane contenant un bloc `<hook NAME="MapStyle">...<map_styles>...</map_styles></hook>` (présent dans toutes les cartes enregistrées par Freeplane) ou créer une carte dans DroidPlane2.
2. Enregistrer la carte depuis DroidPlane2 (ex. `DroidPlane2map.mm`).
3. Ouvrir le fichier `.mm` résultant dans Freeplane Desktop :
   - Le fichier débute par `<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>` suivi de `<map>` **sans attribut `version="freeplane ..."`**, ce qui empêche `MapVersionInterpreter` de reconnaître le format Freeplane.
   - Le nœud racine contient des balises `<edge STYLE="hide_edge" />`, `<font BOLD="true" NAME="Liberation Sans" SIZE="10" />`, `<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT" />` et `<arrowlink DESTINATION="ID_1499691433" ... />` (ciblant un `ID` de `<stylenode>` qui n'existe plus dans la carte), et `<hook NAME="MapStyle" ... />` a perdu tout son sous-arbre XML (`<properties>`, `<map_styles>`).

## Suspected Code Paths

- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt:428-431` (`serializeMindmap`) — Appelle `serializer.startDocument("UTF-8", true)` (qui génère le prologue `<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>` non standard pour Freeplane) puis `serializer.startNodeTag(MAP)` **sans écrire l'attribut `version`** (ex. `version="freeplane 1.11.5"` ou la version d'origine de la carte) sur la balise `<map>`.
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt:158-183` (`parseHook`) — Lorsqu'une balise `<hook>` non auto-fermante (comme `<hook NAME="MapStyle">`) est rencontrée, `parseHook` lit uniquement ses attributs et **ne consomme pas le sous-arbre XML jusqu'à la balise fermante `</hook>`**. La boucle principale `loadMindMapFromXml` (`NodeManager.kt:146-176`) continue alors de parser toutes les balises internes de `<map_styles><stylenode>...</stylenode></map_styles>` (`<edge>`, `<font>`, `<cloud>`, `<icon>`, `<arrowlink>`) comme si elles appartenaient directement au `<node>` parent (le nœud racine).
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt:647-660` (`serializeNode`) — Sérialise chaque `GenericHookElement` sans réécrire son contenu XML interne (`hook.rawXml`), transformant `<hook NAME="MapStyle">...</hook>` en une balise vide `<hook NAME="MapStyle" />` et laissant sur le nœud racine l'`<arrowlink DESTINATION="ID_1499691433">` orphelin issu du `<stylenode LOCALIZED_TEXT="default" ID="ID_1499691433">`.

## Root Cause Hypothesis

**Confidence: high.**

L'erreur signalée par Freeplane à l'ouverture de `DroidPlane2map.mm` résulte de la combinaison de **deux bugs majeurs** dans le cycle de lecture/écriture XML de `:data` :

1. **Absence d'attribut `version` sur `<map>` et prologue XML incompatible (`NodeManager.serializeMindmap`)** :
   Freeplane identifie le dialecte et la version d'un fichier `.mm` en inspectant le début du fichier via `MapVersionInterpreter` (qui attend `<map version="freeplane ...">`, par ex. `<map version="freeplane 1.11.5">` ou `<map version="freeplane 1.12.15">`). Or `NodeManager.serializeMindmap` émet `<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>` suivi de `<map>` sans attribut `version`, ce qui fait échouer la détection de version Freeplane à l'ouverture.
2. **Fuite du contenu interne de `<hook NAME="MapStyle">` vers le nœud parent lors du parsing (`XmlParseUtilsDefaultImpl.parseHook`)** :
   Dans une carte Freeplane, le nœud racine contient `<hook NAME="MapStyle"><properties .../><map_styles><stylenode ...>...</stylenode></map_styles></hook>`. Les `<stylenode>` internes définissent les styles par défaut de Freeplane à l'aide des mêmes noms de balises que les nœuds classiques :
   - `<stylenode LOCALIZED_TEXT="default" ID="ID_1499691433">` contient `<arrowlink ... DESTINATION="ID_1499691433" .../>` ;
   - `<stylenode LOCALIZED_TEXT="defaultstyle.floating">` contient `<edge STYLE="hide_edge"/>` et `<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT"/>` ;
   - `<stylenode LOCALIZED_TEXT="styles.subsubtopic">` contient `<font NAME="Liberation Sans" SIZE="10" BOLD="true"/>`.
   Comme `parseHook` ne consomme pas les événements XML jusqu'au `</hook>` fermant, toutes ces balises de `<stylenode>` sont appliquées au nœud racine `ID_1385587321`, tandis que `<map_styles>` est détruit. Lors de l'enregistrement, le nœud racine se retrouve affublé d'un `<arrowlink DESTINATION="ID_1499691433">` pointant vers un ID inexistant, d'un `<edge STYLE="hide_edge" />`, d'un `<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT" />` et d'un `<font SIZE="10" />`.

## Proposed Remediation

**Preferred**:

1. **Préserver et écrire l'en-tête `<map version="...">` compatible Freeplane dans `NodeManager`** :
   - Lors du parsing de `<map>` dans `loadMindMapFromXml`, capturer l'attribut `version` (ex. `freeplane 1.12.15` ou `freeplane 1.11.5`, avec `freeplane 1.11.5` par défaut pour les nouvelles cartes).
   - Lors de la sérialisation dans `serializeMindmap`, ne pas émettre le prologue `<?xml ... standalone='yes' ?>` (ou commencer directement par `<map version="...">`) et écrire systématiquement l'attribut `version` sur `<map>`.
2. **Consommer et conserver le sous-arbre XML des balises `<hook>` dans `XmlParseUtilsDefaultImpl.parseHook` et `NodeManager.serializeNode`** :
   - Dans `parseHook(xpp, nodes)`, si la balise `<hook>` n'est pas auto-fermante, consommer tout son contenu interne jusqu'à revenir à la profondeur initiale (`xpp.depth == startingDepth` sur `END_TAG` `"hook"`), exactement comme le fait `NodeUtilsDefaultImpl.loadRichContent`, et stocker ce contenu XML interne dans `GenericHookElement.rawXml` (ou `textContent`).
   - Ainsi, aucune balise `<font>`, `<edge>`, `<cloud>`, `<icon>` ou `<arrowlink>` située à l'intérieur d'un `<stylenode>` de `<hook NAME="MapStyle">` ne polluera plus le nœud parent.
   - Dans `NodeManager.serializeNode`, si `hook.rawXml` est présent et non vide, l'écrire à l'intérieur de `<hook ...>...</hook>` via `writeRawXmlToSerializer(serializer, hook.rawXml)`.
3. **Réparer le fichier existant `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm`** :
   - Ajouter `version="freeplane 1.12.15"` sur `<map>` (et retirer le prologue `<?xml ... standalone='yes' ?>`).
   - Retirer du nœud racine `ID_1385587321` les 4 éléments parasites injectés par le bug de `MapStyle` (`<edge STYLE="hide_edge" />`, `<font BOLD="true" NAME="Liberation Sans" SIZE="10" />`, `<cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT" />`, `<arrowlink DESTINATION="ID_1499691433" ... />`).

**Files likely to change**:
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt`
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt`
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/NodeAttribute.kt` (si ajout de `VERSION("version")`)
- `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt`
- `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/HooksXmlTest.kt`
- `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm`

**Tests to add or update**:
- Ajouter un test aller-retour dans `FreeplaneXmlRoundTripTest` / `HooksXmlTest` avec une carte contenant `<map version="freeplane 1.12.15">` et un bloc `<hook NAME="MapStyle"><properties .../><map_styles><stylenode ...><arrowlink DESTINATION="ID_STYLE"/><edge STYLE="hide_edge"/><cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT"/><font SIZE="10"/></stylenode></map_styles></hook>` vérifiant que :
  1. Le nœud racine **n'hérite d'aucun** `edge`, `cloud`, `font`, `icon` ou `arrowlink` issu des `<stylenode>` internes à `<hook NAME="MapStyle">`.
  2. Le fichier `.mm` sérialisé commence par `<map version="freeplane 1.12.15">` et conserve le sous-arbre `<hook NAME="MapStyle">...</hook>`.

## Risks & Considerations

- Certains tests existants peuvent vérifier la présence ou non d'éléments XML dans le fichier sérialisé : conserver la compatibilité avec `XmlPullParser` lors de la relecture des fichiers `.mm` générés.
- Les cartes `.mm` déjà sauvegardées par une version antérieure de DroidPlane2 (comme `DroidPlane2map.mm`) ont déjà subi l'injection des 4 balises de `<stylenode>` sur leur nœud racine ; outre la réparation directe de `DroidPlane2map.mm`, il est possible de filtrer lors de la sérialisation (ou du chargement) tout `<arrowlink>` dont `DESTINATION` ne correspond à aucun nœud de la carte si souhaité.

## Open Questions

- Aucune : la cause racine est intégralement confirmée par l'inspection de `DroidPlane2map.mm` et du code de `XmlParseUtilsDefaultImpl.kt` / `NodeManager.kt`.
