# Bug Assessment: Conflit entre le dialogue de mise à jour et les dialogues de fichiers récents / choix au démarrage

- **Slug**: `update-dialog-recent-files-conflict`
- **Created**: 2026-11-03
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Reported Symptom

> il y a un problème entre les dialogues de listing des fichiers récents et la présence de mise à jour sur mobile. Lorsqu'une mise à jour est disponible. J'ai le dialogue qui s'ouvre puis celui des fichiers récents qui ferme alors celui annonçant la mise à jour

Sur mobile (et plus généralement au démarrage de l'application), lorsqu'une mise à jour GitHub est détectée, le dialogue annonçant la mise à jour (`UpdateAvailableDialog`) s'affiche brièvement puis est immédiatement fermé / écrasé par l'ouverture du dialogue de sélection au démarrage / fichiers récents (`StartupChooserDialog` ou `RecentFilesDialog`). Dans le cas inverse (selon la latence réseau par rapport à la vérification I/O des fichiers récents), le dialogue de mise à jour écrase le dialogue des fichiers récents, et sa fermeture laisse l'utilisateur sur un écran vide sans sélecteur de fichier.

## Steps to Reproduce (Inferred)

1. Avoir au moins un fichier `.mm` dans la liste des fichiers récents sur un appareil Android (stocké via SAF `content://`, ce qui nécessite une vérification d'accessibilité asynchrone sur `Dispatchers.IO`).
2. Disposer d'une version de l'application inférieure à la dernière release publiée sur GitHub (ou d'une mise à jour disponible non ignorée).
3. Lancer l'application sans `Intent` de fichier (démarrage normal sur `MainActivity.onCreate`).
4. `triggerUpdateCheck(isManual = false)` et `openStartupChooser()` sont tous deux lancés de manière concurrente au démarrage :
   - Lorsque la vérification de mise à jour (`checkForUpdates`) aboutit avant la fin de la vérification d'accessibilité des fichiers récents (`recentFilesRepository.getRecentFiles(fileAccessibilityChecker)` sur `Dispatchers.IO`), `DialogType.UpdateAvailable` s'affiche en premier.
   - Dès que la coroutine de `showStartupChooser` (ou `showRecentFilesDialog`) termine son contrôle de fichiers et bascule sur `Dispatchers.Main`, elle appelle `setDialogState(DialogType.StartupChooser(...))` (ou `DialogType.RecentFiles(...)`), ce qui remplace et ferme immédiatement le dialogue `UpdateAvailable`.

## Suspected Code Paths

- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainUiState.kt:50-52` — `DialogUiState(val dialogType: DialogType = DialogType.None)` ne possède qu'un seul emplacement (`dialogType`) pour représenter l'ensemble des dialogues de l'application (modaux métier ET notification de mise à jour).
- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt:1064-1066` — `setDialogState(dialogType: MainUiState.DialogType)` écrase inconditionnellement `uiState.value.dialogUiState.dialogType` sans vérifier si un autre dialogue est déjà affiché ni conserver le dialogue précédent.
- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt:329-346` — `showStartupChooser(fileAccessibilityChecker, ...)` lance une coroutine sur `Dispatchers.IO` pour filtrer les fichiers récents (`recentFilesRepository.getRecentFiles(fileAccessibilityChecker)`), puis bascule sur `Dispatchers.Main` pour appeler la surcharge synchrone `showStartupChooser(recentFiles, ...)` (`MainViewModel.kt:355-391`) qui invoque `setDialogState(MainUiState.DialogType.StartupChooser(...))`.
- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt:399-429` — `showRecentFilesDialog(...)` effectue le même chargement asynchrone sur `Dispatchers.IO` suivi d'un appel à `setDialogState(MainUiState.DialogType.RecentFiles(...))` sur `Dispatchers.Main`.
- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt:1328-1362` — `checkForUpdates(isManual)` s'exécute dans `viewModelScope.launch` et appelle `setDialogState(MainUiState.DialogType.UpdateAvailable(...))` dès que `checkForUpdateUseCase` retourne `UpdateCheckResult.UpdateAvailable`, et son callback `onDismiss` réinitialise `setDialogState(MainUiState.DialogType.None)`.
- `app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/MainActivity.kt:286-288` & `399-407` — Dans `MainActivity.onCreate`, `triggerUpdateCheck(isManual = false)` est appelé à la ligne 287 et `openStartupChooser()` est appelé à la ligne 404 (lorsque `intent?.data == null`), déclenchant systématiquement la course entre les deux coroutines au lancement sur Android.
- `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/DesktopDroidplaneApp.kt:200-212` — Même lancement concurrent dans `LaunchedEffect(Unit)` entre `viewModel.showStartupChooser(...)` et `triggerUpdateCheck(isManual = false)`.

## Root Cause Hypothesis

**Confidence**: high

L'état d'interface `MainUiState` modélise tous les dialogues via un unique champ mutuellement exclusif `dialogUiState.dialogType: DialogType`. Or, au démarrage de l'application (`MainActivity.onCreate` sur Android et `LaunchedEffect(Unit)` sur Desktop), deux opérations asynchrones indépendantes sont lancées simultanément :
1. `triggerUpdateCheck(isManual = false)` → `MainViewModel.checkForUpdates()`, qui interroge l'API GitHub (ou le cache) et appelle `setDialogState(DialogType.UpdateAvailable(...))`.
2. `openStartupChooser()` → `MainViewModel.showStartupChooser(fileAccessibilityChecker, ...)`, qui vérifie l'existence et les permissions URI SAF (`DocumentFile.fromSingleUri` / `ContentResolver`) de chaque fichier récent sur `Dispatchers.IO`, puis appelle `setDialogState(DialogType.StartupChooser(...))`.

Comme `setDialogState` remplace directement `dialogUiState.dialogType` :
- **Scénario A (observé par l'utilisateur)** : `checkForUpdates` ouvre `DialogType.UpdateAvailable` en premier ; quelques dizaines ou centaines de millisecondes plus tard, l'inspection I/O SAF de `showStartupChooser` (ou `showRecentFilesDialog`) se termine et appelle `setDialogState(DialogType.StartupChooser(...))`, fermant brusquement le dialogue de mise à jour avant que l'utilisateur ait pu interagir avec.
- **Scénario B (course inverse)** : `showStartupChooser` ouvre `DialogType.StartupChooser` en premier ; lorsque la requête réseau de `checkForUpdates` se termine, `setDialogState(DialogType.UpdateAvailable(...))` écrase `StartupChooser`. En outre, lorsque l'utilisateur ferme le dialogue de mise à jour ("Plus tard" ou "Ignorer cette version"), `onDismiss` appelle `setDialogState(DialogType.None)`, faisant disparaître définitivement le dialogue des fichiers récents au démarrage.

## Proposed Remediation

### Direction

Découpler ou empiler proprement l'état du dialogue de mise à jour (`UpdateAvailable`) par rapport aux dialogues de navigation/fichiers (`StartupChooser`, `RecentFiles`, etc.) afin qu'ils ne s'écrasent jamais mutuellement :

1. **Séparation ou mise en file d'attente dans `MainUiState` / `MainViewModel`** :
   - **Approche recommandée (file d'attente / restauration de dialogue ou état dédié `updateDialogState`)** :
     - Soit conserver une pile / file de dialogues dans `MainViewModel` (ou un champ dédié `updateAvailableDialog: DialogType.UpdateAvailable? = null` dans `MainUiState` ou `DialogUiState`), soit gérer la priorité d'affichage de sorte que :
       - Si un dialogue (`StartupChooser` ou `RecentFiles`) est déjà affiché quand `UpdateAvailable` arrive (ou inversement si `UpdateAvailable` est affiché quand `StartupChooser` / `RecentFiles` arrive), aucun des deux n'est perdu.
       - Le dialogue `UpdateAvailable` est présenté par-dessus (ou en priorité), et dès que l'utilisateur le ferme (`onDismiss` / `onLater` / `onSkipVersion`), le dialogue sous-jacent (`StartupChooser` ou `RecentFiles`) est automatiquement restauré / affiché au lieu de retomber sur `DialogType.None`.
       - De même, si `UpdateAvailable` est déjà à l'écran lorsque `showStartupChooser` ou `showRecentFilesDialog` termine son chargement asynchrone, le nouveau dialogue (`StartupChooser` / `RecentFiles`) ne ferme pas `UpdateAvailable` : il est mis en attente (ou conservé comme dialogue sous-jacent) et s'affiche dès que `UpdateAvailable` est fermé (ou `UpdateAvailable` reste affiché au premier plan jusqu'à action de l'utilisateur).

### Files Likely to Change

- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainUiState.kt` — Faire évoluer `DialogUiState` (par ex. ajout de `pendingDialogType` / pile ou champ dédié pour `UpdateAvailable`) ou la structure d'état des dialogues.
- `app/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/MainViewModel.kt` — Adapter `setDialogState`, `checkForUpdates`, `showStartupChooser`, et `showRecentFilesDialog` pour préserver les dialogues en concurrence et restaurer le dialogue des fichiers récents après fermeture du dialogue de mise à jour.
- `app/src/androidMain/kotlin/fr/julien/quievreux/droidplane2/MainActivity.kt` — Vérifier le rendu de `DialogType.UpdateAvailable` / `StartupChooser` / `RecentFiles` si `DialogUiState` expose un état séparé, ou conserver le `when (dialogType)` existant si la pile de dialogues est gérée de manière transparente dans `MainViewModel`.
- `app/src/desktopMain/kotlin/fr/julien/quievreux/droidplane2/ui/DesktopDroidplaneApp.kt` — Même alignement côté Desktop si la structure de `DialogUiState` évolue.
- `app/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/MainViewModelTest.kt` — Ajouter des tests unitaires couvrant les deux ordres d'arrivée (`UpdateAvailable` puis `StartupChooser`/`RecentFiles`, et `StartupChooser`/`RecentFiles` puis `UpdateAvailable`) ainsi que la restauration du dialogue après fermeture de `UpdateAvailable`.

### Risks & Side Effects

- **Bouton Retour Android (`BackHandler`)** : Sur Android (`MainActivity.kt:314-316`), `BackHandler(true) { onBackPress() }` intercepte le bouton retour lorsque l'application est à la racine. Il faut s'assurer que la fermeture du dialogue `UpdateAvailable` (via "Plus tard", "Ignorer" ou retour système dans le `AlertDialog`) restaure proprement `StartupChooser` / `RecentFiles` sans déclencher `upOrClose()` par erreur.
- **Compatibilité multiplateforme (Android & Desktop)** : En centralisant la gestion de la pile / file d'attente dans `MainViewModel.setDialogState` (tout en exposant le dialogue actif au sommet via `dialogUiState.dialogType`), Android et Desktop bénéficient automatiquement du correctif sans dupliquer la logique d'affichage dans `MainActivity` et `DesktopDroidplaneApp`.

## Open Questions

None.
