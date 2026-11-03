# Bug Verification: Conflit entre le dialogue de mise à jour et les dialogues de fichiers récents / choix au démarrage

- **Slug**: `update-dialog-recent-files-conflict`
- **Tested**: 2026-11-03
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

Le bug de course et d'écrasement mutuel entre le dialogue de mise à jour (`DialogType.UpdateAvailable`) et les dialogues de fichiers récents / choix au démarrage (`DialogType.StartupChooser`, `DialogType.RecentFiles`) ne se reproduit plus. Les nouveaux tests unitaires ainsi que l'intégralité de la suite de tests multiplateforme (Android et Desktop) passent sans aucune régression.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | Tests automatisés dans `MainViewModelTest` reproduisant les deux ordres de course (`checkForUpdates` avant `showStartupChooser`, et `showRecentFilesDialog` avant `checkForUpdates`) | pass | `UpdateAvailable` reste affiché au premier plan et sa fermeture restaure `StartupChooser` ou `RecentFiles`. |
| New / updated tests | `./gradlew :app:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.MainViewModelTest"` | pass | Les 2 nouveaux tests de concurrence et restauration des dialogues passent. |
| Regression suite | `./gradlew :app:testDebugUnitTest :app:desktopTest` | pass | Toutes les suites de tests unitaires Android et Desktop passent (`BUILD SUCCESSFUL`). |
| Lint / type-check | Compilation Kotlin Android (`compileDebugKotlinAndroid`) et Desktop (`compileKotlinDesktop`) | pass | Compilation complète sans erreur sur les deux cibles KMP. |

## Output Excerpts

```text
> Task :app:testDebugUnitTest
> Task :app:compileKotlinDesktop
> Task :app:compileTestKotlinDesktop
> Task :app:desktopTest

BUILD SUCCESSFUL in 25s
68 actionable tasks: 5 executed, 63 up-to-date
```

## Residual Risks

- La file d'attente `deferredDialogType` est volontairement ciblée sur la coexistence entre `DialogType.UpdateAvailable` et `DialogType.StartupChooser` / `DialogType.RecentFiles`. Les autres dialogues modaux métier (confirmations de suppression, d'écrasement ou inspecteur de propriétés) conservent leur comportement de remplacement direct lorsqu'ils sont déclenchés par une action explicite de l'utilisateur sur une carte ouverte.

## Recommendation

Close the bug — verified end-to-end.
