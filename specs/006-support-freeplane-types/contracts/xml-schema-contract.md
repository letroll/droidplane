# XML Schema Contract: Freeplane `.mm` XML Format

**Feature Branch**: `006-support-freeplane-types`  
**Feature Directory**: `specs/006-support-freeplane-types/contracts`  
**Status**: Completed  

## Overview

This contract formally documents the XML tag structures, attribute keys, and nesting hierarchy expected by desktop Freeplane (`.mm` files) and implemented in Droidplane's XML parsing and serialization pipelines.

---

## 1. Document Root (`<map>`)

```xml
<map version="freeplane 1.2.0">
  <!-- Optional attribute registry -->
  <attribute_registry SHOW_ATTRIBUTES="selected|hide" RESTRICTED="true|false">
    <attribute_name NAME="Priority" MANUAL="true">
      <attribute_value VALUE="High"/>
      <attribute_value VALUE="Low"/>
    </attribute_name>
  </attribute_registry>

  <!-- Root node -->
  <node ID="ID_12345" TEXT="Root Topic" CREATED="1283093380553" MODIFIED="1419682526875">
    ...
  </node>
</map>
```

---

## 2. Node Element (`<node>`)

### Attributes
- `ID`: String (e.g., `ID_123456789`)
- `TEXT`: String (plain text label)
- `CREATED`: Unix timestamp in milliseconds (Long)
- `MODIFIED`: Unix timestamp in milliseconds (Long)
- `POSITION`: `"left"` or `"right"` (valid on first-level branches)
- `FOLDED`: `"true"` or `"false"`
- `STYLE`: Shape/Style (`"fork"`, `"bubble"`, `"as_parent"`, etc.)
- `COLOR`: Foreground text hex color (`#RRGGBB`)
- `BACKGROUND_COLOR`: Background fill hex color (`#RRGGBB`)
- `LINK`: External URL or internal fragment (`#ID_...`)
- `HGAP`, `VGAP`, `VSHIFT`: Integer layout adjustments

### Child Elements (Canonical Sequencing)

```xml
<node ID="ID_1" TEXT="Sample Node" COLOR="#003366" BACKGROUND_COLOR="#fff4cc" STYLE="bubble">
  <!-- 1. Edge to parent -->
  <edge COLOR="#003366" STYLE="bezier" WIDTH="2"/>

  <!-- 2. Font specification -->
  <font NAME="SansSerif" SIZE="12" BOLD="true" ITALIC="false"/>

  <!-- 3. Cloud grouping -->
  <cloud COLOR="#f0f0f0" SHAPE="ROUND_RECT" WIDTH="1"/>

  <!-- 4. Icons -->
  <icon BUILTIN="yes"/>
  <icon BUILTIN="priority-1"/>

  <!-- 5. Rich content: Node text -->
  <richcontent TYPE="NODE">
    <html><body><p>Formatted <b>Node</b> Text</p></body></html>
  </richcontent>

  <!-- 6. Rich content: Details -->
  <richcontent TYPE="DETAILS">
    <html><body><p>Secondary details content</p></body></html>
  </richcontent>

  <!-- 7. Rich content: Note annotation -->
  <richcontent TYPE="NOTE">
    <html><body><p>Extended note documentation</p></body></html>
  </richcontent>

  <!-- 8. Key-value attributes -->
  <attribute NAME="Status" VALUE="In Progress"/>
  <attribute NAME="Owner" VALUE="Alice"/>

  <!-- 9. Connector Arrow Links -->
  <arrowlink DESTINATION="ID_2" COLOR="#ff0000" STARTARROW="None" ENDARROW="Default" MIDDLE_LABEL="depends on"/>

  <!-- 10. Hooks (External objects, LaTeX, MapStyle, Scripts) -->
  <hook NAME="ExternalObject" URI="file:/path/to/diagram.png" SIZE="0.8"/>
  <hook NAME="plugins/latex/LatexNodeHook.properties" EQUATION="\sum_{i=1}^n i^2"/>

  <!-- 11. Nested Child Nodes -->
  <node ID="ID_1_1" TEXT="Subtopic">
    ...
  </node>
</node>
```

---

## 3. Round-Trip Preservation Rules

1. Any unrecognized tag or `<hook>` encountered during parsing is retained in memory with its raw attributes and contents.
2. During serialization, preserved hooks are written back into the node element in their original order.
3. Empty or null optional elements (e.g. no cloud, no details, no edge styling) MUST NOT be serialized as empty self-closing tags.
