# EPIC — Live Test Cards, Detailed Ping, and Consistent Socket-ID Auto Increment (Settings vs Dashboard)

**Audience:** delivery agent with limited project context  
**Goal:** fix user-facing inconsistencies and improve test-run UX without introducing tech debt or architecture drift.

---

## 0) Mandatory reading (do this first)

Before touching code, read these project sources to avoid drift:

- `.github/copilot-instructions.md` (agent rules, “no assumptions”, maintainability expectations)
- `docs/explanation/architecture.md` (Clean / Ports & Adapters boundaries)
- `docs/reference/project-structure.md` (package structure + naming conventions)
- `docs/decisions/ADR-0004-socket-id-lite.md` (Socket-ID Lite formatting + increment rules)
- `docs/decisions/ADR-0007-package-structure-and-naming.md` (naming conventions)
- `docs/decisions/ADR-0008-no-dto-leaks-across-ports.md` (DIP guardrail: no DTO leaks)

**Non‑negotiables derived from docs:**
- `core/domain/**` must stay pure Kotlin (no Android/Room/Retrofit/iText).
- Avoid “generic Impl” names when the implementation is technology-specific.
- Business rules must not be duplicated across UI; use domain policies/use-cases.
- Socket-ID Lite formatting is deterministic and **always concatenates both separators** (even if prefix/suffix are blank).

---

## 1) Problem statement (what users reported)

1) **Test execution screen**: cards do not appear progressively / the execution timeline feels “stuck”.
2) **Ping card**: not detailed (missing packet loss chip and RTT information).
3) **Auto increment mismatch**: Settings behavior differs from Dashboard suggestion (especially “Fill gaps”).

---

## 2) Root causes confirmed in code

### 2.1 Test execution: only one PENDING section is shown
`app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt` builds `visibleSections` and includes **only the first PENDING** section, hiding the rest.  
This makes the run look incomplete and prevents the user from seeing the full plan up-front.

### 2.2 Ping card expects “Packet Loss”, but domain does not provide it
In `TestExecutionScreen.kt`, Ping details rendering searches for a detail with label **exactly** `"Packet Loss"` to show the LOSS chip.
`RunTestUseCaseImpl.pingDetails()` currently returns only `"targets" -> "<target>:OK/LOSS/ERR; …"` and does **not** include `"Packet Loss"` or RTT aggregates.

### 2.3 Dashboard “Fill gaps” cannot parse ID numbers from real socket names
`DashboardViewModel.findNextAvailableId()` extracts existing IDs via:
`report.socketName?.removePrefix(client.socketPrefix)?.toIntOrNull()`

This fails for real socket names shaped like:
`prefix + separator + paddedNumber + separator + suffix`  
Example: `"SW-001-A"` → removePrefix `"SW"` yields `"-001-A"` → not an Int → parsing fails → gaps never detected.

### 2.4 Settings preview drift from ADR-0004 formatting
`ClientEditScreen.kt` builds `socketPreview` conditionally:
- if prefix is blank, it drops `prefix + separator`
- if suffix is blank, it drops `separator + suffix`

ADR-0004 states the function **always concatenates both separators**, even when prefix/suffix are empty.  
Therefore the preview can differ from generated socket names used in reports/dashboard.

---

## 3) Epic outcome (Definition of Done)

### Must-have functional outcomes
- **Test execution screen** shows the full list of sections immediately (including PENDING), and statuses update live (PENDING → RUNNING → PASS/FAIL/SKIP).
- **Ping card** shows:
  - LOSS chip (driven by `"Packet Loss"`)
  - Min/Avg/Max RTT (if available)
  - Per-target breakdown lines (at least one per configured target)
- **Socket-ID auto increment**:
  - Settings preview matches ADR-0004 formatting
  - Dashboard “Fill gaps” correctly finds the first missing number based on historical report socket names

