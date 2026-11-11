# Feature Specification: Génération et inclusion du format universel AppImage pour Linux lors des releases

**Feature Branch**: `019-linux-appimage-release`

**Created**: 2026-11-11

**Status**: Draft

**Input**: User description: "inclut aussi pour les releases le format appimage pour linux"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Téléchargement et exécution portable immédiate sur toute distribution Linux (Priority: P1) 🎯 MVP

Un utilisateur sur ordinateur sous Linux (quelle que soit sa distribution : Ubuntu, Debian, Arch Linux, Fedora, openSUSE, Manjaro, etc.) souhaite télécharger directement depuis la page des releases un livrable unique et autonome au format AppImage, lui attribuer les droits d'exécution et lancer Droidplane immédiatement, sans installation complexe, sans dépendance Java préalable et sans nécessiter les privilèges administrateur (root).

**Why this priority**: C'est la valeur centrale demandée : lever la barrière des distributions spécifiques (comme le paquet `.deb` restreint à la famille Debian/Ubuntu) et offrir une compatibilité universelle "plug-and-play" pour l'ensemble de l'écosystème Linux.

**Independent Test**: Télécharger le fichier AppImage produit lors d'une release sur une machine ou un conteneur Linux x64, le rendre exécutable (`chmod +x`), le lancer et vérifier que l'interface de Droidplane s'affiche correctement et permet de consulter et éditer des cartes mentales.

**Acceptance Scenarios**:

1. **Given** une nouvelle version publiée sur le dépôt GitHub, **When** un utilisateur sous Linux télécharge le fichier AppImage, **Then** le fichier est exécutable directement sur toute distribution Linux x64 supportée sans nécessiter de gestionnaire de paquets spécifique.
2. **Given** l'application lancée via AppImage, **When** l'utilisateur ouvre, visualise et sauvegarde une carte mentale `.mm`, **Then** l'ensemble des fonctionnalités graphiques et de persistance des données fonctionne de manière intègre et identique aux autres distributions.

---

### User Story 2 - Intégration automatisée de l'AppImage dans le cycle de publication multiplateforme (Priority: P2)

Un mainteneur du projet souhaite que la génération du livrable AppImage pour Linux soit intégrée de manière transparente et automatisée dans le workflow de release, afin qu'il soit produit et publié au sein de la même release GitHub avec le même numéro de version et le même journal des modifications que les autres livrables (Android APK, Windows MSI, Linux DEB, macOS DMG).

**Why this priority**: Élimine toute manipulation manuelle de création d'AppImage après la release et garantit une disponibilité synchrone et cohérente de tous les livrables.

**Independent Test**: Déclencher le pipeline de release et vérifier qu'à la fin du processus, l'actif AppImage est attaché à la release GitHub aux côtés de l'APK, du paquet DEB, du MSI et du DMG sous le même tag de version.

**Acceptance Scenarios**:

1. **Given** le déclenchement d'un cycle de release automatisé, **When** le pipeline de construction Linux se termine, **Then** un fichier AppImage est compilé et intégré à la liste des actifs à publier.
2. **Given** la publication de release finalisée, **When** un utilisateur consulte les notes et fichiers de version, **Then** le fichier AppImage partage le même numéro de version et les mêmes notes de version que les autres plateformes.

---

### User Story 3 - Traçabilité, intégration au bureau et nommage explicite (Priority: P3)

Un utilisateur Linux souhaite que le fichier AppImage respecte une convention de nommage claire du projet, embarque l'icône officielle de l'application et intègre un fichier de métadonnées `.desktop` valide pour une intégration fluide avec son environnement de bureau (GNOME, KDE, XFCE) ou des utilitaires de gestion d'AppImage (comme AppImageLauncher).

**Why this priority**: Améliore la convivialité et l'aspect professionnel de l'application sur le bureau de l'utilisateur final en fournissant l'icône adéquate et les raccourcis système standard.

**Independent Test**: Inspecter les métadonnées embarquées dans l'AppImage et vérifier la présence de l'icône officielle et d'une entrée de bureau valide (`.desktop`).

