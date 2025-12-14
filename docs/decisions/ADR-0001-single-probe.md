# ADR-0001 — Single probe (nessun probeId)

- **Status:** Accepted
- **Data:** 2025-12-13

## Contesto

L'app gestisce una sola sonda MikroTik configurabile dall'utente. Introdurre un `probeId` porterebbe complessità (UI, DB, migrazioni, logica multi‑probe) non richiesta dal prodotto.

## Decisione

- `ProbeConfig` non espone alcun id nel dominio.
- Persistenza come singleton in DB: `probe_config.id = 1`.

## Conseguenze

- I repository/DAO lavorano con un singolo record.
- Import backup può mantenere la probe esistente quando il backup non la include.
