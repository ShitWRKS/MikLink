# Database (Room)

## Source of truth

- **Versione corrente**: definita in `@Database(version = X)` in `MikLinkDatabase.kt`
- **Schema snapshot**: `app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/<version>.json`
- Il file JSON dell'ultima versione rappresenta lo schema corrente

## Policy pre-production

Gli aggiornamenti di schema usano migrazioni Room esplicite e non distruttive registrate in `DatabaseMigrations.kt` (vedi **ADR-0014**).

## Atomicità report + contatore (ADR-0010/ADR-0013)

L'inserimento di un `TestReport` e l'incremento di `nextIdNumber` del client avvengono nella
**stessa transazione** Room (via `TransactionRunner`, porta in `core/data`, implementazione in `data`).

- `SaveTestReportUseCase` avvolge inserimento report + incremento quando `incrementClientCounter = true`.
- Se `incrementClientCounter = false`: salva solo il report (nessun incremento).
- Se `clientId` è `null`: report orfano (nessun incremento).
- Se è richiesto l'incremento e il client non esiste: **fallisci e rollback** (no read-copy-update).

Query atomica di incremento (restituisce il numero di righe aggiornate):

```sql
UPDATE clients
SET nextIdNumber = nextIdNumber + 1
WHERE clientId = :clientId
```

`0` righe aggiornate ⇒ client inesistente ⇒ rollback della transazione.

## Invarianti

- `probe_config` è **singleton**: PK fissa `id = 1`
- Le tabelle `clients`, `test_profiles`, `test_reports` usano ID autogenerati

## Tabella `clients`

| Colonna | Tipo | Nullabilità | Key | Default |
|---|---|---|---|---|
| `clientId` | INTEGER | NOT NULL | PK |  |
| `companyName` | TEXT | NOT NULL |  |  |
| `location` | TEXT | NULL |  |  |
| `notes` | TEXT | NULL |  |  |
| `networkMode` | TEXT | NOT NULL |  |  |
| `staticIp` | TEXT | NULL |  |  |
| `staticSubnet` | TEXT | NULL |  |  |
| `staticGateway` | TEXT | NULL |  |  |
| `staticCidr` | TEXT | NULL |  |  |
| `minLinkRate` | TEXT | NOT NULL |  |  |
| `socketPrefix` | TEXT | NOT NULL |  |  |
| `socketSuffix` | TEXT | NOT NULL |  |  |
| `socketSeparator` | TEXT | NOT NULL |  |  |
| `socketNumberPadding` | INTEGER | NOT NULL |  |  |
| `nextIdNumber` | INTEGER | NOT NULL |  |  |
| `speedTestServerAddress` | TEXT | NULL |  |  |
| `speedTestServerUser` | TEXT | NULL |  |  |
| `speedTestServerPassword` | TEXT | NULL |  |  |


### Indici
- `index_clients_companyName` su (companyName)

## Tabella `probe_config`

| Colonna | Tipo | Nullabilità | Key | Default |
|---|---|---|---|---|
| `id` | INTEGER | NOT NULL | PK |  |
| `ipAddress` | TEXT | NOT NULL |  |  |
| `username` | TEXT | NOT NULL |  |  |
| `password` | TEXT | NOT NULL |  |  |
| `testInterface` | TEXT | NOT NULL |  |  |
| `isHttps` | INTEGER | NOT NULL |  |  |
| `isOnline` | INTEGER | NOT NULL |  |  |
| `modelName` | TEXT | NULL |  |  |
| `tdrSupported` | INTEGER | NOT NULL |  |  |

## Tabella `test_profiles`

| Colonna | Tipo | Nullabilità | Key | Default |
|---|---|---|---|---|
| `profileId` | INTEGER | NOT NULL | PK |  |
| `profileName` | TEXT | NOT NULL |  |  |
| `profileDescription` | TEXT | NULL |  |  |
| `runTdr` | INTEGER | NOT NULL |  |  |
| `runLinkStatus` | INTEGER | NOT NULL |  |  |
| `runLldp` | INTEGER | NOT NULL |  |  |
| `runPing` | INTEGER | NOT NULL |  |  |
| `pingTarget1` | TEXT | NULL |  |  |
| `pingTarget2` | TEXT | NULL |  |  |
| `pingTarget3` | TEXT | NULL |  |  |
| `pingCount` | INTEGER | NOT NULL |  |  |
| `runSpeedTest` | INTEGER | NOT NULL |  |  |

## Tabella `test_reports`

| Colonna | Tipo | Nullabilità | Key | Default |
|---|---|---|---|---|
| `reportId` | INTEGER | NOT NULL | PK |  |
| `clientId` | INTEGER | NULL |  |  |
| `timestamp` | INTEGER | NOT NULL |  |  |
| `socketName` | TEXT | NULL |  |  |
| `notes` | TEXT | NULL |  |  |
| `probeName` | TEXT | NULL |  |  |
| `profileName` | TEXT | NULL |  |  |
| `overallStatus` | TEXT | NOT NULL |  |  |
| `resultFormatVersion` | INTEGER | NOT NULL |  |  |
| `resultsJson` | TEXT | NOT NULL |  |  |


### Indici
- `index_test_reports_clientId` su (clientId)
- `index_test_reports_timestamp` su (timestamp)
- `index_test_reports_clientId_timestamp` su (clientId, timestamp)

