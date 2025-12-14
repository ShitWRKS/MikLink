# Refactor — Obiettivi e confini

Questa pagina serve per mantenere allineati “noi” e gli agent durante il refactor.

## Obiettivo

Rilasciare una versione “pulita” e manutenibile, con:

- layering SOLID/clean rispettato (vedi `architecture.md`)
- single probe (nessun `probeId`)
- DB rebase distruttivo (Room v1 baseline)
- suite test anti-regressione (golden parsing + quality)
- PDF export basato su risultati normalizzati di dominio
- compatibilità contesto “cantiere”:
  - HTTP o HTTPS (trust-all consapevole solo su HTTPS)

## Scope incluso

- CRUD clienti / profili / sonda / report
- Esecuzione test
- Storico + dettaglio report
- Export PDF
- Backup import/export (manteniamo feature, ma rispettando i layer)
- Socket ID **Lite**:
  - `prefix + counter + suffix`
  - separatore + padding
  - incremento `nextIdNumber` **solo su SUCCESS**

## Fuori scope

- Logs: rimossi (vedi ADR-0005)
- Socket-ID “full” (campi/ordine arbitrari, auto-increment multipli, ecc.) → dopo stabilizzazione
- Analytics strutturati sui report (split resultsJson) → epic futura dedicata
