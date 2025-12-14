You are working on Android/Kotlin project MikLink.

GOAL (single epic): make the project pass BOTH:
1) ./gradlew :app:testDebugUnitTest
2) ./gradlew :app:lintDebug
AND achieve “SOLID/layering compliance” and “legacy removal” per findings below.

CONSTRAINTS
- Do NOT add a lint baseline.
- Minimal diffs where possible, but correctness over minimalism if needed.
- Preserve existing behavior (tests must continue to pass).
- Keep domain free of com.app.miklink.core.data.remote.* imports.
- Keep core/data free of com.app.miklink.ui.* imports.
- Avoid Android framework usage in domain.
- Provide patch steps + show diffs per file. After each milestone, run the required Gradle task and report remaining failures count.

MILESTONE 0 — TRIAGE / INVENTORY (no changes yet)
- Run and capture:
  - ./gradlew :app:testDebugUnitTest
  - ./gradlew :app:lintDebug
- Print summary of lint ERRORs grouped by IssueId (at least: SuspiciousIndentation, RestrictedApi, LocalContextGetResourceValueCall).
- Locate all references to:
  - com.app.miklink.ui.history.model.ParsedResults
   - legacy Room v1 package path (vecchia AppDatabase)
   - riferimenti all'AppDatabase legacy del modulo data
  - com.app.miklink.core.data.remote.* inside core/domain/**
  - TonalPalette usage in Theme.kt

MILESTONE 1 — LINT MUST PASS (0 errors)
Fix lint errors with the following approach:
1) SuspiciousIndentation (HistoryScreen.kt around line ~312)
   - Ensure showExportDialog?.let { ... } is actually inside the intended scope (Scaffold content) or outdented to correct scope.
   - Fix indentation so lint stops flagging it.
2) RestrictedApi (Theme.kt TonalPalette.*)
   - Remove all TonalPalette.fromInt / tone usage (private restricted API).
   - Replace with public Material3 APIs:
     - dynamicLightColorScheme/dynamicDarkColorScheme for Android 12+
     - lightColorScheme/darkColorScheme fallback
   - Ensure no RestrictedApi lint errors remain.
3) LocalContextGetResourceValueCall (Compose)
   - Replace context.getString/resources.getString usage in composables with stringResource().
   - If used inside coroutine/callback, capture stringResource(...) in a local val before launching.
After each sub-step, run: ./gradlew :app:lintDebug and report remaining errors.

MILESTONE 2 — REMOVE UI↔DATA COUPLING IN PDF PIPELINE (BLOCKER)
Problem: core/data/pdf and parser import UI model com.app.miklink.ui.history.model.ParsedResults; that UI model depends on MikroTik DTOs.
Target state:
- PDF generator and parsing must consume ONLY domain/report models.
- No imports from com.app.miklink.ui.* inside core/data/**.
Work items:
A) Create or reuse a domain model family under:
   com.app.miklink.core.domain.model.report (e.g., ReportData / ParsedResultsDomain)
   - Must not depend on RouterOS/MikroTik DTOs.
B) Refactor ParsedResultsParser.kt to output ONLY domain report models.
   - Delete legacy JSON normalization/fallback branches once codec migration is complete.
C) Update PdfGeneratorIText.kt to consume the new domain report models.
D) Wire parser via DI (PdfModule) so it can be swapped/tested.
E) After migration, delete or relocate ui/history/model/ParsedResults (UI should map from domain to UI, not vice versa).
Validation:
- Run grep to ensure NO com.app.miklink.ui. imports remain in core/data/**.
- Run tests + lint.

MILESTONE 3 — DOMAIN MUST NOT DEPEND ON DATA/REMOTE DTOs (WARN -> FIX)
Problem: RunTestUseCaseImpl imports DTOs (MonitorResponse, CableTestResult, NeighborDetail, etc.) because StepResult.Success carries raw Any.
Target state:
- core/domain/** has zero imports from com.app.miklink.core.data.remote.*
Approach:
1) Introduce typed domain payloads per step (e.g., LinkStatusResult, CableTestSummary, NeighborDiscoveryResult, PingResultsSummary, SpeedTestSummary).
2) Make StepResult.Success generic or sealed with typed payloads (no Any).
3) Update step implementations (data/teststeps/*StepImpl) to map DTO -> domain payload BEFORE returning StepResult.
4) Update RunTestUseCaseImpl to use only domain payloads and remove DTO imports.
Validation:
- Search in core/domain/** for core.data.remote imports must return none.
- Run tests + lint.

MILESTONE 4 — LEGACY DB SCHEMA ARTIFACTS CLEANUP (WARN -> FIX)
Problem: only MikLinkDatabase exists (version 1) but repo contains schema exports for old DB classes:
- app/schemas/<legacy Room v1 AppDatabase>/13.json
- app/schemas/<legacy data-module AppDatabase>/7..13.json
Target state:
- Only app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/1.json remains tracked.
Steps:
1) Delete legacy schema folders from repo (or archive to docs/ if you must, but prefer delete).
2) Update docs (database.md and others) to reflect:
   - Database class: com.app.miklink.data.local.room.MikLinkDatabase
   - DB filename: miklink
   - Version: 1
3) Ensure Gradle Room schema export emits only MikLinkDatabase path going forward.
4) Add a small regression guard:
   - CI script or local check that fails if the legacy Room v1 namespace or the old data AppDatabase name appears in repo.
Validation:
- Re-run schema generation if needed; confirm only 1.json exists under schemas.
- Run tests + lint.

MILESTONE 5 — DEAD PLACEHOLDER INTERFACES (WARN -> DECIDE & ACT)
List:
- core/domain/logs/LogFilter.kt
- LogStreamPolicy.kt
- LinkStabilizer.kt
- NeighborSelector.kt
- SocketTemplate.kt
- SocketIdGenerator.kt
- TdrCapabilities.kt
These are unreferenced except ignored contract tests.
Action:
- For each, decide:
  A) Remove interface + ignored tests and add short note in docs/roadmap
  OR
  B) Keep it, but add real call sites + DI bindings + real tests (no ignored).
Prefer A unless there is an active epic in progress.
Validation:
- No ignored contract tests remain for these placeholders (unless converted to real tests).
- Run tests + lint.

Status:
- ✅ 2025-12-13 — Milestone 0: Baseline :app:testDebugUnitTest and :app:lintDebug runs captured and the inventory of legacy references recorded in [docs/reference/epic_done.md](docs/reference/epic_done.md).
- ✅ 2025-12-13 — Milestone 1: HistoryScreen indentation, Theme TonalPalette, and LocalContext string lookups fixed; latest :app:lintDebug run is clean and free of previous SuspiciousIndentation/RestrictedApi/LocalContextGetResourceValueCall errors.
- ✅ 2025-12-13 — Milestone 2: PDF parser/generator now operate on core/domain/model/report types, `ui/history/model/ParsedResults` was removed, and grep confirms no com.app.miklink.ui.* imports remain under core/data/**.
- ✅ 2025-12-13 — Milestone 3: Step orchestration now uses typed domain payloads, and grep confirms core/domain/** has zero com.app.miklink.core.data.remote.* imports; :app:testDebugUnitTest + :app:lintDebug both pass.
- ✅ 2025-12-13 — Milestone 4: Legacy Room schema folders deleted, docs updated with MikLinkDatabase v1 details, guardLegacySchemas added, and only app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/1.json remains tracked.
- ✅ 2025-12-13 — Milestone 5: Option A applied to all placeholder interfaces, ignored contract tests removed, and the future backlog is tracked in [docs/reference/epic_done.md](docs/reference/epic_done.md).

DEFINITION OF DONE (ALL MUST BE TRUE)
- :app:testDebugUnitTest passes.
- :app:lintDebug passes with 0 errors.
- No file in core/data/** imports com.app.miklink.ui.*
- No file in core/domain/** imports com.app.miklink.core.data.remote.*
- PDF pipeline consumes domain report models only.
- Only schema file tracked: app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/1.json
- Repo contains no references to:
  - core.data.local.room.v1
  - com.app.miklink.data.db.AppDatabase
  - ui/history/model/ParsedResults
- Docs updated: database.md, epic_done.md, DISCREPANCIES.md (or equivalents) reflect new DB + codec + layering rules.

OUTPUT FORMAT
For each milestone:
- Show changed files list.
- Provide diffs/snippets of key changes.
- Paste command output summary: tests + lint status.
- Provide final “DoD checklist” with ✅/❌.

Start now with Milestone 0 and proceed sequentially until DoD is met.
