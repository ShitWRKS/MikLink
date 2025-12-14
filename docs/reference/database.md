# Database (baseline)

<<<<<<< HEAD
Questa pagina descrive lo **schema target** dopo il rebase distruttivo.

## Decisioni chiave

- Nome file DB: `miklink`
- Room schemaVersion: `1`
- Nessuna migrazione: se lo schema cambia durante lo sviluppo, si accetta reset (vedi ADR-0003).
- **Single probe**: la configurazione sonda è unica e non espone `probeId`.
- `Report.resultsJson` resta una **singola colonna JSON**.

## Tabelle

### `clients`
=======
## Stato attuale (baseline Room v1)

- Database: `com.app.miklink.data.local.room.MikLinkDatabase`
- Nome file DB: `miklink`
- Versione Room: `1`
- Schema versionato: `app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/1.json`

La posizione dello schema resta definita nei Gradle args:

- `room.schemaLocation = app/schemas` (annotation processor/KSP)

> Nota 2025-12-13: tutte le cartelle legacy `AppDatabase` sono state rimosse dal repo; la sola fonte di verità è MikLinkDatabase.
>>>>>>> aec31fe18138fb571fc1c1b9dd890bac55425d41

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

<<<<<<< HEAD
- `tdrSupported` è una cache per UI/query: la decisione di “supportato/non supportato” è nel domain (`TdrCapabilities`).
- Se in futuro serviranno analytics su metriche dei report, si farà una epic dedicata “Report Analytics” (no in baseline).
=======
## Guard rails

- Task Gradle `guardLegacySchemas` (root build) fallisce se nel repo ricompaiono i vecchi identificatori Room v1 o l'AppDatabase legacy del modulo data.
- CI/local scripts devono invocare `./gradlew guardLegacySchemas` quando si toccano schemi Room.

## Rebaseline DB (decisione di progetto)

Il progetto è in sviluppo e **non ci sono dati da preservare**: è accettabile rifare il DB “da zero” per:
- rimuovere campi legacy (`probeId`, `lastFloor`, `lastRoom`, ecc.)
- riallineare i layer (Room come infrastruttura in `data/**`)
- semplificare migrazioni (ripartire da versione 1)

**Importante:** prima di implementare la rebase dobbiamo fissare:
1) quali dati devono esistere in v1 (Client / Profile / Probe config / Report / altro)
2) quali relazioni e indici servono (query reali)
3) quali campi sono “fonte di verità” vs “cache”

Le discrepanze e scelte vanno tracciate in ADR e in `docs/DISCREPANCIES.md`.
>>>>>>> aec31fe18138fb571fc1c1b9dd890bac55425d41
