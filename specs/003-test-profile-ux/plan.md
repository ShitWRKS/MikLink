# Implementation Plan: Test Profile Tabbed UX

**Branch**: `develop` | **Date**: 2026-08-11 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification from `/specs/003-test-profile-ux/spec.md`

## Summary

Replace the current single scrolling profile form and its collapsible Ping/threshold cards with four directly selected tabs: General, Link, Ping, and Speed test. Keep `TestProfileViewModel` as the sole form-state owner and preserve every domain/persistence field. Split feature-scoped composables into small files, add Canvas/Material-based illustrative threshold previews, migrate stable UI semantics and the functional CRUD journey, update EN/IT resources, and correct only directly stale testing documentation.

No research artifact is required: the user brief and existing source of truth fully determine tab behavior, preview semantics, state ownership, validation, and scope. No external interface contract is introduced.

## Technical Context

**Language/Version**: Kotlin 2.4.10; Java 17 source/target

**Primary Dependencies**: Existing Jetpack Compose UI and Material 3 through Compose BOM 2026.06.01; AndroidX Lifecycle Compose; Hilt Navigation Compose. No new dependency.

**Storage**: Existing Room-backed `TestProfile` persistence and thresholds JSON mapping; no schema, entity, mapper, or migration change

**Testing**: JUnit 4 JVM tests, existing quality scans, AndroidX instrumentation with the repository `ScenarioRule` and functional UI support, Gradle lint/build gates

**Target Platform**: Android API 30 minimum; compile SDK 37; target SDK 36

**Project Type**: Single-module Android mobile application

**Performance Goals**: Preview geometry and labels update in the same Compose state update as the edited field; tab changes retain all unsaved values and introduce no network, storage, or background work

**Constraints**: Exactly four tabs; click navigation only; independent tab scrolling; no domain/policy/persistence changes; no new chart library; previews are deterministic illustrations, not measurements; no global theme changes; no parallel legacy layout

**Scale/Scope**: One profile edit route, four feature-scoped tab composables, one small preview file, stable semantics, one primary functional UI journey, two locale files, and directly related documentation

## Constitution Check

*GATE: Passed before design; re-checked and passed after design.*

| Principle | Pre-design gate | Post-design evidence |
|-----------|-----------------|----------------------|
| I. Production Safety and Release Isolation | PASS: the change is local profile UI with no remotely reachable or destructive test control | PASS: semantic IDs retain the existing debug-only exposure model; no release behavior or dependency changes are introduced |
| II. Production-Path Fidelity | PASS: the existing ViewModel, save use case, repository, domain models, validators, and policy remain authoritative | PASS: the functional journey creates, saves, reopens, edits, and deletes only through the production UI and persistence path |
| III. Secret-Safe, Correlated Evidence | PASS: no credentials, payload logging, or new evidence format is involved | PASS: the existing `ScenarioRule` provides correlated failure evidence; the test uses generated non-sensitive profile data and introduces no logs |
| IV. Deterministic Native Validation | PASS: previews use deterministic illustrative data and validation uses Gradle/Android instrumentation | PASS: no external network or hardware is required for the profile CRUD journey; unavailable devices remain `NOT_RUN` |
| V. Preservation Until Verified Parity | PASS: stable semantic IDs and existing tests are preserved until consumers migrate | PASS: `PING_CONFIG` is removed only with its consumer update; the CRUD test retains create/edit/delete parity and extends round-trip coverage |

No constitutional violation or exception is required. Tasks explicitly cover release isolation, secret-safe evidence review, cleanup/failure artifacts, and semantic migration parity.

## Project Structure

### Documentation (this feature)

```text
specs/003-test-profile-ux/
├── spec.md
├── plan.md
├── data-model.md
├── quickstart.md
├── verification.md
├── checklists/
│   ├── requirements.md
│   └── ux.md
└── tasks.md
```

`research.md` is intentionally omitted because there are no unresolved technical decisions. `contracts/` is omitted because the feature introduces no external API or file contract.

### Source Code (repository root)

