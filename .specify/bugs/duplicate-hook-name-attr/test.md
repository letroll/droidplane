# Bug Verification: Duplicate `NAME` attribute on `<hook>` XML elements

- **Slug**: duplicate-hook-name-attr
- **Tested**: 2026-11-02
- **Assessment**: ./assessment.md
- **Fix**: ./fix.md
- **Result**: verified

## Summary

The bug no longer reproduces: loading and saving `.mm` mindmaps containing generic `<hook>` elements (`MapStyle`, `AutomaticEdgeColor`, `ScriptHook`, `AutomaticLayout.properties`, etc.) now emits `NAME="..."` exactly once per `<hook>` element. All new and existing unit tests in the `:data` module pass with zero regressions.

## Checks Performed

| Check | Command / Action | Result | Notes |
|-------|------------------|--------|-------|
| Reproduction (post-fix) | `FreeplaneXmlRoundTripTest` & `HooksXmlTest` (load `.mm` with generic hooks, serialize to `.mm`, inspect `<hook>` tags) | pass | Every `<hook>` tag in the serialized XML has `Regex("\\bNAME=").findAll(tag).count() == 1` and `hook.attributes.containsKey("NAME") == false`. |
| New / updated tests | `./gradlew :data:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.data.HooksXmlTest" --tests "fr.julien.quievreux.droidplane2.data.FreeplaneXmlRoundTripTest"` | pass | Both parser exclusion of `NAME` and serializer guard against duplicate `NAME` attributes pass. |
| Regression suite | `./gradlew :data:testDebugUnitTest --rerun-tasks` | pass | Full `:data` unit test suite (27 actionable tasks executed) passed cleanly. |
| Lint / type-check | `:data:compileDebugKotlinAndroid`, `:data:compileDebugUnitTestKotlinAndroid` | pass | Compiled with zero errors as part of `:data:testDebugUnitTest --rerun-tasks`. |

## Output Excerpts

```text
> Task :data:compileDebugUnitTestKotlinAndroid
> Task :data:testDebugUnitTest

BUILD SUCCESSFUL in 21s
27 actionable tasks: 27 executed
```

## Residual Risks

- Mindmap `.mm` files that were *already* saved on disk prior to this fix still contain duplicate `NAME` attributes in their raw XML. Opening and re-saving them in DroidPlane (or removing the duplicate `NAME` attribute in a text editor) is required to repair those previously corrupted files for Freeplane Desktop.

## Recommendation

Close the bug — verified end-to-end across XML parsing, `GenericHookElement` in-memory representation, and XML serialization.
