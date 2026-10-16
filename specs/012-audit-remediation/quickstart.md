# Quickstart & Validation Guide (`012-audit-remediation`)

**Date**: 2026-10-16  
**Feature**: [spec.md](spec.md)  
**Contracts**: [module-and-state-contracts.md](contracts/module-and-state-contracts.md)  
**Data Model**: [data-model.md](data-model.md)

## Prerequisites

- JDK 21 configured (`java -version`)
- Android SDK with API 35 installed
- Working directory: repository root or isolated worktree `.worktrees/012-audit-remediation`

## Validation Scenarios

### 1. Run Full Unit Test Suite Across All Modules (`SC-002`, `SC-005`, `SC-006`)

Verify that moving `KStringSpec` and `FakeDataSource` to `testFixtures` and applying all data/UI fixes passes 100% of unit tests:

```bash
./gradlew testDebugUnitTest
```

**Expected Outcome**:
- `:core:testDebugUnitTest`, `:data:testDebugUnitTest`, and `:app:testDebugUnitTest` succeed with 0 failures.
- New regression tests in `:data` verify:
  - Literal `<![CDATA[` and `]]>` inside node text and rich content are preserved across `serializeMindmap` round trips.
  - Special characters (`<`, `>`, `&`, `"`) inside `<richcontent>` survive `loadRichContent` + `serializeMindmap`.
  - Invalid file paths or filenames trigger `onError` and return immediately without calling `onSaveFinished` or creating files.
  - Editing an ancestor node updates `parentNode` references across grandchildren and deeper descendants (`propagateUpdatedParentToRoot`).
  - `Node.equals` and `Node.hashCode` distinguish nodes differing only by `fontSize`, `fontName`, `isBold`, `isItalic`, `isFolded`, `latexEquation`, or `externalObject`.
  - `loadMindMapFromXml` emits the final node list once rather than $N$ intermediate list copies.

---

### 2. Verify Production Release APK Hygiene & R8 Shrinking (`SC-001`)

Build the release APK with `isMinifyEnabled = true` and inspect packaged classes:

```bash
./gradlew :app:assembleRelease
```

Inspect DEX contents to verify no test frameworks or `FakeDataSource` are shipped in the release APK:

```bash
unzip -p app/build/outputs/apk/release/app-release-unsigned.apk "classes*.dex" | strings | grep -E "io/kotest|io/mockk|app/cash/turbine|FakeDataSource|KStringSpec" || echo "CLEAN: Zero test classes in release APK"
```

**Expected Outcome**:
- `:app:assembleRelease` succeeds using `app/proguard-rules.pro`.
- Output prints `CLEAN: Zero test classes in release APK`.

---

### 3. Verify Static Analysis & Dead Code Removal (`SC-006`)

 Confirm that all 6 obsolete source/layout files and 7 unused catalog libraries are removed and Android Lint passes:

```bash
./gradlew lintDebug
```

**Expected Outcome**:
- Build completes without unresolved symbol errors or fatal lint issues.
