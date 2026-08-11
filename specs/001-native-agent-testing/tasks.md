# Tasks: Native Agent-Driven Application Testing

**Input**: Design documents in `/specs/001-native-agent-testing/`  
**Tests**: Required by the specification; write the listed test before its paired
implementation and confirm the intended failure.

## Format

`[ID] [P?] [Story?] Description with exact path`

- `[P]` means different files and no unfinished dependency conflict.
- `[US#]` maps work to the independently testable story in `spec.md`.

## Phase 1: Setup

**Purpose**: Pin native Android test capabilities and build identity.

- [X] T001 Add stable UI Automator 2.4.0 and required AndroidX Test aliases in `gradle/libs.versions.toml`
- [X] T002 Add the UI Automator androidTest dependency, deterministic test options, and source revision in `app/build.gradle.kts` without introducing an agent-mode runtime or `BuildConfig` activation flag
- [X] T003 [P] Define catalog groups, scenario IDs, and feature-group mappings in `app/src/androidTest/java/com/app/miklink/e2e/catalog/E2ETestCatalog.kt`

---

## Phase 2: Foundational Evidence, State, and Safety

**Purpose**: Shared contracts that block all user-story scenarios.

- [X] T004 [P] Add failing JSON contract and atomic-finalization tests in `app/src/androidTest/java/com/app/miklink/e2e/support/EvidenceContractTest.kt`
- [X] T005 [P] Extend failing nested/map/list/serialized-value and truncation tests in `app/src/test/java/com/app/miklink/core/domain/test/logging/LogSanitizerTest.kt`
- [X] T006 [P] Add failing terminal-state, timeout, last-step, and cleanup-transition tests in `app/src/androidTest/java/com/app/miklink/e2e/support/TestSessionStateTest.kt`
- [X] T007 Implement session, scenario, prerequisite, step, cleanup, artifact, device, and build models in `app/src/androidTest/java/com/app/miklink/e2e/support/TestEvidenceModels.kt`
- [X] T008 Implement versioned JSON/NDJSON serialization and atomic result finalization in `app/src/androidTest/java/com/app/miklink/e2e/support/EvidenceWriter.kt`
- [X] T009 Implement four-way outcome-to-JUnit mapping, bounded execution, and always-run cleanup in `app/src/androidTest/java/com/app/miklink/e2e/support/ScenarioRule.kt`
- [X] T010 Implement screenshot, hierarchy, trace, PDF, digest, ResultsReporter registration, and manifest-listed adb retrieval fallback in `app/src/androidTest/java/com/app/miklink/e2e/support/ArtifactCollector.kt`
- [X] T011 [P] Implement explicit device/probe/capability/speed-server/policy prerequisite results in `app/src/androidTest/java/com/app/miklink/e2e/support/PrerequisiteEvaluator.kt`
- [X] T012 [P] Add failing session-owned fixture/isolation/idempotent-cleanup tests in `app/src/androidTest/java/com/app/miklink/e2e/support/TestFixtureManagerTest.kt`
- [X] T013 Implement uniquely prefixed client/profile/report creation and ID-scoped cleanup through existing repositories/use cases in `app/src/androidTest/java/com/app/miklink/e2e/support/TestFixtureManager.kt`
- [X] T014 [P] Add failing API-30 crash/ANR/session-time filtering tests in `app/src/androidTest/java/com/app/miklink/e2e/support/ProcessFailureCollectorTest.kt`
- [X] T015 Implement last-visible-state, `ApplicationExitInfo`, and targeted logcat collection in `app/src/androidTest/java/com/app/miklink/e2e/support/ProcessFailureCollector.kt`
- [X] T016 Implement recursive/value-aware pre-serialization redaction and payload size bounds in `app/src/main/java/com/app/miklink/core/domain/test/logging/LogSanitizer.kt`
- [X] T017 Add session/scenario/operation/exchange correlation fields while preserving generation ownership in `app/src/main/java/com/app/miklink/core/domain/test/logging/DebugTraceRunContext.kt` and `DebugTraceSink.kt`
- [X] T018 Update debug NDJSON schema/output registration and retain only the production-safe no-op binding required by dependency wiring in `app/src/debug/java/com/app/miklink/core/domain/test/logging/DebugTraceSinkImpl.kt` and `app/src/release/java/com/app/miklink/core/domain/test/logging/DebugTraceSinkImpl.kt`

