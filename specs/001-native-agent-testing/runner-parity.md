# Runner Replacement Parity

**Status**: Same-lab live parity accepted; recovery rehearsal pending  
**Removal authorized**: No

| Responsibility | Current PowerShell | Current Bash | Native evidence | Status |
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
| Recover after interrupted execution | Partial | Partial | fixture cleanup PASS; Wi-Fi restoration contract PASS | RECOVERY_REHEARSAL_NOT_RUN |

The current and replacement workflows ran on device `6pr8q4nncyhqrcx8` against the
same explicitly configured probe. The UI-driven `legacy-7` session passed through
the normal app workflow and produced a manifest-linked, credential-scanned trace.
The `native-2` replacement session produced five PASS outcomes (link, TDR, network,
neighbors, ping), one precise NOT_RUN for the independently missing speed server,
and cleanup PASS. The primary workflow was refactored from a MIUI-blocked
`ActivityScenario` launcher to explicit UI Automator lifecycle/semantics while
retaining the same production UI/domain/repository/network path.

The product trace retains the historical `mikrotik_raw_response` event as a
sanitized, same-correlation alias of `probe_response`; the mapping is covered by
`MikroTikTraceContractTest`, so both unchanged wrappers can continue validating
their required event list. A later wrapper retry was rejected by MIUI during test-APK
installation and ran zero tests; it is recorded as an installer failure and is not
used as parity evidence. The non-disposable native path now uses preserving adb
updates and direct instrumentation to avoid UTP cleanup erasing local app state.

Only the opted-in recovery rehearsal remains outstanding. It must not run until the
independent Wi-Fi disruption and retained-host-control permissions are both granted.

Both `tools/agent/run_live_probe_e2e.ps1` and
`tools/agent/run_live_probe_e2e.sh` remain present and unchanged. They remain required
until every row is PASS and the owner explicitly accepts removal in a separate
change.
