# Data Model: Freeplane Node Icons

**Feature Branch**: `008-manage-node-icons`
**Created**: 2026-10-14
**Status**: Completed

## 1. Domain Entities (`:data` module)

The domain entity already exists and remains unchanged to preserve backward compatibility and Principle IV (Data Integrity & Format Compatibility).

### `Node`
Represents an individual node in the Freeplane mindmap hierarchy.
- **`iconNames`**: `MutableList<String>`
  - Ordered collection of raw Freeplane icon names as read from or written to `<icon BUILTIN="..."/>`.
  - Preserved verbatim during reading, editing, and writing.
  - Examples: `["button_ok", "flag-red", "priority-1", "clanbomber"]`.

---

## 2. Presentation Models (`:app` module)

### `NodeIconVisual` (Sealed Interface)
Represents the visual representation of an icon for rendering in Jetpack Compose.

```kotlin
sealed interface NodeIconVisual {
    /** Renders via androidx.compose.material3.Icon with ImageVector */
    data class Vector(val imageVector: ImageVector) : NodeIconVisual

    /** Renders via androidx.compose.ui.res.painterResource(@DrawableRes resId) */
    data class Resource(@DrawableRes val resId: Int) : NodeIconVisual

    /** Fallback rendering: a compact textual badge displaying [name] */
    data class Unmapped(val name: String) : NodeIconVisual
}
```

### `NodeIconCategory` (Enum)
Thematic groupings for organizing the icon catalog.

```kotlin
enum class NodeIconCategory(val label: String) {
    ALL("All"),
    STATUS("Status & Flags"),
    PRIORITY("Priorities & Numbers"),
    EMOTION("Emoticons"),
    ACTIONS_TOOLS("Actions & Tools"),
    PEOPLE_OBJECTS("People & Objects")
}
```

### `NodeIconDescriptor`
Metadata describing an available icon in the catalog.

| Field | Type | Description |
|---|---|---|
| `id` | `String` | Canonical Freeplane built-in identifier (e.g. `"button_ok"`, `"flag-red"`) |
| `visual` | `NodeIconVisual` | Associated visual graphic (Vector or Drawable Resource) |
| `label` | `String` | Human-readable name displayed in the catalog |
| `category` | `NodeIconCategory` | Category for filtered browsing |
| `aliases` | `List<String>` | Variations and alternative names for search/matching (e.g. `["flag_red", "flag-red"]`) |

---

## 3. Operations & State Transitions

### Reordering Icons on a Node
When a user moves an icon at index `fromIndex` to index `toIndex`:
1. Validate `fromIndex` and `toIndex` are in `0 until node.iconNames.size`.
2. Produce a new immutable copy of the list:
   ```kotlin
   val updated = node.iconNames.toMutableList()
   val item = updated.removeAt(fromIndex)
   updated.add(toIndex, item)
   onNodeChanged(node.copy(iconNames = updated))
   ```

### Adding an Icon to a Node
1. Append the selected icon's canonical `id` to `node.iconNames`:
   ```kotlin
   val updated = node.iconNames.toMutableList()
   updated.add(descriptor.id)
   onNodeChanged(node.copy(iconNames = updated))
   ```

### Removing an Icon from a Node
1. Remove the icon at specific index:
   ```kotlin
   val updated = node.iconNames.toMutableList()
   updated.removeAt(index)
   onNodeChanged(node.copy(iconNames = updated))
   ```

### Clearing All Icons
1. Clear the collection:
   ```kotlin
   onNodeChanged(node.copy(iconNames = mutableListOf()))
   ```
