# Feature Specification: Génération simultanée des distributions desktop (Windows, Linux, macOS) lors de la publication d'une release

**Feature Branch**: `018-desktop-multiplatform-releases`

**Created**: 2026-11-11

**Status**: Draft

**Input**: User description: "J'aimerais à présent que le script qui génère la release en fasse une supplémentaire en même temps, pour chacune des plateformes classiques, à savoir Windows, Linux et Mac."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Téléchargement et utilisation directe sur les plateformes de bureau (Priority: P1)

Un utilisateur sur ordinateur (fonctionnant sous Windows, Linux ou macOS) souhaite télécharger directement depuis la page des versions publiées (Releases) le livrable prêt à l'emploi correspondant à son système d'exploitation, afin d'utiliser Droidplane pour ouvrir, consulter et modifier ses cartes mentales sans devoir configurer d'environnement de développement ou compiler le projet.

**Why this priority**: C'est la valeur centrale attendue par les utilisateurs : étendre la disponibilité de Droidplane au-delà d'Android en fournissant des binaires directement utilisables sur chacun des trois systèmes d'exploitation desktop majeurs.

**Independent Test**: Télécharger le livrable publié pour une plateforme donnée (par exemple Windows, Linux ou macOS) sur une machine disposant de ce système d'exploitation, lancer l'application et valider que l'interface s'affiche correctement et permet d'ouvrir une carte mentale.

**Acceptance Scenarios**:

1. **Given** une nouvelle version publiée sur le dépôt, **When** un utilisateur sous Windows consulte les fichiers de la release, **Then** un livrable installable ou exécutable pour Windows est disponible au téléchargement et permet d'exécuter l'application.
2. **Given** une nouvelle version publiée sur le dépôt, **When** un utilisateur sous Linux consulte les fichiers de la release, **Then** un livrable installable ou exécutable pour Linux est disponible au téléchargement et permet d'exécuter l'application.
3. **Given** une nouvelle version publiée sur le dépôt, **When** un utilisateur sous macOS consulte les fichiers de la release, **Then** un livrable installable ou exécutable pour macOS est disponible au téléchargement et permet d'exécuter l'application.

---

### User Story 2 - Publication automatisée et synchronisée de l'ensemble des plateformes (Priority: P2)

Un mainteneur du projet souhaite que le processus automatisé de release génère et attache simultanément tous les livrables (Android et les plateformes desktop Windows, Linux, macOS) à la même release GitHub, sous un numéro de version identique et avec un journal des modifications commun.

**Why this priority**: Élimine toute manipulation manuelle risquée ou asynchrone, garantissant la cohérence stricte des versions et du changelog entre toutes les plateformes supportées.

**Independent Test**: Déclencher une publication de release via le processus automatisé et vérifier sur la page de la release que l'ensemble des fichiers pour Android, Windows, Linux et macOS sont présents sous la même étiquette de version avec le même changelog.

**Acceptance Scenarios**:

1. **Given** le déclenchement d'un cycle de release automatisé, **When** le pipeline de construction et de publication se termine, **Then** une unique release est créée, contenant à la fois l'application mobile et les distributions pour Windows, Linux et macOS.
2. **Given** une publication de release terminée, **When** un utilisateur examine les fichiers mis à disposition, **Then** tous les fichiers portent le même numéro de version et partagent les mêmes notes de version (changelog).

---

### User Story 3 - Traçabilité et identification claire des livrables par plateforme (Priority: P3)

Un utilisateur ou un mainteneur souhaite identifier immédiatement et sans ambiguïté quel fichier correspond à quel système d'exploitation dans la liste des actifs de la release grâce à un nommage standardisé et explicite.

**Why this priority**: Évite les confusions lors du téléchargement et facilite les contrôles automatisés d'intégrité de la release.

**Independent Test**: Inspecter la liste des actifs attachés à la release et vérifier que chaque fichier indique explicitement le nom de l'application, la version et le système d'exploitation cible.

**Acceptance Scenarios**:

1. **Given** la liste des actifs d'une release publiée, **When** l'utilisateur examine les noms de fichiers, **Then** chaque livrable desktop mentionne clairement la plateforme cible (Windows, Linux ou macOS) ainsi que la version de l'application.

---

### Edge Cases

- **Échec partiel de génération sur une plateforme** : Si la compilation ou l'empaquetage échoue pour l'une des plateformes de bureau, le processus de release doit signaler clairement la défaillance et ne pas publier une release incohérente ou silencieusement tronquée.
- **Différences d'architectures processeur** : Les plateformes desktop peuvent utiliser des architectures différentes (x64 / 64-bit et ARM64 notamment pour macOS Apple Silicon). Le système doit cibler en priorité les architectures 64-bit standards garantissant l'accessibilité sur chaque OS.
- **Convention de nommage et collisions** : Les livrables de chaque plateforme doivent porter des extensions et noms distincts pour éviter tout conflit lors du téléversement sur la même release.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le processus automatisé de release DOIT générer un livrable autonome ou installable pour la plateforme **Windows** lors de chaque publication.
- **FR-002**: Le processus automatisé de release DOIT générer un livrable autonome ou installable pour la plateforme **Linux** lors de chaque publication.
- **FR-003**: Le processus automatisé de release DOIT générer un livrable autonome ou installable pour la plateforme **macOS** lors de chaque publication.
- **FR-004**: Le système DOIT publier les livrables de toutes les plateformes (Android, Windows, Linux, macOS) simultanément au sein d'une seule et même publication de version (release).
- **FR-005**: Le système DOIT synchroniser le numéro de version de sorte que l'ensemble des livrables partagent la même version et le même journal des modifications (changelog).
- **FR-006**: Les livrables DOIVENT porter des noms de fichiers normalisés et explicites comprenant le nom du produit, le numéro de version et la plateforme cible.
- **FR-007**: Le processus de release DOIT valider les tests automatisés du projet avant de procéder à la publication finale des livrables.

### Key Entities

- **Release (Publication de version)** : Objet représentant une version officielle, défini par un numéro de version unifié, une étiquette de tag Git, un journal des modifications (changelog) et une collection d'actifs téléchargeables.
- **Livrable de distribution (Distribution Asset)** : Paquet ou archive exécutable spécifique à une plateforme cible (Android, Windows, Linux, macOS), prêt pour l'installation ou l'exécution directe par l'utilisateur final.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des publications de release réussies mettent à disposition simultanément les livrables pour Android, Windows, Linux et macOS sous la même étiquette de version.
- **SC-002**: Le processus de publication de version s'exécute de façon entièrement automatisée, sans étape manuelle intermédiaire entre le déclenchement et la disponibilité des fichiers sur la release.
- **SC-003**: 0 divergence de numéro de version ou de journal des modifications entre les livrables d'une même release.
- **SC-004**: Un utilisateur final sur n'importe laquelle des plateformes desktop cibles peut lancer l'application en moins de 3 minutes après avoir téléchargé son livrable.

## Assumptions

- L'infrastructure d'exécution de la CI/CD dispose des accès et capacités nécessaires (matrice de systèmes ou environnements d'empaquetage adaptés) pour produire les formats de chaque plateforme cible.
- Le format des distributions desktop retenu correspond aux formats standards reconnus sur chaque système d'exploitation (ex. installateur natif, paquet ou archive exécutable).
- Les mécanismes existants d'auto-incrémentation de version et de génération de notes de version restent la référence unique pour l'ensemble des plateformes.
