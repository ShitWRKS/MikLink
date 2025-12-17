<!--
Purpose: Document the original Socket ID Lite formatting and increment policy baseline.
Inputs: Client socket formatting fields and run-test save flow assumptions at the time.
Outputs: Baseline decision for formatting; increment rule is superseded by ADR-0010.
Notes: Keep formatting guidance; see ADR-0010 for the active increment behavior.
-->
# ADR-0004 ƒ?" Socket ID Lite (formattazione deterministica)

- **Status:** Accepted  
- **Data:** 2025-12-14
- **Superseded-by:** ADR-0010 (increment policy)

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

La formattazione Çù pure function nel dominio:

- `Client.socketNameFor(idNumber: Int): String`

Regola:

- `padded = idNumber` formattato con zero-padding in base a `socketNumberPadding`
- I separatori sono inseriti solo se cƒ?TÇù contenuto adiacente:
  - se `prefix` non Çù vuoto ƒÅ' `prefix + separator + padded`
  - se `suffix` non Çù vuoto ƒÅ' `padded + separator + suffix`
  - se entrambi presenti ƒÅ' `prefix + separator + padded + separator + suffix`
  - se entrambi vuoti ƒÅ' `padded` puro
> Nota: il separatore non viene mostrato dopo il numero se `suffix` Çù vuoto e non viene mostrato prima del numero se `prefix` Çù vuoto.

### Incremento `nextIdNumber`

> Regola originale **superseded** da ADR-0010. Manteniamo la traccia storica.

- Il contatore si incrementa **solo** quando un report salvato ha `overallStatus == "PASS"` ed Çù salvato dal **flow di run-test**.
- L'incremento Çù applicato nel use case `SaveTestReportUseCase`; il repository Room rimane CRUD e non muta `Client`.
- Percorsi di duplicazione/import/restore devono usare il repository raw (senza incrementare).

### Parsing (fill-gaps e suggerimenti dashboard)

- Parsing Çù gestito dal policy `SocketIdLite.parseIdNumber(socketName, prefix, separator)`.
- Formato atteso: separatori presenti solo quando cƒ?TÇù contenuto adiacente (allineato alla formattazione attuale).
- Regole:
  - se `prefix` non Çù vuoto il nome deve iniziare con `prefix + separator`;
  - con `prefix` vuoto viene tollerato (solo in lettura) un separatore iniziale legacy;
  - il segmento numerico Çù la parte prima del prossimo separatore (se presente) o fino alla fine stringa;
  - restituisce `Int?` da `toIntOrNull()` (supporta padding con zeri).
- Se il nome non rispetta il formato o contiene segmenti non numerici, ritorna `null` e l'id viene ignorato (sicuro per fill-gaps).
- Usato da dashboard per strategia `FILL_GAPS` e mantiene coerenza con la preview di Settings e con il formato aggiornato.

## Conseguenze

- Comportamento deterministico e testabile.
- Nessun side-effect nascosto nei repository; le policy restano nel livello use case.
- Un eventuale ƒ?ofull templateƒ?? resta fuori scope e richiede ADR dedicato.