**Acceptance Scenarios**:

1. **Given** le livrable AppImage généré, **When** il est intégré ou épinglé dans l'environnement de bureau de l'utilisateur, **Then** l'icône officielle de Droidplane et les informations applicatives (nom, catégorie productivité/bureautique) s'affichent correctement.

---

### Edge Cases

- **Compatibilité glibc et systèmes hôtes** : L'AppImage doit être construite sur un environnement Linux standard (comme `ubuntu-20.04` ou `ubuntu-22.04` / `ubuntu-latest`) garantissant une rétrocompatibilité maximale avec les bibliothèques C standard des distributions cibles.
- **Présence ou absence de libfuse2 / FUSE** : Certaines distributions récentes (ex. Ubuntu 22.04+) n'embarquent plus `libfuse2` par défaut pour les anciens runtimes AppImage ; l'outil de packaging doit utiliser un runtime AppImage moderne (type 2) compatible ou documenter l'exécution via `--appimage-extract-and-run` si FUSE n'est pas installé.
- **Convention de nommage et collisions** : Le nom du fichier doit être clair et distinct du paquet Debian (ex: `Droidplane-<version>-x86_64.AppImage` vs `droidplane_<version>_amd64.deb`).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Le système de release DOIT générer automatiquement un livrable distribuable universel au format **AppImage** pour les systèmes Linux 64-bit lors de chaque publication de version.
- **FR-002**: Le livrable AppImage DOIT être entièrement autonome en embarquant son propre runtime d'exécution (JRE optimisé) et ses dépendances sans prérequis d'installation Java sur la machine hôte.
- **FR-003**: Le livrable AppImage DOIT intégrer l'icône officielle de l'application, les métadonnées de lanceur `.desktop` et la catégorie bureautique/cartes mentales appropriée.
- **FR-004**: Le système DOIT publier le livrable AppImage simultanément au sein de la même release GitHub officielle que les autres formats (Android APK, Windows MSI, macOS DMG, Linux DEB).
- **FR-005**: Le livrable AppImage DOIT partager strictement le même numéro de version et le même journal des modifications (changelog) que l'ensemble des autres livrables de la release.
- **FR-006**: Le fichier AppImage DOIT suivre une convention de nommage claire intégrant le nom de l'application, le numéro de version et l'architecture (`Droidplane-<version>-x86_64.AppImage`).
- **FR-007**: La documentation du projet DOIT être mise à jour pour référencer la disponibilité du livrable AppImage et expliciter sa commande d'exécution sous Linux.

### Key Entities

- **Livrable universel Linux (AppImage Asset)** : Fichier exécutable autonome portable pour architecture x86_64, encapsulant les binaires compilés de Droidplane, le runtime Java minimal, les ressources graphiques et les métadonnées desktop.
- **Release (Publication de version)** : Entité officielle de publication GitHub regroupant tous les livrables de la version (APK, DEB, AppImage, MSI, DMG).

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100 % des releases publiées mettent à disposition un livrable AppImage valide aux côtés des formats DEB, MSI, DMG et APK sous le même tag de version.
- **SC-002**: Un utilisateur Linux peut exécuter l'application via l'AppImage en moins de 30 secondes après téléchargement sans requérir de droits administrateur (root).
- **SC-003**: 0 dépendance système ou paquet tiers spécifique requis pour lancer l'AppImage sur les distributions Linux 64-bit majeures.
- **SC-004**: Le processus automatisé de release génère et attache l'AppImage sans aucune intervention manuelle intermédiaire.

## Assumptions

- Le runner Linux utilisé dans GitHub Actions (`ubuntu-latest`) permet d'exécuter `appimagetool` ou un outil équivalent pour empaqueter le dossier distributable produit par Compose Desktop (`createDistributable`) sous forme d'AppImage.
- L'architecture cible principale pour l'AppImage est x86_64 (architecture dominante sur desktop Linux).
- Les scripts existants d'incrémentation de version et de publication GitHub Releases sont conservés et réutilisés.
