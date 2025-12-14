# Backup format (JSON)

Fonte: `data/repository/BackupData.kt` + mapping in `data/repository/BackupManagerImpl`.

## Struttura

Il JSON serializza `BackupData`:

- `version` (Int) — attualmente **1**
- `probe` (ProbeConfig?) — nullable (la probe è singleton; se null l'import mantiene quella esistente)
- `clients` (List<BackupClient>)
- `profiles` (List<TestProfile>)
- `reports` (List<BackupReport>)

## clientKey (stable key)

Per evitare di esportare ID DB (`clientId`), il backup usa una chiave stabile:

- `BackupClient.clientKey: String` (**sempre presente**)
- `BackupReport.clientKey: String?` (**nullable**)

### Generazione

In export (`BackupManagerImpl`):

- `companyName` → trim + lowercase
- `location` → trim + lowercase + whitespace → `_`
- `clientKey = "<company>|<location>"`  
  (se `location` è vuota, la parte dopo `|` è vuota)

### Uso in export

- Si costruisce una mappa `clientId -> clientKey`
- Ogni report esporta `clientKey = report.clientId?.let { clientIdToKey[it] }`

Quindi i report “orfani” (senza `clientId`) vengono esportati con `clientKey = null`.

## Import

In import:

1) Si inseriscono tutti i clienti e si costruisce `clientKey -> newClientId`
2) Per ogni report:
   - `clientId = r.clientKey?.let { clientKeyToNewId[it] }`
   - se `clientKey` è null (o non trovata) il report viene importato con `clientId = null`

## Compatibilità

- Il formato è pensato per essere estendibile: usa `version`.
- Se in futuro rendi `clientKey` obbligatorio sui report, serve:
  - migrazione del formato (bump `version`)
  - strategia per report orfani (es. client “UNKNOWN” o drop controllato)
