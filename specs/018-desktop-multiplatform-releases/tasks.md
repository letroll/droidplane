# Tasks: Génération simultanée des distributions desktop (Windows, Linux, macOS) lors de la publication d'une release

**Input**: Design documents from `specs/018-desktop-multiplatform-releases/`  
**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/release-pipeline-contract.md](contracts/release-pipeline-contract.md), [quickstart.md](quickstart.md)

**Tests**: Tests automatisés du projet (`testDebugUnitTest`, `:core:desktopTest`, `:data:desktopTest`, `:app:desktopTest`) intégrés comme prérequis obligatoire avant packaging et release (Constitution Principe III).

**Organization**: Les tâches sont regroupées par histoire utilisateur (`US1`–`US3`) pour permettre une implémentation et une vérification indépendantes de chaque incrément.

## Format: `[ID] [P?] [Story] Description`

- **[P]** : Tâche parallélisable (fichiers distincts, pas de dépendance bloquante sur des tâches incomplètes)
- **[Story]** : Histoire utilisateur d'appartenance (`[US1]`–`[US3]`)
- Chemins de fichiers explicites inclus dans chaque description de tâche

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Vérification de l'environnement de développement et de la chaîne de build existante

- [X] T001 Vérifier l'environnement de compilation et les versions d'outils (Java 21, Gradle, AGP, Compose Multiplatform 1.7.3) via `./gradlew --version`
- [X] T002 [P] Vérifier la validité et le bon fonctionnement du script d'incrémentation de version dans `scripts/bump-version.sh`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Configuration du DSL de distribution native Compose Desktop indispensable avant tout packaging

**⚠️ CRITICAL**: Cette phase doit être complétée avant les phases de déploiement et d'orchestration CI/CD

- [X] T003 [P] Configurer les imports `org.jetbrains.compose.desktop.application.dsl.TargetFormat` nécessaires au packaging natif dans `app/build.gradle.kts`
- [X] T004 Déclarer le bloc `nativeDistributions` dans `compose.desktop.application` de `app/build.gradle.kts` avec `packageName = "Droidplane"`, `packageVersion = appVersionName`, `description = "Droidplane Mind Map Application"`, `vendor = "Droidplane"`, `copyright = "© 2026 Julien Quiévreux"` et les formats cibles `TargetFormat.Deb`, `TargetFormat.Msi`, `TargetFormat.Dmg`
- [X] T005 [P] Déclarer les configurations spécifiques aux systèmes d'exploitation dans `compose.desktop.application.nativeDistributions` de `app/build.gradle.kts` (`macOS { bundleID = "fr.julien.quievreux.droidplane2" }`, `windows { menuGroup = "Droidplane"; upgradeUuid = "a4c28f6d-74d1-4cb5-8292-1e96a40a5a22" }`, `linux {}`)

**Checkpoint**: Le modèle de distribution native Compose Desktop est déclaré et prêt pour le packaging multi-OS.

---

## Phase 3: User Story 1 - Téléchargement et utilisation directe sur les plateformes de bureau (Priority: P1) 🎯 MVP

**Goal**: Produire localement et de manière reproductible les packages distribuables pour Linux, Windows et macOS

**Independent Test**: Exécuter `./gradlew :app:packageDistributionForCurrentOS` (ou `./gradlew :app:packageDeb` sous Linux) et vérifier que le paquet binaire est généré sous `app/build/compose/binaries/main/`

### Implementation for User Story 1

- [X] T006 [US1] Valider et configurer la cible de packaging Linux Debian (`:app:packageDeb`) générant `app/build/compose/binaries/main/deb/Droidplane-*.deb` dans `app/build.gradle.kts`
- [X] T007 [P] [US1] Configurer et documenter la conformité des cibles de packaging Windows (`:app:packageMsi` vers `app/build/compose/binaries/main/msi/Droidplane-*.msi`) et macOS (`:app:packageDmg` vers `app/build/compose/binaries/main/dmg/Droidplane-*.dmg`) dans `specs/018-desktop-multiplatform-releases/quickstart.md`
- [X] T008 [US1] S'assurer que la classe principale `fr.julien.quievreux.droidplane2.MainKt` dans `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/Main.kt` est correctement invoquée par les exécutables empaquetés sans dépendance externe manquante