**Checkpoint**: Contract tests pass; destructive permissions default false; a forced
failure produces a schema-valid result and safe artifacts.

---

## Phase 3: User Story 1 — Direct Inspection and Operation (P1 MVP)

**Goal**: Direct adb exploration can reliably find, operate, and observe the app
without a new compiled scenario class.

**Independent Test**: Follow `quickstart.md` to launch, navigate, interact, dump
semantic state, assert, and screenshot on one explicitly selected device.

### Tests

- [X] T019 [P] [US1] Add failing debug-enabled root semantics and resource-ID exposure tests in `app/src/androidTest/java/com/app/miklink/e2e/catalog/AgentSemanticsIsolationTest.kt`
- [X] T020 [P] [US1] Add failing uniqueness/reachability assertions for stable tags across the navigation graph in `app/src/androidTest/java/com/app/miklink/e2e/catalog/SemanticSurfaceTest.kt`

### Implementation

- [X] T021 [P] [US1] Add source-set-specific enabled/disabled semantic exposure policies in `app/src/debug/java/com/app/miklink/ui/testing/AgentSemanticsConfig.kt` and `app/src/release/java/com/app/miklink/ui/testing/AgentSemanticsConfig.kt`
- [X] T022 [US1] Enable `testTagsAsResourceId` once at the Compose root under the test-capable policy in `app/src/main/java/com/app/miklink/MainActivity.kt`
- [X] T023 [P] [US1] Consolidate stable dashboard/execution/result semantic identifiers in `app/src/main/java/com/app/miklink/ui/dashboard/DashboardTags.kt` and `app/src/main/java/com/app/miklink/ui/test/components/TestExecutionTags.kt`
- [X] T024 [P] [US1] Add stable semantic identifiers to probe, client, and profile flows in `app/src/main/java/com/app/miklink/ui/probe/ProbeEditScreen.kt`, `app/src/main/java/com/app/miklink/ui/client/ClientListScreen.kt`, `ClientEditScreen.kt`, `app/src/main/java/com/app/miklink/ui/profile/TestProfileListScreen.kt`, and `TestProfileEditScreen.kt`
- [X] T025 [P] [US1] Add stable semantic identifiers to history, report, settings, PDF, and backup flows in `app/src/main/java/com/app/miklink/ui/history/HistoryScreen.kt`, `ReportDetailScreen.kt`, `PdfExportDialog.kt`, `app/src/main/java/com/app/miklink/ui/settings/SettingsScreen.kt`, `PdfSettingsScreen.kt`, and `BackupSettingsScreen.kt`
- [X] T026 [US1] Document bounded debug-only adb lifecycle/input/hierarchy/screenshot/result commands, a debuggability precheck, release refusal, and forbidden secret/destructive operations in `docs/reference/testing.md`
- [X] T027 [US1] Execute the no-one-off-class ad-hoc acceptance flow and validate its files against `specs/001-native-agent-testing/contracts/` with outputs under `app/build/outputs/agent-tests/ad-hoc/`

**Checkpoint**: US1 is demonstrable independently and release semantics remain off.

---

## Phase 4: User Story 2 — Whole-Application Regression (P1)

**Goal**: Probe-independent catalog covers every applicable visible feature group and
supports targeted/full reruns with structured terminal results.

**Independent Test**: Run only the app-only catalog with no probe and obtain one
terminal result per scenario plus an inventory accounting report.

### Tests and Scenarios

