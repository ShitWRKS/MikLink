# Miklink — Consolidated Technical Decisions, Target Architecture, and Refactor Plan (from LLM chat)

> **Scope of this document**  
> This is a **single, consolidated Markdown source** that captures the **decisions, constraints, target structure, naming, documentation methodology, feature map, and refactor plan** discussed throughout the provided LLM chat.  
> It is meant to be used as an **implementation guide** for applying the changes to the codebase and for keeping documentation aligned with code (**docs-as-code**).  
>
> **Important note on provenance**  
> Some parts of the chat contain claims like “I already updated X” or “ZIP ready”. In this environment, we **cannot verify** those artifacts. This document treats those as **declared actions** and focuses on the **decisions and required outcomes** that you asked to implement.

---

## Table of contents
- [1. Context and goals](#1-context-and-goals)
- [2. Documentation methodology and process](#2-documentation-methodology-and-process)
- [3. Non‑negotiable rules and constraints](#3-nonnegotiable-rules-and-constraints)
- [4. Confirmed functional decisions](#4-confirmed-functional-decisions)
- [5. Target architecture (SOLID / Clean / Ports & Adapters)](#5-target-architecture-solid--clean--ports--adapters)
- [6. Target folder/package structure](#6-target-folderpackage-structure)
- [7. Naming conventions](#7-naming-conventions)
- [8. Feature map (how the app works today)](#8-feature-map-how-the-app-works-today)
- [9. Refactor work items](#9-refactor-work-items)
- [10. Database reset policy (pre‑prod)](#10-database-reset-policy-preprod)
- [11. UI architecture guidelines (resilience to change)](#11-ui-architecture-guidelines-resilience-to-change)
- [12. Dependency Injection (DI) modularization](#12-dependency-injection-di-modularization)
- [13. Testing strategy and required updates](#13-testing-strategy-and-required-updates)
- [14. Documentation deliverables and ADR set](#14-documentation-deliverables-and-adr-set)
- [15. Acceptance criteria / Definition of Done](#15-acceptance-criteria--definition-of-done)
- [16. Suggested migration order (PR plan)](#16-suggested-migration-order-pr-plan)
- [17. Risks and guardrails](#17-risks-and-guardrails)
- [18. Appendix: “Declared completed actions” from the chat](#18-appendix-declared-completed-actions-from-the-chat)

---

## 1. Context and goals
- The project is an **Android app** (“Miklink”) originally developed “100% in vibe coding” style.
- Goal: **refactor deeply** to remove legacy, align code/tests/db/docs, and adopt a **SOLID + Clean Architecture / Ports & Adapters** approach.
- Emphasis:
  - **No technical debt** (practically: eliminate known debt sources: TODO/FIXME, dead code, hardcoded UI strings, inverted dependencies, naming collisions, etc.).
  - **No duplicate / confusing layers** (remove stubs, placeholders, redundant parsers, “Impl” ambiguity).
  - **Docs are correct** and **verified against code** (no guesses, no “assumed” behavior).
  - **Pre‑production** policy explicitly allows destructive resets for speed.

---

## 2. Documentation methodology and process

### 2.1 Methodology (organization)
Adopt a combination of:
- **Diátaxis** for documentation information architecture:
  - **Tutorials** (learning-oriented)
  - **How‑to guides** (task-oriented)
  - **Reference** (technical specification, source-of-truth for APIs/DB/build)
  - **Explanation** (architecture rationale, invariants, “why”)
- **ADRs (Architecture Decision Records)** to capture and freeze decisions (package structure, “no DTO leaks”, DB destructive policy, etc.).
- **Docs‑as‑code**: documentation is versioned, reviewed, and updated with code changes.

### 2.2 Process requirements (verification)
- Documentation must be **checked against code**:
  - Dependency versions from `gradle/libs.versions.toml`
  - DB schema from Room exported schema JSON: `app/schemas/.../1.json`
  - Retrofit services and endpoints from actual source paths
- If discrepancies exist, the workflow is:
  1) Identify mismatch  
  2) Prefer **code as source of truth**  
  3) Update docs (and optionally code if docs reflect desired target)  
  4) If intent is unclear, **ask** (avoid “deciding” without input)

### 2.3 Documentation pruning rules
- Remove “diary/epic/process” docs that are **dated, fragmented, or not useful now or later**.
- Keep fewer files with clearer structure; avoid duplication.
- Maintain a canonical **project structure + naming reference** and ADRs to prevent drift.

---

## 3. Non‑negotiable rules and constraints
These were repeatedly requested and should be treated as hard requirements:

### 3.1 Architectural constraints
- **Unidirectional dependency rule**: `UI → Domain → Data` and never the reverse.
- `core/domain/**` is **pure Kotlin**: must not import Android/Room/Retrofit/iText.
- `core/*` (domain + ports) must not depend on `data/**` implementation details.
- **No DTOs in ports** (no DTO leak across module boundaries).

### 3.2 Code hygiene constraints
- **Remove legacy**: no commented-out “deprecated” code, no placeholder features, no unused utilities.
- **Remove TODO/FIXME** (and replace with explicit behavior or proper implementation).
- **No monolithic buckets** like `Utils.kt` and `Models.kt` that accumulate unrelated types.
- **Avoid generic `*Impl`** names where a more descriptive implementation name exists (e.g., `RoomReportRepository`, `MikroTikTestRepository`).
- Normalize encoding: **UTF‑8 without BOM**.
- Add file headers and meaningful English comments in modified/created files:
  ```
  /*
   * Purpose: ...
   * Inputs: ...
   * Outputs: ...
   * Notes: ...
   */
  ```

### 3.3 UI strings constraint
- There is a quality gate: `HardcodedStringsScanTest` (described in chat) that fails when UI uses hardcoded string literals like `Text("...")` or `contentDescription="..."`.
- Requirement: **move all UI strings into `strings.xml`**, but keep the *text exactly as it currently is* (mixed EN/IT), per your explicit instruction.

### 3.4 Pre‑production DB policy
- Until production release: use Room **destructive migration** (data wipe on version mismatch), to allow rapid iteration:
  - `fallbackToDestructiveMigration(dropAllTables = true)`
- You explicitly requested: “**all data destroyed on every update until production**.”

---

## 4. Confirmed functional decisions

### 4.1 Backup format: `clientKey` is nullable in `BackupReport`
- Decision: **OK NULLABLE** for `BackupReport.clientKey` (`String?`).
- Implication: import/export must support “orphan” reports or incomplete historical data without forcing a workaround.

### 4.2 Socket‑ID increment policy
- Decision: `nextIdNumber` increments **only when**:
  - Report is saved as part of **Run Test flow**
  - `overallStatus == "PASS"`
- Decision: **Duplication does NOT increment**.
  - Duplicate/import/restore must use “raw save” paths that do not apply the increment policy.

### 4.3 Root package
- Decision: keep root package **`com.app.miklink`** (minimize churn). Reorganize **internally**.

### 4.4 Strings localization policy (explicit)
- Decision: **Do not translate**, do not normalize language.
- Move existing mixed-language UI text “as-is” into `strings.xml` and reference via `stringResource()`.

---

## 5. Target architecture (SOLID / Clean / Ports & Adapters)

### 5.1 Conceptual layers
- **UI (Presentation)**: Compose + ViewModels; depends on use cases; emits UI state/events.
- **Domain (Application + Domain)**:
  - Entities/value objects
  - Policies (business rules)
  - Use cases (single responsibility orchestration)
  - Test runner domain (plans/steps/events)
- **Ports (Contracts)**:
  - Repository interfaces
  - IO contracts
  - Codec contracts
  - PDF generation contract
- **Data (Adapters/Implementations)**:
  - Room persistence
  - Retrofit/Moshi DTO parsing
  - OkHttp infra (trust policies)
  - iText PDF generation
  - Android document IO

### 5.2 Key SOLID rules (re-stated)
- **DIP**: domain and ports do not depend on frameworks or DTOs.
- **SRP**: repositories handle persistence; domain policies/use cases handle business rules.
- **ISP**: ports should be small and focused (separate probe connectivity, test execution, backup, preferences, etc.).

---

## 6. Target folder/package structure

> This is the **target tree** described/derived in the chat, refined into a single canonical reference.  
> It keeps the macro-layer names consistent with the docs (“core/domain, core/data, data, ui, di”) while improving internal clarity and maintainability.

### 6.1 Repository root (high-level)
```
/
├─ app/
│  ├─ schemas/                                   # Room exported schema (source of truth)
│  └─ src/
│     ├─ main/
│     │  ├─ AndroidManifest.xml
│     │  ├─ res/                                 # resources (strings, drawables, xml rules, etc.)
│     │  └─ java/com/app/miklink/
│     │     ├─ MainActivity.kt                   # Compose entry point
│     │     ├─ MikLinkApplication.kt             # Hilt application
│     │     ├─ core/
│     │     ├─ data/
│     │     ├─ ui/
│     │     ├─ di/
│     │     └─ platform/                         # optional: Android-specific non-UI helpers
│     ├─ test/
│     └─ androidTest/
├─ docs/                                         # Diátaxis + ADR
├─ gradle/
└─ build.gradle.kts / settings.gradle.kts / gradle.properties / libs.versions.toml
```

### 6.2 `core/domain/**` (pure Kotlin domain)
```
core/domain/
├─ model/                                        # entities + value objects
│  ├─ Client.kt
│  ├─ ProbeConfig.kt
│  ├─ TestProfile.kt
│  ├─ TestReport.kt
│  └─ report/                                    # report result domain structures
├─ policy/                                       # business rules (no persistence)
│  ├─ formatting/                                # MikroTikFormatting / LinkFormatting (category + formatting)
│  ├─ socketid/                                  # Socket-ID Lite rules, increment policies
│  └─ validation/                                # NetworkValidator, input validation
├─ test/                                         # test-runner domain
│  ├─ model/                                     # plan, step result, errors/outcomes
│  ├─ step/                                      # step interfaces (PingStep, CableTestStep, etc.)
│  └─ runner/                                    # optional orchestrator
└─ usecase/                                      # application layer use cases (single responsibility)
   ├─ backup/
   ├─ preferences/
   ├─ report/                                    # SaveTestReportUseCase lives here
   └─ test/
```

### 6.3 `core/data/**` (ports/contracts; **no DTOs**)
```
core/data/
├─ repository/
│  ├─ backup/                                    # BackupRepository (port)
│  ├─ client/
│  ├─ preferences/
│  ├─ probe/
│  │  └─ model/                                  # probe repository boundary models (no DTO)
│  ├─ report/
│  ├─ test/
│  └─ testprofile/
├─ io/                                           # DocumentReader/Writer contracts
├─ pdf/                                          # PdfGenerator contract + export config
└─ codec/                                        # ReportResultsCodec (encode/decode contracts)
```

### 6.4 `data/**` (implementations/adapters)
```
data/
├─ local/
│  ├─ room/
│  │  ├─ db/                                     # MikLinkDatabase
│  │  ├─ dao/
│  │  ├─ entity/
│  │  ├─ mapper/                                 # toDomain/toEntity
│  │  └─ transaction/                            # transaction runner helpers if needed
│  └─ datastore/                                 # DataStore prefs impls
├─ remote/
│  └─ mikrotik/
│     ├─ api/                                    # Retrofit interface(s)
│     ├─ dto/                                    # Moshi DTOs (stay here)
│     ├─ mapper/                                 # dto -> domain (NO leaks outward)
│     ├─ infra/                                  # OkHttp, trust policies, adapters
│     └─ provider/                               # network binding/provider
├─ repository/
│  ├─ room/                                      # RoomClientRepository, RoomReportRepository, ...
│  ├─ mikrotik/                                  # MikroTikTestRepository impl(s), probe repos
│  ├─ backup/                                    # DefaultBackupRepository, backup managers
│  └─ common/                                    # shared data-layer helpers (owned)
├─ teststeps/                                    # step implementations (use ports, no DTO mapping inside)
├─ report/
│  └─ codec/                                     # MoshiReportResultsCodec
├─ pdf/
│  └─ itext/                                     # PdfGeneratorIText + helpers
└─ io/
   └─ android/                                   # Android document IO (SAF, etc.)
```

### 6.5 `ui/**` (feature-first + resilience)
```
ui/
├─ navigation/                                   # nav graph, typed routes, nav helpers
├─ theme/
├─ components/                                   # reusable composables
├─ common/                                       # UiState base, shared UI models/patterns
└─ feature/                                      # vertical slices
   ├─ splash/
   ├─ dashboard/
   ├─ probe/                                     # ProbeEdit, etc. (ProbeList removed as legacy)
   ├─ client/
   ├─ profile/
   ├─ test/
   ├─ history/
   └─ settings/
```

### 6.6 `di/**` (Hilt modules, split by responsibility)
```
di/
└─ module/
   ├─ DatabaseModule.kt
   ├─ NetworkModule.kt
   ├─ DataStoreModule.kt
   ├─ RepositoryBindingsModule.kt
   ├─ UseCaseBindingsModule.kt
   ├─ PdfModule.kt
   ├─ BackupModule.kt
   └─ TestRunnerModule.kt
```

---

## 7. Naming conventions

### 7.1 Ports and repositories
- Ports (interfaces): `XxxRepository`, `XxxReader`, `XxxWriter`, `XxxCodec`
- Implementations: prefer **technology-based** names:
  - `RoomClientRepository`, `RoomReportRepository`
  - `MikroTikTestRepository`
  - `DataStoreUserPreferencesRepository`
  - `DefaultBackupRepository`
- Avoid ambiguous names:
  - ❌ `RepositoryImpl`
  - ❌ `NetworkConfigRepositoryImpl` (unless there’s no better qualifier)
  - ❌ package names like `repositoryimpl/`

### 7.2 Remote layer
- Retrofit: `MikroTikApiService` or `MikroTikApi`
- DTO: `XxxDto`
- Mappers: `toDomain()` / `toDto()` in `data/remote/.../mapper/`

### 7.3 Room layer
- Entity: `XxxEntity`
- Dao: `XxxDao`
- Mapper: `toEntity()` / `toDomain()` in `data/local/room/mapper/`

### 7.4 UI feature files
Inside each `ui/feature/<feature>/`:
- `XxxScreen.kt`
- `XxxViewModel.kt`
- `XxxUiState.kt`
- `XxxEvent.kt` (if using event/intents)
- `components/` (feature-specific composables if needed)

### 7.5 Remove “bucket files”
- Avoid `Utils.kt`, `Models.kt` containing unrelated types.
- Prefer:
  - one public type per file, or
  - grouping strictly-related sealed/data classes together.

---

## 8. Feature map (how the app works today)
This feature map was requested explicitly in the chat to confirm scope before refactor.

### 8.1 Probe Config (single probe)
- UI: `ProbeEditScreen` + `ProbeEditViewModel`
- DB: `probe_config` singleton row (`id = 1`)
- Functions:
  - Save `ip/user/pass/interface/isHttps`
  - “Check probe” via probe connectivity repository (board name, interfaces)

**Legacy to remove:** Probe list feature (contradicts “single probe”).

### 8.2 Clients
- UI: `ClientListScreen`, `ClientEditScreen`
- DB: `clients`
- Client data includes:
  - network mode DHCP/static
  - socket-id template
  - speed test server
  - min link rate

### 8.3 Test Profiles
- UI: `TestProfileListScreen`, `TestProfileEditScreen`
- DB: `test_profiles`
- Defaults inserted at DB creation (e.g., “Full Test”, “Quick Test”).

### 8.4 Run Test (execution)
- UI: `TestExecutionScreen`, `TestViewModel`
- Domain: `RunTestUseCase` emits events/steps via Flow
- Step implementations: `data/teststeps/*`
- Output: `TestReport` with `resultsJson` payload

### 8.5 History + Report Detail
- UI: `HistoryScreen`, `ReportDetailScreen`
- DB: `test_reports`
- Parse results JSON: `ParseReportResultsUseCase` or `ReportResultsCodec` into report domain models.

### 8.6 PDF Export
- Contract: `PdfGenerator` + export configuration types
- Impl: iText generator in data layer (must not depend on legacy core stubs).

### 8.7 Backup Import/Export
- JSON format: `BackupData(version=1)`
- Includes: probe + clients + profiles + reports
- `reports.clientKey` is nullable (`String?`) by decision.

### 8.8 Preferences
- Theme config + palette
- ID numbering strategy (continuous vs fill gaps)
- Repo: `UserPreferencesRepository`
- Use cases observe/set for preferences.

---

## 9. Refactor work items

### 9.1 A — Fix SOLID violation: DTO MikroTik exposed in ports
**Problem**
- A port interface (in `core/data`) imports DTO types from `data/remote/.../dto` (inversion).

**Required outcome**
- Ports expose **only domain/boundary models**.
- DTO parsing/mapping stays in `data/remote/mikrotik/**`.

**Implementation plan**
1) Redesign `MikroTikTestRepository` to return domain models:
   - `monitorEthernet(...) : LinkStatusData`
   - `cableTest(...) : CableTestSummary`
   - `ping(...) : List<PingMeasurement>` (or equivalent domain type)
   - `neighbors(...) : List<NeighborData>`
   - `speedTest(...) : SpeedTestData`
2) Remove unused endpoints (chat specifically suggested `systemResource()` was only used in interface+impl).
3) Create mapper file: `data/remote/mikrotik/mapper/MikroTikTestMappers.kt`
4) Update step implementations to stop doing ad-hoc DTO conversion.

**Files typically involved (as stated in chat)**
- `core/data/repository/test/MikroTikTestRepository.kt`
- `data/repository/mikrotik/MikroTikTestRepositoryRemote.kt`
- `data/remote/mikrotik/mapper/MikroTikTestMappers.kt` (new)
- `data/teststeps/*` (several steps)

### 9.2 B — Move business rule out of repository: `nextIdNumber` increment
**Problem**
- `RoomReportRepository.saveReport()` increments `client.nextIdNumber` (domain policy living in persistence).

**Decision dependency**
- Duplication does **not** increment (confirmed).

**Required outcome**
- Repository is CRUD only.
- Use case applies policy for run-test flow only.

**Implementation plan**
1) Make `RoomReportRepository` save report without touching `ClientRepository`.
2) Introduce `SaveTestReportUseCase` (or `FinalizeTestReportUseCase`):
   - Inputs: report + (optional) metadata needed to identify “run test” context
   - Behavior: save report, and if `PASS` then update client `nextIdNumber`
3) Ensure call sites:
   - **Run Test** uses use case
   - Duplicate/import/restore uses repository raw
4) Update tests to target use case.

### 9.3 C — DB destructive migration in pre‑production
**Required outcome**
- Add `.fallbackToDestructiveMigration(dropAllTables = true)` in Room builder.
- Comment in English to clarify the “pre-production destructive policy”.

### 9.4 D — PDF generator refactor: remove legacy dependencies
**Problem**
- iText generator imports legacy parser/stub and removed utils.

**Required outcome**
- `PdfGeneratorIText` uses the canonical contract:
  - `ReportResultsCodec.decode(resultsJson)` (or equivalent use case)
- Delete legacy core PDF stubs:
  - `core/data/pdf/parser/ParsedResultsParser.kt`
  - `core/data/pdf/impl/PdfGeneratorIText.kt` (stub)
  - `core/data/pdf/PdfDocumentHelper.kt` (stub)

### 9.5 E — Remove TODO/FIXME + legacy comments + BOM
**Required outcome**
- No TODO/FIXME/comments referencing legacy epics or deprecated routes.
- Normalize encoding: no BOM.
- Replace TODO notes with explicit statements (“override intentionally not supported by current flow”, etc.).

### 9.6 Extra (mandatory) — Remove hardcoded UI strings
**Reason**
- There is a quality test scanning for hardcoded strings (described in chat).

**Required outcome**
- Replace hardcoded literals with `stringResource(R.string.key)`.
- Add strings to `res/values/strings.xml`.
- Do **not** change text content; keep mixed language as-is.

### 9.7 Remove legacy UI feature: ProbeList
- Delete:
  - `ProbeListScreen.kt`
  - `ProbeListViewModel.kt`
  - any `probe_list` route
  - commented imports and dead routes in navigation graph

### 9.8 Remove dead/legacy utils and stubs
- Remove unused utils classes (as identified in chat) **if confirmed unused by the build**.
- Move what is truly needed into domain `policy/` (formatting/validation), with strict ownership.

### 9.9 Resolve naming collision: `BackupRepository`
**Problem**
- Both port and implementation share `BackupRepository` name in different packages, causing confusion and import mistakes.

**Required outcome**
- Rename implementation to something explicit:
  - `DefaultBackupRepository` (recommended)
  - or `BackupRepositoryImpl` if you must keep suffix

---

## 10. Database reset policy (pre‑prod)

### 10.1 Destructive migration until production
- Add `fallbackToDestructiveMigration(dropAllTables = true)`.
- Treat as **temporary pre-prod guardrail**; revisit before release.

### 10.2 “Restart DB from zero” allowance
- You explicitly allowed resetting DB and dropping migrations (baseline clean schema).
- Ensure:
  - schema export is updated
  - docs reference DB schema is regenerated from the exported JSON
  - any “default seed data” logic remains correct (profiles, etc.)

---

## 11. UI architecture guidelines (resilience to change)

### 11.1 Feature-first UI structure
- UI organized by feature under `ui/feature/*`.
- Avoid “god navigation” coupled to screens; keep routing typed where possible.

### 11.2 ViewModel responsibilities
- ViewModel depends on **use cases**, not repositories directly (where feasible).
- ViewModel exposes:
  - `StateFlow<UiState>` for persistent UI state
  - `SharedFlow<UiEvent>` for one-shot events (snackbar, navigation triggers, etc.)
- UI composables are mostly **stateless** and receive callbacks.

### 11.3 Navigation
- Prefer typed routes (sealed classes) to avoid string duplication and fragile routing.
- Remove legacy routes entirely (no commented “deprecated” calls).

### 11.4 Localization / strings
- Use `stringResource` for display strings, including content descriptions.
- Keep mixed-language text as-is (per decision); this is about **structure**, not translation.

---

## 12. Dependency Injection (DI) modularization

### 12.1 Problem
- A single DI module was described as too large/monolithic (binding repos, use cases, IO, providers together).

### 12.2 Required outcome
- Split DI by responsibility into multiple modules (examples):
  - `DatabaseModule`
  - `NetworkModule`
  - `DataStoreModule`
  - `RepositoryBindingsModule` (`@Binds` interfaces to impls)
  - `UseCaseBindingsModule`
  - `PdfModule`
  - `BackupModule`
  - `TestRunnerModule`

### 12.3 Best practice rule
- Modules that only bind interfaces: use `@Binds` + abstract class, name it `*BindingsModule`.
- Modules that provide concrete instances: `@Provides`, name it `*ProvidersModule` or keep the functional name.

---

## 13. Testing strategy and required updates

### 13.1 Layered test approach
- **Domain tests**: pure JUnit tests for policies/use cases.
- **Data tests**:
  - Room integration tests
  - Retrofit/DTO parsing tests (Golden tests with stable fixtures)
- **UI tests**:
  - ViewModel tests (Turbine for flows)
  - Instrumented Compose tests for basic navigation/rendering

### 13.2 Required test updates (caused by refactor)
- If ports are changed to remove DTOs:
  - update any “golden parsing” and step tests that previously relied on DTO types
- If socket-id increment policy moves to use case:
  - update `SocketIdLiteIncrementTest` to target use case
  - update `TestViewModelTest` wiring and expectations

### 13.3 Quality test: hardcoded strings scan
- Ensure `HardcodedStringsScanTest` passes after extracting UI strings.

---

## 14. Documentation deliverables and ADR set

### 14.1 Canonical docs to keep (Diátaxis)
- `docs/README.md` (index + rules)
- `docs/contributing/documentation.md` (how to write docs, Diátaxis + ADR templates)
- `docs/explanation/architecture.md` (layering, invariants, guardrails)
- `docs/explanation/features.md` (feature map)
- `docs/reference/project-structure.md` (this doc’s tree condensed as canonical target)
- `docs/reference/ui-architecture.md` (UI rules)
- `docs/reference/build.md` (how to build + version catalog snapshot)
- `docs/reference/database.md` (schema reference generated from Room schema export)
- `docs/reference/mikrotik-rest-api.md` (baseUrl, HTTPS toggle, endpoints, trust-all policy notes)
- `docs/reference/backup-format.md` (backup JSON format + clientKey nullable)

### 14.2 ADR set (minimum)
- `ADR-0001` Single probe (and why ProbeList is removed)
- `ADR-0002` HTTP/HTTPS toggle + trust-all conditional policy
- `ADR-0007` Package structure and naming
- `ADR-0008` No DTO leaks across ports
- `ADR-0009` Pre-prod destructive DB policy
- `ADR-0010` Socket-ID increment rule (PASS-only, run-test-only; duplication no)
- `ADR-0011` UI strings policy (no hardcoded; text kept as-is; moved to resources)

> ADR numbering in chat varied; the important part is that these decisions are recorded and discoverable.

---

## 15. Acceptance criteria / Definition of Done

### 15.1 Architecture
- No `core/**` imports from `data/**`.
- No DTO types are referenced in ports.
- Repositories are persistence-focused; business rules live in use cases/policies.

### 15.2 Code hygiene
- No TODO/FIXME / no commented legacy blocks.
- No unused legacy files (ProbeList, PDF stubs, dead utils) remain.
- UTF‑8 no BOM.
- New/modified files have header comments and meaningful English comments.

### 15.3 UI quality
- `HardcodedStringsScanTest` passes.
- UI uses `stringResource(...)` for all user-visible text and content descriptions.

### 15.4 DB policy
- `.fallbackToDestructiveMigration(dropAllTables = true)` enabled until production.
- Export schema and DB reference docs match the schema JSON.

### 15.5 Behavior
- Backup import/export supports `clientKey = null` for reports.
- `nextIdNumber` increments only on PASS during run test flow.
- Duplication/import/restore does **not** increment.

### 15.6 Tests
- Unit tests updated and passing for changed APIs.
- Data parsing tests updated to new DTO→domain mapping boundaries.
- ViewModel tests adjusted to use new use cases.

---

## 16. Suggested migration order (PR plan)
This ordering reduces breakage and keeps diffs reviewable:

1) **Hard constraints prep**
   - Remove BOM
   - Remove TODO/FIXME + legacy commented code blocks
   - Ensure the project compiles unchanged functionally

2) **Remove legacy features and stubs**
   - Delete ProbeList UI + routes
   - Delete PDF core stubs and redundant parser layers

3) **Close DIP leak (DTO in ports)**
   - Update port interface + mapping
   - Update call sites, steps, tests

4) **Move business rule to use case**
   - Create `SaveTestReportUseCase`
   - Make repo CRUD-only
   - Update run test flow and tests

5) **DB destructive policy**
   - Add `fallbackToDestructiveMigration(dropAllTables = true)`
   - Update DB reference docs

6) **UI hardcoded strings extraction**
   - Move strings to `strings.xml` (text unchanged)
   - Make HardcodedStringsScanTest green

