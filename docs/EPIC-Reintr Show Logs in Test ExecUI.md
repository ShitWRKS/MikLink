EPIC — Reintroduce "Show Logs" in Test Execution UI (in-progress + completed)

**Status:** ✅ COMPLETATO  
**Data Completamento:** 2025-12-16  
**Implementato in:** ADR-0009  
**Files Implementati:**
- `core/domain/test/model/TestEvent.kt` — LogLine event
- `core/domain/usecase/test/RunTestUseCaseImpl.kt` — emitLog() in 35+ punti
- `ui/test/TestViewModel.kt` — Log collection con exhaustive handling
- `ui/test/components/RawLogsPane.kt` — Shared component
- `ui/test/components/TestExecutionTags.kt` — Semantics tags

---

0) Scope and intent

Reintroduce the "Show logs" feature in the Test Execution UI:

While a test is running (in-progress screen)

After a test completes (completed screen, regardless of PASS/FAIL)

The solution must be:


SOLID / Clean Architecture compliant (UI depends on domain/use cases only; no data impl leakage)

No technical debt (no TODOs, no “just for now”, no duplicated UI blocks, no hardcoded strings)

Docs-as-code aligned: code + docs + ADR updated in the same change set.

Current domain event stream from the test runner is a Flow<TestEvent> and today includes Progress, SectionsUpdated, Completed, Failed. 

dump_core_domain


The use case contract is RunTestUseCase.execute(plan): Flow<TestEvent>. 

dump_core_domain

1) Product requirements (UX spec)
1.1 In-progress screen

Add a TextButton toggle near the top of the screen (after the progress header/card and before the section list):

Label toggles:

“Show raw logs” → “Hide raw logs”

(localized in EN + IT)

Optional icon (recommended): code/visibility-off style.

When enabled:

Show a scrollable log pane (bounded height) with appended lines.

Auto-scroll to the most recent line when new lines arrive.

When disabled:

No log pane is shown (keeps the screen clean).

1.2 Completed screen (PASS or FAIL)

Add a TextButton toggle in the “details area header row” (near “Dettagli Test” / details section header).

Label toggles:

“Show logs” → “Hide logs”

(localized in EN + IT)

When enabled:

Show a scrollable log pane (bounded height; min/max) with the lines collected during execution.

Auto-scroll to last line on open and on new line additions (if lines can still arrive in edge cases).

When disabled:

Logs are hidden.

1.3 Behavior rules

Logs are ephemeral:

Never persisted to DB/report JSON.

Kept in-memory only in ViewModel state.

Logs are bounded to prevent memory/performance issues:

Keep last N lines (e.g., 400–1000; see “anti-drift” section for exact policy).

When trimming occurs, optional single line like: "... (older logs trimmed)".

Logs must be safe:

No credentials/tokens/passwords appear in logs.

If any content risks containing secrets, sanitize/redact.

2) Architecture design (Clean + SOLID)
2.1 Domain layer changes

Add a new event to the domain event stream:

sealed class TestEvent {
  data class Progress(val progress: TestProgress) : TestEvent()
  data class LogLine(val message: String) : TestEvent() // NEW
  data class SectionsUpdated(val sections: List<TestSectionResult>) : TestEvent()
  data class Completed(val outcome: TestOutcome) : TestEvent()
  data class Failed(val error: TestError) : TestEvent()
}


This keeps the log transport framework-free and consistent with the existing Flow<TestEvent> design. 

dump_core_domain

“Raw logs” are transported as plain text lines; any formatting/redaction policy is applied before emission (in use case) or before display (in ViewModel), but must be centralized (no ad-hoc filtering scattered around).

2.2 Use case emission strategy

The use case already emits progress events at multiple steps. 

dump_core_domain

Extend the use case to emit TestEvent.LogLine(...) at meaningful points (minimum viable but useful):

Start of execution (“Initialization…”, client/profile/probe loaded — without secrets)

Start/end of each step (Network/Link/TDR/LLDP/Ping/SpeedTest)

