# EPIC-0001 — Single probe (no `probeId`) + Backup single-probe

**Status:** Draft (ready to execute)  
**Owner:** Agent  
**Scope:** Code + tests (no DB rebase yet; DB rebase is EPIC-0002)
- UI navigation and ViewModels
- Domain `TestPlan` and test execution orchestration
- Probe repository contract (no ID-based lookup)
- Backup import/export (single-probe)

- Do **not** rebase Room DB schema/version/name. (Handled in EPIC-0002)
- Do **not** remove `probeId` from Room entity/DAO/migrations packages (`core/data/local/room/v1/**`). (Handled in EPIC-0002)
- Do **not** redesign UI screens beyond what is required to remove `probeId` navigation/usage.
- Do **not** “fix” unrelated compile issues by inventing code. If you hit unrelated errors, STOP and ask.

## Hard rules for the agent (anti-drift)

1) **No invention:** If a file differs from what is described here (missing functions, different naming), STOP and ask before implementing.
2) **No new abstractions:** Do not introduce new architecture patterns or new files unless explicitly requested in this epic.
3) **Keep edits minimal:** Only touch the files listed below (plus any compiler-required follow-ups that you must report).
4) **Update this epic file:** As you complete each step, mark it as done and append a short “Implementation notes” section at the end with:
   - files touched
   - commands run
## Pre-flight checks (must do before editing)
- [x] `git status` is clean
- [x] `./gradlew test` is attempted once (even if it fails today). Record outcome in notes.  
  - Outcome: `./gradlew.bat testDebugUnitTest` executed → **BUILD SUCCESSFUL**

---

**Known anchors (from dump):**
- test execution route at line ~36
- probe edit route at line ~64–66

### Tasks
  - `test_execution/{clientId}/{profileId}/{socketName}`
- [x] Ensure `TestExecutionScreen(navController, vm)` remains unchanged.

- [x] Replace the `probe_add` and `probe_edit/{probeId}` routes with **a single route with no arguments**:
  - route name: `probe_config`
  - composable: `ProbeEditScreen(navController)` (keep file/class name for now)

**STOP condition:** If NavGraph contains literal placeholder lines like `...`, STOP and ask (do not guess what should be there).

---

## Step 2 — Update route builders that currently include `probeId`
### 2.1 Dashboard → Test execution route
**File:** `app/src/main/java/com/app/miklink/ui/dashboard/DashboardScreen.kt`  
**Anchor:** line ~198 contains `probe.probeId`

- [x] Update the navigate route string to match the new route shape:
  - `test_execution/<clientId>/<profileId>/<socketName>`
- [x] Ensure you no longer interpolate `probe.probeId` anywhere in this file.

### 2.2 History → Re-run test route
**File:** `app/src/main/java/com/app/miklink/ui/history/HistoryViewModel.kt`  
- line ~185 reads probe via `probeDao.getAllProbes()...`

  - Remove the `ProbeConfigDao` dependency from this ViewModel (constructor + imports).
- [x] Update route build to:
  - `test_execution/${report.clientId}/${profile.profileId}/$encodedSocket`
- [x] Ensure no reference to `probe.probeId` remains in this file.



**Anchor:** line ~150 navigates to `probe_edit/-1`

- [ ] Replace navigation target with:
  - `probe_config`
 - [x] Replace navigation target with:
   - `probe_config`
---

## Step 4 — Remove `probeId` from TestViewModel plan creation

**File:** `app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt`  
**Anchors:**
- line ~92 reads `probeId`
- line ~101 validates `probeId`
- line ~110 sets `probeId = probeId` in `TestPlan(...)`

### Tasks
- [ ] Remove reading `probeId` from saved state.
- [ ] Update validation to only require:
  - `clientId > 0`
  - `profileId > 0`
  - `socketName` decodes correctly
- [ ] Update `TestPlan(...)` construction to match the new `TestPlan` signature (after Step 5).

---

## Step 5 — Domain: remove `probeId` from `TestPlan` and execution flow

### 5.1 Update `TestPlan`

**File:** `app/src/main/java/com/app/miklink/core/domain/test/model/TestPlan.kt`  
**Anchor:** line ~9 has `val probeId: Long`

- [ ] Remove `probeId` from `TestPlan` entirely.

### 5.2 Update `RunTestUseCaseImpl`

**File:** `app/src/main/java/com/app/miklink/core/domain/usecase/test/RunTestUseCaseImpl.kt`  
**Anchors:**
- line ~67: `probeRepository.getProbe(plan.probeId)`
- line ~511: `probeId = plan.probeId` inside raw results JSON
- line ~633: `RawPlan` contains `val probeId: Long`

Tasks:
- [ ] Replace `probeRepository.getProbe(plan.probeId)` with a **no-arg** probe config lookup (see Step 6).
- [ ] Remove `probeId` from any serialized raw plan/results JSON.
- [ ] Remove `probeId` from `RawPlan` data class.

**STOP condition:** If removing `probeId` breaks JSON adapters/serialization, STOP and ask before reworking structure.

---

## Step 6 — Data port: ProbeRepository becomes singleton

**File:** `app/src/main/java/com/app/miklink/core/data/repository/probe/ProbeRepository.kt`  
**Anchor:** line ~9: `suspend fun getProbe(id: Long): ProbeConfig?`

- [ ] Replace the interface method with:
  - `suspend fun getProbeConfig(): ProbeConfig?`
- [ ] Ensure no other ID-based probe retrieval remains in this interface.

