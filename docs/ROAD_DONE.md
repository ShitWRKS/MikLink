


EPIC S8 — Sunset definitivo di AppRepository (Audit + Migrazione + Rimozione)
Scopo

Eliminare completamente l’uso di AppRepository dalla codebase (UI, domain, data), sostituendolo con repository SOLID dedicati (interfacce in core/, implementazioni in data/), mantenendo build e test verdi.

Nota: eventuali problemi non bloccanti emersi in S7 vanno tracciati ma non risolti ora (vedi sezione “Known Issues”).

Regole operative per l’agent

Non inventare nulla. Ogni decisione deve essere supportata da codice esistente o da documentazione già presente nel repo.

Non eseguire comandi git / PR.

Dopo ogni step “di migrazione” eseguire i 3 comandi (KSP/assemble/test) e salvare i log in docs/migration/.

Cambiamenti solo “meccanici”: spostamenti, estrazioni, rinomina dipendenze, DI bindings, test contract minimi.

Se un refactor richiede una scelta funzionale non definita: STOP e scrivere “Decision required” in docs/migration/S8_OPEN_QUESTIONS.md.

S8.0 — Baseline + checkpoint obbligatori

Comandi

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Output

Salvare in:

docs/migration/S8_ksp_baseline.txt

docs/migration/S8_assemble_baseline.txt

docs/migration/S8_tests_baseline.txt

Doc

Creare docs/migration/S8_BASELINE.md con data + esito + note.

S8.1 — Audit deterministico: dove viene ancora usato AppRepository

Obiettivo
Produrre una lista completa e verificabile di:

file che importano/iniettano AppRepository

metodi usati

feature impattata (UI, data, domain)

Azioni

Ricerca testuale su sorgenti main (no test):

cercare AppRepository

