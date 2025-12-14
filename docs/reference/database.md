# Database (baseline)

Questa pagina descrive lo **schema target** dopo il rebase distruttivo.

## Decisioni chiave

- Nome file DB: `miklink`
- Room schemaVersion: `1`
- Nessuna migrazione: se lo schema cambia durante lo sviluppo, si accetta reset (vedi ADR-0003).
- **Single probe**: la configurazione sonda è unica e non espone `probeId`.
- `Report.resultsJson` resta una **singola colonna JSON**.

## Tabelle

### `clients`

- `clientId` (PK, Long, autoincrement)
- `companyName` (String, required)
- `location` (String?, default "Sede")
- `notes` (String?)
- `networkMode` (Enum/String: DHCP | Static)
- `staticIp` (String?)
- `staticSubnet` (String?)  *(se mantenuta in lite; valutabile deprecazione futura)*
- `staticGateway` (String?)
- `staticCidr` (String?) *(preferita quando disponibile)*
- `minLinkRate` (String) *(threshold PASS: "10M","100M","1G","10G")*
- **Socket-ID Lite**
  - `socketPrefix` (String)
  - `socketSeparator` (String)
  - `socketNumberPadding` (Int)
  - `socketSuffix` (String)
  - `nextIdNumber` (Int)
- Speed test (opzionale)
  - `speedTestServerAddress` (String?)
  - `speedTestServerUser` (String?)
  - `speedTestServerPassword` (String?)

**Rimossi:** `lastFloor`, `lastRoom` (legacy).

### `probe_config` (singleton interno)

La UI e il domain trattano la probe config come **unica**.

Per vincoli tecnici Room, esiste una PK interna **non esposta**:

- `singletonKey` (Int, PK) → valore fisso `1`
- `ipAddress` (String)
- `username` (String)
- `password` (String)
- `testInterface` (String)
- `isOnline` (Boolean)
- `modelName` (String?)
- `tdrSupported` (Boolean) *(cache; fonte di verità = domain TdrCapabilities)*
- `isHttps` (Boolean)

### `test_profiles`

- `profileId` (PK, Long, autoincrement)
- `profileName` (String)
- `profileDescription` (String?)
- `runTdr` (Boolean)
- `runLinkStatus` (Boolean)
- `runLldp` (Boolean)
- `runPing` (Boolean)
- `pingTarget1`/`2`/`3` (String?)
- `pingCount` (Int, default 4)
- `runSpeedTest` (Boolean)

### `test_reports`

- `reportId` (PK, Long, autoincrement)
- `clientId` (Long?, FK opzionale)
- `timestamp` (Long)
- `socketName` (String?)
- `notes` (String?)
- `probeName` (String?)
- `profileName` (String?)
- `overallStatus` (String)
- `resultsJson` (String)  ✅ **singola colonna JSON**

## Note operative

- `tdrSupported` è una cache per UI/query: la decisione di “supportato/non supportato” è nel domain (`TdrCapabilities`).
- Se in futuro serviranno analytics su metriche dei report, si farà una epic dedicata “Report Analytics” (no in baseline).
