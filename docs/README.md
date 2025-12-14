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

## Struttura progetto e naming

- Struttura cartelle/package + convenzioni: `reference/project-structure.md`
- Decisioni correlate: `decisions/ADR-0007-package-structure-and-naming.md`, `decisions/ADR-0008-no-dto-leaks-across-ports.md`
