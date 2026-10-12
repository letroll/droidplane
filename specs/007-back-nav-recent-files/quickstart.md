# Quickstart: Back Navigation to Recent Files

## Prerequisites
- Android Studio Ladybug+ or IntelliJ with Android plugin
- Android SDK 35, Build Tools 35
- JDK 21
- Physical device or emulator (API 26+)

## Setup
```bash
cd /home/letroll/AndroidStudioProjects/droidplane
./gradlew :app:assembleDebug
```

## Test Scenarios

### Scenario 1: Back shows Recent Files (no unsaved changes)
1. Launch app → opens empty mindmap (or recent file)
2. Make NO changes to mindmap
3. Press system back button OR toolbar back button
4. **Expected**: Recent Files dialog opens with recent files list
5. Select a recent file → mindmap loads, dialog closes
6. Press "New Mindmap" → new empty mindmap, dialog closes
7. Press "Exit App" → app closes completely
8. Press dialog dismiss → returns to mindmap

### Scenario 2: Back shows Exit Confirmation (unsaved changes)
1. Launch app → opens mindmap
2. Edit a node (creates unsaved changes)
3. Press system back button OR toolbar back button
4. **Expected**: Exit Confirmation dialog opens (not Recent Files)
5. Confirm exit → app closes without saving
6. Cancel → returns to mindmap with changes preserved

### Scenario 3: Toolbar menu access
1. Launch app → opens mindmap
2. Open toolbar overflow menu (three-dot)
3. Select "Recent Files"
4. **Expected**: Recent Files dialog opens
5. Verify all buttons work as in Scenario 1

### Scenario 4: Large screen / tablet
1. Run on tablet/emulator with large screen
2. Open toolbar menu or equivalent navigation
3. Select "Recent Files"
4. **Expected**: Dialog opens and functions identically

## Validation Commands
```bash
# Unit tests
./gradlew :app:test

# UI tests (requires device/emulator)
./gradlew :app:connectedAndroidTest

# Lint
./gradlew :app:lint

# All checks
./gradlew check
```

## Expected Outcomes
- All existing tests pass (regression)
- New tests for RecentFilesDialog and back navigation logic pass
- Manual verification matches acceptance scenarios in spec