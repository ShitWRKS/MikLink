# Architettura

MikLink è un'app Android (Compose) che comunica con una sonda MikroTik, esegue test di rete, salva lo storico e genera PDF.

Questa pagina descrive:
- l'architettura a layer (Clean / Ports & Adapters)
- i confini non negoziabili (per evitare drift)
- dove vive cosa (con link alla reference della struttura)

## Obiettivi architetturali

- **SOLID / testabilità**: logica di dominio isolata da Android/Retrofit/Room/iText
- **Manutenibilità**: ogni feature ha owner chiaro e naming coerente
- **Compatibilità dati**: DB schema e backup JSON restano importabili nel tempo

## Strati e direzione delle dipendenze

```
ui (Compose + VM)
  ↑ (dipende da)
core/domain (modelli + policy + usecase)
  ↑ (dipende da)
core/data (porte/contract: repository, IO, PDF, codec)
  ↑ (implementato da)
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
4) Salva `TestReport` tramite use case applicativo (policy Socket-ID Lite applicata qui, repository restano CRUD)
5) Genera PDF tramite contract `core/data/pdf` + implementazione iText in `data/pdf/itext` che decodifica con `ReportResultsCodec`

## Punti di attenzione (guardrail attivi)

### Porta MikroTik e mapping

- Le porte espongono solo tipi dominio o boundary model senza annotation Moshi/Retrofit (DIP).
- Il repository di test MikroTik restituisce `LinkStatusData`, `CableTestSummary`, `PingMeasurement`, `NeighborData`, `SpeedTestData`.
- La conversione `dto -> domain` è centralizzata in `data/remote/mikrotik/mapper`.
- Il confine RouterOS è isolato dietro `MikroTikCallExecutor` + `RouterOsResponseDecoder` + `RouterOsNormalizer`
  (vedi `decisions/ADR-0013-runtime-stability-routeros-boundary-and-verifiable-baseline.md`):
  - `MikroTikApiService` restituisce `retrofit2.Response<T>` per tutti gli endpoint.
  - `RouterOsResponseDecoder` classifica HTTP e decodifica l'error body (`RouterOsErrorBody`) in modo strutturato.
  - `RouterOsNormalizer` normalizza in modelli puri Kotlin (no Retrofit/Moshi) e mantiene il report v1
    tramite conversione esplicita in un solo punto.
  - I repository non costruiscono Retrofit, non interpretano direttamente errori HTTP e non analizzano
    liberamente `Throwable.message`.
- Moshi è suddiviso in `@AppMoshi` (strict: backup, report, codec applicativi) e `@RouterOsMoshi`
  (solo Retrofit RouterOS). Nessun adapter globale Boolean/Int, nessuna coercizione globale.
Vedi ADR: `decisions/ADR-0008-no-dto-leaks-across-ports.md`.

## Guardrail consigliati

- static analysis (Detekt o equivalente) per vietare import:
  - `core/domain` ← `android.*`, `androidx.*`, `com.app.miklink.data.*`, `com.app.miklink.ui.*`
  - `core/data` ← `com.app.miklink.data.*`
- test “bussola” per parsing/mapping:
  - golden test per mapping dto → domain
  - test di round-trip per backup JSON (export/import)
  - test di stabilità per `ReportResultsCodec`
