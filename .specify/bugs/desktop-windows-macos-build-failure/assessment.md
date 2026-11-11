# Bug Assessment: Desktop Windows & macOS builds fail in CI

- **Slug**: desktop-windows-macos-build-failure
- **Created**: 2026-11-11
- **Source**: pasted text — "les builds pour windows et mac semblent échouer"
- **Verdict**: valid
- **Severity**: high

---

## Report (verbatim ou résumé)

> "les builds pour windows et mac semble échoué corrige cela"

Le job `build-desktop` de la matrice GitHub Actions échoue pour les runners `windows-latest` et `macos-latest`, alors que le runner `ubuntu-latest` (Linux) réussit.

---

## Symptom

Les builds Desktop Windows (`.msi`) et macOS (`.dmg`) échouent en CI, empêchant la publication complète de la release. Plusieurs causes racines coexistent dans le workflow actuel.

---

## Reproduction

1. Pousser sur `master` pour déclencher `.github/workflows/android_release.yml`
2. Observer les jobs `build-desktop (windows-latest)` et `build-desktop (macos-latest)` → ils échouent
3. Le job `build-desktop (ubuntu-latest)` réussit lui

---

## Suspected Code Paths

- `.github/workflows/android_release.yml` lignes 198–210 — étape "Compiler la distribution native"
- `.github/workflows/android_release.yml` lignes 162–181 — définition de la matrice
- `app/build.gradle.kts` lignes 12–60 — bloc `android {}` avec `compileSdk` / `minSdk`

---

## Root Cause Hypothesis

**Confidence : high** — Trois causes distinctes ont été identifiées par analyse statique :

### Cause 1 — `shell: bash` + `chmod +x gradlew` échoue sur Windows (CRITIQUE)
L'étape "Compiler la distribution native" utilise `shell: bash` et tente `chmod +x ./gradlew`. Sur `windows-latest`, Git Bash est disponible mais `chmod +x` ne rend pas `gradlew` (fichier Unix) exécutable dans le sens Windows. De plus, Windows doit appeler `gradlew.bat` (ou `./gradlew` via Git Bash fonctionne *si* le fichier est marqué exécutable dans Git — ce qui n'est pas garanti sans `git update-index`). La logique `if [ -f "./gradlew" ]` est toujours vraie sous Git Bash Windows, elle ne bascule jamais vers `gradlew.bat`, ce qui peut provoquer un échec selon les permissions.

**La vraie cause** : le `chmod +x ./gradlew` fait partie d'une condition `if [ -f "./gradlew" ]` mais aucune étape dédiée de chmod n'existe dans le job `build-desktop` pour Windows/macOS (contrairement au job `build-android` qui a `run: chmod +x gradlew` en étape séparée). Sur Windows Git Bash, `chmod` est un no-op silencieux, mais l'absence d'une étape dédiée `chmod +x gradlew` *avant* le build est une fragilité.

### Cause 2 — Le projet `app` applique le plugin `android` → nécessite Android SDK (CRITIQUE)
`app/build.gradle.kts` applique `alias(libs.plugins.android.application)` (AGP). Ce plugin recherche le SDK Android via `local.properties` (`sdk.dir`) ou la variable d'env `ANDROID_HOME`/`ANDROID_SDK_ROOT`. Or :
- `local.properties` est dans `.gitignore` → absent sur les runners CI
- **Le job `build-desktop` ne configure pas l'Android SDK** (pas d'action `android/setup-android@v3` ni de variable `ANDROID_HOME`)
- Sur `ubuntu-latest` les runners GitHub ont le SDK Android préinstallé → ça marche fortuitement
- Sur `windows-latest` et `macos-latest` les runners GitHub **ont aussi le SDK Android** (pré-installé), MAIS il est possible que le chemin diffère ou que certaines versions de SDK manquent → fragile et non garanti

La solution robuste : ajouter `uses: android/setup-android@v3` dans le job `build-desktop` pour garantir la disponibilité de l'Android SDK sur toutes les plateformes, ou mieux, générer `local.properties` dynamiquement depuis `$ANDROID_HOME`.

### Cause 3 — `packageVersion` format non compatible MSI (MOYEN)
Compose Desktop / WiX (outil de packaging MSI sur Windows) exige que `packageVersion` suive le format **`major.minor.patch`** avec des entiers positifs, sans composante supplémentaire. La valeur actuelle `1.0.40` est valide. Cependant si `appVersionName` devenait un format non-numérique ou comportait un suffixe (ex. `1.0.40-SNAPSHOT`), WiX échouerait. À surveiller.

### Cause 4 — `packageVersion` sur macOS (MOYEN)
La valeur `1.0.40` est valide pour `packageDmg`. Le `bundleID` est présent. Pas de problème direct ici, mais l'absence du SDK Android (Cause 2) touche aussi macOS.

---

## Proposed Remediation

**Préféré** : Ajouter une étape `android/setup-android@v3` dans le job `build-desktop` pour garantir la présence de l'Android SDK sur tous les runners, et ajouter une étape `chmod +x gradlew` dédiée en bash.

**Détail des changements** dans `.github/workflows/android_release.yml` :

1. Ajouter après le checkout dans `build-desktop` :
   ```yaml
   - name: 🤖 Configurer l'Android SDK
     uses: android/setup-android@v3
   ```

2. Ajouter une étape dédiée de chmod (fonctionnelle sur Linux/macOS via bash, no-op safe sur Windows) :
   ```yaml
   - name: 🔑 Rendre Gradle exécutable
     shell: bash
     run: chmod +x gradlew
   ```

3. Simplifier l'étape de compilation (supprimer la logique `chmod` inline, elle est déjà gérée par l'étape dédiée) :
   ```yaml
   - name: 📦 Compiler la distribution native
     shell: bash
     run: |
       if [ "${{ matrix.platform }}" = "linux" ]; then
         chmod +x scripts/package-appimage.sh
         ./gradlew ${{ matrix.gradle_task }} --no-daemon
         ./scripts/package-appimage.sh --version "${{ needs.prepare-release.outputs.version_name }}"
       else
         ./gradlew ${{ matrix.gradle_task }} --no-daemon
       fi
   ```

**Files likely to change**:
- `.github/workflows/android_release.yml`

**Tests to add or update**:
- Vérifier en CI que les 3 runners (linux, windows, macos) produisent leurs artefacts avant de fusionner

---

## Risks & Considerations

- `android/setup-android@v3` ajoute ~1–2 min au build desktop sur chaque runner (téléchargement SDK si absent)
- Sur les runners GitHub hébergés, l'Android SDK est généralement pré-installé mais la version peut varier — l'action garantit la bonne version
- Si le projet évolue pour découpler totalement le module `app` du plugin Android sur desktop (via un module KMP dédié), la dépendance au SDK Android disparaîtrait pour les builds desktop → à considérer en refactoring futur
- `packageVersion = appVersionName` (`1.0.40`) est compatible WiX MSI et Apple DMG

## Open Questions

- Aucune — les causes sont identifiées par analyse statique du workflow et du `build.gradle.kts`
