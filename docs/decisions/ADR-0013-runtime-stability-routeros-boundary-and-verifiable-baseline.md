# ADR-0013 — Runtime stability, RouterOS boundary and verifiable baseline

- **Status:** Accepted
- **Data:** 2026-07-19
- **Supersedes:** nessuno (integra e preserva le decisioni precedenti)

## Contesto

La separazione tra il confine RouterOS e il dominio è stata introdotta ma non completata.
Diverse fragilità runtime sono emerse e devono essere risolte in modo deterministico e verificabile:

- la separazione RouterOS è introdotta ma non completata;
- `RunTestUseCaseImpl` non è puro e ha responsabilità multiple (Android, UI, implementazioni data);
- esiste una race tra l'esecuzione precedente e quella successiva;
- il trace context globale può essere cancellato dal run sbagliato;
- la perdita della sonda non è distinta dal fallimento del target;
- la serializzazione usa un fallback a `{}` che maschera errori;
- il salvataggio del report e l'incremento del Socket-ID non sono atomici;
- il `clientKey` dei backup può andare in collisione;
- il Moshi globale è permissivo (coercizione/adapter globali);
- manca una CI ordinaria che esegua i gate;
- le dipendenze non sono allineate alla baseline approvata (Fase 7).

Questo ADR congela l'architettura approvata e la semantica dell'esecuzione, in modo che le
fasi successive (2–11) siano implementazioni meccaniche di quanto qui deciso.

## Decisioni preservate

Si riferiscono esplicitamente e si dichiarano **invariate** le seguenti decisioni:

- **ADR-0001** — singola sonda (nessun `probeId`);
- **ADR-0002** — trust-all HTTPS con fallback HTTPS → HTTP;
- **ADR-0006** — backup con `clientKey` stabile e report orfani (esteso a v2, vedi sotto);
- **ADR-0009** — log di esecuzione test sanitizzati, bounded, solo UI;
- **ADR-0010** — incremento Socket-ID a ogni salvataggio run-test (esteso ad atomicità, vedi sotto);
- **ADR-0011** — contratto tipizzato di esecuzione e renderer unificato (esteso a termination reason, vedi sotto);
- **ADR-0012** — migrazioni distruttive pre-produzione.

### Invarianti dichiarati

Restano invariati e non negoziabili:

- singola sonda (ADR-0001);
- trust-all HTTPS (ADR-0002);
- fallback handshake HTTPS → HTTP (ADR-0002);
- credenziali salvate ed esportate (ADR-0002/ADR-0006);
- migrazioni distruttive pre-produzione (ADR-0012);
- incremento Socket-ID a ogni salvataggio run-test (ADR-0010);
- report format v1 (`resultFormatVersion = 1`);
- log sanitizzati e non persistenti (ADR-0009);
- stop dopo fallimento Layer 1.

## Architettura approvata

Flusso unidirezionale, con confine RouterOS isolato dietro `MikroTikCallExecutor` e decoder/normalizer:

```
RouterOS REST
    ↓
MikroTikApiService
    ↓
MikroTikCallExecutor
    ↓
RouterOsResponseDecoder
    ↓
RouterOsNormalizer
    ↓
MikroTik repositories
    ↓
test steps
    ↓
RunTestUseCase
    ↓
TestQualityPolicy
    ↓
snapshot/report/UI
```

- `MikroTikApiService` restituisce `retrofit2.Response<T>` per tutti gli endpoint.
- `MikroTikCallExecutor` è l'unico path di chiamata; i repository non costruiscono Retrofit.
- `RouterOsResponseDecoder` classifica HTTP e decodifica l'error body in modo strutturato.
- `RouterOsNormalizer` centralizza la normalizzazione in modelli puri Kotlin (no Retrofit/Moshi).
- I repository restituiscono modelli DTO-free; non interpretano direttamente errori HTTP.
- `RunTestUseCase` è puro Kotlin; la semantica dei messaggi è delegata a `TestRunTextProvider`.
- `TestQualityPolicy` è l'unico decisore di PASS/FAIL sulle soglie utente.

## Semantica dell'esecuzione

- **Una sola esecuzione attiva**: una nuova esecuzione sostituisce la precedente.
- La sostituzione usa `cancelAndJoin` sul run precedente prima di avviare il nuovo.
- **Ownership generazionale dello stato**: ogni run riceve una generazione incrementale;
  solo la generazione corrente può mutare `uiState`, `snapshot`, `logs`, `isRunning`.
  Il `finally` del vecchio run non può modificare lo stato del nuovo run.
