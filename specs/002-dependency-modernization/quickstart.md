# Quickstart: Verify Dependency Modernization

## Prerequisites

- Checkout the feature changes based on `develop` with a clean or intentionally reviewed worktree.
- Use the repository's configured Java 17 Gradle daemon toolchain and Android SDK.
- For runtime verification, connect and authorize an Android API 30+ device. Resolve ADB from `sdk.dir` in `local.properties` when it is not in `PATH`.
- No RouterOS probe, network mutation, signing secret, or production credential is required for the focused PDF scenario.

## 1. Toolchain and generated sources

```powershell
.\gradlew.bat --version
.\gradlew.bat clean check
.\gradlew.bat :app:assembleDebug
```

Expected: Gradle 9.6.1; configuration succeeds; Hilt, Room, and Moshi KSP output compiles; no Kotlin metadata force is required.

## 2. Resolved iText Android graph

```powershell
.\gradlew.bat :app:dependencyInsight --dependency com.itextpdf --configuration debugRuntimeClasspath
.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath
```

Expected: resolved iText modules are the Android 9.7.1 distribution; no 7.2.6 artifact and no parallel generic Java Core distribution are present.

## 3. Focused production PDF runtime test

```powershell
.\gradlew.bat :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.app.miklink.e2e.catalog.PdfScenarioTest
```

Expected: portrait and landscape reports are generated via the production binding, reopened, and verified for page count, page orientation, header/title, report data/results table, footer/timestamp, and page number. Generated cache files are registered for cleanup.

Run the existing UI E2E PDF journey as a second vertical check:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.app.miklink.e2e.functional.PdfExportUiTest
```

Expected: the real history-detail dialog produces a retrievable PDF through the normal UI path.

## 4. Release and shrinker

```powershell
.\gradlew.bat :app:assembleRelease
.\gradlew.bat :app:analyzeReleaseR8Config
```

Expected: minification/resource shrinking succeeds without speculative iText keep rules. If the AGP task is unavailable, record `NOT_RUN`; if it fails, retain the full error and add only evidence-driven rules.

## 5. Final audit

```powershell
rg -n "7\.2\.6|com\.itextpdf:itext7-core|com\.itextpdf:itext-core|2\.3\.21|2\.3\.9|9\.5\.0|kotlin-metadata-jvm" . --glob '!specs/**'
git diff --check
git status --short
```

Expected: no stale active configuration or workaround references; documentation history is distinguished from active declarations; only feature-related files are changed.

## 6. GitHub activation boundary

Validate `.github/dependabot.yml` and `.github/workflows/dependency-submission.yml` locally. The feature branch includes them for alignment, but GitHub-side Dependabot remains inactive until those two files—without application upgrades—are placed on `master`. Do not merge automatically.
