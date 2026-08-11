# Research: Native Agent-Driven Application Testing

## Repository Findings

- The app is a single Android module using Compose, Hilt, Room, Retrofit/OkHttp,
  Moshi, DataStore, and iText. The normal test execution path is coordinated by
  `RunTestUseCaseImpl` and rendered by `TestViewModel`/`TestExecutionScreen`.
- `LiveProbeE2ETest.kt` launches `MainActivity` and exercises the UI, but uses Hilt
  entry points for fixture setup, hard-coded probe fallback values, and one monolithic
  workflow. The current working copy has an unrelated import-only correction that
  this specification work preserves.
- `DebugTraceSinkImpl` writes NDJSON only for debug while the release implementation
  is no-op. `MikroTikTestRepositoryRemote` already emits operation-level data, and
  `RunTestUseCaseImpl` emits normalization/decision events. Correlation and redaction
  need strengthening rather than a second tracing stack.
- The former host runners discovered/selected devices, invoked Gradle, passed
  scenario arguments, inspected logcat, pulled trace, validated required events,
  classified results, and arranged host artifacts. These responsibilities define
  the parity inventory now implemented by standard Android tooling.
- Current Compose tags are concentrated in dashboard and test execution. Client,
  profile, probe, history, settings, backup, and PDF flows need stable semantic
  handles for deterministic automation.

## Decisions

### D-001 — Direct exploratory control uses standard adb, not an app protocol

**Decision**: The agent operates app lifecycle and safe generic input through direct
`adb` commands against the debug build, selects an explicit serial (`-s` or
`ANDROID_SERIAL`), and captures the accessibility hierarchy and screenshots as
files. Compose test tags are exposed as resource identifiers only in debug. There is
no persistent app
server, socket, broadcast command language, or compiled test per question.

**Rationale**: adb already selects devices, installs/starts/stops apps, runs shell
commands, and captures screenshots. Compose officially supports exposing `testTag`
as a UiAutomator resource ID with `testTagsAsResourceId`. This meets the ad-hoc need
without a custom remote-control surface.

**Alternatives rejected**:

- A debug-only HTTP/socket/broadcast controller: creates a custom protocol and
  security/lifecycle burden.
- Generate a new instrumentation class for every investigation: violates the core
  ad-hoc acceptance scenario and imposes a build/deploy loop.
- Coordinate-only taps with no semantic hierarchy: too brittle and not
  machine-inspectable.

### D-002 — Named regression uses AndroidJUnitRunner, Compose test APIs, and UI Automator 2.4

**Decision**: Keep `AndroidJUnitRunner`; use Compose test APIs for in-app semantic
assertions and stable UiAutomator 2.4 for app lifecycle, cross-app/system surfaces,
stability waits, hierarchy, screenshots, and result reporting. Scenario selection
uses standard instrumentation class/method filters.

**Rationale**: AndroidJUnitRunner supports JUnit4, Compose, Espresso, UI Automator,
filtering, and sharding. UI Automator 2.4 is stable as of 2026-07-01 and provides
Kotlin-friendly selectors, bounded waits, lifecycle helpers, screenshots, and
`ResultsReporter`. Direct Gradle/adb execution already produces native XML/HTML test
reports.

**Alternative rejected**: a new desktop test framework would duplicate Android's
device/session semantics and recreate wrapper responsibilities.

### D-003 — Keep debug as the test-capable build; smoke the exact release artifact

**Decision**: Agent mode, semantic exposure, and enhanced trace are compiled and
active only in debug. Main product behavior remains shared. Release has no agent-mode
activation path; only production-safe no-op bindings may remain where normal
dependency wiring requires them. Build, inspect, install, launch, attempt forbidden
activation, and minimally navigate the release artifact externally through direct
adb as a black-box smoke gate.

**Rationale**: The repository already has a debug trace/release no-op boundary, so
debug is the single test-capable mode and a third product flavor is unnecessary.
Release smoke tooling stays outside the production app.

### D-004 — Isolate fixtures by ownership; never clear data implicitly

**Decision**: A fixture manager creates uniquely session-prefixed clients, profiles,
and reports through existing repositories/use cases and records their IDs. Cleanup
deletes only recorded entities in `finally`. A full package-data clear is a separate
explicit disposable-device mode and is never a default runner argument.

**Rationale**: Android Test Orchestrator can isolate instrumentation processes and
can clear package data, but its clear flag is destructive. Process isolation may be
adopted after compatibility validation; data isolation is still an application-level
responsibility.

### D-005 — One versioned manifest/result/trace contract

