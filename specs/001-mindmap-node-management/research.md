# Phase 0 Research: Mindmap Viewing & Node Management

## 1. State Management & Node Synchronization Architecture

### Decision
Move all node mutations (addition, text editing, deletion, and reparenting) exclusively into `NodeManager` within the `:data` module as the single source of truth. `MainViewModel` in `:app` delegates all mutation operations to `NodeManager` and observes/refreshes immutable UI states.

### Rationale
- **Eliminate Divergent State**: Previously, `MainViewModel` attempted to manually patch parent-child references and called `updateRootNode(newParentNode)` on child nodes, destroying the global node index and disconnecting child views from parent listings.
- **Constitutional Alignment**: Adheres directly to Principle I (Modular Separation of Concerns) and Principle II (Unidirectional Data Flow & State Consistency). Business logic for tree manipulation belongs in `:data`, while `:app` observes state.
- **Reusability and Testability**: Centralizing mutation in `NodeManager` allows comprehensive JVM unit tests using Kotest and MockK without relying on Android framework classes or Jetpack Compose UI test scopes.

### Alternatives Considered
- **Direct in-place mutation in ViewModel**: Rejected because it causes UI desynchronization, violates clean architecture boundaries, and duplicates tree-traversal logic.
- **Full mindmap re-parsing on every edit**: Rejected because re-parsing XML on each keystroke or save is computationally prohibitive for maps with thousands of nodes ($O(N)$ XML parsing latency).
- **Relational database cache (Room/SQLite)**: Rejected per YAGNI (Principle V). The Freeplane XML document is already loaded into an indexed in-memory graph (`MindmapIndexes` with `nodesByIdIndex` and `nodesByNumericIndex`). Storing in SQLite adds massive boilerplate with zero user benefit for single-document workflows.

---

## 2. Hierarchy Navigation & History Management

### Decision
Model navigation using a stack of node identifiers (`List<String>`) in `MainViewModel`. Provide three explicit navigation intents:
1. `onChildNodeSelected(nodeId: String)`: Push child `nodeId` onto navigation stack and set as `nodeCurrentlyDisplayed`.
2. `navigateUp()`: Pop the current node; display the previous node (parent).
3. `navigateToTop()`: Reset the stack to only `[rootNode.id]` and display the root.

### Rationale
- **Deterministic Traversal**: A simple immutable stack guarantees predictable back-navigation without getting trapped in cycles (e.g., from internal node links or search jumps).
- **Fast Leaf Node Handling**: Directly queries `nodeManager.getNodeByID(id)`, enabling instantaneous transitions (< 50ms) well below the 100ms threshold in SC-001.

### Alternatives Considered
- **Pointer-only traversal via `node.parentNode`**: Fragile if parent pointers become stale during tree mutations or when following cross-branch internal links.
- **Android Jetpack Navigation Component per node**: Overkill and ill-suited for arbitrary recursive tree graphs; Jetpack Navigation routes are intended for top-level screen destinations, not individual mindmap node items.

---

## 3. Search Engine & Match Highlighting

### Decision
Retain `SearchManager` inside `:data`, driven by `NodeManager._allNodes` StateFlow. Search executes case-insensitively across all indexed nodes matching titles, descriptions, and cleaned rich-text HTML. Results are exposed as a StateFlow list of matching nodes, with `MainViewModel` tracking `currentMatchIndex`.

### Rationale
- **Sub-second Response**: In-memory linear scan over 2,000 nodes using pre-indexed text fields completes in under 20ms on modern mobile CPUs, satisfying SC-002 (< 500ms).
- **Clean UI Separation**: The UI in `AppTopBar` only renders query inputs and next/previous controls, while `SearchManager` handles filtering.

### Alternatives Considered
- **External indexing library (Lucene / Full-Text Search)**: Violates Principle V (Simplicity / YAGNI). In-memory string matching is more than fast enough for typical mindmap sizes (< 10,000 nodes).

---

## 4. Freeplane XML Serialization & Data Preservation

### Decision
Preserve all attributes, formatting tags (`<font>`, `<icon>`, `<richcontent>`, `<arrowlink>`), and hierarchy during serialization in `NodeManager.serializeMindmap()`. Ensure atomic writes by serializing first to a temporary file (`temp_[filename]`), stripping raw CDATA wrappers if necessary, and atomically renaming/moving to target destination.

### Rationale
- **Data Loss Prevention**: Fulfills Principle IV (Freeplane Format Compatibility & Data Integrity) and SC-004 / SC-006. If a write is interrupted by an Android process kill or low battery, the original `.mm` file remains intact.
- **Desktop Interoperability**: Retains node IDs (`ID_...`), timestamps (`CREATED`, `MODIFIED`), and positioning attributes (`POSITION`) exactly matching desktop Freeplane conventions.

### Alternatives Considered
- **In-place file overwrite**: Rejected because an unhandled exception or abrupt app termination mid-write results in an unrecoverable corrupted XML document.

---

## 5. UI Rendering & Virtualization (Jetpack Compose)

### Decision
Render child node lists using `LazyColumn` with stable keys (`key = { it.id }`) in `NodeList.kt`. Each node item is represented by `Cell.kt`, displaying custom text typography (bold/italic), icon badges, chevron indicators, and context menus (Edit, Copy, Link).

### Rationale
- **Rendering Performance**: Virtualization ensures only currently visible nodes are drawn, supporting smooth 60/120 fps scrolling on long lists.
- **Composable Decoupling**: Reusable `Cell` composable accepts pure data and lambda callbacks, avoiding direct references to ViewModels.

### Alternatives Considered
- **Standard `Column` with `verticalScroll`**: Fails performance requirements when a node has hundreds of direct children, causing UI stutter during layout passes.
