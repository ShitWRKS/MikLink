# Feature Specification: Native Agent-Driven Application Testing

**Feature Branch**: `001-native-agent-testing-functional-acceptance`  
**Repository Baseline**: `develop` at `fa2e9d15e542850956e3db92bdadd2b41dbfe9d4`  
**Created**: 2026-08-09  
**Status**: In implementation  
**Input**: Give the coding agent direct, native access to MikLink on a compatible
Android device through the debug build only, for repeatable E2E validation and
ad-hoc UI/product investigation, including live MikroTik-probe workflows, without
depending on a repository host runner.

## Problem and Scope

MikLink has a physical-device instrumentation scenario and structured trace, but the
agent-facing workflow previously depended on host orchestration and one predefined live-probe
path. The agent needs a reusable way to operate and inspect the real app, run named
regressions, exercise live probe behavior through the product, and collect safe,
correlated evidence.

This capability is a debug-only testing mode, separate from the production app.
Production/release builds share the product behavior under test but cannot activate
the agent mode through a flag, intent, instrumentation argument, or runtime setting.

The initial feature excludes production remote control, a second RouterOS client,
unbounded probe administration, cloud device-farm operation, pixel-baseline testing,
and replacement of existing lower-level suites.

## Acceptance Levels and Fidelity Rule

Coverage is reported independently at four levels:

1. **Integration coverage** verifies repositories, use cases, persistence, codecs,
   generators, and presentation mapping without claiming a user journey.
2. **Functional UI acceptance** drives the primary user journey through rendered UI
   on a physical device and observes persistence/results through that same UI.
3. **Live-hardware acceptance** extends a Functional UI journey through the normal
   networking stack to the explicitly configured MikroTik probe.
4. **Agent exploratory validation** permits bounded ad-hoc UI operation and evidence
   capture without creating a scenario-specific test class.

> **Fixtures may arrange a scenario. They MUST NOT replace through internal APIs the
> functionality that the scenario claims to test.**

Repository/use-case fixtures may prepare a client and profile for a test-execution
scenario, and may support cleanup or diagnostic verification. A client/profile CRUD,
settings, history, report-settings, or PDF-export Functional UI result is accepted
only when the declared behavior is performed through the normal UI. Existing tests
that bypass that UI remain valuable integration coverage and are not removed.

## Clarifications

### Session 2026-08-09

- **Local reset authority**: full local-data reset is allowed only when the current
  session explicitly designates the device state as disposable. Otherwise, scenarios
  create/isolate and clean up only their own records.
- **Wi-Fi interruption authority**: connectivity disruption requires a separately
  named per-session opt-in on a designated physical device, retained host control,
  and best-effort restoration verified during cleanup.
- **Direct RouterOS manipulation**: out of scope for v1. Faults and mutations are
  exercised through MikLink or the Android device environment, never a test-only
  RouterOS administration backchannel.
- **Probe source**: live scenarios use the probe explicitly configured for the test
  session; hard-coded address or credentials are forbidden.
- **Visual scope**: screenshots, semantic/state assertions, and evidence-backed agent
  review are required. Pixel-diff baselines are out of scope for v1.
- **Speed-test dependency**: a configured server is an explicit scenario
  prerequisite. Its absence yields NOT_RUN for that scenario and does not fail
  unrelated coverage.
- **Host support**: the primary contract is host-neutral standard Android/Gradle
  tooling. Shell-specific convenience wrappers are not required for symmetry.
- **Build boundary**: agent control and enhanced diagnostics are compiled and active
  only in the debug build. The production/release app has no activation path for the
  mode; an external black-box release smoke check proves the separation while normal
  product behavior remains representative.
- Q: In which build may the agent testing mode be active? → A: Debug build only;
  production/release builds cannot activate it.

No owner decision remains unresolved for planning.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Directly inspect and operate MikLink (Priority: P1)

As a developer using a coding agent, I want the agent to operate a selected Android
test device so I can request a safe UI or behavior investigation without manually
performing it or adding a scenario-specific test class.

