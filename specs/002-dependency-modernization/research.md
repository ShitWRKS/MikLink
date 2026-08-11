# Research: Dependency Modernization

**Date**: 2026-08-11  
**Baseline**: `develop`, clean before specification artifacts; Gradle 9.5.0; baseline `check` and `:app:assembleDebug` passed; physical device `22101316G` online.

## Decisions

### Toolchain coherence

- **Decision**: Upgrade Gradle 9.5.0 → 9.6.1 using the wrapper task, AGP 9.3.0 → 9.3.1, Kotlin 2.3.21 → 2.4.10, KSP 2.3.9 → 2.3.10, and Hilt 2.59.2 → 2.60.1 as one verified toolchain slice.
- **Rationale**: The first four targets were already verified stable in the feature brief. Hilt 2.60.1 is the latest stable and its official release notes identify AGP 9 as the supported plugin baseline and a Kotlin update in 2.60, making it the correct point to retest the metadata workaround.
- **Alternatives considered**: Mechanically changing the forced `kotlin-metadata-jvm` version was rejected. Retaining Hilt 2.59.2 was rejected because it is no longer latest stable and the compatibility override must first be disproved with the newer toolchain.

### iText Android coordinate and relevant breaking changes

- **Decision**: Replace `com.itextpdf:itext7-core:7.2.6` with the final Android coordinate `com.itextpdf.android:itext-core-android:9.7.1`, then confirm the runtime selection via Gradle dependency insight.
- **Rationale**: The feature brief already verified that Maven Central's compatibility coordinate relocates to this Android artifact. The official iText 8 breaking-change list centers on forms/cryptography APIs MikLink does not use. The relevant iText 9 change is the event package/model migration: `IEventHandler` becomes `AbstractPdfDocumentEventHandler`, `Event` becomes `AbstractPdfDocumentEvent`, and `PdfDocumentEvent` moves to `com.itextpdf.kernel.pdf.event`. Other used layout, font, color, image, table, and canvas APIs have no documented required migration for this use.
- **Alternatives considered**: The generic `com.itextpdf:itext-core` artifact was rejected because MikLink is Android. Retaining Core 7 was rejected because it misses the required supported Android distribution. A second PDF engine or snapshot/pixel test was rejected for production-path fidelity.

### PDF verification design

- **Decision**: Extend the existing Android `PdfScenarioTest` instead of adding JVM-only PDF infrastructure. Generate both orientations through the Hilt-bound production `PdfGenerator`, reopen them with iText, extract text, verify pages/content/orientation, and clean up with the existing scenario rule.
- **Rationale**: This exercises Android resources, cache output, DI, `PdfGeneratorIText`, `PdfDocumentHelper`, and the actual Android artifact. Structural assertions are stable and cover the requested vertical path without pixel snapshots.
- **Alternatives considered**: A Robolectric-only test would not prove runtime Android compatibility. A UI-only signature/header check cannot verify internal page structure or text. A new snapshot framework would add unnecessary infrastructure.

### OkHttp 5 migration

- **Decision**: Upgrade `com.squareup.okhttp3:okhttp` 4.12.0 → 5.4.0, the latest stable shown by Maven Central, as an independently verified dependency slice.
- **Rationale**: The project directly uses OkHttp APIs and Retrofit 3.0.0 already aligns with the OkHttp 5 generation. Fixed stable 5.4.0 satisfies the latest-stable requirement; compile and networking unit tests will reveal any actually relevant API break.
- **Alternatives considered**: Pinning 4.12.0 without a reproduced failure was rejected. Adopting a 5.x preview was rejected; 5.4.0 is stable.

### Dependency automation

- **Decision**: Add `.github/dependabot.yml` for Gradle and GitHub Actions with normal updates targeting `develop`, plus a dedicated `.github/workflows/dependency-submission.yml` for pushes to `master` and `develop`.
- **Rationale**: No equivalent dependency-graph workflow exists. A dedicated workflow confines `contents: write` to graph submission and leaves release workflows unchanged. GitHub reads Dependabot configuration from the default branch, so the two files form a separate minimal activation boundary for `master`.
- **Alternatives considered**: Enabling graph generation indirectly in release workflows would couple security inventory to release signing. Adding only the file on `develop` would not activate Dependabot. Auto-merge and permanent ignores were rejected.