### Must-have engineering outcomes
- No layer violations (domain stays framework-free; UI does not duplicate business rules).
- No hidden tech debt: any new helper must have tests and a clear home in the package structure.
- Every modified file contains the required header comment (see §4).

---

## 4) Mandatory coding standard: file header comment

Add this block comment at the top of **every file you modify** (including existing files):

```kotlin
/*
 * Purpose: <what this file does in 1–3 sentences>
 * Inputs: <parameters, dependencies, state it consumes>
 * Outputs: <what it produces: state/flows/return values/side effects>
 * Notes: <invariants, edge cases, assumptions, links to ADR if relevant>
 */
```

**Do not** replace existing headers if already present; **extend** them if incomplete.

---

## 5) Implementation plan (tasks, file-by-file)

> Recommended delivery: 1 PR per task group (A/B/C), keeping diffs reviewable.

---

### A) Domain policy: single source of truth for Socket-ID Lite (format + parse)

#### A1 — Add a new pure domain policy file (NEW)
**File (new):**  
`app/src/main/java/com/app/miklink/core/domain/policy/socketid/SocketIdLite.kt`

**Package:** `com.app.miklink.core.domain.policy.socketid`

**Responsibilities:**
1) Format Socket-ID Lite deterministically (match ADR-0004):
```kotlin
fun format(
  prefix: String,
  separator: String,
  numberPadding: Int,
  suffix: String,
  idNumber: Int
): String
```

2) Parse the numeric ID from an existing socketName deterministically:
```kotlin
fun parseIdNumber(
  socketName: String,
  prefix: String,
  separator: String
): Int?
```

**Parsing rule (must match existing format):**
- The expected shape is: `prefix + separator + <digits> + separator + suffix`
- Implementation must:
  - verify it starts with `prefix + separator` (even if prefix is empty → the token is just `separator`)
  - isolate the number between the first and second separator
  - return `toIntOrNull()` on that number segment (support leading zeros)
- If any step fails (missing separators, wrong prefix token, non-digit number segment), return `null`.

**Why a policy file?**
- Eliminates drift between UI preview and dashboard parsing.
- Keeps logic in domain as required by architecture docs.

#### A2 — Optional but recommended: make `Client.socketNameFor` delegate to policy (MOD)
**File (modify):**  
`app/src/main/java/com/app/miklink/core/domain/model/Client.kt`

**Change:**
- Keep `Client.socketNameFor(idNumber: Int)` as the public canonical API.
- Internally delegate to `SocketIdLite.format(...)` to avoid future duplication.

**Acceptance checks:**
- Formatting output must remain identical to current behavior (still includes both separators).

#### A3 — Unit tests for policy (NEW)
**File (new):**  
`app/src/test/java/com/app/miklink/core/domain/policy/socketid/SocketIdLiteTest.kt`

**Test cases (minimum):**
- Format:
  - `format("SW", "-", 3, "A", 1) == "SW-001-A"`
  - `format("", "-", 2, "", 1) == "-01-"`
- Parse:
  - `parseIdNumber("SW-001-A", "SW", "-") == 1`
  - `parseIdNumber("-01-", "", "-") == 1`
  - `parseIdNumber("SW001A", "SW", "-") == null`
  - `parseIdNumber("SW-ABC-A", "SW", "-") == null`

---

### B) Fix Settings preview drift (Socket-ID Lite preview must match ADR)

#### B1 — Replace conditional preview building with domain policy (MOD)
**File (modify):**  
`app/src/main/java/com/app/miklink/ui/client/ClientEditScreen.kt`

**Current issue:** preview drops separators when prefix/suffix are blank.

**Required change:**
- Replace the `socketPreview` string assembly with a call to `SocketIdLite.format(...)`.
- Use a stable preview idNumber (e.g., 1) exactly as today.

**Important:**
- This screen does not have a `Client` instance; call the policy directly.
- Keep `Locale.US` for `String.format` behavior by implementing padding inside the policy in a stable locale.

