# MikLink – Roadmap di Refactor e Feature

## 1. Introduzione e Regole di Ingaggio

Questa roadmap definisce come evolvere MikLink da progetto “funzionante” a prodotto
open source **manutenibile**, **estendibile** e **coerente** dal punto di vista tecnico
(SOLID, Clean Architecture) e dell’esperienza utente (UI/UX consistente, help chiari, multilingua).

Lo scopo è:

- ridurre il debito tecnico accumulato,
- rendere il codice comprensibile anche a contributor esterni,
- introdurre nuove feature (LLDP/VLAN, TDR, Socket ID configurabile, log live, progetti, ecc.)
  senza rompere la UI esistente delle schermate di test,
- allineare il progetto alle best practices Kotlin/Android e alle linee guida Mikrotik.

> ⚠️ **Importante**:  
> Tutte le attività descritte in questa roadmap devono essere eseguite con estrema
> attenzione. Errori di design in questa fase introdurranno problemi difficili da
> rimuovere in futuro.

---

## 1.1 Ruolo richiesto all’implementatore

Chi lavora su questa roadmap (sviluppatore o agent) deve comportarsi come:

> **Senior Kotlin/Android Developer**  
> con esperienza in:
> - Kotlin + coroutines
> - Android (ViewModel, Jetpack Compose)
> - Room
> - Retrofit/Moshi
> - Dependency Injection (es. Hilt)
> - principi SOLID e Clean Architecture

### Comportamento richiesto

- **Non inventare né assumere nulla** che non sia:
  - esplicitamente descritto in questa roadmap,
  - già presente nel codice esistente,
  - oppure documentato chiaramente nella documentazione Mikrotik ufficiale.
- Se per completare un task mancano informazioni:
  - **fermarsi**,
  - raccogliere dubbi specifici,
  - chiedere chiarimenti PRIMA di procedere.
- Non stravolgere il modello di dominio (Client, Probe, TestProfile, Report):  
  le evoluzioni devono essere **incrementali** e motivate.

---

## 1.2 Principi architetturali (SOLID / Clean Architecture)

Tutte le nuove implementazioni devono rispettare i seguenti principi:

1. **Single Responsibility Principle (SRP)**  
   Ogni classe / file ha **una sola responsabilità chiara**.  
   Esempi:
   - un componente che seleziona il neighbor primario LLDP/CDP non deve anche salvare un report;
   - un generatore di Socket ID non deve chiamare direttamente la rete.

2. **Separation of Concerns (Presentation / Domain / Data)**  
   - **Presentation**: UI + ViewModel (stato, navigation, mapping da dominio a UI).  
   - **Domain**: logica di business pura (regole su socket, LLDP, TDR, link, logging, ecc.).  
   - **Data**: accesso a DB, rete, file system (Room, Retrofit, file, backup).

3. **Open/Closed Principle (OCP)**  
   Nuove feature o varianti devono essere aggiunte estendendo componenti esistenti
   (nuove implementazioni, nuovi use case), non aggiungendo “if” in giro per il codice.

4. **Dependency Inversion Principle (DIP)**  
   - Presentation e Domain dipendono da **interfacce**, non da implementazioni concrete.
   - Room/Retrofit/Android SDK devono rimanere confinati negli strati Data / UI.

5. **Niente nuove “God class”**  
   - `AppRepository` e simili sono già sovraccarichi: le epiche serviranno anche a ridurne
     lentamente le responsabilità.
   - Ogni nuova classe deve avere un compito limitato e chiaro.

---

## 1.3 Linee guida Kotlin / Android da seguire

L’implementatore non deve conoscere a memoria tutte le style guide:  
ogni epic/task indicherà i vincoli specifici.  
Tuttavia sono sempre valide queste regole generali:

- **Kotlin official coding conventions**  
  - naming e formattazione standard Kotlin,
  - classi/interfacce in PascalCase,
  - funzioni/variabili in camelCase,
  - niente import inutili.

- **Null-safety e immutabilità**  
  - evitare `!!` (force unwrap) se non assolutamente necessario;
  - preferire `val` a `var` quando la variabile non deve cambiare;
  - preferire collezioni immutabili come default;
  - per risultati complessi, usare **data class dedicate**, non `Pair`/`Triple`.

