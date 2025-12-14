# ADR-0003 — Rebase DB (baseline) + probe singleton interno

- **Status:** Accepted
- **Data:** 2025-12-14

## Contesto

La codebase ha ereditato naming e migrazioni storiche (v1/v2, migrations 7..13, ecc.) e non ci sono installazioni attive da preservare.

Il refactor punta a semplificare: database “baseline” e single probe.

## Decisione

- Il database viene ricreato in modo **distruttivo**:
  - Room `schemaVersion = 1`
  - Nome file DB: `miklink`
  - Nessuna migrazione “storica”: si accetta reset durante sviluppo.
- La configurazione sonda è **una sola** e non espone `probeId`.
  - Per vincoli tecnici Room esiste una PK **interna** (es. `singletonKey=1`), mai esposta fuori dal layer DB.
  - I repository espongono solo:
    - `getProbeConfig(): ProbeConfig?`
    - `saveProbeConfig(config: ProbeConfig)`

## Conseguenze

- File/namespace `v1/v2` e migrazioni diventano legacy e vanno rimossi.
- I test di migrazione DB diventano irrilevanti dopo la rebase.
- Il domain e la UI non devono più avere riferimenti a id di probe.