**Why this priority**: This reusable exploratory capability is the gap not addressed
by the existing fixed live-probe scenario.

**Independent Test**: Launch the debug build of MikLink, navigate to a named screen,
perform a safe interaction, inspect semantic UI state, and capture a screenshot
without adding and compiling a one-off test class.

**Acceptance Scenarios**:

1. **Given** exactly one selected compatible device with the debug build installed,
   **when** an exploratory session starts, **then** the agent can start/restart the
   app, navigate, interact, assert observable state, and capture machine-readable UI
   evidence plus a screenshot.
2. **Given** zero, multiple, offline, or unauthorized devices, **when** a session is
   requested, **then** it ends as NOT_RUN with the precise prerequisite reason and
   never silently selects a device.
3. **Given** the app crashes or becomes unresponsive, **when** the session finalizes,
   **then** it is FAIL and includes the last successful step and available diagnostics.
4. **Given** a requested state cannot be reached, **when** the agent reports its
   review, **then** it identifies the unmet step instead of describing unseen UI.

---

### User Story 2 - Run whole-application regression (Priority: P1)

As a developer, I want maintained device scenarios across the app's feature groups
so I can detect regressions and rerun affected coverage after a change.

**Why this priority**: Exploratory access does not replace repeatable assertions.

**Independent Test**: Run the probe-independent catalog on a compatible device and
obtain a structured result for every scenario and the suite.

**Acceptance Scenarios**:

1. **Given** application-only prerequisites, **when** the catalog runs, **then** every
   maintained scenario reaches PASS, FAIL, NOT_RUN, or SKIP with reason and evidence.
2. **Given** a missing external prerequisite, **when** the suite runs, **then** only
   dependent scenarios are NOT_RUN and unrelated scenarios continue.
3. **Given** a product assertion fails, **when** the run ends, **then** the outcome is
   distinguishable from setup, hardware, and optional-step outcomes.
4. **Given** a new app build is installed, **when** one affected scenario and then the
   applicable catalog are rerun, **then** evidence identifies that exact build.

---

### User Story 3 - Validate live-probe exchanges (Priority: P1)

As a developer, I want requests, responses, normalization, decisions, and visible
results correlated for real probe operations so integration failures can be diagnosed
from evidence rather than inferred from UI symptoms.

**Why this priority**: Probe communication is core behavior that UI-only assertions
cannot validate fully.

**Independent Test**: Run one supported operation against the explicitly configured
probe and reconstruct its sanitized request-to-visible-result chain.

**Acceptance Scenarios**:

1. **Given** a configured reachable probe, **when** MikLink runs an operation through
   its normal path, **then** evidence correlates scenario, operation, sanitized request,
   response/error, parsed result, decision, and visible outcome where applicable.
2. **Given** request or response material contains protected data, **when** evidence is
   persisted, **then** no credential or protected value appears in any artifact.
3. **Given** the probe, selected interface, capability, or speed server is unavailable,
   **when** preconditions are evaluated, **then** the dependent step or scenario has
   the defined NOT_RUN/SKIP outcome without a fabricated success.

---

### User Story 4 - Exercise failure and recovery (Priority: P2)

As a developer, I want controlled connectivity and lifecycle disturbances so MikLink
does not remain stuck or report misleading results in field conditions.

**Why this priority**: The product depends on Wi-Fi, a physical probe, and Android
lifecycle transitions.

**Independent Test**: With explicit disruption authorization, interrupt connectivity
during one probe operation, restore it, and verify bounded failure and recovery.

**Acceptance Scenarios**:

1. **Given** an opted-in probe-dependent operation, **when** device connectivity is
   interrupted, **then** the operation reaches a defined terminal state within its
   declared bound and produces failure evidence.
2. **Given** connectivity is restored, **when** the documented recovery path runs,
   **then** MikLink becomes usable without reinstallation and cleanup verifies Wi-Fi.
3. **Given** rapid/repeated starts or a foreground/background transition, **when** the
   action occurs, **then** state remains deterministic and internally consistent.
