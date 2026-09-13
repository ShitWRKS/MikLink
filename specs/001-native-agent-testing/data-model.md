# Data Model: Native Agent-Driven Testing

These are test/evidence entities. They do not add product database tables or a
production control API.

## TestSession

| Field | Type | Rules |
|---|---|---|
| schemaVersion | string | Required; semantic contract version |
| sessionId | UUID/string | Unique and non-secret |
| startedAt / endedAt | UTC timestamp | End is set exactly once at finalization |
| build | BuildIdentity | APK application ID, version code/name, variant, source revision |
| device | DeviceIdentity | adb serial fingerprint, model, API, authorization state |
| policy | SessionPolicy | Reset and Wi-Fi opt-ins default false |
| prerequisites | list | Evaluated before dependent actions |
| scenarios | list of scenario IDs | Ordered execution catalog |
| artifacts | list of ArtifactReference | Relative paths, type, digest, redaction state |
| cleanup | CleanupResult | Required even after failure |

`adbSerial` may be recorded because it identifies the selected lab target; device
properties that reveal user/person identity are excluded.

Validation rule: an agent-capable `TestSession` requires `build.variant = debug`.
Release manifests may describe only an external black-box smoke session and cannot
contain agent-control actions or enhanced trace artifacts.

## SessionPolicy

- `disposableLocalState`: permits full package-data reset only when true.
- `allowWifiDisruption`: permits connectivity mutation only when true.
- `hostControlRetained`: must be true before Wi-Fi disruption.
- `probeMutationMode`: fixed to `APP_PATH_ONLY` in v1.

Validation: no destructive action is allowed by omission, inheritance, or a generic
“unsafe” flag. Reset and Wi-Fi permissions are independent.

## TestScenario

| Field | Type | Rules |
|---|---|---|
| scenarioId | stable string | Unique catalog key |
| name | string | Human-readable, non-localized result identity |
| featureGroups | list | References `FG-*` inventory entries |
| prerequisites | list | Required vs optional is explicit |
| steps | ordered list | Each has a bound and observable completion |
| cleanupSteps | ordered list | Idempotent and always attempted |

## ScenarioStep

- `stepId`, `kind` (`SETUP`, `ACTION`, `ASSERTION`, `OBSERVATION`, `CLEANUP`)
- `startedAt`, `endedAt`, `status`, and sanitized `detail`
- optional `operationId`/`exchangeId` correlation
- artifact references created at this step

The result writer persists the last completed step after every transition so a crash
does not erase progress context.

## PrerequisiteResult

- `prerequisiteId`, `scope` (`SESSION`, `SCENARIO`, `STEP`)
- `required` boolean
- `status` (`AVAILABLE`, `UNAVAILABLE`, `NOT_APPLICABLE`)
- stable `reasonCode` and sanitized human detail
- `checkedAt` and optional evidence references

Required `UNAVAILABLE` before scenario evaluation maps to `NOT_RUN`. Optional or
not-applicable coverage maps to a `SKIP` step within an otherwise runnable scenario.

## ScenarioResult

- identity/correlation fields and timestamps
- terminal status: `PASS`, `FAIL`, `NOT_RUN`, or `SKIP`
- stable reason code plus sanitized detail
- `lastSuccessfulStepId`
- ordered prerequisite and step results
- crash/ANR observations, artifact references, and cleanup result

### State Transitions

```text
DISCOVERING -> NOT_RUN                    (device/session prerequisite absent)
DISCOVERING -> READY                      (session prerequisites available)
READY -> NOT_RUN                          (required scenario prerequisite absent)
READY -> RUNNING                          (first scenario action starts)
RUNNING -> PASS | FAIL                    (assertions complete or execution fails)
RUNNING -> CLEANING -> PASS | FAIL         (cleanup augments result)
READY -> SKIP                             (scenario intentionally excluded by catalog policy)
```

A failed cleanup cannot convert FAIL to PASS. If assertions pass but mandatory
cleanup/recovery fails, the terminal result is FAIL with a cleanup reason.

## ProbeExchange

| Field | Meaning |
|---|---|
| exchangeId | One request/response correlation |
| sessionId/scenarioId/operationId | Parent correlations |
| request | operation, method/path template, sanitized parameters/body summary |
| response | status/RouterOS outcome, sanitized body summary, duration |
| error | typed transport/protocol error when no normal response |
| processing | parse/normalize/threshold/decision event references |

Raw evidence is permitted only after recursive/value-aware sanitization and size
bounding. Secrets are never placed in the in-memory event to be “cleaned later.”

## TestDataOwnership

- session prefix and created IDs for client, profile, and report records
- creation/deletion timestamps and repository operation outcomes
- no probe credential value and no exported backup payload
- cleanup may retry safely; “already absent” is success

## ArtifactReference

- relative filename, media type, byte size, SHA-256 digest
- producer step/scenario and timestamp
- `redactionStatus`: `NOT_REQUIRED`, `SANITIZED`, or `VERIFIED_SCAN`
- title/category: manifest, result, trace, screenshot, hierarchy, log excerpt, PDF