- [X] T028 [P] [US2] Add dashboard launch/setup/selection assertions for FG-01 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/DashboardScenarioTest.kt`
- [X] T029 [P] [US2] Add session-owned client CRUD and validation scenario for FG-03 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/ClientScenarioTest.kt`
- [X] T030 [P] [US2] Add session-owned profile flags/targets/thresholds CRUD scenario for FG-04 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/ProfileScenarioTest.kt`
- [X] T031 [P] [US2] Add probe configuration UI validation without requiring reachability for FG-02 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/ProbeConfigurationScenarioTest.kt`
- [X] T032 [P] [US2] Add settings/locale/polling/glow/numbering/protocol round-trip scenario for FG-09 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/SettingsScenarioTest.kt`
- [X] T033 [P] [US2] Add history/search/detail/delete/duplicate/repeat scenario for FG-06 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/HistoryReportScenarioTest.kt`
- [X] T034 [P] [US2] Add backup export/import round trip with session-owned data and artifact exclusion for FG-08 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/BackupScenarioTest.kt`
- [X] T035 [P] [US2] Add PDF preference/generation/retrieval/non-empty/signature/basic-open scenario for FG-07 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/PdfScenarioTest.kt`
- [X] T036 [P] [US2] Add data-driven Link/TDR/network/neighbors/ping/speed renderer assertions for FG-10 in `app/src/androidTest/java/com/app/miklink/e2e/catalog/ResultPresentationScenarioTest.kt`
- [X] T037 [US2] Add catalog selection, per-scenario continuation, aggregate outcome, and inventory-accounting tests in `app/src/androidTest/java/com/app/miklink/e2e/catalog/ApplicationCatalogTest.kt`
- [X] T038 [US2] Run targeted and full probe-independent catalog acceptance and update executed evidence/status columns in `specs/001-native-agent-testing/coverage-inventory.md`

**Checkpoint**: US2 passes without a live probe; missing external prerequisites do
not stop unrelated scenarios.

---

## Phase 5: User Story 3 — Live-Probe Exchange Validation (P1)

**Goal**: Real operations use the configured probe and yield a complete, correlated,
secret-free chain through the visible result.

**Independent Test**: Run one live Ping or Link scenario and validate its complete
trace chain against `trace-event.schema.json`.

### Tests

- [X] T039 [P] [US3] Add failing request/response/error correlation and schema tests in `app/src/test/java/com/app/miklink/data/repository/mikrotik/MikroTikTraceContractTest.kt`
- [X] T040 [P] [US3] Add failing configured-probe/no-fallback/auth/capability/speed prerequisite tests in `app/src/androidTest/java/com/app/miklink/e2e/catalog/LiveProbePrerequisiteTest.kt`
- [X] T041 [P] [US3] Add failing credential-canary scans for all live artifact types in `app/src/androidTest/java/com/app/miklink/e2e/support/ArtifactSecretScanTest.kt`

### Implementation and Scenarios

- [X] T042 [US3] Emit sanitized correlated probe request/response/error events at the existing product boundary in `app/src/main/java/com/app/miklink/data/repository/mikrotik/MikroTikTestRepositoryRemote.kt`
- [X] T043 [US3] Propagate session/scenario/operation correlations through normal execution decisions in `app/src/main/java/com/app/miklink/core/domain/usecase/test/RunTestUseCaseImpl.kt`
- [X] T044 [US3] Replace hard-coded probe fallbacks and monolithic setup with explicit selected-probe/session-owned fixtures in `app/src/androidTest/java/com/app/miklink/e2e/LiveProbeE2ETest.kt`
- [X] T045 [US3] Split independently selectable Link/TDR/network/neighbors/ping/speed live cases with per-step NOT_RUN/SKIP semantics in `app/src/androidTest/java/com/app/miklink/e2e/catalog/LiveProbeScenarioTest.kt`
- [X] T046 [US3] Assert request→response/error→parse→normalize→threshold→decision→UI completeness and scan collected evidence in `app/src/androidTest/java/com/app/miklink/e2e/catalog/LiveProbeEvidenceTest.kt`
- [X] T047 [US3] Run the replacement and current live workflows on the same configured lab and record parity evidence in `specs/001-native-agent-testing/runner-parity.md`

