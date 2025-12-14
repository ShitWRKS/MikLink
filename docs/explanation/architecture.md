# Architettura

MikLink è un'app Android (Compose) che comunica con una sonda MikroTik, esegue test di rete, salva lo storico e genera PDF.

Questa pagina descrive:
- l'architettura a layer (Clean / Ports & Adapters)
- i confini “non negoziabili” (per evitare drift)
- dove vive cosa (con link alla reference della struttura)

## Obiettivi architetturali

- **SOLID / testabilità**: logica di dominio isolata da Android/Retrofit/Room/iText
- **Manutenibilità**: ogni feature ha owner chiaro e naming coerente
- **Compatibilità dati**: DB schema e backup JSON restano importabili nel tempo

## Strati e direzione delle dipendenze

```
ui (Compose + VM)
  ↓ (dipende da)
core/domain (modelli + policy + usecase)
  ↓ (dipende da)
core/data (porte/contract: repository, IO, PDF, codec)
  ↓ (implementato da)
data (Room, Retrofit/Moshi, iText, DataStore, Android IO)
```

### Contract (non negoziabile)

- `core/domain/**` è **puro Kotlin**:
  - non importa Android, Retrofit/Moshi, Room, iText
- `core/data/**` contiene **solo contract**:
  - interfacce repository
  - contract per IO documenti e PDF
  - codec/serializer come contract
  - **non** importa `com.app.miklink.data.*` (niente DTO nelle porte)
- `data/**` è il layer di implementazione e può dipendere dai framework
- `ui/**` usa use case/porte e modelli di dominio; non deve dipendere da implementazioni concrete

Per i dettagli operativi e l'albero package, vedi: `reference/project-structure.md`.

## Flusso alto livello (runtime)

1) L'utente configura la sonda (`ProbeConfig`) e verifica la connettività
2) Lancia un test profile (`TestProfile`)
3) Il runner esegue chiamate MikroTik (layer `data/remote`) + interpreters di dominio (layer `core/domain`)
4) Salva `TestReport` e, quando previsto dalle policy di dominio, aggiorna lo stato client (es. Socket-ID Lite)
5) Genera PDF tramite contract `core/data/pdf` + implementazione iText in `data/pdf/itext`

## Punti di attenzione (drift già individuato)

### DTO leak in una porta

È stato individuato un caso in cui una porta in `core/data` dipende da DTO in `data/remote/**`.
Questo viola DIP (le porte non devono dipendere dalle implementazioni).

**Regola**: le porte espongono solo tipi dominio o boundary model senza annotation Moshi/Retrofit.

**Fix**: spostare i DTO in `data/remote/mikrotik/dto` e introdurre mapper `dto -> domain` in `data/remote/mikrotik/mapper`.
Vedi ADR: `decisions/ADR-0008-no-dto-leaks-across-ports.md`.

## Guardrail consigliati

- static analysis (Detekt o equivalente) per vietare import:
  - `core/domain` → `android.*`, `androidx.*`, `com.app.miklink.data.*`, `com.app.miklink.ui.*`
  - `core/data` → `com.app.miklink.data.*`
- test “bussola” per parsing/mapping:
  - golden test per mapping dto → domain
  - test di round-trip per backup JSON (export/import)
  - test di stabilità per `ReportResultsCodec`
