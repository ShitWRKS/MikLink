# Upgrade Readiness Checklist: Dependency Modernization

**Purpose**: Review whether the requirements completely and unambiguously gate stable dependency selection, production PDF parity, release safety, and GitHub automation boundaries.
**Created**: 2026-08-11
**Feature**: [spec.md](../spec.md)

**Audience / timing**: Author and PR reviewer before implementation and again before completion. Depth is formal release-gate level because the feature affects toolchain, generated code, PDF output, shrinking, and default-branch automation.

## Requirement Completeness

- [x] CHK001 Are all direct libraries, plugins, wrapper components, BOM-managed families, and workflow actions explicitly included in the audit scope? [Completeness, Spec §FR-002]
- [x] CHK002 Are selection requirements documented for latest stable, preview exclusion, and evidence-backed compatibility pins? [Completeness, Spec §FR-003–FR-004]
- [x] CHK003 Are both declared and resolved dependency states required for relocated iText artifacts? [Completeness, Spec §FR-005]
- [x] CHK004 Are build, unit, instrumentation compilation, connected-device, release, shrinker, and dependency-graph outcomes all covered with explicit terminal states? [Completeness, Spec §FR-008]
- [x] CHK005 Are requirements present for every requested cleanup class: workaround, alias, duplicate version, obsolete comment, and speculative shrinker rule? [Completeness, Spec §FR-009]

## Requirement Clarity

- [x] CHK006 Is “stable” constrained to fixed releases without alpha, beta, RC, snapshot, nightly, dynamic, or ranged versions? [Clarity, Spec §FR-003]
- [x] CHK007 Is a compatibility pin permitted only after causal reproduction and selection of the newest compatible stable release? [Clarity, Spec §FR-004]
- [x] CHK008 Is the production PDF chain named precisely enough to exclude a fake generator, alternate engine, or test-only implementation? [Clarity, Spec §FR-006]
- [x] CHK009 Are the required PDF content regions and geometry outcomes individually enumerated rather than described as merely “valid”? [Clarity, Spec §FR-007]
- [x] CHK010 Is GitHub-side activation clearly distinct from committing the updater file on a non-default branch? [Clarity, Spec §FR-012 and Assumptions]

## Requirement Consistency

- [x] CHK011 Does the latest-stable requirement remain consistent with the exception rule and forbid convenience pins without evidence? [Consistency, Spec §FR-002–FR-004]
- [x] CHK012 Are the `develop` application boundary and `master` automation-only boundary consistent across user scenarios, functional requirements, and success criteria? [Consistency, Spec §US4, FR-012, SC-007]
- [x] CHK013 Does runtime PDF verification align with the constitution’s production-path fidelity requirement while remaining isolated from release artifacts? [Consistency, Spec §FR-006–FR-008; Constitution II]
- [x] CHK014 Are dependency alerts for resolved transitives consistently distinguished from automatic manifest update proposals? [Consistency, Spec §FR-014]

## Acceptance Criteria Quality

- [x] CHK015 Can complete dependency audit coverage be measured as a percentage with a primary-source and outcome record per family? [Measurability, Spec §SC-001]
- [x] CHK016 Can upgrade completion be objectively separated into updated, already latest, compatibility pinned, and removed statuses? [Measurability, Spec §SC-002; Data Model §Dependency Record]
- [x] CHK017 Do PDF outcomes define objective file, page, geometry, and content assertions for both supported orientations? [Measurability, Spec §SC-004]
- [x] CHK018 Can build cleanup success be measured as zero avoidable duplicate sources, unused aliases, and unjustified workarounds? [Measurability, Spec §SC-005]
- [x] CHK019 Can automation coverage and branch routing be objectively reviewed without requiring an actual GitHub schedule run before merge? [Measurability, Spec §SC-006 and Assumptions]

## Scenario and Edge-Case Coverage

- [x] CHK020 Are primary scenarios specified independently for toolchain, PDF export, remaining libraries, and dependency automation? [Coverage, Spec §User Scenarios]
- [x] CHK021 Are exception requirements defined for preview releases, relocated artifacts, unavailable devices, release-only shrinker failures, and transitive-only vulnerabilities? [Coverage, Spec §Edge Cases]
- [x] CHK022 Is cleanup/recovery required for generated PDF artifacts even when structural or content assertions fail? [Coverage, Data Model §PDF Verification Artifact]
- [x] CHK023 Is the unavailable/unauthorized device case required to produce `NOT_RUN` or `FAIL`, never a false success? [Coverage, Spec §Edge Cases and FR-008]
- [x] CHK024 Are historical references and unrelated working-tree changes explicitly excluded from mechanical cleanup? [Coverage, Spec §Edge Cases and FR-013]

## Non-Functional and Safety Requirements

- [x] CHK025 Are release isolation requirements sufficient to prevent application upgrades from reaching `master` merely to activate automation? [Safety, Spec §FR-012]
- [x] CHK026 Are permission requirements scoped so dependency submission alone receives `contents: write`? [Security, Spec §FR-011]
- [x] CHK027 Are test fidelity requirements sufficient to forbid snapshot infrastructure, pixel comparison, alternate engines, and external lab dependencies for PDF validation? [Safety/Fidelity, Spec §FR-006–FR-008 and Assumptions]
- [x] CHK028 Are unrelated UI, architecture, and opportunistic refactor changes explicitly outside scope? [Scope, Spec §FR-013]

## Dependencies and Assumptions

- [x] CHK029 Is the supplied 2026-08-11 verified baseline clearly distinguished from versions independently audited through repository metadata? [Assumption, Spec §Assumptions; Research §Dependency Matrix]
- [x] CHK030 Are the connected device, Android API level, and absence of RouterOS/network/signing prerequisites documented for the focused PDF check? [Dependency, Spec §Assumptions; Quickstart §Prerequisites]
- [x] CHK031 Is default-branch ownership of Dependabot configuration documented as a deployment dependency outside local verification? [Dependency, Spec §US4 and Assumptions]

## Notes

- All requirement-quality items pass before task generation; no ambiguity or conflict needs clarification.
- Implementation evidence will be recorded in `tasks.md`, `research.md`, and the final command matrix rather than added as checklist questions.