**Checkpoint**: US3 provides one accepted real-operation trace with zero secret
findings; retirement remained gated on the later parity decision.

---

## Phase 6: User Story 4 — Failure and Recovery (P2)

**Goal**: Repeated actions, lifecycle change, probe loss, and recovery always reach a
bounded, evidenced state without unauthorized mutation.

**Independent Test**: Opt into Wi-Fi disruption on the designated device, interrupt
one live operation, restore connectivity, and verify terminal/cleanup evidence.

### Tests

- [X] T048 [P] [US4] Add rapid/repeated start ownership and terminal-state device tests in `app/src/androidTest/java/com/app/miklink/e2e/catalog/RapidStartScenarioTest.kt`
- [X] T049 [P] [US4] Add foreground/background/resume consistency device tests in `app/src/androidTest/java/com/app/miklink/e2e/catalog/LifecycleScenarioTest.kt`
- [X] T050 [P] [US4] Add failing no-opt-in, lost-host-control, restore, and cleanup-policy tests in `app/src/androidTest/java/com/app/miklink/e2e/support/WifiDisruptionControllerTest.kt`

### Implementation and Scenarios

- [X] T051 [US4] Implement explicit-policy Android-side Wi-Fi state capture/disruption/restoration with `finally` cleanup in `app/src/androidTest/java/com/app/miklink/e2e/support/WifiDisruptionController.kt`
- [X] T052 [US4] Add the opted-in probe-loss/terminal/recovery scenario in `app/src/androidTest/java/com/app/miklink/e2e/catalog/ConnectivityRecoveryScenarioTest.kt`
- [X] T053 [US4] Verify crash, observable ANR, timeout, lost-device, and cleanup-failure artifacts in `app/src/androidTest/java/com/app/miklink/e2e/catalog/FailureEvidenceScenarioTest.kt`

**Checkpoint**: US4 passes both fail-closed and opted-in paths; initial Wi-Fi state is
verified restored or cleanup is FAIL.

---

## Phase 7: User Story 5 — Evidence-Backed UI/UX Review (P2)

**Goal**: Agent reports contain only UI facts observed in the current device session.

**Independent Test**: Review one reachable and one intentionally unreachable state;
validate screenshots, hierarchy, actions, and observation claims.

- [X] T054 [P] [US5] Add the factual-observation/evidence-link/unseen-state checklist to `docs/reference/testing.md`
- [X] T055 [P] [US5] Add UI review artifact validation and before/after correlation tests in `app/src/androidTest/java/com/app/miklink/e2e/support/UiReviewEvidenceTest.kt`
- [X] T056 [US5] Perform reachable and unreachable ad-hoc reviews and store schema-valid acceptance outputs under `app/build/outputs/agent-tests/ui-review/`

**Checkpoint**: US5 observations are traceable; unreachable UI is explicitly reported.

---

## Phase 8: Release, Parity, and Final Verification

- [X] T057 [P] Add debug/release source-set, manifest, and forbidden-control scans proving no release activation flag, intent, instrumentation argument, exported component, or setting in `app/src/test/java/com/app/miklink/quality/ReleaseIsolationScanTest.kt`
- [X] T058 [P] Update native commands, outcome semantics, prerequisites, artifacts, and safety policy in `docs/reference/testing.md` and `docs/reference/production-readiness.md`
- [X] T059 Run `testDebugUnitTest`, lint, and all existing quality/golden/contract suites and record results in `specs/001-native-agent-testing/verification.md`
- [X] T060 Run the complete applicable physical-device catalog and validate every artifact against `specs/001-native-agent-testing/contracts/`
- [ ] T061 Build/inspect/install the exact signed release artifact, externally attempt every forbidden activation path, run black-box smoke navigation, and record absence of control/trace exposure in `specs/001-native-agent-testing/verification.md`
- [X] T062 Complete every responsibility row in `specs/001-native-agent-testing/runner-parity.md`; owner accepted contract-level recovery and excluded destructive disruption rehearsal as a retirement blocker
- [X] T063 Reconcile 100% of `FG-*`, `FR-*`, and `SC-*` coverage and final results in `specs/001-native-agent-testing/coverage-inventory.md` and `verification.md`
- [X] T064 Confirm transitional runners at the parity checkpoint; superseded by owner-accepted retirement after T062