**Checkpoint**: User Story 1 complète — les installeurs et paquets pour Windows, Linux et macOS peuvent être générés via les tâches Compose Desktop correspondantes.

---

## Phase 4: User Story 2 - Publication automatisée et synchronisée de l'ensemble des plateformes (Priority: P2)

**Goal**: Automatiser dans le workflow GitHub Actions la génération simultanée des livrables Android, Windows, Linux et macOS et leur publication unifiée dans une seule release GitHub

**Independent Test**: Simuler ou déclencher le pipeline GitHub Actions sur master et vérifier que l'ensemble des 4 livrables binaires (`.apk`, `.deb`, `.msi`, `.dmg`) sont attachés à la même release avec le changelog synchronisé

### Implementation for User Story 2

- [X] T009 [US2] Restructurer le job initial de `.github/workflows/android_release.yml` en un job `prepare-release` sur `ubuntu-latest` exécutant `scripts/bump-version.sh`, générant `CHANGELOG.md` avec `git-cliff`, committant avec `[skip ci]`, poussant le tag Git `v${APP_VERSION_NAME}` et exposant `version_name` en sortie de job (`outputs.version_name`)
- [X] T010 [P] [US2] Définir le job `build-android` dans `.github/workflows/android_release.yml` dépendant de `prepare-release` sur `ubuntu-latest`, restaurant le Keystore, exécutant les tests (`./gradlew testDebugUnitTest :core:desktopTest :data:desktopTest :app:desktopTest`), compilant l'APK (`./gradlew assembleRelease --no-daemon`) et téléversant l'APK via `actions/upload-artifact@v4` sous le nom d'artefact `release-android`
- [X] T011 [US2] Définir le job matriciel `build-desktop` dans `.github/workflows/android_release.yml` dépendant de `prepare-release` avec une stratégie multi-OS (`ubuntu-latest` pour `:app:packageDeb`, `windows-latest` pour `:app:packageMsi`, `macos-latest` pour `:app:packageDmg`), compilant chaque format et téléversant les artefacts sous `release-desktop-linux`, `release-desktop-windows`, `release-desktop-macos` via `actions/upload-artifact@v4`
- [X] T012 [US2] Définir le job `publish-release` dans `.github/workflows/android_release.yml` dépendant de `[prepare-release, build-android, build-desktop]` sur `ubuntu-latest`, téléchargeant l'ensemble des artefacts via `actions/download-artifact@v4` et publiant la release unique via `softprops/action-gh-release@v2` avec le tag `v${{ needs.prepare-release.outputs.version_name }}`, le corps `CHANGELOG.md` et tous les fichiers attachés

**Checkpoint**: User Story 2 complète — la release GitHub est générée de manière atomique avec les distributions pour toutes les plateformes.

---

## Phase 5: User Story 3 - Traçabilité et identification claire des livrables par plateforme (Priority: P3)

**Goal**: Garantir un nommage standardisé sans collision, l'isolation des erreurs de build par plateforme et des résumés d'exécution transparents

**Independent Test**: Vérifier les artefacts générés sur GitHub Releases : chaque fichier doit porter un nom normalisé explicite indiquant la version et le système d'exploitation cible

### Implementation for User Story 3

- [X] T013 [US3] Ajouter des étapes de contrôle et de renommage normalisé des artefacts dans `.github/workflows/android_release.yml` avant téléversement pour garantir des noms transparents (`Droidplane-<version>.deb`, `Droidplane-<version>.msi`, `Droidplane-<version>.dmg`, `droidplane-<version>.apk`)
- [X] T014 [P] [US3] Configurer `fail-fast: false` dans la section `strategy.matrix` du job `build-desktop` dans `.github/workflows/android_release.yml` afin qu'un éventuel incident sur un système d'exploitation n'interrompe pas le diagnostic des autres plateformes
- [X] T015 [US3] Ajouter des étapes de génération de résumé d'exécution GitHub Actions via `$GITHUB_STEP_SUMMARY` dans `.github/workflows/android_release.yml` pour tracer la taille, le nom et le checksum de chaque livrable produit

