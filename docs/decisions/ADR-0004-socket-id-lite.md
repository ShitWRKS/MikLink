# ADR-0004 — Socket ID “Lite”

- **Status:** Accepted
- **Data:** 2025-12-14

## Contesto

Serve un meccanismo di generazione socket-id per i report che sia utile subito, senza introdurre complessità “full template”.

## Decisione

Implementiamo una versione **Lite** basata su campi semplici nel `Client`:

- `socketPrefix` (String)
- `socketSeparator` (String)
- `socketNumberPadding` (Int)
- `socketSuffix` (String)
- `nextIdNumber` (Int)

Generazione:

1) `socketId = prefix + separator + nextIdNumber(padded) + separator + suffix`  
   (gestire separator vuoto e suffix/prefix vuoti)
2) `nextIdNumber` si incrementa **solo** quando un test termina con SUCCESS.

## Conseguenze

- Nessuna configurazione JSON nella lite.
- Il refactor “full template” resta fuori scope e richiede ADR dedicata.