**Acceptance checks:**
- Preview always contains both separators (even with blank prefix/suffix).
- Preview matches what `Client.socketNameFor(1)` would produce for the same fields.

**Cleanup:**
- Remove the now-dead local variables `prefixPart`/`suffixPart` and any related duplication.

---

### C) Fix Dashboard “Fill gaps” logic (auto increment consistency)

#### C1 — Parse existing IDs using domain policy (MOD)
**File (modify):**  
`app/src/main/java/com/app/miklink/ui/dashboard/DashboardViewModel.kt`

**Current bug:**
`removePrefix(client.socketPrefix).toIntOrNull()` fails for real formatted socket names.

**Required change:**
- Replace extraction with `SocketIdLite.parseIdNumber(report.socketName, client.socketPrefix, client.socketSeparator)`
- Ignore `null` parses (do not crash; just skip unparseable values)

**Gap algorithm requirement (keep simple and deterministic):**
- `existingIds` = sorted unique IDs
- `expectedId` starts at 1
- first mismatch returns `expectedId`
- if no gap, return `last + 1`, else 1 when empty

**Acceptance checks:**
- With historical socket names `SW-001-A`, `SW-002-A`, `SW-004-A` → next is **3**
- With no parseable socket names → next is **1**
- Unparseable socket names do not break the flow

#### C2 — Add a unit test for the “gap fill” computation (NEW or refactor)
Because `DashboardViewModel` involves Flow and repositories, choose one of the two approaches:

**Option 1 (preferred):** Extract a pure function in domain policy
- Add in `SocketIdLite.kt`:
```kotlin
fun firstMissingPositive(existing: Collection<Int>): Int
```
Then test it with simple unit tests.

**Option 2:** ViewModel unit test with fakes
- Create:
`app/src/test/java/com/app/miklink/ui/dashboard/DashboardFillGapsTest.kt`
- Provide a fake `ReportRepository` returning reports with socketName strings.
- Run under coroutine test dispatcher.

**Minimum acceptance for tests:**
- The “missing id” selection is covered by at least one deterministic unit test.

---

### D) Test execution screen: cards must be visible and update live

#### D1 — Remove “only one pending” filter (MOD)
**File (modify):**  
`app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt`

**Required change:**
- Remove `visibleSections` filtering logic.
- Group sections into “Info” vs “Test” using the original `sections` list, not the filtered subset.

**Why:** users must see the full plan; hiding PENDING sections creates a “stuck” feeling.

**Acceptance checks:**
- At test start, all sections are present (most PENDING).
- As events arrive, individual sections flip to RUNNING/PASS/FAIL/SKIP without sections disappearing.

**Non-goals:**
- Do not redesign UI; just ensure visibility and correct live updates.

#### D2 — Add header comment to this file
This file currently lacks the required header comment. Add it without altering existing composables.

---

### E) Ping: provide “Packet Loss” and RTT details during execution

#### E1 — Upgrade `pingDetails()` (MOD)
**File (modify):**  
`app/src/main/java/com/app/miklink/core/domain/usecase/test/RunTestUseCaseImpl.kt`

**Requirements:**
- `pingDetails(results)` must return a `LinkedHashMap<String, String>` (order matters for UI readability).
- It must include **at minimum**:
  - `"Packet Loss"`: a single value used by the UI chip
  - `"Min RTT"`, `"Avg RTT"`, `"Max RTT"`: aggregated values if possible (fallback `"-"`)
  - `"targets"`: existing compact summary `<target>:OK/LOSS/ERR; …` (keep for quick glance)
  - One per-target line, e.g.:
    - `"Target 8.8.8.8"` → `loss=0% min=10ms avg=12ms max=15ms`
    - if error: `"Target 8.8.8.8"` → `ERR: <message>`

