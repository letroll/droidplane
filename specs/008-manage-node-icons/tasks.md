# Tasks: Manage and Display Freeplane Node Icons

**Input**: Design documents from `/specs/008-manage-node-icons/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/

**Tests**: Test-driven verification (Principle III) included for JVM unit tests and Compose previews.
**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Exact file paths are provided for each task

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Establish localized strings and shared presentation models

- [X] T001 [P] Add string resources in `app/src/main/res/values/strings.xml` for icon categories (All, Status & Flags, Priorities & Numbers, Emoticons, Actions & Tools, People & Objects), icon management actions (Clear All, Move Left, Move Right, Remove Icon), and catalog search placeholder
- [X] T002 [P] Create presentation models `NodeIconVisual`, `NodeIconCategory`, and `NodeIconDescriptor` in `app/src/main/java/fr/julien/quievreux/droidplane2/model/NodeIconModel.kt` per data-model.md

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core registry and UI primitives that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T003 Implement `NodeIconRegistry` in `app/src/main/java/fr/julien/quievreux/droidplane2/model/NodeIconRegistry.kt` mapping standard Freeplane icon names and aliases (normalizing `-` and `_`) to the 81 existing drawables and vector icons, and returning `NodeIconVisual.Unmapped(name)` for unmapped icons
- [X] T004 [P] Create unit test suite `NodeIconRegistryTest` in `app/src/test/java/fr/julien/quievreux/droidplane2/model/NodeIconRegistryTest.kt` verifying normalization, icon resolution, unmapped fallback, category filtering, and search using `kotlin.test`
- [X] T005 [P] Create reusable composable `NodeIconView` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeIconView.kt` rendering Vector, Resource, or Unmapped compact textual badge (`[name]`), with `@Preview`
- [X] T006 Create reusable composable `NodeIconsRow` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeIconsRow.kt` rendering inline icons with `maxInlineIcons` limit and `+N` overflow badge per ui-contracts.md, with `@Preview`

**Checkpoint**: Foundation ready - user story implementation can now begin

---

## Phase 3: User Story 1 - Visual Display of Assigned Node Icons (Priority: P1) 🎯 MVP

**Goal**: Render all assigned Freeplane node icons alongside the node title in Canvas cards, and up to 3 icons inline plus `+N` overflow badge in the hierarchical NodeList.

**Independent Test**: Load a mindmap containing nodes with 1, 3, and 5+ icons; verify that all icons render on Canvas cards, and exactly 3 icons plus `+N` badge appear in the list view row, with distinct separation from link and rich-text indicators.

### Implementation for User Story 1

- [X] T007 [US1] Update `MindMapNodeCard` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt` to render assigned icons via `NodeIconsRow(iconNames = node.iconNames, maxInlineIcons = Int.MAX_VALUE)`
- [X] T008 [US1] Update `NodeList` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt` to replace legacy icon rendering loop and reflection lookup with `NodeIconsRow(iconNames = node.iconNames, maxInlineIcons = 3)` while keeping link and rich-text markers distinctly separated
- [X] T009 [P] [US1] Update Compose `@Preview` composables in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/mindmap/MindMapNodeCard.kt` and `app/src/main/java/fr/julien/quievreux/droidplane2/ui/components/NodeList.kt` to display nodes with multiple icons and unmapped badges

**Checkpoint**: User Story 1 complete and independently testable (MVP)

---

## Phase 4: User Story 2 - Managing Node Icons: Adding, Removing, and Reordering (Priority: P1)

**Goal**: Provide an active icon management section in the inspector allowing users to view all currently assigned icons with visual graphics, remove individual icons, reorder them, and clear all icons.

**Independent Test**: Open the inspector on a node with assigned icons, remove an icon, reorder two icons using move buttons, click clear all, and verify the node updates immediately in the active view and document.

### Implementation for User Story 2

- [X] T010 [P] [US2] Create `ActiveIconsManager` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/ActiveIconsManager.kt` with active icon cards showing visual preview (`NodeIconView`), name label, move earlier / move later buttons, delete action, and "Clear All" action, with `@Preview`
- [X] T011 [US2] Integrate `ActiveIconsManager` into `IconsMediaTab` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/IconsMediaTab.kt` to handle active icon sequence, removal, and clear all with immutable state emissions via `onNodeChanged`

**Checkpoint**: User Stories 1 AND 2 complete and independently testable

