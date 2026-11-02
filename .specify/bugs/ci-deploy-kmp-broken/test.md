# Bug Verification: CI Release Deployment Broken Since KMP Migration

- **Slug**: ci-deploy-kmp-broken
- **Tested**: 2026-11-03
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The CI release build failure no longer reproduces: `./gradlew assembleRelease` (including `:core:lintVitalAnalyzeRelease`, `:app:lintVitalAnalyzeRelease`, and `:app:minifyReleaseWithR8`) succeeds and produces the release APK in `app/build/outputs/apk/release/`. All Android and Desktop KMP unit test suites (`testDebugUnitTest`, `:core:desktopTest`, `:data:desktopTest`, `:app:desktopTest`) pass without regression.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `./gradlew assembleRelease` | pass | `:core:lintVitalAnalyzeRelease`, `:app:lintVitalAnalyzeRelease`, `:app:minifyReleaseWithR8`, and `:app:packageRelease` succeeded; generated `app/build/outputs/apk/release/app-release-unsigned.apk` (3.55 MB) |
| New / updated tests | `./gradlew testDebugUnitTest :core:desktopTest :data:desktopTest :app:desktopTest` | pass | All Android and Desktop unit tests in `:core`, `:data`, and `:app` passed (`xpp3` available on `debugUnitTestRuntimeClasspath` and `desktopTestRuntimeClasspath`) |
| Regression suite | `./gradlew testDebugUnitTest :core:desktopTest :data:desktopTest :app:desktopTest` | pass | 85 actionable tasks completed with 0 test failures |
| Lint / type-check | `./gradlew :data:lintVitalAnalyzeRelease :core:lintVitalAnalyzeRelease :app:lintVitalRelease` | pass | Executed as part of `assembleRelease` with 0 fatal lint errors |

## Output Excerpts

```text
> Task :data:lintVitalAnalyzeRelease
> Task :core:lintVitalAnalyzeRelease
> Task :app:lintVitalAnalyzeRelease
> Task :app:lintVitalReportRelease
> Task :app:lintVitalRelease
> Task :app:minifyReleaseWithR8
> Task :app:compileReleaseArtProfile
> Task :app:packageRelease
> Task :app:createReleaseApkListingFileRedirect
> Task :app:assembleRelease

BUILD SUCCESSFUL in 1m 1s
126 actionable tasks: 35 executed, 91 up-to-date
```

```text
> Task :core:testDebugUnitTest UP-TO-DATE
> Task :data:testDebugUnitTest
> Task :core:desktopTest UP-TO-DATE
> Task :data:desktopTest UP-TO-DATE
> Task :app:desktopTest UP-TO-DATE
> Task :app:testDebugUnitTest

BUILD SUCCESSFUL in 22s
85 actionable tasks: 4 executed, 81 up-to-date
```

## Residual Risks

- GitHub Actions signing and release upload steps (`🔓 Restaurer le Keystore`, `💾 Committer et pousser la nouvelle version`, `🚀 Publier sur GitHub Releases`) depend on repository secrets (`SIGNING_KEY_STORE_BASE64`, `KEY_STORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `GITHUB_TOKEN`) and will execute on the next push to `master`.

## Recommendation

Close the bug — verified end-to-end across all local reproduction steps (`lintVitalAnalyzeRelease`, `minifyReleaseWithR8`, `assembleRelease`) and multiplatform test suites (`testDebugUnitTest`, `desktopTest`).
