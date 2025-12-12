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