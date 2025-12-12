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