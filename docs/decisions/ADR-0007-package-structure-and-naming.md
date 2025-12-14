# ADR-0007: Struttura package e convenzioni di naming

## Contesto

Il progetto ha già una separazione a layer (`core/domain`, `core/data`, `data`, `ui`, `di`), ma alcune aree risultano ambigue:
- package come `repositoryimpl` o file contenitore (`Models.kt`, `Utils.kt`) rendono difficile capire ownership e confini
- implementazioni con suffisso `Impl` non comunicano la tecnologia/strategia usata (Room, MikroTik, DataStore)

Serve una reference unica e stabile per “dove mettere cosa” e come nominare classi/file.

## Decisione

Adottiamo una struttura package coerente con Clean Architecture e convenzioni di naming esplicite:

- `core/domain`: modelli + policy + use case (puro Kotlin)
- `core/data`: porte/contract (repository, IO, PDF, codec)
- `data`: implementazioni concrete (Room, Retrofit/Moshi, iText, DataStore)
- `ui`: Compose + ViewModel organizzata per feature (`ui/feature/*`)
- `di`: moduli Hilt separati per responsabilità

Naming:
- porte: `XxxRepository`
- implementazioni: `RoomXxxRepository`, `MikroTikXxxRepository`, `DataStoreXxxRepository`
- evitare `*Impl` generico dove possibile

La reference operativa è: `docs/reference/project-structure.md`.

## Conseguenze

- Migliore leggibilità e navigazione del codice
- Riduzione drift e di package “disciplina debole”
- Refactoring iniziale (move/rename) ma payoff in manutenzione e testabilità

## Alternative considerate

- Mantenere i package attuali “as-is”: scartato per drift e ambiguità crescenti
- Feature-first totale anche nei layer `core` e `data`: possibile in futuro, ma non necessario per raggiungere i confini SOLID
