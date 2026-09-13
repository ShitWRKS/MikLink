# Tasks: Dependency Modernization

**Input**: Design documents from `/specs/002-dependency-modernization/`

**Prerequisites**: `plan.md`, `spec.md`, `research.md`, `data-model.md`, `quickstart.md`

**Tests**: Focused structural PDF tests, existing unit/instrumentation suites, debug/release builds, R8 analysis, and dependency graph checks are required by the feature.

**Organization**: Tasks are grouped by user story and ordered so failures remain attributable to a specific upgrade slice.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel with other marked work when it does not touch the same files or depend on an incomplete slice
- **[Story]**: User story mapping from `spec.md`

## Phase 1: Setup and Audit

**Purpose**: Establish the feature artifacts, baseline, sources, and device availability before build changes.

- [x] T001 Create and validate the feature specification in `specs/002-dependency-modernization/spec.md` and `specs/002-dependency-modernization/checklists/requirements.md`
- [x] T002 Record the clean `develop` baseline, Gradle 9.5.0, passing `check`/`:app:assembleDebug`, and SDK-local ADB device in `specs/002-dependency-modernization/research.md`
- [x] T003 [P] Audit all direct libraries, plugins, wrapper, and actions against primary sources in `specs/002-dependency-modernization/research.md`
- [x] T004 [P] Document the implementation plan, data model, review checklist, and validation guide in `specs/002-dependency-modernization/plan.md`, `data-model.md`, `checklists/upgrade-readiness.md`, and `quickstart.md`

---

## Phase 2: Foundational Consistency Gate

**Purpose**: Prove specification, plan, tasks, and constitutional requirements are mutually consistent before changing production build inputs.

- [x] T005 Run non-destructive Spec Kit analysis across `specs/002-dependency-modernization/spec.md`, `plan.md`, and `tasks.md`; resolve all CRITICAL/HIGH findings in those files
- [x] T006 Review `git status --short` and active dependency declarations across `gradle/libs.versions.toml`, `settings.gradle.kts`, `build.gradle.kts`, `app/build.gradle.kts`, `gradle.properties`, `gradle/wrapper/gradle-wrapper.properties`, and `.github/workflows/`

**Checkpoint**: Analysis is free of unresolved CRITICAL/HIGH findings and the pre-change baseline remains attributable.

---

## Phase 3: User Story 1 — Reproducible Modern Build (Priority: P1) 🎯 MVP

**Goal**: Upgrade the coherent toolchain, preserve KSP-generated Room/Hilt/Moshi code, and eliminate unsupported duplicate/legacy configuration.

**Independent Test**: Gradle 9.6.1 configures and `check` plus `:app:assembleDebug` pass with AGP 9.3.1, Kotlin 2.4.10, KSP 2.3.10, and Hilt 2.60.1 without the metadata force.

- [x] T007 [US1] Update AGP, Kotlin, KSP, and Hilt versions and remove the unused Kotlin Android alias in `gradle/libs.versions.toml`
- [x] T008 [US1] Remove the duplicate Room plugin-management version from `settings.gradle.kts`
- [x] T009 [US1] Remove the Kotlin metadata resolution force and its stale explanation from `app/build.gradle.kts`
- [x] T010 [US1] Generate the full Gradle 9.6.1 wrapper update in `gradle/wrapper/gradle-wrapper.properties`, `gradle/wrapper/gradle-wrapper.jar`, `gradlew`, and `gradlew.bat`
- [x] T011 [US1] Run `--version`, `check`, and `:app:assembleDebug`; reproduce and minimally document any toolchain incompatibility in `specs/002-dependency-modernization/research.md`

**Checkpoint**: Modern toolchain slice passes independently and no unused Kotlin plugin alias, duplicate Room version, or unjustified metadata force remains.

---

## Phase 4: User Story 2 — Reliable PDF Report Export (Priority: P1)

**Goal**: Use iText Core Android 9.7.1 and prove real portrait/landscape report generation, reopening, structure, content, and release shrinking.

**Independent Test**: The focused instrumentation scenario creates and reopens both orientations through the injected production generator and asserts page count, geometry, header/title, results table data, footer/timestamp, and page number; dependency insight shows Android 9.7.1 only.

- [x] T012 [US2] Extend structural portrait/landscape production-path assertions in `app/src/androidTest/java/com/app/miklink/e2e/catalog/PdfScenarioTest.kt`
- [x] T013 [US2] Replace the generic iText 7 declaration with `com.itextpdf.android:itext-core-android:9.7.1` in `gradle/libs.versions.toml`
- [x] T014 [US2] Migrate the iText 9 PDF event-handler imports/base class/callback in `app/src/main/java/com/app/miklink/data/pdf/PdfDocumentHelper.kt`
- [x] T015 [US2] Update iText version-specific comments/imports/event registration only as required in `app/src/main/java/com/app/miklink/data/pdf/impl/PdfGeneratorIText.kt`
- [x] T016 [US2] Remove broad legacy iText shrinker rules from `app/proguard-rules.pro`, restoring only a minimal evidence-backed rule if release verification fails
- [x] T017 [US2] Run `dependencyInsight` and `dependencies` for `debugRuntimeClasspath`; record resolved Android artifact/module evidence in `specs/002-dependency-modernization/research.md`
- [x] T018 [US2] Compile instrumentation tests and run focused `PdfScenarioTest` plus `PdfExportUiTest` on the connected ADB device; record structural and E2E outcomes in `specs/002-dependency-modernization/research.md`
- [x] T019 [US2] Run `:app:assembleRelease` and `:app:analyzeReleaseR8Config`; document any unavailable task or reproduced shrinker requirement in `specs/002-dependency-modernization/research.md`

**Checkpoint**: iText Android 9.7.1 is the sole runtime distribution and both device/runtime plus release/shrinker checks pass.

