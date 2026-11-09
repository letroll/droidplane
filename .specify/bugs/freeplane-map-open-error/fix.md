# Bug Fix: Freeplane reports an error when opening `DroidPlane2map.mm` saved by DroidPlane2

- **Slug**: freeplane-map-open-error
- **Fixed**: 2026-11-08T12:39:15+01:00
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Fixed `XmlParseUtilsDefaultImpl.parseHook` to consume the entire XML subtree of `<hook>...</hook>` elements up to `</hook>` and store the inner XML in `GenericHookElement.rawXml`, preventing child tags inside `<hook NAME="MapStyle"><map_styles><stylenode>...</stylenode></map_styles></hook>` (`<edge>`, `<font>`, `<cloud>`, `<icon>`, `<arrowlink>`) from leaking onto the parent `<node>`. Also updated `NodeManager.serializeMindmap` to preserve and emit `<map version="freeplane ...">` at the start of `.mm` files (without `<?xml ... standalone='yes' ?>`), updated `NodeManager.serializeNode` and `writeRawXmlToSerializer` to round-trip `hook.rawXml` fragments, updated `NodeUtilsDefaultImpl.fillArrowLinks` to drop dangling `<arrowlink>` references, and repaired `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm`.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/NodeAttribute.kt` | modified | Added `VERSION("version")` attribute entry for `<map version="...">`. |
| `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt` | modified | Added `consumeHookInnerXml(xpp)` to advance `XmlPullParser` through the full `<hook>...</hook>` subtree up to the closing `</hook>` tag and populate `GenericHookElement.rawXml`. |
| `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt` | modified | Preserved `<map version="...">` across load/save (defaulting to `"freeplane 1.11.5"`), omitted the incompatible `<?xml ... standalone='yes' ?>` prologue, serialized `hook.rawXml` inside `<hook>`, and wrapped raw XML fragments in `writeRawXmlToSerializer` so multi-sibling fragments (`<properties/>` + `<map_styles>`) serialize cleanly. |
| `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeUtilsDefaultImpl.kt` | modified | Filtered out dangling `connectors` and `arrowLinkDestinationIds` whose target node ID does not exist in `nodesById` during `fillArrowLinks`. |
| `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/HooksXmlTest.kt` | added test | Added regression test verifying `<hook NAME="MapStyle">` subtree isolation, `<map version="freeplane 1.12.15">` preservation, and round-trip serialization. |
| `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm` | modified | Restored `<map version="freeplane 1.12.15">` header and removed the 4 `<stylenode>` tags (`<edge>`, `<font>`, `<cloud>`, `<arrowlink DESTINATION="ID_1499691433">`) that had leaked onto root node `ID_1385587321`. |

## Diff Highlights (optional)

```kotlin
// XmlParseUtilsDefaultImpl.kt
val rawXml = consumeHookInnerXml(xpp).takeIf { it.isNotBlank() }
parent.addGenericHook(
    GenericHookElement(
        name = name,
        attributes = attrs,
        rawXml = rawXml,
    )
)
```

```kotlin
// NodeManager.kt
serializer.setOutput(outputStream, "UTF-8")
serializer.startNodeTag(MAP)
serializer.nodeAttribute(VERSION, mapVersion)
```

## Tests Added or Updated

- `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/HooksXmlTest.kt::MapStyle hook with nested stylenodes does not leak edge, font, cloud, or arrowlink onto parent node and round-trips map version and hook XML` — Verifies that `<stylenode>` child tags inside `<hook NAME="MapStyle"><map_styles>` do not leak onto the root `<node>`, that `<map version="freeplane 1.12.15">` is emitted at the start of the serialized `.mm` file without `<?xml`, and that `<map_styles>` round-trips cleanly inside `<hook NAME="MapStyle">`.

## Local Verification

- Commands run: `./gradlew :data:testDebugUnitTest :data:desktopTest :app:testDebugUnitTest :app:desktopTest` → `BUILD SUCCESSFUL in 41s`
- Manual checks: Verified repaired `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm` starts with `<map version="freeplane 1.12.15">` and no longer contains the dangling `<arrowlink DESTINATION="ID_1499691433" .../>` or leaked `<stylenode>` tags on the root node.

## Deviations from Assessment

- In `NodeManager.writeRawXmlToSerializer`, wrapped the fragment in a synthetic `<_fragment>...</_fragment>` root element when parsing with `XmlPullParser` (skipping the wrapper tag at depth 1) because `<hook NAME="MapStyle">` contains two top-level sibling elements (`<properties .../>` and `<map_styles>...</map_styles>`), which would otherwise fail a standalone `XmlPullParser` expecting a single root element.

## Follow-ups

- Run `/speckit-bug-test slug=freeplane-map-open-error` and verify opening `/Users/julienquievreux/SynologyDrive/document/perso/mobile/carte_cognitive/DroidPlane2map.mm` in Freeplane Desktop.