On failures/skips with stable reason codes + human-readable message

On completion: overall PASS/FAIL

Important: avoid dumping raw JSON blobs or request payloads if they could include sensitive data. Prefer concise summaries.

2.3 UI / ViewModel responsibilities (SOLID)

Use case: emits events (including logs) — no Compose/UI dependencies.

ViewModel:

Collects Flow<TestEvent>

Maintains:

section list state

completion state/report state

log buffer state (bounded)

Compose UI:

Pure rendering and toggle state (rememberSaveable)

Displays the logs: List<String> fed by the ViewModel

3) Anti-drift mechanisms (mandatory)
3.1 Exhaustive event handling “compile-time drift stop”

In TestViewModel, remove any else branch in when(event) and handle every TestEvent explicitly.
This ensures if someone adds a new TestEvent in the future, compilation forces a decision in the collector logic.

3.2 Centralized log buffer policy (single source of truth)

Create a dedicated, test-covered component:

ExecutionLogBuffer (pure Kotlin)

append(line: String)

keeps last MAX_LOG_LINES

optional trimming marker

MAX_LOG_LINES is defined once (e.g., ExecutionLogsConfig.MAX_LINES) and used by both code + tests.

3.3 UI test stability via semantics tags

Add testTags for:

In-progress toggle button

Completed toggle button

Log pane container

A single log line node (optional)

Update instrumentation tests to use tags rather than matching localized text.

3.4 Quality tests must pass

No hardcoded strings: add string resources in values + values-it

Existing quality tests must pass (HardcodedStringsScanTest / Italian coverage tests as applicable per project docs)

4) Work breakdown (tasks)

Every Kotlin file you touch must have (or keep) a top header comment describing purpose / inputs / outputs. If a file already has it, update it only if needed; do not remove it.

Task A — Domain: add TestEvent.LogLine

Files

app/src/main/java/com/app/miklink/core/domain/test/model/TestEvent.kt

Steps

Add data class LogLine(val message: String) : TestEvent()

Ensure no Android/Compose imports appear in domain module.

Run compilation; fix any exhaustive when failures across the codebase.

Acceptance criteria

App compiles

All references to TestEvent remain correct

Task B — UseCase: emit log lines

Files

app/src/main/java/com/app/miklink/core/domain/usecase/test/RunTestUseCaseImpl.kt 

dump_core_domain

Steps

Add a private helper inside execute() scope (or a private function) like:

suspend fun emitLog(message: String)

ensures redaction/sanitization (see Task C)

Emit logs at:

initialization (start)

before each step runs

after each step returns with status (PASS/FAIL/SKIP)

on completion (overall status)

on failure (include only safe error message)

Keep log lines concise and consistent:

Prefix with step name: [Ping] ...

Avoid multi-line payload dumps

Ensure progress emission remains as-is (do not remove it). 

dump_core_domain

Acceptance criteria

Log events appear during execution (verified via unit test or manual run)

No secrets appear in emitted log lines (validated by sanitizer tests in Task C)

Task C — Log safety: redaction/sanitization

Files (new)

app/src/main/java/com/app/miklink/core/domain/test/logging/LogSanitizer.kt (or a similarly precise location/name consistent with ADR-0007 naming guidance)

Implementation rules

Redact patterns:

password=...

token=...

Authorization: ...

any probe credentials fields if they appear

Truncate very long lines (e.g., > 500 chars) with …

Tests (new)

app/src/test/java/.../LogSanitizerTest.kt

verifies redaction + truncation

Acceptance criteria

Sanitizer is covered by tests

Use case uses sanitizer before emitting LogLine

Task D — ViewModel: collect and expose logs

Files

app/src/main/java/com/app/miklink/ui/test/TestViewModel.kt

Steps

Add:

_logs: MutableStateFlow<List<String>>

val logs: StateFlow<List<String>>

Clear logs at the start of startTest()

In event collector:

Progress: optionally append progress.message (or a formatted line like [${progress.currentStep}] ${progress.message})

LogLine: append message

