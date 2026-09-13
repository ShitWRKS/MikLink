# Clarification Record

**Session**: 2026-08-09  
**Status**: Complete — no owner decision remains unresolved

| ID | Resolution | Basis |
|---|---|---|
| CL-001 | Full local reset only with explicit disposable-state authorization; otherwise own and clean only session-created records. | Accepted safe default; Constitution I/IV |
| CL-002 | Wi-Fi disruption requires an independent per-session opt-in, a designated device, retained host control, bounded execution, and verified restoration. | Accepted safe default; Constitution I/IV |
| CL-003 | No direct RouterOS manipulation in v1; mutations occur only through normal MikLink behavior. | Accepted scope; production-path fidelity |
| CL-004 | Use only the probe explicitly configured for the session; no hard-coded address or credentials. | Existing fallback is lab-specific, not a product contract |
| CL-005 | v1 uses screenshots plus semantic/state assertions and agent review; pixel baselines are out of scope. | Accepted maintenance/scope default |
| CL-006 | Speed server is an explicit prerequisite; absence is NOT_RUN only for dependent scenarios. | External dependency isolation |
| CL-007 | Primary workflow is host-neutral Android/Gradle tooling; do not add shell wrappers solely for symmetry. | User goal and native-tooling research |
| CL-008 | Full observability runs in the test-capable build; exact release artifact receives an isolation/representative smoke check. | Existing debug trace/release no-op boundary |
| CL-009 | Agent testing mode is compiled and active only in debug; production/release has no runtime activation path, while shared abstractions may exist only as production-safe no-ops. | Explicit owner clarification; release separation |

These resolutions are normative in `spec.md`. This file is the audit trail and does
not introduce requirements beyond the specification.