4. **Given** disruption opt-in or safe host control is absent, **when** the scenario is
   selected, **then** it is NOT_RUN and does not alter connectivity.

---

### User Story 5 - Review UI/UX from actual device state (Priority: P2)

As a product owner, I want an agent to inspect real rendered screens and report what
is visible and how interactions behave.

**Why this priority**: Product review needs observed behavior, not source inference.

**Independent Test**: Ask for a review of a named reachable state and receive current
screenshots, semantic state, actions, and an evidence-linked description.

**Acceptance Scenarios**:

1. **Given** a reachable state, **when** a UI review is requested, **then** every
   factual observation is supported by current device evidence.
2. **Given** an interaction changes state, **when** the review reports it, **then** the
   before/after action and resulting state are preserved.
3. **Given** a prerequisite is missing, **when** the review ends, **then** the report
   says what was not observed and why.

---

### User Story 6 - Complete functional UI acceptance (Priority: P1)

As a maintainer, I want independently runnable physical-device scenarios that use
MikLink like a user so CRUD, settings, execution, history, results, and export defects
cannot be hidden by repository-level round trips.

**Why this priority**: A green integration catalog does not prove that users can
reach, operate, save, reopen, or remove data from the real screens.

**Independent Test**: Install the debug app and test APK with preserving updates,
run one Functional UI class through AndroidJUnitRunner, and obtain a terminal result
and targeted before/after/failure evidence for its actual UI journey.

**Acceptance Scenarios**:

1. **Given** an unlocked compatible device, **when** the app-only Functional UI
   catalog runs, **then** launch/navigation, client CRUD, profile CRUD, representative
   settings/report-settings persistence, history/detail actions, result presentation,
   and PDF export traverse their normal rendered UI.
2. **Given** session-owned names and records, **when** a CRUD scenario completes,
   **then** creation, reopening, meaningful update, persistence, and supported deletion
   are observed through UI, while cleanup remains limited to session-owned records.
3. **Given** a real completed test result, **when** history and PDF scenarios run,
   **then** the report is found/opened through UI and export produces a retrievable,
   non-trivial PDF with valid PDF header and EOF.
4. **Given** the device is initially locked, **when** preflight runs, **then** it wakes
   the device, reports that user unlock is required, waits for a bounded interval, and
   resumes the same operation after unlock; timeout is NOT_RUN/DEVICE_LOCKED.

### Edge Cases

- The selected device disconnects, the app cannot install/start, or the installed
  build differs from the build under evaluation.
- The selected device is connected but locked; this is an unlock prerequisite, not
  an immediate product failure, and credentials/biometrics are never automated.
- The process is killed, crashes, produces an observable ANR, or the UI changes while
  an exploratory action is being evaluated.
- Probe configuration is absent; authentication fails; a response is empty, delayed,
  malformed, or unsupported; capability discovery differs by interface.
- Existing local data conflicts with fixture setup or cleanup is interrupted.
- An action exceeds the authorized app/device/probe mutation scope.
- A generated report/PDF cannot be retrieved or opened.
- The agent mode is requested against a production/release build; the request must
  be rejected or classified NOT_RUN without enabling any embedded test capability.
- Nested, unlabelled, or serialized values could reveal protected information even
  when their field name is not obviously sensitive.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The debug build MUST provide bounded exploratory and named test sessions
  on an explicitly selected compatible Android device.
- **FR-002**: Device discovery MUST distinguish zero, one, multiple, offline,
  unauthorized, and disconnected-device conditions without arbitrary selection.
- **FR-003**: The agent MUST be able to start, stop, restart, navigate, interact with,
  inspect, assert, and screenshot the test-capable app using a host-neutral workflow.
- **FR-004**: Ad-hoc interaction sequences MUST NOT require a new scenario-specific
  compiled test class.
- **FR-005**: Named scenarios MUST be independently selectable and collectively
  runnable as the applicable regression catalog.