Keep SectionsUpdated, Completed, Failed behavior unchanged

Remove the else branch and make the when exhaustive.

Acceptance criteria

Logs are visible to UI through StateFlow

No else -> ignore remains for TestEvent handling (anti-drift)

Task E — UI: add toggles + log panes (no duplication)

Files

app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt

New composable file recommended:

app/src/main/java/com/app/miklink/ui/test/components/RawLogsPane.kt

app/src/main/java/com/app/miklink/ui/test/components/TestExecutionTags.kt (or similar)

Steps

Wire logs from ViewModel into TestExecutionScreen and down to:

TestInProgressView(...)

TestCompletedView(...)

Add toggle state via rememberSaveable:

In-progress: showRawLogs

Completed: showLogs

Implement RawLogsPane(logs: List<String>, modifier: Modifier, ...)

bounded height

internal vertical scrolling

auto-scroll on logs.size change

Add semantics testTags to:

toggle buttons

log pane container

Acceptance criteria

Toggle appears in both screens

Logs display correctly, scroll, auto-scroll, and can be hidden

UI remains responsive with large logs (bounded buffer prevents freeze)

Task F — Strings: restore EN + IT resources

Files

app/src/main/res/values/strings.xml

app/src/main/res/values-it/strings.xml

Add

test_toggle_show_raw_logs

test_toggle_hide_raw_logs

test_toggle_show_logs

test_toggle_hide_logs

Optional: test_logs_empty (“No logs yet” / “Nessun log”)

Acceptance criteria

No hardcoded strings in Compose implementation

Italian coverage tests (if enforced) pass

Task G — Tests: instrumentation + unit

Instrumentation

app/src/androidTest/java/com/app/miklink/ui/test/TestExecutionToggleTest.kt

Update from “assertDoesNotExist” to:

toggle visible

click toggle shows sample logs

click again hides

Use testTags (not localized text matching)

Unit

Add tests for ExecutionLogBuffer:

appending lines

trimming behavior

trimming marker presence/absence

Optional: ViewModel test verifying:

receiving Progress and LogLine updates logs as expected

Acceptance criteria

./gradlew test passes

./gradlew connectedCheck (or relevant androidTest task) passes

5) Documentation updates (docs-as-code)
5.1 New ADR to reintroduce logs

Add a new ADR (next available number):

Title suggestion: “Reintroduce Test Execution Logs as ephemeral UI-only feature”

Must explicitly reference ADR-0005 as prior decision and explain the trade-off reversal in a controlled scope:

logs are now allowed only as bounded, sanitized, UI-only, non-persisted

5.2 Update ADR-0005

Keep history, but mark as “Superseded” or add a “Superseded by ADR-XXXX” note (consistent with your ADR conventions).

ADR-0005 itself already anticipates reintroduction via dedicated epic/ADR. (So this is fully aligned.)

5.3 Update docs/explanation/features.md

In the “Runtime UX” section for test execution, add:

“Optional raw logs toggle during execution”

“Optional logs toggle in completed screen”

“Logs are not persisted; bounded in memory; sanitized”

Acceptance criteria

Docs accurately reflect the implemented behavior

Paths referenced in docs match actual code paths

6) Definition of Done (DoD)

 All tasks A→G completed

 No else branch hiding unhandled TestEvent variants in ViewModel collector

 Log buffer is bounded and unit-tested

 No hardcoded UI strings; EN + IT updated

 Instrumentation test covers both toggles using semantics tags

 ADR added + ADR-0005 updated + features doc updated

 All touched Kotlin files include/keep the required header comment (purpose/input/output)

 ./gradlew test + ./gradlew connectedCheck green

 No new lint/Detekt regressions (if configured in project)

7) Implementation notes (to keep it “clean”)

Prefer one shared RawLogsPane composable used by both screens (avoid duplicated UI blocks).

Keep log formatting consistent:

one-line entries

stable step prefixes

Keep sanitization centralized:

do not redact in multiple places

Avoid “giant diff refactors”: only refactor where it directly reduces duplication caused by adding logs.