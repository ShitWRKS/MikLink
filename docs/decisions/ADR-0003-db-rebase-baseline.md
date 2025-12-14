# ADR-0003 — DB baseline (Room v1) e schema come fonte di verità

- **Status:** Accepted
- **Data:** 2025-12-14

## Contesto

La codebase non deve portarsi dietro “storia” di migrazioni o namespace (v1/v2/…) che creano attrito durante il refactor.
L'obiettivo è una base semplice e verificabile.

## Decisione

- Room parte da **versione 1** (`@Database(version = 1, exportSchema = true)`).
- Lo schema esportato in `app/schemas/**` è la **fonte di verità** per la reference DB.
- Nome database file: **`miklink`** (vedi `DatabaseModule`).
- La configurazione sonda è singleton:
  - tabella `probe_config` con PK fissa `id = 1`
  - nessun `probeId` nel dominio (ADR-0001)

## Conseguenze

- Se cambiamo lo schema, aggiorniamo:
  - schema export
  - `docs/reference/database.md`
  - test che assumono shape dei dati (se presenti)

- Migrazioni “storiche” non fanno parte dello scope corrente: se verranno introdotte, richiedono ADR dedicato.