## Dependency Matrix

Primary repository metadata was read directly on 2026-08-11. Stable selection excludes versions containing preview qualifiers.

| Dependency / plugin | Current | Latest stable | Primary source | Breaking change relevant? | Action | Verification result |
|---------------------|---------|---------------|----------------|---------------------------|--------|---------------------|
| Gradle Wrapper | 9.5.0 | 9.6.1 | [Gradle releases](https://gradle.org/releases/) | No project-specific break identified | Update with wrapper task | Pending implementation |
| Android Gradle Plugin | 9.3.0 | 9.3.1 | Feature brief verified; [Google Maven](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml) | Patch only | Update | Pending implementation |
| Kotlin / Compose compiler plugin | 2.3.21 | 2.4.10 | Feature brief verified; [Kotlin releases](https://github.com/JetBrains/kotlin/releases) | Metadata level changes; paired verification required | Update | Pending implementation |
| KSP | 2.3.9 | 2.3.10 | Feature brief verified; [Maven Central](https://repo1.maven.org/maven2/com/google/devtools/ksp/com.google.devtools.ksp.gradle.plugin/maven-metadata.xml) | Generated-source compatibility | Update | Pending implementation |
| kotlinx-coroutines BOM/test | 1.11.0 | 1.11.0 | [Maven Central](https://repo1.maven.org/maven2/org/jetbrains/kotlinx/kotlinx-coroutines-bom/maven-metadata.xml) | No | Already latest | Baseline passed |
| MockK | 1.14.9 | 1.14.11 | [Maven Central](https://repo1.maven.org/maven2/io/mockk/mockk/maven-metadata.xml) | No relevant break identified | Update | Pending implementation |
| AndroidX Core KTX | 1.19.0 | 1.19.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/core/core-ktx/maven-metadata.xml) | No | Already latest | Baseline passed |
| Lifecycle Runtime KTX | 2.11.0 | 2.11.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/lifecycle/lifecycle-runtime-ktx/maven-metadata.xml) | No | Already latest | Baseline passed |
| Activity Compose | 1.13.0 | 1.13.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/activity/activity-compose/maven-metadata.xml) | No | Already latest | Baseline passed |
| Navigation Compose | 2.9.8 | 2.9.8 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/navigation/navigation-compose/maven-metadata.xml) | No | Already latest | Baseline passed |
| Compose BOM and BOM-managed UI libraries | 2026.06.00 | 2026.06.01 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml) | Patch only | Update | Pending implementation |
| Hilt Android/compiler/plugin | 2.59.2 | 2.60.1 | [Official releases](https://github.com/google/dagger/releases) | Hilt plugin requires AGP 9; minSdk 23; project minSdk is 30 | Update and retest metadata workaround | Pending implementation |
| AndroidX Hilt Navigation Compose | 1.4.0 | 1.4.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/hilt/hilt-navigation-compose/maven-metadata.xml) | No | Already latest | Baseline passed |
| Room runtime/KTX/compiler/testing/plugin | 2.8.4 | 2.8.4 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/room/room-runtime/maven-metadata.xml) | No | Already latest; remove duplicate plugin version | Pending cleanup |
| Retrofit and Moshi converter | 3.0.0 | 3.0.0 | [Maven Central](https://repo1.maven.org/maven2/com/squareup/retrofit2/retrofit/maven-metadata.xml) | No | Already latest | Baseline passed |
| Moshi/Kotlin/codegen | 1.15.2 | 1.15.2 | [Maven Central](https://repo1.maven.org/maven2/com/squareup/moshi/moshi/maven-metadata.xml) | No | Already latest | Baseline passed |
| OkHttp | 4.12.0 | 5.4.0 | [Maven Central](https://repo1.maven.org/maven2/com/squareup/okhttp3/okhttp/maven-metadata.xml) | Major version; source compatibility must be compiled/tested | Update separately | Pending implementation |
| DataStore Preferences | 1.2.1 | 1.2.1 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/datastore/datastore-preferences/maven-metadata.xml) | No | Already latest | Baseline passed |
| iText Core | 7.2.6 generic Java | 9.7.1 Android | Feature brief verified; [Android artifact POM](https://repo1.maven.org/maven2/com/itextpdf/android/itext-core-android/9.7.1/itext-core-android-9.7.1.pom) | Event API/package migration | Replace distribution and migrate handler | Pending implementation |
| Coil Compose/GIF | 3.5.0 | 3.5.0 | [Maven Central](https://repo1.maven.org/maven2/io/coil-kt/coil3/coil-compose/maven-metadata.xml) | No | Already latest | Baseline passed |
| JUnit 4 | 4.13.2 | 4.13.2 | [Maven Central](https://repo1.maven.org/maven2/junit/junit/maven-metadata.xml) | No | Already latest | Baseline passed |
| AndroidX Test JUnit | 1.3.0 | 1.3.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/test/ext/junit/maven-metadata.xml) | No | Already latest | Baseline passed |
| Espresso Core | 3.7.0 | 3.7.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/test/espresso/espresso-core/maven-metadata.xml) | No | Already latest | Baseline passed |
| UIAutomator | 2.4.0 | 2.4.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/test/uiautomator/uiautomator/maven-metadata.xml) | No | Already latest | Baseline passed |
| Robolectric | 4.15 | 4.16.1 | [Official release](https://github.com/robolectric/robolectric/releases/tag/robolectric-4.16.1) | Minor; Android framework shadow updates | Update | Pending implementation |
| AndroidX Tracing | 1.3.0 | 1.3.0 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/tracing/tracing/maven-metadata.xml) | No | Already latest | Baseline passed |
| AppCompat | 1.7.1 | 1.7.1 | [Google Maven](https://dl.google.com/dl/android/maven2/androidx/appcompat/appcompat/maven-metadata.xml) | No | Already latest | Baseline passed |
| Foojay resolver convention | 1.0.0 | 1.0.0 | [Gradle Plugin Portal metadata](https://plugins.gradle.org/m2/org/gradle/toolchains/foojay-resolver-convention/org.gradle.toolchains.foojay-resolver-convention.gradle.plugin/maven-metadata.xml) | No | Already latest | Baseline passed |
| actions/checkout | v6 | v6 | [Official releases](https://github.com/actions/checkout/releases) | No | Already latest | Existing workflows inspected |
| actions/setup-java | v5 | v5 | [Official releases](https://github.com/actions/setup-java/releases) | No | Already latest | Existing workflows inspected |
| gradle/actions | v6 | v6 | [Official releases](https://github.com/gradle/actions/releases) | No | Already latest; use dependency-submission v6 | Existing workflows inspected |

## Build Configuration Cleanup Research

- `org.jetbrains.kotlin.android` is not applied because AGP 9 provides Built-in Kotlin; its catalog alias is unused and should be removed.
- `androidx.room` is already declared through the catalog and applied by alias; the `pluginManagement.plugins` hardcoded `2.8.4` declaration is a duplicate source and should be removed.
- The `kotlin-metadata-jvm:2.3.21` force is not retained by default. It will be removed before the upgraded build; only a reproduced Hilt compiler failure could justify a minimum coherent replacement.
- Broad iText `-keep`/`-dontwarn` rules predate the Android 9.7.1 distribution. They will be removed first and restored narrowly only if release/R8 produces evidence.
- Current GitHub Actions already use the requested stable major tags; no action version update is required.

## Implementation Verification Log

### Toolchain slice

- Gradle wrapper was regenerated with the 9.6.1 wrapper task; properties, bootstrap JAR, POSIX script, and Windows script all changed together. The resolved wrapper reports Gradle 9.6.1 and bootstrap JAR SHA-256 `497C8C2A7E5031F6AA847F88104AA80A93532EC32EE17BDB8D1D2F67A194A9C7`.
- AGP 9.3.1, Kotlin 2.4.10, KSP 2.3.10, and Hilt 2.60.1 completed repository `check` and `:app:assembleDebug` with exit code 0.
- The build passed after removing the `kotlin-metadata-jvm` resolution force. No metadata incompatibility was reproduced, so retaining or mechanically updating the workaround would be unjustified.
- The Room plugin resolves through the catalog after removing the duplicate `pluginManagement` version, and Built-in Kotlin remains active without the unused Kotlin Android alias.

### iText Android 9.7.1 slice

- Declared coordinate: `com.itextpdf.android:itext-core-android:9.7.1`. `dependencyInsight` and `debugRuntimeClasspath` resolve only the Android family at 9.7.1: `itext-core-android`, `barcodes-android`, `bouncy-castle-connector-android`, `commons-android`, `font-asian-android`, `forms-android`, `hyph-android`, `io-android`, `kernel-android`, `layout-android`, `pdfa-android`, `pdfua-android`, `sign-android`, `styled-xml-parser-android`, and `svg-android`. No 7.2.6 or generic Java Core artifact is present.
- Production source changes are limited to iText 9's documented PDF event migration (`AbstractPdfDocumentEventHandler`, `AbstractPdfDocumentEvent`, and the moved `PdfDocumentEvent`) plus version-neutral comments/imports.
- `:app:assembleDebugAndroidTest` passed. On physical device `22101316G`, `PdfScenarioTest` passed after generating and reopening both portrait and landscape reports through the injected production generator. Assertions covered file/PDF signature/EOF, page count, orientation, title/header, client header, socket/status results table content, signature footer, generation footer, and page number.
- The existing debug UI E2E `PdfExportUiTest` passed via the compiled debug app/test APKs and `AndroidJUnitRunner`, reporting `ASSERTIONS_PASSED` for `historyDetailDialogProducesRetrievableValidPdf`.
- The first UTP install attempt was correctly separated from product results: MIUI returned `INSTALL_FAILED_USER_RESTRICTED` while locked and ran zero tests. Waking/dismissing the keyguard and using standard ADB install/instrumentation succeeded without changing verifier or device policy.
- Removing the old broad iText keep/dontwarn rules exposed one real R8 missing-class error for the optional non-Android `com.itextpdf.bouncycastle.BouncyCastleFactory`. MikLink does not perform cryptographic PDF operations and device generation/reopening passed without that adapter, so one exact `-dontwarn` rule was added. With that minimal rule, both `:app:assembleRelease` and `:app:analyzeReleaseR8Config` passed. No iText keep rule is required.

### Remaining stable libraries

- MockK 1.14.11, Compose BOM 2026.06.01, OkHttp 5.4.0, and Robolectric 4.16.1 were applied. The focused remote/networking/serialization/repository unit selection passed, followed by full `:app:testDebugUnitTest`, repository `check`, and `:app:assembleDebug`.
- OkHttp 5.4.0 required no MikLink source change; existing Retrofit/networking code is source-compatible for the APIs actually used.
- Kotlin 2.4.10 reported `-Xannotation-default-target=param-property` as redundant, so the obsolete compiler flag was removed rather than carried forward.

### Dependency automation behavior

- `.github/dependabot.yml` schedules Gradle at Monday 06:00 and GitHub Actions at 06:15 Europe/Rome. Normal version-update pull requests target `develop`; minor/patch updates are grouped per ecosystem and majors remain individual. No auto-merge or ignore rule is configured.
- Dependabot security updates are distinct from these normal version-update schedules: GitHub creates security-update pull requests for the repository's default branch. The non-default `target-branch: develop` configuration applies to normal version updates, not a promise that security updates will target `develop`.
- `.github/workflows/dependency-submission.yml` submits the resolved Gradle graph on pushes to `master` and `develop` and on manual dispatch, using official stable `checkout@v6`, `setup-java@v5`, and `gradle/actions/dependency-submission@v6`. `contents: write` is confined to this workflow.
- Both files are present on `develop` for branch alignment, but Dependabot is not GitHub-side active until the minimal automation set is placed on the default branch `master`.

## Dependabot Capability Boundary

Dependency submission exposes resolved direct and transitive Gradle dependencies to GitHub's dependency graph and alerts. A security alert for a transitive dependency does not guarantee that Dependabot can create an automatic pull request: version updates operate on dependencies declared in supported manifests, and a vulnerable transitive may require changing an owning direct dependency manually. This distinction is documented here rather than overstating automation coverage.

## Final Dependency Matrix

| Dependency | Before | After | Stable latest | Status | Note |
|------------|--------|-------|---------------|--------|------|
| Gradle Wrapper | 9.5.0 | 9.6.1 | 9.6.1 | UPDATED | Full wrapper task updated properties, JAR, and scripts |
| Android Gradle Plugin | 9.3.0 | 9.3.1 | 9.3.1 | UPDATED | Toolchain build passed |
| Kotlin / Compose compiler | 2.3.21 | 2.4.10 | 2.4.10 | UPDATED | Built-in Kotlin retained; redundant compiler flag removed |
| KSP | 2.3.9 | 2.3.10 | 2.3.10 | UPDATED | Room/Hilt/Moshi generated code passed |
| kotlinx-coroutines | 1.11.0 | 1.11.0 | 1.11.0 | ALREADY_LATEST | BOM/test aligned |
| MockK | 1.14.9 | 1.14.11 | 1.14.11 | UPDATED | Full unit suite passed |
| AndroidX Core KTX | 1.19.0 | 1.19.0 | 1.19.0 | ALREADY_LATEST | No change |
| Lifecycle | 2.11.0 | 2.11.0 | 2.11.0 | ALREADY_LATEST | No change |
| Activity Compose | 1.13.0 | 1.13.0 | 1.13.0 | ALREADY_LATEST | No change |
| Navigation Compose | 2.9.8 | 2.9.8 | 2.9.8 | ALREADY_LATEST | No change |
| Compose BOM | 2026.06.00 | 2026.06.01 | 2026.06.01 | UPDATED | Debug/release/UI compilation passed |
| Hilt | 2.59.2 | 2.60.1 | 2.60.1 | UPDATED | Metadata workaround no longer needed |
| AndroidX Hilt | 1.4.0 | 1.4.0 | 1.4.0 | ALREADY_LATEST | No change |
| Room | 2.8.4 | 2.8.4 | 2.8.4 | ALREADY_LATEST | Duplicate settings plugin version removed |
| Retrofit | 3.0.0 | 3.0.0 | 3.0.0 | ALREADY_LATEST | No change |
| Moshi | 1.15.2 | 1.15.2 | 1.15.2 | ALREADY_LATEST | KSP codegen passed |
| OkHttp | 4.12.0 | 5.4.0 | 5.4.0 | UPDATED | No source changes; focused networking tests passed |
| DataStore | 1.2.1 | 1.2.1 | 1.2.1 | ALREADY_LATEST | No change |
| iText Core | `com.itextpdf:itext7-core:7.2.6` | `com.itextpdf.android:itext-core-android:9.7.1` | 9.7.1 | UPDATED | Android-only resolved graph; device generation/reopening and R8 passed |
| Coil | 3.5.0 | 3.5.0 | 3.5.0 | ALREADY_LATEST | No change |
| JUnit 4 | 4.13.2 | 4.13.2 | 4.13.2 | ALREADY_LATEST | No change |
| AndroidX JUnit | 1.3.0 | 1.3.0 | 1.3.0 | ALREADY_LATEST | No change |
| Espresso | 3.7.0 | 3.7.0 | 3.7.0 | ALREADY_LATEST | No change |
| UIAutomator | 2.4.0 | 2.4.0 | 2.4.0 | ALREADY_LATEST | No change |
| Robolectric | 4.15 | 4.16.1 | 4.16.1 | UPDATED | Full unit suite passed |
| AndroidX Tracing | 1.3.0 | 1.3.0 | 1.3.0 | ALREADY_LATEST | No change |
| AppCompat | 1.7.1 | 1.7.1 | 1.7.1 | ALREADY_LATEST | No change |
| Foojay resolver | 1.0.0 | 1.0.0 | 1.0.0 | ALREADY_LATEST | Required settings plugin remains outside catalog |
| actions/checkout | v6 | v6 | v6 | ALREADY_LATEST | Existing workflows and submission workflow aligned |
| actions/setup-java | v5 | v5 | v5 | ALREADY_LATEST | Existing workflows and submission workflow aligned |
| gradle/actions | v6 | v6 | v6 | ALREADY_LATEST | Dependency submission uses v6 |
| Kotlin Android catalog alias | Present, unused | Removed | N/A | REMOVED | AGP 9 Built-in Kotlin |
| Room hardcoded plugin version | 2.8.4 in settings | Removed | N/A | REMOVED | Catalog is the single source |
| Kotlin metadata force | 2.3.21 | Removed | N/A | REMOVED | Upgraded Hilt/toolchain passes without it |
| Kotlin annotation target flag | Explicit compiler flag | Removed | N/A | REMOVED | Kotlin 2.4 reports it as redundant |

No dependency is `PINNED_COMPATIBILITY`; no latest-stable incompatibility required a version pin.

## Final Verification Matrix

| Command / check | Result | Note |
|-----------------|--------|------|
| `specify version` | NOT_RUN | `specify` executable not installed in local `PATH`; repository Spec Kit skills/scripts used without updating Spec Kit |
| `specify self check` | NOT_RUN | Same local executable limitation; no mutation attempted |
| Baseline `./gradlew check` | PASS | Before build/source upgrades |
| Baseline `./gradlew :app:assembleDebug` | PASS | Before build/source upgrades |
| `./gradlew --version` | PASS | Gradle 9.6.1; project Kotlin plugin is 2.4.10 (Gradle's embedded Kotlin line is separate) |
| `./gradlew clean check` | PASS | Final clean generated-code/lint/unit/repository gate |
| `./gradlew :app:testDebugUnitTest` | PASS | Final full unit suite |
| Focused remote/network/repository unit selection | PASS | OkHttp 5 verification |
| `./gradlew :app:assembleDebug` | PASS | Final debug APK |
| `./gradlew :app:assembleRelease` | PASS | Final minified/resource-shrunk release APK |
| `./gradlew :app:analyzeReleaseR8Config` | PASS | Report generated; one evidence-backed exact optional-factory dontwarn |
| `./gradlew :app:assembleDebugAndroidTest` | PASS | Final instrumentation APK |
| `./gradlew :app:dependencyInsight --dependency com.itextpdf --configuration debugRuntimeClasspath` | PASS | Only `com.itextpdf.android:*:9.7.1` |
| `./gradlew :app:dependencies --configuration debugRuntimeClasspath` | PASS | Full graph inspected |
| Focused `connectedDebugAndroidTest` `PdfScenarioTest` | PASS | Physical device; portrait/landscape structural production path |
| Debug `am instrument` `PdfExportUiTest` | PASS | Physical device; UI → report → production PDF → retrievable file |
| Final full `./gradlew :app:connectedDebugAndroidTest` | NOT_RUN | MIUI rejected final APK install with `INSTALL_FAILED_USER_RESTRICTED`; 0 tests started |
| Full already-installed debug `am instrument` suite | NOT_RUN | Supplementary runner produced no summary/activity for >11 minutes and was stopped; no PASS/FAIL claimed |
| Dependabot/dependency-submission YAML parse | PASS | Both files parsed successfully; `git diff --check` also covers syntax whitespace |

Intermediate diagnostic failures were resolved and are not hidden: test fixture nullability/wrapping assertions were corrected, and the initial release/R8 missing optional Bouncy Castle factory was narrowed to one exact `-dontwarn` rule before the final PASS runs.
