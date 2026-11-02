# Bug Assessment: Duplicate `NAME` attribute on `<hook>` XML elements

- **Slug**: duplicate-hook-name-attr
- **Created**: 2026-11-02
- **Source**: pasted text
- **Verdict**: valid
- **Severity**: high

## Report (verbatim or summarized)

> j'ai régulièrement mes cartes mentales qui deviennent illisible dans freeplane car son xml à la balise hook qui a plusieurs fois l'attribut NAME. Je suppose qu'un bug du projet le dédouble.

## Symptom

When a mindmap containing generic `<hook>` elements (e.g., `<hook NAME="MapStyle">`, `<hook NAME="AutomaticEdgeColor" .../>`, or `<hook NAME="accessories/plugins/AutomaticLayout.properties"/>`) is loaded and then saved by DroidPlane, the resulting `.mm` XML file contains `<hook>` tags with the `NAME` attribute emitted twice (e.g., `<hook NAME="MapStyle" NAME="MapStyle">`). This produces malformed XML (violating the W3C XML specification's Unique Att Spec constraint), causing Freeplane desktop to fail when parsing and opening the mindmap.

## Reproduction

1. Load a Freeplane `.mm` file that contains at least one generic `<hook NAME="..."/>` element (any hook other than `ExternalObject` or a `latex` hook, such as `MapStyle` or `AutomaticEdgeColor`).
2. Save / serialize the mindmap via `NodeManager.serializeMindmap` (or `serializeMindmapToOutputStream`).
3. Inspect the serialized `.mm` XML or open it in Freeplane desktop: every generic `<hook>` tag has `NAME="..."` written twice.

## Suspected Code Paths

- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt:158-179` (`parseHook`) — Extracts `name` from `NAME` attribute (`line 161`), and in the `else` branch (`lines 173-177`) iterates over `0 until xpp.attributeCount` and copies **all** XML attributes (including `NAME`) into `GenericHookElement.attributes`.
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt:637-648` (`serializeNode`) — Writes `serializer.nodeAttribute(NAME, hook.name)` (`line 641`) and then iterates over `hook.attributes` (`lines 642-644`) calling `serializer.attribute(null, k, v)` for every entry without filtering out `NAME`, resulting in a duplicate `NAME` attribute whenever `hook.attributes` also contains `"NAME"`.
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/Node.kt:77-142` (`equals` / `hashCode`) — Secondary observation: `genericHooks` is missing from `Node.equals` and `Node.hashCode`, which could prevent change detection when only `genericHooks` are modified on a node.

## Root Cause Hypothesis

**Confidence: high.**
During XML parsing in `XmlParseUtilsDefaultImpl.parseHook`, generic hooks store their `NAME` attribute in `GenericHookElement.name`, **and** the loop over `0 until xpp.attributeCount` also inserts `"NAME" -> value` into the `attributes: Map<String, String>` map. Later, when `NodeManager.serializeNode` serializes each `GenericHookElement`, it explicitly writes `serializer.nodeAttribute(NAME, hook.name)` first and then writes all key-value pairs from `hook.attributes` unconditionally. Because `XmlSerializer` (KXmlSerializer) does not deduplicate attributes on `attribute(null, k, v)` calls, `NAME="..."` is emitted twice on the same `<hook>` start tag. Existing round-trip tests (`FreeplaneXmlRoundTripTest`) only checked `savedXml shouldContain "NAME=\"MapStyle\""` and did not assert that `NAME=` appears only once per `<hook>` element or that the saved XML can be re-parsed by a strict XML parser.

## Proposed Remediation

**Preferred**:
1. In `XmlParseUtilsDefaultImpl.parseHook` (`lines 174-176`), skip the `NAME` attribute (`NodeAttribute.NAME.text`) when populating the `attrs` map for `GenericHookElement`, so `GenericHookElement.attributes` only holds additional attributes beyond `NAME`.
2. In `NodeManager.serializeNode` (`lines 642-644`), defensively filter out `NodeAttribute.NAME.text` (or case-insensitive `"NAME"`) when iterating over `hook.attributes` so that even if a `GenericHookElement` was constructed in memory with `"NAME"` inside `attributes`, it can never emit a duplicate `NAME` attribute in XML.
3. Also add `if (genericHooks != other.genericHooks) return false` and `result = 31 * result + genericHooks.hashCode()` in `Node.kt` (`equals` and `hashCode`) to ensure hook edits in `ScriptsTab` are properly detected.

**Alternatives** (optional):
- Only filter `NAME` during serialization in `NodeManager.serializeNode` while leaving `"NAME"` in `GenericHookElement.attributes`. Trade-off: if the user renames the hook in `ScriptsTab` (`hook.copy(name = newName)`), `hook.attributes["NAME"]` would remain stale and diverge from `hook.name`. Stripping `NAME` at parse time *and* guarding at serialization time avoids both issues.

**Files likely to change**:
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/XmlParseUtilsDefaultImpl.kt`
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/NodeManager.kt`
- `data/src/commonMain/kotlin/fr/julien/quievreux/droidplane2/data/model/Node.kt`
- `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/FreeplaneXmlRoundTripTest.kt`
- `data/src/commonTest/kotlin/fr/julien/quievreux/droidplane2/data/HooksXmlTest.kt`

**Tests to add or update**:
- Update `FreeplaneXmlRoundTripTest` and/or `HooksXmlTest` to verify that:
  1. Parsed `GenericHookElement.attributes` does not duplicate `NAME`.
  2. Serialized `<hook>` elements contain `NAME=` exactly once per `<hook>` tag (e.g., regex check or counting occurrences per line/tag, and ensuring `<hook NAME="X" NAME="X"` never appears).
  3. A full round-trip (load -> save -> load again) succeeds and does not accumulate attributes across multiple saves.

## Risks & Considerations

- Existing `.mm` files already corrupted by previous saves have duplicate `NAME="..."` attributes in their XML on disk. Depending on how lenient `XmlPullParser` (`KXmlParser`) is when opening already-corrupted files in DroidPlane, re-saving them once the fix is applied will clean them up if `KXmlParser` allows reading them, or users may need a way to strip duplicate `NAME` attributes from already-damaged `.mm` files.
- Minimal risk of regression: `GenericHookElement.name` is already the canonical property used everywhere in the UI (`ScriptsTab.kt`) and tests (`FreeplaneXmlRoundTripTest.kt`).

## Open Questions

- None — the root cause is deterministic and directly visible in `XmlParseUtilsDefaultImpl.kt` and `NodeManager.kt`.