```text
app/src/main/java/com/app/miklink/ui/profile/
├── TestProfileEditScreen.kt       # scaffold, tab selection, VM orchestration, save/back
├── TestProfileFormComponents.kt    # feature-private shared switches/threshold fields
├── TestProfileGeneralTab.kt       # name and description
├── TestProfileLinkTab.kt          # Link Status, TDR, LLDP and link threshold
├── TestProfilePingTab.kt          # Ping toggle, targets, count, gateway policy and thresholds
├── TestProfileSpeedTab.kt         # Speed toggle and five existing thresholds
└── TestProfileThresholdPreviews.kt # only the three concrete illustrative previews/helpers

app/src/main/java/com/app/miklink/ui/testing/AgentUiTags.kt
app/src/main/res/values/strings.xml
app/src/main/res/values-it/strings.xml
app/src/androidTest/java/com/app/miklink/e2e/functional/ProfileCrudUiTest.kt
app/src/androidTest/java/com/app/miklink/e2e/catalog/SemanticSurfaceTest.kt
app/src/test/java/com/app/miklink/ui/profile/TestProfileThresholdPreviewTest.kt
app/src/test/java/com/app/miklink/quality/StringsItalianCoverageTest.kt
docs/reference/testing.md
docs/reference/ui-architecture.md        # edit only if a stale profile-layout statement is found
```

**Structure Decision**: Preserve the existing `ui/profile` feature slice and ViewModel. The screen collects lifecycle-aware flows once and passes values/callbacks to four stateless tab composables. Feature-private reusable form controls move out of the old mega-file only where two tabs share them. Preview logic remains UI-only and local to the feature.

## Design Decisions

### Tab and scroll state

- `TestProfileEditScreen` uses a Material 3 `PrimaryTabRow`/`TabRow` immediately below the top app bar and a `rememberSaveable` selected-tab index.
- The selected tab body owns a `LazyColumn` or `Column` with vertical scrolling. Each tab uses a stable saveable scroll state so switching tabs and configuration recreation retain the user's place where the platform can restore it.
- Form values remain lifecycle-collected `StateFlow` values from `TestProfileViewModel`; tab switching neither owns nor reconstructs them.
- The bottom bar contains the persistent Save button and a compact validation message when no test is enabled.

### Link preview

- The existing preset list remains presentation data. The selected/custom value is parsed only through `StrictLinkRateParser`.
- A discrete logarithmic-position scale presents preset markers; a valid custom value outside preset bounds is displayed numerically and the indicator may sit at the nearest edge with an explicit out-of-scale presentation. The saved value is never clamped.
- Text communicates the same rule as the existing policy: negotiated speed passes when it is greater than or equal to the configured minimum.

### Ping preview

- Each local/external threshold section keeps loss, maximum average RTT, and maximum RTT numeric fields visible.
- A deterministic normalized sample path is scaled against the valid effective maximum references. Average and maximum threshold lines are independently omitted when their source input is invalid.
- Blank input resolution calls a ViewModel-provided effective-value helper backed by its already-owned `TestThresholds.defaults()` instance, preventing UI duplication of defaults while leaving fields blank.

### Speed preview

- Effective download/upload minima produce comparable horizontal bars whose display maximum is derived from the larger current value with headroom. Direct manipulation uses a fixed logarithmic 0–100G mapping so state updates cannot move the thumb independently of the gesture; the latest explicit 100G maximum is also enforced by shared threshold validation.
- Ping, jitter, and loss remain normal numeric fields plus compact textual value indicators outside the Mbps bar axis.
- Invalid values omit only the affected visual value while retaining the field error.

### Semantic migration and acceptance

- Add stable IDs for General, Link, Ping, and Speed tabs and focused threshold inputs used by the functional acceptance journey.
- Remove `PING_CONFIG` only after the CRUD test no longer consumes it. Existing screen, field, toggle, save, item, and delete tags remain unchanged.
- Extend the single CRUD journey to touch Link and Ping threshold values, verify unsaved state across tab switches, verify saved values after reopen, edit/save/reopen again, and delete through the visible UI.

## Implementation Slices

### Slice A — Semantics and resources

Introduce the four tab IDs and focused threshold field IDs, add paired EN/IT tab/preview/section/semantic labels, and keep the canonical non-translatable tagline untouched.

### Slice B — Screen decomposition and tabs

Reduce `TestProfileEditScreen.kt` to scaffold/orchestration. Add four tab composables and move the existing controls with no change to their ViewModel fields or callbacks. Eliminate the collapsible Ping and global thresholds organization.

### Slice C — Dynamic previews

Add only feature-specific Link, Ping, and Speed preview composables using existing Canvas/Material primitives. Resolve blank effective values through the ViewModel; invalid values degrade without fallback invention.

### Slice D — Functional acceptance and docs

Migrate the CRUD journey to stable tabs and threshold IDs, confirm semantic uniqueness, correct stale UI-delete documentation, and update only any profile-layout documentation that is demonstrably stale.

### Slice E — Verification and convergence

Run focused unit/quality tests, lint/build/instrumentation compilation, then the device test if an authorized device is available. Record exact outcomes in `verification.md`, review the final diff/scope, and run Spec Kit convergence until no CRITICAL/HIGH or remaining task exists.

## Complexity Tracking

No constitution violation requires justification.
