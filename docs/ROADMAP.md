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

EPIC U2.2 — Hardcoded Strings Guard (Test automatico anti-drift)
Goal

Fallire la build se in app/src/main/java/com/app/miklink/ui/** (e/o core/presentation/** se esiste) vengono introdotte stringhe hardcoded in UI (Compose), indicando file + riga + istruzioni di fix.

Scope

Detect hardcoded text in Compose UI: Text("..."), Text(text="..."), contentDescription="...", label="..." (configurabile).

Supportare un meccanismo di escape esplicito e tracciabile: commento // i18n-ignore sulla riga (solo casi giustificati).

Report deterministico in caso di failure.

Non-scope

Non implementa un custom Android Lint (troppo grande). È un unit test JVM che gira con testDebugUnitTest.

Step 0 — Baseline build (obbligatorio)

Eseguire e salvare output (se lungo, incollami solo la riga PASS/FAIL):

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Step 1 — Aggiungi il test “HardcodedStringsScanTest”

Crea file:

app/src/test/java/com/app/miklink/quality/HardcodedStringsScanTest.kt

Implementazione richiesta (regole minime):

Scansiona ricorsivamente solo:

app/src/main/java/com/app/miklink/ui/

(opzionale) app/src/main/java/com/app/miklink/core/presentation/ se esiste

Considera solo *.kt

Ignora righe che contengono // i18n-ignore

Pattern da segnalare (minimo):

Text("...")

Text(text = "...")

contentDescription = "..."

Il test deve fallire con messaggio del tipo:

HARD_CODED_UI_TEXT: <file>:<line> -> <snippet>

e sotto una sezione “FIX” con istruzioni standard:

FIX (standard)

crea una key in res/values/strings.xml

crea la traduzione in res/values-it/strings.xml

sostituisci con stringResource(R.string.<key>) (o context.getString(...) se non sei in composable)

Esegui:

./gradlew testDebugUnitTest --tests "com.app.miklink.quality.HardcodedStringsScanTest"

Se fallisce: non correggere nello stesso step. Limitarsi a rendere il report leggibile.

Step 2 — Aggiungi allowlist minima (anti falsi positivi)

Nel test, aggiungi una allowlist (regex o contains) per NON segnalare:

stringhe vuote ""

stringhe composte solo da simboli tecnici (es. "---", ":", "%") se vi servono nei layout

route/nav id (se presenti) SOLO se confinati in file di navigazione (opzionale)

Step 3 — (Opzionale ma consigliato) Test di “coverage” values-it

Crea:

app/src/test/java/com/app/miklink/quality/StringsItalianCoverageTest.kt

Regola: per ogni <string name="..."> in values/strings.xml translatable=true (default), verificare che esista anche in values-it/strings.xml.
Eccezioni consentite:

<string ... translatable="false">

chiavi che matchano un prefisso di sistema definito (es. app_name se volutamente unico)

Step 4 — Documentazione anti-drift

Aggiorna (o crea) docs/TESTING_STRATEGY.md con:

come lanciare i test

come usare // i18n-ignore (quando è ammesso, e che va motivato nel commento stesso)

Acceptance Criteria

./gradlew testDebugUnitTest -> PASS

Se introduco Text("CIAO") in ui/** il test fallisce e mi dice file+riga e come correggere.

// i18n-ignore sulla stessa riga impedisce il fail (ma resta tracciabile nel diff).

EPIC S9 — HTTPS Insecure Mode “no-verify” centralizzato e stabile
Policy (fissata)

Quando l’utente abilita “Use HTTPS”, la connessione verso la sonda deve:

usare HTTPS senza verifica certificato (trust-all)

senza hostname verification

senza warning UI (nessun banner/alert “insicuro”)

Goal

Consolidare questa policy in un solo punto (provider/service factory) ed evitare regressioni con un test che verifica la configurazione.

Step 0 — Baseline build (obbligatorio)

./gradlew :app:kspDebugKotlin

./gradlew assembleDebug

./gradlew testDebugUnitTest

Step 1 — Identifica il “single source of truth” per creare il service

Usare solo il componente che oggi crea il client Mikrotik (dalle tue note esiste già):

MikroTikServiceProvider / MikroTikServiceProviderImpl (da S6)

Regola: nessun altro punto deve costruire OkHttp/Retrofit direttamente per Mikrotik.

Step 2 — Implementa “Insecure HTTPS” dentro il provider

Nel provider (Impl):

se useHttps == true:

OkHttpClient deve usare:

TrustManager “trustAll”

SSLSocketFactory coerente con quel TrustManager

hostnameVerifier { _, _ -> true }

se useHttps == false:

HTTP normale (come già funziona)

Nota: non aggiungere warning UI o dialog.

Step 3 — Test unitario di configurazione

Crea:

app/src/test/java/com/app/miklink/data/remote/mikrotik/MikroTikServiceProviderTlsPolicyTest.kt

Test richiesti:

useHttps=false -> client NON ha hostnameVerifier permissivo (default)

useHttps=true -> hostnameVerifier permissivo + sslSocketFactory impostata (non null)

L’obiettivo è “anti-regressione”, non un test di rete reale.

Step 4 — Rimuovi duplicazioni

Cerca e rimuovi eventuali builder OkHttp/Retrofit duplicati per Mikrotik (se esistono) usando solo il provider.

Se l’output è lungo, incollami solo:

comandi usati per cercare (es. ripgrep/Select-String)

elenco file trovati

Acceptance Criteria

./gradlew :app:kspDebugKotlin -> PASS

./gradlew assembleDebug -> PASS

./gradlew testDebugUnitTest -> PASS

Il toggle HTTPS usa sempre trust-all e hostnameVerifier permissivo tramite un solo provider.

Nessun warning UI aggiunto.
