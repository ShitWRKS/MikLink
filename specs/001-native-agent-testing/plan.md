# Implementation Plan: Native Agent-Driven Application Testing

**Branch**: `001-native-agent-testing-functional-acceptance` | **Date**: 2026-08-10 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `/specs/001-native-agent-testing/spec.md`

## Summary

Provide two complementary debug-only native paths: direct adb lifecycle/input/
hierarchy/screenshot commands for ad-hoc agent investigations, and a maintained
on-device catalog using AndroidJUnitRunner, Compose tests, and stable UI Automator
2.4. Reuse
the normal MikLink UI/domain/repository/network/export paths, strengthen the existing
debug trace into a versioned secret-safe evidence stream, isolate session-created
fixtures, and retain current wrappers until native parity is accepted.

The completion increment keeps existing repository/use-case scenarios as instrumented
integration tests and adds a separate physical-device Functional UI catalog. A
fixture may arrange prerequisites or clean session-owned records, but the journey
named by a Functional UI scenario is driven and verified only through rendered UI.

## Technical Context

**Language/Version**: Kotlin 2.3.21; Java/JVM 17  
**Primary Dependencies**: Android Gradle Plugin 9.3.0, Compose BOM 2026.06.00,
AndroidX Test/JUnit/Compose testing, UI Automator 2.4.0, Hilt 2.59.2, Room 2.8.4,
Retrofit 3/OkHttp 4/Moshi 1.15, iText 7  
**Storage**: existing Room/DataStore; debug external-files NDJSON; native test result
files registered through UI Automator ResultsReporter and explicitly retrievable by
adb when the connected-test path does not pull them  
**Testing**: JUnit4 local and instrumented tests, AndroidJUnitRunner, Compose tests,
UI Automator 2.4, direct adb debug exploration, external black-box release smoke  
**Target Platform**: physical Android device, min API 30; compile SDK 37, target 36  
**Project Type**: single-module Android application  
**Performance Goals**: no change to release performance; all waits and operations
have explicit scenario bounds; evidence writes remain incremental and size-bounded  
**Constraints**: host-neutral primary workflow; agent mode active only in debug; no
production/release activation path; no custom app-control protocol; no hard-coded
live credentials; no implicit data reset/Wi-Fi disruption; normal product
communication path  
**Scale/Scope**: 10 user-visible feature groups, six prioritized journeys, one
physical-device catalog, one exploratory contract, three versioned evidence schemas

## Constitution Check

*GATE: evaluated before research and re-checked after design.*

| Principle | Pre-research | Post-design evidence |
|---|---|---|
| I. Production Safety and Release Isolation | PASS | Mode compiled/active only in debug; no release activation path; external exact-release smoke; independent destructive opt-ins |
| II. Production-Path Fidelity | PASS | Named tests drive normal UI/use cases/repositories; fixtures arrange state only; no RouterOS backchannel |
| III. Secret-Safe, Correlated Evidence | PASS | Versioned schemas, source-time recursive redaction, operation/exchange correlations, acceptance scans |
| IV. Deterministic Native Validation | PASS | Explicit serial/prerequisites/outcomes/bounds; session-owned data; supported adb/Gradle/AndroidX mechanisms |
| V. Preservation Until Verified Parity | PASS | Responsibility map in `research.md`; both wrappers retained until acceptance evidence |

The 2026-08-10 Functional UI increment rechecked the gates: production paths remain
authoritative; integration tests are preserved; no new flavor, protocol, RouterOS
client, host wrapper, destructive permission, video, or release activation exists.

No violation requires complexity justification.

## Project Structure

### Documentation (this feature)

```text
specs/001-native-agent-testing/
|-- spec.md
|-- clarifications.md
|-- coverage-inventory.md
|-- plan.md
|-- research.md
|-- data-model.md
|-- quickstart.md
|-- contracts/
|   |-- README.md
|   |-- session-manifest.schema.json
|   |-- scenario-result.schema.json
|   `-- trace-event.schema.json
|-- checklists/
|   |-- requirements.md
|   `-- testing-requirements.md
`-- tasks.md
```

### Source Code (repository root; planned implementation)

```text
app/
|-- build.gradle.kts
`-- src/
    |-- main/java/com/app/miklink/
    |   |-- core/domain/test/logging/       # existing trace contracts/sanitizer
    |   |-- data/repository/mikrotik/       # existing probe exchange boundary
    |   `-- ui/                             # stable semantic tags on current screens
    |-- debug/java/com/app/miklink/
    |   |-- core/domain/test/logging/       # versioned enhanced trace producer
    |   `-- ui/testing/                     # debug-only semantic exposure policy
    |-- release/java/com/app/miklink/
    |   |-- core/domain/test/logging/       # production-safe no-op binding only
    |   `-- ui/testing/                     # compile-time disabled; no activation API
    |-- test/java/com/app/miklink/
    |   `-- core/domain/test/logging/       # redaction/schema/correlation tests
    `-- androidTest/java/com/app/miklink/e2e/
        |-- catalog/                        # independently selectable scenarios
        `-- support/                        # session, fixtures, results, artifacts,
                                            # prerequisites, crash/recovery support

gradle/libs.versions.toml                   # pin UI Automator 2.4.0
docs/reference/testing.md                   # direct native workflows/outcomes
tools/agent/run_live_probe_e2e.ps1          # retained until parity acceptance
tools/agent/run_live_probe_e2e.sh           # retained until parity acceptance
```

