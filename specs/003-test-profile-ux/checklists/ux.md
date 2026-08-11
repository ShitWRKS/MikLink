# UX Requirements Checklist: Test Profile Tabbed UX

**Purpose**: Review the completeness, clarity, consistency, and measurability of the profile-tab and threshold-preview requirements before implementation
**Created**: 2026-08-11
**Feature**: [spec.md](../spec.md)

**Review depth**: Standard release-gate review
**Audience/timing**: Author and PR reviewer before implementation

## Requirement Completeness

- [x] CHK001 Are the exact tab count, order, labels, and exclusion of a Thresholds tab/summary explicitly specified? [Completeness, Spec §FR-002]
- [x] CHK002 Are ownership and visibility requirements defined for every existing profile field across the four tabs? [Completeness, Spec §FR-005, §FR-007, §FR-012, §FR-019]
- [x] CHK003 Are the persistent Save action, global no-test validation message, Back behavior, and post-save navigation all covered? [Completeness, Spec §FR-001, §FR-006]
- [x] CHK004 Are requirements present for both create and edit flows plus pre-existing profile compatibility? [Completeness, Spec §FR-001, §FR-025]
- [x] CHK005 Are paired localization and stable locale-independent semantic requirements stated? [Completeness, Spec §FR-026–FR-028]

## Requirement Clarity

- [x] CHK006 Is tab-state lifetime distinguished clearly from persisted profile state? [Clarity, Spec §FR-003]
- [x] CHK007 Is the effect of changing tabs defined for save, defaults, toggles, and unsaved input? [Clarity, Spec §FR-004]
- [x] CHK008 Is the Link visualization explicitly defined as a discrete threshold explanation rather than a measurement or time series? [Clarity, Spec §FR-010, §FR-023]
- [x] CHK009 Is valid custom Link behavior defined for parsing, range overflow, clamping, display, and persistence? [Clarity, Spec §FR-009–FR-011]
- [x] CHK010 Are illustrative Ping series, average/max reference lines, and the absence of minimum RTT unambiguous? [Clarity, Spec §FR-015–FR-016]
- [x] CHK011 Is the distinction between Mbps throughput and ping/jitter/loss metrics explicit? [Clarity, Spec §FR-021–FR-022]

## Requirement Consistency

- [x] CHK012 Are always-visible disabled-test settings consistent for both Ping and Speed test? [Consistency, Spec §FR-013, §FR-020]
- [x] CHK013 Do preview-default requirements align with the unchanged blank-threshold save semantics? [Consistency, Spec §FR-018, §FR-024]
- [x] CHK014 Are the Link, Ping, and Speed threshold fields consistent with the unchanged persisted entities listed in Key Entities? [Consistency, Spec §FR-008, §FR-015, §FR-019, Key Entities]
- [x] CHK015 Is obsolete `PING_CONFIG` removal conditioned on consumer migration while other stable semantic identifiers remain preserved? [Consistency, Spec §FR-026]

## Acceptance Criteria Quality

- [x] CHK016 Can exact four-tab/no-thresholds behavior be objectively verified without subjective visual judgment? [Measurability, Spec §SC-001]
- [x] CHK017 Does the acceptance journey identify create, cross-tab unsaved state, save/reopen, edit/reopen, and real UI deletion outcomes? [Measurability, Spec §FR-027, §SC-002]
- [x] CHK018 Can unchanged valid-input/default/round-trip behavior be compared against the existing source of truth? [Measurability, Spec §SC-003]
- [x] CHK019 Are preview responsiveness, illustrative labeling, and numeric-field visibility objectively observable? [Measurability, Spec §SC-004]
- [x] CHK020 Are build, device, and consistency terminal outcomes defined without treating unavailable execution as success? [Measurability, Spec §SC-005–SC-006]

## Scenario and Edge-Case Coverage

- [x] CHK021 Are blank and invalid threshold behaviors specified separately, including non-mutation and partial-preview degradation? [Coverage, Spec §FR-018, Edge Cases]
- [x] CHK022 Are optional Ping target show/remove/recreate and conditional gateway-policy scenarios covered? [Coverage, Spec §FR-012, §FR-014, Edge Cases]
- [x] CHK023 Is configuration recreation covered in addition to ordinary recomposition and tab switching? [Coverage, Spec §FR-003–FR-004, Edge Cases]
- [x] CHK024 Is narrow-screen reachability and per-tab scrolling addressed without requiring swipe navigation? [Coverage, Edge Cases, Assumptions]
- [x] CHK025 Are unavailable/unauthorized device outcomes and non-destructive device handling stated as acceptance assumptions? [Coverage, Spec §SC-005, Assumptions]

## Scope and Dependency Boundaries

- [x] CHK026 Are prohibited domain, persistence, report, execution, theme, dependency, and parallel-layout changes explicit? [Scope, Spec §FR-024–FR-025, §FR-029]
- [x] CHK027 Are TDR internal fail statuses and LLDP's lack of thresholds protected from new UI configuration? [Scope, Spec §FR-007, Key Entities]
- [x] CHK028 Are preview data persistence and presentation as real measurements explicitly prohibited? [Scope, Spec §FR-023]
- [x] CHK029 Is the directly stale testing documentation correction bounded without a general documentation refactor? [Scope, Spec §FR-030]
- [x] CHK030 Are the existing ViewModel/domain/validation/persistence dependencies identified as authoritative assumptions? [Dependency, Spec §FR-024, Assumptions]

## Notes

- All 30 items pass against the current specification. The supplied feature brief resolves scope, interaction, validation, preview, and compatibility decisions, so no clarification questions were needed.
