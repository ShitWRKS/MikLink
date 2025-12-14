# MikLink — Documentazione

Questa cartella è la **source of truth** per:

- architettura target (SOLID / clean)
- decisioni non negoziabili (ADR)
- riferimento tecnico (DB, dipendenze, test, fixtures)
- discrepanze misurate tra docs e codice

> Regola: se una decisione cambia, si crea/aggiorna un **ADR**.  
> Regola: `DISCREPANCIES.md` contiene **solo evidenze**, non proposte.

## Indice

### Decisioni (ADR)
- [ADR-0001 — Sonda unica (rimozione probeId)](decisions/ADR-0001-single-probe.md)
- [ADR-0002 — Toggle HTTPS + trust-all consapevole](decisions/ADR-0002-https-toggle-trust-all.md)
- [ADR-0003 — Rebase DB (baseline) + probe singleton interno](decisions/ADR-0003-db-rebase-baseline.md)
- [ADR-0004 — Socket ID “Lite”](decisions/ADR-0004-socket-id-lite.md)
- [ADR-0005 — Rimozione Logs dallo scope](decisions/ADR-0005-remove-logs.md)

### Spiegazioni
- [Architettura](explanation/architecture.md)
- [V1 — obiettivi e confini](explanation/v1-scope.md)

### Riferimenti
- [Project structure (canone)](reference/project-structure.md)
- [Database (baseline)](reference/database.md)
- [Dipendenze](reference/dependencies.md)
- [Testing](reference/testing.md)
- [Fixtures](reference/fixtures.md)
- [MikroTik REST API](reference/mikrotik-rest-api.md)

### Operativo
- [Discrepanze (docs vs codice)](DISCREPANCIES.md)
- [How-to: aggiungere un golden test](howto/add-golden-test.md)
