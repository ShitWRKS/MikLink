# Features

This document describes *what the app does* and the end‑to‑end flow for each feature (UI → use cases/ports → data).

## 1) Probe configuration (single probe)

**Goal:** configure a single MikroTik probe used by all tests.

**UI**
- Screen: `ui/probe/ProbeEditScreen`
- ViewModel: `ui/probe/ProbeEditViewModel`

**Data flow**
1. User edits probe fields (`ipAddress`, `username`, `password`, `testInterface`, `isHttps`).
2. VM persists via `ProbeRepository` (port).
3. Optional connectivity check via `ProbeConnectivityRepository` (port) returning `ProbeCheckResult` (board-name + interface list or error).

**Persistence**
- Table: `probe_config` (singleton row, PK fixed to `id=1`).

**Remote**
- Uses MikroTik REST API when performing connectivity checks and when running tests.

**Notes**
- ADR-0001: “single probe”. No `probeId` exists in domain.

## 2) Clients

**Goal:** manage customers/sites (“clients”) and their network constraints & socket id settings.

**UI**
- Screens: `ui/client/ClientListScreen`, `ui/client/ClientEditScreen`
- VMs: `ClientListViewModel`, `ClientEditViewModel`

**Data flow**
- CRUD via `ClientRepository` (port).
- Each client contains:
  - network mode (`DHCP`/`STATIC`) + optional static IP settings
  - min link rate
  - socket id template: prefix/suffix/separator/padding + next id number
  - optional speed-test server settings

**Persistence**
- Table: `clients`

## 3) Test profiles

**Goal:** define which checks to run for a test execution.

**UI**
- Screens: `ui/profile/TestProfileListScreen`, `ui/profile/TestProfileEditScreen`
- VM: `TestProfileViewModel`

**Data flow**
- CRUD via `TestProfileRepository` (port).
- Profile fields:
  - run flags: TDR / Link status / LLDP neighbors / Ping / SpeedTest
  - ping targets (up to 3) + count

**Persistence**
- Table: `test_profiles`
- Default profiles are inserted on DB creation (Room callback).

## 4) Run a test (execution)

**Goal:** execute a test plan (client + profile + socket name) and produce results.

**UI**
- Screen: `ui/test/TestExecutionScreen`
- VM: `ui/test/TestViewModel`
- Entry route: `test_execution/{clientId}/{profileId}/{socketName}`

**Domain orchestration**
- Use case: `core/domain/usecase/test/RunTestUseCaseImpl`
- Emits `TestEvent` via `Flow` (progress updates + completion/failure).

**Steps**
- Interfaces (domain): `core/domain/test/step/*`
- Implementations (data): `data/teststeps/*`

**Result**
- Domain builds a `ReportData` aggregate and serializes it via `ReportResultsCodec` into `resultsJson`.
- UI persists final `TestReport` via `ReportRepository` (port).

**Persistence**
- Table: `test_reports`

## 5) History & report details

**Goal:** browse reports, group by client, view details.

**UI**
- Screen: `ui/history/HistoryScreen` (grouped lists)
- Screen: `ui/history/ReportDetailScreen` (single report details)
- VMs: `HistoryViewModel`, `ReportDetailViewModel`

**Data flow**
- Reports via `ReportRepository` (port)
- Clients via `ClientRepository` (port)
- Profiles via `TestProfileRepository` (port)
- JSON parsing via `ParseReportResultsUseCase` → `ReportData`

## 6) PDF export

**Goal:** export reports into a PDF document.

**UI**
- Triggered from:
  - Client list (export all reports for a client)
  - History / report detail (export a selection)
- PDF settings screen: `ui/settings/PdfSettingsScreen`

**Contracts**
- `PdfGenerator` (port) + `PdfExportConfig` (columns, layout, signatures).

**Implementation**
- `data/pdf/impl/PdfGeneratorIText` (iText 7).

## 7) Backup import/export

**Goal:** export/import probe + clients + profiles + reports using stable keys (no DB ids).

**UI**
- Screen: `ui/settings/SettingsScreen`
- VM: `SettingsViewModel`

**Contracts**
- `BackupRepository` (port) with JSON export/import.
- `ImportBackupUseCase` handles mapping for import.

**Format**
- See `reference/backup-format.md` and ADR-0006.

## 8) Preferences (theme + id numbering strategy)

**Goal:** persist user preferences.

**Preferences**
- Theme: `ThemeConfig` + optional `CustomPalette`
- ID numbering: `IdNumberingStrategy` (continuous vs fill-gaps)

**UI**
- `SettingsScreen` (+ theme UI controls)

**Contracts**
- `UserPreferencesRepository` (port)
- Use cases: `ObserveThemeConfigUseCase`, `SetThemeConfigUseCase`, `ObserveIdNumberingStrategyUseCase`, `SetIdNumberingStrategyUseCase`
