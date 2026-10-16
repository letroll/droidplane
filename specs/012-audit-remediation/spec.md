# Feature Specification: Codebase Audit Remediation & Quality Hardening

**Feature Branch**: `012-audit-remediation`

**Created**: 2026-10-16

**Status**: Draft

**Input**: User description: "prend en compte les recommandations suivantes en vérifiants au fur et à mesure leur pertinance : 17 audit recommendations covering production APK hygiene, XML serialization/parsing data integrity, memory safety, off-main-thread file I/O, node tree state consistency, 2D layout performance, relative file URI sharing, and dead code/dependency cleanup."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Lightweight, Secure Production Application Package (Priority: P1)

As an end user installing and launching the application on my mobile device, I want the distributed application package to contain only production code and essential assets—without automated testing tools, mock data generators, or unused third-party libraries—and to be shrunk and obfuscated for release so that downloads are small, cold starts are fast, and debug/test instrumentation is never exposed in production.

**Why this priority**: Shipping test frameworks and fake data inside a non-minified production package inflates download size, slows application startup, and exposes test/debug instrumentation to end users.

**Independent Test**: Build a release distribution package and inspect its contents and size to verify that zero test framework artifacts, mock data sources, or unused legacy dependencies are included, while all automated test suites continue to execute and pass in test builds.

**Acceptance Scenarios**:

1. **Given** a production release build of the application, **When** the package contents are inspected, **Then** no automated testing libraries, mock generators, or test-only utilities are packaged in the production binary.
2. **Given** a production release build of the application, **When** code shrinking and resource optimization are enabled, **Then** unused code and resources are stripped without breaking dependency injection, screen navigation, or mind map loading/saving.
3. **Given** the project's automated unit and instrumentation test suites, **When** tests are executed across all modules, **Then** shared test helpers and fake data sources remain accessible to test targets and 100% of tests pass.

---

### User Story 2 - Corruption-Free Mind Map Saving and Rich Content Fidelity (Priority: P1)

As a user editing and saving mind maps that contain formatted notes, technical snippets, or mathematical comparisons, I want my files to be validated before saving, written atomically without altering literal text sequences (such as `]]>` or `<![CDATA[`), and preserved with valid rich-text entity encoding so that my notes and mind maps are never silently corrupted on save.

**Why this priority**: Data integrity is a core requirement; silent text replacement across the saved file or unescaped special characters in rich content can corrupt user knowledge bases and lose formatting on reload.

**Independent Test**: Load and save a mind map containing nodes and rich-text notes with literal `<![CDATA[`, `]]>`, `<`, `>`, `&`, and `"` characters, as well as invalid save destinations, and verify that valid saves preserve 100% of text and formatting in a single atomic write while invalid destinations abort immediately with an error notification.

**Acceptance Scenarios**:

1. **Given** a mind map node or note containing literal text sequences such as `<![CDATA[` or `]]>`, **When** the user saves the mind map and reloads it, **Then** the literal characters are preserved verbatim without being stripped.
2. **Given** a rich-text node, details block, or note containing special characters (`<`, `>`, `&`, `"`), **When** the mind map is loaded and saved back to disk, **Then** all rich-text formatting and special characters survive the round trip intact.
3. **Given** an invalid or empty target file path or file name, **When** a save operation is requested, **Then** the system reports the error immediately, aborts the write operation before touching storage, and does not emit a save-completed event.

---

### User Story 3 - Responsive UI and Leak-Free Device Rotation During File Operations (Priority: P2)

As a user opening, saving, or rotating my device while working on a mind map—including files stored on cloud-backed document providers—I want all file checks, saves, and exit confirmations to run smoothly without freezing the screen, leaking memory across screen rotations, or crashing when opening linked relative files.

**Why this priority**: Performing storage I/O on the main user interface thread causes visible freezes and "Application Not Responding" (ANR) dialogs, while retaining screen references across configuration changes causes memory leaks and crashes.

**Independent Test**: Rotate the device repeatedly before and during file save operations, launch the app with multiple recent files, save a large mind map to a document provider, press Back with unsaved changes, and open a relative file link on modern OS versions to confirm zero memory leaks, zero main-thread I/O stalls, and crash-free external file launching.

**Acceptance Scenarios**:

