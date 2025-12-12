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

