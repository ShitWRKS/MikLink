<!--
Sync Impact Report
- Version change: template -> 1.0.0
- Established principles:
  - I. Production Safety and Release Isolation
  - II. Production-Path Fidelity
  - III. Secret-Safe, Correlated Evidence
  - IV. Deterministic Native Validation
  - V. Preservation Until Verified Parity
- Added sections: Operational Constraints; Specification and Review Gates
- Templates reviewed: plan-template.md, spec-template.md, tasks-template.md
- Follow-up TODOs: none
-->

# MikLink Constitution

## Core Principles

### I. Production Safety and Release Isolation
Test automation MUST NOT introduce a remotely reachable control surface, weaken
production authentication or transport policy, or enable destructive behavior in a
release build. Test-only controls, diagnostics, fixtures, and dependencies MUST be
excluded or inert in release artifacts. Any action that can reset local state,
interrupt connectivity, or alter external equipment MUST be explicitly scoped,
opted into for the current session, reversible where possible, and fail closed when
authorization or prerequisites are absent.

### II. Production-Path Fidelity
End-to-end validation MUST exercise the same UI, domain use cases, repositories,
networking stack, persistence, export paths, and state transitions used by the
product. Test code MAY arrange deterministic fixtures and observe outcomes, but it
MUST NOT replace the behavior under test or duplicate the RouterOS/application
protocol behind a parallel test implementation. Architecture boundaries already in
the repository MUST remain the source of truth.

### III. Secret-Safe, Correlated Evidence
Every automated session MUST produce enough structured, correlated evidence to
reconstruct the tested scenario and distinguish setup, execution, assertion, and
cleanup. Evidence MUST identify the build, device, scenario, operation, and terminal
outcome. Credentials, authorization material, cookies, private keys, and sensitive
payload values MUST be redacted before persistence or logging; cleanup-time
redaction alone is insufficient. Release logging MUST remain no-op or limited to
existing production-safe behavior.

### IV. Deterministic Native Validation
Automation MUST use supported Android/Gradle test and device capabilities and MUST
remain host-neutral. Local test data MUST be isolated or limited to records created
by the current session. External hardware and network prerequisites MUST be declared
and checked before assertions that depend on them. PASS, FAIL, NOT_RUN, and SKIP
MUST have unambiguous meanings so an unavailable lab cannot be reported as product
success or product failure.

### V. Preservation Until Verified Parity
Existing useful tests, trace facilities, and runners MUST remain available until the
replacement proves equivalent or better coverage and evidence on the supported
workflow. Migration MUST map every retained responsibility to a native replacement,
identify intentional removals, and verify parity before deletion. Unrelated
working-tree changes MUST be preserved.

## Operational Constraints

- The minimum supported Android level, dependency-injection approach, persistence
  model, networking behavior, and release security posture are constraints, not
  conveniences to bypass in tests.
- Physical-device scenarios MUST declare ownership and recovery expectations for
  the device, Wi-Fi connection, probe, and speed-test endpoint.
- RouterOS configuration MUST NOT be changed directly by a testing backchannel.
  Mutations under test occur only through normal MikLink behavior.
- Destructive local reset and connectivity disruption require separate, explicit
  per-session opt-ins. Their absence produces a non-running outcome, not an implicit
  authorization.
- Host-specific orchestration MUST NOT be required for correctness. Thin convenience
  entry points MAY exist only when the underlying workflow is directly runnable by
  standard Gradle and Android tooling.

## Specification and Review Gates

- A feature specification MUST define independently testable journeys, explicit
  external prerequisites, terminal-state semantics, and measurable outcomes.
- Planning MUST begin with repository evidence and an inventory of current product
  areas, tests, gaps, trace points, and runner responsibilities.
- Research decisions MUST document alternatives and reject custom protocols or
  duplicated production logic unless a demonstrated gap makes them necessary.
- The plan MUST pass the constitutional safety, fidelity, evidence, determinism, and
  preservation gates before task generation.
- Tasks MUST include verification of release isolation, secret redaction, cleanup,
  failure artifacts, and migration parity. Any unresolved critical or high-severity
  consistency finding blocks implementation.

## Governance

This constitution governs all MikLink specifications and plans. Amendments require
a documented rationale, a migration impact statement, and a semantic version
change. A principle removal or incompatible redefinition is MAJOR; a new principle
or materially expanded obligation is MINOR; wording-only clarification is PATCH.
Every review MUST record constitution-gate compliance or an explicit, time-bounded
exception approved by the repository owner. The specification and plan are
subordinate to this constitution.

**Version**: 1.0.0 | **Ratified**: 2026-08-09 | **Last Amended**: 2026-08-09
