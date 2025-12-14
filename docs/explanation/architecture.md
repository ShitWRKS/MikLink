# Architettura

## Obiettivo

MikLink è un'app Android (Compose) che comunica con una **sonda MikroTik** (una sola), esegue test di rete, salva uno storico e genera PDF.

Questa pagina descrive l'architettura **target** (SOLID / clean) e soprattutto le regole **non interpretabili** che impediscono drift.

---

## Architecture contract (non negoziabile)

### Canone (A) — Struttura

- `core/domain/**` = modelli di dominio + regole + use case (puro Kotlin)
- `core/data/**` = **solo** ports/contratti (repository/provider/gateway) + tipi neutrali se indispensabili
- `data/**` = implementazioni concrete (Room/Retrofit/OkHttp/Moshi/iText) + mapper/parser + repositoryImpl
- `ui/**` = Compose + ViewModel + UiState + mapper domain→ui
- `di/**` = wiring/binding. **Zero logica di business**
- `feature/**` = non canonico → da eliminare/migrare
- `domain/**` top-level = non canonico → da migrare in `core/domain/**`

### Regole import (chiuse)

1) `core/domain/model/**` **NON** importa:
   - `core/data/**`
   - Android (`android.*`, `androidx.*`)
   - Room/Retrofit/OkHttp/Moshi/iText
   - qualsiasi package `ui/**`, `di/**`, `data/**`

2) `core/domain/usecase/**` **PUÒ** importare:
   - `core/data/**` (solo ports)
   - `core/domain/**`

3) `core/data/**` **NON** importa:
   - `data/**`
   - `ui/**`
   - `di/**`
   - infrastruttura (Room/Retrofit/OkHttp/Moshi/iText, Android)

4) `data/**` può importare:
   - `core/data/**`
   - `core/domain/**`
   - librerie infra (Room/Retrofit/OkHttp/Moshi/iText)

5) `ui/**` può importare:
   - `core/domain/**`
   - (solo se indispensabile) `core/data/**` **ma non** implementazioni `data/**`

6) `di/**` può importare tutto, ma:
   - niente logica di business
   - niente parsing/mapper “furbi” (solo wiring)

### Invarianti

- **Single probe**: nessun percorso/DTO/usecase/repo espone `probeId`.
- **DB baseline**: niente “v1/v2” in package o classi; Room version parte da 1.
- **Results canonical**: UI e PDF consumano un **modello normalizzato di dominio** (non DTO remoti, non classi UI).
- **HTTPS trust-all**: applicato **solo** quando l'utente ha scelto HTTPS (toggle).
- **Socket-ID Lite**: prima versione semplice; estensioni future solo con ADR.

---

## Data flow (semplificato)

1) UI (Screen) → ViewModel (UiEvent)
2) ViewModel → UseCase (domain) o Port (core/data) per CRUD “thin”
3) UseCase → Port → `data/**` (Room/Retrofit/OkHttp/Moshi) → mapper → Domain model
4) Output domain → mapper `ui/**` → UiState
5) PDF: genera da **domain results** (normalizzati)

---

## Quality gates (per evitare drift)

Regola consigliata: ogni unità di lavoro (epic/PR) chiude con:

- `./gradlew test` verde
- se tocca UI/Room: `./gradlew connectedAndroidTest` quando possibile

Se i test falliscono perché è cambiata l'intenzione, non “fixare a caso”:
- registrare evidenza in `docs/DISCREPANCIES.md`
- decidere tramite ADR o aggiornamento dello scope
