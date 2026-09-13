# Tasks: Test Profile Tabbed UX

**Input**: Design documents from `/specs/003-test-profile-ux/`

**Prerequisites**: `plan.md`, `spec.md`, `data-model.md`, `quickstart.md`

**Tests**: The focused functional CRUD journey, pure preview-helper tests where scaling is non-trivial, existing ViewModel/domain tests, quality scans, lint, debug assembly, and instrumentation compilation are required.

**Organization**: Tasks are grouped by user story and ordered to preserve semantic parity before the obsolete collapsible control is removed.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel when it touches different files and has no incomplete dependency
- **[Story]**: User story mapping from `spec.md`

## Phase 1: Setup and Specification

**Purpose**: Establish the clean baseline and complete feature artifacts before source changes.

- [x] T001 Record branch, HEAD, clean working tree, and feature namespace in `specs/003-test-profile-ux/verification.md`
- [x] T002 Create and validate `specs/003-test-profile-ux/spec.md` and `specs/003-test-profile-ux/checklists/requirements.md`
- [x] T003 [P] Create the constitution-checked design in `specs/003-test-profile-ux/plan.md`, `data-model.md`, `quickstart.md`, and `verification.md`

---

## Phase 2: Foundational Consistency and Semantics

**Purpose**: Make requirements reviewable, establish stable semantic names, and block implementation on artifact consistency.

- [x] T004 Generate and complete the UX requirements-quality checklist in `specs/003-test-profile-ux/checklists/ux.md`
- [x] T005 Run non-destructive Spec Kit analysis across `specs/003-test-profile-ux/spec.md`, `plan.md`, and `tasks.md`; resolve every CRITICAL/HIGH contradiction or coverage gap before implementation
- [x] T006 Add four stable tab IDs and focused Link/Ping/Speed threshold input IDs in `app/src/main/java/com/app/miklink/ui/testing/AgentUiTags.kt` while retaining `PING_CONFIG` until its final consumer migrates
- [x] T007 [P] Add paired English and Italian resources for tabs, sections, preview labels, pass semantics, and compact metric indicators in `app/src/main/res/values/strings.xml` and `app/src/main/res/values-it/strings.xml`

**Checkpoint**: Requirements checklist is complete; analysis has zero unresolved CRITICAL/HIGH findings; new semantic/resource contracts are ready.

---

## Phase 3: User Story 1 — Configure a profile by test area (Priority: P1) 🎯 MVP

**Goal**: Deliver exactly four saveable-state tabs, independent scrolling, global validation messaging, and preserved create/edit/save/delete behavior.

**Independent Test**: Create, navigate tabs without losing unsaved values, save, reopen, edit/save/reopen, and delete a profile through the production UI.

### Tests for User Story 1

- [x] T008 [US1] Rewrite the profile functional CRUD journey to use tab IDs and assert unsaved state, round-trip, second edit/reopen, and real UI deletion in `app/src/androidTest/java/com/app/miklink/e2e/functional/ProfileCrudUiTest.kt`

### Implementation for User Story 1

- [x] T009 [P] [US1] Implement the General tab name/description form in `app/src/main/java/com/app/miklink/ui/profile/TestProfileGeneralTab.kt`
- [x] T010 [US1] Refactor the scaffold, saveable tab selection, four-tab row, per-tab scroll orchestration, persistent Save CTA, and global no-test validation message in `app/src/main/java/com/app/miklink/ui/profile/TestProfileEditScreen.kt`
- [x] T011 [US1] Preserve lifecycle-collected ViewModel state and optional Ping target visibility across tab/configuration changes without adding a duplicated state holder in `app/src/main/java/com/app/miklink/ui/profile/TestProfileEditScreen.kt`

**Checkpoint**: Exactly four tabs exist; General contains only name/description; tab changes retain unsaved ViewModel values; save/back behavior remains intact.

---

## Phase 4: User Story 2 — Configure Link capabilities and threshold (Priority: P1)

**Goal**: Group Link Status, TDR, and LLDP with the existing minimum-rate editor and an explanatory discrete visualization.

**Independent Test**: Toggle all three capabilities, select a preset, enter a valid out-of-preset custom rate, and observe an unclamped pass-threshold explanation.

### Tests for User Story 2

- [x] T012 [P] [US2] Add pure helper tests for Link preset/custom normalization and out-of-scale behavior in `app/src/test/java/com/app/miklink/ui/profile/TestProfileThresholdPreviewTest.kt`

