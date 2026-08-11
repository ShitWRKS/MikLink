# Data Model: Dependency Modernization

This feature changes build metadata and generated PDF artifacts; it introduces no production database entity or migration.

## Dependency Record

| Field | Type | Validation |
|-------|------|------------|
| name | Text | Unique dependency/plugin/tool family |
| currentVersion | Fixed version | Must match pre-change active declaration |
| latestStableVersion | Fixed version | Must come from a primary source and contain no preview/dynamic qualifier |
| selectedVersion | Fixed version or removed | Equals latest stable unless compatibility evidence supports a lower stable |
| source | URL/reference | Official documentation, repository/release, Maven Central, Google Maven, or Plugin Portal |
| breakingChange | Boolean + note | Limited to changes relevant to APIs/configuration MikLink uses |
| status | Enum | `UPDATED`, `ALREADY_LATEST`, `PINNED_COMPATIBILITY`, or `REMOVED` |
| verification | Text | Command/test result proving the selected state |

### State transitions

```text
DISCOVERED → AUDITED → SELECTED → APPLIED → VERIFIED
                      ↘ INCOMPATIBLE_REPRODUCED → PINNED_COMPATIBILITY → VERIFIED
                      ↘ REDUNDANT → REMOVED → VERIFIED
```

No record may reach `PINNED_COMPATIBILITY` from a generic build failure; the causal incompatibility and newest compatible stable version must be established.

## PDF Verification Artifact

| Field | Type | Validation |
|-------|------|------------|
| scenarioId | Text | Correlates generation, assertions, and cleanup |
| orientation | Enum | Portrait or landscape |
| sourcePath | Production chain | Report data → `PdfGenerator` → bound iText implementation → helper → cache file |
| file | File | Exists, `.pdf`, non-empty, PDF signature present |
| pageCount | Integer | At least 1 |
| pageGeometry | Width/height | Width < height for portrait; width > height for landscape |
| extractedText | Text | Contains title/header, primary row data/results table, footer/timestamp, and page number |
| cleanup | Result | Generated file removed by scenario cleanup |

### State transitions

```text
REPORT_READY → GENERATED → REOPENED → STRUCTURE_VERIFIED → CONTENT_VERIFIED → CLEANED
                   ↘ FAILED (with correlated evidence and cleanup attempt)
```

## Automation Boundary

| Field | Value / validation |
|-------|--------------------|
| develop change set | All specification, version, wrapper, source, test, cleanup, CI, and automation files |
| master activation set | Only `.github/dependabot.yml` and `.github/workflows/dependency-submission.yml` |
| normal update target | `develop` |
| updater ecosystems | `gradle`, `github-actions` |
| merge behavior | No auto-merge |
| GitHub activation | Pending until minimal activation set exists on default branch |
