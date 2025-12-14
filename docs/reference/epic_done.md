EPIC v2: SOLID Canone A – Ripulitura layer + Normalized models + PDF/IO + rimozione leak UI/data
Stato attuale (post-script) – fatti

✅ Già fatto

Spostato ImportBackupUseCase.kt da domain/** a:

app/src/main/java/com/app/miklink/core/domain/usecase/backup/ImportBackupUseCase.kt

package patchato a com.app.miklink.core.domain.usecase.backup

Rimosse cartelle vuote domain/** (root) e core/presentation/** (vuote).

Eliminati questi file:

core/data/remote/mikrotik/MikroTikClient.kt

core/presentation/common/UiContracts.kt

core/data/local/room/ClientDaoV2.kt

Pulizia schemi Room: rimane solo `app/schemas/com.app.miklink.data.local.room.MikLinkDatabase/1.json` ed è stato aggiunto il task `guardLegacySchemas` per bloccare nuovi riferimenti legacy.

Rimossi gli stub domain non referenziati (LogFilter, LogStreamPolicy, LinkStabilizer, NeighborSelector, SocketTemplate, SocketIdGenerator, TdrCapabilities) insieme ai rispettivi contract test ignorati: le funzionalità verranno reintrodotte solo tramite epic dedicato quando esisteranno requisiti concreti.

⚠️ Non fatto

feature/** non eliminata perché esistono riferimenti a com.app.miklink.feature

core/data/repository/TestProfileRepository.kt (duplicato) non eliminato perché referenziato

Quindi la v2 dell’epic NON ripete quelle azioni: parte da qui.

Goal (Definition of Done)

Alla fine della epic:

core/data/** contiene solo contratti + tipi neutrali (NO android/room/retrofit/okhttp/moshi/itext).

ui/** non importa data/** e non importa core.data.remote|local concreti.

core/domain/** non importa DTO/Moshi/Room/Android.

Normalized models: core/domain/model/report/ReportData (+ sottotipi) = unico modello condiviso per UI/PDF/History.

PDF export:

core/data/pdf/PdfGenerator.generate(report, options): ByteArray

core/data/io/DocumentWriter.writeBytes(dest, bytes, mimeType)

SAF gestito solo nell’impl Android in data/**

Golden parsing test (Moshi/DTO) vivono in perimetro data/**.

Report audit (gli stessi md) devono andare a zero entry.

Work packages (ordine obbligatorio)
WP0 — Bloccare regole (ADR + docs) per evitare drift

0.1 Crea ADR: docs/adr/0001-solid-canone-a.md con:

canone A + regole import

decisione ReportData in domain

decisione PdfGenerator->ByteArray + DocumentDestination(uriString)

policy “unsafe https” confinata in data/remote

0.2 Aggiorna docs reference layer (es. docs/reference/project-structure.md):

elenca regole import

esempi consentiti/vietati

aggiungi “Usecase può importare core/data (ports)”

WP1 — IO neutro (rimuovere Uri/ContentResolver dai core)

Obiettivo: nessun android.net.Uri in core.

Nuovi ports in core/data:

core/data/io/DocumentDestination.kt

data class DocumentDestination(val uriString: String)

core/data/io/DocumentWriter.kt

suspend fun writeBytes(dest: DocumentDestination, bytes: ByteArray, mimeType: String): Result<Unit>

core/data/io/DocumentReader.kt

suspend fun readText(dest: DocumentDestination): Result<String>

Implementazioni in data:

data/io/AndroidDocumentWriter.kt

data/io/AndroidDocumentReader.kt

DI:

Modulo Hilt in di/** che binda i ports alle impl

Cleanup:

Elimina/migra qualunque file in core/data/io/** che oggi usa android.net.Uri o Context.

WP2 — UI → data leak (3 file): spostare Preferences/Theme/IdStrategy su ports + usecase

Input oggettivo: ui_data_imports.md segnala 3 file.

Crea model domain:

core/domain/model/preferences/ThemeConfig.kt

core/domain/model/preferences/IdNumberingStrategy.kt

Crea port in core/data:

core/data/repository/preferences/UserPreferencesRepository.kt

Crea usecase (UI usa solo questi):

core/domain/usecase/preferences/ObserveThemeConfigUseCase(.kt/.Impl.kt)

core/domain/usecase/preferences/SetThemeConfigUseCase(.kt/.Impl.kt)

core/domain/usecase/preferences/ObserveIdNumberingStrategyUseCase(.kt/.Impl.kt)

core/domain/usecase/preferences/SetIdNumberingStrategyUseCase(.kt/.Impl.kt)

Impl in data:

data/preferences/UserPreferencesRepositoryImpl.kt (DataStore o equivalente)

Fix UI (obbligatorio):

ui/dashboard/DashboardViewModel.kt → rimuove import com.app.miklink.data.repository.*

ui/settings/SettingsViewModel.kt → idem

ui/settings/SettingsScreen.kt → nessun import data/**, solo state da VM

WP3 — Backup usecase: completare migrazione e creare Impl

✅ Hai già ImportBackupUseCase.kt nel path giusto.

Da fare adesso:

Crea core/domain/usecase/backup/ImportBackupUseCaseImpl.kt

L’impl dipende da un port core/data/repository/backup/BackupRepository (o equivalente).

Il domain non deve importare Moshi. Parsing JSON in data/**.

Se esiste un tipo BackupData in data/** usato da core:

spostalo in core/domain/model/backup/BackupData.kt (se serve davvero)

oppure cambia la firma del port a importFromJson(json: String) e tieni la struttura in data.

WP4 — Normalized models: introdurre ReportData e togliere ParsedResults/UI coupling

Crea domain normalized model:

core/domain/model/report/ReportData.kt

tipi minimi (equivalenti a ciò che oggi usate in History/PDF):

TdrResult, Neighbor, PingSample, SpeedTest, ecc.

Crea port di codec in core/data:

core/data/report/ReportResultsCodec.kt

fun encode(report: ReportData): String

fun decode(json: String): Result<ReportData>

Impl in data:

data/report/codec/MoshiReportResultsCodec.kt

mapper in data/report/mapper/**

Usecase per UI/History:

core/domain/usecase/report/ParseReportResultsUseCase(.kt/.Impl.kt)

usa codec.decode(json) e incapsula gli errori

Refactor UI history:

ui/history/** deve usare ReportData, non DTO e non ParsedResults.

Target cleanup:

ui/history/model/ParsedResults.kt deve diventare inutilizzato → rimuovere (o lasciare per un commit intermedio ma target finale = rimozione)

rimuovere parser che importa UI da core/data (es. core/data/pdf/parser/ParsedResultsParser.kt): non deve più esistere come concetto.

WP5 — PDF: iText solo in data; core/data solo contratti; export via ByteArray + DocumentWriter

Port in core/data:

core/data/pdf/PdfGenerator.kt

fun generate(report: ReportData, options: PdfExportConfig): ByteArray

core/data/pdf/PdfExportConfig.kt neutro

Impl in data:

data/pdf/itext/PdfGeneratorIText.kt

data/pdf/itext/PdfDocumentHelper.kt (se serve)

Usecase export:

core/domain/usecase/report/ExportReportPdfUseCase(.kt/.Impl.kt)

PdfGenerator.generate(...) → bytes

DocumentWriter.writeBytes(...) → salva

UI export:

UI sceglie destinazione via SAF e passa DocumentDestination(uri.toString())

WP6 — Remote Mikrotik: spostare infra fuori da core/data e togliere DTO dai contratti

Move meccanico:

core/data/remote/mikrotik/** (dto/service/infra) → data/remote/mikrotik/**

dto con Moshi annotations in data/remote/mikrotik/dto/**

Retrofit API in data/remote/mikrotik/service/**

OkHttp/interceptors in data/remote/mikrotik/infra/**

Ports puliti in core/data:

lascia in core/data solo:

core/data/remote/mikrotik/MikroTikServiceProvider.kt (interfaccia)

nessun retrofit type nelle firme

Repository ports senza DTO:

ogni core/data/repository/** non deve importare dto.

output verso domain = ReportData (o model domain dedicati)

Update golden tests:

spostare test Moshi/DTO in package data/** (anche nei test path)

WP7 — RunTestUseCaseImpl: togliere Moshi/DTO e spezzare SRP

Obiettivo: RunTestUseCaseImpl (domain) fa orchestration + produce ReportData.

niente JsonAdapter

niente DTO import

niente moshi import

salvataggio resultsJson tramite ReportResultsCodec.encode(reportData)

Parsing/codec/mapping sta in data, non in domain.

WP8 — feature/**: risolvere i riferimenti e poi eliminare

✅ Lo script ha detto che ci sono riferimenti reali a com.app.miklink.feature, quindi non si elimina “a forza”.

Procedura corretta (meccanica):

Cerca tutti i riferimenti a com.app.miklink.feature e classi sotto feature/**.

Se sono solo placeholder non usati realmente:

rimuovi riferimenti

poi elimina feature/**

Se sono usati:

migra quelle classi in ui/** (se UI) o core/domain/** (se domain) o core/data/** (se port)

poi elimina feature/**

Done: feature/** non esiste più.

WP9 — TestProfileRepository duplicato: eliminare il duplicato in root repository/

Lo script ha skippato core/data/repository/TestProfileRepository.kt perché è referenziato.

Procedura:

Identifica qual è quello “canonico”:

core/data/repository/test/TestProfileRepository.kt (questo è quello corretto)

Migra tutti gli import che puntano a:

com.app.miklink.core.data.repository.TestProfileRepository
verso:

com.app.miklink.core.data.repository.test.TestProfileRepository

Poi elimina il duplicato in root.

Acceptance checklist (fine epic)

import violations = none (o 0 entry significative)

core_data_infra_hits = none

ui_data_imports = none

feature/** non esiste

duplicato TestProfileRepository eliminato

./gradlew :app:testDebugUnitTest verde

EPIC: Database “tabula rasa” + fondazione modelli Domain (definitivo)
Obiettivo

Ripartire con un DB pulito e definitivo, eliminando tutto il legacy Room v1 e rimpiazzando i modelli Room con domain models puri usati ovunque (UI, PDF, test execution, remote), rispettando la tassonomia SOLID/Clean già decisa.

Invarianti (non negoziabili)

core/domain/** non importa Room/Android/Retrofit/OkHttp.

core/data/** contiene solo contratti e tipi neutrali (idealmente domain models).

Room (Database/Entity/Dao/Migrations) sta solo in data/**.

UI non importa Entity/Dao/DTO/data/**.

DB nuovo:

Persistiamo solo: Client, ProbeConfig (singleton), TestProfile, TestReport/TestRun

ProbeConfig senza probeId, PK fissa (singleton)

resultsJson resta JSON + resultFormatVersion + colonne summary

Best practice da applicare da subito (per “futuro”)

exportSchema=true + room.schemaLocation commitato (già previsto nel progetto).

Migrazioni: quando cambierà lo schema in futuro, incrementare version e aggiungere Migration/AutoMigration (niente fallback distruttivo “globale”).

Se/quando toccate build tooling: migrazione a KSP seguendo guida ufficiale.

Deliverable

Nuovi domain models (puri) per:

Client

ProbeConfig (singleton, con last-known fields persistiti)

TestProfile

TestReport (con resultFormatVersion + resultsJson)

Nuovo Room DB in data/**:

MikLinkDatabase (version = 1)

Entities + DAO + mapper Entity↔Domain

Nome file DB nuovo: miklink

Wipe “one-shot”: cancellare esplicitamente miklink-db all’avvio del builder (tabula rasa)

Repository contracts in core/data/repository/** aggiornati per usare domain models (mai Room models).

Repository implementations in data/** che usano il nuovo DB.

Rimozione definitiva di:

core/data/local/room/v1/**

data/repositoryimpl/roomv1/**

contratti stub duplicati non usati:

app/src/main/java/com/app/miklink/core/data/repository/ClientRepository.kt

app/src/main/java/com/app/miklink/core/data/repository/ProbeRepository.kt

Aggiornamenti minimi per compilare:

DI (DatabaseModule, RepositoryModule)

UI/ViewModel/PDF/Test execution/remote provider: sostituire import Room v1 → domain models + repository contracts.

Procedura di eliminazione (OBBLIGATORIO: PowerShell, definitiva)

Da eseguire dalla root del repo.

# 1) ELIMINA legacy Room v1 (definitivo)
Remove-Item -Recurse -Force .\app\src\main\java\com\app\miklink\core\data\local\room\v1

# 2) ELIMINA repositoryimpl roomv1 (definitivo)
Remove-Item -Recurse -Force .\app\src\main\java\com\app\miklink\data\repositoryimpl\roomv1

# 3) ELIMINA contratti stub duplicati (definitivo)
Remove-Item -Force .\app\src\main\java\com\app\miklink\core\data\repository\ClientRepository.kt
Remove-Item -Force .\app\src\main\java\com\app\miklink\core\data\repository\ProbeRepository.kt

# 4) Verifica che non esistano più
Test-Path .\app\src\main\java\com\app\miklink\core\data\local\room\v1
Test-Path .\app\src\main\java\com\app\miklink\data\repositoryimpl\roomv1

Work breakdown (task list operativa per agent)
Task A — Domain models (nuovi file) ✅ COMPLETATO

Creati/aggiornati in app/src/main/java/com/app/miklink/core/domain/model/:

Client.kt

campi (da legacy, rimuovendo lastFloor, lastRoom):

clientId: Long

companyName: String

location: String?

notes: String?

networkMode: NetworkMode

staticIp: String?

staticSubnet: String?

staticGateway: String?

staticCidr: String?

minLinkRate: String

socketPrefix: String

socketSuffix: String

socketSeparator: String

socketNumberPadding: Int

nextIdNumber: Int

speedtest fields:

speedTestServerAddress: String?

speedTestServerUser: String?

speedTestServerPassword: String?

NetworkMode.kt (enum pulita: DHCP, STATIC)

ProbeConfig.kt

no probeId

ipAddress, username, password, testInterface, isHttps

last-known persistiti: isOnline, modelName, tdrSupported

TestProfile.kt (campi come legacy)

TestReport.kt

reportId: Long

clientId: Long?

timestamp: Long

socketName: String?

notes: String?

probeName: String?

profileName: String?

overallStatus: String

resultFormatVersion: Int

resultsJson: String

Poi aggiorna:

core/domain/test/model/TestExecutionContext.kt → usare domain Client, ProbeConfig, TestProfile (zero Room v1 import).

core/domain/usecase/test/RunTestUseCaseImpl.kt → rimuovere import Room v1, usare domain.

Task B — Repository contracts (core/data) ✅ COMPLETATO

Aggiornati questi contratti per usare domain models e i metodi realmente necessari (Flow + CRUD):

core/data/repository/client/ClientRepository.kt

core/data/repository/probe/ProbeRepository.kt (singleton)

core/data/repository/test/TestProfileRepository.kt

core/data/repository/report/ReportRepository.kt

core/data/repository/ProbeRepositoryModels.kt

ProbeStatusInfo deve contenere probe: core.domain.model.ProbeConfig (non Room)

Aggiorna anche:

core/data/remote/mikrotik/service/MikroTikServiceProvider.kt → parametro ProbeConfig domain (non Room v1)

core/data/remote/mikrotik/infra/MikroTikServiceFactory.kt se usa ProbeConfig v1

Task C — Nuovo Room DB (data/**) + mapper ✅ COMPLETATO

Creato nuovo package app/src/main/java/com/app/miklink/data/local/room/ con:

app/src/main/java/com/app/miklink/data/local/room/

File:

MikLinkDatabase.kt (@Database(version = 1, exportSchema = true))

entity/ClientEntity.kt, ProbeConfigEntity.kt, TestProfileEntity.kt, TestReportEntity.kt

dao/*.kt (DAO interni al data layer)

mapper/*.kt (Entity ↔ Domain)

Decisione ProbeConfig singleton:

tabella probe_config

PK fissa (es. id: Int = 1) non autogenerate

DAO con:

observe(): Flow<ProbeConfigEntity?>

getOnce(): ProbeConfigEntity?

upsert(entity: ProbeConfigEntity) (REPLACE)

Task D — DatabaseModule (DI) con wipe one-shot e nome DB nuovo ✅ COMPLETATO

Aggiornato app/src/main/java/com/app/miklink/di/DatabaseModule.kt:

usa Room.databaseBuilder(..., MikLinkDatabase::class.java, "miklink")

prima del build: context.deleteDatabase("miklink-db") (tabula rasa)

rimuovere TUTTA la logica migrazioni legacy (Migrations.ALL_MIGRATIONS, fallbackToDestructiveMigrationFrom(...), ecc.)

mantenere/ricreare callback seed default profiles sul nuovo DB (o via repository in init).

Nota: per future migrazioni, si seguirà la doc ufficiale Room (Migration/AutoMigration + version bump).

Task E — Repository implementations (data/**) ✅ COMPLETATO

Create nuove impl in data/repositoryimpl/room/:

data/repositoryimpl/room/RoomClientRepository.kt

data/repositoryimpl/room/RoomProbeRepository.kt

data/repositoryimpl/room/RoomTestProfileRepository.kt

data/repositoryimpl/room/RoomReportRepository.kt

Devono:

dipendere dai DAO del nuovo DB

fare mapping Entity↔Domain

Task F — RepositoryModule (DI) ✅ COMPLETATO

Aggiornato app/src/main/java/com/app/miklink/di/RepositoryModule.kt:

rimuovere binding a RoomV1*Repository

bindare alle nuove impl (RoomClientRepository, ecc.)

Task G — Update “choke points” per compilare (minimo indispensabile)

Aggiorna gli import Room v1 → domain/repository in:

UI:

ClientEditViewModel, ClientListViewModel, DashboardViewModel

HistoryViewModel, ReportDetailViewModel

ProbeEditViewModel

TestProfileViewModel

TestViewModel/TestExecutionScreen se passano modelli v1

PDF:

core/data/pdf/PdfGenerator.kt

core/data/pdf/impl/PdfGeneratorIText.kt

Remote impl:

data/remote/mikrotik/MikroTikServiceProviderImpl.kt

data/repositoryimpl/mikrotik/* (ProbeStatus/Connectivity/TestRepository ecc.)

Obiettivo qui: zero import al namespace Room legacy (vecchio package core.data.local.room v1) in tutto il repo.

Definition of Done (DoD)

Le cartelle legacy della Room v1 e data.repositoryimpl.roomv1 non esistono più nel filesystem.

In tutto il progetto: 0 riferimenti al namespace Room legacy (grep).

DB nuovo:

builda con version = 1

schema export continua a generare JSON in app/schemas (e viene committato).

Unit tests (JVM) passano: ./gradlew test

La doc in /docs è aggiornata (già fatto: usare lo zip).