# Docs

Documentazione “docs-as-code” mantenuta insieme al codice.

## Struttura (Diátaxis)

- `explanation/` — spiegazioni e architettura (perché è fatto così)
- `reference/` — reference tecnica (API, DB, build, formati)
- `decisions/` — ADR (decisioni architetturali “congelate”)
- `contributing/` — regole per contribuire alla documentazione

## Regole

- La **fonte di verità** è il codice: la doc deve linkare i file e non duplicare dettagli inutili.
- Ogni decisione non ovvia → ADR.
- Se una modifica rompe un'invariante, aggiorna prima ADR/architecture e poi il codice (o viceversa, ma nello stesso PR).

## Indice rapido

- Architettura: `explanation/architecture.md`
- Build: `reference/build.md`
- Database: `reference/database.md`
- MikroTik REST API: `reference/mikrotik-rest-api.md`
- Testing: `reference/testing.md`
- Backup format: `reference/backup-format.md`
- UI theme: `reference/ui-theme.md`

## Decisioni (ADR)

- [ADR-0001 — Single probe](decisions/ADR-0001-single-probe.md)
- [ADR-0002 — HTTP/HTTPS toggle con trust-all](decisions/ADR-0002-https-toggle-trust-all.md)
- [ADR-0003 — DB rebase baseline](decisions/ADR-0003-db-rebase-baseline.md)
- [ADR-0004 — Socket ID lite](decisions/ADR-0004-socket-id-lite.md)
- [ADR-0005 — Remove logs](decisions/ADR-0005-remove-logs.md)
- [ADR-0006 — Backup: stable clientKey e report orfani](decisions/ADR-0006-backup-stable-client-key.md)
- [ADR-0007 — Package structure and naming](decisions/ADR-0007-package-structure-and-naming.md)
- [ADR-0008 — No DTO leaks across ports](decisions/ADR-0008-no-dto-leaks-across-ports.md)
- [ADR-0009 — Reintroduce test execution logs](decisions/ADR-0009-reintroduce-test-execution-logs.md)
- [ADR-0010 — Socket ID increment on every save](decisions/ADR-0010-socket-id-increment-on-save.md)
- [ADR-0011 — Typed test execution contract and unified renderer](decisions/ADR-0011-typed-test-execution-contract.md)
- [ADR-0012 — Destructive migrations in pre-production](decisions/ADR-0012-room-destructive-migrations-preprod.md)
- [ADR-0013 — Runtime stability, RouterOS boundary and verifiable baseline](decisions/ADR-0013-runtime-stability-routeros-boundary-and-verifiable-baseline.md)

## Struttura progetto e naming

- Struttura cartelle/package + convenzioni: `reference/project-structure.md`
- Decisioni correlate: `decisions/ADR-0007-package-structure-and-naming.md`, `decisions/ADR-0008-no-dto-leaks-across-ports.md`
