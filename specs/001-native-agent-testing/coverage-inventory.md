# Repository-Backed Coverage Inventory

**Baseline**: `develop` at `fa2e9d15e542850956e3db92bdadd2b41dbfe9d4`  
**Inventory date**: 2026-08-10  
**Method**: Correct the existing inventory without removing lower-level tests. A
repository/use-case round trip is integration evidence and cannot be counted as a
Functional UI acceptance result.

## Coverage levels

1. **Lower-level / integration**: unit, contract, golden, repository, use-case,
   persistence, generator, or instrumented integration validation.
2. **Functional UI acceptance**: primary user journey driven and observed through
   rendered UI on a physical device.
3. **Live hardware acceptance**: Functional UI journey through the normal product
   networking path to the explicitly configured MikroTik probe.
4. **Exploratory availability**: ad-hoc semantic discovery and operation with ADB,
   hierarchy, dynamically derived element bounds, and targeted evidence.

> **Fixtures may arrange a scenario. They cannot replace through internal APIs the
> functionality that the scenario claims to test.**

`PASS` in one column never promotes another column.

## Product Areas and Current Coverage

| ID | User-visible feature group | Lower-level / integration coverage | Functional UI acceptance | Live hardware acceptance | Exploratory availability | Remaining acceptance path |
|---|---|---|---|---|---|---|
| FG-01 | Launch, splash, setup guidance, dashboard, client/profile selection | PASS — `DashboardViewModelTest`, `DashboardScenarioTest` tag/state assertions | **NOT_RUN — maintained navigation journey not yet executed on current build** | N/A | PASS — prior ad-hoc dashboard→settings→dashboard evidence | `LaunchNavigationUiTest` on unlocked device |
| FG-02 | Probe create/update, connectivity verification, status polling, transport fallback, capability display | PASS — view-model/connectivity/transport contracts and `ProbeConfigurationScenarioTest` | **NOT_RUN — probe form create/update/verify has not been accepted through UI** | PASS — saved configured probe used by prior live sessions; form itself not covered | AVAILABLE via debug semantic hierarchy | Add/execute probe configuration UI journey without persisting secrets in evidence |
| FG-03 | Client list/add/edit/delete, DHCP/static settings, socket naming, speed server | PASS — save/network/socket tests and repository-driven `ClientScenarioTest` | **NOT_RUN — `ClientScenarioTest` bypasses the form** | PARTIAL — real execution consumed a prepared client | AVAILABLE | `ClientCrudUiTest` create/reopen/edit/delete plus representative network validation |
| FG-04 | Test-profile list/add/edit/delete, enabled steps, targets, counts, gateway policy, thresholds | PASS — view-model/use-case/policy tests and repository-driven `ProfileScenarioTest` | **NOT_RUN — `ProfileScenarioTest` bypasses the form** | PARTIAL — real execution consumed a prepared profile | AVAILABLE | `ProfileCrudUiTest` create/toggle/target/reopen/edit/delete |
| FG-05 | Test execution: link, cable/TDR, network configuration, neighbors, ping, speed; logs; repeat/save | PASS — use-case/view-model/step/parsing contracts and catalog support | PASS — prior `LiveProbeE2ETest` drove select/start/running/completed/result UI | PASS — link, TDR, network, neighbors and ping; **speed NOT_RUN: speed server absent** | AVAILABLE | Extend completed-result/history/PDF handoff and rerun on current build |
| FG-06 | History grouping, search/filter, detail, delete, duplicate, repeat | PASS — repository-driven `HistoryReportScenarioTest` and limited detail Compose tests | **NOT_RUN — history journey bypasses rendered list/detail/actions** | N/A | AVAILABLE | `HistoryUiTest` using a session report produced by normal execution |
| FG-07 | PDF generation, preferences, single/client export, orientation/columns/signatures, retrieval/share | PASS — direct generator/config `PdfScenarioTest` validates PDF framing | **NOT_RUN — export UI/dialog/settings path not exercised** | N/A | AVAILABLE | `ReportSettingsUiTest` + `PdfExportUiTest`, retrieve and validate generated file |
| FG-08 | Backup JSON export/import | PASS — `BackupManagerTest`; device contract fails closed | **NOT_RUN — `disposableLocalState=true` not authorized; SAF UI round trip not run** | N/A | AVAILABLE for non-destructive screen inspection/export | Execute only on explicitly disposable local state; never retain credential-bearing backup evidence |
| FG-09 | Settings: locale, polling, glow, ID numbering, neighbor protocols | PASS — domain tests and direct preferences `SettingsScenarioTest` | **NOT_RUN — settings controls were not changed/reopened through UI** | N/A | AVAILABLE | `SettingsUiTest` representative change/reopen/persist/restore journey |
| FG-10 | Report/test result presentation: typed sections, thresholds, status, raw execution logs | PASS — codec/mapping/use-case and data-driven `ResultPresentationScenarioTest` | PASS/PARTIAL — prior live result reached Completed and visible result; full saved-history detail still pending | PASS — prior live result correlated to 66-event/5-exchange trace | AVAILABLE | Verify visible produced sections, saved history detail, and back navigation on current build |

## Cross-Cutting Coverage

| Concern | Existing evidence | Current gap / required path |
|---|---|---|
| Secret handling | Recursive/value-aware redaction contracts and credential-canary scans | Continue scanning every retained live artifact; never retain backup payloads |
| Release isolation | Debug-only semantics, release no-op trace, static scan | Exact signed release black-box smoke remains owner-deferred |
| Crash/ANR | `FailureEvidenceScenarioTest`, process-exit filtering, bounded timeouts | Real destructive injection is not required; retain targeted logcat/failure image |
| Device identity | Session manifest binds serial/model/API/build | Current acceptance needs a connected API-30+ device |
| Device unlock | `DeviceKeyguard` detects lock | Add wake + bounded manual-unlock wait; timeout must be NOT_RUN/DEVICE_LOCKED |
| Deterministic state | Session-prefixed fixtures and ID-scoped cleanup | Functional scenarios must create/edit/delete their subject through UI |
| Evidence | Manifest/result/trace/hash/hierarchy/screenshot collector | Remove video from all design/docs; capture only critical before/after/final/failure images |
| Exploratory access | Direct ADB recipe and debug resource IDs | Always locate semantically and derive bounds dynamically; no hard-coded coordinates |

## Coverage Acceptance Rule

An operational feature group is Functional UI PASS only after its primary user
journey runs through rendered UI on the physical device and produces accepted
terminal evidence for the exact build. Integration, live hardware, and exploratory
statuses remain separately visible. Missing required prerequisites are NOT_RUN;
optional or non-applicable capabilities are SKIP. Existing lower-level tests remain
complementary and are not deleted.