7) **DI modularization**
   - Split modules, ensure bindings are coherent
   - Verify tests and wiring still pass

8) **Final pass: naming collisions + cleanup**
   - Rename `BackupRepository` implementation to `DefaultBackupRepository`
   - Remove any leftover dead utils, bucket files, ambiguous packages

9) **Docs sync**
   - Regenerate/refresh Diátaxis docs + ADRs
   - Verify doc↔code references again

---

## 17. Risks and guardrails

### 17.1 Primary risks
- Large-scale signature changes (e.g., MikroTikTestRepository) can cause widespread breakage.
- String extraction may miss edge cases (strings used as keys rather than UI labels).

### 17.2 Guardrails
- Apply changes in small PRs with clear acceptance tests (unit + quality scan).
- Keep “ports” strictly free of DTO types and framework dependencies.
- Use “feature-first” naming for strings keys to avoid collisions and future drift.

---

## 18. Appendix: “Declared completed actions” from the chat
The chat contained several claims of “already done” work (not verifiable here). Keep as optional reference:
- ProbeList UI and route removal was claimed more than once.
- Removal of legacy PDF stubs and a parser layer (ParsedResultsParser) was claimed.
- Some utils were claimed removed and replaced with a domain formatting policy file.
- A “massive” UI hardcoded strings scan was claimed (100+ violations).
- Removal of dump markers like `END FILE` was claimed as a mechanical cleanup step.
- Docs ZIPs (v2/v3) were claimed as produced, with additions like:
  - `features.md`
  - `ui-architecture.md`
  - ADRs: package structure, no DTO leaks, etc.

> Treat these as “work that was intended/started” and confirm in the real repo via `git diff`, compilation, and tests.

---

**End of document**

