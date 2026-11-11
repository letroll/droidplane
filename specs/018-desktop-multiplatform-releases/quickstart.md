# Quickstart Validation Guide: Desktop Multiplatform Releases

**Feature**: `018-desktop-multiplatform-releases`  
**Date**: 2026-11-11  

Ce guide présente les scénarios exécutables de validation permettant de prouver de bout en bout le bon fonctionnement de la génération et publication des releases multiplateformes.

---

## 1. Prérequis

- **Java JDK 21** installé (`java -version`).
- Accès aux commandes de packaging Gradle (`./gradlew`).
- Outils de packaging locaux selon l'OS testé :
  - **Linux** : `dpkg` / `fakeroot` (standard sur Debian/Ubuntu) pour le paquet `.deb`.
  - **Windows** : WiX Toolset v3 (standard sur GitHub Actions `windows-latest`) pour `.msi`.
  - **macOS** : `hdiutil` (standard sur macOS et GitHub Actions `macos-latest`) pour `.dmg`.

---

## 2. Scénarios de Validation

### Scénario 1 : Validation locale de la configuration de distribution Desktop
**Objectif** : Valider que le DSL Compose `nativeDistributions` est syntaxiquement valide et produit le livrable attendu pour la plateforme hôte courante.

**Commande d'exécution** :
```bash
./gradlew :app:packageDistributionForCurrentOS
```
*(Ou sous Linux : `./gradlew :app:packageDeb`)*

**Résultat attendu** :
- La compilation s'exécute avec succès (`BUILD SUCCESSFUL`).
- Un binaire autonome est généré sous `app/build/compose/binaries/main/` dans le sous-dossier correspondant au format de l'OS (`deb/`, `msi/` ou `dmg/`).
- Le nom de fichier respecte la convention `Droidplane-<version>.<ext>`.

---

### Scénario 2 : Validation locale de l'incrémentation de version unifiée
**Objectif** : Vérifier que `scripts/bump-version.sh` met à jour simultanément la version consommée par Android et Compose Desktop.

**Commandes d'exécution** :
```bash
# Vérifier la version actuelle dans app/build.gradle.kts
grep -E 'val appVersion' app/build.gradle.kts

# Tester le script de bump
./scripts/bump-version.sh

# Vérifier que le numéro de code et le versionName ont été incrémentés
grep -E 'val appVersion' app/build.gradle.kts
```

**Résultat attendu** :
- `appVersionCode` incrémenté de 1.
- `appVersionName` mis à jour sous la forme `1.0.<appVersionCode>`.
- Le package Desktop hérite directement de ce `appVersionName`.

---

### Scénario 3 : Validation du contrat de workflow GitHub Actions
**Objectif** : Vérifier la syntaxe du fichier `.github/workflows/android_release.yml` et l'exactitude des définitions des jobs, dépendances et artefacts.

**Commandes d'exécution** :
```bash
# Vérifier la validité YAML et les dépendances du workflow
python3 -c "
import yaml
with open('.github/workflows/android_release.yml') as f:
    wf = yaml.safe_load(f)
assert 'prepare-release' in wf['jobs'] or 'build-and-release' in wf['jobs']
print('Workflow syntax and jobs valid.')
"
```

**Résultat attendu** :
- Validation sans erreur de syntaxe.
- Matrice `build-desktop` couvrant `ubuntu-latest`, `windows-latest` et `macos-latest`.
- Job de publication finale téléchargeant tous les actifs (`apk`, `deb`, `msi`, `dmg`).

---

### Scénario 4 : Validation de bout en bout en intégration continue
**Objectif** : Constater la création d'une release GitHub complète lors d'un push sur `master`.

**Procédure** :
1. Pousser les modifications sur la branche `master`.
2. Observer l'exécution du workflow GitHub Actions dans l'onglet **Actions**.
3. Une fois les jobs terminés, naviguer vers la page des **Releases** du dépôt.

**Résultat attendu** :
- Une release unique est créée sous l'étiquette `v1.0.X`.
- Le corps de la release contient le journal des modifications généré.
- La section **Assets** liste :
  - L'application Android (`.apk`)
  - L'installateur Windows (`.msi`)
  - Le paquet Linux (`.deb`)
  - L'image macOS (`.dmg`)
