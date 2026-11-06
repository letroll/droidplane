# Feature Specification: Support de l'état réduit (plié / déplié) des nœuds Freeplane

**Feature Branch**: `016-node-folded-state`

**Created**: 2026-11-06

**Status**: Draft

**Input**: User description: "ajout le support de l'état réduit ou non sur les nodes comme dans freeplane. Tu as un exemple de carte avec des nodes réduit (enfant 1 et enfant 2 dans le node test) dans le fichier joint."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Respect de l'état réduit à l'ouverture d'une carte mentale (Priority: P1)

Lorsqu'un utilisateur ouvre un fichier de carte mentale Freeplane (`.mm`) contenant des nœuds parents enregistrés à l'état réduit (attribut `FOLDED="true"`, comme le nœud `test` masquant ses sous-nœuds `enfant 1` et `enfant 2` dans la carte d'exemple fournie), l'application affiche immédiatement ces branches à l'état replié avec l'indicateur de dépliage (`+`), tout en affichant normalement les branches non réduites (`FOLDED="false"` ou attribut absent).

**Why this priority**: C'est le besoin principal exprimé par l'utilisateur : éviter qu'une grande carte mentale structurée dans Freeplane avec des branches repliées ne s'ouvre entièrement déployée et illisible dans l'application.

**Independent Test**: Peut être testé de manière autonome en ouvrant le fichier d'exemple `feature_test.mm` (où le nœud `test` possède `FOLDED="true"` avec deux enfants `enfant 1` et `enfant 2`) et en vérifiant que `test` est affiché avec l'indicateur `+` et que `enfant 1` et `enfant 2` ne sont pas affichés tant que l'utilisateur ne déplie pas `test`.

**Acceptance Scenarios**:

1. **Given** un fichier `.mm` contenant un nœud non-racine avec `FOLDED="true"` et au moins un nœud enfant (ex. `test` avec `enfant 1` et `enfant 2`), **When** l'utilisateur ouvre ce fichier, **Then** le nœud `test` est affiché à l'état réduit (indicateur `+`) et ses nœuds enfants (`enfant 1`, `enfant 2`) ainsi que leurs sous-branches sont masqués.
2. **Given** un fichier `.mm` contenant des nœuds avec `FOLDED="false"` ou sans attribut `FOLDED`, **When** l'utilisateur ouvre ce fichier, **Then** ces nœuds sont affichés à l'état déplié (indicateur `−` s'ils ont des enfants) et leurs enfants directs sont visibles.
3. **Given** un nœud affiché à l'état réduit après le chargement d'un fichier `.mm`, **When** l'utilisateur actionne son indicateur de dépliage (`+`), **Then** ses enfants directs deviennent immédiatement visibles sur le canevas et l'indicateur passe à `−`.

---

### User Story 2 - Persistance de l'état réduit ou déplié lors de l'enregistrement (Priority: P1)

Lorsqu'un utilisateur replie ou déplie manuellement un ou plusieurs nœuds dans la carte mentale puis enregistre le fichier, l'état réduit (`FOLDED="true"`) ou déplié de chaque nœud est fidèlement conservé dans le fichier `.mm` enregistré afin de garantir une compatibilité bidirectionnelle totale avec Freeplane.

**Why this priority**: Si l'utilisateur réorganise la visibilité des branches (en dépliant un nœud initialement réduit ou en repliant une nouvelle branche) et sauvegarde sa carte, la réouverture du fichier dans Freeplane ou dans l'application doit refléter exactement le dernier état choisi.

**Independent Test**: Peut être testé en ouvrant une carte avec un nœud réduit (`test`), en le dépliant, en repliant un autre nœud parent, puis en sauvegardant et rechargeant la carte pour vérifier que le premier nœud est désormais déplié et le second réduit.

**Acceptance Scenarios**:

1. **Given** un nœud parent initialement déplié, **When** l'utilisateur le replie puis enregistre la carte mentale, **Then** le nœud est écrit avec `FOLDED="true"` dans le fichier `.mm` sauvegardé et la modification de l'état de pliage est reconnue comme un changement non enregistré avant la sauvegarde.
2. **Given** un nœud parent initialement réduit (`FOLDED="true"` à l'ouverture), **When** l'utilisateur le déplie puis enregistre la carte mentale, **Then** le nœud n'a plus `FOLDED="true"` dans le fichier `.mm` sauvegardé et s'ouvre à l'état déplié lors du prochain chargement.
3. **Given** une carte contenant plusieurs niveaux de hiérarchie où un parent et un sous-nœud sont tous deux à l'état réduit, **When** l'utilisateur déplie uniquement le nœud parent, **Then** le sous-nœud conserve son propre état réduit (`+`) tant qu'il n'est pas lui-même déplié.

---

### User Story 3 - Dépliage automatique cohérent lors de l'édition, de la recherche et de la navigation (Priority: P2)

Lorsqu'une action de l'utilisateur cible ou révèle un nœud situé à l'intérieur d'une branche réduite (ajout d'un enfant à un nœud réduit, déplacement d'un nœud vers un parent réduit par glisser-déposer, annulation de suppression, sélection d'un résultat de recherche ou suivi d'un lien interne vers un nœud masqué), la ou les branches parentes concernées se déplient automatiquement (visuellement et dans le modèle de données) pour rendre le nœud cible visible.

**Why this priority**: Empêche que des éléments nouvellement créés, déplacés ou recherchés par l'utilisateur ne restent invisibles derrière un nœud replié, tout en évitant toute désynchronisation entre l'affichage et l'état enregistré.

**Independent Test**: Peut être testé en effectuant une recherche textuelle portant sur `enfant 1` dans la carte `feature_test.mm` (où `test` est initialement réduit) et en vérifiant que `test` se déplie automatiquement pour afficher et mettre en évidence `enfant 1`.

**Acceptance Scenarios**:

1. **Given** un nœud réduit (`test`) contenant un enfant (`enfant 1`), **When** l'utilisateur recherche le texte `enfant 1` ou suit un lien interne pointant vers `enfant 1`, **Then** tous les ancêtres réduits de `enfant 1` (dont `test`) sont automatiquement dépliés afin que `enfant 1` apparaisse à l'écran.
2. **Given** un nœud réduit (`test`), **When** l'utilisateur lui ajoute un nouveau nœud enfant ou dépose un nœud dessus par glisser-déposer, **Then** le nœud `test` passe à l'état déplié tant à l'écran que dans les données persistées.
3. **Given** un nœud réduit dont le dernier enfant est supprimé ou déplacé ailleurs, **When** ce nœud n'a plus aucun enfant, **Then** son état réduit est automatiquement effacé et aucun indicateur de pliage n'est affiché.

---

### Edge Cases

- **Nœud racine avec `FOLDED="true"`** : Dans Freeplane, le nœud racine d'une carte mentale ne peut pas être replié (dans `feature_test.mm`, la racine porte `FOLDED="false"`). Si un fichier externe malformé contient `FOLDED="true"` sur le nœud racine, la racine DOIT toujours rester dépliée afin que la carte reste consultable.
- **Nœud feuille (sans enfant) avec `FOLDED="true"`** : Si un fichier `.mm` contient `FOLDED="true"` sur un nœud qui ne possède aucun enfant, cet attribut est ignoré à l'affichage (pas de bouton `+` sur un nœud sans enfant) et nettoyé lors de la suppression du dernier enfant.
- **Hiérarchies profondément imbriquées avec plusieurs niveaux réduits** : Si un nœud A (`FOLDED="true"`) contient un enfant B lui-même réduit (`FOLDED="true"`) qui contient un petit-enfant C, le chargement du fichier doit enregistrer l'état réduit de A ET de B. Lorsque l'utilisateur déplie A, B apparaît mais reste réduit (C demeure masqué jusqu'au dépliage explicite de B).
- **Recherche de texte dans les branches réduites** : Les nœuds enfants masqués par un parent réduit (ex. `enfant 1` et `enfant 2` sous `test`) DOIVENT toujours être indexés et trouvés par la fonction de recherche ; naviguer vers un résultat de recherche situé dans une branche réduite déplie automatiquement ses ancêtres.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Lors du chargement d'un fichier `.mm`, le système DOIT initialiser l'état réduit des nœuds sur le canevas à partir de l'attribut `FOLDED="true"` présent sur chaque nœud non-racine possédant au moins un enfant, y compris sur les nœuds imbriqués à plusieurs niveaux de profondeur.
- **FR-002**: Le nœud racine de la carte mentale ne DOIT jamais être mis à l'état réduit, même si le fichier source comporte `FOLDED="true"` sur la balise racine.
- **FR-003**: Lorsque l'utilisateur bascule l'état réduit/déplié d'un nœud via son indicateur (`+` / `−`), le système DOIT synchroniser immédiatement cet état avec le modèle de données du nœud (`isFolded`) et marquer la carte mentale comme ayant des modifications non enregistrées.
- **FR-004**: Lors de l'enregistrement d'une carte mentale en fichier `.mm`, le système DOIT sérialiser `FOLDED="true"` uniquement pour les nœuds actuellement à l'état réduit et omettre `FOLDED="true"` pour les nœuds qui ont été dépliés ou qui ne possèdent pas d'enfants.
- **FR-005**: Lorsqu'un enfant est ajouté à un nœud réduit, déplacé sous un nœud réduit (glisser-déposer), ou restauré sous un nœud réduit (annulation de suppression), le nœud parent DOIT être automatiquement déplié à la fois visuellement et dans le modèle de données persisté.
- **FR-006**: Lorsqu'un nœud perd son dernier enfant (par suppression ou déplacement), son état réduit DOIT être automatiquement désactivé visuellement et dans le modèle de données persisté.
- **FR-007**: Lorsque l'utilisateur navigue vers un nœud cible via la recherche textuelle ou via un lien interne entre nœuds, tous les nœuds ancêtres réduits de ce nœud cible DOIVENT être automatiquement dépliés pour rendre le nœud cible visible à l'écran.

### Key Entities *(include if feature involves data)*

- **Node (Nœud de carte mentale)** : Élément hiérarchique de la carte mentale possédant un identifiant unique (`ID`), une liste de nœuds enfants, et un indicateur booléen d'état réduit (`FOLDED` / `isFolded`) déterminant si ses descendants directs et indirects sont repliés.
- **État de pliage du canevas (Collapsed Nodes State)** : Ensemble des identifiants de nœuds actuellement repliés dans la vue de la carte mentale, initialisé à partir des nœuds marqués `FOLDED="true"` au chargement du fichier et maintenu en synchronisation bidirectionnelle avec les nœuds du document.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des nœuds non-racines ayant `FOLDED="true"` et au moins un enfant dans un fichier `.mm` (tel que le nœud `test` avec `enfant 1` et `enfant 2` dans `feature_test.mm`) s'affichent à l'état réduit dès l'ouverture de la carte mentale, sur mobile comme sur ordinateur.
- **SC-002**: 100 % des modifications d'état réduit/déplié effectuées par l'utilisateur sont préservées après un cycle complet d'enregistrement et de réouverture du fichier `.mm` (compatibilité aller-retour sans perte avec Freeplane).
- **SC-003**: 100 % des navigations vers un nœud masqué (via la recherche ou un lien interne) ainsi que les ajouts/déplacements d'enfants sous un parent réduit déplient automatiquement la branche parente en moins d'une action utilisateur supplémentaire.

## Assumptions

- Le rendu visuel de l'indicateur de pliage (`+` lorsque le nœud est réduit, `−` lorsqu'il est déplié) et le masquage des sous-arbres réduits sur le canevas 2D sont déjà en place dans l'interface et seront réutilisés tels quels.
- Conformément au comportement de Freeplane Desktop, l'attribut `FOLDED="false"` sur les nœuds non réduits est implicite lors de l'enregistrement (seul `FOLDED="true"` a besoin d'être écrit sur les nœuds réduits, tandis que `FOLDED="true"` et `FOLDED="false"` sont tous deux reconnus à la lecture).
- Le pliage/dépliage d'une branche par l'utilisateur constitue une modification de la carte mentale au même titre que les autres modifications structurelles, activant l'indicateur de changements non enregistrés.
