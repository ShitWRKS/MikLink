# ADR-0004 — Socket ID Lite (formattazione deterministica)

- **Status:** Accepted
- **Data:** 2025-12-14

## Contesto

Serve un socket-id leggibile e deterministico nei report, senza introdurre un sistema di template complesso.

## Decisione

Implementiamo una versione **Lite** basata su campi semplici nel `Client`:

- `socketPrefix` (String)
- `socketSeparator` (String)
- `socketNumberPadding` (Int)
- `socketSuffix` (String)
- `nextIdNumber` (Int)

### Formattazione

La formattazione è pure function nel dominio:

- `Client.socketNameFor(idNumber: Int): String`

Regola:

- `padded = idNumber` formattato con zero-padding in base a `socketNumberPadding`
- `socketName = prefix + separator + padded + separator + suffix`

> Nota: la funzione concatena sempre entrambi i separatori (anche se prefix/suffix sono vuoti).  
> Fonte: `core/domain/model/Client.kt`.

### Incremento nextIdNumber

`nextIdNumber` si incrementa **solo** quando un report salvato ha `overallStatus == "PASS"`.

Fonte: `data/repositoryimpl/room/RoomReportRepository.kt` + `SocketIdLiteIncrementTest`.

## Conseguenze

- Il comportamento è deterministico e testabile.
- Un eventuale “full template” resta fuori scope e richiede ADR dedicato.