---

## Phase 5: User Story 3 — Current Stable Runtime Dependencies (Priority: P2)

**Goal**: Apply every remaining compatible stable update and prove no user-facing or generated-code regression.

**Independent Test**: The catalog matches the audited stable matrix, OkHttp 5 compiles with existing networking code, and the full unit/check/debug build suite passes.

- [x] T020 [US3] Update MockK 1.14.11, Compose BOM 2026.06.01, OkHttp 5.4.0, and Robolectric 4.16.1 in `gradle/libs.versions.toml`
- [x] T021 [US3] Run networking, serialization, and repository unit tests after the OkHttp 5 change through `app/src/test/java/com/app/miklink/data/remote/` and `app/src/test/java/com/app/miklink/data/repository/`
- [x] T022 [US3] Run `:app:testDebugUnitTest`, `check`, and `:app:assembleDebug`; record all dependency audit verification statuses in `specs/002-dependency-modernization/research.md`

**Checkpoint**: Every direct dependency family is latest stable or has reproduced compatibility evidence; no pin is introduced merely to bypass a generic failure.

---

## Phase 6: User Story 4 — Safe Dependency Automation (Priority: P3)

**Goal**: Configure weekly Gradle/Actions proposals to `develop` and submit the resolved graph, while preserving the minimal `master` activation boundary.

**Independent Test**: YAML parsing and manual review show the two ecosystems, requested schedules/grouping/limits, `develop` targets, no auto-merge, push coverage for `master`/`develop`, and workflow-scoped write permission.

- [x] T023 [P] [US4] Add the requested Gradle and GitHub Actions updater schedules and groups in `.github/dependabot.yml`
- [x] T024 [P] [US4] Add the dedicated official Gradle dependency-submission workflow in `.github/workflows/dependency-submission.yml`
- [x] T025 [US4] Validate both YAML files and document normal-versus-security update behavior plus the inactive-until-`master` condition in `specs/002-dependency-modernization/research.md`
- [x] T026 [US4] Document `.github/dependabot.yml` and `.github/workflows/dependency-submission.yml` as the exclusive `master` activation set in `specs/002-dependency-modernization/data-model.md` and final handoff without switching or merging branches

**Checkpoint**: Local configuration is valid; GitHub-side activation is explicitly still pending on the default branch.

---

## Phase 7: Polish and Cross-Cutting Verification

**Purpose**: Run the complete Definition of Done, remove stale active references, update evidence, and converge the implementation.

- [x] T027 Search active repository configuration/source for old iText coordinates/version, Kotlin/KSP/Wrapper versions, duplicate versions, and removed-workaround comments; update only stale current references
- [x] T028 Run final `--version`, `clean check`, `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease`, `:app:analyzeReleaseR8Config`, `:app:assembleDebugAndroidTest`, and full `:app:connectedDebugAndroidTest`; record PASS/FAIL/NOT_RUN in `specs/002-dependency-modernization/research.md`
- [x] T029 Run `git diff --check`, inspect `git diff`/`git status`, and verify no unrelated changes or secret-bearing evidence in all changed files
- [x] T030 Run Spec Kit convergence against code, `spec.md`, `plan.md`, and `tasks.md`; append/implement any missing tasks and repeat until `specs/002-dependency-modernization/tasks.md` is converged

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup/Audit (Phase 1)**: Complete.
- **Foundational gate (Phase 2)**: Depends on Phase 1 and blocks implementation.
- **US1 Toolchain (Phase 3)**: Depends on Phase 2; establishes the build used by later slices.
- **US2 PDF (Phase 4)**: Depends on US1 for coherent toolchain; independently verifies the production export.
- **US3 Libraries (Phase 5)**: Depends on US1 and follows US2 to keep iText failures attributable.
- **US4 Automation (Phase 6)**: Depends only on Phase 2 and may be authored in parallel with US1–US3 because it touches separate files.
- **Final verification (Phase 7)**: Depends on all desired user stories.

### User Story Dependencies

- **US1 (P1)**: No story dependency after foundational analysis.
- **US2 (P1)**: Requires US1's working upgraded toolchain but retains an independent PDF acceptance test.
- **US3 (P2)**: Requires US1; scheduled after US2 to isolate iText from other library changes.
- **US4 (P3)**: File-level independent after analysis; GitHub activation remains externally dependent on a later minimal `master` change.

### Parallel Opportunities

- T003 and T004 were performed in parallel at the artifact level.
- T023 and T024 touch separate YAML files and can be completed together after analysis.
- Device availability checks and non-mutating source review can run alongside dependency metadata research.
- Gradle build/test tasks are intentionally serialized when they share generated output to keep diagnostics deterministic.

---

## Parallel Example: User Story 4

```text
Task T023: Create `.github/dependabot.yml` with both ecosystems and `develop` targets.
Task T024: Create `.github/workflows/dependency-submission.yml` with official stable actions.
```

---

## Implementation Strategy

### MVP First — User Story 1

1. Pass non-destructive analysis.
2. Upgrade the coherent toolchain and remove the metadata workaround.
3. Run `check` and debug assembly.
4. Stop and diagnose any failure before iText or remaining libraries change.

### Incremental Delivery

1. Baseline + artifacts → attributable starting point.
2. US1 → modern reproducible toolchain.
3. US2 → Android iText 9.7.1 with vertical PDF evidence.
4. US3 → all remaining stable libraries.
5. US4 → automation files and explicit default-branch boundary.
6. Final full suite + convergence → handoff.

## Notes

- Do not create commits or switch to `master` unless separately requested; report the intended boundaries instead.
- Existing user changes, tests, and E2E runners remain intact.
- Every task follows checkbox + ID + optional parallel marker + required story label + exact file path format.