1. **Given** an active mind map session, **When** the user rotates the device multiple times and triggers a file save, **Then** the previous screen instance is garbage-collected without memory leaks and the save prompt or completion targets the active screen cleanly.
2. **Given** a large mind map or a slow external storage provider, **When** the user saves the file or launches the app with recent files, **Then** all file verification and streaming writes occur in the background while the interface remains responsive.
3. **Given** an open mind map with unsaved changes, **When** the user triggers the exit action, **Then** the unsaved-changes confirmation is triggered once as a controlled side-effect rather than repeatedly during screen redraws.
4. **Given** a node referencing a relative local file, **When** the user opens the linked file, **Then** the system shares access via a secure content permission URI rather than a raw file path, opening the external viewer without crashing.

---

### User Story 4 - Consistent Deep-Tree Editing, Fast Large-Map Loading, and Smooth Selection (Priority: P2)

As a user navigating, searching, and editing deep or large mind maps (thousands of nodes), I want node style changes and ancestor updates to propagate accurately to all descendants, large maps to load in linear time, node selection on the 2D canvas to respond instantaneously without recalculating the entire map layout, and live search to report accurate match counts without diagnostic log spam.

**Why this priority**: Stale parent references break upward navigation in deep trees, missing style comparisons prevent visual updates, quadratic list copying slows large file loads, and redundant layout calculations cause lag on every tap.

**Independent Test**: Load a 2,000+ node mind map, edit styling (font size, bold, italic, fold state, formula) on a node, edit an ancestor node and navigate upward from a deep descendant, tap different nodes on the 2D canvas, and type a search query to verify linear load time, instant selection, accurate ancestor navigation, and accurate search result logging.

**Acceptance Scenarios**:

1. **Given** a node in a mind map, **When** only its visual styling (font size, font family, bold, italic, folded state, formula, or external object) is modified, **Then** the system detects the state change and updates the visual display immediately.
2. **Given** a mind map with at least four levels of hierarchy, **When** a top-level or intermediate ancestor node is modified, **Then** all deep descendants (grandchildren and beyond) reference the updated ancestor when navigating upward.
3. **Given** a mind map rendered on the 2D visual canvas, **When** the user taps to change the currently selected node without altering node content or collapse state, **Then** selection highlights update immediately without recomputing node positions or subtree dimensions.
4. **Given** a mind map with 2,000 or more nodes, **When** the file is loaded from storage, **Then** the node collection is published once upon completion rather than copying the full list on every parsed node.
5. **Given** a user typing a search query, **When** search results are updated, **Then** no debug placeholder error logs are emitted and any diagnostic count reflects the newly computed matches.

---

### User Story 5 - Clean, Maintainable Codebase Free of Obsolete Legacy Artifacts (Priority: P3)

As a maintainer evolving the application, I want obsolete screen controllers, unused layout resources, dead helper methods, duplicate imports, and hardcoded version fallbacks removed so that the codebase has a single clear architecture and minimal maintenance overhead.

**Why this priority**: Dead classes, unused XML layouts, and unreferenced dependencies confuse contributors, increase build times, and violate single-responsibility and simplicity principles.

**Independent Test**: Verify that all identified dead files, unreferenced methods, unused dependencies, and redundant configurations are removed and that the project compiles, passes static analysis, and passes all unit tests cleanly.

**Acceptance Scenarios**:

1. **Given** the repository source tree, **When** unused legacy classes, duplicate UI models, obsolete XML layouts, and dead utility functions are removed, **Then** the project builds cleanly with zero unresolved references.
2. **Given** the application version display and build configuration, **When** version metadata and library bill-of-materials are resolved, **Then** they use the centralized project configuration rather than hardcoded fallback strings or duplicate version numbers.

---

### Edge Cases

