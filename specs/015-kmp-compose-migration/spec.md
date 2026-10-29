# Feature Specification: Migration vers Kotlin Multiplatform et Compose Multiplatform

**Feature Branch**: `015-kmp-compose-migration`

**Created**: 2026-10-29

**Status**: Clarified

**Input**: User description: "j'aimerais passer le projet en kotlin multiplatforme avec jetpack compose"

## Clarifications

### Session 2026-10-29

- Q: Quelles plateformes cibles doivent être supportées en plus d'Android ? → A: **Option A — Android + Desktop (JVM : macOS, Windows, Linux)** avec partage du code métier (`:core`, `:data`) et de l'interface utilisateur Compose Multiplatform (`commonMain`).
- Q: Comment traiter les services spécifiques à Android (Firebase Crashlytics/Analytics et vérification de version GitHub) ? → A: **Supprimer complètement Firebase Crashlytics & Analytics**, et **conserver la vérification de version GitHub (`UpdateCheckerService`) partagée sur toutes les plateformes** (téléchargement/installation d'APK ou vue GitHub sur Android ; ouverture de la release sur GitHub dans le navigateur sur Desktop).

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Expérience utilisateur Android préservée à 100 % sur base partagée (Priority: P1)

Un utilisateur existant de Droidplane sur Android ouvre, consulte, recherche, modifie (ajout, édition, déplacement par glisser-déposer 2D, suppression de nœuds) et sauvegarde ses cartes mentales Freeplane (`.mm`) sans aucune régression fonctionnelle ni visuelle, alors que toute la logique métier et l'interface utilisateur reposent désormais sur une base commune multiplateforme.

**Why this priority**: Garantir zéro régression pour les utilisateurs actuels est la condition indispensable de la migration avant d'étendre l'application à d'autres systèmes d'exploitation.

**Independent Test**: Peut être testé indépendamment en lançant l'application Android, en ouvrant un fichier `.mm` existant, en effectuant des opérations de lecture/édition/sauvegarde, et en vérifiant que l'ensemble des tests unitaires et d'interface passent.

**Acceptance Scenarios**:

1. **Given** l'application compilée depuis le code partagé multiplateforme sur Android, **When** l'utilisateur ouvre un fichier `.mm` depuis son appareil ou depuis les exemples intégrés, **Then** la carte mentale s'affiche sur le canevas 2D avec tous ses nœuds, icônes, couleurs et notes exactement comme avant la migration.
2. **Given** une carte mentale ouverte sur Android, **When** l'utilisateur ajoute, renomme, déplace (glisser-déposer 2D) ou supprime un nœud, **Then** les modifications sont immédiatement répercutées à l'écran et sauvegardées de manière intègre dans le fichier `.mm`.
3. **Given** l'application installée sur Android, **When** l'utilisateur vérifie les mises à jour ou change le thème (clair, sombre, système, contraste), **Then** les préférences et la vérification de mise à jour fonctionnent sans perte de données.

---

### User Story 2 - Exécution de l'application sur Desktop (JVM : macOS, Windows, Linux) (Priority: P2)

Un utilisateur souhaite utiliser Droidplane sur ordinateur (macOS, Windows, Linux via la cible Desktop JVM) afin d'ouvrir, visualiser et éditer ses fichiers `.mm` avec la même interface graphique Compose Multiplatform partagée qu'Android.

**Why this priority**: L'objectif principal du passage au multiplateforme avec Compose est de permettre l'exécution de l'application et le partage de l'interface sur plus d'un système d'exploitation.

**Independent Test**: Peut être testé en lançant l'application sur la plateforme cible retenue, en chargeant une carte mentale `.mm` et en naviguant/éditant les nœuds dans l'interface partagée.

**Acceptance Scenarios**:

1. **Given** l'application lancée sur une plateforme cible additionnelle supportée, **When** l'utilisateur ouvre une carte mentale `.mm`, **Then** le canevas 2D, la barre de recherche et les boîtes de dialogue d'édition s'affichent et réagissent aux interactions de l'utilisateur.
2. **Given** une carte mentale modifiée sur la nouvelle plateforme cible, **When** l'utilisateur sauvegarde le fichier et l'ouvre dans Freeplane Desktop ou sur Android, **Then** le fichier `.mm` est 100 % compatible et sans perte d'attributs.

---

### User Story 3 - Validation automatisée multiplateforme de la logique et des modèles (Priority: P3)

Un contributeur exécute la suite de tests automatisés sur le code commun (analyseur/sérialiseur `.mm`, gestionnaire d'arbre de nœuds, logique de présentation et d'état) sans dépendre d'un émulateur ou d'un environnement spécifique à un seul système d'exploitation.

**Why this priority**: Permet une validation rapide et fiable de toute la logique métier et de l'état de l'interface sur toutes les plateformes cibles.

**Independent Test**: Peut être testé en exécutant la commande de test commune et en vérifiant que tous les tests de domaine, de parsing `.mm` et d'état UI s'exécutent et réussissent.

**Acceptance Scenarios**:

1. **Given** le code source du dépôt, **When** un développeur lance la suite de tests du code partagé, **Then** 100 % des tests de lecture/écriture `.mm`, de manipulation de nœuds et d'état de vue s'exécutent avec succès.

---

### Edge Cases

- Que se passe-t-il lorsqu'un fichier `.mm` contient des caractères spéciaux, entités XML ou encodages variés lorsqu'il est lu/écrit sur différentes plateformes ? Le format XML généré doit rester strictement identique sur toutes les plateformes.
- Comment le système gère-t-il les fonctionnalités propres à un système d'exploitation (comme l'installation directe d'APK ou le rapport de crash mobile) sur les plateformes où elles ne s'appliquent pas ? Elles doivent être désactivées proprement ou adaptées sans provoquer d'erreur.
- Que se passe-t-il lors de la mise à jour d'un utilisateur Android existant vers la nouvelle version multiplateforme ? Ses fichiers récents et ses préférences de thème sont conservés sans réinitialisation.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système DOIT partager l'intégralité des modèles de données de cartes mentales, de la logique de lecture/écriture des fichiers Freeplane (`.mm`) et de la gestion de l'arbre de nœuds entre toutes les plateformes cibles.
- **FR-002**: Le système DOIT partager l'interface utilisateur (canevas 2D de la carte mentale, cartes de nœuds, barres d'outils, recherche, boîtes de dialogue et paramètres de thème) entre les plateformes cibles.
- **FR-003**: Le système DOIT prendre en charge les plateformes **Android** et **Desktop (JVM : macOS, Windows, Linux)** via Kotlin Multiplatform et Compose Multiplatform.
- **FR-004**: Le système DOIT conserver une compatibilité à 100 % avec le format de fichier Freeplane (`.mm`) lors de la lecture et de la sauvegarde sur chaque plateforme supportée.
- **FR-005**: Le système DOIT abstraire l'accès aux fichiers (ouverture, sauvegarde, fichiers exemples intégrés) pour fournir une expérience native sur chaque plateforme supportée tout en préservant l'accès existant sur Android (fichiers récents, ouverture depuis le système de fichiers).
- **FR-006**: Le système DOIT supprimer complètement Firebase Crashlytics et Analytics du projet, et DOIT partager la vérification de nouvelle version via GitHub Releases (`UpdateCheckerService`) sur toutes les plateformes (avec téléchargement/installation d'APK ou ouverture GitHub sur Android, et ouverture de la page de release GitHub dans le navigateur sur Desktop).
- **FR-007**: Le système DOIT préserver la séparation modulaire des responsabilités (`:core`, `:data`, et couche de présentation/application) tout en exposant des contrats multiplateformes propres.
- **FR-008**: La migration DOIT conserver toutes les fonctionnalités existantes sur Android (canevas 2D, zoom/panoramique, glisser-déposer Freeplane, ajout/édition/suppression de nœuds, notes, icônes, liens hypertextes, recherche et thèmes).

### Key Entities

- **Carte mentale (`MindMap` / `Node`)**: Représentation hiérarchique des nœuds, attributs, styles, positions, icônes et contenus riches d'un fichier Freeplane `.mm`, indépendante de toute plateforme.
- **Gestionnaire de fichiers de plateforme (`FileStorage` / `DocumentSource`)**: Abstraction permettant d'ouvrir, lire et sauvegarder de manière atomique un document `.mm` quel que soit le système d'exploitation hôte.
- **État de présentation partagé (`MainUiState`)**: Modèle d'état immuable pilotant l'interface utilisateur partagée (nœud sélectionné, recherche, dialogues, thème, préférences).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des fonctionnalités utilisateur existantes sur Android fonctionnent à l'identique après la migration (0 régression fonctionnelle sur les parcours d'ouverture, d'édition, de glisser-déposer, de recherche et de sauvegarde).
- **SC-002**: 100 % des fichiers `.mm` de test produisent un résultat de lecture et de sauvegarde identique sur l'ensemble des plateformes cibles.
- **SC-003**: Au moins 90 % du code métier, de manipulation de données et des composants d'interface utilisateur est partagé dans le socle commun entre les plateformes cibles.
- **SC-004**: L'ensemble de la suite de tests automatisés existante et migrée s'exécute avec 0 échec.

## Assumptions

- Les utilisateurs Android existants qui mettent à jour l'application conserveront le même identifiant d'application (`applicationId`), leurs préférences locales et leur historique de fichiers.
- Le rendu des icônes Freeplane et des ressources textuelles localisées (français/anglais) sera accessible de manière unifiée sur toutes les plateformes cibles.
- Les interactions tactiles (appui court, appui long au relâchement, glisser-déposer 2D, pincer pour zoomer) et les interactions au pointeur/souris fonctionnent à travers les primitives d'entrée unifiées de l'interface partagée.
