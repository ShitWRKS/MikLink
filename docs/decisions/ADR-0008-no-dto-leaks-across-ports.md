# ADR-0008: Vietare DTO Retrofit/Moshi nelle porte (no DTO leak)

## Contesto

Le porte in `core/data/**` rappresentano l'API interna dell'applicazione verso l'esterno (repository, servizi).
Se una porta dipende da DTO Retrofit/Moshi definiti in `data/remote/**`, si viola il principio DIP:
le porte (astrazioni) non devono dipendere dalle implementazioni (dettagli).

Questo rende più difficile:
- testare `core/*` senza dipendenze da framework
- cambiare libreria di parsing/transport senza toccare `core/*`
- mantenere stabilità del dominio

## Decisione

- I DTO di rete vivono solo in `data/remote/**` (es. `data/remote/mikrotik/dto`).
- Le porte `core/data/**` espongono:
  - tipi di dominio (`core/domain/**`)
  - oppure boundary model senza annotation framework (se serve un tipo specifico del confine).
- Ogni conversione `dto -> domain` avviene in `data/remote/**/mapper`.

## Conseguenze

- Confini SOLID più forti e verificabili
- Possibilità di golden test su mapper senza contaminare `core/*`
- Refactoring necessario sui repository/porte che attualmente espongono DTO

## Alternative considerate

- Esporre DTO direttamente per “comodità”: scartato (introduce coupling e drift)
- Generare DTO anche nel core: scartato (duplica modelli e rompe ownership)
