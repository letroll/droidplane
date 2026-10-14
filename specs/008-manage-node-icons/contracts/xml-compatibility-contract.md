# XML Compatibility Contract: Freeplane Node Icons

**Feature Branch**: `008-manage-node-icons`
**Created**: 2026-10-14
**Status**: Completed

## 1. Freeplane `.mm` XML Format Specification

In the Freeplane XML dialect, icons attached to a node are serialized as child `<icon>` tags immediately within `<node>`:

```xml
<node TEXT="Task Item" ID="ID_123456789">
    <icon BUILTIN="button_ok"/>
    <icon BUILTIN="flag-red"/>
    <icon BUILTIN="priority-1"/>
</node>
```

### Attributes
- `BUILTIN`: Required attribute specifying the built-in icon identifier string.

---

## 2. Reading Contract (`:data` module)

In `XmlParseUtilsDefaultImpl.kt`:
```kotlin
override fun parseIcon(xpp: XmlPullParser, nodes: MutableList<Node>) {
    check(nodes.isNotEmpty()) { "Received icon without a parent node" }
    xpp.getNodeAttribute(BUILTIN)?.let { iconName ->
        parentNode.addIconName(iconName)
    }
}
```
- Icon tags are read sequentially and appended in the order they appear in the XML.
- Unknown, custom, or unmapped icon names are parsed verbatim without error or filtering.

---

## 3. Writing Contract (`:data` module)

In `NodeManager.kt`:
```kotlin
if (node.iconNames.isNotEmpty()) {
    node.iconNames.forEach { iconName ->
        serializer.startNodeTag(ICON)
        serializer.nodeAttribute(BUILTIN, iconName)
        serializer.endNodeTag(ICON)
    }
}
```
- All icon names in `node.iconNames` are written out sequentially under `<node>`.
- No modification, normalization, or casing changes are applied during serialization: the string stored in `node.iconNames` is written exactly as-is.
- Preserves 100% round-trip fidelity between desktop Freeplane and Droidplane.
