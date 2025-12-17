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
