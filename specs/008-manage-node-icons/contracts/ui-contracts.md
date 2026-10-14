# UI Contracts: Freeplane Node Icons

**Feature Branch**: `008-manage-node-icons`
**Created**: 2026-10-14
**Status**: Completed

## 1. Composable Components

### `NodeIconView`
Renders a single `NodeIconVisual`.

```kotlin
@Composable
fun NodeIconView(
    visual: NodeIconVisual,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    size: Dp = 16.dp
)
```
- **Vector**: Rendered via `Icon(visual.imageVector, contentDescription = contentDescription, modifier = modifier.size(size), tint = tint ?: LocalContentColor.current)`
- **Resource**: Rendered via `Icon(painterResource(visual.resId), contentDescription = contentDescription, modifier = modifier.size(size), tint = tint ?: Color.Unspecified)` (Note: Freeplane PNG drawables are multi-colored and should not be force-tinted unless specified).
- **Unmapped**: Rendered via a compact badge composable:
  ```kotlin
  Surface(
      shape = RoundedCornerShape(4.dp),
      color = MaterialTheme.colorScheme.surfaceVariant,
      modifier = modifier
  ) {
      Text(
          text = "[${visual.name}]",
          style = MaterialTheme.typography.labelSmall,
          fontSize = 10.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
      )
  }
  ```

---

### `NodeIconsRow`
Renders a sequence of icons with optional maximum count and overflow indicator.

```kotlin
@Composable
fun NodeIconsRow(
    iconNames: List<String>,
    modifier: Modifier = Modifier,
    maxInlineIcons: Int = Int.MAX_VALUE,
    iconSize: Dp = 16.dp,
    spacing: Dp = 4.dp
)
```
- **In `MindMapNodeCard`**:
  `maxInlineIcons = Int.MAX_VALUE` (all icons rendered).
- **In `NodeList`**:
  `maxInlineIcons = 3`. If `iconNames.size > 3`, renders the first 3 icons followed by an overflow indicator:
  ```kotlin
  Surface(
      shape = RoundedCornerShape(4.dp),
      color = MaterialTheme.colorScheme.primaryContainer
  ) {
      Text(
          text = "+${iconNames.size - 3}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onPrimaryContainer,
          modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
      )
  }
  ```

---

### `IconsMediaTab`
Inspector tab composable inside `NodePropertiesInspector`.

```kotlin
@Composable
fun IconsMediaTab(
    node: Node,
    onNodeChanged: (Node) -> Unit,
    modifier: Modifier = Modifier,
    registry: NodeIconRegistry = NodeIconRegistry
)
```
Structure:
1. **Active Icons Section**:
   - Title: "Assigned Icons (N)" + Clear All button if `iconNames.isNotEmpty()`.
   - List/Flow of active items with:
     - Visual preview (`NodeIconView`).
     - Name label.
     - Reorder buttons (Move Left / Move Right) if multiple items.
     - Remove button (`Close` icon).
2. **Icon Catalog Section**:
   - Title: "Icon Catalog"
   - Search bar (`OutlinedTextField` with clear and search icon).
   - Filter chips for `NodeIconCategory` (`ALL`, `STATUS`, `PRIORITY`, `EMOTION`, `ACTIONS_TOOLS`, `PEOPLE_OBJECTS`).
   - Grid of selectable icons (`NodeIconCard` / `AssistChip` with graphic + label).
   - Tapping an icon appends its ID to `node.iconNames`.
3. **External Object / Media Section**:
   - URI and Scale factor inputs (existing functionality preserved).

---

## 2. Registry Contract (`NodeIconRegistry`)

```kotlin
object NodeIconRegistry {
    /**
     * Resolves an icon name from a Freeplane document into a visual representation.
     * Handles case-insensitivity and punctuation normalization ('-' vs '_').
     * If no match is found, returns NodeIconVisual.Unmapped(rawName).
     */
    fun resolve(iconName: String): NodeIconVisual

    /**
     * Returns all registered icon descriptors.
     */
    fun getAllIcons(): List<NodeIconDescriptor>

    /**
     * Filters icons by category and optional text search query.
     */
    fun filterIcons(query: String = "", category: NodeIconCategory = NodeIconCategory.ALL): List<NodeIconDescriptor>
}
```