- **FR-006**: The catalog MUST account for every repository-backed user-visible
  feature group in `coverage-inventory.md` and classify its validation path.
- **FR-007**: Probe-independent scenarios MUST run without a live probe.
- **FR-008**: External prerequisites MUST be declared and checked before dependent
  assertions; missing prerequisites MUST not be PASS or product FAIL.
- **FR-009**: PASS means all required assertions ran and passed; FAIL means a runnable
  scenario violated an assertion, crashed, became observably unresponsive, or failed
  during execution; NOT_RUN means a required scenario prerequisite was unavailable
  before evaluation; SKIP means an optional/not-applicable step, or a scenario
  explicitly excluded by the selected catalog policy in a runnable environment, was
  intentionally omitted.
- **FR-010**: Every scenario MUST reach one terminal classification within its
  declared bound and provide a machine-readable reason.
- **FR-011**: Failure evidence MUST identify the last successful step and include a
  screenshot when the device remains observable, plus available crash/ANR evidence.
- **FR-012**: Evidence MUST identify build/version, selected device, session,
  scenario, timestamps, prerequisites, actions, assertions, cleanup, and outcomes.
- **FR-013**: Live evidence MUST correlate each normal-path probe operation with
  sufficient sanitized request and response/error information and the applicable
  parsing, normalization, threshold, decision, and UI result stages.
- **FR-014**: Credentials, authorization material, cookies, private keys, and other
  protected configuration or payload values MUST be redacted before persistence.
- **FR-015**: Test data MUST be isolated or limited to session-created records unless
  disposable-state reset is explicitly authorized for that session.
- **FR-016**: Cleanup MUST be idempotent, record its result, and preserve unrelated
  application data.
- **FR-017**: Connectivity disruption MUST require a distinct opt-in, retained host
  control, bounded execution, and verified best-effort restoration.
- **FR-018**: Probe mutations MUST occur only through normal MikLink behavior; direct
  test-only RouterOS manipulation is prohibited in v1.
- **FR-019**: Live tests MUST use the explicitly configured session probe and MUST NOT
  contain fallback addresses or credentials.
- **FR-020**: The configured speed-test server MUST be treated as an external
  prerequisite and affect only dependent coverage.
- **FR-021**: Rapid/repeated starts, foreground/background transitions, probe loss,
  recovery, report/PDF generation, retrieval, and basic content validity MUST be
  covered by maintained scenarios.
- **FR-022**: Agent mode, test control, and enhanced diagnostics MUST be compiled,
  registered, and activatable only in the debug build. Production/release artifacts
  MUST expose no activation path through runtime flags, intents, instrumentation
  arguments, exported components, or settings. A shared architectural abstraction
  MAY remain only as a production-safe no-op required by normal dependency wiring.
- **FR-023**: Live validation MUST exercise the production UI/domain/repository/
  networking/export paths, not duplicate them in test code.
- **FR-024**: The primary workflow MUST use standard Android/Gradle capabilities and
  MUST NOT require repository shell-bound orchestration.
- **FR-025**: Existing useful lower-level suites and traces MUST be retained;
  transitional host orchestration MAY be retired only after responsibility-by-
  responsibility replacement parity passes and the owner accepts removal.
- **FR-026**: External black-box release smoke validation MUST prove the agent mode
  cannot be activated and verify representative launch and normal behavior against
  the exact release artifact without relying on embedded test controls.
- **FR-027**: Screenshot-based UI review MUST be combined with semantic/state
  evidence; pixel-baseline comparison is excluded from v1.
- **FR-028**: A feature group MUST NOT be reported as Functional UI PASS unless its
  primary user journey was executed through the rendered UI on a physical device;
  repository/use-case round trips are integration evidence only.
- **FR-029**: The app-only Functional UI catalog MUST provide independently runnable
  UI journeys for launch/navigation, configured-probe open/save/reopen, client CRUD, profile CRUD, representative app
  settings, report settings, history/detail, result presentation, and PDF export.
