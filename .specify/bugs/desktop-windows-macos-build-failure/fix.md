# Bug Fix: Desktop Windows & macOS builds fail in CI

- **Slug**: desktop-windows-macos-build-failure
- **Fixed**: 2026-11-11
- **Assessment**: ./assessment.md
- **Status**: applied

> Remplace le rapport précédent, qui reposait sur une action GitHub inexistante
> (`android/setup-android@v3`) et ne traitait pas les erreurs réellement observées
> dans les logs CI (voir **Deviations from Assessment**).

## Summary

Les échecs Windows et macOS avaient deux causes distinctes, confirmées par les logs
des jobs `build-desktop` du run GitHub Actions `43024312495` : le daemon Gradle exigeait
un JDK 17 introuvable/indisponible en téléchargement sur Windows, et l'empaquetage macOS
échouait sur une entrée ZIP dupliquée dans un jar de test qui fuitait dans le classpath
de production. Le fichier de critères du daemon est aligné sur JDK 21 (le JDK installé en
CI et requis par le projet) et l'artefact fautif est exclu du classpath d'exécution.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `gradle/gradle-daemon-jvm.properties` | modifié | `toolchainVersion=17` → `21` : supprime la recherche/téléchargement d'un JDK 17 sur les runners |
| `app/build.gradle.kts` | modifié | Exclusion de `org.jetbrains.kotlinx:kotlinx-coroutines-debug` de `desktopRuntimeClasspath` et `releaseRuntimeClasspath` |
| `.github/workflows/android_release.yml` | modifié | Étape dédiée `chmod +x gradlew` (bash) ; suppression du `chmod` inline fragile ; pas d'action `setup-android` (SDK déjà fourni par les runners) |

## Diff Highlights

**Windows — daemon Gradle (cause de l'échec `Unable to download toolchain … 17`)** :

```properties
# gradle/gradle-daemon-jvm.properties
-toolchainVersion=17
+toolchainVersion=21
```

**macOS — exclusion du jar contenant `META-INF/versions/9/module-info.class` en double** :

```kotlin
// app/build.gradle.kts
configurations.matching {
    it.name == "desktopRuntimeClasspath" || it.name == "releaseRuntimeClasspath"
}.configureEach {
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-debug")
}
```

**Workflow — étape `chmod` explicite, sans action non résolvable** :

```yaml
- name: 🔑 Rendre Gradle exécutable
  shell: bash
  run: chmod +x gradlew

- name: 📦 Compiler la distribution native (${{ matrix.platform }})
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

## Tests Added or Updated

- Aucun test automatisé ajouté (bug d'outillage CI). La non-régression est vérifiée par :
  - les jobs `build-desktop` de la matrice GitHub Actions (`windows-latest`, `macos-latest`, `ubuntu-latest`) ;
  - la vérification locale décrite ci-dessous, qui reproduit les deux mécanismes en échec
    (résolution du daemon JVM sans JDK 17, et unicité des entrées ZIP des 79 jars du
    `desktopRuntimeClasspath`).

## Local Verification

- `python3 -c "import yaml; yaml.safe_load(...)"` → **OK** : 4 jobs, `build-desktop` expose 6 étapes (checkout, JDK, chmod, build, staging, upload).
- `./gradlew --stop` puis `JAVA_HOME=<jdk21> ./gradlew -Dorg.gradle.java.installations.auto-detect=false -Dorg.gradle.java.installations.auto-download=false help` → **BUILD SUCCESSFUL** : le daemon démarre avec le seul JDK 21, sans détection ni téléchargement d'un autre JDK.
- `./gradlew :app:dependencies --configuration desktopRuntimeClasspath | grep kotlinx-coroutines-debug` → **0 occurrence** ; même commande sur `desktopTestRuntimeClasspath` → **2 occurrences** (les tests conservent l'artefact).
- Scan des 79 jars du `desktopRuntimeClasspath` → **aucune entrée ZIP dupliquée** ; `skiko-awt-runtime-macos-arm64-0.8.18.jar` (variante macOS) vérifié séparément → **aucun doublon**.
- `./gradlew :core:desktopTest :data:desktopTest :app:desktopTest` → **BUILD SUCCESSFUL**.
- `./gradlew :app:createDistributable` → **BUILD SUCCESSFUL** (distribution écrite dans `app/build/compose/binaries/main/app`).

## Deviations from Assessment

1. **Action `android/setup-android@v3` retirée au lieu d'être ajoutée.** L'assessment proposait
   `android/setup-android@v3`, qui n'existe pas (l'action officielle est
   `android-actions/setup-android@v3`) et faisait échouer la résolution du workflow. Surtout,
   l'hypothèse « le SDK Android manque sur les runners » est fausse : les images
   `windows-latest` et `macos-latest` définissent déjà `ANDROID_HOME`/`ANDROID_SDK_ROOT`
   (vérifié dans les readme des images et dans le log macOS, qui a configuré puis compilé
   le projet sans aucun setup SDK). L'étape a donc été supprimée plutôt que corrigée.
2. **Cause racine Windows réelle : `gradle/gradle-daemon-jvm.properties`.** Le log Windows
   s'arrête avant toute tâche avec
   `Unable to download toolchain … {languageVersion=17} … No defined toolchain download url for WINDOWS`.
   La cause est le fichier `gradle-daemon-jvm.properties` qui épinglait un JDK 17 ; les
   runners Linux/macOS en ont un préinstallé par hasard, pas Windows (seul `JAVA_HOME_17_X64`
   existe, non détecté par Gradle). L'assessment ne mentionnait pas ce fichier.
3. **Cause racine macOS réelle : entrée ZIP dupliquée.** `:app:createDistributable` échoue avec
   `java.util.zip.ZipException: duplicate entry: META-INF/versions/9/module-info.class`.
   Compose Desktop re-zippe chaque jar du runtime sur macOS (pour signer les `.dylib`), et
   `kotlinx-coroutines-debug-1.10.1.jar` — qui fuit dans le classpath de production via les
   dépendances de test déclarées dans `commonMain` de `:core` — contient deux fois cette
   entrée. L'assessment évoquait `packageVersion`/bundleID, qui ne sont pas en cause.
4. **Portée élargie à `app/build.gradle.kts`.** Fichier absent de « Files likely to change »
   de l'assessment ; l'élargissement est requis par la cause macOS ci-dessus.

## Follow-ups

- Déplacer `KStringSpec.kt` (et les dépendances Kotest / `kotlinx-coroutines-test`) hors de
  `:core/src/commonMain` vers un vrai `testFixtures`/source set de test, pour ne plus
  embarquer de bibliothèques de test dans les distributions desktop et l'APK.
- Surveiller le premier run CI après merge pour confirmer les trois runners ; si un autre jar
  multi-release dupliqué apparaît, ajouter son artefact à l'exclusion (ou corriger la
  dépendance amont).
- Envisager `actions/setup-java@v5` (v4 est déprécié) et la migration `checkout`/`upload-artifact`
  hors de Node 20, sans lien avec ce bug.
