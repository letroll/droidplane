# Bug Fix: Duplicate `NAME` attribute on `<hook>` XML elements

- **Slug**: duplicate-hook-name-attr
- **Fixed**: 2026-11-02
- **Assessment**: ./assessment.md
- **Status**: applied

## Summary

Filtered out the `NAME` attribute when populating `GenericHookElement.attributes` during XML parsing in `XmlParseUtilsDefaultImpl.parseHook`, and added a defensive check in `NodeManager.serializeNode` so `NAME` is never emitted twice on `<hook>` tags. Also included `genericHooks` in `Node.equals` and `Node.hashCode` for complete change detection.

## Changes

| File | Change | Notes |
|------|--------|-------|
| `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt` | modified | Skip `NAME` attribute (case-insensitive) when copying attributes into `GenericHookElement.attributes` in `parseHook`. |
| `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt` | modified | Guard against writing `NAME` a second time when iterating over `hook.attributes` in `serializeNode`. |
| `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/Node.kt` | modified | Added `genericHooks` comparison and hash in `Node.equals` and `Node.hashCode`. |
| `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt` | modified | Added assertions verifying `hook.attributes` does not contain `"NAME"` and each serialized `<hook>` tag has `NAME=` exactly once. |
| `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/HooksXmlTest.kt` | added test | Added unit test covering generic hook parsing, serialization guard when `"NAME"` is present in `attributes`, and single `NAME=` occurrence per `<hook>`. |

## Diff Highlights (optional)

```kotlin
// XmlParseUtilsDefaultImpl.kt
val attrs = mutableMapOf<String, String>()
for (i in 0 until xpp.attributeCount) {
    val attrName = xpp.getAttributeName(i)
    if (!attrName.equals(NodeAttribute.NAME.text, ignoreCase = true)) {
        attrs[attrName] = xpp.getAttributeValue(i)
    }
}
parent.addGenericHook(GenericHookElement(name = name, attributes = attrs))
```

```kotlin
// NodeManager.kt
serializer.nodeAttribute(NAME, hook.name)
hook.attributes.forEach { (k, v) ->
    if (!k.equals(NAME.text, ignoreCase = true)) {
        serializer.attribute(null, k, v)
    }
}
```

## Tests Added or Updated

- `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt::load complex map with desktop hooks, edit a node, and verify complete round-trip preservation` — Verifies that parsed `GenericHookElement.attributes` excludes `"NAME"` and that every serialized `<hook>` element contains `NAME=` exactly once.
- `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/HooksXmlTest.kt::generic hooks should not emit duplicate NAME attributes across multiple round-trips or when attributes map contains NAME` — Verifies both the parser exclusion of `NAME` and the serializer's defensive guard when `GenericHookElement.attributes` manually contains `"NAME"`.

## Local Verification

- Commands run: `./gradlew :data:testDebugUnitTest --tests "fr.julien.quievreux.droidplane2.data.HooksXmlTest" --tests "fr.julien.quievreux.droidplane2.data.FreeplaneXmlRoundTripTest" --tests "fr.julien.quievreux.droidplane2.data.NodeManagerUpdateTest"` → `BUILD SUCCESSFUL` (all tests passed).
- Manual checks: Inspected serialized `<hook>` tags in test assertions via regex `<hook\b[^>]*>` to confirm `NAME=` count is strictly `1`.

## Deviations from Assessment

None.

## Follow-ups

- For `.mm` files that were already saved with duplicate `NAME` attributes on disk, opening and re-saving them in DroidPlane (or removing the duplicate `NAME="..."` attribute once) will restore compatibility with Freeplane Desktop.
