# Quickstart Validation Guide: Linux AppImage Packaging

**Feature**: `019-linux-appimage-release`  
**Date**: 2026-11-11  

Ce guide décrit les étapes concrètes et exécutables pour valider la génération et le fonctionnement du package universel AppImage pour Linux.

---

## 1. Prérequis

- **Java JDK 21** installé (`java -version`).
- Distribution Linux x86_64 avec accès à `bash`.
- Outil `appimagetool` installé localement ou connectivité pour son téléchargement automatique.

---

## 2. Scénarios de Validation

### Scénario 1 : Génération locale complète de l'AppImage
**Objectif** : Vérifier que `scripts/package-appimage.sh` génère un fichier `.AppImage` valide à partir de la sortie de `:app:createDistributable`.

**Commandes d'exécution** :
```bash
# 1. Compiler l'arborescence autonome
./gradlew :app:createDistributable --no-daemon

# 2. Exécuter le script d'empaquetage AppImage
chmod +x scripts/package-appimage.sh
./scripts/package-appimage.sh

# 3. Vérifier le fichier produit
ls -lh app/build/compose/binaries/main/appimage/Droidplane-*-x86_64.AppImage
```

**Résultat attendu** :
- Le script affiche `[OK] AppImage created successfully`.
- Le fichier `Droidplane-<version>-x86_64.AppImage` existe et a une taille d'environ 90-120 Mo (contenant le JRE 21 et l'application).

---

### Scénario 2 : Inspection des métadonnées de l'AppImage
**Objectif** : Vérifier que l'AppImage embarque les métadonnées requises (`AppRun`, `droidplane.desktop`, icône PNG).

**Commandes d'exécution** :
```bash
APPIMAGE=$(ls app/build/compose/binaries/main/appimage/Droidplane-*-x86_64.AppImage | head -n 1)

# Extraire le contenu de l'AppImage dans un dossier temporaire
$APPIMAGE --appimage-extract
ls -la squashfs-root/

# Vérifier la présence des fichiers clés
test -f squashfs-root/AppRun && echo "✓ AppRun present"
test -f squashfs-root/droidplane.desktop && echo "✓ Desktop entry present"
test -f squashfs-root/droidplane.png && echo "✓ Icon present"

# Nettoyer le dossier extrait
rm -rf squashfs-root
```

**Résultat attendu** :
- Tous les fichiers de métadonnées et l'exécutable `AppRun` sont présents à la racine de l'image.

---

### Scénario 3 : Validation de la conformité du Workflow GitHub Actions
**Objectif** : Valider que `.github/workflows/android_release.yml` inclut la génération de l'AppImage et la publication de l'actif `*.AppImage`.

**Commandes d'exécution** :
```bash
python3 -c "
with open('.github/workflows/android_release.yml') as f:
    content = f.read()
assert 'package-appimage.sh' in content, 'Missing package-appimage.sh execution'
assert '*.AppImage' in content, 'Missing *.AppImage asset filter in publish-release'
print('Workflow AppImage integration verified successfully!')
"
```

**Résultat attendu** :
- Validation réussie sans assertion levée.
