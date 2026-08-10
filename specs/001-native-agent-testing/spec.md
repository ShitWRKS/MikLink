# Feature Specification: Native Agent-Driven Application Testing

**Feature Branch**: `001-native-agent-testing`  
**Repository Baseline**: `fix/production-readiness` at `cd629064968db3b633d0a16b6e7e4e63bf209e6d`  
**Created**: 2026-08-09  
**Status**: Ready for planning  
**Input**: Give the coding agent direct, native access to MikLink on a compatible
Android device through the debug build only, for repeatable E2E validation and
ad-hoc UI/product investigation, including live MikroTik-probe workflows, without
depending on the PowerShell runner.

## Problem and Scope

MikLink has a physical-device instrumentation scenario and structured trace, but the
agent-facing workflow is centered on host wrappers and one predefined live-probe
path. The agent needs a reusable way to operate and inspect the real app, run named
regressions, exercise live probe behavior through the product, and collect safe,
correlated evidence.

This capability is a debug-only testing mode, separate from the production app.
Production/release builds share the product behavior under test but cannot activate
the agent mode through a flag, intent, instrumentation argument, or runtime setting.

The initial feature excludes production remote control, a second RouterOS client,
unbounded probe administration, cloud device-farm operation, pixel-baseline testing,
and replacement of existing lower-level suites.

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

### Edge Cases

- The selected device disconnects, the app cannot install/start, or the installed
  build differs from the build under evaluation.
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
  MUST NOT require the existing PowerShell runner or an equivalent shell-bound
  orchestration wrapper.
- **FR-025**: Existing useful lower-level suites, traces, and both current runners
  MUST be retained until responsibility-by-responsibility replacement parity passes.
- **FR-026**: External black-box release smoke validation MUST prove the agent mode
  cannot be activated and verify representative launch and normal behavior against
  the exact release artifact without relying on embedded test controls.
- **FR-027**: Screenshot-based UI review MUST be combined with semantic/state
  evidence; pixel-baseline comparison is excluded from v1.

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
  screenshot without a one-off test class or the PowerShell runner.
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
- **SC-011**: Each current runner responsibility has accepted native replacement
  evidence before either runner is eligible for deletion.
- **SC-012**: Every factual UI/UX observation in an agent report is traceable to a
  screenshot or machine-readable state captured in that session.

## Assumptions and Dependencies

- The baseline remains `fix/production-readiness`; unrelated working-tree changes are
  outside this feature.
- A compatible physical Android device is available when device acceptance is run.
- Live scenarios require the phone to reach the explicitly configured MikroTik probe
  while the host retains device control.
- The existing PowerShell and Bash runners are transitional and are not removed by
  this specification-only work.
- Native Android/Gradle tooling is preferred over custom host orchestration.
- Agent sessions target the debug build only. Release validation is external and
  black-box; it never enables the agent mode inside the production app.