- What happens when a user saves a node whose text or rich-text note literally contains markup sequences like `<![CDATA[some code]]>` or `a < b & c > d`? The content is escaped and saved cleanly without stripping literal substrings or breaking document structure.
- What happens when mind map saving is requested with a blank path, a relative path without a leading directory separator, or a filename missing the `.mm` extension? Validation fails immediately, invokes the error callback once, and returns before creating any temporary or target file.
- What happens when the user rotates the screen while a background save is completing? The save completion event is held in observable state and handled safely by the newly attached screen without referencing the destroyed screen instance.
- What happens when a recent file reference in the startup list points to an unreachable or deleted cloud document? The availability check runs asynchronously off the main thread so startup rendering is never blocked, and unreachable entries are pruned cleanly.
- What happens when a user taps a node link pointing to a relative local file on disk? A secure, permission-granted content reference is provided to the external viewer app, avoiding file-exposure security crashes.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Production application builds MUST exclude all automated testing frameworks, assertion libraries, mocking agents, and fake test data sources from the packaged release binary while keeping shared test utilities available to unit and instrumentation test suites.
- **FR-002**: Production release builds MUST enable code shrinking and resource optimization with appropriate retention rules so that dependency wiring, screen rendering, and mind map saving/loading function identically to development builds.
- **FR-003**: Mind map saving MUST validate target directory paths and filenames prior to file creation and MUST return immediately upon validation failure or write failure without invoking completion callbacks.
- **FR-004**: Mind map saving MUST write atomically via a temporary staging file and replace the destination file directly without performing a second line-by-line pass that strips `<![CDATA[` or `]]>` substrings from user content.
- **FR-005**: Rich-content markup reconstruction during mind map loading MUST preserve special character entity escaping (`<`, `>`, `&`, `"`) and build content in linear time without repeated string reallocation.
- **FR-006**: The main screen state holder MUST NOT hold a direct reference to the visual screen instance for launching file save pickers; file save requests MUST be communicated through observable UI state or events.
- **FR-007**: Writing a saved mind map to a user-selected document destination MUST stream bytes on a background I/O thread without loading the entire file into memory at once on the main UI thread.
- **FR-008**: Validating the existence and metadata of recent files at application startup MUST execute asynchronously on a background I/O thread so the initial screen renders without main-thread storage reads.
- **FR-009**: Handling application exit confirmation when leaving the screen MUST execute inside a lifecycle-aware side-effect block rather than mutating dialog state or closing the screen directly during UI rendering.
- **FR-010**: Node equality and hash calculation MUST account for all user-editable visual and structural attributes (including font size, font family, bold, italic, folded state, formula, and external object) so visual state updates are never skipped.
- **FR-011**: Updating an ancestor node in the mind map tree MUST recursively update parent references across all depths of descendants (grandchildren and deeper) so upward tree traversal remains consistent.
- **FR-012**: Loading a mind map file MUST accumulate parsed nodes in a local collection and publish the full node list once upon completion rather than copying and emitting the entire node list on every parsed node.
- **FR-013**: The 2D mind map layout engine MUST memoize subtree height calculations per layout pass, and changing the selected node ID on the 2D canvas MUST NOT trigger a full layout recomputation of node coordinates.
- **FR-014**: Opening a relative file linked from a node MUST generate a secure, permission-granted content reference instead of exposing a raw filesystem path.
- **FR-015**: Live search filtering MUST remove placeholder error logging (`"toto"`) and MUST log or report match counts derived from the current filter computation rather than a stale state value.
- **FR-016**: Unused third-party dependencies, obsolete state holders, duplicate UI node models, unused layout files, dead permission helpers, unreferenced search/recursion methods, duplicate imports, invalid lint suppressions, and hardcoded version/library strings MUST be removed or consolidated into the centralized version catalog.

### Key Entities

- **Node**: Represents a single topic in the mind map hierarchy, including its text, rich content (node, details, note), visual styling (colors, font attributes, cloud, edge, icons), fold state, formula, external object, parent reference, and child list.
- **MindMapLayout**: Represents the computed 2D spatial coordinates, dimensions, connectors, and bounding boxes for all visible nodes in an expanded mind map tree.
- **RecentFileEntry**: Represents a recently accessed mind map document reference and its display metadata, verified asynchronously against device or cloud storage.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Production release packages contain 0 test-only libraries or fake data generators, and release code shrinking reduces compiled application code footprint without runtime regressions.
- **SC-002**: 100% of mind maps containing literal `<![CDATA[`, `]]>`, `<`, `>`, `&`, or `"` sequences in node text or rich-text notes preserve their exact content and formatting across save-and-reload round trips.
- **SC-003**: 0 bytes of disk or document-provider I/O are read or written on the main UI thread during startup recent-file checks or document save operations.
- **SC-004**: Rotating the device during or between mind map sessions retains 0 destroyed screen instances in the main state holder.
- **SC-005**: Loading a mind map with N nodes performs 1 final state publication of the complete node list instead of N intermediate list copies, and selecting a node on the 2D canvas triggers 0 full-tree layout coordinate recalculations.
- **SC-006**: 100% of identified dead source files (6 obsolete source/layout files) and 7 unused library catalog entries are removed while 100% of automated tests pass.

## Assumptions

- Shared test base classes and fake test data are still required by existing automated tests across modules, so they will be relocated to test-only source sets or test fixtures rather than deleted.
- Existing Freeplane `.mm` files and standard platform file-sharing providers are used for local file storage and external viewer launching.
- Enabling release code shrinking applies to the production release build while keeping development builds fast for local iteration.
