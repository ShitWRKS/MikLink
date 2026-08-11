# Data Model: Test Profile Tabbed UX

## Existing persisted model (unchanged)

The feature reorganizes how an existing `TestProfile` is edited. It adds no persisted entity, relationship, column, serialized field, or state transition.

### TestProfile

- Identity and description: `profileId`, `profileName`, optional `profileDescription`
- Existing test toggles: `runTdr`, `runLinkStatus`, `runLldp`, `runPing`, `runSpeedTest`
- Existing Ping execution values: optional `pingTarget1`, `pingTarget2`, `pingTarget3`, and `pingCount`
- Existing thresholds: `TestThresholds`

### TestThresholds

- Link: optional `linkMinRate`
- TDR internal configuration: `tdrFailStatuses` (not exposed as a new editor)
- Local Ping: `maxLossPercent`, `maxAvgRttMs`, `maxRttMs`
- External Ping: `maxLossPercent`, `maxAvgRttMs`, `maxRttMs`
- Gateway: existing `GatewayUnresolvedPolicy`
- Speed: `maxPingMs`, `maxJitterMs`, `maxLossPercent`, `minDownloadMbps`, `minUploadMbps`

## Transient UI state

### Selected profile tab

- Values: General, Link, Ping, Speed test
- Lifetime: current edit-screen instance and platform-restorable UI state
- Persistence: saveable UI state only; never written to `TestProfile` or Room

### Optional target visibility

- Values: whether target 2 and target 3 editors are currently shown
- Initial derivation: corresponding ViewModel target value is non-blank
- Lifetime: current edit session, saveable across configuration recreation
- Persistence: visibility itself is not profile data; target strings retain existing persistence behavior

### Threshold previews

- Inputs: current string fields plus effective defaults already owned by `TestProfileViewModel`
- Derived values: parsed numeric/rate values, drawing ranges, normalized positions, deterministic illustrative samples
- Invalid state: affected drawing element omitted; no substitute value invented
- Persistence: none; no preview series or geometry enters domain, database, backup, report, or execution data

## Existing validation (unchanged)

- Non-blank profile name
- At least one test enabled
- Link rate accepted only by `StrictLinkRateParser`
- Percentages in 0 through 100
- Numeric threshold metrics finite and non-negative
- Ping count in 1 through 20 when Ping is enabled
- Non-blank Ping targets accepted only by `NetworkValidator` when Ping is enabled
- Blank thresholds resolve to existing defaults during save

## Mapping by tab

| Tab | Existing state presented |
|-----|--------------------------|
| General | `profileName`, `profileDescription` |
| Link | `runLinkStatus`, `linkMinRate`, `runTdr`, `runLldp` |
| Ping | `runPing`, targets, `pingCount`, `gatewayPolicy`, local/external Ping thresholds |
| Speed test | `runSpeedTest`, five Speed thresholds |
