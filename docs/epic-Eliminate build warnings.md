EPIC: Eliminate build warnings (KT-73255 + Room/Compose deprecations) with zero tech debt
Context & problem

The current assembleDebug output shows two classes of warnings:

Kotlin annotation default target warning (KT-73255)

“This annotation is currently applied to the value parameter only, but in the future it will also be applied to field … use @param: … or -Xannotation-default-target=param-property …”
This is tied to Kotlin’s evolving rules for annotation use-site targets (param/property/field). Kotlin 2.2 introduces a preview defaulting rule and recommends explicit use-site targets when you need stable behavior.

Deprecations in Room + Compose Material3

Room: the no-arg fallbackToDestructiveMigration call is deprecated in favor of the overload with dropAllTables: Boolean.

Compose M3: TabRow deprecated in favor of PrimaryTabRow / SecondaryTabRow.

Compose M3: TopAppBarDefaults.centerAlignedTopAppBarColors deprecated; use topAppBarColors.

Goal

Zero warnings from the list you posted, without suppressions, without “temporary” flags, and without altering app behavior (UX unchanged except where API semantics require no-op replacements).

Non-goals

No dependency upgrades.

No refactor beyond what is necessary to remove the warnings cleanly.

No “turn all warnings into errors” unless explicitly requested (too risky as a blanket policy).

Key decision (anti-tech-debt)
Decision: Fix KT-73255 by using explicit use-site targets (NOT compiler flags)

We will not introduce -Xannotation-default-target=param-property because it’s a preview behavior change and can create hidden drift in how annotations are emitted in bytecode across the codebase. Kotlin explicitly documents both approaches and the “preview” nature of the new rule.

Instead:

For DI qualifiers on constructor parameters, we will use @param:....

For Moshi @Json(...) on properties, we will use @field:Json(...) (stable Java-visible placement; Moshi supports honoring annotations on fields/properties; using field: is a well-known safe approach for Kotlin interop).

Scope (files involved from your warning list)
KT-73255 (annotation default target)

app/src/main/java/com/app/miklink/data/io/AndroidDocumentWriter.kt

app/src/main/java/com/app/miklink/data/pdf/impl/PdfGeneratorIText.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/MikroTikServiceProviderImpl.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/dto/MikroTikDto.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/dto/SpeedTestRequest.kt

app/src/main/java/com/app/miklink/data/remote/mikrotik/dto/SpeedTestResult.kt

app/src/main/java/com/app/miklink/data/repository/mikrotik/MikroTikTestRepositoryRemote.kt

app/src/main/java/com/app/miklink/data/repository/mikrotik/MikroTikProbeConnectivityRepository.kt

Also add a quick search-based sweep for the same pattern in nearby files (e.g., AndroidDocumentReader.kt), so we don’t fix only the currently-reported subset and leave landmines for the next build variant.

Room deprecation

app/src/main/java/com/app/miklink/di/DatabaseModule.kt

Compose Material3 deprecations

app/src/main/java/com/app/miklink/ui/history/ReportDetailScreen.kt

app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt

Docs to update (docs-as-code consistency)

Search and update references of:

fallbackToDestructiveMigration no-arg usage

TabRow (deprecated)

annotation target policy (new doc to add)

Implementation plan (step-by-step, agent-proof)
Phase 0 — Guardrails (anti-drift)

Task 0.1: Add a “Forbidden Patterns” quality gate (source-based, deterministic)
Create a small, cross-platform checker that fails CI if deprecated/known-bad patterns reappear.

Preferred approach (portable): Gradle task using Kotlin