### Implementation for User Story 2

- [x] T013 [US2] Implement Link Status, TDR, LLDP, existing presets/custom entry, stable threshold semantics, and no new TDR/LLDP settings in `app/src/main/java/com/app/miklink/ui/profile/TestProfileLinkTab.kt`
- [x] T014 [US2] Implement the discrete Link threshold scale and explicit negotiated-rate pass rule without clamping saved values in `app/src/main/java/com/app/miklink/ui/profile/TestProfileThresholdPreviews.kt`

**Checkpoint**: Link configuration is local to Link, custom rates remain governed only by `StrictLinkRateParser`, and the preview is not a time series.

---

## Phase 5: User Story 3 — Configure Ping execution and quality thresholds (Priority: P1)

**Goal**: Put all existing Ping execution/configuration/threshold fields in one visible tab and add local/external RTT previews.

**Independent Test**: Enable Ping, use quick fill, add/remove targets, edit count and thresholds, trigger conditional gateway policy, and retain values through tab changes.

### Tests for User Story 3

- [x] T015 [P] [US3] Extend pure preview-helper tests for blank effective defaults, independently invalid RTT references, and deterministic series scaling in `app/src/test/java/com/app/miklink/ui/profile/TestProfileThresholdPreviewTest.kt`

### Implementation for User Story 3

- [x] T016 [US3] Expose UI-only effective default helpers backed by the existing private defaults in `app/src/main/java/com/app/miklink/ui/profile/TestProfileViewModel.kt` without changing save validation or domain mapping
- [x] T017 [US3] Implement the always-visible Ping toggle, quick fill, three targets/removal, count, conditional gateway policy, and local/external threshold sections in `app/src/main/java/com/app/miklink/ui/profile/TestProfilePingTab.kt`
- [x] T018 [US3] Implement deterministic local/external RTT threshold previews with separate average/max references and invalid-part omission in `app/src/main/java/com/app/miklink/ui/profile/TestProfileThresholdPreviews.kt`

**Checkpoint**: Ping has no nested configuration card, no minimum RTT, packet loss remains separate, and disabling Ping erases nothing.

---

## Phase 6: User Story 4 — Configure Speed test thresholds (Priority: P2)

**Goal**: Keep exactly five Speed thresholds in their test tab and visually compare throughput minima without mixing units.

**Independent Test**: Edit all five thresholds, disable/re-enable Speed test, and observe download/upload comparison plus separate ping/jitter/loss indicators.

### Tests for User Story 4

- [x] T019 [P] [US4] Extend pure preview-helper tests for effective throughput values and derived non-limiting display range in `app/src/test/java/com/app/miklink/ui/profile/TestProfileThresholdPreviewTest.kt`

### Implementation for User Story 4

- [x] T020 [US4] Implement the Speed toggle and exact five existing threshold inputs with retained disabled-state configuration in `app/src/main/java/com/app/miklink/ui/profile/TestProfileSpeedTab.kt`
- [x] T021 [US4] Implement derived-range download/upload bars and separate compact ping/jitter/loss indicators in `app/src/main/java/com/app/miklink/ui/profile/TestProfileThresholdPreviews.kt`

**Checkpoint**: Mbps values share only their derived throughput comparison; latency, jitter, and loss stay on separate controls/indicators.

---

## Phase 7: Migration Parity, Documentation, and Verification

**Purpose**: Remove obsolete UI/semantics only after parity, correct directly stale documentation, and execute every required gate.