- **Best practices Android**  
  - ViewModel senza riferimenti diretti a `Context` (se non tramite pattern appropriati);
  - niente logica di business pesante nei Composable;
  - niente operazioni blocccanti nel main thread.

- **Futurice Android best practices (estratto rilevante)**  
  - non committare nel VCS:
    - file di IDE (`.idea`, `.vscode`, ecc.),
    - output di build (`build/`),
    - keystore o file con segreti,
    - `local.properties`;
  - mantenere la struttura standard Gradle/Android (`app/src/main/java`, `res`, ecc.).

Ogni epic includerà una piccola **“Kotlin / Android Style Checklist”**
ad hoc, che l’implementatore deve seguire.

---

## 1.4 Regole di ingaggio generali per le EPIC

1. **Ogni EPIC rappresenta una feature/fix unica e testabile**  
   - Deve coprire l’intera verticale:  
     Data → Domain → Presentation (UI) per quella feature specifica.
   - Deve avere criteri di accettazione chiari.

2. **Backend + Frontend nello stesso contesto**  
   - Non esistono epiche “solo backend” o “solo UI” scollegate:  
     ogni modifica di dominio deve riflettersi nella UI, dove ha senso, e viceversa.

3. **Compatibilità progressiva**  
   - La refactorizzazione avviene per passi:  
     non si riscrive tutto in un colpo.
   - Esisterà del codice `_legacy` che conviverà temporaneamente con il nuovo codice SOLID.

4. **Nessun cambio “di massa” non necessario**  
   - Niente rename globali di classi/cartelle senza bisogno concreto e senza
     un piano di migrazione specifico per l’epic coinvolta.
   - Ogni cambiamento strutturale va motivato e circoscritto.

5. **UI di test invariate nella struttura di base**  
   - Le schermate di test già esistenti (progress, card, pass/fail) non devono essere
     modificate nella loro struttura generale.
   - È consentito **solo**:
     - aggiungere campi informativi in card esistenti,
     - migliorare testi/help,
     - correggere bug visuali.

---

## 1.5 Uso delle API Mikrotik e dei dati di rete

Per tutto ciò che riguarda la sonda Mikrotik:

- Non inventare endpoint REST o parametri non documentati ufficialmente
  o non già presenti nel codice esistente.
- Basarsi sempre su:
  - documentazione Mikrotik ufficiale (RouterOS, REST API, Neighbor discovery),
  - comportamenti osservabili reali della sonda,
  - endpoint effettivamente in uso nel codice corrente.
- Se una certa detection (es. TDR) non è affidabile usando solo API generiche,
  si preferisce:
  - una **lista di compatibilità fissa** documentata,
  - e/o un comportamento chiaramente marcato come “informativo”.

---

## 1.6 In caso di dubbio o informazione insufficiente

Se, durante l’implementazione di una epic:

- non è chiaro cosa debba fare una funzione o classe,
- la documentazione Mikrotik non conferma un’ipotesi,
- la roadmap non specifica esplicitamente un comportamento,

l’implementatore deve:

1. **fermarsi**,  
2. documentare:
   - qual è il dubbio,
   - quali opzioni vede,
   - quali rischi si corrono scegliendo da soli,
3. chiedere istruzioni, **prima** di introdurre codice che “sembra” funzionare
   ma non è allineato alla visione del progetto.

Non è accettabile “indovinare” il comportamento del sistema,
soprattutto per:

- logica di test e certificazione (LLDP, VLAN, TDR, link),
- regole di generazione Socket ID,
- struttura dei dati di report,
- interazione con dispositivi Mikrotik.

---

## 1.7 Definizione di Done (DoD) per ogni EPIC

Un’epic è considerata completata solo se:

- il comportamento funzionale è allineato alla specifica di roadmap;  
- la UI è coerente con le regole di UX (test UI non stravolta);  
- la logica di dominio aggiunta/modificata è:
  - in un componente dedicato,
  - con responsabilità unica,
  - richiamata da Presentation/Data in modo chiaro;
- non ci sono regressioni evidenti (test manuali sulle parti toccate + build pulita);
- eventuali modifiche a DB, backup, PDF sono state aggiornate in modo coerente;
- non sono stati introdotti file di IDE/build/segreti nel repository;
- la **Kotlin / Android Style Checklist** della epic è rispettata.

Solo quando tutti questi punti sono soddisfatti, l’epic può essere considerata “Done” e si può passare alla successiva.

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