**Aggregation rules (must be written in code comments):**
- Prefer **worst-case loss** across targets for `"Packet Loss"` (max numeric) to avoid hiding issues.
- For RTT:
  - Min RTT = min of parsed per-target mins
  - Avg RTT = average of parsed per-target avgs
  - Max RTT = max of parsed per-target maxes
- Parse numbers safely from strings that may contain non-numeric chars (e.g., `"0%"`, `"12.3ms"`).
- If no parseable values exist, return `"-"` for that metric.

**Why this exact key?**
- The in-progress Ping UI explicitly searches for `"Packet Loss"` to show the chip; keep it stable.

#### E2 — Add targeted unit test for pingDetails output (NEW)
**File (new):**
`app/src/test/java/com/app/miklink/core/domain/usecase/test/PingDetailsContractTest.kt`

**Minimum checks:**
- Given a success outcome with packetLoss `"0"`:
  - details contains key `"Packet Loss"`
- Given an outcome with packetLoss > 0:
  - `"Packet Loss"` reflects a non-zero value
- At least one per-target entry is present for a configured target

> If `pingDetails()` remains private, extract a small pure helper (package-private or internal) to test deterministically without reflecting.

---

## 6) Tests: what may break and what to update

### Existing tests likely unaffected
- `app/src/androidTest/java/com/app/miklink/ui/test/TestExecutionToggleTest.kt` should remain valid because it only checks presence of “Ping” and absence of raw logs, not pending filtering.

### Tests you must add or update
- Add new unit tests for `SocketIdLite` format/parse.
- Add at least one deterministic unit test that proves “fill gaps” returns the first missing positive integer.
- Add a ping-details contract test to prevent regressions on `"Packet Loss"`.

### Test commands (run locally/CI)
- Unit tests: `./gradlew testDebugUnitTest`
- Instrumented tests (if available in pipeline): `./gradlew connectedDebugAndroidTest`

---

## 7) Cleanup and deletion candidates

### Code deletions (in-file)
- Remove conditional preview assembly code in `ClientEditScreen.kt` once replaced by policy formatting.
- Remove `visibleSections` computation in `TestExecutionScreen.kt`.

### File deletions
Based on repository scan in this epic scope, there is **no standalone file** that is clearly unused and safe to delete.  
If you discover dead files while implementing (e.g., duplicate helpers), you may delete them **only if**:
1) global reference search is zero,
2) app builds,
3) unit tests pass,
4) instrumented tests (if configured) pass.

---

## 8) Review checklist (PR gate)

- [ ] All modified files have the required header comment block.
- [ ] No layer violations (domain remains framework-free).
- [ ] Socket-ID Lite preview matches ADR-0004 behavior for blank prefix/suffix.
- [ ] Dashboard “Fill gaps” works with realistic socketName strings.
- [ ] Ping details contain `"Packet Loss"` + RTT aggregates + per-target lines.
- [ ] Test execution screen shows full plan at start and updates live.
- [ ] New unit tests added and passing; no existing tests regress.

---

## Appendix — Files in scope (exact paths)

**Modify:**
- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt`
- `app/src/main/java/com/app/miklink/core/domain/usecase/test/RunTestUseCaseImpl.kt`
- `app/src/main/java/com/app/miklink/ui/client/ClientEditScreen.kt`
- `app/src/main/java/com/app/miklink/ui/dashboard/DashboardViewModel.kt`
- (Optional) `app/src/main/java/com/app/miklink/core/domain/model/Client.kt`

**Add:**
- `app/src/main/java/com/app/miklink/core/domain/policy/socketid/SocketIdLite.kt`
- `app/src/test/java/com/app/miklink/core/domain/policy/socketid/SocketIdLiteTest.kt`
- `app/src/test/java/com/app/miklink/core/domain/usecase/test/PingDetailsContractTest.kt`
- (Optional) `app/src/test/java/com/app/miklink/core/domain/policy/socketid/FirstMissingPositiveTest.kt` (if you split tests)