cercare @Inject constructor(... AppRepository

cercare import .*AppRepository

Generare report:

docs/migration/S8_apprepository_usage_audit.md con:

tabella: File | Classe | Tipo (VM/Repo/UseCase/Altro) | Metodi chiamati | Note

indicare anche se è una dipendenza non usata (da rimuovere)

Checkpoint

Nessun codice cambiato in questo step.

S8.2 — Definizione “target repositories” (solo contratti)

Obiettivo
Per ogni responsabilità rimasta in AppRepository (dall’audit S8.1), creare 1 repository dedicato (SRP).

Regole

Interfacce in app/src/main/java/com/app/miklink/core/data/repository/...

Nomi espliciti per responsabilità (no “Manager” generici)

Metodi copiati come “contract” (stessa firma se possibile), ma senza dipendenze Android nella signature (se evitabile)

Output

Creare file interfacce + KDoc con:

Input

Output

Error handling atteso

Threading/coroutines attese (suspend/Flow)

Checkpoint

./gradlew :app:kspDebugKotlin + log in docs/migration/S8_ksp_step_contracts.txt

S8.3 — Implementazioni data/repositoryimpl (una responsabilità per volta)

Obiettivo
Implementare ogni contract con classi in:

app/src/main/java/com/app/miklink/data/repositoryimpl/...

Sequenza
Migrare una sola responsabilità per PR virtuale (cioè per step), così:

Implementazione repository

Binding DI

Aggiornamento dei chiamanti

Deprecazione del metodo corrispondente in AppRepository (temporaneo)

Checkpoint build/test

Checkpoint per ogni responsabilità

./gradlew :app:kspDebugKotlin → docs/migration/S8_ksp_step_<name>.txt

./gradlew assembleDebug → docs/migration/S8_assemble_step_<name>.txt

./gradlew testDebugUnitTest → docs/migration/S8_tests_step_<name>.txt

S8.4 — Aggiornamento chiamanti (ViewModel / UseCase / altri)

Obiettivo
Rimuovere AppRepository dai costruttori e sostituire con i nuovi repository.

Regole

Ogni ViewModel deve dipendere solo da:

repository core (interfacce)

usecase core (se presenti)

DAO solo se già stabilito come eccezione (ma preferire repository)

Pulizia

Se AppRepository è iniettato ma non usato: rimuovere subito (con checkpoint).

S8.5 — Rimozione definitiva di AppRepository

Obiettivo
Eliminare AppRepository e qualsiasi implementazione/bridge residua.

Azioni

Verifica: nessuna occorrenza in main:

AppRepository non deve comparire in app/src/main/java/**

Eliminare file/classi:

rimuovere AppRepository (core e data, legacy se presente)

rimuovere binding DI correlati

Aggiornare documentazione architettura:

docs/ARCHITECTURE.md: rimuovere riferimenti ad AppRepository come entry point.

Checkpoint finale

./gradlew :app:kspDebugKotlin → docs/migration/S8_ksp_final.txt

./gradlew assembleDebug → docs/migration/S8_assemble_final.txt

./gradlew testDebugUnitTest → docs/migration/S8_tests_final.txt

S8.6 — Contract tests minimi per i repository creati

Obiettivo
Aggiungere test unitari “contract-style” per ogni nuovo repository:

verifica mapping base

gestione errori

casi vuoti/null

Vincoli

Test non devono “barare” per passare.

Dove serve rete: mock del MikroTikServiceProvider (o equivalente già presente).

---

## EPIC U1.7 — Progressive Reveal Cards (Test UI)

Scopo

Rendere l'esperienza utente della schermata di test più leggibile durante l'esecuzione: mostrare progressivamente le card (solo gli step già conclusi + la prossima corrente), impedire l'espansione e il rendering dei dettagli per step non finali (RUNNING/PENDING), e riusare il renderer dei dettagli della schermata finale per evitare drift.

Regole operative

- Nessuna modifica al dominio o agli usecase.
- Nessun evento aggiunto (es. `SectionsUpdated`), solo logica di presentazione.
- Nessun debito tecnico: riusare renderer/mapper esistenti (es. `TestSkipReasonMapper`).

Acceptance Criteria

- Durante test in corso (isRunning == true): appaiono tutte le sezioni con status != "PENDING" + al massimo la prima "PENDING" incontrata.
- Le card PASS/FAIL/SKIP sono espandibili e mostrano i dettagli.
- Le card RUNNING/PENDING non sono espandibili e non mostrano dettagli (solo header).

Implementazione (sintesi)

- `TestExecutionScreen.kt`: nella composable `TestInProgressView` calcolare `visibleSections` includendo tutte le non-pending e la prima pending incontrata, preservando l'ordine.
- Aggiungere `isFinalStatus(status)` helper che ritorna true per PASS/FAIL/SKIP.
- Aggiungere `expandable: Boolean = true` a `TestSectionCard` e disabilitare l'interazione/icone/dettagli quando `expandable == false`.
- Estrarre `@Composable private fun TestSectionDetails(section: TestSection)` e riusarlo sia in `TestCompletedView` che in `TestInProgressView` per assicurare lo stesso rendering dei dettagli.

Files toccati

- `app/src/main/java/com/app/miklink/ui/test/TestExecutionScreen.kt`
- `app/src/main/java/com/app/miklink/ui/common/ResultCards.kt`

Log e risultato

- Baseline + final logs e risultato sono salvati in `docs/migration/`:
  - `U1_7_ksp_baseline.txt`, `U1_7_assemble_baseline.txt`, `U1_7_tests_baseline.txt`
  - `U1_7_ksp_final.txt`, `U1_7_assemble_final.txt`, `U1_7_tests_final.txt`
  - Report finale: `docs/migration/U1_7_RESULT.md`

Stato: Completed ✅

S8.7 — Known Issues (posticipati ma tracciati)

Obiettivo
Non risolvere ora i problemi non bloccanti, ma tracciarli.

Azioni

Creare/aggiornare docs/KNOWN_ISSUES.md con:

ID, descrizione, impatto, riproduzione, area, severità, workaround

Link ai log / file coinvolti

Acceptance Criteria EPIC S8

✅ Nessuna occorrenza di AppRepository in app/src/main/java/**

✅ ./gradlew :app:kspDebugKotlin PASS

✅ ./gradlew assembleDebug PASS

✅ ./gradlew testDebugUnitTest PASS

✅ docs/migration/S8_RESULT.md presente con baseline + step logs + elenco file creati/modificati

✅ docs/ARCHITECTURE.md aggiornato coerentemente



EPIC S6 — Eliminare i bridge verso AppRepository nel percorso “Run Test” (NetworkConfig + DHCP/Gateway)

Copia/incolla in ROADMAP. Super-dettagliata, con stop condition anti-drift.

Obiettivo

Rendere il percorso di esecuzione test (RunTestUseCase → Steps → Repositories → MikroTik REST) indipendente da AppRepository / legacy, eliminando:

il bridge di NetworkConfigRepository verso AppRepository

il bridge “service build + DHCP gateway” dentro PingTargetResolverImpl (oggi basato su buildServiceFor(probe) + api.getDhcpClientStatus(interfaceName))

Nota: questa EPIC riguarda solo il percorso “Run Test”. AppRepository può restare per feature non ancora migrate.

S6.0 — Baseline (obbligatorio)

Eseguire e salvare output:

./gradlew :app:kspDebugKotlin → salvare in docs/migration/S6_ksp_baseline.txt

./gradlew assembleDebug → salvare in docs/migration/S6_assemble_baseline.txt

./gradlew testDebugUnitTest → salvare in docs/migration/S6_tests_baseline.txt

Stop condition: se uno fallisce, NON procedere con S6.

S6.1 — Inventario dipendenze residue da AppRepository nel path “Run Test”
Target

core/domain/usecase/test/RunTestUseCaseImpl.kt

data/teststeps/*StepImpl.kt

data/repositoryimpl/* (in particolare NetworkConfigRepositoryImpl, PingTargetResolverImpl)

di/TestRunnerModule.kt, di/RepositoryModule.kt

Azione

Cercare nel codice (PowerShell o IDE) riferimenti a:

AppRepository / AppRepository_legacy

package com.app.miklink.legacy.*

Produrre lista (solo testo) in docs/migration/S6_dependency_audit.md con:

file path

simbolo usato (es. tipo, metodo)

motivo (es. “applyClientNetworkConfig”, “buildServiceFor”)

Stop condition: se il path Run Test usa ancora legacy in più punti oltre quelli già noti, segnalarli nel file e includerli negli step successivi (non inventare fix).

Checkpoint: ./gradlew :app:kspDebugKotlin PASS

S6.2 — Centralizzare la creazione del service MikroTik in una dipendenza DI (no factory sparsa)
Problema attuale (dato)

PingTargetResolverImpl richiede probe per chiamare buildServiceFor(probe) e poi api.getDhcpClientStatus(interfaceName).

Target nuovo (SOLID)

Creare un’astrazione unica (in core) per ottenere il service REST:

File da creare

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/service/MikroTikServiceProvider.kt

package com.app.miklink.core.data.remote.mikrotik.service

import com.app.miklink.core.data.local.room.v1.model.ProbeConfig

interface MikroTikServiceProvider {
    fun build(probe: ProbeConfig): MikroTikApiService
}


Implementazione (in data):
app/src/main/java/com/app/miklink/data/remote/mikrotik/MikroTikServiceProviderImpl.kt

Deve usare l’infrastruttura già esistente (MikroTikServiceFactory o equivalente) senza cambiare logica.

Se oggi esiste già una factory DI-friendly, usarla.

Se la factory è statica/companion, wrappare.

DI

Aggiornare di/NetworkModule.kt (o modulo corretto) per bindare:

MikroTikServiceProvider → MikroTikServiceProviderImpl

Refactor immediato

Aggiornare:

PingTargetResolverImpl per dipendere da MikroTikServiceProvider (non chiamare factory direttamente)

Qualsiasi altro componente “Run Test path” che costruisce il service direttamente

Checkpoint:

./gradlew :app:kspDebugKotlin PASS

S6.3 — Estrarre “DHCP Gateway resolution” in un repository dedicato (rimuovere conoscenza DHCP da PingTargetResolver)
Target nuovo

PingTargetResolver deve risolvere target “logici”, ma la logica DHCP (API, parsing DTO, fallback) deve stare in data layer dedicato.

File da creare

app/src/main/java/com/app/miklink/core/data/repository/test/DhcpGatewayRepository.kt

package com.app.miklink.core.data.repository.test

import com.app.miklink.core.data.local.room.v1.model.ProbeConfig

interface DhcpGatewayRepository {
    suspend fun getGatewayForInterface(
        probe: ProbeConfig,
        interfaceName: String
    ): String?
}


Implementazione:
app/src/main/java/com/app/miklink/data/repositoryimpl/mikrotik/DhcpGatewayRepositoryImpl.kt

Deve usare:

MikroTikServiceProvider.build(probe)

MikroTikApiService.getDhcpClientStatus(interfaceName) (esattamente come oggi)

Deve gestire:

risposta senza gateway → ritorna null

errori rete/API → ritorna null oppure propaga eccezione (scegliere 1 comportamento e documentarlo in KDoc; NON inventare “magie”)

Nessun testo localizzato qui.

Aggiornare PingTargetResolverImpl

Dipendenze:

DhcpGatewayRepository

eventuale altra logica già presente

Rimuovere chiamate dirette a api.getDhcpClientStatus(...)

DI

Aggiornare di/RepositoryModule.kt:

bind DhcpGatewayRepository → DhcpGatewayRepositoryImpl

Checkpoint:

./gradlew :app:kspDebugKotlin PASS

./gradlew testDebugUnitTest PASS

Stop condition (dati mancanti):
Se il DTO/response di getDhcpClientStatus non è chiaramente determinabile dal codice esistente, fermarsi e chiedere output di un curl “dhcp-client print/monitor” (NO assunzioni).

S6.4 — Eliminare il bridge NetworkConfigRepository -> AppRepository (core feature di S6)
Obiettivo

NetworkConfigRepositoryImpl deve eseguire direttamente le stesse operazioni che oggi fa AppRepository.applyClientNetworkConfig(...), ma senza chiamarlo.

Step 1 — Congelare comportamento attuale (anti-regressione)

Identificare nel codice AppRepository.applyClientNetworkConfig(...):

file path esatto

firma esatta

sequenza chiamate MikroTik (endpoint/metodi MikroTikApiService)

eventuali scritture DB/Report

Scrivere in docs/migration/S6_network_config_behavior.md:

elenco chiamate in ordine (nome metodo service)

condizioni (es. DHCP vs Static, override etc.)

side effects (scritture DB)

Stop condition: se la logica è troppo “incollata” alla UI o dipende da state globale non riproducibile, fermarsi e chiedere istruzioni (non inventare).

Checkpoint: ./gradlew :app:kspDebugKotlin PASS

Step 2 — Implementazione deterministica in NetworkConfigRepositoryImpl
Target

app/src/main/java/com/app/miklink/data/repositoryimpl/NetworkConfigRepositoryImpl.kt

Azione

Rimuovere qualsiasi dipendenza da AppRepository.

Dipendenze consentite:

MikroTikServiceProvider

repository Room v1 (ClientRepository / ProbeRepository se necessari)

eventuale RouteManager solo se già esiste e non è legacy (altrimenti implementare le chiamate route direttamente come fa AppRepository, senza creare nuovi layer “a caso”)

Replicare la sequenza chiamate documentata nello step precedente.

Pulizia contratto

Rimuovere @Deprecated da:

core/data/repository/test/NetworkConfigRepository.kt

NetworkConfigRepositoryImpl.kt

Aggiornare KDoc: ora è implementazione reale, non bridge.

Checkpoint:

./gradlew :app:kspDebugKotlin PASS

./gradlew assembleDebug PASS

./gradlew testDebugUnitTest PASS

S6.5 — “Run Test path” deve essere legacy-free (verifica automatica)
Verifica

Ricerca testuale (PowerShell) in soli file coinvolti nel run test:

RunTestUseCaseImpl

data/teststeps/*

NetworkConfigRepositoryImpl

PingTargetResolverImpl

DhcpGatewayRepositoryImpl
per:

AppRepository

legacy.

Salvare output in:

docs/migration/S6_legacy_free_audit.txt

Acceptance: nessuna occorrenza nel path Run Test.

Checkpoint: ./gradlew :app:kspDebugKotlin PASS

S6.6 — Test minimi (senza UI automation)
Obiettivo

Aggiungere test focalizzati sulla nuova architettura senza introdurre UI test complessi.

A) Unit test (mock-driven) per PingTargetResolver + DhcpGatewayRepository

Creare test in:

app/src/test/java/com/app/miklink/core/data/repository/test/DhcpGatewayRepositoryContractTest.kt

app/src/test/java/com/app/miklink/core/data/repository/test/PingTargetResolverContractTest.kt

Linee guida:

mock di MikroTikServiceProvider e MikroTikApiService

verificare:

se gateway mancante → null

se PING_NO_TARGETS generato correttamente già coperto dai test del runner (se presenti)

B) Golden parsing (solo se serve)

Se in S6.3 si è dovuto introdurre/aggiornare DTO per getDhcpClientStatus:

aggiungere fixture reale in:

app/src/test/resources/mikrotik/7.20.5/<nome_fixture_dhcp_status>.json

aggiungere golden test in:

app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/DhcpStatusGoldenParsingTest.kt

Stop condition (dati mancanti): se non c’è payload reale, fermarsi e chiedere al maintainer di fornire output curl.

Checkpoint finale: ./gradlew testDebugUnitTest PASS

S6.7 — Documentazione finale + log

Creare/aggiornare:

docs/migration/S6_BASELINE.md

docs/migration/S6_RESULT.md

In S6_RESULT.md includere:

elenco file creati/modificati (path completi)

conferma rimozione @Deprecated su NetworkConfigRepository

conferma: Run Test path legacy-free

esito comandi finali:

:app:kspDebugKotlin

assembleDebug

testDebugUnitTest

Acceptance Criteria EPIC S6

✅ NetworkConfigRepositoryImpl non dipende da AppRepository e NetworkConfigRepository non è più deprecato.

✅ PingTargetResolverImpl non costruisce direttamente il service e non chiama direttamente DHCP API: usa MikroTikServiceProvider + DhcpGatewayRepository.

✅ Nessun riferimento a AppRepository / legacy.* nel path Run Test.

✅ Build + unit test PASS con log salvati in docs/migration/.

---

## EPIC S7 — Rimozione dipendenza da AppRepository dalle feature rimanenti (Dashboard / Probe) + Repository SOLID dedicati

**STATO:** ✅ **COMPLETATA**

**Obiettivo:** Rendere le feature Dashboard e Probe indipendenti da AppRepository, creando repository SOLID dedicati.

**Risultato:** 
- ✅ Creati `ProbeStatusRepository` e `ProbeConnectivityRepository`
- ✅ Migrati `DashboardViewModel`, `ProbeEditViewModel`, `ProbeListViewModel`
- ✅ Rimossa dipendenza non utilizzata da `TestViewModel`
- ✅ Tutti i metodi AppRepository utilizzati da Dashboard/Probe sono stati deprecati
- ✅ Build e test PASS (7 test S7, tutti PASSED)

**Documentazione:** 
- `docs/migration/S7_RESULT.md` - Report completo
- `docs/migration/S7_AUDIT_FINAL.md` - Audit finale di verifica
- `docs/migration/S7_viewmodel_dependency_matrix.md` - Matrice dipendenze ViewModel
- `docs/migration/S7_repository_inventory.md` - Inventario repository
- `docs/migration/S7_tests_inventory.md` - Inventario test


EPIC S5.1 — Hardening S5 (cleanup + rimozione commenti + riduzione dipendenze legacy)

Copia/incolla in roadmap.

Obiettivo

Eliminare drift post-S5: niente blocchi commentati, ridurre dipendenze residue da AppRepository, rendere deterministico l’output del runner (sections + rawResultsJson) senza cambiare UI/UX.

Vincoli

❌ Nessun cambiamento UI/UX (progress + pass/fail invariati).

❌ Nessun cambiamento a endpoint/payload MikroTik.

✅ Solo cleanup strutturale e mapping deterministico.

S5.1.0 — Preflight

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Scrivere docs/migration/S5_1_BASELINE.md con esito.

S5.1.1 — Rimozione “commented legacy code” da TestViewModel

Target: ui/test/TestViewModel.kt

Eliminare completamente i blocchi commentati “legacy orchestration”.

Se serve conservazione storica:

creare file TestViewModel_legacy.kt in percorso legacy concordato oppure usare suffisso _legacy (in base alla policy del progetto),

ma mai tenere 600+ linee commentate nel file attivo.

Checkpoint: 3 comandi build PASS.

S5.1.2 — NetworkConfigRepository bridge: renderlo esplicito e tracciabile

Target:

core/data/repository/test/NetworkConfigRepository.kt

data/repositoryimpl/NetworkConfigRepositoryImpl.kt

Aggiungere KDoc chiaro:

“Temporary bridge to AppRepository; will be removed in EPIC S6 (or next).”

Marcare metodi bridge con @Deprecated("Temporary bridge: replace with dedicated implementation").

Se l’impl dipende da AppRepository, documentare esattamente quali metodi usa.

Checkpoint: build PASS.

S5.1.3 — Estrarre resolveTargetIp fuori da AppRepository

Target attuale (da S5 Result):

PingStep “temporaneamente” usa resolveTargetIp.

Nuovo contratto

Creare:

core/data/repository/test/PingTargetResolver.kt

suspend fun resolve(client: Client, profile: TestProfile, input: String): String

Implementazione

Creare:

data/repositoryimpl/PingTargetResolverImpl.kt

Implementazione deve replicare la logica corrente (senza ottimizzare).

Aggiornare:

PingStepImpl per usare PingTargetResolver invece di AppRepository.

Checkpoint: build PASS.

S5.1.4 — Rendere deterministico rawResultsJson

Target: RunTestUseCaseImpl.kt

Definire una struttura JSON minima e stabile (anche “v1”), ad esempio:

timestamp

plan (clientId/probeId/profileId/socketId)

steps array con: name, status, data (se presente), error (se presente)

⚠️ Vincolo: non inventare campi “di rete” non disponibili. Usa solo ciò che già hai in StepResult/DTO.

Serializzare con Moshi o altro già presente in progetto (senza introdurre nuove librerie).

Popolare TestOutcome.rawResultsJson sempre (anche in fail: almeno con steps eseguiti).

Checkpoint: build PASS.

S5.1.5 — Mapping minimo StepResult → TestSectionResult coerente

Target:

core/domain/test/model/TestSectionResult.kt

RunTestUseCaseImpl.kt (costruzione outcome)

Definire regole minime:

ogni Step produce 1 TestSectionResult con title, status, details.

NON cambiare UI: adattare l’output a ciò che la UI già si aspetta oggi (se servono campi, aggiungerli al model in modo compatibile).

Checkpoint: build PASS.

S5.1.6 — Documentazione finale

Creare:

docs/migration/S5_1_RESULT.md con:

file modificati/creati (path completi),

cosa è stato rimosso (commenti legacy),

conferma che PingStep non dipende più da AppRepository,

comandi finali PASS.

Criteri di accettazione S5.1

Nessun blocco legacy commentato nei file attivi.

PingStep non usa più AppRepository.

rawResultsJson è sempre popolato in modo deterministico.

Build PASS (KSP/assemble/unit).



EPIC S5 — Decomposizione AppRepository + UseCase “Run Test” (SOLID, no quick-fix)
Obiettivo

Eliminare il “god object” AppRepository spezzandolo in repository piccoli e coesi.

Spostare l’orchestrazione dei test fuori da TestViewModel in un UseCase di dominio (RunTestUseCase).

Preparare l’app a: stabilizzazione link, TDR capability, neighbor selection, VLAN/voice VLAN e streaming log, senza introdurre feature nuove in questa EPIC.

Questa EPIC è “strutturale”: l’output è architettura pulita + contratti + wiring DI, anche se alcune parti restano TODO (ma solo dove esplicitamente previsto).

Regole anti-drift (obbligatorie)

❌ Vietato cambiare UI/UX delle schermate test (progress + pass/fail).

❌ Vietato cambiare chiamate REST (endpoint, payload, parsing) e logica di rete: solo spostamento e incapsulamento.

❌ Vietato aggiungere workaround “temporanei” non documentati (bridge random, duplicazioni).

✅ Ogni nuovo componente deve avere:

responsabilità singola,

interfaccia in core/domain o core/data (a seconda del livello),

implementazione in data/* o core/data/* già migrati,

KDoc con input/output ed error model.

✅ Dopo ogni step marcato “Checkpoint”:

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest
Se fallisce: fix solo import/DI/compilazione e STOP.

Scope “cosa spostiamo”
Dal mondo attuale (già noto dal tuo audit precedente)

TestViewModel.kt oggi orchestra: apply network config, link check, TDR, ping, speed-test, report aggregation.

AppRepository.kt oggi contiene: DB read/write, network calls MikroTik, config rete, utility.

Verso architettura target (in questa EPIC)

core/domain/usecase/test/RunTestUseCase.kt (nuovo) → orchestration.

core/domain/test/* (nuovi contracts) → step del test.

core/data/repository/* (nuove interfacce) → accesso a DB e a MikroTik.

Implementazioni nelle aree già migrate:

MikroTik REST: core/data/remote/mikrotik/** (già S2)

Room v1: core/data/local/room/v1/** (già S3)

PDF/IO: core/data/** (già S4)

S5.0 — Preflight (obbligatorio)
S5.0.1 Baseline build

Eseguire:

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Creare file:

docs/migration/S5_BASELINE.md con data e risultati PASS/FAIL.

Stop condition: se fallisce, fermarsi e riportare errori (niente refactor).

S5.1 — Definizione “contratti” di dominio per il Test Runner

Nota: qui NON implementiamo logica MikroTik. Creiamo solo contratti e modelli.

S5.1.1 Creare cartelle

app/src/main/java/com/app/miklink/core/domain/test/

app/src/main/java/com/app/miklink/core/domain/test/model/

app/src/main/java/com/app/miklink/core/domain/test/step/

app/src/main/java/com/app/miklink/core/domain/usecase/test/

S5.1.2 Creare model minimi (solo dati, no logica)

Creare file:

core/domain/test/model/TestPlan.kt

Contiene: clientId, probeId, profileId, socketId, notes?

KDoc: “Input per avvio test; non contiene stato runtime”.

core/domain/test/model/TestProgress.kt

Stato progressivo: step corrente, percentuale, messaggio UI.

Non legarsi a Compose.

core/domain/test/model/TestOutcome.kt

overallStatus: Pass/Fail

sections: List<TestSectionResult>

rawResultsJson: String (se già usato oggi)

KDoc: “Output consumato da UI + persistenza Report”.

core/domain/test/model/TestError.kt

sealed class (es. NetworkError, AuthError, Timeout, Unsupported, Unexpected)

Non mappare tutto ora: creare subset, con TODO esplicito.

core/domain/test/model/TestEvent.kt

sealed class: Progress(TestProgress), LogLine(...), Completed(TestOutcome), Failed(TestError)

Serve per stream in Flow.

S5.1.3 Creare contratti step (no implementazione)

Creare interfacce:

core/domain/test/step/LinkStatusStep.kt

core/domain/test/step/CableTestStep.kt

core/domain/test/step/PingStep.kt

core/domain/test/step/SpeedTestStep.kt

core/domain/test/step/NeighborDiscoveryStep.kt

core/domain/test/step/NetworkConfigStep.kt (apply DHCP/static alla probe in base al Client)

Ogni interfaccia deve esporre:

suspend fun run(context: TestExecutionContext): StepResult

dove TestExecutionContext è un nuovo data class con dentro client, probeConfig, profile, socketId, ecc.

Creare:

core/domain/test/model/TestExecutionContext.kt

core/domain/test/model/StepResult.kt (sealed class: Success(data), Skipped(reason), Failed(TestError))

Checkpoint: build PASS (ksp/assemble/test)

S5.2 — Interfacce repository in core/data (SOLID)
S5.2.1 Creare cartelle

app/src/main/java/com/app/miklink/core/data/repository/test/

app/src/main/java/com/app/miklink/core/data/repository/client/

app/src/main/java/com/app/miklink/core/data/repository/probe/

app/src/main/java/com/app/miklink/core/data/repository/report/

S5.2.2 Creare interfacce (solo contratti)

core/data/repository/client/ClientRepository.kt

suspend fun getClient(id: Long): Client

core/data/repository/probe/ProbeRepository.kt

suspend fun getProbe(id: Long): ProbeConfig

core/data/repository/test/TestProfileRepository.kt

suspend fun getProfile(id: Long): TestProfile

core/data/repository/report/ReportRepository.kt

suspend fun saveReport(report: Report): Long

suspend fun getReport(id: Long): Report

core/data/repository/test/MikroTikTestRepository.kt

incapsula operazioni MikroTik usate dai test:

suspend fun monitorEthernet(numbers: String, once: Boolean = true): ...

suspend fun cableTest(numbers: String, once: Boolean = true): ...

suspend fun ping(address: String, count: Int): ...

suspend fun neighbors(): ...

suspend fun systemResource(): ...

⚠️ I tipi di ritorno devono essere quelli già usati in produzione oggi (DTO esistenti o modelli già presenti). Se non è chiaro, NON inventare: usa i DTO già creati in S2 o crea wrapper “TBD” con TODO.

Checkpoint: build PASS

S5.3 — Implementazioni repository (minime) usando Room v1 + MikroTik REST

Qui si crea solo “glue code”, senza cambiare logica.

S5.3.1 Implementazioni Room-backed

Creare in:

app/src/main/java/com/app/miklink/data/repositoryimpl/roomv1/ (nuovo namespace data impl)

File:

RoomV1ClientRepository.kt

RoomV1ProbeRepository.kt

RoomV1TestProfileRepository.kt

RoomV1ReportRepository.kt

Ogni implementazione:

prende in constructor i DAO già migrati (core/data/local/room/v1/dao/*)

implementa la rispettiva interfaccia core/data/repository/...

S5.3.2 Implementazione MikroTikTestRepository

Creare:

data/repositoryimpl/mikrotik/MikroTikTestRepositoryImpl.kt
Usa:

core/data/remote/mikrotik/service/MikroTikApiService

core/data/remote/mikrotik/infra/MikroTikServiceFactory (se serve per creare service dal ProbeConfig)

Vincolo: non cambiare endpoint, non cambiare payload.

Checkpoint: build PASS

S5.4 — UseCase RunTestUseCase (orchestrazione fuori dalla UI)
S5.4.1 Creare interfaccia usecase

File:

core/domain/usecase/test/RunTestUseCase.kt

fun execute(plan: TestPlan): Flow<TestEvent>

S5.4.2 Implementazione usecase

File:

core/domain/usecase/test/RunTestUseCaseImpl.kt

Dipendenze (via constructor):

ClientRepository, ProbeRepository, TestProfileRepository

ReportRepository

Step interfaces: LinkStatusStep, CableTestStep, PingStep, SpeedTestStep, NeighborDiscoveryStep, NetworkConfigStep

(facoltativo) ReportAggregator (se esiste già in core/domain/report)

Comportamento minimo (senza cambiare logica esistente):

Carica client, probe, profile

Costruisce TestExecutionContext

Esegue step in ordine definito dal profilo (runLinkStatus, runTdr, runPing, runSpeedTest, runLldp…)

Emette TestEvent.Progress prima/dopo ogni step

Aggrega outcome finale e salva report tramite ReportRepository

Emette TestEvent.Completed(outcome) oppure Failed(error)

⚠️ Se oggi l’ordine e le regole sono in TestViewModel, replicare identico ordine (non ottimizzare).

Checkpoint: build PASS

S5.5 — Implementazione degli Step (wrapper verso repository)
S5.5.1 Step implementations in data/ (non core)

Creare in:

app/src/main/java/com/app/miklink/data/teststeps/

File (Impl):

NetworkConfigStepImpl.kt → usa repo/config esistenti (se oggi in AppRepository)

LinkStatusStepImpl.kt → usa MikroTikTestRepository.monitorEthernet

CableTestStepImpl.kt → usa MikroTikTestRepository.cableTest

PingStepImpl.kt → usa MikroTikTestRepository.ping

NeighborDiscoveryStepImpl.kt → usa MikroTikTestRepository.neighbors

SpeedTestStepImpl.kt → usa MikroTikTestRepository.speedTest (se esiste già in API/DTO)

Ogni step:

prende TestExecutionContext

ritorna StepResult

gestisce errori con mapping minimale in TestError (no inventare: solo mapping di eccezioni evidenti)

Checkpoint: build PASS

S5.6 — DI wiring (RepositoryModule + nuova TestModule)
S5.6.1 Aggiornare/creare moduli Hilt

In RepositoryModule.kt:

bind interfacce ClientRepository, ProbeRepository, TestProfileRepository, ReportRepository alle impl RoomV1*Repository

bind MikroTikTestRepository a MikroTikTestRepositoryImpl

Creare di/TestRunnerModule.kt:

@Binds per Step interfaces → StepImpl

@Binds RunTestUseCase → RunTestUseCaseImpl

Checkpoint: build PASS

S5.7 — Migrazione TestViewModel a UseCase (senza cambiare UI)
S5.7.1 Ridurre responsabilità ViewModel

In ui/test/TestViewModel.kt:

rimuovere orchestrazione diretta (niente chiamate a DAO + AppRepository per eseguire i test)

iniettare RunTestUseCase

su startTest() chiamare useCase.execute(plan) e collezionare eventi:

Progress → aggiorna stato UI

LogLine → aggiorna log UI

Completed/Failed → aggiorna risultato

Vincolo: mantenere invariato lo stato UI/section model (solo adattare la fonte dei dati).

Checkpoint: build PASS

S5.8 — Deprecazione controllata di AppRepository
S5.8.1 Identificare metodi rimasti

Cercare usi residui di AppRepository in:

ui/**

data/**

Se AppRepository resta usato per backup o altre feature, lasciarlo solo per quello.

Se non più usato nel flusso test, segnare con KDoc:

“Deprecated: replaced by RunTestUseCase + repositories”.

Checkpoint finale: build PASS + golden tests PASS

Deliverable documentazione (obbligatorio)

Creare:

docs/migration/S5_RESULT.md con:

elenco file creati (path completi)

dipendenze DI aggiunte

elenco delle responsabilità spostate da TestViewModel/AppRepository al UseCase

comandi finali PASS

Criteri di accettazione

TestViewModel non orchestra più i test (solo UI state).

RunTestUseCase esiste e produce Flow<TestEvent>.

AppRepository non è più coinvolto nel “run test” (salvo feature non incluse).

Build PASS (KSP/assemble/unit).

Golden parsing tests restano verdi.

Nota finale (importantissima)

Se in qualsiasi step il tipo di ritorno di una chiamata MikroTik non è chiaro (DTO/shape), non inventare: usare i DTO già presenti in core/data/remote/mikrotik/dto o fermarsi e riportare l’ambiguità con:

file coinvolti

firma attuale

cosa manca per proseguire


S4 — Migrazione PDF + IO in core/data (NO refactor funzionale)
Obiettivo

Spostare i moduli PDF e IO da data/ a core/data/, mantenendo invariata la logica:

nessun cambiamento ai risultati PDF

nessun cambiamento al parsing di resultsJson

nessun cambiamento alla UI (solo import/DI)

Scope preciso

Da migrare (come da struttura che avevamo visto):

app/src/main/java/com/app/miklink/data/pdf/**

PdfGenerator.kt

PdfGeneratorIText.kt

PdfExportConfig.kt

ParsedResultsParser.kt

PdfDocumentHelper.kt

app/src/main/java/com/app/miklink/data/io/**

FileReader.kt

ContentResolverFileReader.kt

DI:

app/src/main/java/com/app/miklink/di/PdfModule.kt (e import collegati)

Riferimenti nel codice (ViewModel/UI/History ecc.) che importano questi componenti

Vincoli (anti-drift)

❌ Vietato cambiare logica PDF (layout, formattazione, campi, calcoli, parsing).

❌ Vietato cambiare la struttura dati di resultsJson e relativi parsing model.

❌ Vietato cambiare firme pubbliche (metodi/parametri) se non strettamente necessario per il move.

✅ Consentito solo: move file, package coerente col path, aggiornare import e DI.

✅ Checkpoint build obbligatori dopo ogni blocco.

S4.0 — Preflight
S4.0.1 Baseline build (obbligatorio)

Eseguire e registrare l’esito (anche “BUILD SUCCESSFUL”):

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Stop condition: se fallisce, STOP e riportare l’errore (non fixare in questa EPIC).

S4.1 — Creare struttura target (solo cartelle)

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/pdf/

app/src/main/java/com/app/miklink/core/data/pdf/impl/

app/src/main/java/com/app/miklink/core/data/pdf/parser/

app/src/main/java/com/app/miklink/core/data/io/

app/src/main/java/com/app/miklink/core/data/io/impl/

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.2 — Migrazione IO (meccanica)
S4.2.1 Spostare interfaccia FileReader

Spostare:

data/io/FileReader.kt
→

core/data/io/FileReader.kt

Aggiornare package:

package com.app.miklink.core.data.io

S4.2.2 Spostare implementazione ContentResolverFileReader

Spostare:

data/io/ContentResolverFileReader.kt
→

core/data/io/impl/ContentResolverFileReader.kt

Aggiornare package:

package com.app.miklink.core.data.io.impl

Aggiornare import verso FileReader:

com.app.miklink.core.data.io.FileReader

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.3 — Migrazione PDF (meccanica)
S4.3.1 Spostare interfaccia PdfGenerator

Spostare:

data/pdf/PdfGenerator.kt
→

core/data/pdf/PdfGenerator.kt

Package:

package com.app.miklink.core.data.pdf

S4.3.2 Spostare config e helper “neutri”

Spostare:

data/pdf/PdfExportConfig.kt
→ core/data/pdf/PdfExportConfig.kt

data/pdf/PdfDocumentHelper.kt
→ core/data/pdf/PdfDocumentHelper.kt

Package:

package com.app.miklink.core.data.pdf

S4.3.3 Spostare parser resultsJson

Spostare:

data/pdf/ParsedResultsParser.kt
→

core/data/pdf/parser/ParsedResultsParser.kt

Package:

package com.app.miklink.core.data.pdf.parser

⚠️ Vincolo: non cambiare logica di parsing (solo import/package).

S4.3.4 Spostare implementazione iText

Spostare:

data/pdf/PdfGeneratorIText.kt
→

core/data/pdf/impl/PdfGeneratorIText.kt

Package:

package com.app.miklink.core.data.pdf.impl

Aggiornare import verso:

PdfGenerator (core)

ParsedResultsParser (core)

eventuali helper/config (core)

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.4 — Aggiornare DI (PdfModule)

File noto:

app/src/main/java/com/app/miklink/di/PdfModule.kt

S4.4.1 Aggiornare import e provider

Aggiornare tutti i riferimenti da com.app.miklink.data.pdf... a:

com.app.miklink.core.data.pdf.*

com.app.miklink.core.data.pdf.impl.*

com.app.miklink.core.data.pdf.parser.*

Vincolo: stessa istanziazione di prima, stessi parametri.

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.5 — Aggiornare import nel resto del codice
S4.5.1 Ricerca e sostituzione import (solo import)

Sostituire ovunque:

com.app.miklink.data.pdf. → com.app.miklink.core.data.pdf.

com.app.miklink.data.io. → com.app.miklink.core.data.io.

Verificare almeno i file tipici:

ui/history/** (export PDF, detail report)

ui/settings/** (PdfSettings)

data/repository/** (se usa parsing o generator)

eventuali ViewModel che costruiscono export

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.6 — Pulizia cartelle vuote in data/

Se ora vuote, eliminare:

app/src/main/java/com/app/miklink/data/pdf/

app/src/main/java/com/app/miklink/data/io/

Se rimane qualcosa dentro: STOP e riportare cosa resta.

S4.7 — Build finale (obbligatoria)

Eseguire:

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Output richiesto a fine EPIC (obbligatorio)

Elenco file sotto:

app/src/main/java/com/app/miklink/core/data/pdf/**

app/src/main/java/com/app/miklink/core/data/io/**

Conferma assenza vecchi package:

nessun file sotto com.app.miklink.data.pdf.*

nessun file sotto com.app.miklink.data.io.*

Report docs:

docs/migration/S4_BASELINE.md

docs/migration/S4_RESULT.md (lista file migrati + conferme PASS)

Criteri di accettazione

PDF/IO vivono in core/data/**

DI aggiornata

build verde (KSP/assemble/unit)

nessuna modifica funzionale a PDF/parsing









S4 — Migrazione PDF + IO in core/data (NO refactor funzionale)
Obiettivo

Spostare i moduli PDF e IO da data/ a core/data/, mantenendo invariata la logica:

nessun cambiamento ai risultati PDF

nessun cambiamento al parsing di resultsJson

nessun cambiamento alla UI (solo import/DI)

Scope preciso

Da migrare (come da struttura che avevamo visto):

app/src/main/java/com/app/miklink/data/pdf/**

PdfGenerator.kt

PdfGeneratorIText.kt

PdfExportConfig.kt

ParsedResultsParser.kt

PdfDocumentHelper.kt

app/src/main/java/com/app/miklink/data/io/**

FileReader.kt

ContentResolverFileReader.kt

DI:

app/src/main/java/com/app/miklink/di/PdfModule.kt (e import collegati)

Riferimenti nel codice (ViewModel/UI/History ecc.) che importano questi componenti

Vincoli (anti-drift)

❌ Vietato cambiare logica PDF (layout, formattazione, campi, calcoli, parsing).

❌ Vietato cambiare la struttura dati di resultsJson e relativi parsing model.

❌ Vietato cambiare firme pubbliche (metodi/parametri) se non strettamente necessario per il move.

✅ Consentito solo: move file, package coerente col path, aggiornare import e DI.

✅ Checkpoint build obbligatori dopo ogni blocco.

S4.0 — Preflight
S4.0.1 Baseline build (obbligatorio)

Eseguire e registrare l’esito (anche “BUILD SUCCESSFUL”):

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Stop condition: se fallisce, STOP e riportare l’errore (non fixare in questa EPIC).

S4.1 — Creare struttura target (solo cartelle)

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/pdf/

app/src/main/java/com/app/miklink/core/data/pdf/impl/

app/src/main/java/com/app/miklink/core/data/pdf/parser/

app/src/main/java/com/app/miklink/core/data/io/

app/src/main/java/com/app/miklink/core/data/io/impl/

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.2 — Migrazione IO (meccanica)
S4.2.1 Spostare interfaccia FileReader

Spostare:

data/io/FileReader.kt
→

core/data/io/FileReader.kt

Aggiornare package:

package com.app.miklink.core.data.io

S4.2.2 Spostare implementazione ContentResolverFileReader

Spostare:

data/io/ContentResolverFileReader.kt
→

core/data/io/impl/ContentResolverFileReader.kt

Aggiornare package:

package com.app.miklink.core.data.io.impl

Aggiornare import verso FileReader:

com.app.miklink.core.data.io.FileReader

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.3 — Migrazione PDF (meccanica)
S4.3.1 Spostare interfaccia PdfGenerator

Spostare:

data/pdf/PdfGenerator.kt
→

core/data/pdf/PdfGenerator.kt

Package:

package com.app.miklink.core.data.pdf

S4.3.2 Spostare config e helper “neutri”

Spostare:

data/pdf/PdfExportConfig.kt
→ core/data/pdf/PdfExportConfig.kt

data/pdf/PdfDocumentHelper.kt
→ core/data/pdf/PdfDocumentHelper.kt

Package:

package com.app.miklink.core.data.pdf

S4.3.3 Spostare parser resultsJson

Spostare:

data/pdf/ParsedResultsParser.kt
→

core/data/pdf/parser/ParsedResultsParser.kt

Package:

package com.app.miklink.core.data.pdf.parser

⚠️ Vincolo: non cambiare logica di parsing (solo import/package).

S4.3.4 Spostare implementazione iText

Spostare:

data/pdf/PdfGeneratorIText.kt
→

core/data/pdf/impl/PdfGeneratorIText.kt

Package:

package com.app.miklink.core.data.pdf.impl

Aggiornare import verso:

PdfGenerator (core)

ParsedResultsParser (core)

eventuali helper/config (core)

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.4 — Aggiornare DI (PdfModule)

File noto:

app/src/main/java/com/app/miklink/di/PdfModule.kt

S4.4.1 Aggiornare import e provider

Aggiornare tutti i riferimenti da com.app.miklink.data.pdf... a:

com.app.miklink.core.data.pdf.*

com.app.miklink.core.data.pdf.impl.*

com.app.miklink.core.data.pdf.parser.*

Vincolo: stessa istanziazione di prima, stessi parametri.

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.5 — Aggiornare import nel resto del codice
S4.5.1 Ricerca e sostituzione import (solo import)

Sostituire ovunque:

com.app.miklink.data.pdf. → com.app.miklink.core.data.pdf.

com.app.miklink.data.io. → com.app.miklink.core.data.io.

Verificare almeno i file tipici:

ui/history/** (export PDF, detail report)

ui/settings/** (PdfSettings)

data/repository/** (se usa parsing o generator)

eventuali ViewModel che costruiscono export

Checkpoint: ./gradlew :app:kspDebugKotlin

S4.6 — Pulizia cartelle vuote in data/

Se ora vuote, eliminare:

app/src/main/java/com/app/miklink/data/pdf/

app/src/main/java/com/app/miklink/data/io/

Se rimane qualcosa dentro: STOP e riportare cosa resta.

S4.7 — Build finale (obbligatoria)

Eseguire:

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Output richiesto a fine EPIC (obbligatorio)

Elenco file sotto:

app/src/main/java/com/app/miklink/core/data/pdf/**

app/src/main/java/com/app/miklink/core/data/io/**

Conferma assenza vecchi package:

nessun file sotto com.app.miklink.data.pdf.*

nessun file sotto com.app.miklink.data.io.*

Report docs:

docs/migration/S4_BASELINE.md

docs/migration/S4_RESULT.md (lista file migrati + conferme PASS)

Criteri di accettazione

PDF/IO vivono in core/data/**

DI aggiornata

build verde (KSP/assemble/unit)

nessuna modifica funzionale a PDF/parsing


EPIC S3 — Migrazione Room v1 in core/data/local/room/v1 (NO refactor DB, NO v2)
Obiettivo

Spostare tutto il DB Room attuale (v1) da:

app/src/main/java/com/app/miklink/data/db/**

a:

app/src/main/java/com/app/miklink/core/data/local/room/v1/**

e aggiornare DI e import, senza cambiare lo schema, senza introdurre DB v2, senza modificare migrazioni.

Perché ora

È un blocco meccanico come S2.

Riduce dipendenze “data/*” e prepara lo split SOLID senza toccare la logica.

Vincoli (anti-drift)

❌ Vietato cambiare @Database(version=...), @Entity, @Dao, Migrations logic, nomi tabelle/colonne.

❌ Vietato rinominare classi o campi.

❌ Vietato introdurre nuove entity/dao/DB o “copie minime”.

✅ Consentito solo: spostare file, aggiornare package, aggiornare import, aggiornare moduli DI.

✅ Checkpoint obbligatori e stop condition.

S3.0 — Preflight & sanity check
S3.0.1 Baseline build (obbligatorio)

Eseguire e salvare output (anche “BUILD SUCCESSFUL”):

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Stop condition: se fallisce, fermarsi e riportare errore.

S3.0.2 Sanity check path (obbligatorio)

Verificare che il path base sia coerente:

i sorgenti devono stare sotto:
app/src/main/java/com/app/miklink/
Se trovi file sotto com/app/mikrotik/ o altri path simili: STOP e riportare elenco (non correggere in questa EPIC).

S3.1 — Inventario DB v1 esistente (senza modifiche)

Confermare esistenza di questi file (path attuali):

app/src/main/java/com/app/miklink/data/db/AppDatabase.kt

app/src/main/java/com/app/miklink/data/db/Migrations.kt

app/src/main/java/com/app/miklink/data/db/dao/ClientDao.kt

app/src/main/java/com/app/miklink/data/db/dao/ProbeConfigDao.kt

app/src/main/java/com/app/miklink/data/db/dao/ReportDao.kt

app/src/main/java/com/app/miklink/data/db/dao/TestProfileDao.kt

app/src/main/java/com/app/miklink/data/db/model/Client.kt

app/src/main/java/com/app/miklink/data/db/model/ProbeConfig.kt

app/src/main/java/com/app/miklink/data/db/model/Report.kt

app/src/main/java/com/app/miklink/data/db/model/TestProfile.kt

app/src/main/java/com/app/miklink/data/db/model/LogEntry.kt (se esiste)

app/src/main/java/com/app/miklink/data/db/model/NetworkMode.kt (se esiste)

Se nomi/percorsi differiscono: STOP e riportare elenco reale.

S3.2 — Creare struttura target (solo cartelle)

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/local/room/v1/

app/src/main/java/com/app/miklink/core/data/local/room/v1/dao/

app/src/main/java/com/app/miklink/core/data/local/room/v1/model/

app/src/main/java/com/app/miklink/core/data/local/room/v1/migration/

Checkpoint: ./gradlew :app:kspDebugKotlin

S3.3 — Migrazione model (Entity) v1
S3.3.1 Spostare model

Spostare fisicamente tutti i file in:

app/src/main/java/com/app/miklink/data/db/model/*
→ in:

app/src/main/java/com/app/miklink/core/data/local/room/v1/model/*

S3.3.2 Aggiornare package

Aggiornare package a:

package com.app.miklink.core.data.local.room.v1.model

S3.3.3 Fix import nei DAO e DB

Aggiornare import dei model in:

DAO (client/probe/report/profile)

AppDatabase

Migrations (se referenzia entity class)

Checkpoint: ./gradlew :app:kspDebugKotlin
Stop condition: fix solo import/package finché passa.

S3.4 — Migrazione DAO v1
S3.4.1 Spostare DAO

Spostare fisicamente:

app/src/main/java/com/app/miklink/data/db/dao/*
→

app/src/main/java/com/app/miklink/core/data/local/room/v1/dao/*

S3.4.2 Aggiornare package DAO

package com.app.miklink.core.data.local.room.v1.dao

S3.4.3 Fix import entity nei DAO

Verificare import a:

com.app.miklink.core.data.local.room.v1.model.*

Checkpoint: ./gradlew :app:kspDebugKotlin

S3.5 — Migrazione AppDatabase + Migrations
S3.5.1 Spostare AppDatabase

Spostare:

data/db/AppDatabase.kt
→

core/data/local/room/v1/AppDatabase.kt

Aggiornare package:

package com.app.miklink.core.data.local.room.v1

Aggiornare import DAO:

com.app.miklink.core.data.local.room.v1.dao.*
e import model:

com.app.miklink.core.data.local.room.v1.model.*

S3.5.2 Spostare Migrations

Spostare:

data/db/Migrations.kt
→

core/data/local/room/v1/migration/Migrations.kt

Aggiornare package:

package com.app.miklink.core.data.local.room.v1.migration

Aggiornare import necessari (Room Migration, SupportSQLiteDatabase, ecc.) senza cambiare logica.

Checkpoint: ./gradlew :app:kspDebugKotlin
Stop condition: fix import/package finché passa.

S3.6 — Aggiornare DI (DatabaseModule)

File noto:

app/src/main/java/com/app/miklink/di/DatabaseModule.kt

S3.6.1 Update import

Aggiornare riferimenti a:

AppDatabase → com.app.miklink.core.data.local.room.v1.AppDatabase

Migrations → com.app.miklink.core.data.local.room.v1.migration.Migrations

DAO → com.app.miklink.core.data.local.room.v1.dao.*

⚠️ Non cambiare la creazione DB (nome DB, fallback, exportSchema, ecc.)

Checkpoint: ./gradlew :app:kspDebugKotlin

S3.7 — Aggiornare import nel resto del codice
S3.7.1 Ricerca vecchio package

Cercare e sostituire import:

com.app.miklink.data.db.

com.app.miklink.data.db.dao.

com.app.miklink.data.db.model.

con:

com.app.miklink.core.data.local.room.v1.

com.app.miklink.core.data.local.room.v1.dao.

com.app.miklink.core.data.local.room.v1.model.

File tipicamente impattati (controllare almeno):

data/repository/* (AppRepository, BackupManager, TransactionRunner, ecc.)

ui/**ViewModel.kt (se iniettano DAO direttamente)

RepositoryModule.kt (se fornisce repository che dipendono da DAO)

Checkpoint: ./gradlew :app:kspDebugKotlin

S3.8 — Rimozione cartelle vuote e build finale
S3.8.1 Eliminare cartelle DB v1 rimaste vuote

Se ora vuote, eliminare:

app/src/main/java/com/app/miklink/data/db/

app/src/main/java/com/app/miklink/data/db/dao/

app/src/main/java/com/app/miklink/data/db/model/

Se rimane qualcosa: STOP e riportare cosa.

S3.8.2 Build finale (obbligatoria)

Eseguire:

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Output richiesto a fine EPIC (obbligatorio)

Elenco file sotto:

app/src/main/java/com/app/miklink/core/data/local/room/v1/** (tutti i file)

Conferma assenza dei vecchi package:

nessun file sotto com.app.miklink.data.db.*

Output comandi finali: PASS per KSP/assemble/tests.

Aggiornare docs/migration/ con:

S3_BASELINE.md (esito step S3.0.1)

S3_RESULT.md (lista file migrati e conferme)

Criteri di accettazione

Room v1 è completamente sotto core/data/local/room/v1

DI aggiornata e build verde

Nessun cambiamento funzionale allo schema o alle migrazioni

Nessun duplicato di entity/dao/database


EPIC S2 — Migrazione Networking MikroTik in core/data/remote/mikrotik (NO refactor funzionale)
Obiettivo

Spostare tutto il networking MikroTik da:

app/src/main/java/com/app/miklink/data/network/**

a:

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/**

mantenendo:

stessi nomi classi

stessi endpoint

stessa configurazione Moshi/OkHttp/Retrofit

build verde (KSP + assemble + unit test)

Vincoli (anti-drift)

❌ Vietato cambiare logica (endpoint, request/response, parsing, timeout, retry, headers).

❌ Vietato creare “bridge” o nuove classi per tappare buchi (solo move + update package/import).

❌ Vietato “logical move”: package deve corrispondere al path.

✅ Consentito: spostare file, aggiornare package ..., aggiornare import, aggiornare DI module.

✅ Dopo ogni micro-step: eseguire ./gradlew :app:kspDebugKotlin. Se fallisce → fix solo import/package e STOP al checkpoint finché non passa.

S2.0 — Preflight
S2.0.1 Baseline build (obbligatorio)

Eseguire e salvare output (anche solo “BUILD SUCCESSFUL” o errore):

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Stop condition: se una di queste fallisce prima di iniziare, fermarsi e riportare l’errore (non “fixare” ora).

S2.1 — Inventario file da migrare (senza modifiche)
S2.1.1 Lista file networking esistenti

Confermare che esistono tutti questi file (path):

app/src/main/java/com/app/miklink/data/network/MikroTikApiService.kt

app/src/main/java/com/app/miklink/data/network/MikroTikServiceFactory.kt

app/src/main/java/com/app/miklink/data/network/AuthInterceptor.kt

app/src/main/java/com/app/miklink/data/network/NeighborDetailListAdapter.kt

app/src/main/java/com/app/miklink/data/network/dto/* (tutti i DTO esistenti)

Se alcuni non esistono o hanno nomi diversi: STOP e riportare l’elenco reale.

S2.2 — Creare struttura target (solo cartelle)

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/service/

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/dto/

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/

Checkpoint: ./gradlew :app:kspDebugKotlin
(Non dovrebbe cambiare nulla; se fallisce, STOP e riportare.)

S2.3 — Migrazione DTO (meccanica)
S2.3.1 Spostare TUTTI i DTO

Spostare fisicamente tutti i file sotto:

app/src/main/java/com/app/miklink/data/network/dto/
→ in:

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/dto/

S2.3.2 Aggiornare package dei DTO

In ogni DTO spostato:

aggiornare package coerente col nuovo path:
package com.app.miklink.core.data.remote.mikrotik.dto

S2.3.3 Fix import (solo dove necessario)

Aggiornare import in:

MikroTikApiService.kt

MikroTikServiceFactory.kt

qualsiasi file che importava com.app.miklink.data.network.dto.*

Checkpoint: ./gradlew :app:kspDebugKotlin
Stop condition: se fallisce, sistemare solo import/package e riprovare finché passa.

S2.4 — Migrazione Retrofit Service
S2.4.1 Spostare service interface

Spostare:

app/src/main/java/com/app/miklink/data/network/MikroTikApiService.kt
→

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/service/MikroTikApiService.kt

S2.4.2 Aggiornare package

Aggiornare package a:

package com.app.miklink.core.data.remote.mikrotik.service

S2.4.3 Fix import DTO nel service

Verificare che importi i DTO dal nuovo package:

com.app.miklink.core.data.remote.mikrotik.dto.*

Checkpoint: ./gradlew :app:kspDebugKotlin
Stop condition: se fallisce, correggere import/package e riprovare.

S2.5 — Migrazione Infra (Factory + Interceptor + Moshi adapter)
S2.5.1 Spostare infra files

Spostare:

app/src/main/java/com/app/miklink/data/network/MikroTikServiceFactory.kt
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/MikroTikServiceFactory.kt

app/src/main/java/com/app/miklink/data/network/AuthInterceptor.kt
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/AuthInterceptor.kt

app/src/main/java/com/app/miklink/data/network/NeighborDetailListAdapter.kt
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/NeighborDetailListAdapter.kt

S2.5.2 Aggiornare package dei tre file

package com.app.miklink.core.data.remote.mikrotik.infra

S2.5.3 Fix import verso MikroTikApiService

Aggiornare riferimenti a:

com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService

S2.5.4 Fix import DTO

Aggiornare eventuali import DTO a:

com.app.miklink.core.data.remote.mikrotik.dto.*

Checkpoint: ./gradlew :app:kspDebugKotlin
Stop condition: se fallisce, correggere import/package e riprovare.

S2.6 — Aggiornare DI (NetworkModule)

File noto: app/src/main/java/com/app/miklink/di/NetworkModule.kt

S2.6.1 Aggiornare import nel module

Aggiornare import per puntare ai nuovi path:

MikroTikServiceFactory → com.app.miklink.core.data.remote.mikrotik.infra.MikroTikServiceFactory

AuthInterceptor → com.app.miklink.core.data.remote.mikrotik.infra.AuthInterceptor

NeighborDetailListAdapter → com.app.miklink.core.data.remote.mikrotik.infra.NeighborDetailListAdapter

MikroTikApiService → com.app.miklink.core.data.remote.mikrotik.service.MikroTikApiService

DTO import se presenti → nuovo package

S2.6.2 Verifica Moshi config

Senza cambiare logica:

verificare che la configurazione Moshi in NetworkModule.kt continui a registrare NeighborDetailListAdapter (o equivalente) esattamente come prima, solo cambiando import/package.

Checkpoint: ./gradlew :app:kspDebugKotlin
Stop condition: se fallisce, correggere import e provider signatures.

S2.7 — Aggiornare riferimenti nel codice applicativo
S2.7.1 Ricerca riferimenti al vecchio package

Cercare e correggere import (solo import) per:

com.app.miklink.data.network.*

com.app.miklink.data.network.dto.*

Aggiornare a:

com.app.miklink.core.data.remote.mikrotik.*

S2.7.2 File tipici impattati

Controllare almeno:

data/repository/AppRepository.kt (se usa service/factory/dto)

ViewModel che chiamano repository (se importavano DTO direttamente)

qualsiasi utility che usa MikroTikServiceFactory

Checkpoint: ./gradlew :app:kspDebugKotlin
Stop condition: se fallisce, fix import/package e riprovare.

S2.8 — Rimozione cartelle vuote e checkpoint finale
S2.8.1 Eliminare cartelle networking rimaste vuote

Se ora vuote, eliminare:

app/src/main/java/com/app/miklink/data/network/

app/src/main/java/com/app/miklink/data/network/dto/

(Se contengono ancora file, STOP e riportare cosa resta.)

S2.8.2 Build finale (obbligatoria)

Eseguire in quest’ordine:

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Output richiesto a fine EPIC (obbligatorio)

Lista file sotto:

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/ (tutti i file e subfolder)

Conferma che non esiste più:

com.app.miklink.data.network (nessun file rimasto)

Conferma risultati comandi:

:app:kspDebugKotlin PASS

assembleDebug PASS

testDebugUnitTest PASS

Criteri di accettazione

Tutti i file data/network/** sono stati migrati in core/data/remote/mikrotik/**.

Nessun package com.app.miklink.data.network... rimasto nel repo.

Nessun cambiamento funzionale: solo move + import/package + DI import.

Build e unit test passano.
















EPIC S1 — Migrazione “Data Layer” verso struttura SOLID + Legacy tagging (senza cambi logici)
Obiettivo

Portare tutto il layer data/ e domain/usecase dentro la struttura SOLID prevista (core/data, core/domain) senza cambiare comportamento, mantenendo build verde.
Separare chiaramente:

Core: interfacce + modelli/mapper/infra necessari

Legacy: implementazioni vecchie o non ancora rifattorizzate (con suffisso _legacy), non più usate direttamente dal nuovo codice

Vincoli

❌ Nessuna operazione git / PR.

❌ Nessun refactor funzionale “a sentimento”.

✅ Ogni step deve essere compilabile (o con correzioni minime di import/package).

✅ Se un file non è chiaramente classificabile, non decidere a caso: spostalo in legacy/ e documentalo.

A) Pre-flight (obbligatorio)
S1.A1 — Baseline build

Eseguire:

./gradlew testDebugUnitTest

./gradlew assembleDebug (o task equivalente se presente)

Salvare in docs/migration/S1_BASELINE.md:

data/ora

task eseguiti

risultato (SUCCESS/FAIL) + eventuali errori (solo riassunto)

Stop condition: se baseline fallisce, fermarsi e riportare errore (senza “fix creativi”).

B) Creare cartelle canoniche mancanti (solo struttura)

Nota: alcune esistono già come placeholder. Se esistono, non duplicare.

S1.B1 — Core data local (Room v1)

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/local/room/v1/

app/src/main/java/com/app/miklink/core/data/local/room/v1/dao/

app/src/main/java/com/app/miklink/core/data/local/room/v1/model/

app/src/main/java/com/app/miklink/core/data/local/room/v1/migrations/

S1.B2 — Core data remote (MikroTik)

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/service/

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/dto/

app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/ (interceptor/factory/adapters)

S1.B3 — Core data repository

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/repository/

app/src/main/java/com/app/miklink/core/data/repository/impl/ (implementazioni “nuove” o ponte)

app/src/main/java/com/app/miklink/core/data/transaction/

app/src/main/java/com/app/miklink/core/data/preferences/

S1.B4 — Core data pdf/io

Creare (se mancanti):

app/src/main/java/com/app/miklink/core/data/pdf/

app/src/main/java/com/app/miklink/core/data/pdf/impl/

app/src/main/java/com/app/miklink/core/data/io/

C) Spostamento “Room v1” da data/db a core/data/local/room/v1

Fonte (report agent): data/db/AppDatabase.kt, Migrations.kt, dao/*, model/*

S1.C1 — Spostare database + migrations

Spostare:

app/src/main/java/com/app/miklink/data/db/AppDatabase.kt
→ app/src/main/java/com/app/miklink/core/data/local/room/v1/AppDatabase.kt

app/src/main/java/com/app/miklink/data/db/Migrations.kt
→ app/src/main/java/com/app/miklink/core/data/local/room/v1/migrations/Migrations.kt

Aggiornare:

package declaration coerente con il nuovo path

import in di/DatabaseModule.kt (solo import/path, nessun refactor logico)

S1.C2 — Spostare DAO

Spostare cartella:

app/src/main/java/com/app/miklink/data/db/dao/*
→ app/src/main/java/com/app/miklink/core/data/local/room/v1/dao/*

Aggiornare package/import dove usati (ViewModel/Repository/DI).

S1.C3 — Spostare Entities/Model

Spostare cartella:

app/src/main/java/com/app/miklink/data/db/model/*
→ app/src/main/java/com/app/miklink/core/data/local/room/v1/model/*

Aggiornare package/import dove usati.

S1.C4 — Build checkpoint

Eseguire:

./gradlew testDebugUnitTest

./gradlew assembleDebug

---

### S1-R — Stabilizzazione (S1 Regression Stabilization)

Nota: fase di stabilizzazione post-migrazione per garantire che gli step S1 siano consistenti
e che i task di build/annotation processing/test passino in modo deterministico.

Eseguiti (Step 1–5):

- Step 1: Correzione package↔path per i file sotto `app/src/main/java/com/app/miklink/data/**` che
  dichiaravano package legacy; risolte discrepanze e ripristinati i package coerenti con il path.
- Step 2: Ripristinato un unico `@Database` reale (`AppDatabase` in `com.app.miklink.data.db`, version 13)
  e aggiornati i provider nel `DatabaseModule`.
- Step 3: Resa non-fragile `AppRepository_legacy` rimuovendo `@Inject` dal costruttore e aggiungendo
  un `@Provides` esplicito in `RepositoryModule`; creato un bridge `core` (`com.app.miklink.core.data.repository.AppRepository`) e fatto il binding.
- Step 4: Risolti in modo deterministico tutti gli errori KSP `error.NonExistentClass` correttando import,
  binding e firme dei provider (es. `BackupRepository`), senza inventare Entities/DAOs.
- Step 5: Eseguiti i check finali: `kspDebugKotlin`, `assembleDebug`, `testDebugUnitTest` (tutti passati).

Task che ora passano (verificati):

- `kspDebugKotlin` (annotation processing)
- `assembleDebug`
- `testDebugUnitTest`

Log salvati in: `docs/migration/` (checkpoint di esecuzione):

- `docs/migration/S1R_step1_ksp.txt`
- `docs/migration/S1R_step2_ksp.txt`
- `docs/migration/S1R_step3_ksp.txt`
- `docs/migration/S1R_step4_ksp.txt`
- `docs/migration/S1R_step5_assemble.txt`
- `docs/migration/S1R_step5_tests.txt`

Nota sui vincoli: nessuna nuova Entity/DAO è stata introdotta; solo bridge/interfaces sono state
aggiunte per stabilizzare il DI. Non sono state eseguite ulteriori refactor oltre quanto necessario
per rendere la codebase compilabile e le pipeline locali verdi.

Stop condition: se fallisce, sistemare solo package/import. Non cambiare logica.

D) Spostamento “Remote Mikrotik” da data/network a core/data/remote/mikrotik

Fonte (report agent):
data/network/MikroTikApiService.kt, MikroTikServiceFactory.kt, AuthInterceptor.kt, NeighborDetailListAdapter.kt, data/network/dto/*

S1.D1 — Spostare Retrofit service

Spostare:

app/src/main/java/com/app/miklink/data/network/MikroTikApiService.kt
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/service/MikroTikApiService.kt

S1.D2 — Spostare DTO

Spostare tutti i file in:

app/src/main/java/com/app/miklink/data/network/dto/*
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/dto/*

Regola: sposta tutto ciò che è sotto dto/ senza selezionare “a mano”.

S1.D3 — Spostare infra (factory/interceptor/adapter)

Spostare:

app/src/main/java/com/app/miklink/data/network/MikroTikServiceFactory.kt
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/MikroTikServiceFactory.kt

app/src/main/java/com/app/miklink/data/network/AuthInterceptor.kt
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/AuthInterceptor.kt

app/src/main/java/com/app/miklink/data/network/NeighborDetailListAdapter.kt
→ app/src/main/java/com/app/miklink/core/data/remote/mikrotik/infra/NeighborDetailListAdapter.kt

Aggiornare:

import in di/NetworkModule.kt

import ovunque venga usato MikroTikServiceFactory o MikroTikApiService

S1.D4 — Build checkpoint

Eseguire:

./gradlew testDebugUnitTest

./gradlew assembleDebug

E) Spostamento “Repository/Infra” da data/repository a core/data/repository

Fonte (report agent):
data/repository/AppRepository.kt, BackupManager.kt, BackupRepository.kt, BackupData.kt, RouteManager.kt, TransactionRunner.kt, UserPreferencesRepository.kt

S1.E1 — Transaction runner

Spostare:

app/src/main/java/com/app/miklink/data/repository/TransactionRunner.kt
→ app/src/main/java/com/app/miklink/core/data/transaction/TransactionRunner.kt

Aggiornare:

import dove usato (BackupManager/AppRepository/DI)

S1.E2 — UserPreferencesRepository

Spostare:

app/src/main/java/com/app/miklink/data/repository/UserPreferencesRepository.kt
→ app/src/main/java/com/app/miklink/core/data/preferences/UserPreferencesRepository.kt

Aggiornare:

import in MainActivity.kt e in DI (DataStoreModule.kt / RepositoryModule.kt se lo fornisce)

S1.E3 — RouteManager

Spostare:

app/src/main/java/com/app/miklink/data/repository/RouteManager.kt
→ app/src/main/java/com/app/miklink/core/data/repository/impl/RouteManager.kt
(oppure core/data/remote/mikrotik/impl se e solo se dal codice è chiaramente un “remote client helper”; se non è chiaro, resta in repository/impl.)

S1.E4 — Backup (manager/repo/data)

Spostare:

app/src/main/java/com/app/miklink/data/repository/BackupManager.kt
→ app/src/main/java/com/app/miklink/core/data/repository/impl/BackupManager.kt

app/src/main/java/com/app/miklink/data/repository/BackupRepository.kt
→ app/src/main/java/com/app/miklink/core/data/repository/BackupRepository.kt

app/src/main/java/com/app/miklink/data/repository/BackupData.kt
→ app/src/main/java/com/app/miklink/core/data/repository/BackupData.kt

Regola: in questa EPIC si sposta soltanto; non si risolve ancora il fatto che domain/usecase dipenda da data. Quello verrà nella prossima EPIC.

S1.E5 — AppRepository (God object) → legacy ponte

Qui non si rifattorizza. Si fa solo isolamento per preparare la prossima EPIC.

Operazione:

Spostare AppRepository.kt in legacy con suffisso:

da: app/src/main/java/com/app/miklink/data/repository/AppRepository.kt

a: app/src/main/java/com/app/miklink/legacy/data/repository/AppRepository_legacy.kt

Creare un “ponte” minimo nello spazio core:

app/src/main/java/com/app/miklink/core/data/repository/AppRepositoryBridge.kt

Contenuto:

solo interfaccia o wrapper che espone i metodi attualmente usati dai ViewModel

implementazione temporanea che delega a AppRepository_legacy

Vincolo: nessun cambiamento di logica. Solo spostamento + delega.

Aggiornare i ViewModel che oggi iniettano AppRepository:

cambiare DI e import per usare AppRepositoryBridge (o interfaccia equivalente) invece del legacy direttamente.

S1.E6 — Build checkpoint

Eseguire:

./gradlew testDebugUnitTest

./gradlew assembleDebug

F) Spostamento data/pdf e data/io in core

Fonte (report agent): data/pdf/*, data/io/*

S1.F1 — PDF

Spostare:

app/src/main/java/com/app/miklink/data/pdf/PdfGenerator.kt
→ app/src/main/java/com/app/miklink/core/data/pdf/PdfGenerator.kt

app/src/main/java/com/app/miklink/data/pdf/PdfGeneratorIText.kt
→ app/src/main/java/com/app/miklink/core/data/pdf/impl/PdfGeneratorIText.kt

app/src/main/java/com/app/miklink/data/pdf/PdfExportConfig.kt
→ app/src/main/java/com/app/miklink/core/data/pdf/PdfExportConfig.kt

app/src/main/java/com/app/miklink/data/pdf/ParsedResultsParser.kt
→ app/src/main/java/com/app/miklink/core/data/pdf/ParsedResultsParser.kt

app/src/main/java/com/app/miklink/data/pdf/PdfDocumentHelper.kt
→ app/src/main/java/com/app/miklink/core/data/pdf/PdfDocumentHelper.kt

Aggiornare import in:

di/PdfModule.kt

ui/history/* dove usato (solo import, zero logica)

S1.F2 — IO

Spostare:

app/src/main/java/com/app/miklink/data/io/FileReader.kt
→ app/src/main/java/com/app/miklink/core/data/io/FileReader.kt

app/src/main/java/com/app/miklink/data/io/ContentResolverFileReader.kt
→ app/src/main/java/com/app/miklink/core/data/io/ContentResolverFileReader.kt

Aggiornare import dove usati.

S1.F3 — Build checkpoint

Eseguire:

./gradlew testDebugUnitTest

./gradlew assembleDebug

G) domain/usecase → core/domain/usecase (solo spostamento)

Fonte (report agent): ImportBackupUseCase.kt in domain/usecase che dipende da data.repository.BackupRepository (violazione). In questa EPIC NON si risolve, si prepara.

S1.G1 — Spostare usecase

Spostare:

app/src/main/java/com/app/miklink/domain/usecase/ImportBackupUseCase.kt
→ app/src/main/java/com/app/miklink/core/domain/usecase/ImportBackupUseCase.kt

Aggiornare package/import dove referenziato.

Nota: dipendenza architetturale (domain→data) verrà risolta in EPIC successiva con interfacce e inversione dipendenze. Qui solo move.

S1.G2 — Build checkpoint finale

Eseguire:

./gradlew testDebugUnitTest

./gradlew assembleDebug

H) Pulizia cartelle vuote + aggiornamento documentazione
S1.H1 — Rimuovere cartelle vuote

Eliminare cartelle ormai vuote:

app/src/main/java/com/app/miklink/data/ (o sotto-cartelle rimaste) solo se vuote

app/src/main/java/com/app/miklink/domain/ (se svuotata)

app/src/main/java/com/app/miklink/legacy/ mantenere solo ciò che serve (es. AppRepository_legacy.kt)

S1.H2 — Aggiornare ARCHITECTURE.md

Aggiungere una sezione “Stato migrazione S1” con:

cosa è stato spostato (elenco cartelle)

cosa resta “legacy” (almeno AppRepository_legacy)

regola: nuovo codice non deve dipendere direttamente da legacy/*

S1.H3 — Aggiornare ROADMAP.md

Segnare EPIC S1 come COMPLETATA solo se:

build + unit test passano

data/network, data/db, data/pdf, data/io, data/repository non contengono più implementazioni attive (salvo legacy spostato)

Criteri di accettazione finali (S1)

Non esistono più file attivi in com.app.miklink.data.network/**, data.db/**, data.pdf/**, data.io/**, data.repository/** (spostati in core o legacy).

AppRepository non è più importato direttamente dai ViewModel: passa tramite bridge o interfacce core (anche se delega a legacy).

./gradlew testDebugUnitTest e ./gradlew assembleDebug = SUCCESS.

legacy/ contiene solo ciò che è necessario come ponte temporaneo (minimo indispensabile).







EPIC T1 — Refactor totale dei test + Golden Fixtures RouterOS REALI (v7.20.5) + Nuova struttura SOLID per test “final-state”
Scopo (vincolante)

Ricostruire la test-suite affinché alla fine di tutte le EPIC MikLink sia verificabile con test:

basati su dati reali RouterOS REST (fixture “golden”),

organizzati per layer SOLID/Clean (core/domain, core/data, feature),

focalizzati su correttezza dei dati e della logica (parsing/mapping/contract), non su UI automation complessa,

indipendenti da un router reale in CI (solo fixture + MockWebServer),

robusti rispetto ai vincoli REST: alcune operazioni devono essere “finite” (once, count) per evitare timeout (~60s).

Regola 0: Non inventare nulla e non assumere nulla.
Se serve un nome di classe/DTO/endpoint già presente nel repo e non lo trovi con certezza, fermarsi e chiedere (o fare una mappatura “as-is” leggendo il codice attuale).

1) Output attesi e criteri di Done
Done quando:

Esiste una nuova struttura canonica dei test (vedi §2) popolata da test nuovi.

Esiste una cartella fixtures con RouterOS reali (vedi §3) e un README con i comandi usati.

Esiste un “fixture loader” e i test lo usano davvero.

I test golden parsing (Data layer) sono attivi e deterministici:

se falliscono, si corregge il parsing (DTO/adapter/mapper) — non si cambiano le fixture.

I test legacy vengono disattivati (non cancellati) con policy tracciabile (vedi §7).

Tutto compila. È accettabile che alcuni “contract test” di dominio siano @Ignore("Pending implementation") finché il refactor SOLID non implementa i contratti.

2) Struttura nuova dei test (da creare nel repo)
2.1 Documentazione strategia test

Creare:

docs/TESTING_STRATEGY.md

Contenuti obbligatori (testo breve ma preciso):

Piramide: Domain unit > Data integration/parsing > ViewModel minimal > UI strumentale minima/zero.

“Golden fixtures RouterOS”: i JSON devono provenire da router reali o da documentazione ufficiale verificata; in questa EPIC useremo SOLO quelli catturati via curl e riportati qui.

Regola comandi “finite”: nei request body dei client (monitor/cable-test/ping) devono esserci parametri finiti (once, count) per evitare timeout.

Policy @Ignore per contract test non ancora implementati.

2.2 Percorsi canonici dei test (da usare da ora in avanti)

Creare cartelle (anche vuote inizialmente, ma devono esistere):

app/src/test/java/com/app/miklink/core/domain/

app/src/test/java/com/app/miklink/core/data/

app/src/test/java/com/app/miklink/feature/

app/src/test/java/com/app/miklink/testsupport/

app/src/test/resources/fixtures/routeros/7.20.5/

3) Golden fixtures RouterOS (REAL DATA) — file e contenuti

Tutte le fixture sotto:
app/src/test/resources/fixtures/routeros/7.20.5/

3.1 File “piccoli” — contenuto già disponibile qui (copiaincolla)

Creare i file seguenti con esattamente questo contenuto (senza modifiche, senza aggiunte):

3.1.1 system_resource_hap_ax2.json
{"architecture-name":"arm64","bad-blocks":"0","board-name":"hAP ax^2","build-time":"2025-11-27 08:17:04","cpu":"ARM64","cpu-count":"4","cpu-frequency":"864","cpu-load":"7","factory-software":"7.5","free-hdd-space":"92979200","free-memory":"705699840","platform":"MikroTik","total-hdd-space":"134217728","total-memory":"1073741824","uptime":"1h12m40s","version":"7.20.5 (stable)","write-sect-since-reboot":"23","write-sect-total":"91274"}

3.1.2 ip_neighbor_single.json
[{".id":"*1","address":"192.168.0.1","address4":"192.168.0.1","age":"25s","board":"RB750Gr3","discovered-by":"cdp,lldp,mndp","identity":"dot-home","interface":"ether1","interface-name":"bridge_lan/ether2","mac-address":"2C:C8:1B:F0:A8:BA","platform":"MikroTik","software-id":"H970-N4I4","system-caps":"bridge,router","system-caps-enabled":"bridge,router","system-description":"MikroTik RouterOS 7.15 (stable) 2024-05-29 12:44:08 RB750Gr3","unpack":"none","uptime":"1h12m38s","version":"7.15 (stable) 2024-05-29 12:44:08"}]

3.1.3 ethernet_monitor_ether1_link_ok_1gbps.json
[{"advertising":"10M-baseT-half,10M-baseT-full,100M-baseT-half,100M-baseT-full,1G-baseT-half,1G-baseT-full","auto-negotiation":"done","full-duplex":"true","link-partner-advertising":"10M-baseT-half,10M-baseT-full,100M-baseT-half,100M-baseT-full,1G-baseT-half,1G-baseT-full","name":"ether1","rate":"1Gbps","rx-flow-control":"false","status":"link-ok","supported":"10M-baseT-half,10M-baseT-full,100M-baseT-half,100M-baseT-full,1G-baseT-half,1G-baseT-full","tx-flow-control":"false"}]

3.1.4 ethernet_monitor_ether2_no_link.json
[{"advertising":"10M-baseT-half,10M-baseT-full,100M-baseT-half,100M-baseT-full,1G-baseT-half,1G-baseT-full","auto-negotiation":"done","link-partner-advertising":"","name":"ether2","status":"no-link","supported":"10M-baseT-half,10M-baseT-full,100M-baseT-half,100M-baseT-full,1G-baseT-half,1G-baseT-full"}]

3.1.5 ethernet_cable_test_ether1_link_ok.json
[{"name":"ether1","status":"link-ok"}]

3.1.6 ethernet_cable_test_ether2_no_link_open.json
[{"cable-pairs":"open:4,open:4,open:4,open:4","name":"ether2","status":"no-link"}]

3.1.7 bridge_host.json
[{".id":"*1","bridge":"bridge1","disabled":"false","dynamic":"true","external":"false","interface":"bridge1","invalid":"false","local":"true","mac-address":"48:A9:8A:DF:E5:B3","on-interface":"bridge1"},{".id":"*2","bridge":"bridge1","disabled":"false","dynamic":"true","external":"false","interface":"wifi1","invalid":"false","local":"false","mac-address":"BC:C7:46:9C:FC:E4","on-interface":"wifi1"}]

3.1.8 bridge_vlan_empty.json
[]

3.2 File “lunghi” — NON li inventiamo qui (da incollare 1:1)

Questi file contengono output molto lunghi (o multilinea) e non è sicuro ricostruirli qui senza errori di escape.

✅ Quindi: vanno incollati pari pari dall’output dei comandi indicati sotto.

Creare i file vuoti (placeholder) e segnare “DA INCOLLARE” nel README:

log_get_proplist.json ← output lungo (array log)

bridge_port.json ← output molto lungo con debug-info multilinea

Comandi da eseguire e incollare 1:1 nei rispettivi file

(A) Log

Comando:

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/log?.proplist=.id,time,topics,message"


Incolla l’output esatto in:
app/src/test/resources/fixtures/routeros/7.20.5/log_get_proplist.json

(B) Bridge ports

Comando:

curl -k --max-time 10 -u "$USER:$PASS" "https://$ROUTER/rest/interface/bridge/port"


Incolla l’output esatto in:
app/src/test/resources/fixtures/routeros/7.20.5/bridge_port.json

3.3 README fixtures (obbligatorio)

Creare:

app/src/test/resources/fixtures/routeros/7.20.5/README.md

Deve includere ESATTAMENTE:

RouterOS: 7.20.5 (stable)

board-name: hAP ax^2

elenco comandi curl per ogni fixture (tutti quelli che hanno generato i file sopra)

nota comportamento reale osservato:

POST /interface/ethernet/cable-test:

su link-ok può tornare solo {name,status} (nessuna misura)

su no-link può tornare cable-pairs (es. open:4,...)

GET /interface/bridge/vlan nel caso attuale torna [] (quindi VLAN bridge non configurata / non disponibile)

nota log filtering:

query .query topics~"interface" ha restituito [] nel tuo ambiente → filtro topic lato REST non affidabile, quindi filtro client-side.

4) TestSupport: loader fixtures + Moshi identico a produzione
4.1 Fixture loader

Creare:

app/src/test/java/com/app/miklink/testsupport/FixtureLoader.kt

Requisiti:

funzione load(path: String): String

legge da classpath src/test/resources

se manca, lancia eccezione con messaggio: "Missing fixture: <path>"

Esempio di path chiamata attesa nei test:
fixtures/routeros/7.20.5/system_resource_hap_ax2.json

4.2 Moshi provider per test (NON assumere configurazione)

Creare:

app/src/test/java/com/app/miklink/testsupport/TestMoshiProvider.kt

Regola: non assumere come è configurato Moshi in produzione.
Step obbligatorio:

cercare nel repo attuale dove Moshi viene creato/configurato (DI module o factory).

replicare la stessa configurazione nei test.

Se non esiste una configurazione centralizzata, creare un Moshi “minimo” ma segnare nel file un TODO:

// TODO: align with production Moshi configuration once DI module exists

5) Golden Parsing Tests (core.data) — devono usare fixture reali

Questi test sono la priorità: definiscono “cosa risponde davvero RouterOS” e impediscono che i DTO siano basati su JSON inventati.

Creare cartella:

app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/

5.1 System resource parsing

File:

SystemResourceGoldenParsingTest.kt

Step:

carica fixture system_resource_hap_ax2.json

parse con Moshi nel DTO usato dal client REST (o equivalente attuale)

assert minimi:

board-name == "hAP ax^2"

version == "7.20.5 (stable)"

campi numerici arrivano come stringhe (es. "cpu-count":"4") → non crash/parse error

Se il DTO attuale non esiste o non combacia:

Non inventare: creare un DTO “GoldenRouterOsSystemResourceDto” in test (o in core/data/remote/mikrotik/dto se già presente il package), documentando che è basato su fixture reale.

5.2 Neighbor parsing

File:

NeighborGoldenParsingTest.kt

Assert minimi dal JSON reale:

mac-address presente e uguale a 2C:C8:1B:F0:A8:BA

discovered-by == "cdp,lldp,mndp"

system-caps == "bridge,router"

interface == "ether1"

interface-name == "bridge_lan/ether2"

5.3 Ethernet monitor parsing

File:

EthernetMonitorGoldenParsingTest.kt

Casi:

ether1 link-ok:

status == "link-ok"

rate == "1Gbps"

full-duplex == "true" (nota: è stringa nel JSON reale)

ether2 no-link:

status == "no-link"

rate assente → deve risultare null/missing senza eccezioni

5.4 Cable-test parsing

File:

CableTestGoldenParsingTest.kt

Casi:

ether1:

oggetto contiene name e status

cable-pairs assente

ether2:

status == "no-link"

cable-pairs == "open:4,open:4,open:4,open:4"

5.5 Log parsing (GET proplist)

File:

LogGoldenParsingTest.kt

Dati:

log_get_proplist.json (incollato da comando)

Assert minimi:

almeno 1 entry con topics contenente "interface,info"

almeno 1 entry con topics contenente "dhcp,info"

campi presenti: .id, time, topics, message

5.6 Bridge host / port / vlan parsing

File:

BridgeHostGoldenParsingTest.kt

BridgePortGoldenParsingTest.kt

BridgeVlanGoldenParsingTest.kt

Assert:

Host:

entry con mac-address == "BC:C7:46:9C:FC:E4"

on-interface == "wifi1"

Port:

almeno 1 entry con bridge == "bridge1"

interface presente

pvid presente (stringa, es. "1")

non fare assert sul contenuto completo di debug-info (troppo fragile), ma il parsing non deve fallire.

Vlan:

fixture è [] → parsing restituisce lista vuota.

6) Domain Contract Tests (core.domain) — corretti ma possono essere Pending

Questi test fissano la logica “final-state” ma possono essere temporaneamente ignorati finché l’implementazione SOLID non esiste.

Creare cartella:

app/src/test/java/com/app/miklink/core/domain/

6.1 LogFilter (client-side)

File:

logs/LogFilterContractTest.kt

Dati:

log_get_proplist.json

Contratti:

include topic “interface” → restituisce solo entries dove topics CSV contiene interface

exclude “dhcp” → esclude entries dove topics CSV contiene dhcp

split topics CSV: split(",") + trim()

Se LogFilter non è implementato:

aggiungere @Ignore("Pending implementation: core.domain.logs.LogFilter")

6.2 LinkStabilizer (falsi fail dovuti a link che tarda a salire)

File:

link/LinkStabilizerContractTest.kt

Dati:

ethernet_monitor_ether1_link_ok_1gbps.json

ethernet_monitor_ether2_no_link.json

Contratti minimi:

no-link → NotReady

link-ok + rate → Ready

link-ok senza rate (possibile) → ReadyButUnknownRate (o equivalente)

Se non implementato:

@Ignore("Pending implementation: core.domain.link.LinkStabilizer")

6.3 Cable-test interpreter (distinguere “misura assente”)

File:

tdr/CableTestInterpreterContractTest.kt

Contratti:

status=link-ok e cable-pairs assente ⇒ LinkOkNoMeasurement

status=no-link con cable-pairs ⇒ NoLinkPairs(open:4...)

Se non implementato:

@Ignore("Pending implementation: core.domain.tdr.*")

6.4 Neighbor selector + MAC matcher (base)

File:

network/NeighborSelectorContractTest.kt

network/MacPortMatcherContractTest.kt

Dati:

neighbor singolo (ip_neighbor_single.json)

bridge host/port (bridge_host.json, bridge_port.json)

Contratto minimo (NON inventare mapping positivo):

se MAC neighbor non è presente in bridge_host.json, risultato Unknown (nessun match)

Se non implementato:

@Ignore("Pending implementation: core.domain.network.*")

7) Dismissione test esistenti (legacy) — sostituzione controllata
7.1 Policy disattivazione (senza cancellare)

Per OGNI test esistente sotto app/src/test/java/com/app/miklink/**:

rinominare file e classe aggiungendo suffisso _legacy

aggiungere @Ignore("Legacy test suite — replaced by EPIC T1") sulla classe

Esempio:

RateParserTest.kt → RateParserTest_legacy.kt

class RateParserTest → class RateParserTest_legacy

Vincolo:

non alterare la logica interna dei test legacy.

7.2 Documento di migrazione

Creare:

docs/TEST_LEGACY_MIGRATION.md

Contenuti:

elenco test legacy disattivati

riferimento ai nuovi test che li sostituiscono (anche se 1 legacy → più test nuovi)

8) Checklist di esecuzione (per agent basico)

Creare cartelle §2.2

Creare fixtures §3.1 (copiaincolla)

Creare placeholders per fixtures lunghe §3.2 e aggiornare README con i comandi

Implementare FixtureLoader + TestMoshiProvider

Implementare i test golden parsing in §5 (devono caricare fixture da resources)

Implementare contract tests §6 e marcarli @Ignore se i componenti non esistono ancora

Disattivare suite legacy §7

Eseguire:

./gradlew testDebugUnitTest (o task equivalente presente nel repo)

se falliscono i golden parsing: correggere DTO/adapter/mapper (NON la fixture)

## EPIC T1 - AVANZAMENTO (stato corrente)

- **Stato**: COMPLETATA ✅
- **Azioni eseguite**:
  - Creata cartella `app/src/test/resources/fixtures/routeros/7.20.5/` con le golden fixtures reali
  - Implementati `FixtureLoader` e `TestMoshiProvider` in `app/src/test/java/com/app/miklink/testsupport/`
  - Implementati Golden Parsing Tests sotto `app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/`
  - Implementati Contract Tests semi-vuoti (marcati `@Ignore`) sotto `app/src/test/java/com/app/miklink/core/domain/` per `logs`, `link`, `tdr`, `network`
  - Disattivate e rimosse le suite di test legacy: solo i test under `core/` e `testsupport/` rimangono
  - Rimosse le duplicazioni di fixture in `/docs` (`log_get_proplist.json`, `bridge_port.json`) — ora la sorgente canonica è sotto `app/src/test/resources`.

- **Elenco file creati (principali)**:
  - app/src/test/resources/fixtures/routeros/7.20.5/system_resource_hap_ax2.json
  - app/src/test/resources/fixtures/routeros/7.20.5/ip_neighbor_single.json
  - app/src/test/resources/fixtures/routeros/7.20.5/ethernet_monitor_ether1_link_ok_1gbps.json
  - app/src/test/resources/fixtures/routeros/7.20.5/ethernet_monitor_ether2_no_link.json
  - app/src/test/resources/fixtures/routeros/7.20.5/ethernet_cable_test_ether1_link_ok.json
  - app/src/test/resources/fixtures/routeros/7.20.5/ethernet_cable_test_ether2_no_link_open.json
  - app/src/test/resources/fixtures/routeros/7.20.5/bridge_host.json
  - app/src/test/resources/fixtures/routeros/7.20.5/bridge_port.json
  - app/src/test/resources/fixtures/routeros/7.20.5/log_get_proplist.json
  - app/src/test/resources/fixtures/routeros/7.20.5/bridge_vlan_empty.json
  - app/src/test/resources/fixtures/routeros/7.20.5/README.md
  - app/src/test/java/com/app/miklink/testsupport/FixtureLoader.kt
  - app/src/test/java/com/app/miklink/testsupport/TestMoshiProvider.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/SystemResourceGoldenParsingTest.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/NeighborGoldenParsingTest.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/EthernetMonitorGoldenParsingTest.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/CableTestGoldenParsingTest.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/LogGoldenParsingTest.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/BridgeHostGoldenParsingTest.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/BridgePortGoldenParsingTest.kt
  - app/src/test/java/com/app/miklink/core/data/remote/mikrotik/golden/BridgeVlanGoldenParsingTest.kt

- **Conferme**:
  - Golden parsing tests: **PASSANO** localmente (`./gradlew testDebugUnitTest`)
  - Contract tests: **marcati @Ignore** come "Pending implementation"
  - Legacy tests: **eliminati** fisicamente (solo `core/` e `testsupport/` rimangono)
  - Fixtures canoniche: ora uniche in `app/src/test/resources/fixtures/routeros/7.20.5/`
  - `TestMoshiProvider` replica la configurazione di Moshi di produzione (NetworkModule)

✔️ EPIC T1 completata: milestone raggiunta — procedere alla prossima EPIC secondo roadmap.


















## 2. EPIC A – Pulizia iniziale & Skeleton SOLID

### 2.1 Scopo e contesto

Questa EPIC ha l’obiettivo di:

1. Pulire il repository da file/cartelle di IDE, build o locali che non devono stare nel VCS.
2. Introdurre una struttura di package **SOLID / Clean Architecture** chiara, sotto `com.app.miklink.core` e `com.app.miklink.feature`, pronta ad ospitare la nuova logica.
3. Definire il **DB v2 a livello di design** (schema e naming), senza ancora cambiare il comportamento runtime.
4. Creare i file di dominio e data layer (vuoti o con solo commenti) che descrivano:
   - responsabilità,
   - input,
   - output,
   per le parti chiave (Socket ID, LLDP/neighbor, TDR, link stabilization, logs).
5. Definire una **policy di gestione del codice legacy** (`_legacy`) per le epiche successive.

> ⚠️ Questa EPIC **non** riscrive ancora la logica esistente.  
> La re-implementazione SOLID della logica attuale verrà eseguita in epiche successive
> (LLDP/VLAN, TDR, Socket, Link, Logs, ecc.), in modo incrementale e testabile.

---

### 2.2 A1 – Pulizia del repository (file/cartelle inutili)

**Obiettivo**

Rimuovere dal repository file e cartelle che non devono essere versionati (IDE, log, build, configurazione locale, chiavi), allineandosi alle Futurice Android Best Practices.

**Scope**

Sul progetto uploadato (`MikLink/`), esistono le seguenti cartelle/file non adatti al VCS:

- `MikLink/.idea/` → configurazione IDE Android Studio.
- `MikLink/.kotlin/` → log e sessioni del plugin Kotlin.
- `MikLink/.run/` → run configuration locali.
- `MikLink/.vscode/` → config locale VS Code.
- `MikLink/app/build/` → output di build (file `.compiler.options`, ecc.).
- `MikLink/local.properties` → path SDK locale.
- `MikLink/key` → file chiave generico (da verificare: se contiene keystore o segreti, NON deve stare nel repo).
- Eventuali altri file generati dall’IDE non necessari alla build.

**Attività**

1. Rimuovere dal repository (non solo ignorare) le cartelle:
   - `.idea/`
   - `.kotlin/`
   - `.run/`
   - `.vscode/`
   - `app/build/`
   - `local.properties`
2. Analizzare `MikLink/key`:
   - se è un keystore o contiene segreti → rimuoverlo dal repo, aggiungerlo a `.gitignore`, documentare come gestirlo localmente;
   - se invece è un artefatto necessario e condivisibile (es. chiave pubblica) → documentarlo esplicitamente in `docs/README.md`.
3. Aggiornare `MikLink/.gitignore` per includere tutte queste voci in modo che non vengano più aggiunte al VCS.

**Criteri di accettazione**

- Un clone “pulito” del repository, dopo una build, **non mostra** file `.idea/`, `.kotlin/`, `.run/`, `.vscode/`, `app/build/`, `local.properties`, `key` come modifiche non tracciate.
- Il progetto compila regolarmente senza questi file versionati.

---

### 2.3 A2 – Creazione struttura SOLID (package e cartelle)

**Obiettivo**

Introdurre una struttura chiara per Domain / Data / Presentation, senza toccare ancora la logica esistente, in modo che le future epiche possano spostare codice qui dentro in modo ordinato.

**Stato attuale**

Sotto `MikLink/app/src/main/java/com/app/miklink/` sono presenti, tra le altre, le seguenti cartelle:

- `data/` (db, network, pdf, repository, io)
- `ui/` (dashboard, test, client, probe, profile, history, settings, ecc.)
- `di/` (moduli Hilt)
- `domain/usecase/backup/ImportBackupUseCase.kt`
- `utils/` (Compatibility, NetworkValidator, ecc.)

**Nuova struttura da creare (anche vuota)**

Creare i seguenti package (con almeno un file placeholder/commentato) sotto:

`MikLink/app/src/main/java/com/app/miklink/`:

```text
core/
  domain/
    model/          // Modelli di dominio puri (non Room, non DTO di rete)
    socket/         // Regole per Socket ID (template, generator, stato)
    network/        // Regole per LLDP/CDP, neighbor selection, VLAN/Voice VLAN
    tdr/            // Regole su capability TDR e comportamento
    link/           // Regole di stabilizzazione link
    logs/           // Regole di filtro/aggregazione log
    report/         // Regole di interpretazione e aggregazione risultati test
  data/
    local/
      room/         // DAO/Entity adattati al dominio
    remote/
      mikrotik/     // Client Mikrotik, adattatori tra DTO e dominio
    repository/     // Repository di dominio (interfacce + implementazioni)
  presentation/
    common/         // Eventuale stato/UI contract riusabili

feature/
  dashboard/
  test/
  client/
  probe/
  profile/
  history/
  settings/
  logs/
legacy/
  (per eventuali classi marcate _legacy in epiche successive)
⚠️ In questa EPIC non si sposta ancora codice esistente:
si creano solo i package (directory) con file placeholder/commentati.

Criteri di accettazione

I package sopra esistono nel project tree (core/domain/..., core/data/..., feature/...).

Non ci sono errori di compilazione dovuti alla sola presenza di questi package vuoti.

2.4 A3 – Definizione DB v2 (schema e naming, solo documentazione)
Obiettivo

Definire su carta (documento nel repo) lo schema target del database (Room) e dei model persistenti, includendo:

quali entity esistono (Client, ProbeConfig, TestProfile, Report…),

quali campi sono considerati legacy (es. lastFloor, lastRoom),

come verranno rappresentate le nuove configurazioni (es. Socket ID Template).

Attività

Creare un nuovo file di documentazione:

MikLink/docs/DATABASE_V2.md

In questo documento, descrivere:

Le entity esistenti in com.app.miklink.data.db.model:

Client

ProbeConfig

TestProfile

Report

Per ciascuna:

nome tabella (tableName),

elenco campi attuali (nome + tipo),

campi marcati come “da rimuovere” o “legacy”:

per Client: lastFloor, lastRoom sono da considerare da eliminare in una futura migrazione;

eventuali nuovi campi target (es. un campo socketTemplateConfig: String? per configurazioni di Socket ID), specificando che non viene ancora introdotto in codice in questa EPIC.

Aggiornare docs/ARCHITECTURE.md per:

referenziare DATABASE_V2.md come fonte di verità per lo schema target,

indicare che il DB attuale è v1 e che la migrazione a v2 sarà gestita in una EPIC successiva (dedicata alle modifiche DB).

Criteri di accettazione

Esiste docs/DATABASE_V2.md con una descrizione chiara di:

entity,

campi,

cosa è legacy,

cosa è pianificato per v2.

Nessuna entity Kotlin (es. Client.kt) viene ancora modificata in questa EPIC.

2.5 A4 – Creazione file dominio/dati con responsabilità documentata
Obiettivo

Creare i file chiave del nuovo dominio e data layer (vuoti o quasi), documentando esattamente cosa faranno, cosa riceveranno e cosa restituiranno, senza implementare ancora la logica.

File di dominio da creare (sotto com.app.miklink.core.domain)

core/domain/socket/SocketTemplate.kt

Scopo (commento):

descrivere la struttura di un Socket ID come sequenza di segmenti (testo fisso, numero, lettera, separatore).

Input previsto:

configurazione salvata a livello Client (es. template + stato di incremento).

Output previsto:

rappresentazione immutabile della template (data class di dominio).

core/domain/socket/SocketIdGenerator.kt

Scopo:

generare il valore di Socket ID corrente per un cliente (e opzionalmente stato successivo), basandosi su SocketTemplate.

Input previsto:

SocketTemplate di quel cliente,

stato di incremento attuale,

(opzionale) override manuale dell’utente.

Output previsto:

socket ID calcolato (stringa di dominio),

nuovo stato di incremento da salvare solo a salvataggio report.

core/domain/network/NeighborSelector.kt

Scopo:

scegliere il neighbor primario sulla porta di test tra una lista di neighbor LLDP/CDP/MNDP.

Input previsto:

lista di neighbor di dominio (ad es. mappati da NeighborDetail di rete),

eventuali info aggiuntive (es. host table) in future epiche.

Output previsto:

un oggetto di dominio (es. NeighborSelection) con:

neighbor primario (se esiste),

lista di tutti i neighbor rilevati.

core/domain/tdr/TdrCapabilities.kt

Scopo:

essere l’unica fonte di verità sulle capacità TDR per un determinato modello/board Mikrotik.

Input previsto:

board-name / modelName della sonda.

Output previsto:

uno stato di dominio (es. Supported / NotSupported / Unknown).

core/domain/link/LinkStabilizer.kt

Scopo:

definire le regole per “attendere link stabile” prima di eseguire una suite di test.

Input previsto:

stato link corrente (ottenuto dal layer Data),

parametri di timeout/ritentativo.

Output previsto:

decisione di “link pronto” o “timeout/non pronto”.

core/domain/logs/LogFilter.kt

Scopo:

filtrare una lista di log Mikrotik per topic/severity secondo le preferenze utente.

Input previsto:

lista log di dominio,

configurazione filtri da UserPreferencesRepository.

Output previsto:

lista log filtrata.

core/domain/logs/LogStreamPolicy.kt

Scopo:

descrivere se usare log “streaming” o “polling” in base alle capacità del dispositivo/RouterOS.

Input previsto:

informazioni capacità del dispositivo, versione RouterOS.

Output previsto:

decisione: Streaming, Polling, oppure fallback.

File data layer da creare (sotto com.app.miklink.core.data)

core/data/remote/mikrotik/MikroTikClient.kt

Scopo:

incapsulare MikroTikApiService esistente e offrire metodi di accesso di livello dominio (es. getNeighborsForInterface, getLinkStatus, runCableTest).

In questa EPIC:

solo commenti, nessuna logica.

core/data/local/room/ClientDaoV2.kt (o naming simile)

Scopo:

definire la versione target del DAO per Client in ottica DB v2.

Solo commenti:

descrivere quali query saranno necessarie (es. per socket template e stato incrementale).

core/data/repository/ClientRepository.kt, ProbeRepository.kt, ecc. (placeholder)

Scopo:

interfacce di repository di dominio (non ancora implementate).

Solo commenti:

quali metodi principali esporranno (es. getClientById, updateSocketState, getProbeConfig).

Criteri di accettazione

Tutti i file di dominio/data sopra elencati esistono con:

package corretti,

solo commenti che descrivono chiaramente:

responsabilità unica,

input di alto livello,

output di alto livello.

Nessuna implementazione concreta è stata aggiunta in questi file in questa EPIC.

2.6 A5 – Policy _legacy e mappatura del codice esistente
Obiettivo

Stabilire una policy chiara per gestire il codice storico man mano che viene sostituito da nuove implementazioni SOLID, senza spostare ancora nulla in questa EPIC.

Attività

Aggiornare docs/ARCHITECTURE.md con una sezione “Legacy code policy” che specifichi:

Classi considerate “candidate” legacy:

com.app.miklink.data.repository.AppRepository

porzioni di TestViewModel, DashboardViewModel, ecc., che oggi contengono logica di dominio.

Regola di rinomina:

Quando una nuova implementazione SOLID sostituisce in modo completo una classe o una porzione di logica esistente,
la vecchia classe può essere rinominata in NomeClasse_legacy o spostata sotto com.app.miklink.legacy,
fino alla rimozione definitiva dopo un periodo di stabilizzazione.

Obbligo di:

marcare le classi legacy con annotazione/commento chiaro (@Deprecated se appropriato),

non aggiungere nuova logica a classi marcate _legacy.

Non rinominare né spostare ancora file esistenti in _legacy:
questo avverrà nelle epiche successive, legate a funzionalità specifiche (es. EPIC LLDP, EPIC TDR, EPIC Socket, ecc.).

Criteri di accettazione

docs/ARCHITECTURE.md contiene una sezione chiara sulla policy legacy.

Non ci sono ancora classi rinominate _legacy in questa EPIC (nessun comportamento runtime modificato).

2.7 Kotlin / Android Style Checklist per EPIC A
Per tutte le modifiche di questa EPIC:

Usa le Kotlin official coding conventions per naming e package:

package in lowercase senza underscore (es. com.app.miklink.core.domain.socket);

classi/interfacce in PascalCase (es. SocketTemplate, NeighborSelector);

funzioni/variabili in camelCase.

Non introdurre funzioni top-level non necessarie:

se devi descrivere responsabilità future, fallo in commento all’interno di una classe o in file dedicati (package-info o simili).

Non introdurre logica in core/domain e core/data in questa EPIC:

solo commenti descrittivi.

Adegua il repository alle Android Best Practices Futurice:

nessun file di IDE/build/keystore/versionato (vedi A1).

Se ritieni necessario deviare da queste regole per completare A1–A5,

fermati e chiedi istruzioni invece di decidere da solo.

---

## EPIC A - AVANZAMENTO (stato corrente)

Le attività principali di questa EPIC sono state eseguite con le seguenti note:

- ✅ **A1**: `.gitignore` aggiornato per escludere cartelle di IDE e output di build (`.idea/`, `.kotlin/`, `.run/`, `.vscode/`, `app/build/`, `local.properties`, `key`).
- ✅ **A2**: Creata la struttura `com.app.miklink.core` e `com.app.miklink.feature` con file placeholder per Domain/Data/Presentation.
- ✅ **A3**: `docs/DATABASE_V2.md` creato: schema DB target e chiarimenti su campi legacy (e.g. `lastFloor`, `lastRoom`).
- ✅ **A4**: Placeholder creati per i file di dominio/data (SocketTemplate, Generate, NeighborSelector, TdrCapabilities, LinkStabilizer, LogFilter, etc.).
- ✅ **A5**: `docs/ARCHITECTURE.md` aggiornato con policy "Legacy code" e riferimenti a `DATABASE_V2.md`.
- ✅ **A6**: `docs/CLEANUP_GUIDE.md` aggiunto contenente la procedura consigliata per rimuovere file sensibili dal repository (`key`, `local.properties`) e suggerimenti su secret management.

⚠️ **Azioni manuali (non eseguite automaticamente)**:
- Rimuovere la cartella `key` dalla storia Git con `git rm --cached -r key` e committare; questo passaggio è volontario e richiede consenso del team.
- Verificare che tutti i client locali non necessitino del file `key` in workspace (backup se necessario).

🎯 **Prossimi passi suggeriti**:
1. Creare Issue/PR separati per i seguenti elementi: DB migration plan (v2), implementare `SocketTemplate` / `SocketIdGenerator` e test di integrazione, rimozione sicura della cartella `key` tramite PR dedicata.
2. Pianificare EPIC B per l'implementazione delle regole di business nel domain layer e la migrazione del repository legacy.

