# ADR-0014 — Migrazioni Room esplicite in produzione

- **Status:** Accepted
- **Data:** 2026-08-02

## Decisione

Ogni aggiornamento dello schema Room deve includere migrazioni esplicite, non distruttive e testate con gli schema esportati. Il builder registra `ALL_MIGRATIONS`; i fallback distruttivi sono vietati nel codice principale.