- **FR-030**: Functional UI fixtures MAY arrange unrelated prerequisites and perform
  ID-scoped cleanup/diagnostics, but MUST NOT perform the behavior under test.
- **FR-031**: Device preflight MUST wake a connected device when needed, distinguish
  a locked device, request manual user unlock, wait for a bounded interval, and map an
  unresolved lock to NOT_RUN/DEVICE_LOCKED without attempting PIN/password/biometric
  automation.
- **FR-032**: Functional evidence MUST use targeted before/after/final/failure images,
  semantic hierarchy snapshots, structured trace, targeted logcat, and generated
  files when applicable; screen recording/video is prohibited.
- **FR-033**: PDF Functional UI acceptance MUST follow the visible export action and
  validate a retrieved file for existence, non-trivial size, PDF header, and EOF.

### Key Entities

- **Test Session**: bounded validation on one device/build with policy and artifacts.
- **Test Scenario**: named prerequisites, steps, assertions, cleanup, and outcome.
- **Exploratory Action**: generic interaction or observation within a session.
- **Prerequisite**: condition required to evaluate a scenario or optional step.
- **Probe Exchange**: sanitized, correlated request and response/error evidence.
- **Artifact Manifest**: identities, correlations, files, redaction status, and result.
- **Terminal Result**: PASS, FAIL, NOT_RUN, or SKIP with reason and timestamps.
- **Test Data Set**: session-owned or explicitly disposable local state.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: On a compatible physical device running the debug build, an agent
  completes launch, navigation, interaction, semantic inspection, assertion, and
  screenshot without a one-off test class or repository host runner.
- **SC-002**: 100% of maintained scenarios and sessions end with a machine-readable
  terminal classification and reason; none wait without a declared bound.
- **SC-003**: 100% of unavailable hardware/external prerequisites in acceptance runs
  are distinguishable from product failures and passes.
- **SC-004**: Every RouterOS operation in the live acceptance catalog has one
  correlated sanitized request and response/error chain through the applicable final
  decision and visible result.
- **SC-005**: Automated artifact scans and review find zero live-probe credentials or
  protected values in accepted artifacts.
- **SC-006**: A controlled probe disconnect always reaches a terminal outcome,
  attempts restoration, records cleanup, and does not require app reinstallation.
- **SC-007**: One affected scenario and then 100% of the applicable catalog can be
  rerun against an identified newly deployed build using the same capability.
- **SC-008**: The repository-backed inventory accounts for 100% of in-scope visible
  feature groups and every group has at least one accepted validation path.
- **SC-009**: 100% of probe-independent scenarios remain runnable without the probe.
- **SC-010**: Release inspection and external smoke validation find zero active
  agent-control entry points or enhanced diagnostic payloads, and every attempted
  runtime activation path remains unavailable.
- **SC-011**: Each transitional host-runner responsibility has accepted native
  replacement evidence before host orchestration is eligible for deletion.
- **SC-012**: Every factual UI/UX observation in an agent report is traceable to a
  screenshot or machine-readable state captured in that session.
- **SC-013**: Every applicable operational feature group has at least one accepted
  Functional UI path, while integration-only evidence is never counted as that PASS.
- **SC-014**: A connected initially locked device either resumes the same requested
  run after manual unlock or terminates as NOT_RUN/DEVICE_LOCKED within the bound.
- **SC-015**: Accepted Functional UI sessions contain no screen recording and no
  screenshots outside critical before/after/final/failure evidence points.

## Assumptions and Dependencies

- The baseline remains `fix/production-readiness`; unrelated working-tree changes are
  outside this feature.
- A compatible physical Android device is available when device acceptance is run.
- Live scenarios require the phone to reach the explicitly configured MikroTik probe
  while the host retains device control.
- Native parity and owner acceptance authorize retirement of the transitional host
  runners; standard Android/Gradle tooling is the sole supported workflow.
- Native Android/Gradle tooling is preferred over custom host orchestration.
- Agent sessions target the debug build only. Release validation is external and
  black-box; it never enables the agent mode inside the production app.
