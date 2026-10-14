# Implementation Plan: Manage and Display Freeplane Node Icons

**Branch**: `008-manage-node-icons` | **Date**: 2026-10-14 | **Spec**: [specs/008-manage-node-icons/spec.md](spec.md)

**Input**: Feature specification from `specs/008-manage-node-icons/spec.md`

## Summary

Enable rich, faithful support for Freeplane node icons in Droidplane:
1. Provide a high-performance compile-time `NodeIconRegistry` mapping Freeplane icon identifiers (normalizing `-` and `_`) to the 80+ existing Freeplane drawables and vector icons, replacing slow runtime reflection.
2. Render assigned icons on nodes in both the mindmap canvas (`MindMapNodeCard`) and list view (`NodeList`), implementing user-directed policies: up to 3 icons inline with `+N` overflow badge in list view, and a clean textual badge `[iconName]` for unmapped icons.
3. Enhance the Inspector tab (`IconsMediaTab`) into a comprehensive icon manager: visual previews of active icons with reordering and removal, plus a hybrid icon catalog featuring real-time search filtering and thematic category chips.

## Technical Context

**Language/Version**: Kotlin 2.1.0, JVM 21, Java 21 toolchain

**Primary Dependencies**: Jetpack Compose (BOM 2024.12.01, Material 3), Compose Icons (`compose.icons:font-awesome:1.1.1`, `compose.icons:tabler-icons:1.1.1`), Koin DI (BOM 4.0.0)

**Storage**: Freeplane XML documents (`.mm`), local Android storage

**Testing**: `kotlin.test`, Kotest (`io.kotest`), MockK, Turbine, Compose UI Testing

**Target Platform**: Android (Min SDK 26, Target SDK 35)

**Project Type**: Android mobile application

**Performance Goals**: 60 fps fluid scrolling on list and canvas with nodes containing multiple icons; O(1) static icon lookup (zero reflection)

**Constraints**: Completely offline, two-way Freeplane XML compatibility with zero data loss on save, cyclomatic complexity strictly < 20 per method

**Scale/Scope**: 80+ bundled icon assets, arbitrary unmapped icon support, full reordering and catalog browsing

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Evaluation & Architectural Alignment |
|---|---|---|
| **I. Modular Separation of Concerns** | **PASSED** | `:core` remains untouched. `:data` preserves `Node.iconNames` and XML parser/serializer. `:app` encapsulates presentation models (`NodeIconVisual`, `NodeIconCategory`, `NodeIconDescriptor`), registry, and Compose UI components. |
| **II. Unidirectional Data Flow & State Consistency** | **PASSED** | Icon reordering, additions, and deletions in `IconsMediaTab` emit updated `Node` copies to `onNodeChanged` and propagate via `NodeManager` single source of truth. |
| **III. Test-Driven Verification** | **PASSED** | Fast JVM unit tests (`NodeIconRegistryTest`) test normalization, resolution, catalog search, and filtering without Android Context. Compose previews for all new/updated composables. |
| **IV. Freeplane Format Compatibility & Data Integrity** | **PASSED** | Unmapped icon names preserved verbatim in XML. Zero loss or formatting disruption of `<icon BUILTIN="...">` tags. |
| **V. Performance & Simplicity** | **PASSED** | Static map lookup eliminates reflection (`getIdentifier`). Lightweight badges for unmapped icons. No heavy new external dependencies. |
| **VI. SOLID Principles & Cyclomatic Complexity** | **PASSED** | `NodeIconRegistry` has single responsibility of icon resolution. Composables split into granular functions (`NodeIconView`, `NodeIconsRow`, `ActiveIconsList`, `IconCatalogGrid`). All methods strictly maintain cyclomatic complexity < 10. |

---

## Project Structure

### Documentation (this feature)

```text
specs/008-manage-node-icons/
├── spec.md              # Feature specification
├── plan.md              # Implementation plan (this file)
├── research.md          # Architectural research & decisions
├── data-model.md        # Presentation & domain data model
├── quickstart.md        # Verification & test guide
├── checklists/
│   └── requirements.md  # Specification quality checklist
└── contracts/
    ├── ui-contracts.md  # Composable component contracts & signatures
    └── xml-compatibility-contract.md # Freeplane XML format contract
```

### Source Code (repository root)

```text
app/src/main/java/fr/julien/quievreux/droidplane2/
├── model/
│   ├── NodeIcons.kt                   # Deprecate legacy reflection helpers, adapt to NodeIconRegistry
│   └── NodeIconRegistry.kt            # Core icon catalog, registry, and normalization
├── ui/
│   ├── components/
│   │   ├── NodeIconView.kt            # Reusable icon renderer (Vector, Resource, Unmapped badge)
│   │   ├── NodeIconsRow.kt            # Inline row renderer with maxInlineIcons and overflow badge (+N)
│   │   └── NodeList.kt                # Updated to use NodeIconsRow (max 3 + overflow badge)
│   ├── mindmap/
│   │   └── MindMapNodeCard.kt         # Updated to use NodeIconsRow (renders all assigned icons)
│   └── inspector/
│       ├── IconsMediaTab.kt           # Enhanced icon manager (active list, reordering, catalog search)
│       └── NodePropertiesInspector.kt # Host inspector dialog
└── res/
    ├── drawable/                      # Existing 81 Freeplane icon PNGs (icon_*.png)
    └── values/
        └── strings.xml                # Localized strings for icon categories and actions

app/src/test/java/fr/julien/quievreux/droidplane2/
└── model/
    └── NodeIconRegistryTest.kt        # JVM unit tests for resolution, aliases, categories, and search
```

**Structure Decision**: Standard Android modular layout keeping presentation logic strictly inside `:app` and domain logic in `:data`.

---

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

*No violations identified. Design adheres fully to all Constitution principles.*
