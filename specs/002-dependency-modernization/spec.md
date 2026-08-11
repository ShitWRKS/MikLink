# Feature Specification: Dependency Modernization

**Feature Branch**: `develop`

**Created**: 2026-08-11

**Status**: Draft

**Input**: User description: "Modernize MikLink's stable dependency baseline, migrate production PDF generation to the supported Android iText distribution, remove obsolete build workarounds, and add dependency automation without promoting application upgrades to master."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Reproducible Modern Build (Priority: P1)

As a maintainer, I can build and test MikLink with a mutually compatible, current stable toolchain so releases are reproducible and dependency-generated sources remain valid.

**Why this priority**: Every other upgrade and verification depends on a working build toolchain.

**Independent Test**: Configure, compile, and test the unchanged application with the upgraded toolchain and confirm generated persistence, injection, and serialization code is produced without unsupported compatibility overrides.

**Acceptance Scenarios**:

1. **Given** a clean checkout on `develop`, **When** the standard build and test entry points run, **Then** configuration, generated sources, compilation, and tests complete successfully.
2. **Given** the upgraded build configuration, **When** all declared plugin and tool versions are audited, **Then** each has one authoritative stable version and no unused legacy plugin declaration remains.
3. **Given** a previously required compatibility workaround, **When** the upgraded build runs without it, **Then** the workaround is removed if no failure is reproduced or retained only with reproducible evidence.

---

### User Story 2 - Reliable PDF Report Export (Priority: P1)

As a MikLink user, I can export a real report as a readable PDF on Android after the document engine upgrade, with the report's content and layout intact.

**Why this priority**: Report export is a production user journey and a major document-engine change can compile while still producing invalid output.

**Independent Test**: Generate portrait and supported landscape reports through the production report-export path, reopen the resulting files, and inspect their structure and text.

**Acceptance Scenarios**:

1. **Given** valid report data, **When** the production PDF export is invoked, **Then** a non-empty, reopenable PDF with at least one page is created.
2. **Given** the generated PDF, **When** its content is inspected, **Then** the main report content, header, footer or page number, and results table are present.
3. **Given** portrait and landscape report configurations supported by MikLink, **When** each is exported, **Then** each PDF uses the requested orientation and remains readable.
4. **Given** a release build with shrinking enabled, **When** the release path is built and analyzed, **Then** the PDF dependencies require no speculative retention rules and introduce no shrinker failure.

---

### User Story 3 - Current Stable Runtime Dependencies (Priority: P2)

As a maintainer, I can rely on an audited dependency set in which every direct library is current stable or has a documented, reproducible compatibility constraint.

**Why this priority**: Current dependencies reduce maintenance and security exposure while documented exceptions prevent accidental regressions.

**Independent Test**: Compare every direct dependency and plugin with its primary release source, then run the relevant unit, build, and device checks after each compatible upgrade group.

**Acceptance Scenarios**:

1. **Given** the repository dependency declarations, **When** the audit completes, **Then** every direct dependency and plugin has a recorded before version, latest stable version, action, source, and verification result.
2. **Given** a stable major version with relevant breaking changes, **When** it is adopted, **Then** only the application changes necessary to preserve existing behavior are included and the affected production slice is verified immediately.
3. **Given** a latest stable version that is incompatible, **When** the failure is reproduced, **Then** the newest compatible stable version is selected and the evidence is documented.

---

### User Story 4 - Safe Dependency Automation (Priority: P3)

As a maintainer, I receive scheduled dependency update proposals against `develop` and GitHub receives the resolved dependency graph, while `master` receives only the minimal automation configuration.

**Why this priority**: Automation keeps the verified baseline current without bypassing release isolation.

**Independent Test**: Validate the automation files, branch targets, permissions, update grouping, and the separation between the application-upgrade change set and the default-branch activation change set.

**Acceptance Scenarios**:

1. **Given** the dependency updater configuration on the default branch, **When** its weekly schedules run, **Then** Gradle and workflow minor or patch version updates are grouped, majors remain individual, and normal update proposals target `develop` without auto-merge.
2. **Given** a push to `master` or `develop`, **When** dependency submission runs, **Then** GitHub receives the resolved Gradle graph using write permission confined to that workflow.
3. **Given** the prepared Git boundaries, **When** maintainers review them, **Then** application upgrades target `develop` and only updater/submission configuration is eligible for the activation change on `master`.

### Edge Cases

