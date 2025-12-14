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
