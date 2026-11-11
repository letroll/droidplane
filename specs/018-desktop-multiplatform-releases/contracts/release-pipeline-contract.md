# Release Pipeline Contracts & Specifications

**Feature**: `018-desktop-multiplatform-releases`  
**Date**: 2026-11-11  

---

## 1. Contrat de Configuration Gradle (`app/build.gradle.kts`)

Le bloc `compose.desktop.application.nativeDistributions` dans `:app` doit satisfaire le contrat suivant :

```kotlin
compose.desktop {
    application {
        mainClass = "fr.julien.quievreux.droidplane2.MainKt"
        javaHome = javaToolchains.launcherFor {
            languageVersion.set(JavaLanguageVersion.of(21))
        }.get().metadata.installationPath.asFile.absolutePath

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "Droidplane"
            packageVersion = appVersionName
            description = "Droidplane Mind Map Application"
            vendor = "Droidplane"
            copyright = "© 2026 Julien Quiévreux"

            macOS {
                bundleID = "fr.julien.quievreux.droidplane2"
            }
            windows {
                menuGroup = "Droidplane"
                upgradeUuid = "a4c28f6d-74d1-4cb5-8292-1e96a40a5a22"
            }
            linux {
                // Métadonnées Linux / conventions standard freedesktop
            }
        }
    }
}
```

### Contrats des Tâches Gradle
- **Linux** : `./gradlew :app:packageDeb` produit `app/build/compose/binaries/main/deb/Droidplane-<version>.deb`
- **Windows** : `./gradlew :app:packageMsi` produit `app/build/compose/binaries/main/msi/Droidplane-<version>.msi`
- **macOS** : `./gradlew :app:packageDmg` produit `app/build/compose/binaries/main/dmg/Droidplane-<version>.dmg`
- **Android** : `./gradlew assembleRelease` produit `app/build/outputs/apk/release/*.apk`

---

## 2. Contrat du Workflow GitHub Actions (`.github/workflows/android_release.yml` ou `release.yml`)

### Schéma des Dépendances entre Jobs

```yaml
jobs:
  prepare-release:
    runs-on: ubuntu-latest
    outputs:
      version_name: ${{ steps.bump.outputs.version_name }}
      version_code: ${{ steps.bump.outputs.version_code }}
      changelog_generated: ${{ steps.changelog.outputs.status }}

  build-android:
    needs: prepare-release
    runs-on: ubuntu-latest

  build-desktop:
    needs: prepare-release
    strategy:
      fail-fast: false
      matrix:
        include:
          - os: ubuntu-latest
            platform: linux
            task: ":app:packageDeb"
            artifact_name: "release-desktop-linux"
            artifact_path: "app/build/compose/binaries/main/deb/*.deb"
          - os: windows-latest
            platform: windows
            task: ":app:packageMsi"
            artifact_name: "release-desktop-windows"
            artifact_path: "app/build/compose/binaries/main/msi/*.msi"
          - os: macos-latest
            platform: macos
            task: ":app:packageDmg"
            artifact_name: "release-desktop-macos"
            artifact_path: "app/build/compose/binaries/main/dmg/*.dmg"

  publish-release:
    needs: [prepare-release, build-android, build-desktop]
    runs-on: ubuntu-latest
    steps:
      - uses: actions/download-artifact@v4
      - uses: softprops/action-gh-release@v2
        with:
          tag_name: v${{ needs.prepare-release.outputs.version_name }}
          name: "Release v${{ needs.prepare-release.outputs.version_name }}"
          body_path: artifacts/changelog/CHANGELOG.md
          files: |
            artifacts/release-android/*.apk
            artifacts/release-desktop-linux/*.deb
            artifacts/release-desktop-windows/*.msi
            artifacts/release-desktop-macos/*.dmg
```

### Contrat des Variables et Secrets
- `SIGNING_KEY_STORE_BASE64` : Clé de signature Keystore Android (Job Android).
- `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` : Mots de passe Keystore Android (Job Android).
- `GITHUB_TOKEN` : Jeton d'authentification pour pusher le commit de version, créer le tag Git et publier les actifs sur GitHub Releases.
