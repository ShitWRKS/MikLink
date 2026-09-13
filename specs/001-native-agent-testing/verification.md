# Verification — Native Agent Testing

## Final functional acceptance correction — 2026-08-11

**Implementation revisions**: `4faa1eb5830405785dc09eb3cf5a7c55268f7ffe`,
`ae15a069b25995e8cb2c8848cb477375d2712a87`
**Device**: `6pr8q4nncyhqrcx8`, model `22101316G`, API 34
**Device policy**: preserving installs (`adb install -r -t`), no data reset, no
Wi-Fi disruption. Screen timeout and USB stay-awake were temporarily enabled for
the runs and restored afterward (`screen_off_timeout=60000`, stay-awake disabled).

| Verification | Result |
|---|---|
| `ClientCrudUiTest` — `acceptance-4faa1eb-client` | PASS / `ASSERTIONS_PASSED`; real create, invalid-static validation, reopen/edit, delete confirmation and list absence; 4/4 artifacts valid |
| `ProfileCrudUiTest` — `acceptance-4faa1eb-profile-v2` | PASS / `ASSERTIONS_PASSED`; real create/toggle/targets, reopen/edit, delete confirmation and list absence; 4/4 artifacts valid |
| `SettingsUiTest` — `acceptance-4faa1eb-settings` | PASS / `ASSERTIONS_PASSED`; language and ID strategy changed independently, checked, reopened, persisted and restored through UI; 4/4 artifacts valid |
| `ProbeConfigurationUiTest` — `acceptance-4faa1eb-probe` | NOT_RUN / `PROBE_UNREACHABLE_TIMEOUT`; form and real Verify action reached without hard-coded credentials; success/save/reopen correctly not claimed; 2/2 artifacts valid |
| `FunctionalAcceptanceSuite` — `acceptance-ae15a06-suite-v2` | PASS — AndroidJUnitRunner `OK (8 tests)`; 7 PASS plus Probe-only `PROBE_UNREACHABLE` NOT_RUN; exact source revision `ae15a069b25995e8cb2c8848cb477375d2712a87`; 31/31 artifacts valid |
| `LiveProbeE2ETest` — `acceptance-ae15a06-live-v2` | NOT_RUN / `missing_live_probe_or_configuration`; AndroidJUnitRunner `OK (1 test)`; 1/1 artifact valid; no live operation or new trace started |
| Local build and quality gates | PASS — `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, `:app:testDebugUnitTest`, `:app:lint` |
| Accepted-artifact integrity | PASS — all 46 files listed by the six accepted manifests exist and match recorded size and SHA-256 |
| Accepted-artifact configured-credential scan | PASS — focused `ArtifactSecretScanTest` read configured values internally without logging them and found no occurrence in the six accepted sessions |
| Live trace continuity | NOT_RUN for a new trace because no probe hardware was available; prior accepted same-lab evidence remains 66 schema-valid events and 5 correlated exchanges |
| Host-runner retirement | PASS — responsibility parity and owner acceptance recorded; PowerShell and Bash runners removed with no replacement wrapper |

Two product defects were corrected during acceptance: Client Save could remain
enabled for invalid static network input, and Settings PDF-title persistence could
lose/reorder rapid writes. Semantic targeting, dialog discovery, instrumentation
lifecycle, scrolling, structured step evidence, and PDF synchronization corrections
are acceptance-harness/UI observability fixes rather than additional product claims.

Final Spec Kit Analyze is PASS: 33 functional requirements and 15 success criteria
are present and task-traced, all 90 task IDs are unique, both checklists are complete
(44/44 total), and no unresolved critical, high, or medium consistency finding
remains. The owner-deferred signed-release smoke and external hardware prerequisites
remain explicit acceptance gaps rather than specification contradictions.

## Functional UI completion increment — 2026-08-10

**Baseline**: `develop` at `fa2e9d15e542850956e3db92bdadd2b41dbfe9d4`  
**Branch**: `001-native-agent-testing-functional-acceptance`  
**Device preflight**: `NOT_RUN / DEVICE_NOT_CONNECTED` — the configured Android SDK
ADB returned an empty device list, so no new physical-device PASS is claimed.

| Verification | Result |
|---|---|
| Functional UI source + debug/androidTest compilation | PASS |
| `:app:testDebugUnitTest` | PASS |
| `:app:lint` | PASS |
| `:app:assembleDebug :app:assembleDebugAndroidTest` | PASS |
| Functional UI physical suite | NOT_RUN — compatible unlocked device not connected |
| Live probe extension | NOT_RUN — device and configured reachable probe unavailable |

The new classes keep integration coverage separate and compile independently for
launch/navigation, client/profile UI CRUD, settings/report-settings persistence,
history/detail/delete, and PDF export/retrieval. `LiveProbeE2ETest` now also verifies
produced section cards, saves through UI, reopens the session report through History,
and deletes the session report through the visible action. Device execution remains
required before any new Functional UI row can become PASS.

## Previous accepted baseline — 2026-08-09

**Date**: 2026-08-09  
**Baseline revision**: `cd629064968db3b633d0a16b6e7e4e63bf209e6d`  
**Device**: `6pr8q4nncyhqrcx8`, model `22101316G`, API 34  
**Policy used**: `disposableLocalState=false`, `allowWifiDisruption=false`,
`hostControlRetained=false`

## Automated quality gate

| Command/evidence | Result |
|---|---|
| `:app:testDebugUnitTest :app:lint` | PASS — all unit, quality, golden and contract tests; lint report generated |
| `ReleaseIsolationScanTest` | PASS — debug-only entry point/semantics, release no-op trace, no runtime activation surface |
| `:app:assembleDebug`, `:app:assembleDebugAndroidTest` | PASS |
| `:app:assembleRelease` | PASS — minified unsigned release assembled |
| `ScenarioRuleRedactionTest` / support package | PASS — persisted failure canary removed; copied artifacts indexed; support regressions pass on device |
| `MikroTikTraceContractTest` | PASS — correlated request/response/error contract and transitional `mikrotik_raw_response` alias retained for evidence compatibility |
| Evidence boundary contracts | PASS — 6 focused evidence/trace tests and 23/23 support-package tests; invalid terminal claims, unknown variants/events, unfinalized sessions, unsafe paths, destructive release policy and release NDJSON are rejected before persistence |
| Accepted manifest revalidation | PASS — all 6 retained final session manifests satisfy the strengthened identity/finalization/path/digest invariants |
| Accepted scenario-result revalidation | PASS — all 55 retained terminal results satisfy strengthened identity, path, prerequisite, step, cleanup and outcome invariants |
| Accepted trace revalidation | PASS — 2 retained NDJSON files / 132 events satisfy JSON, required-field, correlation, version and event-enum checks |

## Physical-device acceptance

| Scope | Result |
|---|---|
| Debug semantic launch/navigation | PASS — `run-as` and debug resource IDs verified; dashboard → settings → dashboard artifacts captured |
| App-only maintained catalog | 8 PASS, backup NOT_RUN because `disposableLocalState=true` was not granted |
| Native complete applicable catalog | PASS — 47 support/catalog tests, zero failures; maintained scenario session additionally produced 19 machine-readable outcomes |
| Real same-lab live parity | PASS — `legacy-7` UI workflow PASS; `native-2` produced 5 PASS and speed-only NOT_RUN with cleanup PASS |
| Live trace acceptance | PASS — 66 schema-valid events, 5 correlated exchanges, manifest-linked trace, digest/size checks and configured-credential scan |
| Rapid/repeated start ownership | PASS |
| Lifecycle | PASS on targeted unlocked rerun; the earlier complete catalog remains correctly recorded as NOT_RUN for its locked-device state |
| Connectivity recovery | PASS fail-closed path; real Wi-Fi disruption NOT_RUN because both disruption opt-ins were false |
| Failure/ANR/timeout/lost-device evidence | PASS contract |
| UI review contract | PASS — 3 tests |

An additional unchanged-wrapper invocation was rejected by the MIUI installer before
test execution (`INSTALL_FAILED_USER_RESTRICTED`, zero tests). Gradle/UTP cleanup then
removed the debug and test packages, which erased the local probe configuration even
though the session policy was non-disposable. This is classified as a host/device
installation failure, not a product or probe result. The primary documented workflow
now uses preserving `adb install -r -t` updates followed by direct `am instrument`;
`connectedDebugAndroidTest` is restricted to explicitly disposable device state.
Both APKs were restored by preserving direct installs, and a direct instrumentation
preflight correctly returned `NOT_RUN: PROBE_NOT_CONFIGURED` until the probe is saved
again.

The maintained session `native-catalog-20260809-v3` contains 19 scenario results:
9 PASS and 10 NOT_RUN (locked UI, backup opt-in, Wi-Fi opt-in, legacy live and six
probe cases). Its aggregate manifest reports cleanup PASS. AJV 2020 validated all
19 results and the manifest, and every listed artifact was re-hashed and size-checked.
Reachable and locked/unreachable UI reviews remain under
`app/build/outputs/agent-tests/ui-review/`.

Same-lab acceptance used the explicitly configured probe on device
`6pr8q4nncyhqrcx8`. The current UI workflow was migrated from the MIUI-blocked
`ActivityScenario` launcher to explicit UI Automator lifecycle and stable semantic
selectors; it still drives the normal MikLink UI/domain/repository/network path.
Session `live-parity-20260809-legacy-7` passed in 25 seconds and indexed its scanned
`probe-trace.ndjson` in both result and manifest. The 66 events share session
`live-parity-20260809-legacy-7` and scenario `legacy-live-probe`; five exchanges
cover LINK, TDR, NEIGHBORS and two PING requests. Session
`live-parity-20260809-native-2` then produced five PASS outcomes and one precise
`SPEED_SERVER_NOT_CONFIGURED` NOT_RUN, with cleanup PASS and validated hashes.

During hardening, one pre-body `ActivityScenario` timeout and two deliberately
terminated launcher attempts were retained as failed diagnostic sessions rather
than being misreported as product results. The replacement UI Automator run is the
accepted evidence.

## Release isolation evidence

`app-release-unsigned.apk` was inspected after R8/resource shrinking:

- merged manifest contains only the launcher activity and normal application/library
  components; no agent, E2E, instrumentation or test-control component is present;
- mapping/usage inspection contains no `DebugE2EEntryPoint`, Wi-Fi controller, live
  scenario, or `MIKLINK_E2E_TRACE`; the shared release trace abstraction remains a
  no-op as required by dependency wiring;
- `apksigner verify` reports `DOES NOT VERIFY`; the APK filename is explicitly
  `app-release-unsigned.apk`;
- ADB installation was rejected with `INSTALL_PARSE_FAILED_NO_CERTIFICATES`.

For additional runtime evidence, an unchanged copy of that release payload was
zip-aligned and signed with the local Android **debug certificate only**. This copy
is not a production artifact and cannot satisfy T061, but its black-box smoke passed:

- package flags omitted `DEBUGGABLE` and `run-as com.app.miklink` was rejected;
- the separate test package was removed and `pm list instrumentation` returned no
  runner targeting MikLink;
- explicit `agentMode`, `e2eMode`, `testControl` and instrumentation extras did not
  expose control behavior;
- `com.app.miklink.AGENT_MODE` resolved to no Activity;
- normal `MainActivity` launch completed and the app process remained alive;
- targeted logcat contained no `MIKLINK_E2E`, trace, crash or ANR marker, and no
  `debug_trace_*.ndjson` existed in external app files.

The debug application and androidTest package were restored afterward. `run-as`
succeeded, the support regressions ran, and real-probe acceptance completed on the
restored debug build.

Exact production-signature verification and visible black-box navigation required
by FR-026/T061 remain **DEFERRED by owner direction**. The debug-signed smoke above
is recorded only as supporting evidence, never as a substitute.

## Requirement reconciliation

| Requirements | Final result and evidence |
|---|---|
| FR-001–FR-007 | PASS — debug sessions, direct adb, catalog selection and feature inventory |
| FR-008–FR-012 | PASS — prerequisite/outcome/session/evidence contracts and bounded tests |
| FR-013–FR-014 | PASS — real correlated probe trace, configured-credential scan, recursive redaction and persisted-failure canary |
| FR-015–FR-016 | PASS — session-owned fixtures and idempotent cleanup; backup destructive path excluded |
| FR-017 | PASS fail-closed and restoration contracts; opted-in physical disruption NOT_RUN |
| FR-018–FR-020 | PASS — app-path-only policy, no fallback, independent speed prerequisite |
| FR-021 | PARTIAL — rapid start, targeted lifecycle, PDF/report and live probe PASS; opted-in probe-loss disruption NOT_RUN |
| FR-022 | PASS static/build inspection; external exact signed-release smoke remains part of FR-026 |
| FR-023 | PASS — real UI and independently selectable live cases exercised production paths on the configured probe |
| FR-024–FR-025 | PASS — standard Gradle/ADB/AndroidJUnitRunner workflow; parity accepted and transitional host runners retired without replacement |
| FR-026 | DEFERRED — owner requested no further signed-release work |
| FR-027 | PASS — screenshots paired with semantic state and before/after correlation |
| FR-028 | PASS — only rendered physical-device UI runs are reported as Functional UI PASS |
| FR-029 | PASS — each required app-only journey is independently runnable; the current Probe journey terminates precisely as hardware-dependent NOT_RUN |
| FR-030 | PASS — fixtures arrange unrelated prerequisites while Client/Profile/Settings/History/PDF claimed actions occur through UI |
| FR-031 | PASS — wake/lock/manual-unlock bounds and DEVICE_LOCKED mapping are maintained; final device was unlocked |
| FR-032 | PASS — targeted screenshots, hierarchy, structured steps, logcat and generated-file evidence; no video |
| FR-033 | PASS — visible PDF export produced a retrieved non-trivial file with valid header and EOF |
| SC-001–SC-003 | PASS |
| SC-004 | PASS — five real correlated RouterOS exchanges through applicable decision/UI stages |
| SC-005 | PASS — configured credentials absent from accepted trace and canary scans |
| SC-006 | NOT_RUN — no independent Wi-Fi disruption authority |
| SC-007 | PASS for applicable native catalog after redeployment |
| SC-008 | PARTIAL — all feature groups accounted and corrected UI journeys accepted; FG-08 still lacks destructive import opt-in and FG-02 awaits reachable hardware |
| SC-009 | PASS — probe-independent catalog does not depend on a probe |
| SC-010 | PARTIAL — static/unsigned artifact inspection PASS; exact signed black-box smoke owner-deferred |
| SC-011 | PASS — every retired-runner responsibility has accepted native evidence or owner-accepted fail-closed/restoration contract evidence |
| SC-012 | PASS — reachable/unreachable UI facts are evidence-linked |
| SC-013 | PASS for every currently applicable operational group; FG-02 and FG-08 remain explicit prerequisite-dependent NOT_RUN rather than false PASS |
| SC-014 | PASS — bounded wake/lock/unlock behavior is contract-covered and the accepted physical suite ran unlocked |
| SC-015 | PASS — accepted Functional UI sessions contain no video and every manifest-listed artifact passed size/digest validation |

## Outstanding acceptance inputs

1. A device session explicitly authorizing `disposableLocalState=true` for the backup
   import round trip.
2. A configured speed-test server if the optional live-speed case must move from its
   precise NOT_RUN classification to PASS.
3. Separate `allowWifiDisruption=true` and `hostControlRetained=true` authorization
   for the recovery rehearsal.
4. T061 remains intentionally open but owner-deferred; do not request or pursue a
   release signing environment unless that direction changes.
5. A reachable configured probe is required to convert the current Probe Verify and
   Live NOT_RUN outcomes into a new success/save/reopen run and a new correlated trace.