Add buildSrc (or existing build-logic) task checkForbiddenPatterns that scans app/src/** and docs/**.

Patterns to fail on:

fallbackToDestructiveMigration (no-arg variant)

TabRow or ScrollableTabRow calls

centerAlignedTopAppBarColors usage

constructor property injection pattern without use-site target:

@ApplicationContext private val

Moshi @Json(name = without use-site target:

(@Json\\() on same line as val / var in primary constructor

Output requirements

Print file path + line number + suggested fix.

Exit non-zero.

Acceptance

Running ./gradlew checkForbiddenPatterns fails on the current code, then passes once all tasks below are completed.

Phase 1 — Fix KT-73255 (DI qualifiers)

Kotlin’s docs: use-site targets @param:, @field:, etc.

Task 1.1: Apply @param: to Hilt qualifier annotations on constructor properties
Wherever you have:

@Inject constructor(
    @ApplicationContext private val context: Context
)


Change to:

@Inject constructor(
    @param:ApplicationContext private val context: Context
)


Rules

Use @param: for any constructor parameter qualifier when the parameter is also a property (private val ... / val ...).

Do not change behavior, do not move dependencies, do not add wrappers.

Files

All listed above with @ApplicationContext private val context: Context

Plus any additional hits from the sweep.

Task 1.2: Add/refresh the required top-of-file header comment
You requested this standard previously: every modified file must start with a short header comment describing purpose, inputs, outputs.

If a file already has such header: update it if necessary.

If missing: add it.

Acceptance

The KT-73255 warning lines related to Hilt qualifiers disappear.

No functional changes (app still runs; Hilt graph unchanged).

Phase 2 — Fix KT-73255 (Moshi DTOs)

Moshi + Kotlin often benefits from explicit field targeting for JSON name mapping; Kotlin docs explain why ambiguity exists and how @field: pins it.

Task 2.1: Apply @field:Json in DTO primary constructors
Wherever you have:

data class X(
    @Json(name = "foo") val foo: String
)


Change to:

data class X(
    @field:Json(name = "foo") val foo: String
)


Rules

Only for Moshi com.squareup.moshi.Json.

Do not rename fields.

Do not change defaults/nullability.

Keep formatting consistent.

Files

MikroTikDto.kt

SpeedTestRequest.kt

SpeedTestResult.kt

Also sweep app/src/test/** for Moshi DTOs (your tests/golden parsing likely compile under different tasks; eliminate future warning reintroduction).

Acceptance

All KT-73255 warnings in DTO packages are gone.

Existing parsing/golden tests still pass.

Phase 3 — Fix Room deprecation

Room docs: the no-arg fallbackToDestructiveMigration call is deprecated; use overload with dropAllTables (recommended true).

Task 3.1: Update Room builder call
In DatabaseModule.kt:

Replace:

.fallbackToDestructiveMigration(/* no-arg */)

With:

.fallbackToDestructiveMigration(dropAllTables = true)

Rules

Keep your current “pre-production destructive migration” policy unchanged.

Keep the existing inline comment but update it if it mentions the old signature.

Task 3.2: Update docs references
Search docs/** for the no-arg fallbackToDestructiveMigration mention and update examples to:

fallbackToDestructiveMigration(dropAllTables = true)

Acceptance

Room deprecation warning disappears.

App DB init behavior unchanged (still destructive migration in pre-prod).

Phase 4 — Fix Compose Material3 deprecations

Material3 release notes: TabRow is deprecated in favor of Primary/Secondary variants.
TopAppBarDefaults API: centerAlignedTopAppBarColors deprecated; use topAppBarColors.

Task 4.1: Replace TabRow with PrimaryTabRow
In ReportDetailScreen.kt:

Replace TabRow (selectedTabIndex = ...) { ... }

With PrimaryTabRow (selectedTabIndex = ...) { ... }

Rules

Choose PrimaryTabRow specifically to minimize visual drift from the old default TabRow behavior.

Don’t change the Tab contents.

Keep semantics and state logic identical.

Task 4.2: Replace deprecated top app bar colors factory
In TestExecutionScreen.kt:

Replace:

TopAppBarDefaults.centerAlignedTopAppBarColors (...)

With:

TopAppBarDefaults.topAppBarColors(...)

Rules

Keep the same parameter values (container/scrolled/icon/title/action colors).

Do not change any logic that decides which colors to use.

No UI redesign.

Acceptance

Compose deprecation warnings disappear.

Manual smoke check: open affected screens; tabs and top bar look/behave the same.

Verification checklist (Definition of Done)
Build / static

 ./gradlew :app:assembleDebug --warning-mode all shows none of the posted warnings.

 ./gradlew checkForbiddenPatterns passes (new gate).

 No new warnings introduced by the changes.

Tests

 Unit tests pass: ./gradlew testDebugUnitTest

 Instrumentation tests (at least smoke subset) pass: ./gradlew connectedDebugAndroidTest (or your CI equivalent)

Code quality

 No @Suppress("DEPRECATION") added.

 No preview compiler flags added (-Xannotation-default-target=param-property avoided).

 All modified files have the required header comment (purpose / inputs / outputs).

Docs-as-code

 docs/** updated where it references deprecated APIs (fallbackToDestructiveMigration (no-arg) old signature, etc.).

 Add a short new reference doc: docs/reference/annotations-use-site-targets.md describing:

When to use @param: vs @field: (DI vs JSON DTOs)

Rationale referencing Kotlin’s use-site target rules (link/citation for future maintainers).

Rollback strategy

All changes are mechanical and localized. If anything unexpected happens:

Revert Phase 2 (DTO annotations) first.

Revert Phase 4 (Compose) next.

Keep Phase 3 (Room) unless Room version is pinned pre-2.7 (but your warning confirms you’re on the deprecated API).

Notes for the agent (to prevent “drift by creativity”)

Do not refactor architecture, DI modules, or UI structure.

Do not change any runtime logic—only annotation targets and API replacements.

Keep diffs minimal and reviewable.

If you encounter another warning of the same kind, fix it in the same pass (don’t leave “we’ll do it later”).