- [x] T022 Remove the obsolete collapsible Ping/threshold code and `PING_CONFIG` semantic ID after confirming no consumer remains in `app/src/main/java/com/app/miklink/ui/profile/TestProfileEditScreen.kt` and `app/src/main/java/com/app/miklink/ui/testing/AgentUiTags.kt`
- [x] T023 [P] Update the real Client/Profile UI-delete description and only directly related profile-layout text in `docs/reference/testing.md` and, if demonstrably stale, `docs/reference/ui-architecture.md`
- [x] T024 Run `StringsItalianCoverageTest`, `HardcodedStringsScanTest`, `TestProfileViewModelTest`, `TestQualityPolicyTest`, preview-helper tests, and full `:app:testDebugUnitTest`; record observed outcomes in `specs/003-test-profile-ux/verification.md`
- [x] T025 Run `:app:lint`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest`; record observed outcomes in `specs/003-test-profile-ux/verification.md`
- [x] T026 Perform explicit device preflight; if one authorized device is available, run `ProfileCrudUiTest` and then `FunctionalAcceptanceSuite` only after focused PASS, preserving data and recording PASS/FAIL/NOT_RUN/SKIP in `specs/003-test-profile-ux/verification.md`
- [x] T027 Review release isolation, secret-safe/failure evidence, session-scoped UI cleanup, semantic migration parity, unchanged persistence/domain/policy/dependencies, and final `git diff`/`git status` in `specs/003-test-profile-ux/verification.md`
- [x] T028 Scan changed source for introduced TODO/FIXME and run `git diff --check`; record exact results in `specs/003-test-profile-ux/verification.md`
- [x] T029 Run Spec Kit convergence after implementation, append and implement any remaining findings, and repeat until `specs/003-test-profile-ux/tasks.md` has no actionable CRITICAL/HIGH or unfinished task

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: Complete.
- **Foundational gate (Phase 2)**: Depends on Phase 1 and blocks source implementation.
- **US1 (Phase 3)**: Depends on stable resources/semantics from Phase 2 and establishes the tab scaffold.
- **US2, US3, US4 (Phases 4–6)**: Depend on the US1 scaffold. Their separate tab files can be authored independently, while shared preview-file changes remain sequential.
- **Migration/verification (Phase 7)**: Depends on all desired user stories and removes obsolete semantics only after consumer parity.

### User Story Dependencies

- **US1 (P1)**: No story dependency after Phase 2.
- **US2 (P1)**: Uses the US1 Link tab slot; otherwise independent.
- **US3 (P1)**: Uses the US1 Ping tab slot and supplies the CRUD journey's main round-trip fields.
- **US4 (P2)**: Uses the US1 Speed tab slot; otherwise independent.

### Parallel Opportunities

- T006 and T007 touch separate semantic/resource files after the artifact gate.
- T009 can be implemented separately from the scaffold orchestration in T010.
- T012/T015/T019 evolve one test file and therefore execute sequentially even though their story logic is independent.
- Tab files T013, T017, and T020 are file-independent after shared contracts exist; preview tasks T014, T018, and T021 are serialized in their shared file.
- Documentation review T023 can run after the UI shape is final while local tests begin, provided verification evidence is recorded after both complete.

---

## Parallel Example: Test-Specific Tabs

```text
Task T013: Build the Link tab in `TestProfileLinkTab.kt`.
Task T017: Build the Ping tab in `TestProfilePingTab.kt`.
Task T020: Build the Speed tab in `TestProfileSpeedTab.kt`.
```

---

## Implementation Strategy

### MVP First — User Story 1

1. Complete the requirement-quality and consistency gates.
2. Establish stable semantics/resources.
3. Build the four-tab scaffold and General tab.
4. Compile before adding test-specific previews.

### Incremental Delivery

1. Artifacts and semantics → stable contracts.
2. US1 → navigable state-preserving form shell.
3. US2 → Link grouping and discrete preview.
4. US3 → complete Ping tab and RTT previews.
5. US4 → complete Speed tab and throughput preview.
6. Semantic cleanup, documentation, gates, device acceptance, and convergence.

## Notes

- Do not switch branches, commit, push, or touch `master` unless separately requested.
- Do not edit domain models, quality policy, persistence entity/schema/mapper, backup/report formats, dependencies, or global theme files.
- Every task follows checkbox + ID + optional parallel marker + required story label + exact file path format.

## Phase 8: Convergence

- [x] T030 Add stable, bidirectional Link preset, Ping RTT, and Speed throughput sliders that update the existing ViewModel-owned numeric values and their previews in the same interaction; retain unrestricted manual entry, invalid/blank handling, domain validation, and add focused helper/semantic verification per SC-004 (partial)

## Phase 9: Convergence

- [x] T031 Replace the adaptive Speed throughput slider range with a stable logarithmic 0–100G mapping, enforce the latest explicit 100,000 Mbps Download/Upload limit for manual input and saving, and cover boundaries/round trips with pure and ViewModel tests per SC-004 and the 2026-08-11 user clarification (partial)
- [x] T032 Review paired EN/IT profile headings for consistent title capitalization and add a project-wide convention-based quality test that discovers analogous resource defects without enumerating screens or example keys per FR-028 (partial)
- [x] T033 Run focused quality, validation, preview, ViewModel, full unit/lint/build, and authorized-device profile acceptance gates; update verification with exact terminal outcomes per SC-005 (partial)
