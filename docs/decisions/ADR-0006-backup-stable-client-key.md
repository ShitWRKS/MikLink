# ADR-0006 — Backup: stable clientKey e report orfani

- **Status:** Accepted
- **Data:** 2025-12-14

## Contesto

Nel DB i report referenziano i clienti tramite `clientId` (PK). In backup/export non vogliamo esportare ID interni perché:
- non sono stabili tra installazioni
- in import vanno rimappati

Inoltre possono esistere report “orfani” (senza client associato).

## Decisione

- `BackupClient` contiene `clientKey: String` (stable key calcolata in export).
- `BackupReport` contiene `clientKey: String?` (nullable).
  - se il report non ha client, `clientKey = null`
  - in import il report resta senza `clientId`

## Conseguenze

- Import/export resta robusto verso dati incompleti.
- La UI (quando serve) deve gestire report senza client associato.
- Se in futuro si vuole rendere `clientKey` obbligatorio sui report, serve bump `BackupData.version` + strategia per gli orfani.

## Aggiornamento (backup v2 — ADR-0013)

`BackupData.version` passa a **2**. Vedi `reference/backup-format.md` per i dettagli completi.

- `BackupClient.clientRef: String` — riferimento opaco unico per il file (sostituisce `clientKey` come
  identificatore primario di riferimento interno al backup). Non aggiunge UUID al database.
- `BackupReport.clientRef: String?` — nullable; i report orfani hanno `clientRef = null`.

### Compatibilità

- **version 2**: usa `clientRef` per il collegamento report→client.
- **version 1**: usa `clientKey` (legacy), mantenuto per retrocompatibilità in import.
- versioni diverse da 1 e 2: **rifiuto** dell'import.
- `clientRef` duplicati nel v2: **rifiuto** (collisione).
- report con `clientRef` inesistente nel v2: **rifiuto**.
- `clientKey` duplicati nel v1: **rifiuto esplicito**.
- report orfano: riferimento `null` (preservato).
- credenziali preservate.
