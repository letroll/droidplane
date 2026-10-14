# Research & Architectural Decisions: Freeplane Node Icons

**Feature Branch**: `008-manage-node-icons`
**Created**: 2026-10-14
**Status**: Completed

## 1. Icon Representation & Asset Resolution

### Decision
Implement a centralized, compile-time `NodeIconRegistry` in the `:app` presentation layer that maps Freeplane built-in icon identifiers (and known aliases) to a `NodeIconVisual` sealed interface:
```kotlin
sealed interface NodeIconVisual {
    data class Vector(val imageVector: ImageVector) : NodeIconVisual
    data class Resource(@DrawableRes val resId: Int) : NodeIconVisual
    data class Unmapped(val name: String) : NodeIconVisual
}
```
All 81 legacy Freeplane drawables (`icon_*.png`) and existing Compose icon sets (`compose.icons:tabler-icons` and `compose.icons:font-awesome`) will be statically registered in a lookup table.

### Rationale
- **Performance & Purity**: Replaces legacy runtime reflection (`context.resources.getIdentifier("@drawable/$drawableName", ...)`) which ran inside Compose rendering loops and required `LocalContext.current`.
- **Testability**: Pure JVM testable without Android `Context` or Robolectric.
- **SOLID Compliance**: Single responsibility for icon resolution and presentation descriptors.

### Alternatives Considered
- *Keep runtime reflection via `getIdentifier`*: Rejected because it incurs reflection overhead during virtualized list/canvas scrolling and fails if resource naming patterns change.
- *Strict Enum only*: Rejected because Freeplane allows arbitrary string identifiers in `<icon BUILTIN="..."/>`; an open registry with unmapped fallback accommodates dynamic or unexpected icon names gracefully.

---

## 2. Unmapped / Missing Freeplane Icons Strategy

### Decision
As resolved via user choice (**Option B**), when an icon name found in a mindmap does not match any registered asset or drawable:
- Render a compact visual badge containing the exact icon name (e.g. `[clanbomber]`, `[custom_tag]`) using a subtle container with monospace/caption text.
- Preserve the exact raw string in the node's `iconNames` collection and document XML when saving.

### Rationale
- Guarantees transparency: users know an icon exists even if mobile doesn't have the graphic.
- Zero data loss: desktop Freeplane continues to display the icon when the file is transferred back.
- Directly satisfies user requirement.

### Alternatives Considered
- *Option A (Generic placeholder glyph)*: Obscures which specific icon was missing.
- *Option C (Silent hiding)*: Users might assume the icon was deleted or lost.

---

## 3. Node List View (`NodeList`) Icon Display Constraints

### Decision
As resolved via user choice (**Option B**), in the compact hierarchical `NodeList` row:
- Render up to 3 icons inline at a compact size (16–18 dp).
- If a node has more than 3 icons, render the first 3 followed by an overflow indicator badge `+N` (e.g., `+2` for 5 icons).
- Clearly separate functional indicators (hyperlink icon, rich-text marker, note badge) from user-assigned Freeplane icons.

### Rationale
- Prevents row clutter on narrow mobile screens (320–400 dp width).
- Leaves ample space for the node text label while signaling that extra icons exist.
- Consistent visual hierarchy between user-assigned icons and system metadata badges.

### Alternatives Considered
- *Option A (All icons inline)*: Causes line wrapping or pushes node text off-screen when nodes have 5+ icons.
- *Option C (Hard truncation to 1 or 2 icons without indicator)*: Users would have no indication that additional icons are attached.

---

## 4. Inspector Icon Catalog Organization & Browsing

### Decision
As resolved via user choice (**Option C**), organize the icon catalog in `IconsMediaTab` as a hybrid browser:
- **Search Bar**: Real-time substring and alias filter by icon name (e.g. searching "flag", "ok", "yes", "1").
- **Category Filter Chips**: Single-select category chips:
  - `ALL` (All 80+ icons)
  - `STATUS` (Yes, OK, Cancel, Stop, Checked, Unchecked, Flags...)
  - `PRIORITY` (Numbers 0–9, Priorities 1–9, Percentages 0–100%...)
  - `EMOTION` (Smilies, positive, negative, neutral...)
  - `ACTIONS_TOOLS` (Pencil, Edit, Attach, Link, Bookmark, Help, Idea, Calendar, Clock...)
  - `PEOPLE_OBJECTS` (Family, Male, Female, Group, Penguin, Bee...)
- **Grid Layout**: Virtualized or flow grid displaying icon graphic and label, optimized for touch targets (minimum 44x44 dp).

### Rationale
- Caters to both structured category browsing and fast instant searching.
- Supports intuitive discovery for first-time mobile users.

### Alternatives Considered
- *Option A (Categories only)*: Slower for users who know the exact icon they want.
- *Option B (Search bar only)*: Hard to discover available icons without typing.

---

## 5. Active Icon Management & Reordering

### Decision
In `IconsMediaTab`:
- Display active icons as structured cards or chips with:
  - Visual preview of the icon (graphic or textual badge for unmapped).
  - Icon identifier name.
  - Left / Right (or Up / Down) shift buttons to adjust sequence.
  - Delete button (`Close` icon) to remove the icon.
- A "Clear all icons" action button when `node.iconNames.isNotEmpty()`.

### Rationale
- Freeplane nodes support ordered icons; maintaining and adjusting sequence is a core Freeplane feature.
- Explicit buttons are more reliable and accessible on touch screens than drag-and-drop on small chips.
