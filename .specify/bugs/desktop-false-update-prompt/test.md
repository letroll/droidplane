# Bug Verification: desktop-false-update-prompt

- **Slug**: `desktop-false-update-prompt`
- **Tested**: 2026-11-17
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: partial

## Summary

Clean builds and full automated test suites confirm that `DESKTOP_APP_VERSION = "1.0.31"` is gone, `AppBuildInfo.VERSION_NAME` (`"1.0.38"`) is regenerated from `app/build.gradle.kts` after `:app:clean`, and Desktop update checks now receive `"1.0.38"` (`VersionConfigurationTest`, `UpdateCheckerServiceTest`, and `AppVersionTest` all pass with zero failures). The live Desktop GUI launch against the GitHub Releases API from the assessment's reproduction steps was not executed interactively (network-dependent GUI check), so the verification result is marked `partial` pending a manual Desktop launch check.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (automated equivalent: clean codegen + version wiring + update comparison) | `./gradlew --no-daemon --offline :app:clean :app:desktopTest :app:compileKotlinDesktop` + `rg -n "DESKTOP_APP_VERSION\|1\.0\.31" app/src/desktopMain/` | pass | Clean build regenerates `AppBuildInfo.VERSION_NAME = "1.0.38"`; zero matches for `DESKTOP_APP_VERSION` or `"1.0.31"` in `app/src/desktopMain/`; `VersionConfigurationTest[desktop]` (2/2), `AppVersionTest[desktop]` (6/6), and `UpdateCheckerServiceTest[desktop]` (11/11) pass. |
| Reproduction (live Desktop GUI + GitHub API) | Launch `:app:run` (`Main.kt` -> `DesktopDroidplaneApp`) and observe startup check against live GitHub release `1.0.38` | skipped | Network-dependent and interactive GUI launch not run from CLI without explicit user run. |
| New / updated tests | `./gradlew --no-daemon --offline :app:clean :app:desktopTest` | pass | `VersionConfigurationTest[desktop]` ran 2 tests (`0` skipped, `0` failures, `0` errors). |
| Regression suite | `./gradlew --no-daemon --offline :app:clean :app:desktopTest :app:testDebugUnitTest :app:compileKotlinDesktop` | pass | `desktopTest`: 136 tests across 14 suites (`0` failed, `0` skipped); `testDebugUnitTest`: 136 tests across 14 suites (`0` failed, `0` skipped); `:app:compileKotlinDesktop` and `:app:compileDebugKotlinAndroid` succeeded (`BUILD SUCCESSFUL in 34s`). |
| Lint / type-check | Kotlin compilation via `:app:compileKotlinDesktop`, `:app:compileDebugKotlinAndroid`, and test compilation | pass | Compiles cleanly for both Desktop and Android targets with no new warnings or errors in modified code. |

## Output Excerpts

```text
> Task :app:desktopTest
> Task :app:testDebugUnitTest
BUILD SUCCESSFUL in 34s
71 actionable tasks: 33 executed, 38 up-to-date
```

```xml
<testsuite name="VersionConfigurationTest[desktop]" tests="2" skipped="0" failures="0" errors="0" ...>
<testsuite name="AppVersionTest[desktop]" tests="6" skipped="0" failures="0" errors="0" ...>
<testsuite name="UpdateCheckerServiceTest[desktop]" tests="11" skipped="0" failures="0" errors="0" ...>
```

```text
NO_MATCHES
```

## Residual Risks

- Live network behavior against the actual GitHub Releases endpoint when launching the Desktop GUI (`./gradlew :app:run`) was not exercised interactively during this offline CLI pass.
- `compose.desktop.application.nativeDistributions.packageVersion` is not configured from `appVersionName` if native installers are packaged later (does not affect in-app update checks).

## Recommendation

Hold only for a quick manual smoke launch (`./gradlew :app:run` on Desktop while online) to confirm `UpdateAvailableDialog` does not appear on `1.0.38` against live GitHub releases; all automated build, code-generation, Desktop, and Android unit checks pass cleanly.
