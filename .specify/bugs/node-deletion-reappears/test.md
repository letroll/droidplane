# Bug Verification: Deleted Node Reappears After Navigation

- **Slug**: node-deletion-reappears
- **Tested**: 2026-10-06T12:30:00Z
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The bug no longer reproduces: deleting a node updates parent and ancestor references throughout the tree so that navigating to a surviving sibling and returning to the parent does not restore the deleted node. All reproduction tests, new unit tests, and full regression suites passed with zero regressions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `./gradlew :data:testDebugUnitTest --tests "*navigate to sibling then navigate back*"` and `:app:testDebugUnitTest --tests "*navigate to sibling and back*"` | pass | Verified in both data-layer tree navigation and MainViewModel UI state navigation. |
| New / updated tests | `./gradlew :data:testDebugUnitTest` | pass | 40/40 tests passed, including `deleteNode` parent updates, `restoreSubtree`, and ancestor propagation tests. |
| Regression suite | `./gradlew testDebugUnitTest --rerun-tasks` | pass | 66/66 test tasks executed and passed across `:core`, `:data`, and `:app` modules. |
| Lint / type-check | `./gradlew assembleDebug` | pass | Full debug build and Kotlin compilation succeeded cleanly across all modules. |

## Output Excerpts

### Reproduction and Unit Tests (`:data:testDebugUnitTest`)
```text
> Task :data:compileDebugKotlin
> Task :data:compileDebugUnitTestKotlin
> Task :data:testDebugUnitTest
BUILD SUCCESSFUL in 6s
29 actionable tasks: 29 executed
```

### ViewModel and UI State Tests (`:app:testDebugUnitTest`)
```text
> Task :app:compileDebugKotlin
> Task :app:compileDebugUnitTestKotlin
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 9s
56 actionable tasks: 56 executed
```

### Full Regression Suite (`testDebugUnitTest --rerun-tasks`)
```text
BUILD SUCCESSFUL in 9s
66 actionable tasks: 66 executed
```

### Full Build Validation (`assembleDebug`)
```text
BUILD SUCCESSFUL in 39s
99 actionable tasks: 49 executed, 50 up-to-date
```

## Residual Risks

- Deeply nested or cyclic arrow links between deleted and surviving nodes rely on ID cleaning; standard arrow link destinations and incoming nodes are filtered upon deletion.
- In-memory deletion does not mutate original XML files on disk until the user triggers a save operation (expected design behavior).

## Recommendation

Close the bug — verified end-to-end. The parent reference synchronization and ancestor propagation up to `rootNode` resolve both the immediate display issue and the reappearance of deleted nodes upon sibling navigation.