---

## Phase 9: Complete Physical-Device Functional UI Acceptance

**Goal**: Correct integration-only PASS claims and add independently runnable primary
user journeys that operate and verify the real UI without a new host platform.

- [X] T065 [US6] Define integration, Functional UI, live-hardware, and exploratory coverage levels plus the fixture fidelity rule in `spec.md`, `plan.md`, and `coverage-inventory.md`
- [X] T066 [P] [US6] Add only the missing stable interactive/observable identifiers in `app/src/main/java/com/app/miklink/ui/testing/AgentUiTags.kt` and the existing client/profile/settings/history/PDF/result screens
- [X] T067 [P] [US6] Extend connected-device wake/locked/unlock-wait/DEVICE_LOCKED preflight contracts in `app/src/androidTest/java/com/app/miklink/e2e/support/DeviceKeyguard.kt` and focused tests
- [X] T068 [US6] Add shared dynamic semantic UI operations and targeted before/after/failure evidence support without coordinates or video in `app/src/androidTest/java/com/app/miklink/e2e/functional/FunctionalUiSupport.kt`
- [X] T069 [P] [US6] Add dashboard launch, section navigation, and return Functional UI acceptance in `app/src/androidTest/java/com/app/miklink/e2e/functional/LaunchNavigationUiTest.kt`
- [X] T081 [P] [US6] Add configured-probe open/save/reopen Functional UI acceptance with precise NOT_RUN prerequisites in `app/src/androidTest/java/com/app/miklink/e2e/functional/ProbeConfigurationUiTest.kt`
- [X] T070 [P] [US6] Add UI-driven client create/reopen/edit/persist plus representative network validation and session-owned cleanup in `app/src/androidTest/java/com/app/miklink/e2e/functional/ClientCrudUiTest.kt`
- [X] T071 [P] [US6] Add UI-driven profile create/toggle/target/save/reopen/edit plus session-owned cleanup in `app/src/androidTest/java/com/app/miklink/e2e/functional/ProfileCrudUiTest.kt`
- [X] T072 [P] [US6] Add representative UI settings persistence/restoration acceptance in `app/src/androidTest/java/com/app/miklink/e2e/functional/SettingsUiTest.kt`
- [X] T073 [P] [US6] Add UI report-settings persistence and real-result PDF export acceptance in `app/src/androidTest/java/com/app/miklink/e2e/functional/ReportSettingsUiTest.kt` and `PdfExportUiTest.kt`
- [X] T074 [P] [US6] Add UI history search/detail/action and completed-result presentation acceptance in `app/src/androidTest/java/com/app/miklink/e2e/functional/HistoryUiTest.kt`
- [X] T075 [US6] Register Functional UI scenario IDs and suite selection without relabeling integration scenarios in `app/src/androidTest/java/com/app/miklink/e2e/catalog/E2ETestCatalog.kt` and `FunctionalAcceptanceSuite.kt`
- [X] T076 [US6] Extend the existing live-probe UI flow to verify Completed, visible result sections, save, history persistence, detail, and session-owned delete through the normal UI
- [X] T077 [US6] Update `docs/reference/testing.md` with direct Gradle/ADB build-install-unlock-targeted/full-rerun and artifact retrieval workflow; explicitly prohibit coordinates/video and retire legacy runners only after parity acceptance
- [X] T078 [US6] Run local/unit/contract/lint and compile the Functional UI test APK; correct product defects rather than weakening assertions
- [X] T079 [US6] Run targeted Functional UI scenarios and then the complete applicable suite on the unlocked physical device; record separate outcomes and evidence in `coverage-inventory.md` and `verification.md`
- [X] T080 [US6] Run Spec Kit analyze after the modified spec/plan/tasks are stable and reconcile all material findings before final acceptance

---