**Decision**: Each session has a manifest and each scenario has one result document.
Trace events use session/scenario/operation/exchange IDs and a schema version.
UiAutomator `ResultsReporter` registers generated files for Android Studio test
results; the trace is copied into the reporter output at finalization. Connected
Gradle retrieval MUST be verified during implementation. If a reported file is not
retrieved by that path, the agent uses direct adb pull from the manifest-listed
device path; this is standard tooling, not a repository wrapper. Direct adb
exploration writes the same artifact shapes.

**Rationale**: This maps current event validation and host artifact arrangement into
test-owned, native output while retaining NDJSON's streaming/debugging value.

### D-006 — Outcomes are explicit, scoped, and bounded

**Decision**: `PASS`, `FAIL`, `NOT_RUN`, and `SKIP` retain the meanings in FR-009.
JUnit success/failure/assumption remains compatible with tools, while the scenario
result document carries the four-way product semantics and reason. Each operation
uses the product timeout plus a bounded observation/cleanup allowance; there is no
unbounded polling.

**Rationale**: A JUnit assumption alone cannot distinguish an unavailable whole
scenario from an optional in-scenario capability in all reports.

### D-007 — Crash/ANR evidence combines test state and platform evidence

**Decision**: Record the last completed step continuously. On failure, collect UI
state/screenshot if observable and query `ApplicationExitInfo` for exits after the
session start (API 30 is the app minimum); add a targeted session-time logcat excerpt
when available. Do not clear all logcat as a correctness prerequisite.

**Rationale**: `ApplicationExitInfo` distinguishes Java/native crashes and ANRs on
API 30+, but not every hang necessarily produces a historical exit. Bounded UI waits,
instrumentation failures, and targeted logcat complement it.

### D-008 — Live prerequisites are explicit and secret-free

**Decision**: Use only the singleton probe explicitly saved/selected in MikLink for
the session. No address, username, or password defaults and no credentials passed as
instrumentation/Gradle arguments. Probe reachability, authentication, interface/TDR
capability, and speed-server availability produce structured prerequisite outcomes.

**Rationale**: Command-line arguments and build output are poor secret transports,
and the existing fallback is lab-specific rather than a product contract.

### D-009 — Connectivity fault injection is Android-side, gated, and reversible

**Decision**: Only the designated physical-device scenario may change Wi-Fi state,
only with explicit per-session opt-in and retained adb control. Record the initial
state, restore it in `finally`, verify recovery, and fail cleanup visibly. Do not
modify RouterOS directly.

**Alternative rejected**: probe-side fault injection requires a privileged RouterOS
backchannel and expands the security/safety scope.

### D-010 — Migration is responsibility-by-responsibility

| Current wrapper responsibility | Native replacement and acceptance evidence |
|---|---|
| Discover device state | `adb devices -l`; explicit serial; session prerequisite result |
| Select target | `-s`/`ANDROID_SERIAL`; ambiguity is NOT_RUN |
| Clear logcat | Removed as correctness step; correlation IDs and session-time filtering |
| Invoke Gradle/test | `connectedDebugAndroidTest` or direct `am instrument`; class/method filters |
| Provide client/profile/probe defaults | Session-owned fixtures and explicitly saved probe; no secrets/default probe |
| Collect stdout/JUnit | Native Gradle XML/HTML/instrumentation output |
| Pull screenshots/trace | UI Automator `ResultsReporter`; direct adb for exploration/release smoke |
| Check required trace events | On-device assertions against versioned trace contract |
| Detect crash/ANR | Bounded runner state, screenshot/hierarchy, `ApplicationExitInfo`, targeted logcat |
| Classify PASS/FAIL/NOT_RUN | JUnit mapping plus `scenario-result.json` |
| Arrange host artifacts | Gradle/Android Studio test outputs and manifest-indexed files |

Every required responsibility has accepted evidence on the supported physical
device. After explicit owner acceptance, the host runners were retired and the
native Gradle/ADB/AndroidJUnitRunner workflow became authoritative.

## Primary Sources

- [AndroidJUnitRunner and Test Orchestrator](https://developer.android.com/training/testing/instrumented-tests/androidx-test-libraries/runner)
- [Run Android tests from the command line](https://developer.android.com/studio/test/command-line)
- [Android Debug Bridge](https://developer.android.com/tools/adb)
- [UI Automator 2.4 guide](https://developer.android.com/training/testing/other-components/ui-automator)
- [UI Automator 2.4 release notes](https://developer.android.com/jetpack/androidx/releases/test-uiautomator)
- [Compose and UI Automator interoperability](https://developer.android.com/develop/ui/compose/testing/interoperability)
- [UI Automator ResultsReporter](https://developer.android.com/reference/androidx/test/uiautomator/ResultsReporter)
- [Advanced Android test setup](https://developer.android.com/studio/test/advanced-test-setup)
- [ApplicationExitInfo](https://developer.android.com/reference/android/app/ApplicationExitInfo)
- [Android ANR diagnostics](https://developer.android.com/topic/performance/vitals/anr)
