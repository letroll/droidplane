# Bug Fix: Screen Rotation Data Loss

- **Slug**: screen-rotation-data-loss
- **Fixed**: 2026-10-14
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Prevented destructive reinitialization of the active mindmap during Android configuration changes (screen rotation). Guarded cold-start mindmap creation in `MainActivity.kt` with `savedInstanceState == null && getRootNode() == null` and handled `onNewIntent`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `app/src/main/java/fr/julien/quievreux/droidplane2/MainActivity.kt` | modified | Guarded cold-start mindmap creation/loading, added `onNewIntent`, guarded `triggerUpdateCheck` |
| `app/src/test/java/fr/julien/quievreux/droidplane2/MainActivityRotationTest.kt` | added test | Unit test verifying cold start vs. activity recreation state preservation |

## Diff Highlights

```kotlin
// MainActivity.kt
        viewModel.apply {
            setFileRegister(this@MainActivity)
            if (savedInstanceState == null && getRootNode() == null) {
                if (isAnExternalMindMapEdit()) {
                    ...
                } else {
                    createEmptyMindmap(getString(R.string.default_root_node_title))
                    ...
                    showStartupChooser(...)
                }
            }
        }
```

## Tests Added or Updated

- `app/src/test/java/fr/julien/quievreux/droidplane2/MainActivityRotationTest.kt::cold start condition is true when savedInstanceState is null and getRootNode is null`
- `app/src/test/java/fr/julien/quievreux/droidplane2/MainActivityRotationTest.kt::screen rotation does not trigger cold start reinitialization when savedInstanceState is not null`
- `app/src/test/java/fr/julien/quievreux/droidplane2/MainActivityRotationTest.kt::screen rotation does not trigger cold start when getRootNode is already loaded`

## Local Verification

- Commands run: `./gradlew test` → BUILD SUCCESSFUL (133+ tasks passed)
- Manual checks: Verified that `setFileRegister(this@MainActivity)` is still executed on every `onCreate()` to ensure saving remains functional across rotations.

## Deviations from Assessment

None. The fix strictly aligns with the preferred remediation in `assessment.md`.

## Follow-ups

- Run `/speckit-bug-test slug=screen-rotation-data-loss` to validate the fix report.
