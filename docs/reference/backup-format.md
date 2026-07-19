# Backup format (JSON)

Fonte: `data/repository/BackupData.kt` + mapping in `data/repository/BackupManagerImpl`.

## Struttura

Il JSON serializza `BackupData`:

- `version` (Int) — **2** (dal baseline ADR-0013; v1 ancora importabile)
- `probe` (ProbeConfig?) — nullable (la probe è singleton; se null l'import mantiene quella esistente)
- `clients` (List<BackupClient>)
- `profiles` (List<TestProfile>)
- `reports` (List<BackupReport>)

## clientRef (v2) / clientKey (v1)

Per evitare di esportare ID DB (`clientId`), il backup usa un riferimento stabile:

- **v2**: `BackupClient.clientRef: String` (sempre presente), `BackupReport.clientRef: String?` (nullable)
- **v1** (legacy): `BackupClient.clientKey: String` (sempre presente), `BackupReport.clientKey: String?` (nullable)

`clientRef` è un riferimento opaco unico per il file; **non** aggiunge UUID al database.

### Generazione (v2 export)

In export (`BackupManagerImpl`):

- `companyName` → trim + lowercase
- `location` → trim + lowercase + whitespace → `_`
- `clientRef = "<company>|<location>"`  
  (se `location` è vuota, la parte dopo `|` è vuota)

### Uso in export

- Si costruisce una mappa `clientId -> clientRef`
- Ogni report esporta `clientRef = report.clientId?.let { clientIdToRef[it] }`

Quindi i report “orfani” (senza `clientId`) vengono esportati con `clientRef = null`.

## Import

In import:

1) Si inserisce il formato in base a `version`:
   - **version 2**: usa `clientRef`;
   - **version 1**: usa `clientKey` (legacy);
   - versioni diverse da 1 e 2: **rifiuto**.
2) Per v2: `clientRef` duplicati → **rifiuto**; report con `clientRef` inesistente → **rifiuto**.
3) Per v1: `clientKey` duplicati → **rifiuto esplicito**.
4) Per ogni report:
   - `clientId = ref?.let { refToNewId[it] }`
   - se `ref` è null (o non trovata) il report viene importato con `clientId = null` (orfano).
5) Credenziali preservate.

## Compatibilità

- Il formato è estendibile tramite `version`.
- v2 introduce `clientRef` come riferimento primario; v1 mantenuto per retrocompatibilità.
- Report orfano: riferimento `null` (preservato in entrambe le versioni).