**Checkpoint**: User Story 3 complète — traçabilité maximale et reporting détaillé pour chaque plateforme.

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Nettoyage, ajustement des documentations et validation d'intégrité

- [X] T016 [P] Mettre à jour le nom du workflow dans `.github/workflows/android_release.yml` en `CI/CD Release (Android & Desktop Multiplatform)`
- [X] T017 [P] Documenter dans `README.md` la disponibilité des installateurs natifs pour Windows, Linux et macOS dans la section des versions publiées
- [X] T018 Valider la conformité YAML et la cohérence de l'ensemble du workflow via le scénario de test automatisé décrit dans `specs/018-desktop-multiplatform-releases/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)** : Aucune dépendance — exécution immédiate.
- **Foundational (Phase 2)** : Dépend de la Phase 1 — BLOQUE l'implémentation des histoires utilisateur.
- **User Story 1 (Phase 3)** : Dépend de la Phase 2 — Déclaration et validation des tâches de packaging Compose Desktop.
- **User Story 2 (Phase 4)** : Dépend de la Phase 3 — Intégration du packaging dans la chaîne CI/CD GitHub Actions multi-OS.
- **User Story 3 (Phase 5)** : Dépend de la Phase 4 — Normalisation des noms, résumés d'exécution et tolérance aux pannes (`fail-fast: false`).
- **Polish (Phase 6)** : Dépend de l'achèvement des phases 3, 4 et 5.

### User Story Dependencies

- **User Story 1 (P1)** : Autonome après la Phase 2.
- **User Story 2 (P2)** : Nécessite la configuration Gradle validée en US1 pour brancher les tâches dans les jobs GitHub Actions.
- **User Story 3 (P3)** : Enrichit le workflow GitHub Actions défini en US2.

### Parallel Opportunities

- T002 et T001 en Phase 1 peuvent s'exécuter en parallèle.
- T003 et T005 en Phase 2 peuvent être préparés en parallèle de T004.
- T007 et T006 en Phase 3 peuvent être configurés en parallèle.
- T010 (`build-android`) et T011 (`build-desktop`) en Phase 4 sont exécutés en parallèle par GitHub Actions.
- T014 en Phase 5 peut être configuré en parallèle de T013.
- T016 et T017 en Phase 6 peuvent s'exécuter en parallèle.

---

## Parallel Example: User Story 1

```bash
# Vérifier la compilation Desktop KMP :
Task: "T006 [US1] Valider et configurer la cible de packaging Linux Debian (:app:packageDeb) dans app/build.gradle.kts"

# En parallèle, documenter et vérifier les cibles Windows et macOS :
Task: "T007 [P] [US1] Configurer et documenter la conformité des cibles de packaging Windows et macOS dans specs/018-desktop-multiplatform-releases/quickstart.md"
```

---

## Implementation Strategy

### MVP First (User Story 1 & Configuration Gradle)

1. Compléter Phase 1 (Setup) et Phase 2 (Foundational : déclaration de `nativeDistributions` dans `app/build.gradle.kts`).
2. Compléter Phase 3 (User Story 1 : validation locale de la génération de distribution desktop).
3. **STOP et VALIDATION** : Vérifier que `./gradlew :app:packageDistributionForCurrentOS` produit un paquet natif valide sans erreur.

### Incremental Delivery (CI/CD Multiplatform)

1. Après validation de l'US1, implémenter Phase 4 (User Story 2 : orchestration modulaire GitHub Actions).
2. Ajouter Phase 5 (User Story 3 : traçabilité, résumés `$GITHUB_STEP_SUMMARY` et `fail-fast: false`).
3. Appliquer Phase 6 (Polish : documentation README et validation globale).
