# Repository-Backed Coverage Inventory

**Baseline**: `fix/production-readiness` at
`cd629064968db3b633d0a16b6e7e4e63bf209e6d`  
**Inventory date**: 2026-08-09  
**Method**: routes, screens, view models, repositories, tests, and runners were read
from the baseline. README claims were not treated as evidence by themselves.

## Product Areas and Current Coverage

| ID | User-visible feature group | Repository evidence | Existing automated evidence | Current device/E2E gap | Planned validation path | 2026-08-09 device status | Executed evidence |
|---|---|---|---|---|---|---|---|
| FG-01 | Launch, splash, setup guidance, dashboard, client/profile selection | `MainActivity.kt`, `SplashScreen.kt`, `NavGraph.kt`, `DashboardScreen.kt` | `DashboardViewModelTest.kt` | No launch/setup/navigation regression | Probe-independent named UI scenario; exploratory path | PASS | Ad-hoc dashboardâ†’settingsâ†’dashboard; `DashboardScenarioTest` |
| FG-02 | Probe create/update, connectivity verification, status polling, transport fallback, capability display | `ProbeEditScreen.kt`, `ProbeEditViewModel.kt`, probe repositories and service provider | `ProbeEditViewModelTest.kt`; probe connectivity/status contract tests; transport guard tests | Live test bypasses probe UI and contains fallback configuration | Probe UI scenario; explicit configured-probe live prerequisite; live connectivity evidence | PASS | `ProbeConfigurationScenarioTest`; configured-probe/no-fallback contract; same-lab `legacy-7` and `native-2` sessions reached the saved probe through normal product paths |
| FG-03 | Client list/add/edit/delete, DHCP/static settings, socket naming, speed server | `ClientListScreen.kt`, `ClientEditScreen.kt`, client repository and network config repository | `SaveClientUseCaseTest.kt`; socket ID tests; `NetworkConfigRepositoryTest.kt`; route tests | No CRUD UI or persistence round trip | Probe-independent CRUD scenario; live network configuration step only through normal execution | PASS | `ClientScenarioTest` with fixture cleanup |
| FG-04 | Test-profile list/add/edit/delete, enabled steps, targets, counts, gateway policy, thresholds | profile screens/view model, `TestProfile.kt`, `TestThresholds.kt` | `TestProfileViewModelTest.kt`; `SaveTestProfileUseCaseTest.kt`; quality policy tests | No profile UI round trip or field validation | Probe-independent CRUD scenario and semantic assertions | PASS | `ProfileScenarioTest` with fixture cleanup |
| FG-05 | Test execution: link, cable/TDR, network configuration, neighbors, ping, speed; logs; repeat/save | `TestExecutionScreen.kt`, `TestViewModel.kt`, `RunTestUseCaseImpl.kt`, step implementations | Run/use-case, view-model, step, parsing/golden and contract tests; `LiveProbeE2ETest.kt` | One fixed scenario; weak prerequisite/result distinction; incomplete failure/lifecycle catalog | Maintained app-only and live named scenarios; correlated evidence; rapid-start/lifecycle/recovery cases | PASS (core live); NOT_RUN (speed only) | Same-build/same-lab replacement: link, TDR, network, neighbors and ping PASS; legacy UI workflow PASS; speed precisely NOT_RUN because no speed server is configured |
| FG-06 | History grouping, search/filter, detail, delete, duplicate, repeat | `HistoryScreen.kt`, `HistoryViewModel.kt`, `ReportDetailScreen.kt`, `ReportDetailViewModel.kt` | Two report-detail Compose tests exercise limited Ping expansion | No full history-to-detail or actions workflow | Probe-independent saved-report/history scenario; targeted exploratory review | PASS | `HistoryReportScenarioTest` |
| FG-07 | PDF generation, preferences, single/client export, orientation/columns/signatures, retrieval/share | `PdfExportDialog.kt`, `PdfSettingsScreen.kt`, `PdfGeneratorIText.kt`, document writer | No device-level PDF acceptance coverage found | Generated file is not validated end-to-end | Named PDF generation/retrieval scenario; non-empty/signature/basic-open validation | PASS | `PdfScenarioTest` generated and parsed PDF framing |
| FG-08 | Backup JSON export/import | `BackupSettingsScreen.kt`, backup use cases/repositories, `BackupManagerImpl.kt` | `BackupManagerTest.kt` | No Storage Access Framework UI or round trip on device | Probe-independent device scenario using session-owned data; secrets excluded from artifacts | NOT_RUN | Machine-readable `backup-round-trip` result: `DISPOSABLE_LOCAL_STATE_NOT_AUTHORIZED`; import was not attempted without explicit opt-in |
| FG-09 | Settings: locale, polling, glow, ID numbering, neighbor protocols | `SettingsScreen.kt`, `SettingsViewModel.kt`, preferences repository | ID numbering/domain tests; string hardcoding and Italian coverage scans | No settings persistence/UI scenario | Probe-independent settings round trip; locale semantic assertions | PASS | `SettingsScenarioTest`, original values restored |
| FG-10 | Report/test result presentation: typed sections, thresholds, status, raw execution logs | result renderers, `TestDetailsContent.kt`, `ResultCards.kt`, report codec | report codec/use-case and execution tests; limited Ping Compose tests | Link/TDR/network/neighbors/speed visual states not covered | Data-driven device scenarios per result type plus live-result correlation | PASS | `ResultPresentationScenarioTest`; real `legacy-7` UI result plus 66-event trace, 5 exchanges, manifest/hash validation and configured-credential scan |

## Cross-Cutting Coverage

| Concern | Existing evidence | Gap | Planned path |
|---|---|---|---|
| Secret handling | `LogSanitizer.kt` and tests; debug/release trace split | Key-name redaction does not prove nested/serialized value safety across all artifacts | PASS: recursive/value-aware redaction contracts and device credential-canary scans cover every live artifact type |
| Release isolation | Release `DebugTraceSinkImpl` is no-op | No release artifact smoke/static acceptance gate | Build/install/launch smoke and package/artifact inspection |
| Crash/ANR | Host wrappers grep logcat markers | Correctness depends on wrapper parsing and may miss historical exits | PASS (contract): `FailureEvidenceScenarioTest` verifies current-session crash/ANR filtering, timeout, lost-device evidence and cleanup override; destructive real crash injection is not required |
| Device identity | Wrappers select `ANDROID_SERIAL`/adb | No single artifact manifest ties serial/model/API to build | Session manifest populated before actions |
| Deterministic state | Live test creates/selects fixtures via repositories | Cleanup and unrelated-data preservation are incomplete | Session-owned IDs/names, `finally` cleanup, explicit disposable reset policy |
| Exploratory access | None beyond manual adb use | No documented selectors/evidence/result contract | PASS: direct adb recipe, stable debug resource IDs, reachable before/after review and explicitly unreachable locked-device review are schema-valid under `app/build/outputs/agent-tests/ui-review/` |

## Coverage Acceptance Rule

Implementation is complete only when every `FG-*` row has at least one executed and
accepted planned path, or an owner-approved external/manual classification with a
reason. Existing lower-level coverage remains complementary and is not deleted.