- A primary repository advertises a preview release newer than its stable channel; previews must not be selected.
- A relocated dependency coordinate resolves through a compatibility POM; the runtime graph must contain only the intended Android distribution and no parallel legacy or generic Java distribution.
- An available Android device is offline, unauthorized, or incompatible; the device check must report `NOT_RUN` or `FAIL` accurately rather than treating absence as success.
- Release shrinking exposes behavior not seen in debug builds; retention rules are added only for a reproduced failure.
- A transitive vulnerable dependency appears in the submitted graph but cannot be directly updated by automation; the alert and version-update capabilities must not be conflated.
- Existing working-tree or historical documentation references are unrelated; they remain untouched unless they describe active configuration inaccurately.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST use the newest mutually compatible stable build toolchain versions identified by the verified baseline: Gradle 9.6.1, Android Gradle Plugin 9.3.1, Kotlin 2.4.10, and KSP 2.3.10.
- **FR-002**: Every direct dependency, build plugin, wrapper, and workflow action version MUST be audited against a primary source and recorded in a dependency matrix.
- **FR-003**: The project MUST NOT introduce alpha, beta, release-candidate, snapshot, nightly, dynamic, or ranged dependency versions.
- **FR-004**: A dependency MAY remain below latest stable only when the incompatibility is reproduced, the newest compatible stable is used, and the evidence is documented.
- **FR-005**: PDF export MUST use iText Core Android 9.7.1 through the final Android coordinate actually resolved at runtime, without the previous 7.2.6 distribution or a parallel generic Java distribution.
- **FR-006**: PDF verification MUST exercise the existing production chain from report data through the production generator and helper to a real output file.
- **FR-007**: PDF verification MUST cover file existence and size, reopening, page count, main content, header, footer or page number, results table, portrait layout, and the already-supported landscape layout.
- **FR-008**: Debug and release builds, release shrinker configuration, unit tests, instrumentation test compilation, and connected device tests MUST be run where locally applicable, with unavailable checks reported as `NOT_RUN`.
- **FR-009**: Compatibility workarounds, unused plugin aliases, duplicate version declarations, and stale technical comments MUST be removed once their necessity is disproved.
- **FR-010**: The dependency updater MUST monitor Gradle and GitHub Actions weekly, target normal version updates to `develop`, group minor and patch updates, leave major updates individual, and enable no auto-merge.
- **FR-011**: Dependency graph submission MUST cover pushes to `master` and `develop`, use official stable actions, and confine `contents: write` to the submission workflow.
- **FR-012**: Application dependency upgrades and their specification artifacts MUST remain in the `develop` change set; the separate `master` activation boundary MUST contain only dependency updater and dependency-submission configuration.
- **FR-013**: The implementation MUST NOT include opportunistic refactors, UI changes, architecture changes, or unrelated modifications.
- **FR-014**: Security alerts based on resolved transitive dependencies MUST be described separately from automatic version-update pull request capability.
- **FR-015**: The final repository MUST contain no unresolved critical or high consistency findings and no unimplemented convergence tasks.

### Key Entities

- **Dependency Record**: A direct library, plugin, wrapper, or workflow action with its previous version, selected version, latest stable version, primary source, compatibility decision, and verification evidence.
- **PDF Verification Artifact**: A generated report file and correlated assertions covering its structure, content, orientation, and production generation path.
- **Automation Boundary**: The minimal configuration shared with `master`, distinct from application and build changes targeting `develop`.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of direct dependencies, plugins, wrapper components, and workflow actions have an explicit audit result and primary-source reference.
- **SC-002**: 100% of compatible stable updates are applied; every retained older version has a reproducible compatibility record.
- **SC-003**: All required local build and test commands terminate with an accurately recorded `PASS`, `FAIL`, or `NOT_RUN`, and no upgrade-caused failure remains unresolved.
- **SC-004**: Both portrait and supported landscape exports produce non-empty, reopenable documents with at least one page and all four required content regions present.
- **SC-005**: The final active build configuration contains zero avoidable duplicate version sources, zero unused legacy plugin aliases, and zero unjustified compatibility workarounds.
- **SC-006**: The automation configuration covers both requested ecosystems, both dependency-submission branches, and routes 100% of normal update proposals to `develop` without auto-merge.
- **SC-007**: The `master` activation boundary contains zero application source, dependency, or toolchain upgrades.

## Assumptions

- The verified stable target versions supplied in the feature brief remain authoritative for the 2026-08-11 implementation unless repository evidence has changed.
- `develop` is the integration target and `master` remains the default/release branch.
- Existing report fixtures and Android instrumentation infrastructure may be extended, but no new snapshot or alternate PDF engine infrastructure is required.
- A connected, authorized ADB device may be used for test execution and debugging; external network or RouterOS prerequisites remain subject to the constitution's explicit availability checks.
- GitHub-side activation cannot be claimed until the minimal configuration is merged into the default branch.
