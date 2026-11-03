# Bug Fix: Conflit entre le dialogue de mise à jour et les dialogues de fichiers récents / choix au démarrage

- **Slug**: `update-dialog-recent-files-conflict`
- **Fixed**: 2026-11-03
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Ajout d'une file d'attente de dialogue (`deferredDialogType`) dans `MainUiState.DialogUiState` et gestion de priorité dans `MainViewModel.setDialogState` afin que `DialogType.UpdateAvailable` et `DialogType.StartupChooser` / `DialogType.RecentFiles` ne s'écrasent plus mutuellement lors de leur chargement asynchrone au démarrage ou en cours de navigation. Le dialogue de mise à jour reste affiché en priorité et la fermeture de `UpdateAvailable` restaure automatiquement le dialogue des fichiers récents ou du sélecteur de démarrage.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainUiState.kt` | modified | Ajout de `val deferredDialogType: DialogType = DialogType.None` à `DialogUiState`. |
| `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt` | modified | Mise à jour de `setDialogState` pour prioriser `UpdateAvailable`, différer `StartupChooser`/`RecentFiles` lorsque `UpdateAvailable` est actif, et restaurer `deferredDialogType` lors du passage à `DialogType.None`. Uniformisation de `onSaveNodeProperties` et `onMoveCloudToParent` pour passer par `setDialogState`. |
| `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` | added test | Ajout de deux tests unitaires vérifiant les deux ordres d'arrivée concurrente (`UpdateAvailable` avant `StartupChooser` et `RecentFiles` avant `UpdateAvailable`) ainsi que la restauration du dialogue à la fermeture. |

## Diff Highlights (optional)

```kotlin
// MainUiState.kt
data class DialogUiState(
    val dialogType: DialogType = DialogType.None,
    val deferredDialogType: DialogType = DialogType.None,
)
```

```kotlin
// MainViewModel.kt
fun setDialogState(dialogType: DialogType) {
    updateDialogState { current ->
        when {
            dialogType is DialogType.None -> {
                DialogUiState(
                    dialogType = current.deferredDialogType,
                    deferredDialogType = DialogType.None,
                )
            }
            dialogType is DialogType.UpdateAvailable -> {
                val dialogToDefer = when (current.dialogType) {
                    is DialogType.StartupChooser, is DialogType.RecentFiles -> current.dialogType
                    else -> current.deferredDialogType
                }
                DialogUiState(
                    dialogType = dialogType,
                    deferredDialogType = dialogToDefer,
                )
            }
            (dialogType is DialogType.StartupChooser || dialogType is DialogType.RecentFiles) &&
                current.dialogType is DialogType.UpdateAvailable -> {
                current.copy(deferredDialogType = dialogType)
            }
            else -> {
                DialogUiState(
                    dialogType = dialogType,
                    deferredDialogType = DialogType.None,
                )
            }
        }
    }
}
```

## Tests Added or Updated

- `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt::StartupChooser arriving while UpdateAvailable is displayed keeps UpdateAvailable visible and restores StartupChooser on dismiss` — Vérifie que si `checkForUpdates` affiche `UpdateAvailable` avant la fin du chargement I/O de `showStartupChooser`, `UpdateAvailable` n'est pas fermé par `StartupChooser` et la fermeture de `UpdateAvailable` ouvre `StartupChooser`.
- `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt::UpdateAvailable arriving while StartupChooser or RecentFiles is displayed shows UpdateAvailable and restores previous dialog on dismiss` — Vérifie que si `RecentFiles` (ou `StartupChooser`) est déjà affiché quand `checkForUpdates` se termine, `UpdateAvailable` s'affiche par-dessus et restaure `RecentFiles` lorsqu'il est fermé.

## Local Verification

- Commands run: `./gradlew :app:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.MainViewModelTest"` → `BUILD SUCCESSFUL` (tous les tests de `MainViewModelTest` passent).
- Manual checks: Vérification des appels à `setDialogState` et `DialogUiState` dans `MainViewModel`, `MainActivity` et `DesktopDroidplaneApp`.

## Deviations from Assessment

Comme anticipé dans l'analyse (section *Risks & Side Effects*), la centralisation de la mise en attente (`deferredDialogType`) directement dans `MainUiState.DialogUiState` et `MainViewModel.setDialogState` a permis de corriger à la fois Android (`MainActivity.kt`) et Desktop (`DesktopDroidplaneApp.kt`) sans avoir besoin de modifier le code des vues Compose de ces deux fichiers.

## Follow-ups

- Exécuter `/speckit-bug-test slug=update-dialog-recent-files-conflict` pour valider l'ensemble de la suite de tests Android et Desktop.
