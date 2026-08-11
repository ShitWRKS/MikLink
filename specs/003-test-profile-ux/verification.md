# Verification: Test Profile Tabbed UX

**Baseline branch**: `develop`

**Baseline HEAD**: `eddcdb3726da08237e3784fae587e992f930f028`

**Baseline working tree**: clean; only `specs/001-native-agent-testing` and `specs/002-dependency-modernization` existed before this feature.

## Required local gates

| Gate | Result | Evidence |
|------|--------|----------|
| `StringsItalianCoverageTest` | PASS | Focused Gradle run completed successfully after final resource cleanup |
| `HardcodedStringsScanTest` | PASS | Focused Gradle run completed successfully after final resource cleanup |
| `TestProfileViewModelTest` | PASS | Focused Gradle run completed successfully, including blank/default and invalid-preview helper cases |
| `TestQualityPolicyTest` | PASS | Focused Gradle run completed successfully with unchanged policy source |
| `TestProfileThresholdPreviewTest` | PASS | Focused Gradle run completed successfully for Link scaling, deterministic RTT series, and derived throughput range |
| `:app:testDebugUnitTest` | PASS | `BUILD SUCCESSFUL`; full unit task executed with the implemented UI |
| `:app:lint` | PASS | `BUILD SUCCESSFUL`; reports written to `app/build/reports/lint-results-debug.*` |
| `:app:assembleDebug` | PASS | `BUILD SUCCESSFUL`; debug APK assembled |
| `:app:assembleDebugAndroidTest` | PASS | `BUILD SUCCESSFUL`; androidTest APK assembled and updated CRUD journey compiled |
| Main/unit Kotlin compilation | PASS | `:app:compileDebugKotlin :app:compileDebugUnitTestKotlin` completed after correcting two invalid explicit Compose imports |
| Android test Kotlin compilation | PASS | `:app:compileDebugAndroidTestKotlin` completed with the new stable-tab journey |

The initial targeted compilation failed because two files explicitly imported Compose's internal `weight` symbol. Removing those imports allowed normal `RowScope` resolution; the immediate rerun and all subsequent full gates passed.

## Device gates

| Gate | Result | Evidence |
|------|--------|----------|
| Device preflight | PASS | SDK-local ADB found one authorized device: serial `6pr8q4nncyhqrcx8`, model `22101316G`, API 34 |
| Preserving APK installation | PASS | Updated app and androidTest APKs installed with `adb install -r -t`; no uninstall or data clear was attempted |
| `ProfileCrudUiTest` | PASS | Focused device run completed `OK (1 test)` after verifying the Link and local Ping slider semantic surfaces plus create/edit/reopen/delete parity |
| `FunctionalAcceptanceSuite` | PASS with declared NOT_RUN | Suite completed `OK (8 tests)`: seven scenarios reported `ASSERTIONS_PASSED`; Probe configuration reported `NOT_RUN` with `PROBE_UNREACHABLE_TIMEOUT` |

## Scope and parity review

| Check | Result | Evidence |
|-------|--------|----------|
| Domain models and quality policy unchanged | PASS | No diff under `core/domain/model` or `core/domain/policy`; no RTT minimum or quality-rule change |
| Persistence/schema/migrations unchanged | PASS | No diff under Room/local data or `app/schemas`; no entity, mapper, or migration file changed |
| No new dependency | PASS | No diff in `app/build.gradle.kts` or `gradle/libs.versions.toml` |
| EN/IT keys and canonical tagline preserved | PASS | Italian coverage and hardcoded-text scans pass; canonical tagline test remained unchanged |
| Semantic migration has no dead/duplicate IDs | PASS | `PING_CONFIG` and `profile_ping_config` have no application consumer; 106 scanned stable constants contain zero duplicate values; four tab IDs compile in androidTest |
| Old collapsible/global-threshold layout removed | PASS | No `CollapsibleCard`, `pingConfigExpanded`, `thresholdsExpanded`, or obsolete threshold-summary resource remains in application source |
| Existing form/domain ownership preserved | PASS | ViewModel remains owner of every field and save mapping; new helpers only derive preview values through existing validators/defaults |
| Release isolation and secret-safe evidence preserved | PASS | No release control surface, logging, credential, or evidence-format change; functional data remains generated and session scoped |
| UI cleanup and failure artifacts preserved | PASS | CRUD still deletes through UI; `SessionRecordCleanup` remains the scoped fallback and `ScenarioRule` retains failure capture |
| No TODO/FIXME introduced | PASS | Targeted scan found no source/documentation marker introduced by the feature |
| `git diff --check` | PASS | Command exited 0; only Git line-ending conversion notices were emitted |
| Pre-implementation artifact analysis | PASS | 30/30 functional requirements and 6/6 success criteria mapped; zero CRITICAL/HIGH/MEDIUM/LOW findings |
| Post-implementation Spec Kit convergence | PASS | 30 requirements, 6 success criteria, 4 journeys, plan decisions, and 5 constitution principles checked; zero findings and zero appended tasks |

## Slider convergence correction

- User clarification identified a partial `SC-004` gap: previews reacted to text fields but lacked direct slider manipulation.
- Link now exposes a preset slider while retaining unrestricted custom entry through `StrictLinkRateParser`.
- Local/external Ping previews expose independent maximum-average and maximum RTT sliders; packet loss remains outside the RTT axis.
- Speed throughput bars expose download/upload sliders. Their UI ranges expand from effective current values and do not alter accepted domain values.
- Every slider writes through the existing ViewModel callbacks, so the numeric field and preview update in the same Compose state interaction.
- Stable slider semantics are included in `AgentUiTags.stableTags`; the CRUD journey verifies Link and Ping slider reachability.
- Focused preview-helper tests, full `:app:testDebugUnitTest`, `:app:lint`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest` all completed successfully after the correction.

## Throughput cap and copy follow-up

- The latest user clarification supersedes the earlier unbounded Speed throughput input: Download/Upload now accept `0..100,000 Mbps` (100G), including manual input and save validation.
- Speed sliders use a fixed logarithmic 0–100G mapping. Pure round-trip tests verify that a value update maps back to the same thumb fraction without adaptive-range repositioning.
- `StringTitleCapitalizationTest` scans convention-named headings in every available locale without enumerating screens or resource keys. Its first run found 44 inconsistencies across EN/IT resources; all are corrected and the rerun passes.
- Focused validator, ViewModel, preview, title-capitalization, Italian-coverage, and hardcoded-string tests passed.
- Full non-E2E gates passed: `:app:testDebugUnitTest`, `:app:lint`, `:app:assembleDebug`, and `:app:assembleDebugAndroidTest`.
- Updated app/test APK installation preserved existing data; `ProfileCrudUiTest` completed `OK (1 test)` before the request to stop further E2E runs.
- A previously started `FunctionalAcceptanceSuite` completed while its host wait was being stopped: `OK (8 tests)`, with seven scenario PASS outcomes and Probe configuration correctly classified `NOT_RUN` because the external probe was unreachable. No further E2E run was started.

## Terminal semantics

- `PASS`: the listed command/check actually completed successfully.
- `NOT_RUN`: an external prerequisite prevented valid execution.
- `SKIP`: a deliberately gated later suite was not attempted because its focused prerequisite did not pass.