**Structure Decision**: Keep one app module and use existing Android source-set
isolation. A separate control service, desktop module, or duplicated networking
client is unnecessary. Instrumentation support remains test code; only stable
semantics and the already established debug/release policy boundary touch app code.

## Phase 0: Research Outcome

All technical unknowns are resolved in [research.md](research.md). Key decisions:

1. direct adb is the generic ad-hoc surface;
2. AndroidJUnitRunner + Compose + UI Automator 2.4 form the named catalog;
3. debug is test-capable while the exact release artifact gets a direct smoke;
4. session-owned records replace implicit resets;
5. one versioned manifest/result/trace contract replaces wrapper parsing;
6. live prerequisites are explicit, secret-free, and fail closed;
7. wrapper removal waits for responsibility-level parity.

## Phase 1: Design Outcome

### Session and evidence foundation

- Implement `TestSession`, `ScenarioResult`, prerequisite and step recording in
  `androidTest/e2e/support`, matching [data-model.md](data-model.md) and
  [contracts](contracts/README.md).
- Persist last-step progress, finalize atomically, register artifacts with
  `ResultsReporter`, and map JUnit outcomes without losing four-way semantics.
- Capture build/device identity before actions. Add source revision to the debug build
  through a generated `BuildConfig` value without reading Git at app runtime.

### UI observability and exploratory access

- Add stable, non-localized tags to each inventory flow and enable
  `testTagsAsResourceId` only in debug through the source-set policy. Do not add a
  runtime flag capable of enabling it in release.
- Document direct adb discovery, lifecycle, semantic hierarchy, input, screenshot,
  and evidence commands. Do not add a long-running service or command protocol.
- Use Compose assertions for app-local named cases and UI Automator for app/system
  lifecycle, bounded waits, cross-app Wi-Fi surfaces, screenshots, and hierarchy.

### Deterministic state and prerequisites

- Create uniquely prefixed client/profile/report fixtures through existing
  repository/use-case entry points, record IDs, and delete only those IDs in `finally`.
- Treat configured probe, reachability/authentication, interface capability, speed
  server, reset permission, disruption permission, and retained control as typed
  prerequisite checks.
- Remove live-test fallback values only when the replacement scenario is ready; use
  the current selected singleton probe and never transport credentials in arguments.

### Probe evidence and failures

- Extend the current trace producer rather than adding an interceptor-backed parallel
  log. Add session/scenario/operation/exchange identifiers and request/response/error
  stages at the existing repository boundary.
- Redact recursively and detect secret-like serialized values before events are
  written. Bound payload size and test nested DTO/map/list and error-body cases.
- On failure record last step, stable UI hierarchy/screenshot, current app visibility,
  post-session process exits, and a correlated logcat excerpt when obtainable.

### Regression catalog

- Preserve probe-independent repository/use-case CRUD/settings/history/report/PDF/
  backup and result-card scenarios as integration coverage.
- Add independently selectable UI Automator/Compose Functional UI scenarios for
  launch/navigation, client/profile CRUD, representative settings/report settings,
  history/detail, real result presentation, and PDF export. They use dynamic semantic
  selectors and never hard-coded coordinates.
- Migrate live link/TDR/network/neighbors/ping/speed coverage from the current test,
  with per-step capability/prerequisite semantics and correlated trace assertions.
- Add rapid-start, background/resume, and opted-in Wi-Fi loss/recovery scenarios.
- Validate every `FG-*` row and preserve lower-level tests as complementary coverage.

### Device preflight and evidence

- Discover exactly one explicit ADB serial, wake the device if needed, and evaluate
  keyguard state. A lock requests manual user action and is polled only for a bounded
  interval; no credential automation is allowed. Timeout maps to
  `NOT_RUN/DEVICE_LOCKED` and the requested operation is not replaced.
- Capture hierarchy plus targeted before/after/final/failure screenshots. Prefer the
  existing structured trace and targeted logcat to repeated images. Do not produce
  screen recordings.
- Use preserving `adb install -r -t` and direct AndroidJUnitRunner invocation as the
  primary workflow. No shell-language runner is part of correctness.

### Migration and release gates

- Execute the `research.md` responsibility table against the designated device and
  attach evidence for every row.
- Inspect/build/install/launch the signed release artifact externally and verify no
  debug trace, agent policy, exported control component, runtime flag, intent,
  instrumentation argument, or setting can activate agent mode.
- Keep PowerShell and Bash runners during implementation. A later, explicit deletion
  task may run only after parity evidence and owner acceptance; it is not part of
  specification import.

## Verification Strategy

1. Local contract/redaction/correlation tests fail first, then pass.
2. Probe-independent device scenarios pass on an API-30+ physical device without a
   probe and account for their inventory rows.
3. Live scenarios run only with explicitly configured prerequisites; absence is
   verified as NOT_RUN and a reachable lab exercises real product paths.
4. Artifact schemas validate; credential canaries are absent; all results are bounded
   and indexed.
5. Wi-Fi disruption is tested once with opt-in and once without to prove fail-closed
   behavior and recovery.
6. Release smoke and static inspection verify isolation.
7. Current wrappers are run for comparison, not deleted, until the parity matrix is
   fully accepted.
8. Functional UI results are reported separately from integration and live-hardware
   results; an integration PASS cannot promote a Functional UI row.

## Complexity Tracking

No constitutional violation or additional module is planned.
