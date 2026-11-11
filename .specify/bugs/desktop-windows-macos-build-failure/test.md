# Bug Verification: Desktop Windows & macOS builds fail in CI

- **Slug**: desktop-windows-macos-build-failure
- **Tested**: 2026-11-11
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Le correctif a été validé de bout en bout dans l'environnement de reproduction réel : le run
GitHub Actions `43035393091` (commit `b31de48`, incluant le correctif) est **vert** sur les
trois runners, et la release `v1.0.42` publie bien `Droidplane-1.0.42.msi` et
`Droidplane-1.0.42.dmg`. Les mécanismes d'échec sont également neutralisés localement
(daemon Gradle en JDK 21, plus aucun jar à entrée ZIP dupliquée). Aucune régression détectée.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix, environnement réel) | `gh run view 43035393091 --repo letroll/droidplane` | pass | `build-desktop` **windows-latest** ✓ (4m15s), **macos-latest** ✓ (3m53s), **ubuntu-latest** ✓ (3m22s), `publish-release` ✓ |
| Artefacts publiés | `gh release view v1.0.42 --repo letroll/droidplane` | pass | `.msi`, `.dmg`, `.deb`, `.AppImage`, `.apk` présents |
| Mécanisme Windows (proxy local) | `JAVA_HOME=<jdk21> ./gradlew -Dorg.gradle.java.installations.auto-detect=false -Dorg.gradle.java.installations.auto-download=false help` | pass | Daemon démarré sans recherche de JDK 17 ; `toolchainVersion=21` |
| Mécanisme macOS (proxy local) | Re-zip Java de chaque jar du `desktopRuntimeClasspath` (équivalent `MacJarSignFileCopyingProcessor`) | pass | `jars_rezipped_ok=79 failed=0`, `coroutines-debug` absent |
| Témoin pré-fix macOS | Re-zip de `kotlinx-coroutines-debug-1.10.1.jar` | pass (erreur reproduite) | `duplicate entry: META-INF/versions/9/module-info.class` — identique au log CI initial |
| Workflow valide | `python3` + `yaml.safe_load` + `grep uses:` | pass | 4 jobs, 6 étapes dans `build-desktop`, aucune référence à `android/setup-android` |
| New / updated tests | — | not-run | Aucun test automatisé ajouté (bug d'outillage CI) |
| Regression suite | `./gradlew testDebugUnitTest :core:desktopTest :data:desktopTest :app:desktopTest` | pass | `BUILD SUCCESSFUL` (85 tâches) |
| Packaging local (référence Linux) | `./gradlew :app:createDistributable :app:packageDeb` | pass | `.deb` produit, `createDistributable` OK |
| Lint / type-check | — | skipped | Pas de tâche lint dans le workflow de release ; compilation couverte par les tests |

## Output Excerpts

**Run CI post-fix (environnement de reproduction réel) :**

```
✓ prepare-release in 9s
✓ build-desktop (ubuntu-latest, linux, …) in 3m22s
✓ build-desktop (windows-latest, windows, …) in 4m15s
✓ build-android in 6m49s
✓ build-desktop (macos-latest, macos, …) in 3m53s
✓ publish-release in 19s
```

**Release v1.0.42 :**

```
asset: Droidplane-1.0.42-x86_64.AppImage
asset: droidplane-1.0.42.apk
asset: Droidplane-1.0.42.dmg
asset: Droidplane-1.0.42.msi
asset: droidplane_1.0.42_amd64.deb
```

**Témoin pré-fix / proxy post-fix :**

```
ZIPEXCEPTION: duplicate entry: META-INF/versions/9/module-info.class   (jar fautif)
jars=79
jars_rezipped_ok=79 failed=0                                            (classpath après correctif)
```

## Residual Risks

- Les bibliothèques de test (Kotest, JNA…) sont toujours embarquées dans la distribution
  desktop (≈11 jars) ; elles ne cassent pas l'empaquetage mais gonflent l'artefact
  (cf. follow-up « déplacer `KStringSpec` vers `testFixtures` »).
- L'exclusion cible un artefact précis : un futur jar multi-release dupliqué dans le
  classpath reproduirait le même échec macOS.
- Avertissements de dépréciation CI (`actions/setup-java@v4`, actions Node 20) sans lien
  avec ce bug.

## Recommendation

Clôturer le bug — vérifié de bout en bout : les jobs Windows et macOS du workflow de release
passent et les installeurs `.msi`/`.dmg` sont publiés. Le follow-up de nettoyage des
dépendances de test peut être traité séparément.
