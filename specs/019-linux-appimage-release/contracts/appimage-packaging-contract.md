# AppImage Packaging Contract & Pipeline Integration

**Feature**: `019-linux-appimage-release`  
**Date**: 2026-11-11  

---

## 1. Contrat d'Interface du Script d'Empaquetage (`scripts/package-appimage.sh`)

### Signature & Arguments
```bash
scripts/package-appimage.sh [options]
```

### Options et Paramètres
| Option / Argument | Variable d'environnement équivalente | Valeur par défaut | Description |
|---|---|---|---|
| `--version <ver>` | `APP_VERSION` | Détectée depuis `app/build.gradle.kts` (`appVersionName`) | Version applicative à insérer dans le nom de fichier et les métadonnées. |
| `--dist-dir <dir>` | `DISTRIBUTABLE_DIR` | `app/build/compose/binaries/main/app/Droidplane` | Répertoire d'entrée produit par `:app:createDistributable`. |
| `--output-dir <dir>` | `OUTPUT_DIR` | `app/build/compose/binaries/main/appimage` | Répertoire de destination du fichier `.AppImage`. |
| `--appimagetool <path>` | `APPIMAGETOOL_PATH` | Recherche locale (`which appimagetool`) ou téléchargement auto | Chemin vers le binaire `appimagetool`. |
| `--help` | N/A | N/A | Affiche l'aide et les options disponibles. |

### Codes de retour
- `0` : AppImage créée avec succès dans `--output-dir`.
- `1` : Erreur (répertoire source introuvable, échec de génération ou outil manquant).

---

## 2. Contrat d'Intégration GitHub Actions (`.github/workflows/android_release.yml`)

### Mise à jour du Job `build-desktop` pour la plateforme Linux
Sur le runner `ubuntu-latest` :
```yaml
      - name: 📦 Compiler la distribution native (${{ matrix.platform }})
        shell: bash
        run: |
          if [ "${{ matrix.platform }}" = "linux" ]; then
            chmod +x ./gradlew scripts/package-appimage.sh
            ./gradlew :app:packageDeb :app:createDistributable --no-daemon
            ./scripts/package-appimage.sh --version "${{ needs.prepare-release.outputs.version_name }}"
          else
            if [ -f "./gradlew" ]; then
              chmod +x ./gradlew
              ./gradlew ${{ matrix.gradle_task }} --no-daemon
            else
              ./gradlew.bat ${{ matrix.gradle_task }} --no-daemon
            fi
          fi

      - name: 🏷️ Préparer et normaliser l'artefact Desktop (${{ matrix.platform }})
        shell: bash
        run: |
          mkdir -p dist-desktop
          if [ "${{ matrix.platform }}" = "linux" ]; then
            cp app/build/compose/binaries/main/deb/*.deb dist-desktop/
            cp app/build/compose/binaries/main/appimage/*.AppImage dist-desktop/
          else
            cp ${{ matrix.artifact_path }} dist-desktop/
          fi
          echo "### Desktop Artifact (${{ matrix.platform }})" >> $GITHUB_STEP_SUMMARY
          ls -lh dist-desktop/ >> $GITHUB_STEP_SUMMARY
```

### Mise à jour du Job `publish-release`
```yaml
      - name: 🗂️ Rassembler tous les actifs de distribution
        run: |
          mkdir -p release-assets
          find downloaded-artifacts -type f \( -name "*.apk" -o -name "*.deb" -o -name "*.AppImage" -o -name "*.msi" -o -name "*.dmg" \) -exec cp {} release-assets/ \;
          if [ -f downloaded-artifacts/release-changelog/CHANGELOG.md ]; then
            cp downloaded-artifacts/release-changelog/CHANGELOG.md ./CHANGELOG.md
          fi
          echo "### Publication Récapitulative des Actifs" >> $GITHUB_STEP_SUMMARY
          echo "Release tag : v${{ needs.prepare-release.outputs.version_name }}" >> $GITHUB_STEP_SUMMARY
          ls -lh release-assets/ | tee -a $GITHUB_STEP_SUMMARY
```
