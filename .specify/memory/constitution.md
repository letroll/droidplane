<!--
SYNC IMPACT REPORT
==================
- Version change: 0.0.0 (Uninitialized Template) -> 1.0.0
- Bump type: MAJOR (Initial ratification of repository constitution)
- Modified principles:
  * [PRINCIPLE_1_NAME] -> I. Modular Separation of Concerns (:core, :data, :app)
  * [PRINCIPLE_2_NAME] -> II. Unidirectional Data Flow & State Consistency
  * [PRINCIPLE_3_NAME] -> III. Test-Driven Verification (NON-NEGOTIABLE)
  * [PRINCIPLE_4_NAME] -> IV. Freeplane Format Compatibility & Data Integrity
  * [PRINCIPLE_5_NAME] -> V. Performance, Large Map Responsiveness & Simplicity
- Added sections:
  * Technical Constraints & Quality Standards
  * Development Workflow & Quality Gates
- Removed sections: None
- Follow-up TODOs: None (all template slots defined)
-->

# Droidplane Constitution

## Core Principles

### I. Modular Separation of Concerns
The project MUST maintain strict boundary separation across its three Gradle modules:
- `:core`: MUST contain generic, domain-agnostic utilities, extensions, loggers, and base UI primitives reusable across Android projects. It MUST NOT depend on `:data` or `:app`.
- `:data`: MUST encapsulate Freeplane domain models, XML parsing and serialization (`.mm` format), and data manipulation (`NodeManager`). It MUST depend only on `:core` and MUST NOT depend on `:app` or UI libraries.
- `:app`: MUST manage the presentation layer (Jetpack Compose, ViewModels, navigation, and activity orchestration).

Rationale: Isolating data models and XML processing ensures that parser logic can be evolved and tested independently without UI dependencies or Android framework overhead.

### II. Unidirectional Data Flow & State Consistency
The application MUST maintain a single, cohesive source of truth for the active mindmap state:
- ViewModels MUST expose state through Kotlin `StateFlow` and immutable UI state models.
- UI components MUST emit intents or events to ViewModels and MUST NEVER mutate shared domain state directly.
- Hierarchical updates (such as node edits, addition of child nodes, and deletions) MUST propagate through central state managers (`NodeManager`) to guarantee synchronization across current view and child listings.

Rationale: Decoupled or fragmented states lead to UI desynchronization where parent and child views display divergent node data.

### III. Test-Driven Verification (NON-NEGOTIABLE)
Automated verification is mandatory for all business logic, data models, and parser operations:
- All domain operations, XML parsing, serialization routines, and ViewModel transformations MUST have automated test coverage using Kotest, MockK, and Turbine.
- Fast JVM unit tests running on JUnit Platform MUST be prioritized for parser and data algorithms.
- Parsing edge cases (empty streams, malformed XML, unhandled tags, deep nesting, rich content) and round-trip serialization MUST be accompanied by regression tests before merging.
- UI components MUST be verifiable through Jetpack Compose Previews and Compose UI tests.

Rationale: Mindmaps represent user knowledge and notes; defects in parsing or serialization risk catastrophic data loss.

### IV. Freeplane Format Compatibility & Data Integrity
Droidplane MUST prioritize data integrity and compatibility with desktop Freeplane (`.mm` files):
- XML parsing and serialization MUST preserve existing attributes, structure, and metadata even when specific visual features are not yet supported on mobile.
- File saving MUST be atomic (via safe replacement or staging) to prevent file corruption during interrupted writes.
- Character encoding, line breaks, and XML entity escaping MUST strictly match the Freeplane XML dialect.

Rationale: Users exchange mindmaps across desktop Freeplane and Droidplane; lossy writes or schema corruption destroy trust.

### V. Performance, Large Map Responsiveness & Simplicity
The application MUST remain fast, responsive, and lightweight even with complex mindmaps:
- Intensive operations (XML parsing, disk I/O, deep search traversal) MUST run off the main thread on appropriate Kotlin Coroutine dispatchers (`Dispatchers.IO` or `Dispatchers.Default`).
- Mindmap listings and node trees MUST leverage virtualized Compose components (`LazyColumn`, optimized layout passes) to guarantee smooth 60/120 fps scrolling.
- Embrace simplicity and YAGNI: Favor Kotlin standard library primitives and official Android Jetpack APIs over speculative abstractions or superfluous external libraries.

Rationale: Mobile devices face constrained memory and CPU; large mindmaps must render effortlessly without ANRs or UI lag.

## Technical Constraints & Quality Standards

Droidplane targets modern Android platforms while adhering to proven architectural standards:
- Language & Toolchain: Kotlin (target JVM 21, Java 21 toolchain), Android Gradle Plugin 8.7+.
- Platform Targets: Minimum SDK 26 (Android 8.0 Oreo), Target SDK 35 (Android 15).
- UI Framework: Jetpack Compose with Material 3 styling and explicit dark/light mode preview support.
- Dependency Injection: Koin (BOM 4.0.0+) for modular dependency wiring across `:core`, `:data`, and `:app`.
- Logging & Observability: Centralized `Logger` interface from `:core` MUST be used instead of raw `android.util.Log` to allow test mocking and log filtering.
- Error Handling: Methods performing I/O or parsing MUST use explicit Kotlin `Result<T>` or structured error states rather than leaking raw exceptions to the UI.

## Development Workflow & Quality Gates

All contributions and automated implementations MUST satisfy the following quality gates:
- Automated Tests: The test suite (`./gradlew test`) MUST pass with zero failures before changes are committed or merged.
- Static Analysis: Code MUST comply with project formatting conventions and build without compilation warnings or fatal lint errors (`./gradlew lint`).
- Architecture Review: Any new dependency or cross-module linkage MUST be evaluated against module boundaries defined in Principle I.
- Backward Compatibility: Any change affecting XML reading or writing MUST include a test using realistic `.mm` fixtures.

## Governance

This constitution is the authoritative reference for architectural rules, design standards, and code quality in Droidplane.
- Supremacy: These principles supersede ad-hoc development practices or conflicting conventions. All feature designs, refactors, and pull requests must comply.
- Amendment Procedure: Any changes to this constitution require:
  1. Clear documentation of the technical rationale.
  2. Review and approval by project maintainers.
  3. A corresponding semantic version bump and updated amendment date.
- Semantic Versioning:
  - MAJOR version bumps indicate breaking changes to architectural principles, module definitions, or governance rules.
  - MINOR version bumps indicate the addition of new principles, sections, or significant expansions of existing guidance.
  - PATCH version bumps indicate clarifications, typo fixes, or non-semantic wording refinements.
- Compliance Review: Pull request reviews and automated tools MUST verify compliance with these principles during feature development and refactoring.

**Version**: 1.0.0 | **Ratified**: 2026-10-03 | **Last Amended**: 2026-10-03
