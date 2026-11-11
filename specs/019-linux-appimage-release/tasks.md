# Tasks — Feature 019 : Linux AppImage Release

> Dernière mise à jour : 2026-11-11
> Statut global : ✅ COMPLÉTÉ

---

## Phase 1 — Setup & validation de l'environnement

- [X] T001 [P0] [US0] Vérifier que `./gradlew :app:createDistributable` produit `app/build/compose/binaries/main/app/Droidplane/bin/Droidplane`
- [X] T002 [P0] [US0] Confirmer la disponibilité d'`appimagetool` en local (`/usr/local/bin/appimagetool`) et valider le flag `--appimage-extract-and-run` (contournement FUSE)

---

## Phase 2 — Création du script de packaging AppImage

- [X] T010 [P1] [US1] Créer `scripts/package-appimage.sh` avec parsing des options `--version`, `--dist-dir`, `--output-dir`, `--appimagetool`
- [X] T011 [P1] [US1] Implémenter l'extraction automatique de version depuis `app/build.gradle.kts` via `awk` (fallback si `--version` absent)
- [X] T012 [P1] [US1] Implémenter la construction de l'AppDir : `AppRun`, `droidplane.desktop`, `droidplane.png`, `.DirIcon` symlink, `bin/` et `lib/` depuis le distributable
- [X] T013 [P1] [US1] Implémenter la détection / téléchargement automatique d'`appimagetool` (priorités : `--appimagetool` > `/usr/local/bin` > PATH > curl download)
- [X] T014 [P1] [US1] Implémenter l'exécution d'`appimagetool` avec `APPIMAGE_EXTRACT_AND_RUN=1` + `--appimage-extract-and-run` pour environnements sans FUSE (CI/CD)
- [X] T015 [P1] [US1] Corriger les catégories `.desktop` : `Office;FlowChart;X-MindMapping;` (supprimer la valeur non-XDG `MindMapping`)
- [X] T016 [P1] [US1] Rendre le script exécutable (`chmod +x scripts/package-appimage.sh`)

---

## Phase 3 — Validation locale de la génération AppImage

- [X] T020 [P1] [US1] Exécuter `./scripts/package-appimage.sh` localement et valider la création de `app/build/compose/binaries/main/appimage/Droidplane-1.0.39-x86_64.AppImage` (132 Mo)

---

## Phase 4 — Mise à jour du workflow GitHub Actions

- [X] T030 [P2] [US2] Mettre à jour la matrice Linux dans `build-desktop` : `gradle_task` → `:app:packageDeb :app:createDistributable`
- [X] T031 [P2] [US2] Modifier l'étape "Compiler la distribution native" pour Linux : appel de `scripts/package-appimage.sh --version ${{ needs.prepare-release.outputs.version_name }}` après les tâches Gradle
- [X] T032 [P2] [US2] Modifier l'étape "Préparer l'artefact Desktop" pour Linux : copier `.deb` ET `.AppImage` dans `dist-desktop/`
- [X] T033 [P2] [US2] Mettre à jour le `find` dans `publish-release` pour inclure `*.AppImage` aux côtés des autres formats
- [X] T034 [P2] [US2] Valider le YAML mis à jour via `python3 -c "import yaml; yaml.safe_load(...)"` (4 jobs présents, matrice Linux correcte)

---

## Phase 5 — Documentation

- [X] T040 [P3] [US3] Mettre à jour `README.md` : ajouter l'entrée AppImage dans "Supported Platforms & Downloads" avec instruction `chmod +x`

---

## Phase 6 — Commit final

- [X] T050 [P0] [US0] Committer tous les fichiers de la feature 019 : `scripts/package-appimage.sh`, `.github/workflows/android_release.yml`, `README.md`, `specs/019-linux-appimage-release/`

---

## Récapitulatif des artefacts créés / modifiés

| Fichier | Opération | Description |
|---|---|---|
| `scripts/package-appimage.sh` | CRÉÉ | Script bash complet de packaging AppImage |
| `.github/workflows/android_release.yml` | MODIFIÉ | Linux : Deb + AppImage, publish inclut `*.AppImage` |
| `README.md` | MODIFIÉ | Ajout de l'entrée Linux Universal (AppImage) |
| `specs/019-linux-appimage-release/tasks.md` | CRÉÉ | Ce fichier |