---

## Phase 5: User Story 3 - Unified Icon Catalog and Unmapped Icon Handling (Priority: P2)

**Goal**: Provide a hybrid visual icon picker in `IconsMediaTab` with instant text search and category filter chips, allowing users to tap any catalog item to append it to the node, and verify unmapped icons round-trip safely.

**Independent Test**: Search for "flag" in the catalog, filter by "Priorities & Numbers", tap an icon to append it, and verify an unmapped icon name in a test file displays `[name]` without corrupting document XML on save.

### Implementation for User Story 3

- [X] T012 [P] [US3] Create `IconCatalogPicker` composable in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/IconCatalogPicker.kt` featuring search input, `NodeIconCategory` filter chips, and a grid of selectable icon cards (`NodeIconView` + label) with `@Preview`
- [X] T013 [US3] Integrate `IconCatalogPicker` into `IconsMediaTab` in `app/src/main/java/fr/julien/quievreux/droidplane2/ui/inspector/IconsMediaTab.kt`, replacing legacy hardcoded text chips and wiring icon selection to append to `node.iconNames`
- [X] T014 [P] [US3] Create unit test `FreeplaneIconXmlTest` in `app/src/test/java/fr/julien/quievreux/droidplane2/model/FreeplaneIconXmlTest.kt` verifying that nodes with multiple mapped and unmapped icons round-trip through `NodeManager` save/load with 100% XML fidelity

**Checkpoint**: All user stories complete and independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Clean up legacy reflection, enrich demo maps, and validate performance and test suite

- [X] T015 [P] Deprecate obsolete reflection helpers in `app/src/main/java/fr/julien/quievreux/droidplane2/model/NodeIcons.kt` and redirect calls to `NodeIconRegistry`
- [X] T016 [P] Update demo mindmaps in `data/src/main/java/fr/julien/quievreux/droidplane2/data/FakeDataSource.kt` and `app/src/main/res/raw/example.mm` to showcase various icons (status, priority, flags, emoticons, unmapped badge)
- [X] T017 Run full test suite `./gradlew test` to ensure zero regressions across `:core`, `:data`, and `:app`
- [X] T018 Execute validation scenarios per `specs/008-manage-node-icons/quickstart.md`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - starts immediately (T001, T002 in parallel)
- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user stories
  - T003 (`NodeIconRegistry`) depends on T002
  - T004 (`NodeIconRegistryTest`) depends on T003
  - T005 (`NodeIconView`) depends on T002
  - T006 (`NodeIconsRow`) depends on T003, T005
- **User Story 1 (Phase 3)**: Depends on Foundational Phase (T006). Delivers MVP.
- **User Story 2 (Phase 4)**: Depends on Foundational Phase (T005). Can run in parallel with or after US1.
- **User Story 3 (Phase 5)**: Depends on Foundational Phase (T003, T005) and US2 (T011).
- **Polish (Phase 6)**: Depends on US1, US2, and US3 completion.

---

## Parallel Opportunities

- **Phase 1**: T001 and T002 can run in parallel.
- **Phase 2**: T004 and T005 can run in parallel once T002/T003 are implemented.
- **Phase 3**: T007 and T008 can run in parallel once T006 is ready.
- **Phase 4 & 5**: T010 (`ActiveIconsManager`) and T012 (`IconCatalogPicker`) can be developed in parallel as separate composable files.
- **Phase 6**: T015 and T016 can run in parallel.

---

## Implementation Strategy

### MVP First (User Story 1)
1. Complete Phase 1: Setup (`strings.xml`, `NodeIconModel.kt`).
2. Complete Phase 2: Foundational (`NodeIconRegistry.kt`, `NodeIconRegistryTest.kt`, `NodeIconView.kt`, `NodeIconsRow.kt`).
3. Complete Phase 3: User Story 1 (`MindMapNodeCard.kt`, `NodeList.kt`).
4. **STOP and VALIDATE**: Test User Story 1 independently in canvas and list view.

### Incremental Delivery
1. Foundation + US1 -> Nodes visibly display icons with overflow handling and unmapped badges (MVP).
2. Add US2 -> Users can inspect, reorder, and remove icons in the inspector.
3. Add US3 -> Full searchable, categorized catalog with 80+ icons.
4. Polish -> Clean up legacy reflection code, update demo maps, and run full test suite.
