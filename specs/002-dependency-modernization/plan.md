# Implementation Plan: Dependency Modernization

**Branch**: `develop` | **Date**: 2026-08-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/002-dependency-modernization/spec.md`

## Summary

Modernize the single-module Android application in independently verifiable slices: first update the Gradle/AGP/Kotlin/KSP/Hilt toolchain and prove generated-code builds without the metadata override; then migrate the production PDF chain to iText Core Android 9.7.1 and strengthen its device-side structural test; then update the remaining stable libraries, clean duplicate declarations, and add GitHub dependency automation. Keep all application changes on `develop` and identify the two automation files as the only `master` activation boundary.

## Technical Context

**Language/Version**: Kotlin 2.4.10, Java 17 source/target, Gradle Kotlin DSL

**Primary Dependencies**: Android Gradle Plugin 9.3.1 with Built-in Kotlin, KSP 2.3.10, Compose BOM, Hilt/KSP, Room/KSP, Retrofit/Moshi/OkHttp, iText Core Android 9.7.1

**Storage**: Existing Room database, DataStore preferences, Android cache files for PDF output

**Testing**: JUnit 4, MockK, Robolectric, AndroidX Test/JUnit, Espresso, UIAutomator; Gradle unit, lint, assemble, R8 analysis, and connected-device instrumentation tasks

**Target Platform**: Android API 30 minimum; compile SDK 37; target SDK 36; physical Android device available through the SDK-local ADB executable

**Project Type**: Single-module Android mobile application with JVM unit tests and Android instrumentation/E2E tests

**Performance Goals**: Preserve existing application behavior and generate representative PDFs within existing device-test timeouts; introduce no new network or external-hardware prerequisite for PDF verification

**Constraints**: Stable fixed versions only; production generation path must be exercised; no UI/architecture refactor; no speculative ProGuard rules; release isolation between `develop` and `master`; GitHub activation remains pending until the minimal files reach the default branch

**Scale/Scope**: One application module, 23 version families plus Gradle wrapper and GitHub Actions; two production PDF source files; one focused instrumentation test extended for structural PDF assertions

## Constitution Check

*GATE: Passed before Phase 0 research; re-checked and passed after Phase 1 design.*

| Principle | Pre-research gate | Post-design evidence |
|-----------|-------------------|----------------------|
| I. Production Safety and Release Isolation | PASS: no test backchannel or release behavior change is planned | PASS: PDF verification remains in `androidTest`; automation-only `master` boundary is explicit |
| II. Production-Path Fidelity | PASS: the existing `PdfGenerator` binding and iText implementation remain the system under test | PASS: quickstart invokes the focused test through `DebugE2EEntryPoint` and the production generator/helper |
| III. Secret-Safe, Correlated Evidence | PASS: no secrets are needed; existing scenario evidence contract is reused | PASS: output is scoped to app cache and ScenarioRule cleanup/evidence; no payload or credential logging added |
| IV. Deterministic Native Validation | PASS: standard Gradle and ADB tasks are sufficient; unavailable checks have explicit states | PASS: structural assertions avoid pixel snapshots and external services; portrait/landscape are deterministic |
| V. Preservation Until Verified Parity | PASS: no existing tests or runners are removed | PASS: existing PDF catalog and UI tests are strengthened/retained; cleanup occurs only after green replacement builds |

No constitutional violation or exception is required.

## Project Structure

### Documentation (this feature)

```text
specs/002-dependency-modernization/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── checklists/
│   └── requirements.md
└── tasks.md
```

No external application interface is introduced, so no `contracts/` artifact is required.

### Source Code (repository root)

```text
gradle/
├── libs.versions.toml
└── wrapper/
    ├── gradle-wrapper.jar
    └── gradle-wrapper.properties

app/
├── build.gradle.kts
├── proguard-rules.pro
└── src/
    ├── main/java/com/app/miklink/data/pdf/
    │   ├── PdfDocumentHelper.kt
    │   └── impl/PdfGeneratorIText.kt
    └── androidTest/java/com/app/miklink/e2e/catalog/PdfScenarioTest.kt

.github/
├── dependabot.yml
└── workflows/dependency-submission.yml

settings.gradle.kts
build.gradle.kts
gradle.properties
```

**Structure Decision**: Preserve the existing single Android application module. Version and automation changes stay in existing root configuration; PDF migration changes only the current implementation/helper and its existing production-path instrumentation test.

## Implementation Slices

### Slice A — Baseline

Record clean worktree, Gradle 9.5.0, `check`, `assembleDebug`, and the connected device before build changes. The initial `check` and `assembleDebug` both passed; the SDK-local ADB reported device `22101316G` online.

### Slice B — Toolchain

Use the wrapper task to update Gradle fully to 9.6.1. Update AGP, Kotlin, KSP, and Hilt together because Hilt 2.60.1 explicitly targets AGP 9 and updates its Kotlin metadata support. Remove the forced metadata reader before validating generated Room/Hilt/Moshi sources. Remove the unused Kotlin Android alias and Room's duplicate plugin-management version once the catalog-backed plugin resolves.

### Slice C — iText Android

Declare `com.itextpdf.android:itext-core-android:9.7.1`. Migrate only the PDF event handler API moved in iText 9. Extend `PdfScenarioTest` to create portrait and landscape documents through the injected production generator, reopen each with `PdfReader`/`PdfDocument`, extract text, assert page geometry/content regions, and retain the UI export E2E test. Confirm the resolved Android modules with `dependencyInsight`; then build/analyze the minified release and remove broad iText keep rules if R8 proves them unnecessary.

### Slice D — Remaining Libraries

Apply the stable patch/minor updates from the audit. Treat OkHttp 4.12.0 → 5.4.0 as its own major slice and verify networking compilation/unit tests before proceeding. No application adaptation is planned unless compilation or tests reproduce a relevant break.

### Slice E — Automation and Cleanup

Add the requested updater schedule and dedicated dependency-submission workflow after confirming no equivalent workflow exists. Search active configuration for stale versions, old iText coordinates, duplicate version sources, and removed workarounds. Keep the two automation files identifiable as the complete `master` activation set; do not switch branches or merge automatically.

## Complexity Tracking

No constitution violation requires justification.
