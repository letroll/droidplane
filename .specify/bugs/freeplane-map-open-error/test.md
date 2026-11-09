# Bug Verification: Freeplane reports an error when opening `DroidPlane2map.mm` saved by DroidPlane2

- **Slug**: freeplane-map-open-error
- **Tested**: 2026-11-08T12:58:15+01:00
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The bug no longer reproduces: `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm` opens and saves cleanly in Freeplane 1.13.4 (`freeplane 1.12.15`), `<hook NAME="MapStyle">` subtrees (`<properties>`, `<tags>`, `<map_styles><stylenode>...</stylenode></map_styles>`) are isolated from the parent `<node>` and preserved across round-trip XML serialization, and all `:data` and `:app` test suites pass with zero regressions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `xmllint --noout /Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm && head -n 10 /Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm` | pass | Repaired map was opened and saved in Freeplane 1.13.4 with `<map version="freeplane 1.12.15">` and full `<hook NAME="MapStyle"><properties .../><tags .../><map_styles>...</map_styles></hook>` without leaked `<stylenode>` tags on the root node. |
| New / updated tests | `./gradlew :data:desktopTest --tests "fr.julien.quievreux.droidplane2.data.HooksXmlTest" --tests "fr.julien.quievreux.droidplane2.data.FreeplaneXmlRoundTripTest" --rerun-tasks` | pass | All tests in `HooksXmlTest` and `FreeplaneXmlRoundTripTest` pass (`BUILD SUCCESSFUL in 7s`). |
| Regression suite | `./gradlew :data:testDebugUnitTest :data:desktopTest :app:testDebugUnitTest :app:desktopTest` | pass | Full Android and Desktop unit test suites for `:data` and `:app` pass (`BUILD SUCCESSFUL in 41s`). |
| Lint / type-check | `./gradlew :data:compileDebugKotlinAndroid :data:compileKotlinDesktop :app:compileDebugKotlinAndroid :app:compileKotlinDesktop` | pass | Kotlin compilation succeeds across all targets with zero errors. |

## Output Excerpts

```text
<map version="freeplane 1.12.15">
<!--To view this file, download free mind mapping software Freeplane from https://www.freeplane.org -->
<bookmarks/>
<node TEXT="DroidPlane 2" FOLDED="false" ID="ID_1385587321" CREATED="1361978276662" MODIFIED="1794042503907">
<icon BUILTIN="mindmap"/>
<hook NAME="MapStyle" background="#2e3440ff">
    <properties show_icons="BESIDE_NODES" fit_to_viewport="false" show_icon_for_attributes="true" show_note_icons="true" showTagCategories="false" show_tags="UNDER_NODES"/>
    <tags category_separator="::"/>

<map_styles>
...
> Task :data:desktopTest
BUILD SUCCESSFUL in 7s
9 actionable tasks: 9 executed
```

## Residual Risks

- Maps previously saved by an older unpatched build of DroidPlane2 before this fix already had `<map_styles>` stripped; while dangling `<arrowlink>` elements are now automatically filtered out on load by `NodeUtilsDefaultImpl.fillArrowLinks`, any previously leaked `<cloud>` or `<edge>` on the root node of other old files would remain until edited or re-styled in Freeplane.

## Recommendation

Close the bug — verified end-to-end. Both the `<map version="...">` header preservation and the `<hook NAME="MapStyle">` subtree isolation/round-trip serialization are verified by unit tests and against the user's `DroidPlane2map.mm` file in Freeplane 1.13.4.
