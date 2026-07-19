<!--
Purpose: Update the socket-id increment rule to advance on every saved test (PASS or FAIL).
Inputs: Run-test save flow using SaveTestReportUseCase with incrementClientCounter = true.
Outputs: Decision that nextIdNumber increments on any saved result from the run-test flow; duplication/import keeps raw saves.
Notes: Supersedes ADR-0004 increment section only; formatting rules remain unchanged.
-->
# ADR-0010 — Socket ID increment on every save

- **Status:** Accepted  
- **Data:** 2025-12-17  
- **Supersedes:** ADR-0004 (increment rule)

## Contesto

L'incremento del contatore socket avveniva solo sui report `PASS`, causando suggerimenti obsoleti dopo salvataggi `FAIL` e richiedendo refresh manuale per riallineare dashboard e DB.

## Decisione

- Il contatore `nextIdNumber` del `Client` viene incrementato per **ogni salvataggio di report** effettuato dal flusso di run-test (`SaveTestReportUseCase` con `incrementClientCounter = true`), indipendentemente da `overallStatus`.
- I percorsi di duplicazione/import/restore continuano a usare salvataggi raw (flag `incrementClientCounter = false`) per non toccare il contatore.
- La dashboard deve derivare il client selezionato dal flusso DB (non da snapshot in memoria) per riflettere immediatamente l'aggiornamento del contatore.

## Conseguenze

- Dopo un salvataggio `PASS` o `FAIL`, il suggerimento socket viene avanzato senza dover riaprire la schermata o riselezionare il cliente.
- La policy di incremento resta centralizzata nel use case; repository Room rimangono CRUD.
- Documentazione e test devono considerare l'incremento sempre-on-save come comportamento di default.
## Aggiornamento (atomicità report + contatore — ADR-0013)

L'incremento del contatore e l'inserimento del report devono essere **atomici** (stessa transazione
Room tramite `TransactionRunner`). Vedi `reference/database.md` e `core/domain/usecase/report/SaveTestReportUseCase.kt`.

- In `SaveTestReportUseCase`:
  - se `incrementClientCounter = true`: inserimento report + incremento nella stessa transazione;
  - se `incrementClientCounter = false`: salva soltanto il report (nessun incremento);
  - se `clientId` è `null`: preserva il comportamento previsto per report orfani;
  - se è richiesto l'incremento e il client non esiste: **fallisci e rollback** (non read-copy-update).
- La query atomica di incremento è equivalente a:

  ```sql
  UPDATE clients
  SET nextIdNumber = nextIdNumber + 1
  WHERE clientId = :clientId
  ```

  e restituisce il numero di righe aggiornate (0 ⇒ client inesistente ⇒ rollback).
- Il rollback è affidato alla transazione; non esiste ripristino manuale post-failure.