### Update binding

**File:** `app/src/main/java/com/app/miklink/di/RepositoryModule.kt`  
(Only adjust if compiler requires import/signature updates.)

---

## Step 7 — Update RoomV1ProbeRepository implementation to match singleton port

**File:** `app/src/main/java/com/app/miklink/data/repositoryimpl/roomv1/RoomV1ProbeRepository.kt`  
**Anchors:**
- line ~15: `override suspend fun getProbe(id: Long)`

Tasks:
- [ ] Replace `getProbe(id: Long)` with `getProbeConfig()`.
- [ ] Implement by reading the singleton from DAO:
  - Use `probeConfigDao.getSingleProbe().firstOrNull()`
  - Import `kotlinx.coroutines.flow.firstOrNull` if needed.

---

## Step 8 — Probe config UI: remove SavedState `probeId` and ID-based editing

**File:** `app/src/main/java/com/app/miklink/ui/probe/ProbeEditViewModel.kt`  
**Anchors:**
- line ~20: `BaseEditViewModel(..., "probeId")`
- line ~22: reads `probeId` from SavedState
- line ~99: `getProbeById(id)`
- line ~131: sets `probeId = if (isEditing) ...`

Tasks:
- [ ] Remove any use of SavedState key `"probeId"` and any ID-based editing logic.
- [ ] Load probe config via DAO singleton:
  - `probeConfigDao.getSingleProbe().firstOrNull()`
- [ ] Save probe config via singleton upsert:
  - `probeConfigDao.upsertSingle(probeToSave)`
- [ ] When building `ProbeConfig`, do **not** set the `probeId` named parameter in UI code.
  - (The current schema will be replaced in EPIC-0002; keep UI free from ID concepts.)

---

## Step 9 — Backup format: single probe (no list)

### 9.1 BackupData shape

**File:** `app/src/main/java/com/app/miklink/data/repository/BackupData.kt`  
**Anchor:** line ~11: `val probes: List<ProbeConfig>`

Tasks:
- [ ] Replace `probes: List<ProbeConfig>` with:
  - `probe: ProbeConfig?`
- [ ] Keep naming exactly `probe` (singular).

### 9.2 BackupManager export/import/rollback

**File:** `app/src/main/java/com/app/miklink/data/repository/BackupManager.kt`  
**Anchors:**
- line ~27: `probeConfigDao.getAllProbes()`
- line ~44: validation on `backupData.probes.any { ... }`
- line ~59: `insertAll(backupData.probes)`
- line ~72: rollback `insertAll(originalBackup.probes)`

Tasks:
- [ ] Export: replace `getAllProbes()` with `getSingleProbe()` and assign to `probe`.
- [ ] Import validation: validate `backupData.probe` (if null → invalid), and check required fields are not blank.
- [ ] Import apply: replace any `insertAll` usage with:
  - `probeConfigDao.upsertSingle(backupData.probe)`
- [ ] Rollback: replace `insertAll(originalBackup.probes)` with:
  - `probeConfigDao.upsertSingle(originalBackup.probe)` (only if non-null)

### 9.3 ImportBackupUseCase validation

**File:** `app/src/main/java/com/app/miklink/domain/usecase/backup/ImportBackupUseCase.kt`  
**Anchor:** line ~29 uses `backupData.probes.any`

Tasks:
- [ ] Replace list-based validation with singular validation on `backupData.probe`.

### 9.4 BackupRepository contract compilation

**File:** `app/src/main/java/com/app/miklink/core/data/repository/BackupRepository.kt`  
(Adjust only as needed so it compiles with new BackupData.)

---

## Step 10 — Tests: update only tests that will not compile

### Required updates (known breakages)

1) **RunTestUseCaseImplTest**
- **File:** `app/src/test/java/com/app/miklink/core/domain/usecase/test/RunTestUseCaseImplTest.kt`
- Anchors:
  - line ~163 / ~194: constructs `TestPlan(probeId = 1, ...)`
  - line ~189: stubs `probeRepository.getProbe(1)`

Tasks:
- [ ] Update `TestPlan(...)` construction to match new signature (no probeId).
- [ ] Update stubbing to `probeRepository.getProbeConfig()`.

2) **TestViewModelTest**
- **File:** `app/src/test/java/com/app/miklink/ui/test/TestViewModelTest.kt`
- Anchor: line ~46 contains `"probeId" to 1L`

Tasks:
- [ ] Remove the `"probeId"` saved state entry.

### Do NOT update (unless compilation requires)
- Contract tests that create `ProbeConfig(probeId = 1, ...)` are allowed to remain until EPIC-0002 (Room schema rebase).

---

## Completion gates (must run and record results)

- [ ] `git grep -n "probeId" app/src/main/java/com/app/miklink/ui app/src/main/java/com/app/miklink/core/domain app/src/main/java/com/app/miklink/domain app/src/main/java/com/app/miklink/data/repository`
  - Expected: **0 results**
  - Note: `core/data/local/room/v1/**` is excluded on purpose (handled in EPIC-0002).
- [ ] `git grep -n "test_execution/.*/{probeId}" -n app/src/main/java` (or equivalent)
  - Expected: **0 results**
- [ ] `./gradlew test`
  - If it fails due to *pre-existing unrelated code* (e.g., literal `...` placeholders), STOP and ask, providing error output.

---

## Implementation notes (agent must fill)

- Files touched:
- Commands run:
- Test results:
- Problems / discrepancies found (with exact file + line):
