# ADR-0005 — Rimozione Logs dallo scope

- **Status:** Accepted
- **Data:** 2025-12-14

## Contesto

La feature Logs introduce complessità (UI, storage, policy) e non è prioritaria per il refactor “v1 pulita”.

## Decisione

- La sezione Logs viene rimossa dal prodotto durante il refactor.
- Qualsiasi codice, screen, navigation route o entity “logs” può essere eliminata.

## Conseguenze

- Riduzione scope e rischio drift.
- Se in futuro servirà, verrà reintrodotta con epic/ADR dedicati.
