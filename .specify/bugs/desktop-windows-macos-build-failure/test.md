# Bug Verification: Desktop Windows & macOS builds fail in CI

- **Slug**: desktop-windows-macos-build-failure
- **Tested**: 2026-11-11
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

Les deux mécanismes d'échec identifiés dans les logs CI sont neutralisés et reproductibles
localement : le daemon Gradle démarre avec le seul JDK 21 (plus de recherche de JDK 17), et
les 79 jars du `desktopRuntimeClasspath` se re-zippent sans erreur (le jar fautif déclenche
bien, lui, `duplicate entry: META-INF/versions/9/module-info.class`). En revanche, les
artefacts `.msi` (Windows) et `.dmg` (macOS) n'ont pas pu être produits ici : la validation
finale reste l'exécution réelle de la matrice GitHub Actions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction Windows (post-fix) | `JAVA_HOME=<jdk21> ./gradlew -Dorg.gradle.java.installations.auto-detect=false -Dorg.gradle.java.installations.auto-download=false help` | pass | Le daemon accepte JDK 21 sans détection ni téléchargement ; l'erreur `No defined toolchain download url for WINDOWS` ne peut plus se produire |
| Reproduction macOS (post-fix) | Re-zip Java de chaque jar du `desktopRuntimeClasspath` (équivalent de `MacJarSignFileCopyingProcessor`) | pass | `jars_rezipped_ok=79 failed=0` |
| Reproduction macOS (pré-fix, témoin) | Même re-zip sur `kotlinx-coroutines-debug-1.10.1.jar` (toujours présent sur le classpath de test) | pass (erreur reproduite) | `ZIPEXCEPTION: duplicate entry: META-INF/versions/9/module-info.class` — identique au log CI macOS |
| Workflow valide | `python3 -c "yaml.safe_load(...)"` + `grep uses:` | pass | YAML OK ; aucune référence à `android/setup-android` ; seules des actions existantes sont utilisées |
| New / updated tests | — | not-run | Aucun test automatisé ajouté par le fix (bug d'outillage CI) |
| Regression suite | `./gradlew testDebugUnitTest :core:desktopTest :data:desktopTest :app:desktopTest` | pass | `BUILD SUCCESSFUL in 41s` |
| Packaging Linux (référence) | `./gradlew :app:createDistributable :app:packageDeb` | pass | `.deb` 107 Mo produit ; `createDistributable` OK |
| Contenu de la distribution | `find …/app -name '*coroutines-debug*'` | pass | 0 jar `coroutines-debug` dans la distribution (80 jars dans `lib/app`) |
| Packaging Windows (`.msi`) | — | not-run | Runner Linux ; `packageMsi`/WiX indisponibles localement |
| Packaging macOS (`.dmg`) | — | not-run | Runner Linux ; `packageDmg`/jpackage macOS indisponible localement |
| Lint / type-check | — | skipped | Pas de tâche lint dans le workflow de release ; la compilation est couverte par les suites de tests |

## Output Excerpts

**Windows (critère daemon JVM) :**

```
BUILD SUCCESSFUL in 7s
1 actionable task: 1 executed
```

**macOS (témoin pré-fix — le jar fautif re-zippé tel que le fait Compose) :**

```
ZIPEXCEPTION: duplicate entry: META-INF/versions/9/module-info.class
exit=3
```

**macOS (post-fix — tout le runtime classpath re-zippé) :**

```
jars=79
jars_rezipped_ok=79 failed=0
```

**Suites de tests :**

```
BUILD SUCCESSFUL in 41s
85 actionable tasks: 58 executed, 6 from cache, 21 up-to-date
```

**Packaging Linux :**

```
The distribution is written to …/compose/binaries/main/deb/droidplane_1.0.40_amd64.deb
BUILD SUCCESSFUL in 14s
```

## Residual Risks

- La matrice CI n'a pas été exécutée : `windows-latest` et `macos-latest` restent les seuls
  environnements qui produisent réellement `.msi`/`.dmg`. Des particularités runner
  (WiX, codesign, architecture arm64 de `macos-latest`) ne sont pas couvertes localement.
- Les bibliothèques de test (Kotest, JNA…) fuient toujours dans la distribution desktop
  (11 jars constatés dans `lib/app`) ; elles ne contiennent pas d'entrée ZIP dupliquée, donc
  elles ne cassent pas l'empaquetage, mais elles gonflent l'artefact.
- L'exclusion cible un artefact précis ; un futur jar multi-release dupliqué dans le
  classpath reproduirait le même échec macOS.
- `actions/setup-java@v4` et les actions Node 20 restent dépréciées (avertissements CI,
  sans lien avec ce bug).

## Recommendation

Correctif cohérent et mécanismes d'échec vérifiés localement, mais l'environnement de
reproduction (builds Windows/macOS sur GitHub Actions) n'a pas pu être exercé : garder le
bug en `partial` et confirmer par un run CI après merge. Si les jobs `build-desktop`
(Windows et macOS) produisent leurs artefacts, clôturer ; sinon rouvrir avec les logs et
relancer `/speckit.bug.assess`.