## Phase 10: Final Functional Acceptance Corrections

- [X] T082 Add real Client delete confirmation semantics and verify deletion/absence through the rendered UI
- [X] T083 Add real Profile delete confirmation semantics and verify deletion/absence through the rendered UI
- [X] T084 Drive Probe Verify through UI and preserve precise NOT_RUN classification when hardware is unavailable
- [X] T085 Make Settings language/ID strategy independently observable and verify change, reopen, persistence, and UI restoration
- [X] T086 Record structured Functional UI actions/assertions through `ScenarioRule.recordStep`
- [X] T087 Run four targeted scenarios, the complete Functional Acceptance suite, live selection, artifact integrity, and configured-credential scans on the physical device
- [X] T088 Retire the parity-complete PowerShell and Bash host runners without adding replacement wrappers
- [X] T089 Reconcile coverage, parity, verification, workflow documentation, and residual gaps
- [X] T090 Run final Spec Kit Analyze and reconcile all material findings

---

## Dependencies and Execution Order

- Phase 1 precedes Phase 2; Phase 2 blocks all stories.
- US1 establishes the semantic surface used by US2 and US5.
- US2 can proceed without live hardware after Phase 2/US1.
- US3 depends on Phase 2 evidence/correlation; it does not block US2.
- US4 depends on the US3 live path for the probe-loss case, while its rapid/lifecycle
  tests can be authored earlier.
- US5 depends on US1 but not on live hardware.
- Final verification depends on all selected stories; host-runner retirement is
  authorized by completed parity and Phase 10 owner acceptance.

## Requirement Traceability

| Requirements | Primary tasks |
|---|---|
| FR-001–FR-004 | T007–T011, T019–T027 |
| FR-005–FR-007 | T003, T028–T038 |
| FR-008–FR-010 | T006, T009, T011, T037, T040, T045 |
| FR-011–FR-012 | T002, T007–T010, T014–T015, T053 |
| FR-013 | T017–T018, T039, T042–T046 |
| FR-014 | T005, T016, T041, T046 |
| FR-015–FR-016 | T012–T013, T051 |
| FR-017–FR-018 | T042, T050–T052 |
| FR-019–FR-020 | T011, T040, T044–T045 |
| FR-021 | T035, T048–T053 |
| FR-022–FR-023 | T013, T019, T021, T042–T045, T057, T061 |
| FR-024–FR-025 | T026–T027, T047, T058, T062, T064, T088 |
| FR-026–FR-027 | T010, T055–T057, T061 |

| FR-028-FR-030 | T065-T076 |
| FR-031 | T067-T069, T077, T079 |
| FR-032 | T068-T069, T077-T079 |
| FR-033 | T073, T077-T079 |

| Success criteria | Acceptance tasks |
|---|---|
| SC-001 | T027 |
| SC-002–SC-003 | T037–T038, T060 |
| SC-004–SC-005 | T039–T046 |
| SC-006 | T050–T053 |
| SC-007–SC-009 | T038, T060, T063, T079, T087 |
| SC-010 | T057, T061 |
| SC-011 | T047, T062, T064, T088 |
| SC-012 | T054–T056 |

| SC-013 | T065-T076, T079, T082-T087 |
| SC-014 | T067, T077, T079, T087 |
| SC-015 | T068, T077, T079, T086-T087 |

## Parallel Opportunities

- Foundation test files T004–T006, T012, and T014 can be authored in parallel.
- Semantic tags T023–T025 touch separate UI areas.
- App-only scenario classes T028–T036 are independent after fixture/tag foundations.
- Live contract tests T039–T041 and failure scenario tests T048–T050 touch separate
  files, but implementation follows their failing results.

## Implementation Strategy

1. Deliver evidence/safety foundation and US1 as the MVP.
2. Deliver probe-independent US2 for immediate repeatable value.
3. Add live correlation in US3 without deleting the known-good fallback workflow.
4. Add disruptive/lifecycle cases and UI review.
5. Prove release isolation and responsibility parity; request separate authorization
   before removing either runner.
