# Database (baseline — Room v1)

Questa pagina descrive lo **stato attuale “as-is”** del database, allineato a:

- Decisioni chiuse (ADR / Context Pack)
- Room schema export: `app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/1.json`

## Metadati

- Room database: `com.app.miklink.data.local.room.MikLinkDatabase`
- Nome file DB: `miklink`
- Room schemaVersion: `1`
- Policy refactor DB: **rebase distruttivo** (reset accettato durante sviluppo; vedi ADR-0003)

## Single probe (probe_config)

La configurazione sonda è **una sola**.

- Tabella: `probe_config`
- PK interna: `id` (INTEGER, **non autogenerata**)
- Convenzione runtime: record singleton con `id = 1`
  - In DAO sono usate query `WHERE id = 1`
- Nota: `id` è tecnica **interna Room** e non deve diventare un concetto pubblico (UI/domain/backup).

### `probe_config` (schema)

PK: `id` (autoGenerate = false)

| Column | Type | Nullable | Default |
|---|---|---|---|
| `id` | INTEGER | NO |  |
| `ipAddress` | TEXT | NO |  |
| `username` | TEXT | NO |  |
| `password` | TEXT | NO |  |
| `testInterface` | TEXT | NO |  |
| `isHttps` | INTEGER | NO |  |
| `isOnline` | INTEGER | NO |  |
| `modelName` | TEXT | YES |  |
| `tdrSupported` | INTEGER | NO |  |

## Tabelle

### `clients`

PK: `clientId` (autoGenerate = true)  
Index: `index_clients_companyName`

| Column | Type | Nullable | Default |
|---|---|---|---|
| `clientId` | INTEGER | NO |  |
| `companyName` | TEXT | NO |  |
| `location` | TEXT | YES |  |
| `notes` | TEXT | YES |  |
| `networkMode` | TEXT | NO |  |
| `staticIp` | TEXT | YES |  |
| `staticSubnet` | TEXT | YES |  |
| `staticGateway` | TEXT | YES |  |
| `staticCidr` | TEXT | YES |  |
| `minLinkRate` | TEXT | NO |  |
| `socketPrefix` | TEXT | NO |  |
| `socketSuffix` | TEXT | NO |  |
| `socketSeparator` | TEXT | NO |  |
| `socketNumberPadding` | INTEGER | NO |  |
| `nextIdNumber` | INTEGER | NO |  |
| `speedTestServerAddress` | TEXT | YES |  |
| `speedTestServerUser` | TEXT | YES |  |
| `speedTestServerPassword` | TEXT | YES |  |

### `test_profiles`

PK: `profileId` (autoGenerate = true)

| Column | Type | Nullable | Default |
|---|---|---|---|
| `profileId` | INTEGER | NO |  |
| `profileName` | TEXT | NO |  |
| `profileDescription` | TEXT | YES |  |
| `runTdr` | INTEGER | NO |  |
| `runLinkStatus` | INTEGER | NO |  |
| `runLldp` | INTEGER | NO |  |
| `runPing` | INTEGER | NO |  |
| `pingTarget1` | TEXT | YES |  |
| `pingTarget2` | TEXT | YES |  |
| `pingTarget3` | TEXT | YES |  |
| `pingCount` | INTEGER | NO |  |
| `runSpeedTest` | INTEGER | NO |  |

### `test_reports`

PK: `reportId` (autoGenerate = true)  
Indici:
- `index_test_reports_clientId`
- `index_test_reports_timestamp`
- `index_test_reports_clientId_timestamp`

Note:
- `clientId` è presente ma **non** è definito come foreign key nello schema export.
- `resultsJson` resta una **singola colonna JSON** (TEXT).
- `resultFormatVersion` esiste come INTEGER per versionare il formato del JSON.

| Column | Type | Nullable | Default |
|---|---|---|---|
| `reportId` | INTEGER | NO |  |
| `clientId` | INTEGER | YES |  |
| `timestamp` | INTEGER | NO |  |
| `socketName` | TEXT | YES |  |
| `notes` | TEXT | YES |  |
| `probeName` | TEXT | YES |  |
| `profileName` | TEXT | YES |  |
| `overallStatus` | TEXT | NO |  |
| `resultFormatVersion` | INTEGER | NO |  |
| `resultsJson` | TEXT | NO |  |
