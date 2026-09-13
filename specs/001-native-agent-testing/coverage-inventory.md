# Repository-Backed Coverage Inventory

**Acceptance revision**: `ae15a069b25995e8cb2c8848cb477375d2712a87`
**Inventory date**: 2026-08-11
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
| FG-01 | Launch, splash, setup guidance, dashboard, client/profile selection | PASS — `DashboardViewModelTest`, `DashboardScenarioTest` tag/state assertions | PASS — `LaunchNavigationUiTest` in `acceptance-ae15a06-suite-v2` | N/A | PASS — semantic navigation and screenshots available | None for current scope |
| FG-02 | Probe create/update, connectivity verification, status polling, transport fallback, capability display | PASS — view-model/connectivity/transport contracts and `ProbeConfigurationScenarioTest` | NOT_RUN — real form and Verify action executed; configured probe was unreachable, so success/save/reopen assertions correctly did not run | NOT_RUN — probe hardware unavailable on 2026-08-11; prior same-lab configured-probe path remains PASS | AVAILABLE via debug semantic hierarchy | Rerun `ProbeConfigurationUiTest` with the configured probe reachable |
| FG-03 | Client list/add/edit/delete, DHCP/static settings, socket naming, speed server | PASS — save/network/socket tests and repository-driven `ClientScenarioTest` | PASS — real UI create, static validation, reopen, edit, delete confirmation, and absence in `acceptance-4faa1eb-client` and final suite | PARTIAL — prior real execution consumed a prepared client | AVAILABLE | None for current app-only scope |
| FG-04 | Test-profile list/add/edit/delete, enabled steps, targets, counts, gateway policy, thresholds | PASS — view-model/use-case/policy tests and repository-driven `ProfileScenarioTest` | PASS — real UI create, toggle/targets, reopen, edit, delete confirmation, and absence in `acceptance-4faa1eb-profile-v2` and final suite | PARTIAL — prior real execution consumed a prepared profile | AVAILABLE | None for current app-only scope |
| FG-05 | Test execution: link, cable/TDR, network configuration, neighbors, ping, speed; logs; repeat/save | PASS — use-case/view-model/step/parsing contracts and catalog support | PASS — prior `LiveProbeE2ETest` drove select/start/running/completed/result UI | PASS — prior same-lab link, TDR, network, neighbors and ping; speed remained independently NOT_RUN without a server | AVAILABLE | Current rerun requires probe hardware |
| FG-06 | History grouping, search/filter, detail, delete, duplicate, repeat | PASS — repository-driven `HistoryReportScenarioTest` and detail Compose tests | PASS — session report search, detail, deletion and absence through UI in `acceptance-ae15a06-suite-v2` | N/A | AVAILABLE | Duplicate/repeat remain lower-level coverage, outside the corrected primary journey |
| FG-07 | PDF generation, preferences, single/client export, orientation/columns/signatures, retrieval/share | PASS — direct generator/config `PdfScenarioTest` validates PDF framing | PASS — settings persistence/restore, visible export, external-viewer return, retrieval, non-trivial size, `%PDF-` header and `%%EOF` in final suite | N/A | AVAILABLE | None for current export acceptance |
| FG-08 | Backup JSON export/import | PASS — `BackupManagerTest`; device contract fails closed | **NOT_RUN — `disposableLocalState=true` not authorized; SAF UI round trip not run** | N/A | AVAILABLE for non-destructive screen inspection/export | Execute only on explicitly disposable local state; never retain credential-bearing backup evidence |
| FG-09 | Settings: locale, polling, glow, ID numbering, neighbor protocols | PASS — domain tests and direct preferences `SettingsScenarioTest` | PASS — language and ID strategy changed independently, checked state observed, persisted after reopen, and restored through UI in `acceptance-4faa1eb-settings` and final suite | N/A | AVAILABLE | None for representative settings scope |
| FG-10 | Report/test result presentation: typed sections, thresholds, status, raw execution logs | PASS — codec/mapping/use-case and data-driven `ResultPresentationScenarioTest` | PASS — saved result was found and opened through rendered History/detail UI in final suite; produced live sections remain covered by prior accepted live run | PASS — prior live result correlated to 66-event/5-exchange trace | AVAILABLE | Current live rerun requires probe hardware |

## Cross-Cutting Coverage

| Concern | Existing evidence | Current gap / required path |
|---|---|---|
| Secret handling | Recursive/value-aware redaction contracts and credential-canary scans | Continue scanning every retained live artifact; never retain backup payloads |
| Release isolation | Debug-only semantics, release no-op trace, static scan | Exact signed release black-box smoke remains owner-deferred |
| Crash/ANR | `FailureEvidenceScenarioTest`, process-exit filtering, bounded timeouts | Real destructive injection is not required; retain targeted logcat/failure image |
| Device identity | Session manifest binds serial/model/API/build | PASS on serial `6pr8q4nncyhqrcx8`, model `22101316G`, API 34 |
| Device unlock | `DeviceKeyguard` wakes, detects lock, and bounds manual-unlock wait | PASS on the unlocked acceptance device; locked timeout remains contract-covered |
| Deterministic state | Session-prefixed fixtures and ID-scoped cleanup | PASS for real UI client/profile/report creation and deletion |
| Evidence | Manifest/result/trace/hash/hierarchy/screenshot collector | Remove video from all design/docs; capture only critical before/after/final/failure images |
| Exploratory access | Direct ADB recipe and debug resource IDs | Always locate semantically and derive bounds dynamically; no hard-coded coordinates |

## Coverage Acceptance Rule

An operational feature group is Functional UI PASS only after its primary user
journey runs through rendered UI on the physical device and produces accepted
terminal evidence for the exact build. Integration, live hardware, and exploratory
statuses remain separately visible. Missing required prerequisites are NOT_RUN;
optional or non-applicable capabilities are SKIP. Existing lower-level tests remain
complementary and are not deleted.