- **Trace context pulito tramite run ID atteso**: `clear(expectedRunId)` usa
  `compareAndSet(expectedRunId, null)`; non esiste più un `clear()` incondizionale.
- **Snapshot parziale salvabile** in caso di perdita sonda (vedi modello errori).
- `overallStatus` resta `PASS` o `FAIL` (nessun terzo valore).
- **Termination reason additiva**: `TestOutcome.termination` e `TestOutcome.terminalError`
  si aggiungono al report senza cambiare `resultFormatVersion`.
- **Nessuna sezione finale `RUNNING` o `PENDING`**: lo snapshot finale è coerente
  (sezioni `PASS`/`FAIL`/`SKIP`/`CANCELLED`, mai `RUNNING`/`PENDING`).

## Modello errori

Categorie di `TestError` (enum/value object tipizzati), classificate al confine RouterOS:

- `ProbeUnavailable` — sonda non raggiungibile (ConnectException, NoRouteToHostException,
  UnknownHostException, EOF/connection reset/socket closed durante la chiamata);
- `Authentication` — HTTP 401/403;
- `Tls` — SSLHandshakeException finale (handshake fallito, dopo fallback ADR-0002);
- `Timeout` — SocketTimeoutException;
- `RouterOsError` — HTTP RouterOS con error body decodificato;
- `InvalidResponse` — body di successo vuoto o invalido;
- `Unsupported` — operazione non supportata dalla sonda (es. TDR non supportato);
- `ConfigurationError` — configurazione iniziale mancante (client/probe/profilo);
- `SerializationError` — il codec report fallisce (mai fallback a `{}`);
- `Unexpected` — qualsiasi altra eccezione non classificabile sopra.

`CancellationException` viene sempre rilanciata (non classificata).

## PASS/FAIL

- Parser e normalizzatore **non** decidono PASS/FAIL.
- Le soglie utente sono applicate **esclusivamente** da `TestQualityPolicy`.
- Un test disabilitato o opzionale non configurato **non** causa FAIL.
- Uno speed test assente può produrre `PASS` (se non richiesto da alcuna soglia attiva).
- Una metrica necessaria a una soglia attiva ma invalida produce `FAIL` tecnico.
- La perdita della sonda produce `FAIL` terminale con snapshot parziale
  (sezioni successive dipendenti dalla sonda → `SKIP` con reason `PROBE_UNAVAILABLE`).

## Dipendenze

Baseline approvata (vedi Fase 7). Le versioni sono vincolanti e centralizzate nel version catalog:

| Componente | Versione |
|---|---|
| AGP | 9.3.0 |
| Gradle wrapper | 9.5.0 |
| JDK | 17 |
| compileSdk | 37 |
| targetSdk | 36 |
| minSdk | 30 |
| Kotlin/KGP | 2.3.21 |
| KSP | 2.3.9 |
| Compose compiler plugin | 2.3.21 |
| Dagger/Hilt | 2.59.2 |
| AndroidX Hilt | 1.4.0 |
| Coroutines | 1.11.0 |
| Core KTX | 1.19.0 |
| Lifecycle | 2.11.0 |
| Activity Compose | 1.13.0 |
| Navigation Compose | 2.9.8 |
| Compose BOM | 2026.06.00 |
| Room | 2.8.4 |
| DataStore | 1.2.1 |
| Tracing | 1.3.0 |
| Retrofit | 3.0.0 |
| OkHttp | 4.12.0 |
| Moshi | 1.15.2 |
| Moshi codegen | 1.15.2 |
| Coil | 3.5.0 |
| AndroidX Test JUnit | 1.3.0 |
| Espresso | 3.7.0 |
| MockK | 1.14.9 |
| Robolectric | 4.15 |
| iText | 7.2.6 |

Vietato: Kotlin 2.4, OkHttp 5, Room 3, versioni pre-release, repository snapshot.

## Non-obiettivi

- modifica delle decisioni di sicurezza ADR-0002;
- cifratura credenziali;
- migrazioni Room production-ready;
- Room 3;
- OkHttp 5;
- cambio licenza;
- migrazione iText;
- nuovi moduli Gradle;
- riscrittura completa del runner;
- modifica del formato report v1;
- retry automatici generalizzati.
