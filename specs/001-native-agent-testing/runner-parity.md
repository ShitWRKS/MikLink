# Runner Replacement Parity

**Status**: Native parity accepted; legacy host runners retired
**Removal authorized**: Yes — explicit owner acceptance on 2026-08-11

| Responsibility | Retired PowerShell | Retired Bash | Native evidence | Status |
|---|---:|---:|---|---|
| Discover device and state | Yes | Yes | adb discovery and typed prerequisite tests on serial `6pr8q4nncyhqrcx8` | PASS |
| Require explicit/unique target | Yes | Yes | selected serial recorded in schema-valid manifests | PASS |
| Invoke targeted/full instrumentation | Yes | Yes | class filters plus 47-test complete applicable catalog | PASS |
| Arrange deterministic client/profile data | Yes | Yes | session-owned fixture manager and cleanup device tests | PASS |
| Require explicitly configured probe | Yes (delegated to test) | Yes (delegated to test) | no-fallback preflight plus real configured-probe sessions `legacy-7` and `native-2` | PASS |
| Capture native test result | Yes | Yes | prior 19-result catalog plus real same-lab legacy/native manifests | PASS |
| Capture screenshot/UI hierarchy | Partial | Partial | direct adb and artifact collector; reachable/unreachable review | PASS |
| Capture and validate structured trace | Yes | Yes | `legacy-7`: 66 schema-valid events, 5 correlated exchanges, manifest-linked NDJSON and credential scan | PASS |
| Detect crash/ANR/timeout | Logcat/timeout | Logcat/timeout | process-exit filtering, bounded failure and artifact contracts | PASS_CONTRACT |
| Distinguish PASS/FAIL/NOT_RUN/SKIP | Partial | Partial | four-way state machine and catalog tests | PASS |
| Preserve secret safety | Partial | Partial | recursive redaction, persisted-failure canary, and scan against configured probe credentials | PASS |
| Collect files on host | Yes | Yes | catalog/UI sessions plus pulled `legacy-7` and `native-2` manifests with digest/size validation | PASS |
| Recover after interrupted execution | Partial | Partial | fixture cleanup PASS; Wi-Fi restoration contract PASS; destructive rehearsal explicitly not required for retirement | PASS_CONTRACT |

The current and replacement workflows ran on device `6pr8q4nncyhqrcx8` against the
same explicitly configured probe. The UI-driven `legacy-7` session passed through
the normal app workflow and produced a manifest-linked, credential-scanned trace.
The `native-2` replacement session produced five PASS outcomes (link, TDR, network,
neighbors, ping), one precise NOT_RUN for the independently missing speed server,
and cleanup PASS. The primary workflow was refactored from a MIUI-blocked
`ActivityScenario` launcher to explicit UI Automator lifecycle/semantics while
retaining the same production UI/domain/repository/network path.

The product trace retains the historical `mikrotik_raw_response` event as a
sanitized, same-correlation alias of `probe_response`; the mapping remains covered by
`MikroTikTraceContractTest`. A transitional host-runner retry was rejected by MIUI
during test-APK installation and ran zero tests; it is recorded as an installer
failure and is not used as parity evidence. The non-disposable native path uses
preserving adb updates and direct instrumentation to avoid UTP cleanup erasing local
app state.

The destructive Wi-Fi recovery rehearsal remains optional and still requires its
independent disruption and retained-host-control permissions. Explicit owner direction
states that this chaos exercise is not a retirement blocker; the fail-closed and
restoration contracts cover the runner responsibility without broadening device risk.

The former PowerShell and Bash runners were removed on 2026-08-11 after owner
acceptance. The final native physical run on commit `ae15a06` completed the
Functional Acceptance suite with seven PASS results and one precise probe-hardware
NOT_RUN; the previously accepted same-lab live trace remains the live parity evidence.
No replacement shell wrapper was introduced.
